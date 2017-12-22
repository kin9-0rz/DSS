/*
 * MIT License
 *
 * Copyright (c) 2017 mikusjelly
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.mikusjelly.dss;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import me.mikusjelly.dss.dl.PluginManager;

/**
 * Dexsim 解密服务
 * <p>
 * Created by bin on 13/12/2017.
 */

public class DSService extends Service {

    private final String TAG = "DSS";
    /**
     * 标记解密对象是否为新的APK，如果为新，则重新加载APK
     * dss 客户端启动时，会把该属性置为1
     * dss 服务端读取该属性，加载AP时，之后会把其置为0
     */
    private final String PROP_IS_NEW = "dss.is.new";
    /**
     * 标记解密过程是否结束
     * dss 客户端内部每次提交解密数据时，都会将该属性设置为0
     * dss 服务端每次解密结束后，都会把该属性设置为1
     * dss 客户端会定时读取属性的值，如果为1，则获取解密后的数据
     */
    private final String PROP_IS_FINISH = "dss.is.finish";

    public PluginManager mPluginManager;

    private static Gson GSON = null;
    List<InvocationTarget> targets = null;

    public DSService() {
        Log.d(TAG, "DSService");
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
    }





    class TaskThread extends Thread {
        public void run() {
            dss("/data/local/od-targets.json");
            PropertyUtil.set(PROP_IS_FINISH, "Yes");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (PropertyUtil.get(PROP_IS_NEW).equals("Yes")) {
            if (mPluginManager != null) {
                mPluginManager.clear();
            }
            mPluginManager = PluginManager.getInstance(this);
            PropertyUtil.set(PROP_IS_NEW, "No");
        }

        initData();

        new TaskThread().start();

        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void initData() {
        String pluginFolder = "/data/local/dss";
        File file = new File(pluginFolder);
        File[] plugins = file.listFiles();


        for (File plugin : plugins) {
            mPluginManager.loadApk(plugin.getAbsolutePath());
        }

    }


    /**
     * 执行解密
     *
     * @param fileName 解密数据的文件名
     */
    private void dss(String fileName) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        try {
            targets = loadTargetsFromFile(fileName, gson);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IOException e) {
            System.out.println("Unable to parse targets.");
        }

        String output;
        String status;
        Map<String, String[]> idToOutput = new HashMap<>();
        for (InvocationTarget target : targets) {
            try {
                Object object = mPluginManager.invokeTarget(target);
                status = "success";

                if (object == null) {
                    continue;
                }

                output = String.valueOf(object);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                continue;
            }

            idToOutput.put(target.getId(), new String[]{status, escapeString(output)});
        }

        String json = gson.toJson(idToOutput);
        try {
            FileUtils.writeFile("/data/local/od-output.json", json);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    // https://github.com/mikusjelly/OracleDriver
    private static List<InvocationTarget> loadTargetsFromFile(String fileName, Gson gson)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, IOException {
        String targetJson = FileUtils.readFile(fileName);
        JsonArray targetItems = new JsonParser().parse(targetJson).getAsJsonArray();
        // JsonArray targetItems = json.getAsJsonArray();
        List<InvocationTarget> targets = new LinkedList<>();
        for (JsonElement element : targetItems) {
            JsonObject targetItem = element.getAsJsonObject();
            String id = targetItem.get("id").getAsString();
            String className = targetItem.get("className").getAsString();
            String methodName = targetItem.get("methodName").getAsString();
            JsonArray argumentsJson = targetItem.get("arguments").getAsJsonArray();
            String[] arguments = new String[argumentsJson.size()];
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = argumentsJson.get(i).getAsString();
            }

            InvocationTarget target;
            try {
                target = buildTarget(gson, id, className, methodName, arguments);
            } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
                System.out.println("Could not build target: " + className + ";->" + methodName);
                e.printStackTrace();
                continue;
            }

            targets.add(target);

        }


        return targets;
    }

    // https://github.com/mikusjelly/OracleDriver
    private static Class<?> smaliToJavaClass(String className) throws ClassNotFoundException {
        switch (className) {
            case "I":
                return int.class;
            case "V":
                return void.class;
            case "Z":
                return boolean.class;
            case "B":
                return byte.class;
            case "S":
                return short.class;
            case "J":
                return long.class;
            case "C":
                return char.class;
            case "F":
                return float.class;
            case "D":
                return double.class;
            default:
                return Class.forName(className);
        }
    }

    // https://github.com/mikusjelly/OracleDriver
    private static InvocationTarget buildTarget(Gson gson, String id, String className, String methodName,
                                                String... args) throws ClassNotFoundException, NoSuchMethodException, SecurityException {
        Class<?>[] parameterTypes = new Class[args.length];
        Object[] parameters = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            String[] parts = args[i].split(":", 2);
            parameterTypes[i] = smaliToJavaClass(parts[0]);

            if (parts.length == 1) {
                parameters[i] = null;
            } else {
                String jsonValue = parts[1];
                if (parameterTypes[i] == String.class) {
                    try {
                        // Normalizing strings to byte[] avoids escaping ruby, bash, adb shell, and java
                        byte[] stringBytes = (byte[]) gson.fromJson(jsonValue, Class.forName("[B"));
                        parameters[i] = new String(stringBytes);
                    } catch (JsonSyntaxException ex) {
                        // Possibly not using byte array format for string (good luck)
                        parameters[i] = jsonValue;
                    }
                } else if (parameterTypes[i] == char.class) {
                    try {
                        int ii = Integer.valueOf(jsonValue);
                        char c = (char) ii;
                        parameters[i] = c;
                    } catch (JsonSyntaxException ex) {
                        System.out.println(ex.getLocalizedMessage());
                        parameters[i] = jsonValue;
                    }
                } else if (parameterTypes[i] == char[].class) {
                    int[] bytes = (int[]) gson.fromJson(jsonValue, Class.forName("[I"));
                    char[] chars = new char[bytes.length];
                    for (int j = 0; j < bytes.length; j++) {
                        char c = (char) bytes[j];
                        chars[j] = c;
                    }
                    parameters[i] = chars;
                } else {
//                    System.out.println("Parsing: " + jsonValue + " as " + parameterTypes[i]);
                    parameters[i] = gson.fromJson(jsonValue, parameterTypes[i]);
                }
            }
        }

        return new InvocationTarget(id, className, methodName, parameterTypes, parameters);
    }

    // From https://github.com/jmendeth/quickvm/blob/master/src/quickvm/util/StringUtil.java
    public static String escapeString(String str) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : str.toCharArray()) {
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                default:
                    if (c < 0x20 || c == 0x7F) sb.append(String.format("\\u%04X", (int) c));
                    else sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

}
