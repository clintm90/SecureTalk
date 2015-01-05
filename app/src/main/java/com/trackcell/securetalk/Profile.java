package com.trackcell.securetalk;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class Profile extends Activity
{
    private WebView mWebView;
    private SharedPreferences mPrefsGlobal;
    private SharedPreferences.Editor mStorageGlobal;
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
        {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        setContentView(R.layout.activity_profile);

        overridePendingTransition(R.anim.trans_left_in, R.anim.trans_left_out);

        mPrefsGlobal = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mStorageGlobal = mPrefsGlobal.edit();
        
        mPrefs = getSharedPreferences("securetalk_elements", MODE_APPEND);
        mStorage = mPrefs.edit();
        
        mWebView = (WebView)findViewById(R.id.webView);
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.loadData("<h1>Loading...</h1>", "test/html", "UTF-8");

        LoadProfile();
    }

    public void LoadProfile()
    {
        final String ID = getIntent().getStringExtra("contactId");

        AsyncTask<Void, Void, Boolean> jsonRequest = new AsyncTask<Void, Void, Boolean>()
        {
            @Override
            protected Boolean doInBackground(Void... strings)
            {
                try
                {
                    String rts = "", c;
                    URL mURL = new URL("http://en.gravatar.com/" + ID + ".json");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(mURL.openStream()));

                    while ((c = reader.readLine()) != null)
                        rts += c;

                    if(rts.equals("User not found"))
                    {
                        return false;
                    }
                    else
                    {
                        return true;
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean isGravatarMember)
            {
                if(isGravatarMember)
                    mWebView.loadUrl("http://en.gravatar.com/" + ID);
                else
                    mWebView.loadUrl("https://gravatar.com/site/signup/");
            }
        };
        jsonRequest.execute();
    }

    public void Disconnect(MenuItem item)
    {
        mStorage.clear();
        mStorageGlobal.clear();
        mStorage.apply();
        mStorageGlobal.apply();
        setResult(RESULT_OK, new Intent().putExtra("result", 1));
        finish();
    }

    @Override
    public void onBackPressed()
    {
        setResult(RESULT_OK, new Intent().putExtra("result", 1));
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.profile, menu);
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
