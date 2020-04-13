package io.lava.ion.receivers;

import io.lava.ion.connectivity.LampManager;
import io.lava.ion.wakelock.WakeLockManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// this receiver gets called every ~60 seconds by AlarmManager
public class IONDiscoveryReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, final Intent intent) {
		// discover lamps
    	if (LampManager.createInstanceIfNeeded(context).shouldBackgroundDiscoveryRun()) {
    		// grab wake lock
    		WakeLockManager.getInstance(context).setDiscoveringLamps(true);
    		
    		LampManager.createInstanceIfNeeded(context).discoverLamps();
    	}
	}
}
