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
import android.util.Log;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import dalvik.system.DexClassLoader;
import me.mikusjelly.dss.InvocationTarget;

/**
 * Created by bin on 08/12/2017.
 */

public class PluginManager {

    private static PluginManager sInstance;

    public ArrayList<DexClassLoader> getDexClassLoaders() {
        return mDexClassLoaders;
    }

    ArrayList<DexClassLoader> mDexClassLoaders = new ArrayList<>();
    private Context mContext;
    private String mNativeLibDir = null;
    private DexClassLoader mDexClassLoader = null;
    private String className;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;

    private PluginManager(Context context) {
        mContext = context.getApplicationContext();
        mNativeLibDir = mContext.getDir("pluginlib", Context.MODE_PRIVATE).getAbsolutePath();
    }

    public static PluginManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PluginManager.class) {
                if (sInstance == null) {
                    sInstance = new PluginManager(context);
                }
            }
        }

        return sInstance;
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
     * @return
     */
    public void loadApk(final String dexPath, boolean hasSoLib) {
        mDexClassLoader = createDexClassLoader(dexPath);

        mDexClassLoaders.add(mDexClassLoader);

        if (hasSoLib) {
            copySoLib(dexPath);
        }

    }

    private DexClassLoader createDexClassLoader(String dexPath) {
        File dexOutputDir = mContext.getDir("dex", Context.MODE_PRIVATE);
        String dexOutputPath = dexOutputDir.getAbsolutePath();
        return new DexClassLoader(dexPath, dexOutputPath, mNativeLibDir, mContext.getClassLoader());
    }

    /**
     * copy .so file to pluginlib dir.
     *
     * @param dexPath
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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public Class<?> loadClass(String className) {
        Class<?> clz = null;
        for (DexClassLoader dcl : mDexClassLoaders) {
            try {
                clz = Class.forName(className, true, dcl);
            } catch (Exception | ExceptionInInitializerError ignored) {
            }
        }

        return clz;
    }

    public Method getMethod(Class<?> clz) {
        Method mtd = null;
        try {
            mtd = clz.getDeclaredMethod(this.methodName, this.parameterTypes);
            mtd.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return mtd;
    }

    public Object invokeTarget(InvocationTarget invocationTarget) {
        setClassName(invocationTarget.getClassName());
        setMethodName(invocationTarget.getMethodName());
        setParameterTypes(invocationTarget.getParameterTypes());
        setParameters(invocationTarget.getParameters());

        return invoke();
    }

    //   https://github.com/wuxiaosu/DexClassLoaderDemo/blob/master/dexclassloaderdemo/src/main/java/com/wuxiaosu/dexclassloaderdemo/DexClassManage.java

    /**
     * Example:
     * mPluginManager.setClassName("com.ms.plugin.bm");
     * mPluginManager.setMethodName("a");
     * mPluginManager.setParameterTypes(new Class[]{String.class});
     * mPluginManager.setParameters(new Object[]{"BgcdHBQf"});
     * Object object = mPluginManager.invoke();
     *
     * @return Object result
     */

    public Object invoke() {

        Class<?> clz = loadClass(this.className);
        if (clz == null) {
            return null;
        }

        Method mtd = getMethod(clz);
        if (mtd == null) {
            return null;
        }
//        int  modifiers  =  mtd.getModifiers();
//        boolean mIsStatic = Modifier.isStatic(modifiers);

        // 实例化
        Object instance = null;
//        Constructor localConstructor = null;

        Object result = null;

        try {
            result = mtd.invoke(instance, this.parameters);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return result;

    }

    public void clear() {
        this.mDexClassLoaders.clear();
    }
}
