// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: braces fieldsfirst space lnc

package com.trackcell.securetalk;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
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
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

public class Main extends Activity
{
    private ConnectivityManager mConnectivityManager;
    private PhoneNumberUtil mPhoneUtil;
    private SharedPreferences mPrefs;
    private SharedPreferences mPrefsGlobal;
    private android.content.SharedPreferences.Editor mStorage;
    private android.content.SharedPreferences.Editor mStorageGlobal;
    private TelephonyManager mTelephonyManager;

    private KeyPair mKP;
    private KeyFactory mKeyFactory;
    private KeyPairGenerator mKeyPairGenerator;
    private Cipher mCipher;

    private final ArrayList<EnumContact> CONTACTLIST = new ArrayList<EnumContact>();

    public ListView mMainContent;
    public TextView mWelcomeLabel;

    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        mConnectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        mPhoneUtil = PhoneNumberUtil.getInstance();

        mPrefsGlobal = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mStorageGlobal = mPrefsGlobal.edit();

        mPrefs = getSharedPreferences("securetalk_elements", MODE_APPEND);
        mStorage = mPrefs.edit();

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
        {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        try
        {
            mCipher = Cipher.getInstance("RSA");
            mKeyPairGenerator = KeyPairGenerator.getInstance("RSA");
            mKeyPairGenerator.initialize(512);
            mKeyFactory = KeyFactory.getInstance("RSA");
        }
        catch (Exception exception)
        {
            mKeyPairGenerator = null;
        }

        InitUser(false);

        setContentView(R.layout.activity_main);

        mMainContent = (ListView)findViewById(R.id.mainContent);
        mWelcomeLabel = (TextView)findViewById(R.id.welcomeLabel);

        mMainContent.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                GotoChat(view);
            }
        });

        mMainContent.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                return DeleteContact(((EnumContact)view.getTag()).ID);
            }
        });
    }

    public static void LoadGravatar(final Context context, final ImageView imageView, String id, final boolean isPhotoBW)
    {
        final AsyncTask<String, Void, Bitmap> GravatarTask = new AsyncTask<String, Void, Bitmap>()
        {
            @Override
            protected Bitmap doInBackground(String... params)
            {
                HttpURLConnection httpURLConnection = null;

                try
                {
                    httpURLConnection = (HttpURLConnection)(new URL("http://www.gravatar.com/avatar/" + params[0] + "?s=80&d=mm")).openConnection();
                    return BitmapFactory.decodeStream(httpURLConnection.getInputStream(), null, new android.graphics.BitmapFactory.Options());
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
                finally
                {
                    httpURLConnection.disconnect();
                }
            }

            protected void onPostExecute(Bitmap bitmap)
            {
                if (bitmap != null)
                {
                    imageView.setImageBitmap(bitmap);
                    if (isPhotoBW)
                    {
                        Main.BWImageView(imageView, 50F);
                    }
                }
                else
                {
                    imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_photo_none));
                }
            }
        };
        GravatarTask.execute(id);

        /*(new AsyncTask(s, imageview, flag, context) {

            final Context val$context;
            final String val$id;
            final ImageView val$imageView;
            final boolean val$isPhotoBW;

            protected transient Bitmap doInBackground(String as[])
            {
                String s1;
                HttpURLConnection httpurlconnection;
                s1 = (new StringBuilder()).append("http://www.gravatar.com/avatar/").append(id).append("?s=80&d=mm").toString();
                httpurlconnection = null;
                Bitmap bitmap;
                httpurlconnection = (HttpURLConnection)(new URL(s1)).openConnection();
                bitmap = BitmapFactory.decodeStream(httpurlconnection.getInputStream(), null, new android.graphics.BitmapFactory.Options());
                httpurlconnection.disconnect();
                return bitmap;
                Exception exception1;
                exception1;
                cancel(true);
                httpurlconnection.disconnect();
                return null;
                Exception exception;
                exception;
                httpurlconnection.disconnect();
                throw exception;
            }

            protected volatile Object doInBackground(Object aobj[])
            {
                return doInBackground((String[])aobj);
            }

            protected void onPostExecute(Bitmap bitmap)
            {
                if (bitmap != null)
                {
                    imageView.setImageBitmap(bitmap);
                    if (isPhotoBW)
                    {
                        Main.BWImageView(imageView, 50F);
                    }
                    return;
                } else
                {
                    imageView.setImageDrawable(context.getResources().getDrawable(0x7f02001c));
                    return;
                }
            }

            protected volatile void onPostExecute(Object obj)
            {
                onPostExecute((Bitmap)obj);
            }


            {
                id = s;
                imageView = imageview;
                isPhotoBW = flag;
                context = context1;
                super();
            }
        }).execute(new String[] {
                s
        });*/
    }

    public static void BWImageView(ImageView imageView, float level)
    {
        imageView.setColorFilter(new ColorMatrixColorFilter(new float[]
                {
                0.33F, 0.33F, 0.33F, 0.0F, level, 0.33F, 0.33F, 0.33F, 0.0F, level,
                0.33F, 0.33F, 0.33F, 0.0F, level, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F
        }));
    }

    public boolean AutorizeActivityLaunch(Intent intent, int i)
    {
        if(!mPrefsGlobal.getString("owner", "none").equals("none"))
        {
            NetworkInfo networkinfo = mConnectivityManager.getActiveNetworkInfo();
            if(networkinfo != null && networkinfo.isConnectedOrConnecting())
            {
                startActivityForResult(intent, i);
                return true;
            }
            else
            {
                mWelcomeLabel.setText(getString(R.string.noconnection));
                mWelcomeLabel.setBackgroundColor(getResources().getColor(R.color.red));
                return false;
            }
        }
        else
        {
            recreate();
            return false;
        }
    }

    public boolean DeleteContact(final String id)
    {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        try
        {
            alertDialog.setTitle(new JSONObject(mPrefs.getString(id, "none")).getString("name"));
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        alertDialog.setMessage(getString(R.string.wouldelete));
        alertDialog.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                mStorage.remove(id);
                mStorage.apply();
                Populate();
            }
        });
        alertDialog.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
            }
        });
        alertDialog.show();
        return false;
    }

    public Drawable DrawableFromURL(String s)
    {
        BitmapDrawable bitmapdrawable;
        try
        {
            HttpURLConnection httpurlconnection = (HttpURLConnection)(new URL(s)).openConnection();
            httpurlconnection.setRequestMethod("GET");
            httpurlconnection.setReadTimeout(10000);
            httpurlconnection.setConnectTimeout(15000);
            httpurlconnection.setDoOutput(true);
            httpurlconnection.connect();
            bitmapdrawable = new BitmapDrawable(BitmapFactory.decodeStream(httpurlconnection.getInputStream()));
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
            return null;
        }
        return bitmapdrawable;
    }

    public void GotoChat(View view)
    {
        Intent intent = new Intent(this, Chat.class);
        intent.putExtra("contact", ((EnumContact)view.getTag()).Name);
        intent.putExtra("public_key", ((EnumContact)view.getTag()).PublicKey);
        intent.putExtra("recipient", ((EnumContact)view.getTag()).ID);
        AutorizeActivityLaunch(intent, 1);
    }

    public void GotoParameters(MenuItem menuitem)
    {
        AutorizeActivityLaunch(new Intent(this, Parameters.class), 1);
    }

    public void GotoProfile(MenuItem menuitem)
    {
        Intent intent = new Intent(this, Profile.class);
        intent.putExtra("contactId", mPrefsGlobal.getString("owner", "none"));
        AutorizeActivityLaunch(intent, 1);
    }

    public void GotoAbout(MenuItem menuItem)
    {
        Intent intent = new Intent(this, About.class);
        AutorizeActivityLaunch(intent, 1);
    }

    public void GotoSearch(MenuItem menuitem)
    {
        AutorizeActivityLaunch(new Intent(this, Search.class), 1);
    }

    public void InitFirstLaunch(View view)
    {
        //setContentView(0x7f030002);
        mStorageGlobal.putString("first_launch", "ok");
        mStorageGlobal.apply();
        Populate();
    }

    public void InitUser(boolean registeringError)
    {
        final ProgressDialog mDialog = ProgressDialog.show(this, "", getString(R.string.register));
        //mDialog.setProgressStyle(R.layout.model_register);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        mDialog.setCancelable(false);
        mDialog.show();

        final AsyncTask<Object, Void, String[]> MainTask = new AsyncTask<Object, Void, String[]>()
        {
            @Override
            protected String[] doInBackground(Object... params)
            {
                try
                {
                    String rts = "", c;
                    URL mURL = new URL(Initialize.SecureTalkServer + "registerUserByID.php?id="+ params[1].toString() + "&name=" + params[4].toString() + "&description=description" + "&public_key=" + params[2].toString() + "&put=" + params[0].toString());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(mURL.openStream()));

                    while ((c = reader.readLine()) != null)
                        rts += c;

                    String[] toReturn = new String[5];
                    toReturn[0] = params[1].toString();
                    toReturn[1] = params[2].toString();
                    toReturn[2] = params[3].toString();
                    toReturn[3] = params[4].toString();
                    toReturn[4] = rts;
                    return toReturn;
                }
                catch (UnknownHostException e)
                {
                    NetworkInfo networkinfo = mConnectivityManager.getActiveNetworkInfo();
                    if (networkinfo == null || !networkinfo.isConnectedOrConnecting())
                    {
                        cancel(true);
                    }
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
            protected void onCancelled()
            {
                mDialog.dismiss();
            }

            @Override
            protected void onPostExecute(String[] value)
            {
                mDialog.dismiss();
                try
                {
                    InitUser(true);
                    mStorageGlobal.putBoolean("initialized", true);
                    mStorageGlobal.putString("owner", value[0]);
                    mStorageGlobal.putString("public_key", value[1]);
                    mStorageGlobal.putString("private_key", value[2]);
                    mStorageGlobal.putString("name", value[3]);
                    mStorageGlobal.apply();
                }
                catch (Exception e)
                {
                    Toast.makeText(getApplicationContext(), mPrefsGlobal.getAll().toString(), Toast.LENGTH_LONG).show();
                }
            }
        };

        if (mPrefsGlobal.getBoolean("initialized", false) && !mPrefsGlobal.getString("owner", "none").equals("none") && !mPrefsGlobal.getString("private_key", "none").equals("none"))
        {
            MainTask.execute(false, mPrefsGlobal.getString("owner", "none"), mPrefsGlobal.getString("public_key", "none"), mPrefsGlobal.getString("private_key", "none"), mPrefsGlobal.getString("name", "none"));
        }
        else
        {
            try
            {
                mKP = mKeyPairGenerator.generateKeyPair();
                final Account aaccount[] = AccountManager.get(getApplicationContext()).getAccountsByType("com.google");
                final String mAccountsMailID = new String(Hex.encodeHex(DigestUtils.md5(aaccount[0].name)));
                if (mAccountsMailID.length() > 0 && aaccount[0].name.contains("@"))
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    final View modelRegister = getLayoutInflater().inflate(R.layout.model_register, null);

                    if(registeringError)
                        modelRegister.findViewById(R.id.model_register_error).setVisibility(View.VISIBLE);

                    builder.setView(modelRegister);
                    builder.setCancelable(false);
                    builder.setNegativeButton(getString(R.string.refuse), new android.content.DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialoginterface, int i)
                        {
                            finish();
                        }
                    });

                    builder.setPositiveButton(getString(R.string.valid), new android.content.DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialoginterface, int i)
                        {
                            String Name = ((EditText)modelRegister.findViewById(R.id.model_register_name)).getText().toString();
                            String Mail = ((EditText)modelRegister.findViewById(R.id.model_register_mail)).getText().toString();
                            MainTask.execute(true, mAccountsMailID, new String(Hex.encodeHex(mKP.getPublic().getEncoded())), new String(Hex.encodeHex(mKP.getPrivate().getEncoded())), Name);
                        }
                    });

                    builder.create();
                    builder.show();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        /*final AsyncTask MainTask = new AsyncTask() {

            private String cancelString;
            final Main this$0;
            final ProgressDialog val$mDialog;

            protected volatile Object doInBackground(Object aobj1[])
            {
                return doInBackground(aobj1);
            }

            protected transient String[] doInBackground(Object aobj1[])
            {
                String as[];
                String s;
                BufferedReader bufferedreader;
                String s1;
                try
                {
                    as = new String[3];
                }
                catch (UnknownHostException unknownhostexception)
                {
                    NetworkInfo networkinfo = mConnectivityManager.getActiveNetworkInfo();
                    if (networkinfo == null || !networkinfo.isConnectedOrConnecting())
                    {
                        cancel(true);
                    }
                    return null;
                }
                catch (Exception exception1)
                {
                    exception1.printStackTrace();
                    cancel(true);
                    return null;
                }
                s = "";
                bufferedreader = new BufferedReader(new InputStreamReader((new URL((new StringBuilder()).append(Initialize.SecureTalkServer).append("registerUserByID.php?id=").append(aobj1[1].toString()).append("&public").append(aobj1[2].toString()).toString())).openStream()));
                _L1:
                s1 = bufferedreader.readLine();
                if (s1 == null)
                {
                    break MISSING_BLOCK_LABEL_117;
                }
                s = (new StringBuilder()).append(s).append(s1).toString();
                goto _L1
                as[0] = aobj1[1].toString();
                as[1] = aobj1[2].toString();
                as[2] = aobj1[3].toString();
                return as;
            }

            protected void onCancelled()
            {
                mWelcomeLabel.setText(0x7f080012);
                mWelcomeLabel.setBackgroundColor(getResources().getColor(0x7f06000b));
                mDialog.dismiss();
            }

            protected volatile void onPostExecute(Object obj)
            {
                onPostExecute((String[])obj);
            }

            protected void onPostExecute(String as[])
            {
                mDialog.dismiss();
                try
                {
                    mStorageGlobal.putBoolean("initialized", true);
                    mStorageGlobal.putString("owner", as[0]);
                    mStorageGlobal.putString("public_key", as[1]);
                    mStorageGlobal.putString("private_key", as[2]);
                    mStorageGlobal.apply();
                    return;
                }
                catch (Exception exception1)
                {
                    Toast.makeText(getApplicationContext(), mPrefsGlobal.getAll().toString(), 0).show();
                }
            }

            protected void onPreExecute()
            {
                mDialog.show();
            }
        };

        if (mPrefsGlobal.getBoolean("initialized", false) && !mPrefsGlobal.getString("owner", "none").equals("none") && !mPrefsGlobal.getString("private_key", "none").equals("none"))
        {
            Object aobj[] = new Object[4];
            aobj[0] = Boolean.valueOf(true);
            aobj[1] = mPrefsGlobal.getString("owner", "none");
            aobj[2] = mPrefsGlobal.getString("public_key", "none");
            aobj[3] = mPrefsGlobal.getString("private_key", "none");
            MainTask.execute(aobj);
            return;
        }

        try
        {
            mKP = mKeyPairGenerator.generateKeyPair();
            Account aaccount[] = AccountManager.get(getApplicationContext()).getAccountsByType("com.google");
            final String mAccountsMailID = new String(Hex.encodeHex(DigestUtils.md5(aaccount[0].name)));
            if (mAccountsMailID.length() > 0 && aaccount[0].name.contains("@"))
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setView(getLayoutInflater().inflate(R.layout.model_register, null));
                builder.setCancelable(false);
                builder.setNegativeButton(getString(R.string.refuse), new android.content.DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialoginterface, int i)
                    {
                        finish();
                    }
                });

                builder.setPositiveButton(getString(R.string.valid), new android.content.DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialoginterface, int i)
                    {
                        AsyncTask asynctask = MainTask;
                        Object aobj1[] = new Object[4];
                        aobj1[0] = Boolean.valueOf(false);
                        aobj1[1] = mAccountsMailID;
                        aobj1[2] = new String(Hex.encodeHex(mKP.getPublic().getEncoded()));
                        aobj1[3] = new String(Hex.encodeHex(mKP.getPrivate().getEncoded()));
                        asynctask.execute(aobj1);
                    }
                });

                builder.create();
                builder.show();
            }
        }
        catch (Exception exception)
        {
            Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_LONG).show();
            exception.printStackTrace();
            return;
        }
        Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_LONG).show();*/
    }

    public void Populate()
    {
        final ContactListAdapter mContactListAdapter = new ContactListAdapter(this, CONTACTLIST);

        mContactListAdapter.clear();

        mContactListAdapter.add((new EnumContact(getApplicationContext(), "785b86f1ea73414d6c0493b2411421ba", "Clint Mourlevat", "Pas de nouveau message", mPrefsGlobal.getString("public_key", "none"), false)).bwPhoto());

        for(Map.Entry<String, ?> current : mPrefs.getAll().entrySet())
        {
            try
            {
                JSONObject json = new JSONObject(current.getValue().toString());
                mContactListAdapter.add(new EnumContact(getApplicationContext(), current.getKey(), json.getString("name"), "Comment ca marche ?", json.getString("public_key"), false).bwPhoto());
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }

        mMainContent.setAdapter(mContactListAdapter);

        if (mPrefs.getAll().values().size() > 0)
        {
            findViewById(R.id.noContact).setVisibility(View.INVISIBLE);
        }
        else
        {
            findViewById(R.id.noContact).setVisibility(View.VISIBLE);
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

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch(data.getExtras().getInt("result"))
        {
            case 0:
                if(data.getExtras().getBoolean("callback"))
                {
                    EnumContact enumcontact = (EnumContact)data.getExtras().getSerializable("contact");
                    mStorage.putString(enumcontact.ID, "{'id':'"+enumcontact.ID+"', 'name':'"+enumcontact.Name+"', 'public_key':'"+enumcontact.PublicKey+"'}");
                    mStorage.apply();
                }
            break;

            case 1:
                try
                {
                    overridePendingTransition(R.anim.trans_right_in, R.anim.trans_right_out);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            break;

            case 2:
                recreate();
                break;

            case 3:
                mWelcomeLabel.setText(data.getExtras().getString("error_content"));
                break;

            default:
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem menuitem)
    {
        if (menuitem.getItemId() == R.string.action_settings)
        {
            return true;
        }
        else
        {
            return super.onOptionsItemSelected(menuitem);
        }
    }

    protected void onResume()
    {
        mWelcomeLabel.setText(getString(R.string.appwelcome));
        mWelcomeLabel.setBackgroundColor(getResources().getColor(R.color.grey));
        super.onResume();
    }

    protected void onStart()
    {
        Populate();
        super.onStart();
    }

    protected void onStop()
    {
        //stopService(Initialize.mMessageWorkerService);
        super.onStop();
    }
}