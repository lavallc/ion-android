package io.lava.ion.connectivity.lamp;

import io.lava.ion.connectivity.BLEController;
import io.lava.ion.connectivity.ILampConnectionCallback;
import io.lava.ion.connectivity.LampDiscoverer;
import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.OnPacketResponseListener;
import io.lava.ion.connectivity.PacketConstants;
import io.lava.ion.connectivity.PacketRateLimiter;
import io.lava.ion.connectivity.incomingpackets.DeviceNamePacket;
import io.lava.ion.connectivity.incomingpackets.DeviceSettingsPacket;
import io.lava.ion.connectivity.incomingpackets.FirmwareVersionPacket;
import io.lava.ion.connectivity.incomingpackets.LumenPacket;
import io.lava.ion.connectivity.incomingpackets.MoodConfigPacket;
import io.lava.ion.connectivity.incomingpackets.MoodPacket;
import io.lava.ion.connectivity.incomingpackets.NotificationConfigPacket;
import io.lava.ion.connectivity.incomingpackets.RotationPacket;
import io.lava.ion.connectivity.outgoingpackets.BeginBondPacket;
import io.lava.ion.connectivity.outgoingpackets.ClearNotificationPacket;
import io.lava.ion.connectivity.outgoingpackets.EnterDFUModePacket;
import io.lava.ion.connectivity.outgoingpackets.GetCurrentMoodPacket;
import io.lava.ion.connectivity.outgoingpackets.GetDeviceSettingsPacket;
import io.lava.ion.connectivity.outgoingpackets.GetMoodConfigPacket;
import io.lava.ion.connectivity.outgoingpackets.GetNotificationConfigPacket;
import io.lava.ion.connectivity.outgoingpackets.GetRotationPacket;
import io.lava.ion.connectivity.outgoingpackets.InitPacket;
import io.lava.ion.connectivity.outgoingpackets.RestoreMoodConfigsPacket;
import io.lava.ion.connectivity.outgoingpackets.SaveMoodConfigsPacket;
import io.lava.ion.connectivity.outgoingpackets.SetCurrentMoodPacket;
import io.lava.ion.connectivity.outgoingpackets.SetDeviceNamePacket;
import io.lava.ion.connectivity.outgoingpackets.SetDeviceSettingsPacket;
import io.lava.ion.connectivity.outgoingpackets.SetMoodConfigPacket;
import io.lava.ion.connectivity.outgoingpackets.SetNotificationConfigPacket;
import io.lava.ion.connectivity.outgoingpackets.SetTimePacket;
import io.lava.ion.connectivity.outgoingpackets.SetWeatherPacket;
import io.lava.ion.connectivity.outgoingpackets.ShowNotificationPacket;
import io.lava.ion.connectivity.outgoingpackets.TriggerNotificationPacket;
import io.lava.ion.connectivity.outgoingpackets.UpdateRotationPacket;
import io.lava.ion.fragments.home.lamplist.LampListItem;
import io.lava.ion.fragments.update.UpdateDialogManager;
import io.lava.ion.logger.Logger;
import io.lava.ion.moods.MoodManager;
import io.lava.ion.services.weather.WeatherData;
import io.lava.ion.services.weather.WeatherService;
import io.lava.ion.utility.Now;

import java.util.ArrayList;
import java.util.Calendar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class Lamp implements ILampConnectionCallback, LampListItem {
	// what firmware version of Lamp does this class support?
	public static final int EXPECTED_PROTOCOL_VERSION = 1;
	
	// toggle mood IDs
	public static final int MOOD_OFF_ID = 1;
	public static final int MOOD_LIGHT_ID = 2;
	
	// connection objects
	private Context mContext;
	private BluetoothDevice mDevice;
	private String mName;
	private String macAddress;
	private BluetoothGatt mBluetoothGatt;
	private BluetoothGattCharacteristic controlChar;
	
	// lamp state
	private boolean knockEnabled, leashEnabled, notificationsEnabled, quietTimeEnabled;
	private int quietHourStart, quietMinuteStart, quietHourEnd, quietMinuteEnd;
	private boolean bondCreated;
	private int shuffleTime;
	private int currentMood;
	private ArrayList<Integer> currentRotation;
	private int protocolVersion, firmwareVersion;
	private int lastMood;
	
	// update state
	private boolean appIsOutOfDate = false;
	private boolean lampIsOutOfDate = false;
	
	// connectivity state
	public enum LampState {
	    DISCONNECTED,
	    CONNECTING,
	    CONNECTED,
	    READY;
	}
	private LampState connectivityState = LampState.DISCONNECTED;
	
	// does this lamp auto connect?
	private boolean remembered;
	
	// the last time this lamp was seen
	private long lastSeen = 0;
	
	// rssi ranging
	private double lastRssi = -200;
	private long lastRssiUpdate = 0;
	private Runnable rssiUpdateRunnable;
	
	// rssi update interval (when lamp drawer is open)
	private static final long RSSI_UPDATE_INTERVAL = 1000 * 3;	// 3 seconds
	
	private PacketRateLimiter moodConfigSetLimiterPrimary, moodConfigSetLimiterSecondary;
	
	// prefs string to store remembered lamps under
	private static final String PREFS_NAME = "lamps";
	
	// anything posted here will run on the UI thread
	private Handler mUIHandler;
	
	
	/* used for creating online lamps */
	public Lamp(Context context, BluetoothDevice device, int rssi) {
		lastSeen = Now.get();
		mDevice = device;
		lastRssi = rssi;
		lastRssiUpdate = Now.get();
		init(context, device.getName(), device.getAddress());
		
		storeName(macAddress, mName);
	}
	
	/* used for creating offline lamps */
	public Lamp(Context context, String name, String macAddr) {
		init(context, name, macAddr);
	}
	
	private void init(Context context, String lampName, String macAddr) {
		mContext = context;
		mName = lampName;
		macAddress = macAddr;
		
		// was this lamp selected before?
		SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		remembered = settings.getBoolean(macAddress + ":autoconnect", false);
		
		currentRotation = new ArrayList<Integer>();
		
		mUIHandler = new Handler(Looper.getMainLooper());
		
		rssiUpdateRunnable = new Runnable() {
			@Override
			public void run() {
				// only update RSSI when lamp list is open (continous discovery mode)
				if (LampDiscoverer.getInstance(mContext).isContinousScanning())
					updateRssi();
				
				// requeue ourself
				mUIHandler.postDelayed(rssiUpdateRunnable, RSSI_UPDATE_INTERVAL);
			}
		};
		
		mUIHandler.post(rssiUpdateRunnable);
	}
	
	@Override
	public void didReadRemoteRssi(double rssi) {
		lastSeen = Now.get();
		
		// if we have been updated within the last 2 seconds, ignore
		if (Now.withinSeconds(lastRssiUpdate, 2))
			return;
		
		lastRssi = rssi;
		lastRssiUpdate = Now.get();
		
		// a lamp rssi changed, update any GUIs displaying lamps
		if (LampManager.getInstanceIfReady() != null) {
			LampManager.getInstanceIfReady().notifyListenersOfLampListUpdates();
		}
	}

	@Override
	public void didConnect() {
		connectivityState = LampState.CONNECTED;
		Logger.i(this.getClass().getSimpleName(), "CONNECTED!");
		
		// remember this lamp, even when offline
		remember();
		
		// reset our firmware flag
		lampIsOutOfDate = false;
	}

	@Override
	public void didDiscoverServices(BluetoothGattCharacteristic control) {
		controlChar = control;
	}
	
	private void cleanupAfterDisconnect() {
		if (moodConfigSetLimiterPrimary != null)
			moodConfigSetLimiterPrimary.clear();
		if (moodConfigSetLimiterSecondary != null)
			moodConfigSetLimiterSecondary.clear();
		
		connectivityState = LampState.DISCONNECTED;
		
		Logger.i(this.getClass().getSimpleName(), "DISCONNECTED!");
		
		// notify the manager that we disconnected
		try {
			LampManager.getInstanceIfReady().lampReadyStateChanged(this);
			LampManager.getInstanceIfReady().notifyListenersOfLampListUpdates();
		} catch (Exception e) {}
	}

	@Override
	public void didDisconnect() {
		// we did not call close manually as we weren't expecting to be disconnected
		// cleanup resources now
		if (mBluetoothGatt != null)
			mBluetoothGatt.close();
		
		lastSeen = 0;
		
		cleanupAfterDisconnect();
	}

	@Override
	public void onLampGenericNotify(byte[] packet) {
		LumenPacket genericPacket = new LumenPacket(packet);
		if (genericPacket.getOpCode() == PacketConstants.OpCode.getCurrentMood) {
			MoodPacket moodPacket = new MoodPacket(packet);
			
			// the lamp was tapped, we got a mood ID update
			currentMood = moodPacket.getMoodId();
			
			Logger.i(this.getClass().getSimpleName(), "MOOD: " + Integer.toString(currentMood));
			
			if (LampManager.getInstanceIfReady() != null) {
				LampManager.getInstanceIfReady().lampMoodDidChange(currentMood);
			}
		}
	}
	
	@Override
	public void readyToBeginInit() {
		// create mood config set rate limiters (drops intermediary packets)
		moodConfigSetLimiterPrimary = new PacketRateLimiter(this, BLEController.getInstance(mContext), controlChar, mBluetoothGatt, PacketRateLimiter.TYPE_LOSSY);
		moodConfigSetLimiterSecondary = new PacketRateLimiter(this, BLEController.getInstance(mContext), controlChar, mBluetoothGatt, PacketRateLimiter.TYPE_LOSSY);
		
		// we're ready, start the init process
		initializeLamp();
	}

	@Override
	public void didReadRemoteDevice(BluetoothDevice device) {
		lastSeen = Now.get();
		
		if (device.getName() != mName) {
			mName = device.getName();
			
			storeName(device.getAddress(), mName);
		}
		
		if (connectivityState == LampState.DISCONNECTED) {
			mDevice = device;
			mBluetoothGatt = null;
			controlChar = null;
		}
	}
	
	
	
	
	
	public String getName() {
		return mName;
	}
	
	public double getRssi() {
		return lastRssi;
	}
	
	public String getDistanceString() {
		if (!seenRecently()) {
			return "Out of range";
		} else if (lastRssi > -70) {
			return "Immediate";
		} else if (lastRssi > -85) {
			return "Near";
		} else {
			return "Far";
		}
	}
	
	public void nextMood() {
		if (connectivityState == LampState.READY) {
			// user has nothing in their rotation, let's just toggle power
			if (currentRotation.size() == 0) {
				togglePower();
				return;
			}
			
			int currentIndex = -1;
			
			for (int i=0; i < currentRotation.size(); i++) {
				if (currentMood == currentRotation.get(i))
					currentIndex = i;
			}
			
			if (currentIndex == -1) {
				// lamp must be OFF since OFF is not in rotation list
				// start at the beginning of the rotation
				setMood(currentRotation.get(0), null);
			} else if (currentIndex == currentRotation.size()-1) {
				// need to loop back around
				setMood(currentRotation.get(0), null);
			} else {
				// jump to next mood
				setMood(currentRotation.get(currentIndex+1), null);
			}
		}
	}
	
	public void clearNotification() {
		if (connectivityState == LampState.READY) {
			ClearNotificationPacket clearNotificationPacket = new ClearNotificationPacket();
			
			BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, clearNotificationPacket, new OnPacketResponseListener() {
				@Override
				public void onAck(byte[] response) {

				}
	
				@Override
				public void onNak(int nakCode) {

				}

				@Override
				public void onWritten() {

				}
			});
		}
	}
	
	public void togglePower() {
		if (connectivityState == LampState.READY) {
			if (currentMood == MOOD_OFF_ID)
				powerOn();
			else
				powerOff();
		}
	}
	
	public void powerOff() {
		if (connectivityState == LampState.READY && currentMood != MOOD_OFF_ID) {
			final int lastMoodPending = currentMood;
			
			setMood(MOOD_OFF_ID, new SuccessFailCallback() {
				@Override
				public void onSuccess() {
					lastMood = lastMoodPending;
				}
	
				@Override
				public void onFail() {
					// ignore
				}
			});
		}
	}
	
	public void powerOn() {
		if (connectivityState == LampState.READY && currentMood == MOOD_OFF_ID) {
			setMood(lastMood, new SuccessFailCallback() {
				@Override
				public void onSuccess() {
					// ignore
				}
	
				@Override
				public void onFail() {
					// ignore
				}
			});
		}
	}
	
	public String getMacAddress() {
		return macAddress;
	}
	
	public LampState getConnectivityState() {
		return connectivityState;
	}
	
	public boolean isAppOutOfDate() {
		return appIsOutOfDate;
	}
	
	public boolean isLampOutOfDate() {
		return lampIsOutOfDate;
	}
	
	public boolean isKnockEnabled() {
		return knockEnabled;
	}
	
	public boolean isQuietTimeEnabled() {
		return quietTimeEnabled;
	}
	
	public boolean areNotificationsEnabled() {
		return notificationsEnabled;
	}
	
	public int getNotificationQuietHourStart() {
		return quietHourStart;
	}
	
	public int getNotificationQuietMinuteStart() {
		return quietMinuteStart;
	}

	public int getNotificationQuietHourEnd() {
		return quietHourEnd;
	}
	
	public int getNotificationQuietMinuteEnd() {
		return quietMinuteEnd;
	}
	
	public boolean isShuffleEnabled() {
		return (shuffleTime > 0);
	}
	
	public int getShuffleTime() {
		if (shuffleTime > 0)
			return shuffleTime;
		else
			return 1;
	}
	
	public boolean isLeashEnabled() {
		return leashEnabled;
	}
	
	public boolean isBonded() {
		return bondCreated;
	}
	
	// should this lamp be displayed in the list?
	public boolean seenRecently() {
		// i know, it's a pretty big assumption
		if (connectivityState != LampState.DISCONNECTED)
			return true;
		
		// was discovery performed within the last 3 seconds?
		boolean scannedRecently = Now.withinSeconds(LampDiscoverer.getInstance(mContext).getLastDiscoveryCompletionTime(), 5);
		
		// has the lamp been seen in the last 5 seconds?
		if (scannedRecently && !Now.withinSeconds(lastSeen, 5))
			return false;
		else {
			// our scan interval is 30 seconds, so we'll remove after 45 regardless
			if (Now.withinSeconds(lastSeen, 45))
				return true;
			else
				return false;
		}
	}
	
	public void remember() {
		if (!remembered) {
			remembered = true;
			
			// persist this into sharedpreferences
			remember(true);
			
			// tell the lampmanager so it can display us in the list even if we're offline
			LampManager.getInstanceIfReady().addedLampToAutoConnectList(macAddress);
		}
	}
	
	public void forget() {
		if (remembered) {
			remembered = false;
			
			// persist this into sharedpreferences
			remember(false);
			
			// tell the lampmanager so it can forget us and not display us in the list when we're offline
			LampManager.getInstanceIfReady().removedLampFromAutoConnectList(macAddress);
		}
	}
	
	public boolean isRemembered() {
		return remembered;
	}
	
	public void connect() {
		if (connectivityState == LampState.DISCONNECTED && seenRecently()) {
			connectivityState = LampState.CONNECTING;
			
			// tell the BLEController to connect to this lamp
			mBluetoothGatt = BLEController.getInstance(mContext).connect(mDevice, this);
			if (mBluetoothGatt == null) {
				// we couldn't connect for some reason, let the user know
				onInitFailed();
			}
		}
	}

	public void disconnect() {
		if (connectivityState != LampState.DISCONNECTED) {
			cleanupAfterDisconnect();
			
			// forget this lamp
			forget();
			
			// tell the BLEController to disconnect from this lamp
			if (mBluetoothGatt != null)
				mBluetoothGatt.close();
		}
	}
	
	// requests an rssi update
	public void updateRssi() {
		if (connectivityState == LampState.READY)
			BLEController.getInstance(mContext).updateRssi(mBluetoothGatt);
	}
	
	public void setMood(final int moodId, final SuccessFailCallback cb) {
		if (connectivityState == LampState.READY && moodId != currentMood) {
			// trigger listeners callback (assume we were successful for responsiveness sake)
			final int moodBeforeChange = currentMood;
			currentMood = moodId;
			
			if (moodId != MOOD_OFF_ID)
				lastMood = moodId;
			
			if (LampManager.getInstanceIfReady() != null) {
				LampManager.getInstanceIfReady().lampMoodDidChange(currentMood);
			}
			
			BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, new SetCurrentMoodPacket(moodId), new OnPacketResponseListener() {
				@Override
				public void onAck(byte[] response) {
					// success, we were right in assuming
					if (cb != null)
						cb.onSuccess();
				}
	
				@Override
				public void onNak(int nakCode) {
					// trigger listeners callback (we were not successful, so we lied to everyone!)
					currentMood = moodBeforeChange;
					lastMood = moodBeforeChange;
					
					if (LampManager.getInstanceIfReady() != null) {
						LampManager.getInstanceIfReady().lampMoodDidChange(moodBeforeChange);
					}
					
					if (cb != null)
						cb.onFail();
				}

				@Override
				public void onWritten() {

				}
			});
		}
	}
	
	public void setMoodConfig(final int moodId, final int configId, final int configVal) {
		if (connectivityState == LampState.READY) {
			// ensure that the current mood is the mood we're configuring
			if (currentMood != moodId) {
				setMood(moodId, new SuccessFailCallback() {

					@Override
					public void onSuccess() {
						moodConfigSetLimiterPrimary.enqueue(new SetMoodConfigPacket(moodId, configId, configVal));
					}

					@Override
					public void onFail() {

					}
					
				});
			} else {
				moodConfigSetLimiterPrimary.enqueue(new SetMoodConfigPacket(moodId, configId, configVal));
			}
		}
	}
	
	public void setMoodConfigDual(final int moodId, final int configIdOne, final int configValOne, final int configIdTwo, final int configValTwo) {
		if (connectivityState == LampState.READY) {
			// ensure that the current mood is the mood we're configuring
			if (currentMood != moodId) {
				setMood(moodId, new SuccessFailCallback() {

					@Override
					public void onSuccess() {
						moodConfigSetLimiterPrimary.enqueue(new SetMoodConfigPacket(moodId, configIdOne, configValOne));
						moodConfigSetLimiterSecondary.enqueue(new SetMoodConfigPacket(moodId, configIdTwo, configValTwo));
					}

					@Override
					public void onFail() {

					}
					
				});
			} else {
				moodConfigSetLimiterPrimary.enqueue(new SetMoodConfigPacket(moodId, configIdOne, configValOne));
				moodConfigSetLimiterSecondary.enqueue(new SetMoodConfigPacket(moodId, configIdTwo, configValTwo));
			}
		}
	}
	
	public void getMoodConfig(int moodId, int configId, final MoodConfigCallback cb) {
		if (cb != null) {
			BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, new GetMoodConfigPacket(moodId, configId), new OnPacketResponseListener() {
				@Override
				public void onAck(byte[] response) {
					// success
					MoodConfigPacket moodConfigPacket = new MoodConfigPacket(response);
					MoodConfig moodConfig = new MoodConfig(moodConfigPacket.getMoodId(), moodConfigPacket.getConfigId(), moodConfigPacket.getConfigVal());
					
					cb.onSuccess(moodConfig);
				}
	
				@Override
				public void onNak(int nakCode) {
					// fail
					cb.onFail();
				}

				@Override
				public void onWritten() {

				}
			});
		}
	}
	
	public void getNotificationConfig(int notificationId, final NotificationConfigCallback cb) {
		if (cb != null) {
			BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, new GetNotificationConfigPacket(notificationId), new OnPacketResponseListener() {
				@Override
				public void onAck(byte[] response) {
					// success
					NotificationConfigPacket notificationConfigPacket = new NotificationConfigPacket(response);
				
					cb.onSuccess(notificationConfigPacket);
				}
	
				@Override
				public void onNak(int nakCode) {
					// fail
					cb.onFail();
				}

				@Override
				public void onWritten() {

				}
			});
		}
	}
	
	public void updateTimeAndWeather(Runnable finished) {
		if (connectivityState == LampState.READY) {
			updateTime(null);
			updateWeather(finished);
		}
	}
	
	public void setNotificationConfig(int notificationId, boolean enabled, int pattern, int hue, int brightness, int saturation, int speed, int duration, boolean sticky, final NotificationConfigCallback cb) {
		if (connectivityState == LampState.READY) {
			SetNotificationConfigPacket notificationConfigPacket = new SetNotificationConfigPacket(notificationId, enabled, pattern, hue, brightness, saturation, speed, duration, sticky);
			
			BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, notificationConfigPacket, new OnPacketResponseListener() {
				@Override
				public void onAck(byte[] response) {
					NotificationConfigPacket notificationConfigPacket = new NotificationConfigPacket(response);

					// success
					if (cb != null)
						cb.onSuccess(notificationConfigPacket);
				}
	
				@Override
				public void onNak(int nakCode) {
					// fail
					if (cb != null)
						cb.onFail();
				}

				@Override
				public void onWritten() {

				}
			});
		}
	}
	
	// useful for previewing a notification without saving it
	public void showNotification(int pattern, int hue, int brightness, int saturation, int speed, int duration, boolean sticky, final SuccessFailCallback cb) {
		if (connectivityState == LampState.READY) {
			ShowNotificationPacket showNotificationPacket = new ShowNotificationPacket(pattern, hue, brightness, saturation, speed, duration, sticky);
			
			BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, showNotificationPacket, new OnPacketResponseListener() {
				@Override
				public void onAck(byte[] response) {
					// success
					if (cb != null)
						cb.onSuccess();
				}
	
				@Override
				public void onNak(int nakCode) {
					// fail
					if (cb != null)
						cb.onFail();
				}

				@Override
				public void onWritten() {

				}
			});
		}
	}
	
	public void saveMoodConfigs(final SuccessFailCallback cb) {
		if (connectivityState == LampState.READY) {
			BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, new SaveMoodConfigsPacket(), new OnPacketResponseListener() {
				@Override
				public void onAck(byte[] response) {
					// success
					if (cb != null)
						cb.onSuccess();
				}
	
				@Override
				public void onNak(int nakCode) {
					// fail
					if (cb != null)
						cb.onFail();
				}

				@Override
				public void onWritten() {

				}
			});
		}
	}
	
	public void restoreMoodConfigs(final SuccessFailCallback cb) {
		if (connectivityState == LampState.READY) {
			BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, new RestoreMoodConfigsPacket(), new OnPacketResponseListener() {
				@Override
				public void onAck(byte[] response) {
					// success
					if (cb != null)
						cb.onSuccess();
				}
	
				@Override
				public void onNak(int nakCode) {
					// fail
					if (cb != null)
						cb.onFail();
				}

				@Override
				public void onWritten() {

				}
			});
		}
	}
	
	public void beginBonding(final SuccessFailCallback cb) {
		if (connectivityState == LampState.READY && !bondCreated) {
			BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, new BeginBondPacket(), new OnPacketResponseListener() {
				@Override
				public void onAck(byte[] response) {
					bondCreated = true;
					
					// success
					if (cb != null)
						cb.onSuccess();
				}
	
				@Override
				public void onNak(int nakCode) {
					// fail
					if (cb != null)
						cb.onFail();
				}

				@Override
				public void onWritten() {

				}
			});
		}
	}
	
	public void triggerNotification(int notificationId) {
		if (connectivityState == LampState.READY) {
			BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, new TriggerNotificationPacket(notificationId), new OnPacketResponseListener() {
				@Override
				public void onAck(byte[] response) {
					// success
				}
	
				@Override
				public void onNak(int nakCode) {
					// fail
				}

				@Override
				public void onWritten() {

				}
			});
		}
	}
	
	public ArrayList<Integer> getRotation() {
		return currentRotation;
	}
	
	public boolean isMoodInRotation(int moodId) {
		return currentRotation.contains(moodId);
	}
	
	public void addToRotation(int moodId, final SuccessFailCallback cb) {
		if (connectivityState == LampState.READY) {
			if (!currentRotation.contains(moodId)) {
				// copy current rotation and remove requested mood ID
				ArrayList<Integer> newRotation = new ArrayList<Integer>();
				newRotation.addAll(currentRotation);
				
				
				// figure out where to add moodId in newRotation
				ArrayList<Integer> allMoodIds = MoodManager.getInstance(mContext).getAllMoodIds();
				int moodToAddIndex = 0;
				boolean addedToList = false;
				
				// here we are looking for the INDEX in the JSON MOOD LIST of the requested MOOD ID
				for (int i=0; i<allMoodIds.size(); i++) {
					if (moodId == allMoodIds.get(i))
						moodToAddIndex = i;
				}
				
				// now we loop over our current rotation and map the MOOD IDs to INDICIES
				for (int i=0; i<currentRotation.size(); i++) {
					int moodIndexFromRotation = allMoodIds.indexOf(currentRotation.get(i));
					
					// we just found a mood in the list that should come AFTER the moods being added
					if (moodIndexFromRotation > moodToAddIndex) {
						newRotation.add(i, moodId);
						addedToList = true;
						break;
					}
				}
				
				if (!addedToList) {
					// we must belong at the end
					newRotation.add(moodId);
				}
				
				// fire BLE packet to lamp, pass along callback
				updateRotation(newRotation, cb);
			} else {
				// mood is already in current rotation
				cb.onFail();
			}
		}
	}
	
	public void removeFromRotation(int moodId, final SuccessFailCallback cb) {
		if (connectivityState == LampState.READY) {
			if (currentRotation.contains(moodId)) {
				// copy current rotation and remove requested mood ID
				ArrayList<Integer> newRotation = new ArrayList<Integer>();
				newRotation.addAll(currentRotation);
				newRotation.remove(newRotation.indexOf(moodId));
				
				// fire BLE packet to lamp, pass along callback
				updateRotation(newRotation, cb);
			} else {
				// mood is not in current rotation
				cb.onFail();
			}
		}
	}
	
	public void updateRotation(ArrayList<Integer> newRotation, final SuccessFailCallback cb) {
		if (connectivityState == LampState.READY) {
			if (newRotation.size() > 18)
				newRotation = new ArrayList<Integer>(newRotation.subList(0, 18));
			
			UpdateRotationPacket rotationPacket = new UpdateRotationPacket(newRotation);
			
			BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, rotationPacket, new OnPacketResponseListener() {
				@Override
				public void onAck(byte[] response) {
					// success
					RotationPacket rotationPacket = new RotationPacket(response);
					
					currentRotation.clear();
					currentRotation.addAll(rotationPacket.getRotationIds());
					
					if (cb != null)
						cb.onSuccess();
				}
	
				@Override
				public void onNak(int nakCode) {
					// fail
					if (cb != null)
						cb.onFail();
				}

				@Override
				public void onWritten() {

				}
			});
		}
	}
	
	public void rename(String newName, final SuccessFailCallback cb) {
		if (connectivityState == LampState.READY) {
			SetDeviceNamePacket namePacket = new SetDeviceNamePacket(newName.substring(0, Math.min(newName.length(), 18)));
			
			BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, namePacket, new OnPacketResponseListener() {
				@Override
				public void onAck(byte[] response) {
					DeviceNamePacket namePacket = new DeviceNamePacket(response);
					mName = namePacket.getName();
					
					storeName(macAddress, mName);
					
					// notify lists that our name has changed
					try {
						LampManager.getInstanceIfReady().notifyListenersOfLampListUpdates();
					} catch (Exception e) {}
					
					if (cb != null)
						cb.onSuccess();
				}
	
				@Override
				public void onNak(int nakCode) {
					// failed to rename
					if (cb != null)
						cb.onFail();
				}

				@Override
				public void onWritten() {

				}
			});
		}
	}
	
	public void setKnockEnabled(boolean knock, SuccessFailCallback cb) {
		setDeviceSettings(knock, shuffleTime, leashEnabled, notificationsEnabled, quietTimeEnabled, quietHourStart, quietMinuteStart, quietHourEnd, quietMinuteEnd, cb);
	}
	
	public void setShuffleTime(int shuffleTime, SuccessFailCallback cb) {
		setDeviceSettings(knockEnabled, shuffleTime, leashEnabled, notificationsEnabled, quietTimeEnabled, quietHourStart, quietMinuteStart, quietHourEnd, quietMinuteEnd, cb);
	}
	
	public void setLeashEnabled(boolean leash, SuccessFailCallback cb) {
		setDeviceSettings(knockEnabled, shuffleTime, leash, notificationsEnabled, quietTimeEnabled, quietHourStart, quietMinuteStart, quietHourEnd, quietMinuteEnd, cb);
	}
	
	public void setNotificationsEnabled(boolean notifications, SuccessFailCallback cb) {
		setDeviceSettings(knockEnabled, shuffleTime, leashEnabled, notifications, quietTimeEnabled, quietHourStart, quietMinuteStart, quietHourEnd, quietMinuteEnd, cb);
	}
	
	public void setQuietTimeStart(int quietHrStart, int quietMinStart, SuccessFailCallback cb) {
		setDeviceSettings(knockEnabled, shuffleTime, leashEnabled, notificationsEnabled, quietTimeEnabled, quietHrStart, quietMinStart, quietHourEnd, quietMinuteEnd, cb);
	}
	
	public void setQuietTimeEnd(int quietHrEnd, int quietMinEnd, SuccessFailCallback cb) {
		setDeviceSettings(knockEnabled, shuffleTime, leashEnabled, notificationsEnabled, quietTimeEnabled, quietHourStart, quietMinuteStart, quietHrEnd, quietMinEnd, cb);
	}
	
	public void setQuietTimeEnabled(boolean quietEnabled, SuccessFailCallback cb) {
		setDeviceSettings(knockEnabled, shuffleTime, leashEnabled, notificationsEnabled, quietEnabled, quietHourStart, quietMinuteStart, quietHourEnd, quietMinuteEnd, cb);
	}
	
	public int getCurrentMood() {
		return currentMood;
	}
	
	public int getProtocolVersion() {
		return protocolVersion;
	}
	
	public int getFirmwareVersion() {
		return firmwareVersion;
	}
	
	
	
	
	private Lamp getSelf() {
		return this;
	}
	
	public void enterDFUMode() {
		BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, new EnterDFUModePacket(), null);
	}
	
	private void storeName(String macAddr, String name) {
		SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.putString(macAddr, name);
		
		editor.commit();
	}
	
	private void remember(boolean shouldRemember) {
		SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.putBoolean(macAddress + ":autoconnect", shouldRemember);
		
		editor.commit();
	}
	
	public void onInitFailed() {
		cleanupAfterDisconnect();
		
		// tell the BLEController to disconnect from this lamp
		if (mBluetoothGatt != null)
			mBluetoothGatt.close();
		
		Toast.makeText(mContext, "connection failed", Toast.LENGTH_LONG).show();
	}
	
	private void initializeLamp() {
		// Now that we know the lamp is home, ask it to come out and play
		if (connectivityState == LampState.CONNECTED) {
            // grab our private mac address and create an init packet with it
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            InitPacket initPkt = new InitPacket(mBluetoothAdapter.getAddress());

			BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, initPkt, new OnPacketResponseListener() {
				@Override
				public void onAck(byte[] response) {
					// get protocol and firmware version from lamp
					FirmwareVersionPacket fvPacket = new FirmwareVersionPacket(response);
					protocolVersion = fvPacket.getProtocolVersion();
                    firmwareVersion = fvPacket.getFirmwareVersion();
					bondCreated = fvPacket.isBonded();
					
					if (protocolVersion == EXPECTED_PROTOCOL_VERSION) {
						// we're good, continue init process
						lampIsOutOfDate = false;
						
						// GET CURRENT MOOD
						BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, new GetCurrentMoodPacket(), new OnPacketResponseListener() {
							@Override
							public void onAck(byte[] response) {
								MoodPacket moodPacket = new MoodPacket(response);
								currentMood = moodPacket.getMoodId();
								
								// we don't know what the last mood would have been, so we'll select 'light'
								if (currentMood == MOOD_OFF_ID)
									lastMood = MOOD_LIGHT_ID;
								
								// GET DEVICE SETTINGS
								BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, new GetDeviceSettingsPacket(), new OnPacketResponseListener() {
									@Override
									public void onAck(byte[] response) {
										DeviceSettingsPacket settingsPacket = new DeviceSettingsPacket(response);
										knockEnabled = settingsPacket.getKnockEnabled();
										quietTimeEnabled = settingsPacket.getQuietEnabled();
										shuffleTime = settingsPacket.getShuffleTime();
										leashEnabled = settingsPacket.getLeashEnabled();
										notificationsEnabled = settingsPacket.getNotificationsEnabled();
										quietHourStart = settingsPacket.getQuietHourStart();
										quietMinuteStart = settingsPacket.getQuietMinuteStart();
										quietHourEnd = settingsPacket.getQuietHourEnd();
										quietMinuteEnd = settingsPacket.getQuietMinuteEnd();
										
										// GET MOOD ROTATION
										BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, new GetRotationPacket(), new OnPacketResponseListener() {
											@Override
											public void onAck(byte[] response) {
												RotationPacket rotationPacket = new RotationPacket(response);
												
												currentRotation.clear();
												currentRotation.addAll(rotationPacket.getRotationIds());
												
												// SET TIME
												updateTime(new Runnable() {
													@Override
													public void run() {
														// SET WEATHER
														updateWeather(new Runnable() {
															@Override
															public void run() {
																// WHEW, WE'RE DONE AND READY!
																connectivityState = LampState.READY;
																Logger.i(this.getClass().getSimpleName(), "FINISHED INITIALIZATION SUCCESSFULLY!");
																
																// notify the manager that we are connected and ready
																try {
																	LampManager.getInstanceIfReady().lampReadyStateChanged(getSelf());
																} catch (Exception e) {}
															}
														});
													}
												});
											}

											@Override
											public void onNak(int nakCode) {
												onInitFailed();
											}

											@Override
											public void onWritten() {

											}
										});
									}

									@Override
									public void onNak(int nakCode) {
										onInitFailed();
									}

									@Override
									public void onWritten() {

									}
								});
							}

							@Override
							public void onNak(int nakCode) {
								onInitFailed();
							}

							@Override
							public void onWritten() {

							}
						});
					} else if (protocolVersion < EXPECTED_PROTOCOL_VERSION) {
						lampIsOutOfDate = true;
						
						// display dialog that the LAMP needs to be updated
						UpdateDialogManager.getInstance(mContext).createLampOutOfDateDialogIfAppVisible(getSelf(), true);
					} else {
						appIsOutOfDate = true;
						onInitFailed();
						
						// display dialog to user to update the APP
						UpdateDialogManager.getInstance(mContext).createAppOutOfDateDialogIfAppVisible();
					}
				}

				@Override
				public void onNak(int nakCode) {
					onInitFailed();
				}

				@Override
				public void onWritten() {

				}
			});
		}
	}
	
	private void updateTime(final Runnable complete) {
		Calendar rightNow = Calendar.getInstance();
		SetTimePacket timePacket = new SetTimePacket(rightNow.get(Calendar.HOUR_OF_DAY), rightNow.get(Calendar.MINUTE), rightNow.get(Calendar.SECOND));
		
		BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, timePacket, new OnPacketResponseListener() {
			@Override
			public void onAck(byte[] response) {
				if (complete != null)
					complete.run();
			}

			@Override
			public void onNak(int nakCode) {
				if (complete != null)
					complete.run();
			}

			@Override
			public void onWritten() {

			}
		});
	}
	
	private void updateWeather(final Runnable complete) {
		if (WeatherData.getInstance().dataIsValid()) {
			// collect data from the WeatherData singleton
			int currentTemp = WeatherData.getInstance().getCurrentTemp();
			int currentConditions = WeatherData.getInstance().getCurrentConditions();
			int futureTemp = WeatherData.getInstance().getFutureTemp();
			int futureConditions = WeatherData.getInstance().getFutureConditions();
			int sunrise24Hr = WeatherData.getInstance().getSunrise24Hr();
			int sunriseMin = WeatherData.getInstance().getSunriseMin();
			int sunset24Hr = WeatherData.getInstance().getSunset24Hr();
			int sunsetMin = WeatherData.getInstance().getSunsetMin();

			// create the weather packet
			SetWeatherPacket weatherPacket = new SetWeatherPacket(currentTemp, currentConditions, futureTemp, futureConditions, sunrise24Hr, sunriseMin, sunset24Hr, sunsetMin);
			
			BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, weatherPacket, new OnPacketResponseListener() {
				@Override
				public void onAck(byte[] response) {

				}
	
				@Override
				public void onNak(int nakCode) {
					
				}

				@Override
				public void onWritten() {
					if (complete != null)
						complete.run();
				}
			});
		} else {
			// weather is not ready, so we're done
			if (complete != null)
				complete.run();
			
			// start a weather update just in case one is not already queued
			if (WeatherService.getInstanceIfExists() != null) {
				WeatherService.getInstanceIfExists().forceUpdate();
			}
		}
	}
	
	private void setDeviceSettings(boolean knock, int shuffle, boolean leash, boolean notifications, boolean quietEnabled, int quietHrStart, int quietMinStart, int quietHrEnd, int quietMinEnd, final SuccessFailCallback cb) {
		if (connectivityState == LampState.READY) {
			SetDeviceSettingsPacket settingsPacket = new SetDeviceSettingsPacket(knock, shuffle, leash, notifications, quietEnabled, quietHrStart, quietMinStart, quietHrEnd, quietMinEnd);
			
			BLEController.getInstance(mContext).enqueuePacket(getSelf(), controlChar, mBluetoothGatt, settingsPacket, new OnPacketResponseListener() {
				@Override
				public void onAck(byte[] response) {
					// keep our lamp object in sync
					DeviceSettingsPacket settingsPacket = new DeviceSettingsPacket(response);
					quietTimeEnabled = settingsPacket.getQuietEnabled();
					knockEnabled = settingsPacket.getKnockEnabled();
					shuffleTime = settingsPacket.getShuffleTime();
					leashEnabled = settingsPacket.getLeashEnabled();
					notificationsEnabled = settingsPacket.getNotificationsEnabled();
					quietHourStart = settingsPacket.getQuietHourStart();
					quietMinuteStart = settingsPacket.getQuietMinuteStart();
					quietHourEnd = settingsPacket.getQuietHourEnd();
					quietMinuteEnd = settingsPacket.getQuietMinuteEnd();
					
					if (cb != null) {
						cb.onSuccess();
					}
				}
	
				@Override
				public void onNak(int nakCode) {
					if (cb != null) {
						cb.onFail();
					}
				}

				@Override
				public void onWritten() {

				}
			});
		}
	}
	
	

	public void onDestroy() {
		// kill rssi timer
		if (rssiUpdateRunnable != null) {
			mUIHandler.removeCallbacks(rssiUpdateRunnable);
		}
	}
}