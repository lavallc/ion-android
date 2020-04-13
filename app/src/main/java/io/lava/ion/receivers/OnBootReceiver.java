package io.lava.ion.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnBootReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent recvIntent) {
		// setup our alarms
		AlarmScheduler.setupAlarms(context);
	}
}
