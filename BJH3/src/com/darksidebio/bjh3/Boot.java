package com.darksidebio.bjh3;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Boot extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("Z", "BOOT3");
	    PendingIntent mTimerIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, Alarm.class), 0);
	    AlarmManager mAlarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, 60000, mTimerIntent);
	}
}
