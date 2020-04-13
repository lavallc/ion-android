package io.lava.ion.receivers;

import io.lava.ion.services.firmware.FirmwareUpdateChecker;
import io.lava.ion.services.weather.WeatherService;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AlarmScheduler {
	public static final String ACTION_DISCOVER = "io.lava.ion.DISCOVER";
	public static final String ACTION_WEATHER_UPDATE = "io.lava.ion.WEATHER_UPDATE";
	public static final String ACTION_FIRMWARE_UPDATE = "io.lava.ion.FIRMWARE_UPDATE";
	
	// intervals
    private static final long DISCOVERY_INTERVAL = 1000 * 60 * 5; 	// 5 minutes
	
	private static PendingIntent discoverIntent, weatherUpdateIntent, firmwareUpdateIntent;
	
	public static void setupAlarms(Context context) {
		AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		
		// only set up background discovery if the preference is checked
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		boolean backgroundDiscovery = sharedPref.getBoolean("background_discovery", false);
    	
    	if (discoverIntent == null) {
    		// background discovery is on, we need to schedule the alarm
			if (backgroundDiscovery) {
				discoverIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_DISCOVER), 0);
				alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, DISCOVERY_INTERVAL, discoverIntent);
			}
    	} else {
    		// background discovery is off, we need to cancel the alarm
    		if (!backgroundDiscovery) {
    			alarmMgr.cancel(discoverIntent);
    			discoverIntent = null;
    		}
    	}
    	
    	if (weatherUpdateIntent == null) {
    		weatherUpdateIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_WEATHER_UPDATE), 0);
    		
    		// weather update
    		alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, AlarmManager.INTERVAL_FIFTEEN_MINUTES, weatherUpdateIntent);
    	}
    	
    	if (firmwareUpdateIntent == null) {
    		firmwareUpdateIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_FIRMWARE_UPDATE), 0);
    		
    		// firmware update check
        	alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, AlarmManager.INTERVAL_DAY, firmwareUpdateIntent);
    	}
    	
    	// fire up our services (if they aren't already)
		scheduleWeatherJob(context);
		//Intent intent = new Intent(context, WeatherService.class);
    	//context.startService(intent);

    	// we aren't going to push more FW. no need to keep this enabled
    	//intent = new Intent(context, FirmwareUpdateChecker.class);
    	//context.startService(intent);
	}

	public static void scheduleWeatherJob(Context context) {
		ComponentName serviceComponent = new ComponentName(context, WeatherService.class);
		JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
		builder.setPeriodic(1000 * 60 * 30);
		builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
		builder.setRequiresCharging(false); // we don't care if the device is charging or not
		JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
		jobScheduler.schedule(builder.build());
	}
	
	public static void removeAlarms(Context context) {
		AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		
		if (discoverIntent != null) {
			alarmMgr.cancel(discoverIntent);
			discoverIntent = null;
		}
		
		if (weatherUpdateIntent != null) {
			alarmMgr.cancel(weatherUpdateIntent);
			weatherUpdateIntent = null;
		}
		
		if (firmwareUpdateIntent != null) {
			alarmMgr.cancel(firmwareUpdateIntent);
			firmwareUpdateIntent = null;
		}
	}
}
