package com.trackcell.securetalk;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Search extends Activity
{
    private NotificationManager mNotificationManager;
    private PhoneNumberUtil mPhoneUtil;
    private AccountManager mManager;
    private ConnectivityManager mConnectivityManager;
    private EditText mSearchField;
    private ProgressBar mSearchProgress;
    public ListView mResultContent;
    private EnumContact mRTS;
    private SharedPreferences mPrefsGlobal;
    private SharedPreferences.Editor mStorageGlobal;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        mConnectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        mPrefsGlobal = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mStorageGlobal = mPrefsGlobal.edit();

        mManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        mPhoneUtil = PhoneNumberUtil.getInstance();

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
        {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        setContentView(R.layout.activity_search);

        overridePendingTransition(R.anim.trans_left_in, R.anim.trans_left_out);

        SearchFromDatabase(".");

        mResultContent = (ListView)findViewById(R.id.ResultContent);
        mSearchField = (EditText)findViewById(R.id.searchField);
        mSearchProgress = (ProgressBar)findViewById(R.id.searchProgress);

        mSearchField.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                {
                    SearchFromDatabase(mSearchField.getText().toString());
                }
                return true;
            }
        });

        mResultContent.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                mRTS = ((EnumContact)view.getTag());
            }
        });
    }

    public void SearchFromDatabase(String query)
    {
        final ProgressDialog mDialog = ProgressDialog.show(this, "", getString(R.string.searching));
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mDialog.setCancelable(false);
        mDialog.show();

        final ArrayList<EnumContact> mContactList = new ArrayList<EnumContact>();
        final ContactListAdapter mSearchListAdapter = new ContactListAdapter(this, mContactList);

        final AsyncTask<String, Void, String> SearchTask = new AsyncTask<String, Void, String>()
        {
            @Override
            protected String doInBackground(String... params)
            {
                Thread.currentThread().setName("SearchFromDatabase");

                try
                {
                    String rts = "", c;
                    URL mURL = new URL(Initialize.SecureTalkServer + "searchByName.php?query=" + params[0]);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(mURL.openStream()));

                    while ((c = reader.readLine()) != null)
                        rts += c;
                    return rts;
                }
                catch(UnknownHostException e)
                {
                    NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();
                    if(netInfo == null || !netInfo.isConnectedOrConnecting())
                        cancel(true);
                    return null;
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    return e.getMessage();
                }
            }

            @Override
            protected void onCancelled()
            {
                mDialog.dismiss();
                setResult(RESULT_OK, new Intent().putExtra("result", 3).putExtra("error_content", getResources().getString(R.string.noconnection)));
                finish();
            }

            @Override
            protected void onPreExecute()
            {
                mDialog.show();
            }

            @Override
            protected void onPostExecute(String input)
            {
                mDialog.dismiss();

                try
                {
                    mSearchProgress.setVisibility(View.INVISIBLE);
                    JSONObject mRoot = new JSONObject(input);
                    JSONObject mItems = mRoot.getJSONObject("result");

                    for (int i = 0; i < mItems.getJSONArray("item").length(); i++)
                    {
                        JSONObject currentObject = mItems.getJSONArray("item").getJSONObject(i);
                        if(!mPrefsGlobal.getString("owner", "none").equals(currentObject.getString("id")))
                            mContactList.add(new EnumContact(getApplicationContext(), -1, currentObject.getString("id"), currentObject.getString("name"), currentObject.getString("description"), currentObject.getString("public_key"), false).hideArrow().bwPhoto());
                        mResultContent.setAdapter(mSearchListAdapter);
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        };

        mDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                SearchTask.cancel(true);
            }
        });

        if(!query.trim().equals("") && query.length() >= 1)
        {
            SearchTask.execute(query);
        }
        else
        {
            mSearchField.setError(getResources().getString(R.string.typemore));
            mDialog.dismiss();
        }
    }

    public void test()
    {
        ArrayList<EnumContact> mContactList = new ArrayList<EnumContact>();
        ContactListAdapter mContactListAdapter = new ContactListAdapter(this, mContactList);
        Cursor phones = this.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

        while (phones.moveToNext())
        {
            String IPN;
            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            try
            {
                Phonenumber.PhoneNumber ProtoNumber = mPhoneUtil.parse(number, "FR");
                IPN = mPhoneUtil.format(ProtoNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
            }
            catch (Exception err)
            {
                IPN = number;
            }
            mContactList.add(new EnumContact(getApplicationContext(), -1, "785b86f1ea73414d6c0493b2411421ba", name, IPN, "none", false).hidePhoto().hideArrow());
        }
        phones.close();
        mResultContent.setAdapter(mContactListAdapter);
    }

    public void valid(MenuItem item)
    {
        closeWithResult();
    }

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onBackPressed()
    {
        setResult(RESULT_OK, new Intent().putExtra("result", 1));
        super.onBackPressed();
    }

    public void closeWithResult()
    {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", 0);
        returnIntent.putExtra("callback", true);
        returnIntent.putExtra("contact", mRTS);
        setResult(RESULT_OK, returnIntent);

        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        switch (id)
        {
            case R.id.action_search_valid:
                if(mRTS != null)
                {
                    closeWithResult();
                }
                else
                {
                    SearchFromDatabase(mSearchField.getText().toString());
                }
                return true;

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
