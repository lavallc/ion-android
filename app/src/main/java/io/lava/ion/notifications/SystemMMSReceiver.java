package io.lava.ion.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SystemMMSReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		// we consider MMS to be an SMS
		CentralNotificationReceiver.getInstance().onSMS();
	}
}