package com.trackcell.securetalk;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.github.liveflow.AsyncWorker;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

public class Chat extends Activity
{
    private ClipboardManager mClipBoard;
    private ListView mMainContent;
    private EditText mChatField;
    private TextView mNoMessages;
    private final static List<EnumChat> CHATLIST = new ArrayList<EnumChat>();

    private SharedPreferences mPrefsGlobal;
    private SharedPreferences.Editor mStorageGlobal;

    private ConnectivityManager mConnectivityManager;
    private KeyPairGenerator mKeyPairGenerator;
    private KeyPair mKP;
    private Cipher mCipher;
    private KeyFactory mKeyFactory;
    private ChatListAdapter mChatListAdapter;
    
    private Timer mTimer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mPrefsGlobal = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mStorageGlobal = mPrefsGlobal.edit();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
        {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        try
        {
            mCipher = Cipher.getInstance("RSA");
            mKeyPairGenerator = KeyPairGenerator.getInstance("RSA");
            mKeyPairGenerator.initialize(512);
            mKP = mKeyPairGenerator.generateKeyPair();
            mKeyFactory = KeyFactory.getInstance("RSA");
        }
        catch (Exception e)
        {
            mKeyPairGenerator = null;
        }
        
        if(mPrefsGlobal.getString("public_key", "none").equals("none"))
        {
            setResult(RESULT_OK, new Intent().putExtra("result", 1));
            CloseApp();
        }

        mChatListAdapter = new ChatListAdapter(this, CHATLIST);

        setContentView(com.trackcell.securetalk.R.layout.activity_chat);

        overridePendingTransition(com.trackcell.securetalk.R.anim.trans_left_in, com.trackcell.securetalk.R.anim.trans_left_out);

        this.setTitle(getIntent().getStringExtra("contact"));

        mMainContent = (ListView) findViewById(com.trackcell.securetalk.R.id.mainContentChat);
        mChatField = (EditText) findViewById(com.trackcell.securetalk.R.id.chatField);
        mNoMessages = (TextView) findViewById(com.trackcell.securetalk.R.id.noMessages);

        mChatField.setCursorVisible(false);
        mChatField.setText(Html.fromHtml("Salut &#9786; &#8252; &#128515; &#x1f608; &#10084; &#128527;"), TextView.BufferType.SPANNABLE);

        //mMainContent.setAdapter(null);
        
        mTimer.scheduleAtFixedRate(MainLoop, Initialize.InitTime, Initialize.ActivityRefreshTime);

        mChatField.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mMainContent.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
            }
        });

        mChatField.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_SEND)
                {
                    SendCurrentMessage(null);
                }
                return true;
            }
        });

        mMainContent.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                Input();
            }
        });

        mMainContent.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_STREAM, ((EnumChat) view.getTag()).Photo);
                sendIntent.setType("image/jpeg");
                startActivity(Intent.createChooser(sendIntent, "Share Image"));
                return true;
            }
        });
    }

    public void Input()
    {
        //mChatField.requestFocus();
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(mChatField, InputMethodManager.SHOW_FORCED);
    }

    public void AddMessage(MenuItem item)
    {
        mChatListAdapter.add(new EnumChat(getApplicationContext(), false, false, 1418342400000L, "salut", Initialize.GenerateRandomWords()));
        mMainContent.setAdapter(mChatListAdapter);
    }

    public void SendParts(MenuItem item)
    {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, 1);
    }

    public void SendCurrentMessage(MenuItem item)
    {
        String currentMessage = mChatField.getText().toString();
        long timestamp = System.currentTimeMillis() * 1000;

        if (currentMessage.length() > 0)
        {
            try
            {
                AsyncTask<String, Void, String> SendTask = new AsyncTask<String, Void, String>()
                {
                    @Override
                    protected String doInBackground(String... params)
                    {
                        Thread.currentThread().setName("Chat_SendCurrentMessage");
                        String MessageContent = "";

                        if (params[2].equals("none"))
                        {
                            cancel(true);
                            return null;
                        }
                        else
                        {
                            try
                            {
                                MessageContent = RSAEncrypt(params[0]);

                                String rts = "", c;
                                URL mURL = new URL(Initialize.SecureTalkServer + "sendMessageByID.php?message=" + MessageContent + "&sender=" + params[1] + "&recipient=" + params[2]);
                                BufferedReader reader = new BufferedReader(new InputStreamReader(mURL.openStream()));

                                while ((c = reader.readLine()) != null)
                                {
                                    rts += c;
                                }

                                JSONObject returnValue = new JSONObject(rts);
                                if (returnValue.getInt("response") == 1)
                                {
                                    return params[0];
                                }
                                else
                                {
                                    cancel(true);
                                    return null;
                                }
                            }
                            catch (UnknownHostException e)
                            {
                                cancel(true);
                                return null;
                            }
                            catch(ArrayIndexOutOfBoundsException e)
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
                    }

                    @Override
                    protected void onPreExecute()
                    {
                        mChatField.setText("");
                        mChatField.setHint(com.trackcell.securetalk.R.string.sending);
                    }

                    @Override
                    protected void onCancelled()
                    {
                        NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();
                        if (netInfo == null || !netInfo.isConnectedOrConnecting())
                        {
                            setResult(RESULT_OK, new Intent().putExtra("result", 3).putExtra("error_content", getResources().getString(com.trackcell.securetalk.R.string.noconnection)));
                            CloseApp();
                        }
                        else
                        {
                            mChatField.setError(getString(com.trackcell.securetalk.R.string.nosend));
                        }
                        mChatField.setHint(com.trackcell.securetalk.R.string.entermessage);
                    }

                    @Override
                    protected void onPostExecute(String params)
                    {
                        if (params != null)
                        {
                            mChatListAdapter.add(new EnumChat(getApplicationContext(), true, false, new Date().getTime(), "salut", params));
                            mMainContent.setAdapter(mChatListAdapter);
                            mNoMessages.setVisibility(View.INVISIBLE);
                        }
                        else
                        {
                            setResult(RESULT_OK, new Intent().putExtra("result", 1));
                            CloseApp();
                        }

                        mChatField.setHint(com.trackcell.securetalk.R.string.entermessage);
                    }
                };
                SendTask.execute(mChatField.getText().toString(), mPrefsGlobal.getString("owner", "none"), getIntent().getStringExtra("recipient"));
                try
                {
                    SendTask.get(3000, TimeUnit.MILLISECONDS);
                }
                catch(TimeoutException e)
                {
                    SendTask.cancel(true);
                    //mChatField.setError(getString(R.string.timeout));
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            if (item != null)
            {
                setResult(RESULT_OK, new Intent().putExtra("result", -1));
                CloseApp();
            }
        }
        else
        {
            mChatField.setError(getResources().getString(com.trackcell.securetalk.R.string.typemore));
        }
    }

    public String RSAEncrypt(String value) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException, DecoderException
    {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Hex.decodeHex(getIntent().getStringExtra("public_key").toCharArray()));

        mCipher.init(Cipher.ENCRYPT_MODE, mKeyFactory.generatePublic(spec));
        byte[] stringBytes = value.getBytes("UTF-8");
        byte[] encryptedBytes = mCipher.doFinal(stringBytes);
        return new String(Hex.encodeHex(encryptedBytes));
    }

    public String RSADecrypt(String value) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, DecoderException, NoSuchAlgorithmException, InvalidKeySpecException
    {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Hex.decodeHex(mPrefsGlobal.getString("private_key", "none").toCharArray()));

        mCipher.init(Cipher.DECRYPT_MODE, mKeyFactory.generatePrivate(spec));
        byte[] stringBytes = Hex.decodeHex(value.toCharArray());
        byte[] decryptedBytes = mCipher.doFinal(stringBytes);
        return new String(decryptedBytes, "UTF-8");
    }

    public void SendPhoto(final Bitmap bitmap)
    {
        AsyncWorker<Bitmap, Object[], Void> SendPhotoTask = new AsyncWorker<Bitmap, Object[], Void>()
        {
            @Override
            protected Object[] onStart(Bitmap... bitmaps)
            {
                Object[] mRTS = new Object[2];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmaps[0].compress(Bitmap.CompressFormat.JPEG, 10, baos);
                byte[] b = baos.toByteArray();
                mRTS[0] = bitmaps[0];
                mRTS[1] = Base64.encodeToString(b, Base64.DEFAULT);
                return mRTS;
            }

            @Override
            protected void onEnd(Object[] objects)
            {
                mChatListAdapter.add(new EnumChat(getApplicationContext(), true, false, 1418342400000L, "salut", "Ok d'accord").putPhoto((Bitmap) objects[0]));
                mMainContent.setAdapter(mChatListAdapter);
            }

            @Override
            protected void onCancel(Void... cancel)
            {
            }
        };
        SendPhotoTask.Run(bitmap);
    }
    
    private class GetMessageByID extends AsyncTask<String, Void, String>
    {
        protected String doInBackground(String... params)
        {
            Thread.currentThread().setName("Chat_GetMessageByID");
            
            if (params[1].equals("none"))
            {
                cancel(true);
            }

            try
            {
                String rts = "", c;
                URL mURL = new URL(Initialize.SecureTalkServer + "getMessageByID.php?sender=" + params[0] + "&recipient=" + params[1] + "&put=true");
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
            NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();
            if (netInfo == null || !netInfo.isConnectedOrConnecting())
            {
                setResult(RESULT_OK, new Intent().putExtra("result", 3).putExtra("error_content", getResources().getString(com.trackcell.securetalk.R.string.noconnection)));
                CloseApp();
            }
            else
            {
                mChatField.setError(getString(com.trackcell.securetalk.R.string.nosend));
            }
        }
        
        protected void onPostExecute(final String input)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    int i = 0;
                    try
                    {
                        JSONObject mRoot = new JSONObject(input);
                        JSONObject mItems = mRoot.getJSONObject("result");

                        //mChatListAdapter.clear();
                        
                        for (i = 0; i < mItems.getJSONArray("item").length(); i++)
                        {
                            JSONObject currentObject = mItems.getJSONArray("item").getJSONObject(i);
                            mNoMessages.setVisibility(View.INVISIBLE);
                            mChatListAdapter.add(new EnumChat(getApplicationContext(), false, false, currentObject.getLong("time") * 1000, null, RSADecrypt(currentObject.getString("content"))));
                        }

                        if (i > 0)
                        {
                            mNoMessages.setVisibility(View.INVISIBLE);
                        }

                        mMainContent.setAdapter(mChatListAdapter);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private TimerTask MainLoop = new TimerTask()
    {
        @Override
        public void run()
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Thread.currentThread().setName("Chat_MainLoop");
                    GetMessageByID CallLoopItem = new GetMessageByID();
                    CallLoopItem.execute(getIntent().getStringExtra("recipient"), mPrefsGlobal.getString("owner", "none"));
                }
            });
        }
    };

    @Override
    public void onStart()
    {
        super.onStart();
        Input();
    }

    @Override
    public void onBackPressed()
    {
        setResult(RESULT_OK, new Intent().putExtra("result", 1));
        super.onDestroy();
        super.onBackPressed();
    }
    
    /*@Override
    public void onStop()
    {
        super.onStop();
        setResult(RESULT_OK, new Intent().putExtra("result", 1));
        CloseApp();
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 1 && data != null && data.getData() != null)
        {
            Cursor mCursor = getContentResolver().query(data.getData(), new String[]{android.provider.MediaStore.Images.ImageColumns.DATA}, null, null, null);
            mCursor.moveToFirst();

            Bitmap mSelectedPhoto = BitmapFactory.decodeFile(mCursor.getString(0));

            SendPhoto(mSelectedPhoto);

            mCursor.close();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(com.trackcell.securetalk.R.menu.chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        switch (id)
        {
            case android.R.id.home:
                setResult(RESULT_OK, new Intent().putExtra("result", 1));
                CloseApp();
                return true;

            case com.trackcell.securetalk.R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void CloseApp()
    {
        MainLoop.cancel();
        mTimer.cancel();
        finish();
    }
}