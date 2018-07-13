package me.mikusjelly.dss.reflect;

import android.support.v4.util.LogWriter;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.text.StringEscapeUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


import me.mikusjelly.dss.dl.PluginManager;
import me.mikusjelly.dss.utils.FileUtils;

public class Driver {


    private final static String Output = "/data/local/dss_data/od-output.json";

    static List<InvocationTarget> targets = null;

    /**
     * 执行解密
     *
     * @param fileName 解密数据的文件名
     */
    public static void dss(PluginManager mPluginManager, String fileName) {
        Log.w("DSS", "----------------------------------------------------------------");
        try {
            try {
                decode(fileName, mPluginManager);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void decode(String fileName, PluginManager mPluginManager) throws IOException, NoSuchMethodException, ClassNotFoundException {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();

        String targetJson = FileUtils.readFile(fileName);
        String head20 = targetJson.substring(0, 20);

        if (head20.contains("\"type\": \"field\"")) {
            JsonObject jsonObject = new JsonParser().parse(targetJson).getAsJsonObject();
            List<InvocationFieldTarget> targets = genarateFieldTargets(jsonObject.get("data").getAsJsonArray(), gson);
            decodeFeilds(targets, mPluginManager, gson);
        } else {
            JsonArray jsonObject = new JsonParser().parse(targetJson).getAsJsonArray();
            List<InvocationTarget> targets = genarateMethodTargets(jsonObject, gson, mPluginManager);
            decodeMethods(targets, mPluginManager, gson);
        }
    }

// ----------------------------------------------------------------------------------------------

    private static void decodeFeilds(List<InvocationFieldTarget> targets, PluginManager mPluginManager, Gson gson) {
        Log.w("DSS", "解密Field - 开始");
        HashMap<String, HashMap<String, String>> idToOutput = new HashMap<>();
        for (InvocationFieldTarget target : targets) {
            HashMap<String, String> fieldValues = mPluginManager.getFieldValues(target);
            if (fieldValues == null || fieldValues.size() == 0) {
                continue;
            }
            Log.w("DSS", "Target Next");
            idToOutput.put(target.getClassName(), fieldValues);

        }

        Log.w("DSS", "解密Field - 结束");

        String json = gson.toJson(idToOutput);

        try {
            FileUtils.writeFile(Output, json);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private static List<InvocationFieldTarget> genarateFieldTargets(JsonArray targetItems, Gson gson)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, IOException {
        List<InvocationFieldTarget> targets = new LinkedList<>();
        for (JsonElement element : targetItems) {

            JsonObject targetItem = element.getAsJsonObject();
            String className = targetItem.get("className").getAsString();
            JsonArray fieldName = targetItem.get("fieldName").getAsJsonArray();
//            JsonArray fieldType = targetItem.get("fieldType").getAsJsonArray();


            ArrayList<String> arr = new ArrayList<>();
            for (JsonElement s : fieldName) {
                arr.add(s.getAsString());
            }

            targets.add(new InvocationFieldTarget(className, arr));
        }

        return targets;
    }

// ----------------------------------------------------------------------------------------------


    private static void decodeMethods(List<InvocationTarget> targets, PluginManager mPluginManager, Gson gson) {
        Log.w("DSS", "解密method");
        String output = null;
        String status;
        Map<String, String[]> idToOutput = new HashMap<>();

        Boolean isArr = false;
        for (InvocationTarget target : targets) {
            output = null;
            try {
                Object object = mPluginManager.invoke(target);
                status = "success";

                if (object == null) {
                    continue;
                }

                if (object instanceof String) {
                    output = String.valueOf(object);
                } else if (object instanceof byte[]) {
                    output = Arrays.toString((byte[]) object);
                    isArr = true;
                    Log.w("DSS", "is Array " + output);
                }

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                continue;
            }

            if (output == null) {
                continue;
            }

            if (isArr) {
                Log.w("DSS", status + "" + output);
                idToOutput.put(target.getId(), new String[]{output});
                break;
            } else {
                idToOutput.put(target.getId(), new String[]{escape(output)});
            }
        }

        String json;
        if (isArr && output != null) {
            json = gson.toJson(output);
        } else {
            json = gson.toJson(idToOutput);
        }

        Log.w("DSS", json);
        try {
            FileUtils.writeFile(Output, json);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private static List<InvocationTarget> genarateMethodTargets(JsonArray targetItems, Gson gson, PluginManager mPluginManager)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, IOException {
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
                target = buildMethodTarget(mPluginManager, gson, id, className, methodName, arguments);
            } catch (Exception e) {
                System.out.println("Could not build target: " + className + ";->" + methodName);
                e.printStackTrace();
                continue;
            }

            targets.add(target);

        }

        return targets;
    }


    // https://github.com/mikusjelly/OracleDriver
    private static InvocationTarget buildMethodTarget(PluginManager mPluginManager, Gson gson, String id, String className, String methodName,
                                                      String... args) throws Exception {
        Class<?>[] parameterTypes = new Class[args.length];     // 存放参数类型
        Object[] parameters = new Object[parameterTypes.length]; // 存放参数的值

        // 这里主要做参数的转换
        for (int i = 0; i < parameterTypes.length; i++) {
            String[] parts = args[i].split(":", 2);

            if (parts[0].equals("Object")) {
                Class<?> aClass = smaliToJavaClass(parts[1], mPluginManager);

                parameterTypes[i] = aClass;
                // 如果是一个对象，则使用插件初始化
                Object o = null;
                for (Constructor<?> constructor : aClass.getDeclaredConstructors()) {
                    if (constructor.getParameterTypes().length == 0) {
                        constructor.setAccessible(true);
                        o = constructor.newInstance();
                    }
                }
                parameters[i] = o;
                continue;
            }

            if (parts.length == 1) {
                // 无参
                parameters[i] = null;
                continue;
            }

            parameterTypes[i] = smaliToJavaClass(parts[0], mPluginManager);
            String jsonValue = parts[1];
            System.out.println("JV >>>> " + jsonValue);
            if (parameterTypes[i] == String.class) {
                parameters[i] = StringEscapeUtils.unescapeJava(jsonValue);
            } else if (parameterTypes[i] == char.class) {
                int ii = Integer.valueOf(jsonValue);
                char c = (char) ii;
                parameters[i] = c;
            } else if (parameterTypes[i] == char[].class) {
                int[] bytes = (int[]) gson.fromJson(jsonValue, Class.forName("[I"));
                char[] chars = new char[bytes.length];
                for (int j = 0; j < bytes.length; j++) {
                    char c = (char) bytes[j];
                    chars[j] = c;
                }
                parameters[i] = chars;
            } else if (parameterTypes[i] == byte[].class) {
                byte[] bytes = (byte[]) gson.fromJson(jsonValue, Class.forName("[B"));
                for (int j = 0; j < bytes.length; j++) {
                    System.out.print(j + ' ');
                }
                System.out.println();
                parameters[i] = bytes;
            } else {
                parameters[i] = gson.fromJson(jsonValue, parameterTypes[i]);
            }
        }

        return new InvocationTarget(id, className, methodName, parameterTypes, parameters);
    }


    // https://github.com/mikusjelly/OracleDriver
    private static Class<?> smaliToJavaClass(String className, PluginManager mPluginManager) throws ClassNotFoundException {
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
//            case "[I":
//                return int[].class;
//            case "[B":
//                return byte[].class;
//            case "[S":
//                return short[].class;
//            case "[J":
//                return long[].class;
//            case "[C":
//                return char[].class;
//            case "[F":
//                return float[].class;
//            case "[D":
//                return double[].class;
            case "java.lang.String":
                System.out.println("class for name????");
                return Class.forName(className);
            default:
                // 仅能反射 [B, [C, java.lang.String，其他非内置对象，只能通过插件管理器加载
                return mPluginManager.loadClass(className);
        }
    }

    // From https://github.com/jmendeth/quickvm/blob/master/src/quickvm/util/StringUtil.java
    public static String escape(String str) {
        StringBuilder sb = new StringBuilder();
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
        return sb.toString();
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
        Log.d("TESTTEST", str + " -> " + sb);
        return sb.toString();
    }
}
