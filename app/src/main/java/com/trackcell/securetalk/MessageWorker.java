package com.trackcell.securetalk;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;

public class MessageWorker extends Service
{
    private NotificationManager mNotificationManager;
    private SharedPreferences mPrefsGlobal;

    final AsyncTask<String, Void, String> Task = new AsyncTask<String, Void, String>()
    {
        @Override
        protected String doInBackground(String... params)
        {
            if(params[0].equals("none"))
            {
                cancel(true);
            }

            try
            {
                String rts = "", c;
                URL mURL = new URL(Initialize.SecureTalkServer + "getMessageByID.php?id=" + params[0]);
                BufferedReader reader = new BufferedReader(new InputStreamReader(mURL.openStream()));
                while ((c = reader.readLine()) != null)
                    rts += c;
                return rts;
            }
            catch(UnknownHostException e)
            {
                cancel(true);
                return null;
            }
            catch(Exception e)
            {
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onCancelled()
        {
        }

        @Override
        protected void onPostExecute(String params)
        {
            try
            {
                JSONObject mRoot = new JSONObject(params);
                JSONObject mItems = mRoot.getJSONObject("result");

                for (int i = 0; i < mItems.getJSONArray("item").length(); i++)
                {
                    JSONObject currentObject = mItems.getJSONArray("item").getJSONObject(i);

                    /*Notification mNotification = new Notification.Builder(getApplicationContext())
                            .setContentTitle(getString(R.string.app_name))
                            .setContentText(getString(R.string.newmesage))
                            .setSmallIcon(R.drawable.ic_stat_mail)
                            .build();
                    mNotificationManager.notify(001, mNotification);*/

                    Toast.makeText(getApplicationContext(), currentObject.getString("content"), Toast.LENGTH_SHORT).show();
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    };

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

        Task.execute(mPrefsGlobal.getString("owner", "none"));

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }
}
