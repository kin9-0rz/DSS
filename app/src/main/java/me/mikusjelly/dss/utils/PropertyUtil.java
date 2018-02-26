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

    public static String get_new() {
        List<String> result = Shell.SH.run("cat /data/local/dss_data/new");
        return result.get(0);
    }

    public static String get_finish() {
        List<String> result = Shell.SH.run("cat /data/local/dss_data/finish");
        return result.get(0);
    }

    public static void set_new(String key) {
        Shell.SH.run(String.format("echo %s > /data/local/dss_data/new", key));
    }

    public static void set_finish(String key) {
        Shell.SH.run(String.format("echo %s > /data/local/dss_data/finish", key));
    }

}
