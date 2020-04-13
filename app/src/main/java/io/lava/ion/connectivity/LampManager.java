package io.lava.ion.connectivity;

import io.lava.ion.IonMainActivity;
import io.lava.ion.NotificationFactory;
import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.firmware.DFULamp;
import io.lava.ion.logger.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Toast;


public class LampManager implements ILampDiscoveryDelegate, OnSharedPreferenceChangeListener {
	// singleton instance of this class
	private static LampManager instance;
	
	// anyone interested in lamp manager changes/updates will be in this list
	private ArrayList<ILampManagerListener> listeners;

	// all discovered lamps are held here
	private ArrayList<Lamp> nonDfuLamps;
	private ArrayList<DFULamp> dfuLamps;
	
	// our discovery class that creates lamp objects
	private LampDiscoverer discoverer;
	
	// used to keep track of remembered lamps that are offline
	private static final String PREFS_NAME = "lamps";
	private HashSet<String> rememberedLamps;
	
	// timer to check for stale lamps
	private Runnable staleLampRunnable;
	
	// stale lamp check interval
	private static final long CLEAR_STALE_LAMPS_INTERVAL = 1000 * 3;	// 3 seconds
	
	// i'm sure this will be useful
	private Context mContext;
	
	// hold reference to main activity so we can switch fragments when updates begin
	private IonMainActivity ionActivity;
	
	// used for ensuring we talk to BLEController via UI thread
	private Handler mCallbackHandler;

	public static LampManager createInstanceIfNeeded(Context context) {
		if (context != null) {
			if (instance == null) {
				instance = new LampManager();
				
				// store context
				instance.mContext = context;
				
				// create discoverer
				instance.discoverer = LampDiscoverer.getInstance(context);
				
				// load up our list of remembered lamps
				SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
				instance.rememberedLamps = new HashSet<String>(settings.getStringSet("lamplist", new HashSet<String>()));
				
				// create lamp objects for offline lamps
				for (String lampMacAddr : instance.rememberedLamps) {
					String lampName = settings.getString(lampMacAddr, "");
					
					if (!lampName.equals("")) {
						Lamp l = new Lamp(context, lampName, lampMacAddr);
						instance.nonDfuLamps.add(l);
					}
				}
				
				PreferenceManager.getDefaultSharedPreferences(instance.mContext).registerOnSharedPreferenceChangeListener(instance);
			}
				
			return instance;
		} else {
			return null;
		}
	}
	
	public static LampManager getInstanceIfReady() {
		return instance;
	}

	private LampManager() {
		// create a handler on THIS THREAD (should be our app's UI thread)
		mCallbackHandler = new Handler(Looper.getMainLooper());
		
		listeners = new ArrayList<ILampManagerListener>();

		nonDfuLamps = new ArrayList<Lamp>();
		dfuLamps = new ArrayList<DFULamp>();
		
		staleLampRunnable = new Runnable() {
			@Override
			public void run() {
				/* NON DFU */
				boolean removedLamps = false;
				
				// check for stale lamps that should not be in the list
				Iterator<Lamp> iter = nonDfuLamps.iterator();
				while (iter.hasNext()) {
					try {
						Lamp l = iter.next();
						
					    if (!l.seenRecently() && !l.isRemembered()) {
					    	l.onDestroy();
					        iter.remove();
					        
					        removedLamps = true;
					    }
					} catch (NoSuchElementException e) {
						// ignore
					}
				}
				
				// notify everyone of updates
				if (removedLamps) {
                    notifyListenersOfLampListUpdates();

                    // cancel or create notification if need be
                    refreshNotificationControls();
                }
				
				
				/* DFU */
				boolean removedDfuLamps = false;
				
				// check for stale lamps that should not be in the list
				Iterator<DFULamp> dfuIter = dfuLamps.iterator();
				while (dfuIter.hasNext()) {
					try {
						DFULamp dl = dfuIter.next();
						
					    if (!dl.seenRecently()) {
					        dfuIter.remove();
					        
					        removedDfuLamps = true;
					    }
					} catch (NoSuchElementException e) {
						// ignore
					}
				}
				
				if (removedDfuLamps) {
					// let UI know that dfu lamps updated
					notifyListenersOfDfuLampListUpdates();
				}
				
				mCallbackHandler.postDelayed(staleLampRunnable, CLEAR_STALE_LAMPS_INTERVAL);
			}
		};
		
		mCallbackHandler.post(staleLampRunnable);
	}
	
	public void setMainActivity(IonMainActivity activity) {
		this.ionActivity = activity;
	}
	
	public void switchToUpdateFragment() {
		// jump to update fragment (4)
		if (ionActivity != null) {
			ionActivity.selectFragment(4);
		}
	}
	
	public void clearStaleLamps() {
		mCallbackHandler.removeCallbacks(staleLampRunnable);
		mCallbackHandler.post(staleLampRunnable);
	}
	
	private void saveRememberedLamps() {
		SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.putStringSet("lamplist", rememberedLamps);
		
		editor.commit();
	}
	
	public void addedLampToAutoConnectList(String macAddr) {
		rememberedLamps.add(macAddr);
		
		saveRememberedLamps();
	}
	
	public void removedLampFromAutoConnectList(String macAddr) {
		rememberedLamps.remove(macAddr);
		
		saveRememberedLamps();
	}
	
	// useful for knowing if we should scan in the background or not (allows us to avoid scanning for nothing)
	public boolean shouldBackgroundDiscoveryRun() {
		// do we actually have any remembered lamps?
		if (rememberedLamps.size() == 0)
			return false;
		
		// have we connected to all remembered lamps?
		for (Lamp l : nonDfuLamps) {
			if (l.isRemembered() && l.getConnectivityState() == Lamp.LampState.DISCONNECTED) {
				// we found a remembered lamp that is not connected
				return true;
			}
		}
		
		return false;
	}
	
	public boolean areMultipleLampsSelected() {
		return (getReadyLamps().size() > 1);
	}
	
	public boolean isSingleLampSelected() {
		return (getReadyLamps().size() == 1);
	}
	
	public boolean areNoLampsSelected() {
		return (getReadyLamps().size() == 0);
	}

	public void addListener(ILampManagerListener listener) {
		if (listener != null)
			listeners.add(listener);
	}
	
	public void removeListener(ILampManagerListener removeListener) {
		for (ILampManagerListener listener : listeners) {
			if (removeListener == listener) {
				listeners.remove(removeListener);
				return;
			}
		}
	}
	
	private void connectToLampIfAutoConnectEnabled(Lamp lamp) {
		// disable auto connect while updating is happening
		if (dfuLamps.size() > 0)
			return;
		
		// if any lamps are out of date, we should also suspend auto connect
		for (Lamp l : nonDfuLamps) {
			if (l.isLampOutOfDate())
				return;
		}
		
		if (lamp.isRemembered() && lamp.getConnectivityState() == Lamp.LampState.DISCONNECTED && !lamp.isAppOutOfDate()) {
			lamp.connect();
		}
	}
	
	public void notifyListenersOfLampListUpdates() {
		Logger.i(this.getClass().getSimpleName(), "lamp list changed");
		
		for (ILampManagerListener listener : listeners) {
			// let all our listeners that a lamp's details changed
			try {
				listener.onLampListUpdate();
			} catch (Exception e) {}
		}
	}
	
	public void notifyListenersOfDfuLampListUpdates() {
		Logger.i(this.getClass().getSimpleName(), "dfu lamp list changed");
		
		for (ILampManagerListener listener : listeners) {
			// let all our listeners that a lamp's details changed
			try {
				listener.onDfuLampListUpdate();
			} catch (Exception e) {}
		}
	}

	@Override
	public void didDiscoverLamp(Lamp newLamp) {
		Logger.i(this.getClass().getSimpleName(), "lamp manager received: " + newLamp.getName());
		
		// connect to this lamp if the user has selected it in the past
		connectToLampIfAutoConnectEnabled(newLamp);
		
		// add lamp to lamps arraylist
		nonDfuLamps.add(newLamp);
		
		// let everyone know we have a new lamp
		notifyListenersOfLampListUpdates();
	}
	
	public void discoverLamps() {
		discoverer.discoverLamps(this);
		
		// clear stale lamps in case we're running in the background and our handler has been sleeping
		mCallbackHandler.removeCallbacks(staleLampRunnable);
		mCallbackHandler.post(staleLampRunnable);
	}
	
	public void suspendDiscovery() {
		discoverer.suspend();
	}
	
	public void resumeDiscovery() {
		discoverer.resume();
	}
	
	public void setLampDiscoveryContinuous(boolean continuous) {
		if (continuous) {
			discoverer.enableContinuous();
			discoverLamps();
		} else {
			discoverer.disableContinuous();
		}
	}
	
	public void stopDiscovery() {
		discoverer.stopDiscovery();
	}
	
	public boolean isDiscovering() {
		return discoverer.isDiscovering();
	}
	
	public ArrayList<Lamp> getAllLamps() {
		return nonDfuLamps;
	}
	
	public ArrayList<DFULamp> getAllDfuLamps() {
		return dfuLamps;
	}
	
	public void disconnectFromAllLamps() {
		for (Lamp l : nonDfuLamps) {
			if (l.getConnectivityState() != Lamp.LampState.DISCONNECTED)
				l.disconnect();
		}
	}

	public ArrayList<Lamp> getReadyLamps() {
		ArrayList<Lamp> readyLamps = new ArrayList<Lamp>();

		for (Lamp l : nonDfuLamps) {
			if (l.getConnectivityState() == Lamp.LampState.READY)
				readyLamps.add(l);
		}

		return readyLamps;
	}
	
	public Lamp getSelectedLampIfExists() {
		// may be null
		if (getReadyLamps().size() == 1)
			return getReadyLamps().get(0);
		else
			return null;
	}
	
	public void notifyWeatherLocationChange() {
		mCallbackHandler.post(new Runnable() {
	        public void run() {
	        	for (ILampManagerListener listener : listeners) {
	    			// let all our listeners know that the weather has been updated
	    			try {
	    				listener.onWeatherLocationUpdate();
	    			} catch (Exception e) {}
	    		}
	        }
	    });
	}
	
	public void sendWeatherAndTimeToConnectedLamps(final Runnable complete) {
		// send time/weather to all connected lamps
		mCallbackHandler.post(new Runnable() {
	        public void run() {
	        	for (Lamp l : getReadyLamps()) {
	    			l.updateTimeAndWeather(complete);
	    		}
	        }
	    });
	}
	
	public void sendNotificationToConnectedLamps(final int notificationId) {
		// send notification to all connected lamps
		mCallbackHandler.post(new Runnable() {
	        public void run() {
	        	for (Lamp l : getReadyLamps()) {
	    			l.triggerNotification(notificationId);
	    		}
	        }
	    });
	}


	@Override
	public boolean isLampKnown(String macAddress) {
		for (Lamp l : nonDfuLamps) {
			if (l.getMacAddress().equals(macAddress))
				return true;
		}
		return false;
	}

	@Override
	public void didSeeLamp(BluetoothDevice device, double rssi) {
		Logger.i(this.getClass().getSimpleName(), "lamp manager received update: " + device.getName());
		
		for (Lamp l : nonDfuLamps) {
			if (l.getMacAddress().equals(device.getAddress())) {
				// this lamp has already been found, just update its rssi and name
				l.didReadRemoteDevice(device);
				l.didReadRemoteRssi(rssi);
				
				// if we somehow got disconnected, reconnect
				connectToLampIfAutoConnectEnabled(l);
			}
		}	
	}
	
	private void selectLampsOnCountChange() {
		Logger.i(this.getClass().getSimpleName(), "lamp count change");
		
		// callbacks for new lamp manager state
		if (getReadyLamps().size() > 1) {
			for (ILampManagerListener listener : listeners) {
				try {
					listener.multipleLampsSelected(getReadyLamps());
				} catch (Exception e) {}
			}
		} else if (getReadyLamps().size() == 1) {
			for (ILampManagerListener listener : listeners) {
				try {
					listener.singleLampSelected(getReadyLamps().get(0));
				} catch (Exception e) {}
			}
		} else {
			for (ILampManagerListener listener : listeners) {
				try {
					listener.noLampsSelected();
				} catch (Exception e) {}
			}
		}

        // cancel or create notification if need be
        refreshNotificationControls();
	}
	
	// called by lamps
	public void lampMoodDidChange(int moodId) {
		for (ILampManagerListener listener : listeners) {
			// let all our listeners know that we found a new lamp
			try {
				listener.onMoodChange(moodId);
			} catch (Exception e) {}
		}
	}
	
	// this is called via the Lamp objects themselves
	public void lampReadyStateChanged(Lamp lamp) {
		Logger.i(this.getClass().getSimpleName(), "lamp ready state change");
		
		if (lamp.getConnectivityState() == Lamp.LampState.READY)
			Toast.makeText(mContext, "connected to " + lamp.getName(), Toast.LENGTH_LONG).show();
		else if (lamp.getConnectivityState() == Lamp.LampState.DISCONNECTED)
			Toast.makeText(mContext, lamp.getName() + " disconnected", Toast.LENGTH_LONG).show();
		
		// reselect lamps
		selectLampsOnCountChange();
		
		// let everyone know we have new information
		notifyListenersOfLampListUpdates();
	}

    public void refreshLampStatus() {
        // clear out any stale lamps
        clearStaleLamps();

        // reselect lamps
        selectLampsOnCountChange();

        // let everyone know we have new information
        notifyListenersOfLampListUpdates();
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (key.equals("notification_bar")) {
            // cancel or create notification if need be
			refreshNotificationControls();
		}
	}

    public void refreshNotificationControls() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean notificationBar = prefs.getBoolean("notification_bar", true);

        if (notificationBar && getReadyLamps().size() >= 1) {
            NotificationFactory.createOrUpdateNotification(mContext);
        } else {
            NotificationFactory.cancelNotificationIfExists(mContext);
        }
    }

	@Override
	public void didDiscoverDfuLamp(DFULamp lamp) {
		Logger.i(this.getClass().getSimpleName(), "lamp manager received dfu lamp: " + lamp.getMacAddress());
		
		// add lamp to dfu lamps arraylist
		dfuLamps.add(lamp);
		
		// let everyone know we have a new lamp
		notifyListenersOfDfuLampListUpdates();
	}

	@Override
	public void didSeeDfuLamp(BluetoothDevice device, double rssi) {
		Logger.i(this.getClass().getSimpleName(), "lamp manager received dfu lamp update: " + device.getAddress());
		
		for (DFULamp l : dfuLamps) {
			if (l.getMacAddress().equals(device.getAddress())) {
				// this lamp has already been found, update timestamp
				l.wasSeen();
			}
		}
	}

	@Override
	public boolean isDfuLampKnown(String macAddress) {
		for (DFULamp l : dfuLamps) {
			if (l.getMacAddress().equals(macAddress))
				return true;
		}
		return false;
	}
}