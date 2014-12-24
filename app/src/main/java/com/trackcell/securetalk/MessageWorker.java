package com.trackcell.securetalk;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MessageWorker extends Service
{
    private Handler mHandler;
    private NotificationManager mNotificationManager;
    private SharedPreferences mPrefsGlobal;
    private Timer mTimer;

    public MessageWorker()
    {
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mPrefsGlobal = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(MainLoop, Initialize.InitTime, Initialize.ServiceRefreshTime);

        return START_STICKY;
    }

    private TimerTask MainLoop = new TimerTask()
    {
        @Override
        public void run()
        {
            new Runnable()
            {
                @Override
                public void run()
                {
                    Handler handler = new Handler()
                    {
                        public void handleMessage(android.os.Message msg)
                        {
                            Toast.makeText(getApplicationContext(), "salut", Toast.LENGTH_LONG).show();
                        }
                    };
                    handler.sendEmptyMessage(0);
                    //Thread.currentThread().setName("MessageWorker_MainLoop");
                    
                    /*GetMessageByID CallLoopItem = new GetMessageByID();
                    CallLoopItem.execute(getIntent().getStringExtra("recipient"), mPrefsGlobal.getString("owner", "none"));*/
                }
            };
        }
    };

    @Override
    public void onDestroy()
    {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    private void runOnUiThread(Runnable runnable)
    {
        mHandler.post(runnable);
    }
}
