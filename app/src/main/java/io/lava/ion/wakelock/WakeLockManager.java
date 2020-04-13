package io.lava.ion.wakelock;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class WakeLockManager {
	// reasons we wakelock...
	private boolean updatingWeather = false;
	private boolean updatingFirmwareMetadata = false;
	private boolean discoveringLamps = false;
	private boolean packetsInBLEQueue = false;
	
	private static WakeLockManager instance;
	
	private PowerManager powerManager;
	private WakeLock wakeLock;
	
	// singleton
	public static WakeLockManager getInstance(Context context) {
		if (instance == null) {
			instance = new WakeLockManager();
			instance.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		}
		
		return instance;
	}
	
	private WakeLockManager() {
		// block instantiation
	}
	
	public void setUpdatingWeather(boolean weather) {
		updatingWeather = weather;
		update();
	}
	
	public void setUpdatingFirmwareMetadata(boolean firmwareUpdate) {
		updatingFirmwareMetadata = firmwareUpdate;
		update();
	}
	
	public void setDiscoveringLamps(boolean discovering) {
		discoveringLamps = discovering;
		update();
	}
	
	public void setPacketsInBLEQueue(boolean queued) {
		packetsInBLEQueue = queued;
		update();
	}
	
	private void update() {
		if (!updatingWeather && !updatingFirmwareMetadata && !discoveringLamps && !packetsInBLEQueue) {
			releaseWakeLock();
		} else {
			grabWakeLock();
		}
	}
	
	private void grabWakeLock() {
		if (wakeLock == null) {
			wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ion:connectivitywakelock");
			wakeLock.acquire();
		}
	}
	
	private void releaseWakeLock() {
		if (wakeLock != null) {
			try {
				wakeLock.release();
			} catch (Exception e) {
				// could not release wake lock
			}
			wakeLock = null;
		}
	}
}
