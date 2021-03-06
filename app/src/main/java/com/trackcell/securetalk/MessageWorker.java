package com.trackcell.securetalk;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.apache.commons.collections4.map.DefaultedMap;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;

public class MessageWorker extends Service
{
    private Handler mHandler;
    private NotificationManager mNotificationManager;
    private SharedPreferences mPrefsGlobal;
    private Timer mTimer;
    private Ringtone mRingtone;
    private Uri mSoundUri;
    
    private DefaultedMap<String,Boolean> MessageList = new DefaultedMap<String,Boolean>(false);

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

        Toast.makeText(getApplicationContext(), "service up", Toast.LENGTH_SHORT).show();
        
        mSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        mRingtone = RingtoneManager.getRingtone(getApplicationContext(), mSoundUri);
        
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new MainLoop(), Initialize.InitTime, Initialize.ServiceRefreshTime);

        return START_STICKY;
    }

    private final Handler ToastHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            try
            {
                JSONObject mRoot = new JSONObject(msg.obj.toString());
                JSONObject mItems = mRoot.getJSONObject("result");
                for (int i = 0; i < mItems.getJSONArray("item").length(); i++)
                {
                    JSONObject currentObject = mItems.getJSONArray("item").getJSONObject(i);
                    String Hash = currentObject.get("hash").toString();
                    if(!MessageList.get(Hash))
                    {
                        if(!currentObject.get("sender").toString().equals(mPrefsGlobal.getString("owner", "none")))
                        {
                            if(Initialize.ActivityForeground)
                            {
                                EventBus.getDefault().post(msg.obj.toString());
                                mRingtone = RingtoneManager.getRingtone(getApplicationContext(), mSoundUri);
                                mRingtone.play();
                            }
                            else
                            {
                                CallNotification(Hash, currentObject.toString());
                            }
                        }
                        MessageList.put(Hash, true);
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        private void CallNotification(final String hash, final String message)
        {
            AsyncTask<String,Void,Object[]> NotificationTask = new AsyncTask<String, Void, Object[]>()
            {
                @Override
                protected Object[] doInBackground(String... params)
                {
                    try
                    {
                        Object[] mRTS = new Object[5];
                        JSONObject json = new JSONObject(params[0]);
                        String sender = json.get("sender").toString();
                        
                        HttpURLConnection httpURLConnection = (HttpURLConnection) (new URL("http://www.gravatar.com/avatar/" + sender + "?s=80&d=mm")).openConnection();
                        Bitmap bmp = BitmapFactory.decodeStream(httpURLConnection.getInputStream(), null, new BitmapFactory.Options());
                        httpURLConnection.disconnect();
                        
                        String rts = "", c;
                        URL mURL = new URL(Initialize.SecureTalkServer + "getUserInfoByID.php?id=" + sender);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(mURL.openStream()));

                        while ((c = reader.readLine()) != null)
                        {
                            rts += c;
                        }
                        mRTS[0] = bmp;
                        mRTS[1] = rts;
                        mRTS[2] = sender;
                        return mRTS;
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

                @Override
                protected void onPostExecute(Object[] input)
                {
                    try
                    {
                        JSONObject json = new JSONObject(input[1].toString());
                        String name = json.getString("name");
                        String public_key = json.getString("public_key");

                        Intent intent = new Intent(getApplicationContext(), Chat.class);
                        intent.putExtra("contact", name);
                        intent.putExtra("public_key", public_key);
                        intent.putExtra("recipient", input[2].toString());

                        final PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);

                        NotificationCompat.Builder mNotification = new NotificationCompat.Builder(getApplicationContext());
                        mNotification.setSmallIcon(R.drawable.ic_stat_notification_sms);
                        mNotification.setContentTitle(name);
                        mNotification.setAutoCancel(true);
                        mNotification.setTicker(String.format(getString(R.string.newmesageby), name));
                        mNotification.setGroup(getString(R.string.app_name));
                        mNotification.setContentText(getString(R.string.newmesage));
                        mNotification.setLargeIcon((Bitmap) input[0]);
                        mNotification.setSound(mSoundUri);
                        mNotification.setContentIntent(mPendingIntent);

                        mNotificationManager.notify(hash.hashCode(), mNotification.build());
                    }
                    catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }
            };
            NotificationTask.execute(message);
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
                URL mURL = new URL(Initialize.SecureTalkServer + "getMessageByID.php?sender=%25&recipient=" + params[0]);
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
                Message msg = Message.obtain();
                msg.obj = input;
                ToastHandler.sendMessage(msg);
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
        mTimer.cancel();
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    private void runOnUiThread(Runnable runnable)
    {
        mHandler.post(runnable);
    }
}