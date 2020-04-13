package io.lava.ion.receivers;

import io.lava.ion.services.weather.WeatherService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class IONWeatherUpdateReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		// tell weather to update
		if (WeatherService.getInstanceIfExists() != null && WeatherService.getInstanceIfExists().isNetworkLocationAndLamps()) {
			WeatherService.getInstanceIfExists().forceUpdate();
		}
	}
}