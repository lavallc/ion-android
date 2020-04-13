package io.lava.ion;

import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.logger.Logger;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class NotificationFactory extends BroadcastReceiver {
	static final String ACTION_ON_OFF = "io.lava.ion.ON_OFF";
	static final String ACTION_NEXT_MOOD = "io.lava.ion.NEXT_MOOD";
	static final String ACTION_CLEAR_NOTIFICATION = "io.lava.ion.CLEAR_NOTIFICATION";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action  = intent.getAction();
		
		if (action.equals(ACTION_ON_OFF)) {
			for (Lamp l : LampManager.getInstanceIfReady().getReadyLamps()) {
				l.togglePower();
			}
		} else if (action.equals(ACTION_NEXT_MOOD)) {
			for (Lamp l : LampManager.getInstanceIfReady().getReadyLamps()) {
				l.nextMood();
			}
		} else if (action.equals(ACTION_CLEAR_NOTIFICATION)) {
			for (Lamp l : LampManager.getInstanceIfReady().getReadyLamps()) {
				l.clearNotification();
			}
		} else {
			Logger.e("NotificationReceiver", "NotificationReceiver got a bad intent.");
		}
	}
	
	public static Notification createOrUpdateNotification(Context activity) {
	    // Sends a signal to toggle all lamps on/off
        Intent onOffIntent = new Intent(ACTION_ON_OFF);
		
        // Sends a signal to go to the next pattern
        Intent nextMoodIntent = new Intent(ACTION_NEXT_MOOD);
        
        // Sends a signal to clear sticky notification
        Intent clearNotificationIntent = new Intent(ACTION_CLEAR_NOTIFICATION);
		
        // Make the notification
		Notification notification = new Notification.Builder(activity)
				.setSmallIcon(R.drawable.ion_notif)
				.setContentTitle("ION is nearby")
				.setOngoing(true)
				.setPriority(Notification.PRIORITY_HIGH)
				.setContentText("Your wish is my command")
				.setContentIntent(PendingIntent.getActivity(activity, 0 , new Intent(activity, IonMainActivity.class), 0))
                .addAction(R.drawable.power_notif, "On/Off", PendingIntent.getBroadcast(activity, 0, onOffIntent, 0))
                .addAction(R.drawable.nextmood_notif, "Next", PendingIntent.getBroadcast(activity, 0, nextMoodIntent, 0))
                .addAction(R.drawable.clearnotifications_notif, "Clear", PendingIntent.getBroadcast(activity, 0, clearNotificationIntent, 0))
                .build();
		 
		// Actually post the notification
		NotificationManager mNotificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(0, notification);
		
		return notification;
	}
	
	public static void cancelNotificationIfExists(Context activity) {
		NotificationManager mNotificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
	}
	
	public static boolean checkForNotificationPermission(Context activity) {
		ContentResolver contentResolver = activity.getContentResolver();
		String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
		String packageName = activity.getPackageName();

		// check to see if the enabledNotificationListeners String contains our package name
		if (enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName)) {
			return false;
		} else {
			return true;
		}
	}
	
	public static void launchNotificationPermissionSettings(Context activity) {
		Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
		activity.startActivity(intent);
	}
}
