package io.lava.ion.services.weather;

import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.PacketConstants;
import io.lava.ion.logger.Logger;
import io.lava.ion.wakelock.WakeLockManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.ContextCompat;

public class WeatherService extends Service {
	// identify ourselves
	private static final String TAG = "WeatherService";
	
	// lava production app id in use
	private static final String WEATHER_API_URL = "http://api.openweathermap.org/data/2.5/weather?units=imperial&APPID=198dee814b0b3b807c12b0ee69dec1de&cnt=1";
	private static final String FORECAST_API_URL = "http://api.openweathermap.org/data/2.5/forecast?units=imperial&APPID=198dee814b0b3b807c12b0ee69dec1de&cnt=2";

    // intervals
    private static final long WEATHER_FAILED_UPDATE_RECHECK_INTERVAL = 1000 * 60 * 3; // 3 minutes
    private static final long RECHECK_CONNECTIVITY_INTERVAL = 1000 * 3; // 3 seconds
    
    private static WeatherService instance;
    
    private int numLocationChecks = 0;

    // only allow one weather request at a time
    private static AtomicBoolean requestingWeather = new AtomicBoolean(false);
    
    private LocationManager locationManager;
    private LocationListener locationListener;
    private double lat, lon;
    private boolean haveLocation = false;
    
    private Handler mUIHandler;

    // runnable for updating weather data periodically
    private Runnable weatherRunnable;

	private boolean canAccessLocation () {
		return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
	}

    @Override
    public void onCreate() {
    	super.onCreate();
    	
    	instance = this;
    	
    	mUIHandler = new Handler(Looper.getMainLooper());
    	
    	weatherRunnable = new Runnable() {
			@Override
			public void run() {
				// one request at a time
	        	if (requestingWeather.compareAndSet(false, true)) {
	                new Thread() {
	                    @Override
	                    public void run() {
	                    	if (WeatherData.getInstance().dataIsValid()) {
	                    		// no need to update!
	                    		
	                    		// release wake lock
								WakeLockManager.getInstance(instance).setUpdatingWeather(false);
	                    	} else if (!isNetworkLocationAndLamps()) { // check for valid state to update
                    			// recheck again shortly
	                        	mUIHandler.removeCallbacks(weatherRunnable);
                    			mUIHandler.postDelayed(weatherRunnable, RECHECK_CONNECTIVITY_INTERVAL);
                    			
                    			// release wake lock
								WakeLockManager.getInstance(instance).setUpdatingWeather(false);
                    		} else {
                    			try {
    	                    		WeatherData.getInstance().updateLocation("Updating..." + getTimestampString());

    	                			String weatherString = WEATHER_API_URL+"&lat="+Double.toString(lat)+"&lon="+Double.toString(lon);
    	                			String forecastString = FORECAST_API_URL+"&lat="+Double.toString(lat)+"&lon="+Double.toString(lon);
    	                			
    	                    		JSONObject currentConditionsJSON = getJSONFromURL(weatherString);
    	                    		JSONObject forecastConditionsJSON = getJSONFromURL(forecastString);
    	                    		
    	                    		if (currentConditionsJSON != null && forecastConditionsJSON != null) {
    	                    			String location = currentConditionsJSON.getString("name");
    	                    			
    	                    			WeatherData.getInstance().updateLocation(location + getTimestampString());
    	                    			
    	                    			// parse out current temp
    	                    			int currentTemp = coerceTemp(currentConditionsJSON.getJSONObject("main").getDouble("temp"));
    	                    			// parse out current conditions
    	                    			int currentConditions = getWeatherCodeForIconString(currentConditionsJSON.getJSONArray("weather").getJSONObject(0).getString("icon"));
    	                    			
    	                    			Logger.i(TAG, "CURRENT TEMP: " + Integer.toString(currentTemp));
    	                    			Logger.i(TAG, "CURRENT CONDITIONS: " + Integer.toString(currentConditions));
    	                    			
    	                    			// parse out forecast temp (6 hours from now - 2nd item in array)
    	                    			int futureTemp = coerceTemp(forecastConditionsJSON.getJSONArray("list").getJSONObject(1).getJSONObject("main").getDouble("temp"));
    	                    			// parse out forecast conditions (6 hours from now - 2nd item in array)
    	                    			int futureConditions = getWeatherCodeForIconString(forecastConditionsJSON.getJSONArray("list").getJSONObject(1).getJSONArray("weather").getJSONObject(0).getString("icon"));
    	                    			
    	                    			Logger.i(TAG, "FUTURE TEMP: " + Integer.toString(futureTemp));
    	                    			Logger.i(TAG, "FUTURE CONDITIONS: " + Integer.toString(futureConditions));
    	                    			
    	                    			// parse out sunrise/sunset unix timestamps
    	                    			long sunriseTimestamp = currentConditionsJSON.getJSONObject("sys").getLong("sunrise");
    	                    			long sunsetTimestamp = currentConditionsJSON.getJSONObject("sys").getLong("sunset");
    	                    			
    	                    			// offset sunrise by Android's current timezone
    	                    			Calendar sunriseCal = Calendar.getInstance(TimeZone.getDefault());
    	                    			sunriseCal.setTimeInMillis(sunriseTimestamp * 1000);
    	                    			
    	                    			// offset sunset by Android's current timezone
    	                    			Calendar sunsetCal = Calendar.getInstance(TimeZone.getDefault());
    	                    			sunsetCal.setTimeInMillis(sunsetTimestamp * 1000);
    	                    			
    	                    			// hours/minutes to be sent to lamps
    	                    			int sunrise24Hr = sunriseCal.get(Calendar.HOUR_OF_DAY);
    	                    			int sunriseMin = sunriseCal.get(Calendar.MINUTE);
    	                    			
    	                    			int sunset24Hr = sunsetCal.get(Calendar.HOUR_OF_DAY);
    	                    			int sunsetMin = sunsetCal.get(Calendar.MINUTE);
    	                    			
    	                    			// send all of this to the WeatherData singleton
    	                    			WeatherData.getInstance().updateData(currentTemp, currentConditions, futureTemp, futureConditions, sunrise24Hr, sunriseMin, sunset24Hr, sunsetMin);
    	                    			
    	                    			if (LampManager.getInstanceIfReady() != null) {
    	                    				// tell the lamp manager that the weather was just updated
    	                    				LampManager.getInstanceIfReady().sendWeatherAndTimeToConnectedLamps(new Runnable() {
    											@Override
    											public void run() {
    												// release wake lock
    												WakeLockManager.getInstance(instance).setUpdatingWeather(false);
    											}
    	                                                 				});
    	                    			} else {
    	                    				// release wake lock
											WakeLockManager.getInstance(instance).setUpdatingWeather(false);
    	                    			}
    	                    			
    	                    			mUIHandler.removeCallbacks(weatherRunnable);
    	                    		} else {
    	                    			Logger.i(TAG, "BAD JSON, retrying soon...");
    	                    			
    	                    			if (!WeatherData.getInstance().dataIsValid())
    	                    				WeatherData.getInstance().updateLocation("Weather data unavailable" + getTimestampString());
    	                    			
    	                    			// update failed
    	                    			mUIHandler.removeCallbacks(weatherRunnable);
    	                    			mUIHandler.postDelayed(weatherRunnable, WEATHER_FAILED_UPDATE_RECHECK_INTERVAL);
    	                    			
    	                    			// release wake lock
										WakeLockManager.getInstance(instance).setUpdatingWeather(false);
    	                    		}
    	                        } catch (Exception e) {
    	                        	// something bad happened
    	                        	Logger.i(TAG, e.toString());
    	                        	
    	                        	if (!WeatherData.getInstance().dataIsValid())
    	                        		WeatherData.getInstance().updateLocation("Weather data unavailable" + getTimestampString());
    	                        	
    	                        	// update failed
    	                        	mUIHandler.removeCallbacks(weatherRunnable);
                        			mUIHandler.postDelayed(weatherRunnable, WEATHER_FAILED_UPDATE_RECHECK_INTERVAL);
                        			
                        			// release wake lock
									WakeLockManager.getInstance(instance).setUpdatingWeather(false);
    	                        }
                    		}
	                        
	                        requestingWeather.set(false);
	                    }
	                }.start();
	            }
			}
            	};
    	
    	// create the lamp manager in all its application context glory (tied to the system process)
    	LampManager.createInstanceIfNeeded(getApplicationContext());
        
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        
        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
            	// Called when a new location is found by the network location provider.
            	lat = location.getLatitude();
                lon = location.getLongitude();
            	haveLocation = true;
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            	if (status != LocationProvider.AVAILABLE) {
            		haveLocation = false;
            	}
            }

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        // Register the listener with the Location Manager to receive location updates (1 second, ~1 mile)
        if (canAccessLocation()) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1600, locationListener);
        }
        
        // start updating
        mUIHandler.post(weatherRunnable);
    }
    
    public static WeatherService getInstanceIfExists() {
    	return instance;
    }
    
    public boolean forceUpdate() {
        // make sure our lamp status (and notification control) is still relevant
        LampManager.getInstanceIfReady().refreshLampStatus();

    	// we are already attempting to update
    	if (requestingWeather.get())
    		return true;
    	
    	// grab wake lock
    	WakeLockManager.getInstance(instance).setUpdatingWeather(true);
    	
    	if (weatherRunnable != null) {
    		mUIHandler.removeCallbacks(weatherRunnable);
			mUIHandler.post(weatherRunnable);
			return true;
    	}
    	return false;
    }
    
    @Override
    public void onDestroy() {
        instance = null;
    	
    	if (weatherRunnable != null) {
        	mUIHandler.removeCallbacks(weatherRunnable);
        }
        
        locationManager.removeUpdates(locationListener);
        
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
    
    private int coerceTemp(double temp) {
    	int currentTempInt = (int)Math.round(temp);
    	int result;
		
		if (currentTempInt > 127)
			result = 127;
		else if (currentTempInt < -128)
			result = -128;
		else
			result = currentTempInt;
		
		return result;
    }
    
    private int getWeatherCodeForIconString(String weatherIcon) {
    	if (weatherIcon.equals("01d") || weatherIcon.equals("01n") || weatherIcon.equals("02d") || weatherIcon.equals("02n")) {
			return PacketConstants.WeatherCodes.clear;
		} else if (weatherIcon.equals("03d") || weatherIcon.equals("03n") || weatherIcon.equals("04d") || weatherIcon.equals("04n")) {
			return PacketConstants.WeatherCodes.clouds;
		} else if (weatherIcon.equals("09d") || weatherIcon.equals("09n") || weatherIcon.equals("10d") || weatherIcon.equals("10n")) {
			return PacketConstants.WeatherCodes.rain;
		} else if (weatherIcon.equals("11d") || weatherIcon.equals("11n")) {
			return PacketConstants.WeatherCodes.thunderstorm;
		} else if (weatherIcon.equals("13d") || weatherIcon.equals("13n")) {
			return PacketConstants.WeatherCodes.snow;
		} else {
			return PacketConstants.WeatherCodes.hazy;
		}
    }
    
    private JSONObject getJSONFromURL(String url) throws IOException {
    	HttpParams httpParameters = new BasicHttpParams();
    	
    	// Set the timeout in milliseconds until a connection is established.
    	// The default value is zero, that means the timeout is not used. 
    	int timeoutConnection = 5000;
    	HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
    	
    	// Set the default socket timeout (SO_TIMEOUT) 
    	// in milliseconds which is the timeout for waiting for data.
    	int timeoutSocket = 5000;
    	HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

    	DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
    	
        HttpResponse response = httpClient.execute(new HttpGet(url));
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();
            String responseString = out.toString();
            
            // parse JSON
			try {
				// create JSON object
				JSONObject json = new JSONObject(responseString);
				
				// return JSON
				return json;
			} catch (JSONException e) {
				// failed to parse, allow null to be returned
			}
        } else {
            // close connection
            response.getEntity().getContent().close();
        }
        
        // we didn't get data for some reason
		return null;
    }
    
    private String getTimestampString() {
    	SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mma");
		Date date = new Date();
		return "\n" + dateFormat.format(date);
    }

    public boolean isNetworkLocationAndLamps() {
    	ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo mNetworkInfo = connManager.getActiveNetworkInfo();

		// wait for valid state to update weather
		if (mNetworkInfo == null || !mNetworkInfo.isConnected() || !haveLocation || LampManager.getInstanceIfReady().getReadyLamps().size() == 0) {
			if (mNetworkInfo == null || !mNetworkInfo.isConnected()) {
				if (!WeatherData.getInstance().dataIsValid())
					WeatherData.getInstance().updateLocation("Waiting for network connection" + getTimestampString());
			} else if (!haveLocation) {
				numLocationChecks++;
				if (!WeatherData.getInstance().dataIsValid())
					WeatherData.getInstance().updateLocation("Waiting for location information" + getTimestampString());
			} else {
				if (!WeatherData.getInstance().dataIsValid())
					WeatherData.getInstance().updateLocation("Waiting for ION to connect" + getTimestampString());
			}
			
			// we've waited long enough for location, let's go with our best guess
			if (numLocationChecks > 3) {
				Location location = null;

                if (canAccessLocation()) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
				
				if (location != null && mNetworkInfo != null && mNetworkInfo.isConnected() && LampManager.getInstanceIfReady().getReadyLamps().size() > 0) {
					lat = location.getLatitude();
					lon = location.getLongitude();
					
					numLocationChecks = 0;
					
					return true;
				}
			}
			
			// not ready
			return false;
		}
		
		// we are ready
		return true;
	}

    // no binding
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
