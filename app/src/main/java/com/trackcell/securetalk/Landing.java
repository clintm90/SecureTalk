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
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
    private DrawerLayout mDrawerLayout;

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

        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mPhoneUtil = PhoneNumberUtil.getInstance();

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
            mKeyFactory = KeyFactory.getInstance("RSA");
        }
        catch (Exception exception)
        {
            mKeyPairGenerator = null;
        }

        setContentView(R.layout.activity_landing);

        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, mDrawerLayout);
    }

    public void test()
    {
        Toast.makeText(getApplicationContext(), "salut", Toast.LENGTH_LONG).show();
    }

    public void InitUser()
    {
        mKP = mKeyPairGenerator.generateKeyPair();
        final Context parent = this;

        if (!mPrefsGlobal.getBoolean("initialized", false) && mPrefsGlobal.getString("owner", "none").equals("none") && mPrefsGlobal.getString("password", "none").equals("none") && mPrefsGlobal.getString("private_key", "none").equals("none"))
        {
            Login(false, null);
        }
        else
        {
            final ProgressDialog alertDialog = ProgressDialog.show(this, "", getString(R.string.loading));
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            alertDialog.setCancelable(false);
            alertDialog.show();

            AsyncTask<String, Void, String> LoginTask = new AsyncTask<String, Void, String>()
            {
                @Override
                protected String doInBackground(String... params)
                {
                    try
                    {
                        Thread.currentThread().setName("Landing_InitUser");
                        String rts = "", c;
                        URL mURL = new URL(Initialize.SecureTalkServer + "registerUserByID.php?id=" + params[0] + "&password=" + params[1] + "&put=false");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(mURL.openStream()));

                        while ((c = reader.readLine()) != null)
                        {
                            rts += c;
                        }
                        return rts;
                    }
                    catch (UnknownHostException e)
                    {
                        NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();
                        if (netInfo == null || !netInfo.isConnectedOrConnecting())
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
                }

                @Override
                protected void onPostExecute(String input)
                {
                    try
                    {
                        alertDialog.dismiss();
                        JSONObject registerValue = new JSONObject(input);
                        if (registerValue.getInt("response") != 1)
                        {
                            Login(true, getString(R.string.noaccount));
                        }
                    }
                    catch (JSONException e)
                    {
                        Login(true, getString(R.string.noconnection));
                        e.printStackTrace();
                    }
                }
            };
            LoginTask.execute(mPrefsGlobal.getString("owner", "none"), mPrefsGlobal.getString("password", "none"));
        }
    }

    private void Login(boolean error, String errorContent)
    {
        final Account aaccount[] = AccountManager.get(getApplicationContext()).getAccountsByType("com.google");

        final Context parent = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        final View mModelLogin = getLayoutInflater().inflate(R.layout.model_login, null);

        final EditText mMail = ((EditText) mModelLogin.findViewById(R.id.model_login_mail));
        final EditText mPassword = ((EditText) mModelLogin.findViewById(R.id.model_login_password));

        mMail.setText(aaccount[0].name);

        if (error)
        {
            TextView mError = (TextView) mModelLogin.findViewById(R.id.model_login_error);
            mError.setVisibility(View.VISIBLE);
            mError.setText(errorContent);
        }

        builder.setView(mModelLogin);
        builder.setCancelable(false);
        builder.setNeutralButton(getString(R.string.newaccount), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialoginterface, int i)
            {
                NewAccount(false, null);
                //android.os.Process.killProcess(android.os.Process.myPid());
                //System.exit(0);
            }
        });

        builder.setPositiveButton(getString(R.string.connect), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialoginterface, int i)
            {
                if (!mMail.getText().toString().contains("@"))
                {
                    Login(true, getString(R.string.nomail));
                }
                else if (mPassword.getText().toString().length() < 3)
                {
                    Login(true, getString(R.string.shortpassword));
                }
                else
                {
                    AsyncTask<String, Void, String> LoginTask = new AsyncTask<String, Void, String>()
                    {
                        @Override
                        protected String doInBackground(String... params)
                        {
                            try
                            {
                                Thread.currentThread().setName("Landing_Login");
                                String rts = "", c;
                                URL mURL = new URL(Initialize.SecureTalkServer + "registerUserByID.php?id=" + params[0] + "&password=" + params[1] + "&put=false");
                                BufferedReader reader = new BufferedReader(new InputStreamReader(mURL.openStream()));

                                while ((c = reader.readLine()) != null)
                                {
                                    rts += c;
                                }
                                return rts;
                            }
                            catch (UnknownHostException e)
                            {
                                NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();
                                if (netInfo == null || !netInfo.isConnectedOrConnecting())
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
                        }

                        @Override
                        protected void onPostExecute(String input)
                        {
                            try
                            {
                                JSONObject registerValue = new JSONObject(input);
                                if (registerValue.getInt("response") == 1)
                                {
                                    String mPublicKey = new String(Hex.encodeHex((mKP.getPublic().getEncoded())));
                                    String mPrivateKey = new String(Hex.encodeHex(mKP.getPrivate().getEncoded()));

                                    mStorageGlobal.putBoolean("initialized", true);
                                    mStorageGlobal.putString("owner", new String(Hex.encodeHex(DigestUtils.md5(mMail.getText().toString()))));
                                    mStorageGlobal.putString("name", registerValue.getString("name"));
                                    mStorageGlobal.putString("password", new String(Hex.encodeHex(DigestUtils.md5(mPassword.getText().toString()))));
                                    mStorageGlobal.putString("public_key", mPublicKey);
                                    mStorageGlobal.putString("private_key", mPrivateKey);
                                    mStorageGlobal.apply();
                                    recreate();
                                    //UpdatePublicKeyTask.execute(input[2].toString(), mPublicKey);
                                }
                                else
                                {
                                    Login(true, getString(R.string.noaccount));
                                }
                            }
                            catch (Exception e)
                            {
                                Login(true, getString(R.string.noconnection));
                                e.printStackTrace();
                            }
                        }
                    };
                    LoginTask.execute(new String(Hex.encodeHex(DigestUtils.md5(mMail.getText().toString()))), new String(Hex.encodeHex(DigestUtils.md5(mPassword.getText().toString()))));
                }
            }
        });

        builder.create();
        builder.show();
    }

    private void NewAccount(boolean error, String errorContent)
    {
        final Context parent = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        final View mModelNewAccount = getLayoutInflater().inflate(R.layout.model_newaccount, null);

        final TextView mName = (TextView) mModelNewAccount.findViewById(R.id.model_newaccount_name);
        final TextView mMail = (TextView) mModelNewAccount.findViewById(R.id.model_newaccount_mail);
        final String gravatarID = new String(Hex.encodeHex(DigestUtils.md5(mMail.getText().toString())));
        final String password = new String(Hex.encodeHex(DigestUtils.md5(((TextView) mModelNewAccount.findViewById(R.id.model_newaccount_mail)).getText().toString())));

        try
        {
            final Account aaccount[] = AccountManager.get(getApplicationContext()).getAccountsByType("com.google");
            mMail.setText(aaccount[0].name);
        }
        catch(Exception e)
        {
        }

        if (error)
        {
            TextView mError = (TextView) mModelNewAccount.findViewById(R.id.model_newaccount_error);
            mError.setVisibility(View.VISIBLE);
            mError.setText(errorContent);
            //((EditText) mModelLogin.findViewById(R.id.model_login_mail)).setError(errorContent);
        }

        builder.setView(mModelNewAccount);
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                Login(false, null);
            }
        });
        builder.setNeutralButton(getString(R.string.refuse), new DialogInterface.OnClickListener()
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
                final AsyncTask<Object, Void, Object[]> NewUserTask = new AsyncTask<Object, Void, Object[]>()
                {
                    @Override
                    protected Object[] doInBackground(Object... params)
                    {
                        try
                        {
                            String mPublicKey = new String(Hex.encodeHex((mKP.getPublic().getEncoded())));
                            String mPrivateKey = new String(Hex.encodeHex(mKP.getPrivate().getEncoded()));

                            Object[] mRTS = new Object[10];
                            String rts = "", c;
                            URL mURL = new URL(Initialize.SecureTalkServer + "registerUserByID.php?id=" + params[0].toString() + "&name=" + params[1].toString() + "&description=" + "description" + "&password=" + params[2].toString() + "&public_key=" + mPublicKey + "&put=true");
                            BufferedReader reader = new BufferedReader(new InputStreamReader(mURL.openStream()));

                            while ((c = reader.readLine()) != null)
                            {
                                rts += c;
                            }
                            mRTS[0] = rts;          //response
                            mRTS[1] = params[0];    //owner
                            mRTS[2] = params[1];    //name
                            mRTS[3] = params[2];    //password
                            mRTS[4] = mPublicKey;    //publickey
                            mRTS[5] = mPrivateKey;    //privatekey
                            return mRTS;
                        }
                        catch (UnknownHostException e)
                        {
                            NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();
                            if (netInfo == null || !netInfo.isConnectedOrConnecting())
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
                    }

                    @Override
                    protected void onPostExecute(Object[] input)
                    {
                        try
                        {
                            JSONObject registerValue = new JSONObject(input[0].toString());
                            if (registerValue.getInt("response") == 1)
                            {
                                mStorageGlobal.putBoolean("initialized", true);
                                mStorageGlobal.putString("owner", input[1].toString());
                                mStorageGlobal.putString("name", input[2].toString());
                                mStorageGlobal.putString("password", input[3].toString());
                                mStorageGlobal.putString("public_key", input[4].toString());
                                mStorageGlobal.putString("private_key", input[5].toString());
                                mStorageGlobal.apply();
                                recreate();
                            }
                            else
                            {
                                NewAccount(true, getString(R.string.userexist));
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                };
                if(!((TextView) mModelNewAccount.findViewById(R.id.model_newaccount_password)).getText().toString().equals(((TextView) mModelNewAccount.findViewById(R.id.model_newaccount_password2)).getText().toString()))
                {
                    NewAccount(true, getString(R.string.notseempassword));
                }
                if(!((TextView) mModelNewAccount.findViewById(R.id.model_newaccount_mail)).getText().toString().contains("@"))
                {
                    NewAccount(true, getString(R.string.nomail));
                }
                else if(((TextView) mModelNewAccount.findViewById(R.id.model_newaccount_password)).getText().toString().length() < 3)
                {
                    NewAccount(true, getString(R.string.shortpassword));
                }
                else
                {
                    NewUserTask.execute(gravatarID, mName.getText().toString(), password);
                }
            }
        });

        builder.create();
        builder.show();
    }

    public static void LoadGravatar(final Context context, final ImageView imageView, String id, final boolean isPhotoBW)
    {
        final AsyncTask<String, Void, Bitmap> GravatarTask = new AsyncTask<String, Void, Bitmap>()
        {
            @Override
            protected Bitmap doInBackground(String... params)
            {
                Thread.currentThread().setName("Landing_LoadGravatar");
                HttpURLConnection httpURLConnection = null;

                try
                {
                    httpURLConnection = (HttpURLConnection) (new URL("http://www.gravatar.com/avatar/" + params[0] + "?s=80&d=404")).openConnection();
                    return BitmapFactory.decodeStream(httpURLConnection.getInputStream(), null, new BitmapFactory.Options());
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
    }

    public static void BWImageView(boolean execute, ImageView imageView, float level)
    {
        if (execute)
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
        if (!mPrefsGlobal.getString("owner", "none").equals("none"))
        {
            NetworkInfo networkinfo = mConnectivityManager.getActiveNetworkInfo();
            if (networkinfo != null && networkinfo.isConnectedOrConnecting())
            {
                Initialize.ActivityForeground = true;
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

    @Override
    public void onStart()
    {
        super.onStart();
        InitUser();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Initialize.ActivityForeground = true;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Initialize.ActivityForeground = false;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopService(Initialize.MessageWorkerService);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        try
        {
            switch (data.getExtras().getInt("result"))
            {
                case 0:
                    if (data.getExtras().getBoolean("callback"))
                    {
                        EnumContact enumcontact = (EnumContact) data.getExtras().getSerializable("contact");
                        mDBSecureTalk.NewElement(enumcontact.ID, enumcontact.Name, enumcontact.Description, enumcontact.PublicKey);
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
                    String error_content = data.getExtras().getString("error_content");
                    Toast.makeText(getApplicationContext(), error_content, Toast.LENGTH_LONG).show();
                    break;

                default:
                    break;
            }

            getFragmentManager().beginTransaction()
                    .replace(R.id.container, PlaceholderFragment.newInstance(1))
                    .commit();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public static class PrefsFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.parameters);
        }
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
    public boolean onKeyDown(int keyCode, KeyEvent e)
    {
        if (keyCode == KeyEvent.KEYCODE_MENU)
        {
            if(!mDrawerLayout.isDrawerOpen(Gravity.LEFT))
            {
                mDrawerLayout.openDrawer(Gravity.LEFT);
            }
            else
            {
                mDrawerLayout.closeDrawer(Gravity.LEFT);
            }
            return true;
        }
        return super.onKeyDown(keyCode, e);
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