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

package me.mikusjelly.dss.utils;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;


public class PropertyUtil {

    public static void set(String key, String val) {
        Shell.SU.run("setprop " + key + " " + val);
    }

    public static String get(String key) {
        List<String> result = Shell.SU.run("getprop " + key);
        return result.get(0);
    }
//
//    public static String getInEmu(String key) {
//        List<String> result = Shell.SH.run("getprop " + key);
//        Log.d("DEBUGX", result.toString());
//        return result.get(0);
//    }
//
//    public static void setInEmu(String key, String val) {
//        String cmd = "setprop " + key + " " + val;
//        Log.d("DEBUG Emu", cmd);
//        Shell.SH.run(cmd);
//    }
//
//    public static List<String> runCmd(String command) {
//        ArrayList<String> result = new ArrayList<>();
//        try {
//            Process process = Runtime.getRuntime().exec(command);
//            process.waitFor();
//            InputStreamReader inputStr = new InputStreamReader(process.getInputStream());
//            BufferedReader br = new BufferedReader(inputStr);
//
//            String line;
//            while ((line = br.readLine()) != null) {
//                result.add(line);
//            }
//
//            process.destroy();
//            br.close();
//            inputStr.close();
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        return result;
//    }
//
//    //    ro.setupwizard.mode]: [EMULATOR]
//    private static String getSystemProperty(String name) throws Exception {
//        Class systemPropertyClazz = Class.forName("android.os.SystemProperties");
//        return (String) systemPropertyClazz.getMethod("get", new Class[]{String.class})
//                .invoke(systemPropertyClazz, new Object[]{name});
//    }
//
//    public static boolean checkEmulator() {
//        boolean emu = false;
//        try {
//            emu = getSystemProperty("ro.kernel.qemu").length() > 0;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return emu;
//
//
//    }
}
