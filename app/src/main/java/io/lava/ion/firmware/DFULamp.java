package io.lava.ion.firmware;

import io.lava.ion.logger.Logger;
import io.lava.ion.utility.Now;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class DFULamp {
	private static final String TAG = "DFUTarget";
	
	private AtomicInteger state;
	private AtomicInteger bytesSent;
	private IDFULampDelegate delegate;
	public BluetoothDevice device;
	private File mFirmware;
	private byte[] buffer;
	private BluetoothGatt mBluetoothGatt;
	private Context mContext;
	
	private boolean connected = false;
	
	private long lastSeen = 0;
	
	private boolean restoreToFactorySettings = false;
	
	// service UUID of DFU lamp
    public static UUID dfuUUID = UUID.fromString("00001530-1212-efde-1523-785feabcd123");
	
	private class OpCodes {
		public static final int START_DFU = 1;
		public static final int INITIALIZE_DFU = 2;
		public static final int RECEIVE_FIRMWARE_IMAGE = 3;
		public static final int VALIDATE_FIRMWARE_IMAGE = 4;
		public static final int ACTIVATE_FIRMWARE_AND_RESET = 5;
		public static final int OP_CODE_START_DFU_FULL_ERASE = 9;
	}

	private class DFUStatus {
		public static final int SUCCESS = 1;
		public static final int INVALID_STATE = 2;
		public static final int NOT_SUPPORTED = 3;
		public static final int DATA_SIZE_EXCEEDS_LIMITS = 4;
		public static final int CRC_ERROR = 5;
		public static final int OPERATION_FAILED = 6;
	}
	
	private class UploadStatus {
		public static final int SET_CCCD = 0;
		public static final int START_DFU = 1;
		public static final int SET_IMAGE_SIZE = 2;
		public static final int SET_RECEIVE_FIRMWARE = 3;
		public static final int WRITE_PAYLOAD_CHUNK = 4;
		public static final int SET_VALIDATE_FIRMWARE = 5;
		public static final int SET_ACTIVATE_AND_RESET = 6;
		public static final int FAILED = -1;
	}

	private static final int PACKET_SIZE = 20;
	
	// anything posted here will run on the UI thread
	private Handler mUIHandler;
	
	public void setDelegate(IDFULampDelegate delegate) {
		this.delegate = delegate;
	}
	
	public void connectAndFlash() {
		mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
	}
	
	public void wasSeen() {
		lastSeen = Now.get();
	}
	
	public void setRestoreToFactorySettings(boolean restore) {
		restoreToFactorySettings = restore;
	}
	
	// should this lamp be displayed in the list?
	public boolean seenRecently() {
		// i know, it's a pretty big assumption
		if (connected)
			return true;

		// has the lamp been seen in the last 5 seconds?
		if (Now.withinSeconds(lastSeen, 5))
			return true;
		else
			return false;
	}
	
	public String getMacAddress() {
		return device.getAddress();
	}
	
	public void setFirmware(File firmware) {
		mFirmware = firmware;
	}
	
	public DFULamp(Context context, BluetoothDevice device) {
		this.mContext = context;
		this.device = device;
		
		mUIHandler = new Handler(Looper.getMainLooper());
		
		// initialize state/counters
		state = new AtomicInteger(0);
		bytesSent = new AtomicInteger(0);

		mGattCallback = new BluetoothGattCallback() {
			@Override
			public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
				if (newState == BluetoothProfile.STATE_CONNECTED) {
					state.set(UploadStatus.SET_CCCD);
					gatt.discoverServices();
					
					mUIHandler.post(new Runnable() {
						@Override
						public void run() {
							if (delegate != null)
								delegate.DFUTargetDidConnect();
						}
					});
					
					connected = true;
				} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					mBluetoothGatt.close();
					mBluetoothGatt = null;
					
					mUIHandler.post(new Runnable() {
						@Override
						public void run() {
							if (delegate != null)
								delegate.DFUTargetDidDisconnect();
						}
					});
					
					connected = false;
					
					// we just failed a flash
					if (state.get() == UploadStatus.WRITE_PAYLOAD_CHUNK) {
						mUIHandler.post(new Runnable() {
							@Override
							public void run() {
								if (delegate != null)
									delegate.DFUTargetDidFailFlash();
							}
						});
					}
				}
			}

			@Override
			public void onServicesDiscovered(BluetoothGatt gatt, int status) {
				BluetoothGattService mBonfire = gatt.getService(dfuUUID);
				if (mBonfire == null) {
					Logger.i(this.getClass().getSimpleName(), "can't find dfu service");
					
					mUIHandler.post(new Runnable() {
						@Override
						public void run() {
							if (delegate != null)
								delegate.DFUTargetNotFound();
						}
					});
					
					return;
				}

				controlStateCharacteristic = mBonfire.getCharacteristic(controlStateUUID);
				packetCharacteristic = mBonfire.getCharacteristic(packetUUID);
				CCCDDescriptor = controlStateCharacteristic.getDescriptor(CCCD);

				mBluetoothGatt.setCharacteristicNotification(controlStateCharacteristic, true);
				
				if (packetCharacteristic == null || controlStateCharacteristic == null || CCCDDescriptor == null || mBluetoothGatt == null) {
					Logger.d(TAG, "Bluetooth connection failed to fully initialize");
					failedFlash();
					return;
				}

                // wait a bit to make sure notifications were enabled
                mUIHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // time to flash hex file
                            buffer = FileUtils.readFileToByteArray(mFirmware);
                            if (mBluetoothGatt != null) {
                                setCccd();
                            }
                        } catch (IOException e) {
                            // could not open file
                            failedFlash();
                        }
                    }
                }, 1000);
			}

			@Override
			public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
				
			}

			@Override
			public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
				if (status != BluetoothGatt.GATT_SUCCESS) {
					failedFlash();
					return;
				}
				
				if (state.get() == UploadStatus.START_DFU)
					setImageSize();
				else if (state.get() == UploadStatus.SET_RECEIVE_FIRMWARE)
					writePayloadChunk();
				else if (state.get() == UploadStatus.WRITE_PAYLOAD_CHUNK)
					writePayloadChunk();
				else if (state.get() == UploadStatus.SET_ACTIVATE_AND_RESET) {
					connected = false;
					
					if (mBluetoothGatt != null) {
						mBluetoothGatt.close();
						mBluetoothGatt = null;
					}
					
					// we successfully flashed the lamp
					mUIHandler.post(new Runnable() {
						@Override
						public void run() {
							if (delegate != null)
								delegate.DFUTargetDidFinishFlash();
						}
					});
				}
			}

			@Override
			public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
				String operation = "";
				String status = "";

				byte[] b = characteristic.getValue();
				
				// op code
				switch (b[1]) {
					case OpCodes.START_DFU:
						operation = "Start DFU";
						break;
					case OpCodes.INITIALIZE_DFU:
						operation = "Initialize DFU";
						break;
					case OpCodes.RECEIVE_FIRMWARE_IMAGE:
						operation = "Receive Firmware Image";
						break;
					case OpCodes.VALIDATE_FIRMWARE_IMAGE:
						operation = "Validate Firmware Image";
						break;
					case OpCodes.ACTIVATE_FIRMWARE_AND_RESET:
						operation = "Activate Firmware and Reset";
						break;
				}
				
				// status code
				switch (b[2]) {
					case DFUStatus.SUCCESS:
						status = "Success";
						break;
					case DFUStatus.INVALID_STATE:
						status = "Invalid State";
						break;
					case DFUStatus.NOT_SUPPORTED:
						status = "Not Supported";
						break;
					case DFUStatus.DATA_SIZE_EXCEEDS_LIMITS:
						status = "Data Size Exceeds Limits";
						break;
					case DFUStatus.CRC_ERROR:
						status = "CRC Error";
						break;
					case DFUStatus.OPERATION_FAILED:
						status = "Operation Failed";
						break;
				}
				
				Logger.d(TAG, "Notification received from operation: " + operation + ". Status: " + status);

				// if op code was not success, we've failed
				if (b[2] != DFUStatus.SUCCESS) {
					failedFlash();
				} else {
					// notification from nrf, continue running state machine
					if (b[1] == OpCodes.START_DFU && state.get() == UploadStatus.SET_IMAGE_SIZE) {
						setReceiveFirmware();
					} else if (b[1] == OpCodes.RECEIVE_FIRMWARE_IMAGE &&  state.get() == UploadStatus.SET_VALIDATE_FIRMWARE) {
						setValidateFirmware();
					} else if (b[1] == OpCodes.VALIDATE_FIRMWARE_IMAGE &&  state.get() == UploadStatus.SET_VALIDATE_FIRMWARE) {
						setActivateAndReset();
					}
				}
			}

			@Override
			public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
				
			}

			@Override
			public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
				if (status != BluetoothGatt.GATT_SUCCESS) {
					failedFlash();
					return;
				}
				
				// this only happens initially when the CCCD is written to
				setStartDfu();
			}

			@Override
			public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
				
			}

			@Override
			public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
				
			}
		};

	}
	
	// set our state to failed, disconnect
	private void failedFlash() {
		state.set(UploadStatus.FAILED);
		disconnect();
		mUIHandler.post(new Runnable() {
			@Override
			public void run() {
				if (delegate != null)
					delegate.DFUTargetDidFailFlash();
			}
		});
	}

	public void disconnect() {
		if (mBluetoothGatt != null) {
			mBluetoothGatt.close();
			mBluetoothGatt = null;
		}
	}

	// DFU defines
	private UUID controlStateUUID = UUID.fromString("00001531-1212-EFDE-1523-785FEABCD123");
	private UUID packetUUID = UUID.fromString("00001532-1212-EFDE-1523-785FEABCD123");
	private UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	private BluetoothGattCharacteristic controlStateCharacteristic;
	private BluetoothGattCharacteristic packetCharacteristic;
	private BluetoothGattDescriptor CCCDDescriptor;

	private BluetoothGattCallback mGattCallback;
	
	private void setCccd() {
		CCCDDescriptor.setValue(new byte[] { (byte) 0x01, (byte) 0x00 });
		mBluetoothGatt.writeDescriptor(CCCDDescriptor);
	}
	
	private void setStartDfu() {
		state.set(UploadStatus.START_DFU);
		
		// to erase or not to erase. that is the question.
		if (restoreToFactorySettings)
			controlStateCharacteristic.setValue(new byte[] { (byte) OpCodes.OP_CODE_START_DFU_FULL_ERASE });
		else
			controlStateCharacteristic.setValue(new byte[] { (byte) OpCodes.START_DFU });
		
		mBluetoothGatt.writeCharacteristic(controlStateCharacteristic);
	}
	
	private void setImageSize() {
		state.set(UploadStatus.SET_IMAGE_SIZE);
		byte[] b = intToByteArray(buffer.length);
		packetCharacteristic.setValue(b);
		mBluetoothGatt.writeCharacteristic(packetCharacteristic);
	}
	
	private void setReceiveFirmware() {
		state.set(UploadStatus.SET_RECEIVE_FIRMWARE);
		controlStateCharacteristic.setValue(new byte[] { 0x03 });
		mBluetoothGatt.writeCharacteristic(controlStateCharacteristic);
	}
	
	private void writePayloadChunk() {
		// wait on background thread
		Thread thread = new Thread() {
		    @Override
		    public void run() {
		    	// we're still writing
		    	state.set(UploadStatus.WRITE_PAYLOAD_CHUNK);
		    	
		    	// doesn't seem to work without slowing this down manually
				// 9 always fails, 10 rarely but sometimes fails
		    	try {
					Thread.sleep(25);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		    	
				// number of bytes to send to nrf this loop
				int bytesToWrite = Math.min(buffer.length - bytesSent.get(), PACKET_SIZE);
				
				// create buffer and load in bytes for this packet
				byte[] nextChunkBuf = new byte[bytesToWrite];
				for(int i=0; i<bytesToWrite; i++) {
				    nextChunkBuf[i] = buffer[bytesSent.get()];
				    bytesSent.incrementAndGet();
				}
				
				// check if this is our last packet
				bytesToWrite = Math.min(buffer.length - bytesSent.get(), PACKET_SIZE);
				if (bytesToWrite == 0)
					state.set(UploadStatus.SET_VALIDATE_FIRMWARE);

				// write the buffer over BLE
				try {
					packetCharacteristic.setValue(nextChunkBuf);
					mBluetoothGatt.writeCharacteristic(packetCharacteristic);
				} catch (Exception e) {
					failedFlash();
				}
				
				mUIHandler.post(new Runnable() {
					@Override
					public void run() {
						// progress
						if (delegate != null)
							delegate.DFUTargetDidSendDataWithProgress((float)bytesSent.get()/(float)buffer.length);
					}
				});
		    }
		};
		thread.run();
	}
	
	private void setValidateFirmware() {
		controlStateCharacteristic.setValue(new byte[] { 0x04 });
		mBluetoothGatt.writeCharacteristic(controlStateCharacteristic);
	}
	
	private void setActivateAndReset() {
		state.set(UploadStatus.SET_ACTIVATE_AND_RESET);
		controlStateCharacteristic.setValue(new byte[] { 0x05 });
		mBluetoothGatt.writeCharacteristic(controlStateCharacteristic);
	}

	// used to convert length of firmware into 4 byte array
	private static byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >> 0 & 0xFF), (byte) (value >> 8 & 0xFF),
				(byte) (value >> 16 & 0xFF), (byte) (value >> 24 & 0xFF) };
	}
}