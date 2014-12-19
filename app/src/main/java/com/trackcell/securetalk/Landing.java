package com.trackcell.securetalk;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;

import javax.crypto.Cipher;

public class Landing extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks
{
    private DBSecureTalk mDBSecureTalk;

    private NavigationDrawerFragment mNavigationDrawerFragment;

    private ConnectivityManager mConnectivityManager;
    private PhoneNumberUtil mPhoneUtil;
    private SharedPreferences mPrefs;
    private SharedPreferences mPrefsGlobal;
    private SharedPreferences.Editor mStorage;
    private SharedPreferences.Editor mStorageGlobal;
    private TelephonyManager mTelephonyManager;

    private KeyPair mKP;
    private KeyFactory mKeyFactory;
    private KeyPairGenerator mKeyPairGenerator;
    private Cipher mCipher;

    private final ArrayList<EnumContact> CONTACTLIST = new ArrayList<EnumContact>();

    public ListView mMainContent;
    public TextView mWelcomeLabel;

    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mDBSecureTalk = new DBSecureTalk(getApplicationContext(), "SecureTalk.db", null, 1, null);
        mDBSecureTalk.onCreate(mDBSecureTalk.getWritableDatabase());

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

        InitUser();

        setContentView(R.layout.activity_landing);

        mNavigationDrawerFragment = (NavigationDrawerFragment)getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    public void InitUser()
    {
        final Context parent = this;
        final ProgressDialog alertDialog = ProgressDialog.show(this, "", getString(R.string.register));
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        alertDialog.setCancelable(false);
        alertDialog.show();

        final Account aaccount[] = AccountManager.get(getApplicationContext()).getAccountsByType("com.google");
        final String mAccountsMailID = new String(Hex.encodeHex(DigestUtils.md5(aaccount[0].name)));
        if (mAccountsMailID.length() > 0 && aaccount[0].name.contains("@"))
        {
            AsyncTask<Object, Void, Object[]> InitUserTask = new AsyncTask<Object, Void, Object[]>()
            {
                @Override
                protected Object[] doInBackground(Object... params)
                {
                    try
                    {
                        Object[] mRTS = new Object[3];
                        String rts = "", c;
                        URL mURL = new URL(Initialize.SecureTalkServer + "registerUserByID.php?id="+ params[0].toString() + "&put=false");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(mURL.openStream()));

                        while ((c = reader.readLine()) != null)
                            rts += c;
                        mRTS[0] = rts;
                        mRTS[1] = params[1];
                        mRTS[2] = params[0];
                        return mRTS;
                    }
                    catch(UnknownHostException e)
                    {
                        NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();
                        if(netInfo == null || !netInfo.isConnectedOrConnecting())
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
                protected void onCancelled()
                {
                    alertDialog.dismiss();
                }

                @Override
                protected void onPostExecute(Object[] input)
                {
                    alertDialog.dismiss();
                    try
                    {
                        JSONObject registerValue = new JSONObject(input[0].toString());
                        String mPublicKey = new String(Hex.encodeHex(mKP.getPublic().getEncoded()));
                        String mPrivateKey = new String(Hex.encodeHex(mKP.getPrivate().getEncoded()));
                        if (registerValue.getInt("response") == 1)
                        {
                            mStorageGlobal.putBoolean("initialized", true);
                            mStorageGlobal.putString("owner", input[2].toString());
                            /*if(registerValue.getString("public_key") != mPublicKey)
                            {
                                //Update mPublicKey to database
                                mStorageGlobal.putString("public_key", mPublicKey);
                                mStorageGlobal.putString("private_key", mPrivateKey);
                            }*/
                            mStorageGlobal.apply();
                        }
                        else
                        {
                            NewUser(aaccount[0].name, false);
                        }
                    }
                    catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }

                private void NewUser(final String name, boolean registeringError)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(parent);

                    final View modelRegister = getLayoutInflater().inflate(R.layout.model_register, null);

                    //((EditText)modelRegister.findViewById(R.id.model_register_name));
                    ((EditText)modelRegister.findViewById(R.id.model_register_mail)).setText(name);

                    if(registeringError)
                        //modelRegister.findViewById(R.id.model_register_error).setVisibility(View.VISIBLE);
                        ((EditText)modelRegister.findViewById(R.id.model_register_name)).setError(getString(R.string.typemore));

                    builder.setView(modelRegister);
                    builder.setCancelable(false);
                    builder.setNegativeButton(getString(R.string.refuse), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialoginterface, int i)
                        {
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(0);
                        }
                    });

                    builder.setPositiveButton(getString(R.string.valid), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialoginterface, int i)
                        {
                            String Name = ((EditText)modelRegister.findViewById(R.id.model_register_name)).getText().toString();
                            String Mail = ((EditText)modelRegister.findViewById(R.id.model_register_mail)).getText().toString();
                            if(Name.trim().equals(""))
                            {
                                NewUser(name, true);
                            }
                            else
                            {
                                //MainTask.execute(true, mAccountsMailID, new String(Hex.encodeHex(mKP.getPublic().getEncoded())), new String(Hex.encodeHex(mKP.getPrivate().getEncoded())), Name);
                            }
                        }
                    });

                    builder.create();
                    builder.show();
                }
            };
            if (mPrefsGlobal.getBoolean("initialized", false) && !mPrefsGlobal.getString("owner", "none").equals("none") && !mPrefsGlobal.getString("private_key", "none").equals("none"))
            {
                InitUserTask.execute(mPrefsGlobal.getString("owner", "none"), mKP);
            }
            else
            {
                mKP = mKeyPairGenerator.generateKeyPair();
                InitUserTask.execute(mAccountsMailID, mKP);
            }
        }
        else
        {
            AlertDialog.Builder alertDialogNewAccount = new AlertDialog.Builder(parent);
            alertDialogNewAccount.setTitle("Nouveau compte");
            alertDialogNewAccount.setMessage("Vous devez disposez d'un compte Google pour utiliser SecureTalk.\nVoulez-vous en créer un ?");
            alertDialogNewAccount.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT));
                }
            });
            alertDialogNewAccount.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }
            });
            alertDialogNewAccount.create();
            alertDialogNewAccount.show();
        }
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
                    return BitmapFactory.decodeStream(httpURLConnection.getInputStream(), null, new BitmapFactory.Options());
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
                        Landing.BWImageView(true, imageView, 50F);
                    }
                }
                else
                {
                    imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_photo_none));
                }
            }
        };
        GravatarTask.execute(id);

        //region old gravatar task
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
        //endregion
    }

    public static void BWImageView(boolean execute, ImageView imageView, float level)
    {
        if(execute)
        {
            imageView.setColorFilter(new ColorMatrixColorFilter(new float[]
                    {
                            0.33F, 0.33F, 0.33F, 0.0F, level, 0.33F, 0.33F, 0.33F, 0.0F, level,
                            0.33F, 0.33F, 0.33F, 0.0F, level, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F
                    }));
        }
        else
        {
            imageView.setColorFilter(null);
        }
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

    //region DeleteContact()
    /*public boolean DeleteContact(View view, final String id)
    {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        try
        {
            JSONObject json = new JSONObject(mPrefs.getString(id, "none"));
            alertDialog.setTitle(json.getString("name"));
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        alertDialog.setItems(new String[]{getString(R.string.showcontact), getString(R.string.deletecontact)}, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                switch(which)
                {
                    case 0:
                        //Populate();
                        break;

                    case 1:
                        mStorage.remove(id);
                        mStorage.apply();
                        //Populate();
                        break;
                }
            }
        });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                Populate();
            }
        });
        alertDialog.show();
        return false;
    }*/
    //endregion

    //region GotoChat()
    /*public void GotoChat(View view)
    {
        Intent intent = new Intent(this, Chat.class);
        intent.putExtra("contact", ((EnumContact)view.getTag()).Name);
        intent.putExtra("public_key", ((EnumContact)view.getTag()).PublicKey);
        intent.putExtra("recipient", ((EnumContact)view.getTag()).ID);
        AutorizeActivityLaunch(intent, 1);
    }*/
    //endregion

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
        //Populate();
    }

    //region old init user
    /*public void InitUser(boolean registeringError, final String registeringErrorContent)
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
                Thread.currentThread().setName("InitUser");

                try
                {
                    String rts = "", c;
                    URL mURL = new URL(Initialize.SecureTalkServer + "registerUserByID.php?id=" + params[1].toString() + "&name=" + params[4].toString() + "&description=description" + "&public_key=" + params[2].toString() + "&put=" + params[0].toString());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(mURL.openStream()));

                    while ((c = reader.readLine()) != null)
                        rts += c;

                    String[] toReturn = new String[10];
                    toReturn[0] = params[1].toString();
                    toReturn[1] = params[2].toString();
                    toReturn[2] = params[3].toString();
                    toReturn[3] = params[4].toString();
                    toReturn[4] = rts;
                    toReturn[5] = params[0].toString();
                    return toReturn;
                }
                catch (UnknownHostException e)
                {
                    NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();
                    if (netInfo == null || !netInfo.isConnectedOrConnecting())
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
            protected void onCancelled()
            {
                mDialog.dismiss();
                mWelcomeLabel.setText(registeringErrorContent);
                mWelcomeLabel.setBackgroundColor(getResources().getColor(R.color.red));
            }

            @Override
            protected void onPostExecute(String[] value)
            {
                mDialog.dismiss();
                try
                {
                    JSONObject registerValue = new JSONObject(value[4]);

                    if (registerValue.getInt("response") == 1)
                    {
                        mStorageGlobal.putBoolean("initialized", true);
                        mStorageGlobal.putString("owner", value[0]);
                        mStorageGlobal.putString("public_key", value[1]);
                        mStorageGlobal.putString("private_key", value[2]);
                        mStorageGlobal.putString("name", value[3]);
                        mStorageGlobal.apply();
                    }
                    else
                    {
                        InitUser(true, getString(R.string.userexist));
                    }
                }
                catch (Exception e)
                {
                    InitUser(true, getString(R.string.cancel));
                }
            }
        };

        if (mPrefsGlobal.getBoolean("initialized", false) && !mPrefsGlobal.getString("owner", "none").equals("none") && !mPrefsGlobal.getString("private_key", "none").equals("none"))
        {
            MainTask.execute(registeringError, mPrefsGlobal.getString("owner", "none"), mPrefsGlobal.getString("public_key", "none"), mPrefsGlobal.getString("private_key", "none"), mPrefsGlobal.getString("name", "none"));
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

                    //((EditText)modelRegister.findViewById(R.id.model_register_name));
                    ((EditText) modelRegister.findViewById(R.id.model_register_mail)).setText(aaccount[0].name);

                    if (registeringError)
                        modelRegister.findViewById(R.id.model_register_error).setVisibility(View.VISIBLE);

                    builder.setView(modelRegister);
                    builder.setCancelable(false);
                    builder.setNegativeButton(getString(R.string.refuse), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialoginterface, int i)
                        {
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(0);
                        }
                    });

                    builder.setPositiveButton(getString(R.string.valid), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialoginterface, int i)
                        {
                            String Name = ((EditText) modelRegister.findViewById(R.id.model_register_name)).getText().toString();
                            String Mail = ((EditText) modelRegister.findViewById(R.id.model_register_mail)).getText().toString();
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
    }*/
    //endregion

    //region Populate()
    /*public void Populate()
    {
        final ContactListAdapter mContactListAdapter = new ContactListAdapter(this, CONTACTLIST);

        mContactListAdapter.clear();

        mContactListAdapter.add((new EnumContact(getApplicationContext(), -1, "785b86f1ea73414d6c0493b2411421ba", "Clint Mourlevat", "Pas de nouveau message", mPrefsGlobal.getString("public_key", "none"), false)).bwPhoto());

        for(Map.Entry<String, ?> current : mPrefs.getAll().entrySet())
        {
            try
            {
                JSONObject json = new JSONObject(current.getValue().toString());
                mContactListAdapter.add(new EnumContact(getApplicationContext(), -1, current.getKey(), json.getString("name"), json.getString("description"), json.getString("public_key"), false).bwPhoto().singleLine());
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
    }*/
    //endregion

    //region RSA Encrypt & Decrypt
    /*public String RSAEncrypt(String value) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException, DecoderException
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
    }*/
    //endregion

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch(data.getExtras().getInt("result"))
        {
            case 0:
                if(data.getExtras().getBoolean("callback"))
                {
                    EnumContact enumcontact = (EnumContact)data.getExtras().getSerializable("contact");
                    mDBSecureTalk.NewElement(enumcontact.ID, enumcontact.Name, enumcontact.Description, enumcontact.PublicKey);
                    //mStorage.putString(enumcontact.ID, "{'id':'"+enumcontact.ID+"', 'name':'"+enumcontact.Name+"', 'public_key':'"+enumcontact.PublicKey+"', 'description':'"+enumcontact.Description+"'}");
                    //mStorage.apply();
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

        getFragmentManager().beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(1))
                .commit();

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position)
    {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number)
    {
        switch (number)
        {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar()
    {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.landing, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}