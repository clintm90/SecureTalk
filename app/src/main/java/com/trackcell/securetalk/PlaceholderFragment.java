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
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
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

                final ContactListAdapter mContactListAdapter = new ContactListAdapter(getActivity().getApplicationContext(), mDBSecureTalk.GetElements());

                mContactListAdapter.add((new EnumContact(getActivity().getApplicationContext(), -1, "785b86f1ea73414d6c0493b2411421ba", "Clint Mourlevat", "Pas de nouveau message", "78", false)).bwPhoto());

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
                        getActivity().startActivityForResult(intent, 1);
                    }
                });

                mMainContent.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
                {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
                    {
                        mDBSecureTalk.RemoveElement(((EnumContact) view.getTag()).RowID);

                        getFragmentManager().beginTransaction()
                                .replace(R.id.container, PlaceholderFragment.newInstance(1))
                                .commit();
                        return false;
                    }
                });
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
                                    mInviteListAdapter.add((new EnumContact(getActivity().getApplicationContext(), -1, email, name, email, null, false)).hideArrow().hidePhoto().singleLine());
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
                //final ListView mInviteList = (ListView) rootViewInvite.findViewById(R.id.fragment_invite_list);
                return rootViewDonation;

            case 4:
                View rootViewAbout = inflater.inflate(R.layout.fragment_about_app, container, false);
                return rootViewAbout;

            default:
                return null;
        }
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        ((Landing) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }
}