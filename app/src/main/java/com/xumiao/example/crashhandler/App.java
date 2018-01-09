package com.xumiao.example.crashhandler;

import android.app.Application;

/*
 * 包名       com.xumiao.example.crashhandler
 * 文件名:    App
 * 创建者:    xuyj
 * 创建时间:  2018/1/9 on 11:15
 * 描述:     TODO
 */

public class App extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        CrashHandler.getInstance().init(this);
    }
}
