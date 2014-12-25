package com.trackcell.securetalk;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
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
        mTimer.scheduleAtFixedRate(new MainLoop(), Initialize.InitTime, Initialize.ServiceRefreshTime);

        return START_STICKY;
    }

    private final Handler ToastHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            Toast.makeText(getApplicationContext(), "test", Toast.LENGTH_SHORT).show();
        }
    };

    private class GetMessageByID extends AsyncTask<String, Void, String>
    {
        protected String doInBackground(String... params)
        {
            Thread.currentThread().setName("MessageWorker_GetMessageByID");

            if (params[0].equals("none"))
            {
                cancel(true);
            }

            try
            {
                String rts = "", c;
                URL mURL = new URL(Initialize.SecureTalkServer + "getMessageByID.php?sender=" + "%" + "&recipient=" + params[0] + "&put=false");
                BufferedReader reader = new BufferedReader(new InputStreamReader(mURL.openStream()));
                while ((c = reader.readLine()) != null)
                {
                    rts += c;
                }
                return rts;
            }
            catch (UnknownHostException e)
            {
                cancel(true);
                return null;
            }
            catch (Exception e)
            {
                cancel(true);
                e.printStackTrace();
                return null;
            }
        }

        protected void onCancelled()
        {
            //TODO: implement onCancelled()
        }

        protected void onPostExecute(final String input)
        {
            int i = 0;
            try
            {
                JSONObject mRoot = new JSONObject(input);
                JSONObject mItems = mRoot.getJSONObject("result");

                for (i = 0; i < mItems.getJSONArray("item").length(); i++)
                {
                    JSONObject currentObject = mItems.getJSONArray("item").getJSONObject(i);

                    ToastHandler.sendEmptyMessage(0);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private class MainLoop extends TimerTask
    {
        public void run()
        {
            Thread.currentThread().setName("MessageWorker_MainLoop");
            GetMessageByID CallLoopItem = new GetMessageByID();
            CallLoopItem.execute(mPrefsGlobal.getString("owner", "none"));
        }
    }

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
