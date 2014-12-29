package com.trackcell.securetalk;

import android.app.Application;
import android.content.Intent;
import android.text.format.DateUtils;

import de.greenrobot.event.EventBus;

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

        EventBus.getDefault().register(this);
    }

    public void onEventMainThread(String input)
    {
        /*NotificationManager mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        Notification n  = new Notification.Builder(this)
                .setContentTitle("New mail from " + "test@gmail.com")
                .setContentText("Subject")
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.icon, "Call", pIntent)
                .addAction(R.drawable.icon, "More", pIntent)
                .addAction(R.drawable.icon, "And more", pIntent).build();

        mNotificationManager.notify(0, n);*/
    }

    @DebugOnly
    public static String GenerateRandomWords()
    {
        String[] words = new String[] {"Ok ben d'accord", "Comment ca va ?", "Ok demain", "Si tu veux !", "ptdr", "Allez à plus", "Ben d'accord", "S'il vous plait ?", ":)", "Oui"};
        return words[((int) Math.round(Math.random() * 9))];
    }
}