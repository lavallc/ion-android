package io.lava.ion.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SystemAlarmReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		CentralNotificationReceiver.getInstance().onAlarm();
	}
}