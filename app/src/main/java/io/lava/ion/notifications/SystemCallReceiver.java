package io.lava.ion.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class SystemCallReceiver extends BroadcastReceiver {
	private static boolean phoneRang = false;
    private static boolean phoneAnswered = false;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// Get the current Phone State
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        
        if (state == null)
        	return;

        // phone is ringing
        if(state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
        	CentralNotificationReceiver.getInstance().onPhoneRinging();
        	phoneRang = true;
        }

        // phone is answered
        if(state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
        	phoneAnswered = true;
        }

        // phone no longer ringing
        if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
        	if (phoneRang && !phoneAnswered) {
        		CentralNotificationReceiver.getInstance().onMissedCall();
        	}
        	
        	phoneRang = false;
        	phoneAnswered = false;
        }
	}
}