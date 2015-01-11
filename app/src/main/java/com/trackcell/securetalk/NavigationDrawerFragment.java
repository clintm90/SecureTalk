package com.trackcell.securetalk;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class NavigationDrawerFragment extends Fragment
{
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    private NavigationDrawerCallbacks mCallbacks;
    private ActionBar mActionBar;
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;
    private TextView mName;
    
    private List<EnumNavigationDrawer> NAVIGATIONDRAWERLIST = new ArrayList<EnumNavigationDrawer>();

    public NavigationDrawerFragment()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null)
        {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            mFromSavedInstanceState = true;
        }

        SwitchToFragmentByNumber(mCurrentSelectedPosition, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public void onEventMainThread(String input)
    {
        /*List<String> mSenders = new ArrayList<String>();
        try
        {
            for (String current : input.split(","))
            {
                mSenders.add(current);
            }
            Populate(mSenders);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState)
    {
        EventBus.getDefault().register(this);
        
        final SharedPreferences mPrefsGlobal = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        
        View rootView = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        mDrawerListView = (ListView) rootView.findViewById(R.id.fragment_navigation_drawer_list);
        ImageView mPhoto = (ImageView) rootView.findViewById(R.id.fragment_navigation_drawer_photo);
        TextView mVersion = (TextView) rootView.findViewById(R.id.fragment_navigation_drawer_about);
        mName = (TextView) rootView.findViewById(R.id.fragment_navigation_drawer_name);
        
        mName.setText(mPrefsGlobal.getString("name", "none"));
        
        mName.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(getActivity().getApplicationContext(), Profile.class);
                intent.putExtra("contactId", mPrefsGlobal.getString("owner", "none"));
                getActivity().startActivityForResult(intent, 1);
            }
        });
        
        Landing.LoadGravatar(getActivity().getApplicationContext(), mPhoto, mPrefsGlobal.getString("owner", "none"), true);

        try
        {
            String versionName = getActivity().getApplicationContext().getPackageManager().getPackageInfo(getActivity().getApplicationContext().getPackageName(), 0).versionName;
            mVersion.setText("SecureTalk " + versionName);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

        mDrawerListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        
        Populate(new ArrayList<String>());

        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                SwitchToFragmentByNumber(position, (EnumNavigationDrawer)view.getTag());
            }
        });

        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
        return rootView;
    }

    private void Populate(List<String> senders)
    {
        NavigationDrawerAdapter mNavigationDrawerAdapter = new NavigationDrawerAdapter(this.getActivity(), NAVIGATIONDRAWERLIST);

        mNavigationDrawerAdapter.clear();

        String star = "";
        if (senders.size() > 0)
        {
            star = "<b>" + getString(R.string.home) + "</b>&nbsp;<font color=\"red\">*</font>";
        }
        else
        {
            star = getString(R.string.home);
        }
        
        mNavigationDrawerAdapter.add(new EnumNavigationDrawer(this.getActivity(), star, getResources().getDrawable(R.drawable.ic_action_home)));
        mNavigationDrawerAdapter.add(new EnumNavigationDrawer(this.getActivity(), getString(R.string.invite), getResources().getDrawable(R.drawable.ic_action_user2b)));
        mNavigationDrawerAdapter.add(new EnumNavigationDrawer(this.getActivity(), getString(R.string.donation), getResources().getDrawable(R.drawable.ic_action_likeb)));
        mNavigationDrawerAdapter.add(new EnumNavigationDrawer(this.getActivity(), getString(R.string.action_settings), getResources().getDrawable(R.drawable.ic_action_parametersb)));
        mNavigationDrawerAdapter.add(new EnumNavigationDrawer(this.getActivity(), getString(R.string.about2), getResources().getDrawable(R.drawable.ic_action_infob)));

        mDrawerListView.setAdapter(mNavigationDrawerAdapter);
    }

    public boolean isDrawerOpen()
    {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    public void setUp(int fragmentId, DrawerLayout drawerLayout)
    {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        mActionBar = getActionBar();
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_drawer2,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        )
        {
            @Override
            public void onDrawerClosed(View drawerView)
            {
                super.onDrawerClosed(drawerView);
                if (!isAdded())
                {
                    return;
                }

                getActivity().invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
                if (!isAdded())
                {
                    return;
                }

                if (!mUserLearnedDrawer)
                {
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        if (!mUserLearnedDrawer && !mFromSavedInstanceState)
        {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        mDrawerLayout.post(new Runnable()
        {
            @Override
            public void run()
            {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void SwitchToFragmentByNumber(int position, EnumNavigationDrawer enumNavigationDrawer)
    {
        mCurrentSelectedPosition = position;

        switch(position)
        {
            case 0:
                getActionBar().setTitle(getString(R.string.app_name));
                break;
            
            default:
                getActionBar().setTitle(enumNavigationDrawer.Name);
                break;
        }
        
        if (mDrawerListView != null)
        {
            mDrawerListView.setItemChecked(position, true);
        }
        if (mDrawerLayout != null)
        {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null)
        {
            mCallbacks.onNavigationDrawerItemSelected(position);
        }
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        try
        {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        if (mDrawerLayout != null && isDrawerOpen())
        {
            inflater.inflate(R.menu.global, menu);
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (mDrawerToggle.onOptionsItemSelected(item))
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showGlobalContextActionBar()
    {
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        mActionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar()
    {
        return getActivity().getActionBar();
    }

    public static interface NavigationDrawerCallbacks
    {
        void onNavigationDrawerItemSelected(int position);
    }
}
