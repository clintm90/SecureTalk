package com.trackcell.securetalk;

import android.app.Application;
import android.content.Intent;

public class Initialize extends Application
{
    public static String SecureTalkServer = "http://securetalk-sql.sourceforge.net/";
    public static Intent mMessageWorkerService;

    @Override
    public void onCreate()
    {
        super.onCreate();

        mMessageWorkerService = new Intent(this, MessageWorker.class);
        startService(mMessageWorkerService);
    }
}