package com.darksidebio.bjh3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Alarm extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("Z", "Alarm");
		context.startService(new Intent(context, Downloader.class));
	}
}
