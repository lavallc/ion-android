package io.lava.ion.services.firmware;

import io.lava.ion.logger.Logger;
import io.lava.ion.wakelock.WakeLockManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class FirmwareUpdateChecker extends Service {
	// lava production app id in use
	private static final String JSON_LATEST_URL = "https://s3.amazonaws.com/lumenfirmware/latest.json";

    // only allow one weather request at a time
    private AtomicBoolean requestingFirmwareJSON = new AtomicBoolean(false);

    // timer for updating firmware json data periodically
    private Runnable firmwareUpdateRunnable;
    
    private static final long FIRMWARE_FAILED_UPDATE_RECHECK_INTERVAL = 1000 * 60; // 1 minute
    private static final long RECHECK_CONNECTIVITY_INTERVAL = 1000 * 3; // 3 seconds
    
    private static FirmwareUpdateChecker instance;
    
    private Handler mUIHandler;

    @Override
    public void onCreate() {
    	super.onCreate();
    	
    	instance = this;
    	
    	mUIHandler = new Handler(Looper.getMainLooper());
    	
    	firmwareUpdateRunnable = new Runnable() {
			@Override
			public void run() {
				// one request at a time
	        	if (requestingFirmwareJSON.compareAndSet(false, true)) {
	                new Thread() {
	                    @Override
	                    public void run() {
	                    	// check for valid state
                    		if (!isNetwork()) {
                    			mUIHandler.removeCallbacks(firmwareUpdateRunnable);
                    			mUIHandler.postDelayed(firmwareUpdateRunnable, RECHECK_CONNECTIVITY_INTERVAL);
                    		} else {
                    			try {
                    				FirmwareManager.getInstance(instance).updateStatusText("Contacting server...");
                    				
    	                    		final JSONObject firmwareJSON = getJSONFromURL(JSON_LATEST_URL);
    	                    		
    	                    		if (firmwareJSON != null) {
    	                    			// feed new JSON to firmware data class
    	                    			FirmwareManager.getInstance(instance).receivedNewJSON(firmwareJSON);
    	                    			
    	                    			mUIHandler.removeCallbacks(firmwareUpdateRunnable);
    	                    		} else {
    	                    			Logger.i(getClass().getSimpleName(), "BAD JSON, retrying soon...");
    	                    			
    	                    			FirmwareManager.getInstance(instance).updateStatusText("Failed to contact update server");
    	                    			
    	                    			// update failed
    	                    			mUIHandler.removeCallbacks(firmwareUpdateRunnable);
    	                    			mUIHandler.postDelayed(firmwareUpdateRunnable, FIRMWARE_FAILED_UPDATE_RECHECK_INTERVAL);
    	                    		}
    	                        } catch (Exception e) {
    	                        	// something bad happened
    	                        	Logger.i(getClass().getSimpleName(), e.toString());
    	                        	
    	                        	FirmwareManager.getInstance(instance).updateStatusText("Failed to contact update server");
    	                        	
    	                        	// update failed
                        			mUIHandler.removeCallbacks(firmwareUpdateRunnable);
                        			mUIHandler.postDelayed(firmwareUpdateRunnable, FIRMWARE_FAILED_UPDATE_RECHECK_INTERVAL);
    	                        }
                    		}
	                        
                    		// release wake lock
                        	WakeLockManager.getInstance(instance).setUpdatingFirmwareMetadata(false);
	                        requestingFirmwareJSON.set(false);
	                    }
	                }.start();
	            }
			}
    	};
    	
    	mUIHandler.post(firmwareUpdateRunnable);
    }
    
    @Override
    public void onDestroy() {
    	instance = null;
    	
    	mUIHandler.removeCallbacks(firmwareUpdateRunnable);
        
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
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
    
    public static FirmwareUpdateChecker getInstanceIfExists() {
    	return instance;
    }
    
    public boolean forceUpdate() {
    	// we are already attempting to update
    	if (requestingFirmwareJSON.get())
    		return true;
    	
    	// grab wake lock
    	WakeLockManager.getInstance(instance).setUpdatingFirmwareMetadata(true);
    	
    	if (firmwareUpdateRunnable != null) {
    		mUIHandler.removeCallbacks(firmwareUpdateRunnable);
			mUIHandler.post(firmwareUpdateRunnable);
			return true;
    	}
    	return false;
    }

    private boolean isNetwork() {
    	ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo mNetworkInfo = connManager.getActiveNetworkInfo();

		// do we have a network connection?
		if (mNetworkInfo == null || !mNetworkInfo.isConnected()) {
			FirmwareManager.getInstance(instance).updateStatusText("Waiting for network...");
			return false;
		}
		
		// yes we do
		return true;
	}

    // no binding
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
