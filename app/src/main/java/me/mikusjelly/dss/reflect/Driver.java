package me.mikusjelly.dss.reflect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import me.mikusjelly.dss.dl.PluginManager;
import me.mikusjelly.dss.utils.FileUtils;

public class Driver {

    static List<InvocationTarget> targets = null;

    /**
     * 执行解密
     *
     * @param fileName 解密数据的文件名
     */
    public static void dss(PluginManager mPluginManager, String fileName) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        try {
            targets = loadTargetsFromFile(fileName, gson);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IOException e) {
            System.out.println("Unable to parse targets.");
            return;
        }

        String output;
        String status;
        Map<String, String[]> idToOutput = new HashMap<>();
        for (InvocationTarget target : targets) {

            try {
                Object object = mPluginManager.invoke(target);
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
