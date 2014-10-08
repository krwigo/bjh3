package com.darksidebio.bjh3;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.AlarmManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends FragmentActivity {
	AlarmManager mAlarmManager;
	MyPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;
	ActionBar mActionBar;
	TabListener mTabListener;
	static List<String> mTabTitles;
	static HashMap<String,String> mTabFeeds;
	static Database mDatabase; 
	
	public MainActivity() {
		Log.d("Z", "Main()");

		mTabTitles = Arrays.asList("BJH3", "Boxer", "FMH", "Trash", "Map");
		
		mTabFeeds = new HashMap<String,String>();
		mTabFeeds.put("BJH3",  "http://www.hash.cn/feed/");
		mTabFeeds.put("Boxer", "http://www.hash.cn/category/boxerh3/feed/");
		mTabFeeds.put("FMH",   "http://www.hash.cn/category/fullmoonh3/feed/");
		mTabFeeds.put("Trash", "http://www.hash.cn/category/hashtrash/feed/");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
/*
          Intent i = new Intent(this, UserSettingActivity.class);
            startActivityForResult(i, RESULT_SETTINGS);
            break;
        }*/

		switch (item.getItemId()) {
		default:
			Log.d("Z", "onOptionsItemSelected()");
			Log.d("Z", "-id:"+item.getItemId());
			Log.d("Z", "-title:"+item.getTitle());
			break;
		}
		return true;
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("Z", "Create()");
		super.onCreate(savedInstanceState);
		
		mActionBar = getActionBar();
	    mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	    mActionBar.setTitle("Beijing Hash House Harriers");
	    
		setContentView(R.layout.activity_main);
///		addPreferencesFromResource(R.xml.prefs);
		
		mViewPager = (ViewPager)findViewById(R.id.pager);
		
		mTabListener = new TabListener() {
			@Override
			public void onTabSelected(Tab t, FragmentTransaction f) {
	            mViewPager.setCurrentItem(t.getPosition());
			}
			@Override
			public void onTabUnselected(Tab t, FragmentTransaction f) {
			}
			@Override
			public void onTabReselected(Tab t, FragmentTransaction f) {
			}
	    };
	    
		mViewPager.setAdapter(
			new MyPagerAdapter(getSupportFragmentManager())
		);
		
		mViewPager.setOnPageChangeListener(
			new ViewPager.SimpleOnPageChangeListener() {
				@Override
				public void onPageSelected(int position) {
					getActionBar().setSelectedNavigationItem(position);
				}
			}
		);
		
		mActionBar.removeAllTabs();
		
	    for (String sTitle : mTabTitles) {
	        mActionBar.addTab(
        		mActionBar.newTab()
                    .setText(sTitle)
                    .setTabListener(mTabListener)
    		);
	    }
	    
	    mDatabase = new Database(this);

	    PendingIntent mTimerIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, Alarm.class), 0);
		mAlarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, 5000, mTimerIntent);
	}
	 
	public class MyPagerAdapter extends FragmentPagerAdapter {
		public MyPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Fragment f = null;
			
			switch (position) {
			case 4:
				f = new MyDummyFragment();
				break;
			default:
				f = new MyRssFragment();
				break;
			}
			
			Bundle args = new Bundle();
			args.putInt("pos", position);
			args.putString("title", mTabTitles.get(position));
			f.setArguments(args);
			
			return f;
		}

		@Override
		public int getCount() {
			return mTabTitles.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return mTabTitles.get(position);
		}
 	}
	
	public static class MyDummyFragment extends Fragment {
		Integer mPos;
		String mTitle;
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			mPos = getArguments().getInt("pos");
			mTitle = getArguments().getString("title");
			setHasOptionsMenu(true);
			
			View rootView = inflater.inflate(R.layout.fragment_dummy, container, false);
			TextView dummyTextView = (TextView)rootView.findViewById(R.id.section_label);
			dummyTextView.setText("Inflated("+Integer.toString(mPos)+", "+mTitle+")");
			return rootView;
		}
	}

	public static class MyRssFragment extends Fragment {
		Integer mPos;
		String mTitle;
		SimpleCursorAdapter mCursorAdapter;
		
		class MyBroadcastReceiver extends BroadcastReceiver {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				Log.d("Z", "FROM RSS FRAGMENT(3) -- MyBroadcastReceiver:onReceive -- "+mTitle);
				Cursor mCursor = mDatabase.getReadableDatabase().rawQuery("SELECT id as _id, viewed, feed, title, url FROM feeditems WHERE feed='"+mTitle+"' ORDER BY _id DESC", null);
				mCursorAdapter.changeCursor(mCursor);
				mCursorAdapter.notifyDataSetChanged();
			}
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			mPos = getArguments().getInt("pos");
			mTitle = getArguments().getString("title");
			
			Cursor mCursor = mDatabase.getReadableDatabase().rawQuery("SELECT id as _id, viewed, feed, title, url FROM feeditems WHERE feed='"+mTitle+"' ORDER BY _id DESC", null);
			
		    String[] fromFieldNames = new String[] { "title", "url" };
			int[] toViewIDs = new int[] { R.id.tvName, R.id.tvURL };
			mCursorAdapter = new SimpleCursorAdapter(getActivity(), R.layout.fragment_list_item, mCursor, fromFieldNames, toViewIDs, 0);
			
			View rootView = inflater.inflate(R.layout.fragment_list, container, false);
			ListView lv = (ListView)rootView.findViewById(R.id.flist);
			lv.setAdapter(mCursorAdapter);
			
			MyBroadcastReceiver br = new MyBroadcastReceiver();
			IntentFilter f = new IntentFilter();
			f.addAction("SOME_ACTION");
			getActivity().registerReceiver(br, f);
			
			return rootView;
		}
	}
}
