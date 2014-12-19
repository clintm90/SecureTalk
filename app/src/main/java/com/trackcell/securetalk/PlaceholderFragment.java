package com.trackcell.securetalk;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

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

        switch(getArguments().getInt(ARG_SECTION_NUMBER))
        {
            case 1:
                View rootView = inflater.inflate(R.layout.fragment_landing, container, false);
                mMainContent = (ListView)rootView.findViewById(R.id.MainContainer);

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
                        mDBSecureTalk.RemoveElement(((EnumContact)view.getTag()).RowID);

                        getFragmentManager().beginTransaction()
                                .replace(R.id.container, PlaceholderFragment.newInstance(1))
                                .commit();
                        return false;
                    }
                });
                return rootView;

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