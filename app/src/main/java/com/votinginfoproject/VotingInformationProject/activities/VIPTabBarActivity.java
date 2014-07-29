package com.votinginfoproject.VotingInformationProject.activities;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.votinginfoproject.VotingInformationProject.R;
import com.votinginfoproject.VotingInformationProject.fragments.BallotFragment;
import com.votinginfoproject.VotingInformationProject.fragments.ContestFragment;
import com.votinginfoproject.VotingInformationProject.fragments.ElectionDetailsFragment;
import com.votinginfoproject.VotingInformationProject.fragments.LocationsFragment;
import com.votinginfoproject.VotingInformationProject.models.GeocodeQuery;
import com.votinginfoproject.VotingInformationProject.models.PollingLocation;
import com.votinginfoproject.VotingInformationProject.models.VIPApp;
import com.votinginfoproject.VotingInformationProject.models.VIPAppContext;
import com.votinginfoproject.VotingInformationProject.models.VoterInfo;

public class VIPTabBarActivity extends FragmentActivity {

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;
    FragmentManager mFragmentManager;
    VIPAppContext mAppContext;
    GeocodeQuery.GeocodeCallBackListener pollingCallBackListener;
    GeocodeQuery.GeocodeCallBackListener homeCallBackListener;
    ArrayList<PollingLocation> allLocations;
    VoterInfo voterInfo;
    HashMap<String, Integer> locationIds;
    Location homeLocation;
    LocationsFragment locationsFragment;
    Context context;

    private final static double MILES_IN_METER = 0.000621371192;
    private final static double KILOMETERS_IN_METER = 0.001;


    /**
     * Non-default constructor for testing, to set the application context.
     * @param context Mock context with a VoterInfo object
     */
    public VIPTabBarActivity(VIPAppContext context) {
        super();
        mAppContext = context;
    }

    public VIPTabBarActivity() {
        super();
    }

    /**
     * Transition from ballot fragment to contest details fragment when user selects list item.
     *
     * @param position Index of selected contest within the VoterInfo object's list of contests
     */
    public void showContestDetails(int position) {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        Fragment contestFragment = ContestFragment.newInstance(position);

        // Ballot fragment is not actually removed by `replace` call here, because it's in a tab bar.
        // Contest fragment will hide the ballot fragment components, then show them again
        // when user navigates back.
        fragmentTransaction.replace(R.id.ballot_fragment, contestFragment);

        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
    
    public VIPAppContext getAppContext() {
        return mAppContext;
    }

    public VoterInfo getVoterInfo() {
        return mAppContext.getVIPApp().getVoterInfo();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viptab_bar);

        mFragmentManager = getSupportFragmentManager();
        mAppContext = new VIPAppContext((VIPApp) getApplicationContext());
        context = mAppContext.getAppContext();

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Set up ViewPager
        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.pager);
        setContentView(mViewPager);

        // Set up TabsAdapter
        mTabsAdapter = new TabsAdapter(this, mViewPager);
        mTabsAdapter.addTab(actionBar.newTab().setText(R.string.tabbar_ballot_tab), BallotFragment.class, "ballot_tab", null);
        mTabsAdapter.addTab(actionBar.newTab().setText(R.string.tabbar_where_to_vote_tab), LocationsFragment.class, "locations_tab", null);
        mTabsAdapter.addTab(actionBar.newTab().setText(R.string.tabbar_details_tab), ElectionDetailsFragment.class, "details_tab", null);

        if (savedInstanceState != null) {
            actionBar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }

        setUpGeocodings();
    }

    /**
     * Helper function to geocode entered address and the polling location results for that address
     * when the tab bar is loaded.
     */
    private void setUpGeocodings() {
        // get LocationsFragment's root view
        locationsFragment = (LocationsFragment)mTabsAdapter.getItem(1);
        voterInfo = getVoterInfo();
        setAllLocations();
        setLocationIds();

        // Callback for polling location geocode result
        pollingCallBackListener = (key, lat, lon) -> {
            if (key == "error") {
                Log.e("VIPTabBarActivity", "Geocode failed!");
                return;
            }

            // find object and set values on it
            PollingLocation foundLoc = allLocations.get(locationIds.get(key));
            foundLoc.address.latitude = lat;
            foundLoc.address.longitude = lon;

            // distance calculation
            Location pollingLocation = new Location("polling");
            pollingLocation.setLatitude(lat);
            pollingLocation.setLongitude(lon);

            if (mAppContext.useMetric()) {
                // convert meters to kilometers
                foundLoc.address.distance = pollingLocation.distanceTo(homeLocation) * KILOMETERS_IN_METER;
            } else {
                // convert result from meters to miles
                foundLoc.address.distance = pollingLocation.distanceTo(homeLocation) * MILES_IN_METER;
            }

            locationsFragment.refreshList();
        };

        // callback for home address geocode result
        homeCallBackListener = (key, lat, lon) -> {
            if (key == "error") {
                Log.e("VIPTabBarActivity", "Failed to geocode home address!");
                return;
            }

            homeLocation = new Location("home");
            homeLocation.setLatitude(lat);
            homeLocation.setLongitude(lon);

            // start background geocode tasks for polling locations
            for (PollingLocation location : allLocations) {
                // key by address, if location has no ID
                if (location.id != null) {
                    new GeocodeQuery(context, pollingCallBackListener, location.id, location.address.toGeocodeString()).execute();
                } else {
                    new GeocodeQuery(context, pollingCallBackListener, location.address.toString(), location.address.toGeocodeString()).execute();
                }
            }
        };

        // geocode home address; once result returned, geocode polling locations
        new GeocodeQuery(context, homeCallBackListener, "home", voterInfo.normalizedInput.toGeocodeString()).execute();
    }

    private void setAllLocations() {
        // get all locations (both polling and early voting)
        allLocations = new ArrayList<PollingLocation>();
        if (voterInfo.pollingLocations != null) {
            allLocations.addAll(voterInfo.pollingLocations);
        }

        if (voterInfo.earlyVoteSites != null) {
            allLocations.addAll(voterInfo.earlyVoteSites);
        }
    }

    public ArrayList<PollingLocation> getAllLocations() {
        return allLocations;
    }

    /**
     * Build map of PollingLocation id to its offset in the list of all locations,
     * to find it later when the distance calculation comes back.
     */
    private void setLocationIds() {
        locationIds = new HashMap<String, Integer>(allLocations.size());
        for (int i = allLocations.size(); i--> 0;) {
            PollingLocation location = allLocations.get(i);
            if (location.id != null) {
                locationIds.put(location.id, i);
            } else {
                locationIds.put(location.address.toString(), i);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.viptab_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // Navigate to HomeActivity from main TabBar
            NavUtils.navigateUpFromSameTask(this);
        }
        if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost.  It relies on a
     * trick.  Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show.  This is not sufficient for switching
     * between pages.  So instead we make the content part of the tab host
     * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
     * view to show as the tab content.  It listens to changes in tabs, and takes
     * care of switch to the correct paged in the ViewPager whenever the selected
     * tab changes.
     *
     * NOTE: This class is lifted directly from the ViewPager class docs:
     *       http://developer.android.com/reference/android/support/v4/view/ViewPager.html
     */
    public static class TabsAdapter extends FragmentPagerAdapter
            implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>(3);
        private LocationsFragment locationsFragment;

        static final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(Class<?> _class, Bundle _args) {
                clss = _class;
                args = _args;
            }
        }

        public TabsAdapter(FragmentActivity activity, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mActionBar = activity.getActionBar();
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, String tag, Bundle args) {
            TabInfo info = new TabInfo(clss, args);
            tab.setTag(tag);
            tab.setTabListener(this);
            mTabs.add(info);
            mActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            // return the proper Fragment type given the tab
            switch (position) {
                case 0: {
                    return BallotFragment.newInstance();
                }
                case 1: {
                    locationsFragment = LocationsFragment.newInstance();
                    return locationsFragment;
                }
                case 2: {
                    return ElectionDetailsFragment.newInstance();
                }
                default: {
                    Log.e("VIPTabBarActivity", "GETTING DEFAULT FRAGMENT FOR POSITION " + position);
                    return LocationsFragment.newInstance();
                }
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
            mViewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {

        }

        /**
         * tell polling locations list to refresh when switching to that tab, to get the
         * geocoding results have returned since its fragment was created
         * (re-select is always triggered on select)
         */
        @Override
        public void onTabReselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
            if (tab.getTag() == "locations_tab") {
                locationsFragment.refreshList();
            }
        }
    }
}
