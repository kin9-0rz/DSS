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
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;

import me.mikusjelly.dss.dl.PluginManager;
import me.mikusjelly.dss.reflect.Driver;
import me.mikusjelly.dss.utils.PropertyUtil;

public class DSService extends Service {

    /**
     * DSS解密线程解密结束后，会标记为Yes
     * dexsim 会定时读取属性的值，结果为Yes，则获取解密后的数据
     */
    private final String PROP_IS_FINISH = "dss.is.finish";

    private PluginManager mPluginManager;

    @Override
    public void onCreate() {
        Log.d("DSS", "onCreate");

        registerCmdReceiver();
        initPlugins();

        super.onCreate();
    }

    private void registerCmdReceiver() {
        IntentFilter filter = new IntentFilter("dss.start");
        CmdReceiver receiver = new CmdReceiver();
        registerReceiver(receiver, filter);
    }


    private void initPlugins() {
        mPluginManager = PluginManager.getInstance(this);

        String pluginFolder = "/data/local/dss";
        File file = new File(pluginFolder);
        File[] plugins = file.listFiles();

        for (File plugin : plugins) {
            mPluginManager.loadApk(plugin.getAbsolutePath());
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("DSS", "onStartCommand");

        // 服务刚刚创建，没有解密操作。
        String PROP_IS_NEW = "dss.is.new";
        if (PropertyUtil.get(PROP_IS_NEW).equals("Yes")) {
            PropertyUtil.set(PROP_IS_NEW, "No");
        } else {
            new Thread() {
                public void run() {
                    Driver.dss(mPluginManager, "/data/local/dss_data/od-targets.json");
                    PropertyUtil.set(PROP_IS_FINISH, "Yes");
                }
            }.start();
        }
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

}
