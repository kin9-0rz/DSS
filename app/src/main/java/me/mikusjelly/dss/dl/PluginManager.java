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

package me.mikusjelly.dss.dl;

import android.content.Context;
import android.util.Base64;

import com.google.gson.Gson;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

import dalvik.system.DexClassLoader;
import me.mikusjelly.dss.reflect.InvocationFieldTarget;
import me.mikusjelly.dss.reflect.InvocationTarget;


public class PluginManager {
    private static WeakReference<PluginManager> weakReferenceInstance;
    private static ArrayList<DexClassLoader> mDexClassLoaders = new ArrayList<>();
    private Context mContext;
    private String mNativeLibDir = null;

    private PluginManager(Context context) {
        mContext = context.getApplicationContext();
        mNativeLibDir = mContext.getDir("pluginlib", Context.MODE_PRIVATE).getAbsolutePath();
    }

    public static PluginManager getInstance(Context context) {
        if (weakReferenceInstance == null || weakReferenceInstance.get() == null) {
            weakReferenceInstance = new WeakReference<>(new PluginManager(context));
        }
        return weakReferenceInstance.get();
    }

    /*
    如果其中一个Class，获取失败，那么该class就跳过
     */
    private static String getFieldValue(Class<?> c, String fieldName) {
        Object obj = c;
        for (Constructor<?> constructor : c.getDeclaredConstructors()) {
            if (constructor.getParameterTypes().length == 0) {
                constructor.setAccessible(true);
                try {
                    obj = constructor.newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        String result = null;
        try {
            Field f = c.getDeclaredField(fieldName);
            f.setAccessible(true);
            if (f.isAccessible()) {
                try {
                    result = (String) f.get(obj);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        return result;
    }

    private static Object getFieldValueEx(Class<?> c, Object obj, String fieldName, boolean flag) {
        Object result = null;
        Field f;
        try {
            f = c.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return result;
        }

        if (flag) {
            boolean isStatic = Modifier.isStatic(f.getModifiers());
            if (!isStatic) {
                return result;
            }
        }

        f.setAccessible(true);
        if (f.isAccessible()) {
            try {
                result = f.get(obj);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public static String escapeFieldValue(String str) {
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

    /**
     * Load a apk. Before start a plugin Activity, we should do this first.<br/>
     * NOTE : will only be called by host apk.
     *
     * @param dexPath APK 路径
     */
    public void loadApk(String dexPath) {
        // when loadApk is called by host apk, we assume that plugin is invoked
        // by host.
        loadApk(dexPath, true);
    }

    /**
     * @param dexPath  plugin path
     * @param hasSoLib whether exist so lib in plugin
     * @return void
     */
    private void loadApk(final String dexPath, boolean hasSoLib) {
        mDexClassLoaders.add(createDexClassLoader(dexPath));

//        if (hasSoLib) {
//            copySoLib(dexPath);
//        }

    }

    private DexClassLoader createDexClassLoader(String dexPath) {
        File dexOutputDir = mContext.getDir("dex", Context.MODE_PRIVATE);
        String dexOutputPath = dexOutputDir.getAbsolutePath();
        return new DexClassLoader(dexPath, dexOutputPath, mNativeLibDir, mContext.getClassLoader());
    }

    /**
     * copy .so file to pluginlib dir.
     *
     * @param dexPath dex path
     */
    private void copySoLib(String dexPath) {
        // TODO: copy so lib async will lead to bugs maybe, waiting for
        // resolved later.

        // TODO : use wait and signal is ok ? that means when copying the
        // .so files, the main thread will enter waiting status, when the
        // copy is done, send a signal to the main thread.
        // new Thread(new CopySoRunnable(dexPath)).start();

        SoLibManager.getSoLoader().copyPluginSoLib(mContext, dexPath, mNativeLibDir);
    }

    public Object invoke(InvocationTarget target) {
        Class<?> clz = loadClass(target.getClassName());
        if (clz == null) {
            return null;
        }

        Method mtd = getMethod(clz, target);
        if (mtd == null) {
            return null;
        }

        Object result = null;

        try {
            result = mtd.invoke(null, target.getParameters());
        } catch (IllegalAccessException ignore) {
            ignore.printStackTrace();
        } catch (InvocationTargetException ignore) {
            ignore.printStackTrace();
        } catch (ExceptionInInitializerError e) {
            e.printStackTrace();
        } catch (VerifyError e) {
            e.printStackTrace();
        }

        return result;
    }

    public Class<?> loadClass(String className) {
        Class<?> clz = null;
        for (DexClassLoader dcl : mDexClassLoaders) {
            try {
                clz = dcl.loadClass(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (VerifyError e) {
                e.printStackTrace();
            }
        }

        return clz;
    }

    private Method getMethod(Class<?> clz, InvocationTarget target) {
        Method mtd = null;
        try {
            mtd = clz.getDeclaredMethod(target.getMethodName(), target.getParameterTypes());
            mtd.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return mtd;
    }

    public HashMap<String, String> getFieldValues(InvocationFieldTarget target) {
        Gson gson = new Gson();
        Object result;
        Class<?> clz = loadClass(target.getClassName());
        if (clz == null) {
            return null;
        }

//        Class<?> superclass = clz.getSuperclass();
//        while (superclass != null) {
//            if(superclass.getName().equals("java.lang.Object")){
//                break;
//            }
//            superclass = superclass.getSuperclass();
//        }

        // TODO 参数为context 是否初始化？ this.mContext
        // 初始化一次就行了
        Object obj = clz;
        Boolean isStatic = true; // 如果初始化失败，那么只有Field的类型是static才获取，非static类型则不获取
        for (Constructor<?> constructor : clz.getDeclaredConstructors()) {
            if (constructor.getParameterTypes().length == 0) {
                constructor.setAccessible(true);
                try {
                    obj = constructor.newInstance();
                    isStatic = false;
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } catch (Error e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        HashMap<String, String> fvs = new HashMap<>();
        for (String fn : target.getFieldNames()) {
            result = getFieldValueEx(clz, obj, fn, isStatic);

            if (result == null) {
                continue;
            }

            if (result instanceof String) {
                String tmp = String.valueOf(result);
                if (tmp.length() > 0) {
                    fvs.put(fn, escapeFieldValue(tmp));
                }
            } else if (result instanceof byte[]) {
                String js = gson.toJson(result, byte[].class);
                System.out.println("bytes -> " + js);
                fvs.put(fn, js);
            } else {
                String js = gson.toJson(result);
                System.out.println("others -> " + js);
                fvs.put(fn, js);
            }
        }

        return fvs;
    }

}
