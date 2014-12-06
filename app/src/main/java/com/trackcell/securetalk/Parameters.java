package com.trackcell.securetalk;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

public class Parameters extends PreferenceActivity
{
    private TelephonyManager mTelephonyManager;
    private PhoneNumberUtil mPhoneUtil;
    private SharedPreferences mPrefs, mPrefsGlobal;
    private SharedPreferences.Editor mStorage, mStorageGlobal;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mTelephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

        mPhoneUtil = PhoneNumberUtil.getInstance();

        mPrefsGlobal = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mStorageGlobal = mPrefsGlobal.edit();
        mPrefs = getSharedPreferences("securetalk_elements", MODE_APPEND);
        mStorage = mPrefs.edit();

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
        {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        //setContentView(R.layout.activity_parameters);
        addPreferencesFromResource(R.xml.parameters);

        overridePendingTransition(R.anim.trans_left_in, R.anim.trans_left_out);
    }

    public void clearCache(View vie)
    {
        mStorage.clear();
        mStorageGlobal.clear();
    }

    public void save(MenuItem item)
    {
        mStorage.apply();
        mStorageGlobal.apply();

        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", 2);
        setResult(RESULT_OK, returnIntent);

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
        getMenuInflater().inflate(R.menu.parameters, menu);
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
