package com.darksidebio.bjh3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
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

	public void onStop() {
		super.onStop();
		Log.d("Z", "PrefsActivity onStop()");
		sendBroadcast(new Intent().setAction("SOME_ACTION"));
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
	}
}
