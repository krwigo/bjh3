package com.darksidebio.bjh3;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class PrefsActivity extends PreferenceActivity{
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
}
