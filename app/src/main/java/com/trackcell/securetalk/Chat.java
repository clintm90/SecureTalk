package com.trackcell.securetalk;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;

import java.io.BufferedReader;
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
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mConnectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        mPrefsGlobal = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mStorageGlobal = mPrefsGlobal.edit();

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
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
        catch(Exception e)
        {
            mKeyPairGenerator = null;
        }

        setContentView(R.layout.activity_chat);

        overridePendingTransition(R.anim.trans_left_in, R.anim.trans_left_out);

        this.setTitle(getIntent().getStringExtra("contact"));

        mMainContent = (ListView)findViewById(R.id.mainContentChat);
        mChatField = (EditText)findViewById(R.id.chatField);
        mNoMessages = (TextView)findViewById(R.id.noMessages);

        mChatField.setCursorVisible(false);

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
                    sendCurrentMessage(null);
                }
                return true;
            }
        });

        mMainContent.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                input();
            }
        });

        mMainContent.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, ((EnumChat)view.getTag()).Title);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
                return true;
            }
        });

        /*ChatListAdapter mChatListAdapter = new ChatListAdapter(this, CHATLIST);

        mChatListAdapter.add(new EnumChat(getApplicationContext(), false, false, 1416867012, "salut", "Salut, comment ca va ?"));
        mChatListAdapter.add(new EnumChat(getApplicationContext(), true, false, 0, "salut", "Très bien et toi :)"));
        mChatListAdapter.add(new EnumChat(getApplicationContext(), false, false, 1416866413, "salut", "alors ta récuperer tes clés de voitures ?"));
        mChatListAdapter.add(new EnumChat(getApplicationContext(), true, false, 1416866413, "salut", "Non toujours pas !"));
        mChatListAdapter.add(new EnumChat(getApplicationContext(), false, false, 1416866413, "salut", "Ah ok"));

        mMainContent.setAdapter(mChatListAdapter);*/
    }

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
                URL mURL = new URL(Initialize.SecureTalkServer + "getMessageByID.php?sender=" + params[0] + "&recipient=" + params[1]);
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

                ChatListAdapter mChatListAdapter = new ChatListAdapter(getApplicationContext(), CHATLIST);
                mChatListAdapter.clear();

                for (int i = 0; i < mItems.getJSONArray("item").length(); i++)
                {
                    JSONObject currentObject = mItems.getJSONArray("item").getJSONObject(i);

                    mChatListAdapter.add(new EnumChat(getApplicationContext(), false, false, currentObject.getString("time"), null, RSADecrypt(currentObject.getString("content"))));
                }

                mNoMessages.setVisibility(View.INVISIBLE);

                mMainContent.setAdapter(mChatListAdapter);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onStart()
    {
        Task.execute(getIntent().getStringExtra("recipient"), mPrefsGlobal.getString("owner", "none"));
        input();
        super.onStart();
    }

    public void input()
    {
        findViewById(R.id.chatField).requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(findViewById(R.id.chatField), InputMethodManager.SHOW_IMPLICIT);
    }

    public void addMessage(MenuItem item)
    {
        ChatListAdapter mChatListAdapter = new ChatListAdapter(this, CHATLIST);
        mChatListAdapter.add(new EnumChat(getApplicationContext(), false, false, "0", "salut", "Ok d'accord"));
        mMainContent.setAdapter(mChatListAdapter);
    }

    public void sendParts(MenuItem item)
    {
        addMessage(null);

        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
//        startActivityForResult(photoPickerIntent, 1);
    }

    public void sendCurrentMessage(MenuItem item)
    {
        final ChatListAdapter mChatListAdapter = new ChatListAdapter(this, CHATLIST);
        String currentMessage = mChatField.getText().toString();
        long timestamp = System.currentTimeMillis()*1000;

        if(currentMessage.length() > 0)
        {
            try
            {
                AsyncTask<String, Void, String> SendTask = new AsyncTask<String, Void, String>()
                {
                    @Override
                    protected String doInBackground(String... params)
                    {
                        String MessageContent = "";

                        if(params[2].equals("none"))
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
                                URL mURL = new URL(Initialize.SecureTalkServer + "sendMessageByID.php" + "?message=" + MessageContent + "&sender=" + params[1] + "&recipient=" + params[2]);
                                BufferedReader reader = new BufferedReader(new InputStreamReader(mURL.openStream()));

                                while ((c = reader.readLine()) != null)
                                    rts += c;
                                return params[0];
                            }
                            catch(UnknownHostException e)
                            {
                                cancel(true);
                                return null;
                            }
                            catch(Exception e)
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
                        mChatField.setHint(R.string.sending);
                    }

                    @Override
                    protected void onCancelled()
                    {
                        NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();
                        if(netInfo == null || !netInfo.isConnectedOrConnecting())
                        {
                            setResult(RESULT_OK, new Intent().putExtra("result", 3).putExtra("error_content", getResources().getString(R.string.noconnection)));
                            finish();
                        }
                        else
                        {
                            mChatField.setError(getString(R.string.nosend));
                        }
                        mChatField.setHint(R.string.entermessage);
                    }

                    @Override
                    protected void onPostExecute(String params)
                    {
                        if(params != null)
                        {
                            mChatListAdapter.add(new EnumChat(getApplicationContext(), true, false, "Il y à quelques secondes", "salut", params));
                            mMainContent.setAdapter(mChatListAdapter);
                        }
                        else
                        {
                            setResult(RESULT_OK, new Intent().putExtra("result", 1));
                            finish();
                        }

                        mChatField.setHint(R.string.entermessage);
                    }
                }.execute(mChatField.getText().toString(), mPrefsGlobal.getString("owner", "none"), getIntent().getStringExtra("recipient"));
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            if(item != null)
            {
                setResult(RESULT_OK, new Intent().putExtra("result", -1));
                finish();
            }
        }
        else
        {
            mChatField.setError(getResources().getString(R.string.typemore));
        }
    }

    public String RSAEncrypt(String value) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException, DecoderException
    {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Hex.decodeHex(new String(getIntent().getStringExtra("public_key")).toCharArray()));

        mCipher.init(Cipher.ENCRYPT_MODE, mKeyFactory.generatePublic(spec));
        byte[] stringBytes = value.getBytes("UTF-8");
        byte[] encryptedBytes = mCipher.doFinal(stringBytes);
        return new String(Hex.encodeHex(encryptedBytes));
    }

    public String RSADecrypt(String value) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, DecoderException, NoSuchAlgorithmException, InvalidKeySpecException
    {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Hex.decodeHex(new String(mPrefsGlobal.getString("private_key", "none")).toCharArray()));

        mCipher.init(Cipher.DECRYPT_MODE, mKeyFactory.generatePrivate(spec));
        byte[] stringBytes = Hex.decodeHex(value.toCharArray());
        byte[] decryptedBytes = mCipher.doFinal(stringBytes);
        return new String(decryptedBytes,"UTF-8");
    }

    public void focusField(View view)
    {
        mMainContent.requestFocus();
    }

    @Override
    public void onBackPressed()
    {
        setResult(RESULT_OK, new Intent().putExtra("result", 1));
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.chat, menu);
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
                finish();
                return true;

            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
