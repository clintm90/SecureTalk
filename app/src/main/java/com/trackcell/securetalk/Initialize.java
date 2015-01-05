package com.trackcell.securetalk;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.format.DateUtils;

public class Initialize extends Application
{
    public static String SecureTalkServer = "http://securetalk-sql.sourceforge.net/";
    public static Intent MessageWorkerService;
    public static boolean NewMessage = true;
    public static boolean ActivityForeground = false;
    public static long ActivityRefreshTime = DateUtils.SECOND_IN_MILLIS * 5;
    public static long ServiceRefreshTime = DateUtils.SECOND_IN_MILLIS * 10;
    public static long InitTime = 50;

    @Override
    public void onCreate()
    {
        super.onCreate();

        MessageWorkerService = new Intent(this, MessageWorker.class);
        getApplicationContext().startService(MessageWorkerService);
    }
    
    public static void Badge(Context context, boolean show)
    {
        if(show)
        {
            context.getPackageManager().setComponentEnabledSetting(new ComponentName("com.trackcell.securetalk", "com.trackcell.securetalk.BadgeNo"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            context.getPackageManager().setComponentEnabledSetting(new ComponentName("com.trackcell.securetalk", "com.trackcell.securetalk.Badge"), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        }
        else
        {
            context.getPackageManager().setComponentEnabledSetting(new ComponentName("com.trackcell.securetalk", "com.trackcell.securetalk.BadgeNo"), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            context.getPackageManager().setComponentEnabledSetting(new ComponentName("com.trackcell.securetalk", "com.trackcell.securetalk.Badge"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    @DebugOnly
    public static String GenerateRandomWords()
    {
        String[] words = new String[] {"Ok ben d'accord", "Comment ca va ?", "Ok demain", "Si tu veux !", "ptdr", "Allez à plus", "Ben d'accord", "S'il vous plait ?", ":)", "Oui"};
        return words[((int) Math.round(Math.random() * 9))];
    }
}