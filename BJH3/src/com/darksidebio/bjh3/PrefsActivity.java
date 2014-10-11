package com.darksidebio.bjh3;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

public class PrefsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public PrefsActivity() {
		Log.d("Z", "PrefsActivity INIT()");
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	   super.onCreate(savedInstanceState);
		Log.d("Z", "PrefsActivity onCreate()");
		addPreferencesFromResource(R.xml.prefs);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		Log.d("Z", "onSharedPreferenceChanged: "+key);
/*		Preference pref = findPreference(key);
	    if (pref instanceof ListPreference) {
	    	Log.d("Z", "[ListPref] Updating");
	        ListPreference listPref = (ListPreference) pref;
	        pref.setSummary(listPref.getEntry());
	    }*/
	}
}
