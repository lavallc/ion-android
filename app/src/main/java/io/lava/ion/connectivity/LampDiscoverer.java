package io.lava.ion.connectivity;

import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.firmware.DFULamp;
import io.lava.ion.logger.Logger;
import io.lava.ion.utility.Now;
import io.lava.ion.wakelock.WakeLockManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class LampDiscoverer {
	private ILampDiscoveryDelegate delegate;
	private boolean isDiscovering = false;
	private boolean continuousDiscovery = false;
	
	private long lastDiscoveryCompletion = 0;
	
	private Handler mCallbackHandler;
	
	private Context mContext;
	
	private boolean suspend = false;
	
	public static final int DISCOVERY_PERIOD_SECONDS = 3;
	
	// singleton instance
	private static LampDiscoverer instance;

	public static LampDiscoverer getInstance(Context context) {
		if (instance == null) {
			instance = new LampDiscoverer(context);
			instance.mContext = context;
			
			// create a handler on THIS THREAD (should be our app's UI thread)
			instance.mCallbackHandler = new Handler(Looper.getMainLooper());
		}

		return instance;
	}
	
	public void enableContinuous() {
		continuousDiscovery = true;
	}
	
	public void disableContinuous() {
		continuousDiscovery = false;
	}

	/*
	 * Bluetooth stuff
	 */

	public BluetoothAdapter mBluetoothAdapter;
	public BluetoothAdapter.LeScanCallback mLeScanCallback;

	public static String ionUUID = "2e8b0001c4b743f1bbdd6285294a4116";
	public static String dfuUUID = "000015301212efde1523785feabcd123";

	private LampDiscoverer(final Context context) {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
			@Override
			public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
				// reverse the scanrecord
				byte[] flippedScanRecord = new byte[scanRecord.length];
				for (int i = 0; i < scanRecord.length; i++)
					flippedScanRecord[scanRecord.length - i - 1] = scanRecord[i];

				// convert the reversed scanrecord to a string
				String scanRecordString = byteArrToString(flippedScanRecord);

				// If the uuid matches the lamp uuid, we've found a lamp! Yay!
				if (scanRecordString.contains(ionUUID)) {
					if (delegate != null) {
						mCallbackHandler.post(new Runnable() {
					        public void run() {
					        	// only create a new lamp if the manager doesn't know about it, otherwise it's an update
					        	if (delegate.isLampKnown(device.getAddress())) {
					        		delegate.didSeeLamp(device, rssi);
					        	} else {
					        		Lamp newLamp = new Lamp(context, device, rssi);
						        	delegate.didDiscoverLamp(newLamp);
					        	}
					        }
					    });
					}
				} else if (scanRecordString.contains(dfuUUID)) {
					// found a DFU target lamp
					if (delegate != null) {
						mCallbackHandler.post(new Runnable() {
					        public void run() {
					        	// only create a new lamp if the manager doesn't know about it, otherwise it's an update
					        	if (delegate.isDfuLampKnown(device.getAddress())) {
					        		delegate.didSeeDfuLamp(device, rssi);
					        	} else {
					        		DFULamp dfuLamp = new DFULamp(context, device);
						        	delegate.didDiscoverDfuLamp(dfuLamp);
					        	}
					        }
					    });
					}
				}
			}
		};
	}
	
	public boolean isContinousScanning() {
		return continuousDiscovery;
	}
	
	public long getLastDiscoveryCompletionTime() {
		return lastDiscoveryCompletion;
	}
	
	private void stopDiscoveryIfNeeded() {
		if (suspend)
			return;
		
		lastDiscoveryCompletion = Now.get();
		
		if (continuousDiscovery) {
			repeatScan();
			mCallbackHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopDiscoveryIfNeeded();
                }
            }, DISCOVERY_PERIOD_SECONDS * 1000);
		} else {
			stopDiscovery();
		}
	}

	// scans for number of seconds (if bluetooth is enabled)
	public void discoverLamps(ILampDiscoveryDelegate delegate) {
		this.delegate = delegate;

		if (!isDiscovering && !suspend) {
			if (mBluetoothAdapter == null) {
				// device does not support bluetooth
				return;
			} else {
			    if (!mBluetoothAdapter.isEnabled()) {
			    	// bluetooth not enabled
			        return;
			    } else {
			    	Logger.i(this.getClass().getSimpleName(), "STARTING DISCOVERY");
			    	mBluetoothAdapter.startLeScan(mLeScanCallback);
					isDiscovering = true;
					
					mCallbackHandler.postDelayed(new Runnable() {
		                @Override
		                public void run() {
		                    stopDiscoveryIfNeeded();
		                }
		            }, DISCOVERY_PERIOD_SECONDS * 1000);
			    }
			}
		}
	}
	
	private void repeatScan() {
		if (isDiscovering) {
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			// slight delay between stop/start for good measure
			mCallbackHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                	mBluetoothAdapter.startLeScan(mLeScanCallback);
                }
            }, 100);
		}
	}
	
	public void stopDiscovery(){
		if (isDiscovering) {
			Logger.i(this.getClass().getSimpleName(), "ENDING DISCOVERY");
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			isDiscovering = false;
		}
		
		// release wake lock
		WakeLockManager.getInstance(mContext).setDiscoveringLamps(false);
	}
	
	public void suspend() {
		suspend = true;
		
		if (isDiscovering) {
			stopDiscovery();
		}
	}
	
	public void resume() {
		suspend = false;
	}

	private String byteArrToString(byte[] b) {
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < b.length; i++) {
			String hex = Integer.toHexString(0xFF & b[i]);
			if (hex.length() == 1)
				hexString.append('0');
			hexString.append(hex);
		}
		
		return hexString.toString();
	}
	
	public boolean isDiscovering(){
		return isDiscovering;
	}
}
