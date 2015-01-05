package com.trackcell.securetalk;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.greenrobot.event.EventBus;

public class PlaceholderFragment extends Fragment
{
    private SharedPreferences mPrefs;
    private SharedPreferences mPrefsGlobal;
    private SharedPreferences.Editor mStorage;
    private SharedPreferences.Editor mStorageGlobal;
    private static final String ARG_SECTION_NUMBER = "section_number";
    private ListView mMainContent;
    private TextView mWelcomeLabel;

    public static PlaceholderFragment newInstance(int sectionNumber)
    {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public PlaceholderFragment()
    {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
    }
    
    public void onEventMainThread(String input)
    {
        if(getArguments().getInt(ARG_SECTION_NUMBER) == 1)
        {
            try
            {
                List<String> SenderList = new ArrayList<String>();
                JSONObject json = new JSONObject(input);
                JSONArray items = json.getJSONObject("result").getJSONArray("item");
                
                for(int i=0; i<items.length(); i++)
                {
                    SenderList.add(items.getJSONObject(i).getString("sender"));
                }

                for (int j = 0; j < mMainContent.getChildCount(); j++)
                {
                    View view = mMainContent.getChildAt(j);
                    EnumContact rowContact = (EnumContact) view.getTag();
                    if (SenderList.contains(rowContact.ID))
                    {
                        int duplicates = Collections.frequency(SenderList, rowContact.ID);
                        ((TextView) view.findViewById(R.id.model_contactList_status)).setText(String.valueOf(duplicates));
                        view.findViewById(R.id.model_contactList_status).setVisibility(View.VISIBLE);
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        EventBus.getDefault().register(this);
        
        final DBSecureTalk mDBSecureTalk = new DBSecureTalk(getActivity().getApplicationContext(), "SecureTalk.db", null, 1, null);

        mPrefs = getActivity().getSharedPreferences("securetalk_elements", getActivity().MODE_APPEND);
        mStorage = mPrefs.edit();

        mPrefsGlobal = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        mStorageGlobal = mPrefsGlobal.edit();

        switch (getArguments().getInt(ARG_SECTION_NUMBER))
        {
            case 1:
                View rootView = inflater.inflate(R.layout.fragment_landing, container, false);
                mMainContent = (ListView) rootView.findViewById(R.id.MainContainer);
                Populate(mDBSecureTalk, new ArrayList<String>());
                return rootView;

            case 2:
                View rootViewInvite = inflater.inflate(R.layout.fragment_invite, container, false);
                final ListView mInviteList = (ListView) rootViewInvite.findViewById(R.id.fragment_invite_list);

                final ProgressDialog alertDialog = ProgressDialog.show(getActivity(), "", getString(R.string.loading));
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                alertDialog.setCancelable(true);
                alertDialog.show();

                final List<EnumContact> INVITELIST = new ArrayList<EnumContact>();

                AsyncTask<Void, Void, ContactListAdapter> Task = new AsyncTask<Void, Void, ContactListAdapter>()
                {
                    @Override
                    protected ContactListAdapter doInBackground(Void... params)
                    {
                        try
                        {
                            final ContactListAdapter mInviteListAdapter = new ContactListAdapter(getActivity().getApplicationContext(), INVITELIST);
                            final Cursor mPeoples = getActivity().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
                            while (mPeoples.moveToNext())
                            {
                                String id = mPeoples.getString(mPeoples.getColumnIndex(ContactsContract.Contacts._ID));
                                String name = mPeoples.getString(mPeoples.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                                Cursor emailCur = getActivity().getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", new String[]{id}, null);
                                while (emailCur.moveToNext())
                                {
                                    String email = emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                                    //String gravatarID = new String(Hex.encodeHex(DigestUtils.md5(email)));
                                    mInviteListAdapter.add((new EnumContact(getActivity().getApplicationContext(), -1, email, name, email, null)).hideArrow().hidePhoto().singleLine());
                                }
                                emailCur.close();
                            }
                            return mInviteListAdapter;
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
                        alertDialog.dismiss();
                    }

                    @Override
                    protected void onPostExecute(ContactListAdapter contactListAdapter)
                    {
                        alertDialog.dismiss();
                        mInviteList.setAdapter(contactListAdapter);
                    }
                };
                Task.execute();

                mInviteList.setOnItemClickListener(new AdapterView.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                    {
                    }
                });

                return rootViewInvite;

            case 3:
                View rootViewDonation = inflater.inflate(R.layout.fragment_donation, container, false);
                final ProgressBar mProgress = (ProgressBar)rootViewDonation.findViewById(R.id.fragment_donation_progress);
                WebView mWebView = (WebView)rootViewDonation.findViewById(R.id.donationWebView);
                mWebView.setWebViewClient(new WebViewClient());
                mWebView.getSettings().setJavaScriptEnabled(true);
                mWebView.loadUrl("https://www.paypal.com/cgi-bin/webscr?cmd=_donations" + "&business=clint%2emourlevat%40gmail%2ecom" + "&lc=FR" + "&item_name=ds" + "&item_number=ds" + "&no_note=0" + "&currency_code=EUR" + "&bn=PP-DonationsBF%3abtn_donateCC_LG%2egif%3aNonHostedGuest");
                return rootViewDonation;

            case 4:
                View rootViewSettings = inflater.inflate(R.layout.fragment_settings, container, false);
                return rootViewSettings;

            case 5:
                View rootViewAbout = inflater.inflate(R.layout.fragment_about_app, container, false);
                return rootViewAbout;

            default:
                return null;
        }
    }

    private void Populate(final DBSecureTalk database, List<String> senders)
    {
        final ContactListAdapter mContactListAdapter = new ContactListAdapter(getActivity().getApplicationContext(), database.GetElements(senders));

        mContactListAdapter.add((new EnumContact(getActivity().getApplicationContext(), -1, "785b86f1ea73414d6c0493b2411421ba", "Clint Mourlevat", "Pas de nouveau message", mPrefsGlobal.getString("public_key", "none"))).bwPhoto());

        mMainContent.setAdapter(mContactListAdapter);

        mMainContent.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Landing.BWImageView(false, (ImageView) view.findViewById(R.id.model_contactList_icon), 0);
                Intent intent = new Intent(getActivity().getApplicationContext(), Chat.class);
                intent.putExtra("contact", ((EnumContact) view.getTag()).Name);
                intent.putExtra("public_key", ((EnumContact) view.getTag()).PublicKey);
                intent.putExtra("recipient", ((EnumContact) view.getTag()).ID);
                getActivity().getApplicationContext().stopService(Initialize.MessageWorkerService);
                getActivity().startActivityForResult(intent, 1);
            }
        });

        mMainContent.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                database.RemoveElement(((EnumContact) view.getTag()).RowID);

                getFragmentManager().beginTransaction()
                        .replace(R.id.container, PlaceholderFragment.newInstance(1))
                        .commit();
                return false;
            }
        });
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        ((Landing) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }
}