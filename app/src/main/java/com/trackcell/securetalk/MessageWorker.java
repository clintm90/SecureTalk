package com.trackcell.securetalk;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.map.DefaultedMap;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.EOFException;
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
    private MediaPlayer mPlayer;
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

        mSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
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
                    String Hash = new String(Hex.encodeHex(DigestUtils.md5(currentObject.get("sender").toString().concat("_").concat(currentObject.get("content").toString()))));
                    if(!MessageList.get(Hash))
                    {
                        //EventBus.getDefault().post(msg.obj.toString());
                        CallNotification(currentObject.get("sender").toString());
                        MessageList.put(Hash, true);
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        private void CallNotification(String message)
        {
            Intent intent = new Intent(getApplicationContext(), Chat.class);
            intent.putExtra("contact", message);
            intent.putExtra("public_key", message);
            intent.putExtra("recipient", message);
            
            final PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);

            Notification.Builder mNotification = new Notification.Builder(getApplicationContext());
            mNotification.setSmallIcon(R.drawable.ic_stat_message);
            mNotification.setContentTitle(getString(R.string.securetalkmessage));
            mNotification.setContentText(getString(R.string.newmesage));
            mNotification.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
            mNotification.setSound(mSoundUri);
            mNotification.setContentIntent(mPendingIntent);

            mNotificationManager.notify(message.hashCode(), mNotification.build());
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
            catch(EOFException e)
            {
                return "null,null";
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
