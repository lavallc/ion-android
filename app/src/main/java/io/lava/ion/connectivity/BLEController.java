package io.lava.ion.connectivity;

import io.lava.ion.connectivity.PacketConstants.OpCode;
import io.lava.ion.connectivity.incomingpackets.LumenPacket;
import io.lava.ion.connectivity.incomingpackets.NakPacket;
import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.connectivity.outgoingpackets.BaseOutgoingPacket;
import io.lava.ion.logger.Logger;
import io.lava.ion.wakelock.WakeLockManager;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
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

public class BLEController extends Thread {
	// singleton instance of this class
	private static BLEController instance;
	
	// should be our service's context
	private Context context;
	
	// ION UUIDs
	private static final UUID ionUUID = UUID.fromString("2e8b0001-c4b7-43f1-bbdd-6285294a4116");
	private static final UUID controlCharUUID = UUID.fromString("2e8b0002-c4b7-43f1-bbdd-6285294a4116");
	private static final UUID notifyCharUUID = UUID.fromString("2e8b0003-c4b7-43f1-bbdd-6285294a4116");
	
	// generic UUIDs
	private static final UUID genericNotifyDescriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	
	// We need Integer and not int because HashMap's refined tastes are too elitist for such primitive types
	@SuppressLint("UseSparseArrays")
	private ConcurrentHashMap<Integer, OnPacketResponseListener> packetListenerMap = new ConcurrentHashMap<Integer, OnPacketResponseListener>();
	
	// incremented by one for each packet, wraps around at 0xFE
	private AtomicInteger nextReqId = new AtomicInteger(0);
	
	// any request ID of 0xFF came from the lamp and was not requested by us
	private static final int NOTIFY_FROM_LAMP_REQID = 0xFF;
	
	// handler for posting callbacks to
	private Handler mCallbackHandler;
	
	private Object transmitComplete = new Object(); 
	private LinkedBlockingQueue<Runnable> queuedPackets;
	
	public static BLEController getInstance(Context context) {
		if (context != null) {
			// singleton
			if (instance == null) {
				instance = new BLEController();
				instance.context = context;
				
				// create a handler on THIS THREAD (should be our app's UI thread)
				instance.mCallbackHandler = new Handler(Looper.getMainLooper());
				
				instance.queuedPackets = new LinkedBlockingQueue<Runnable>();
				
				// start our packet transmit thread
				instance.start();
			}
				
			return instance;
		} else {
			return null;
		}
	}

	private BLEController() {
		// block instantiation
	}
	
	public BluetoothGatt connect(BluetoothDevice device, final ILampConnectionCallback callback) {
		try {
			// a lamp is trying to connect, update any GUIs displaying lamps
			if (LampManager.getInstanceIfReady() != null) {
				LampManager.getInstanceIfReady().notifyListenersOfLampListUpdates();
			}
			
			final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
				@Override
				public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
					// let lamp objects know when they are connected/disconnected
					if (newState == BluetoothProfile.STATE_CONNECTED) {
						mCallbackHandler.post(new Runnable() {
					        public void run() {
					        	callback.didConnect();
					        	
					        	// a lamp connected, update any GUIs displaying lamps
					    		if (LampManager.getInstanceIfReady() != null) {
					    			LampManager.getInstanceIfReady().notifyListenersOfLampListUpdates();
					    		}
					        }
					    });
						gatt.discoverServices();
					} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
						mCallbackHandler.post(new Runnable() {
					        public void run() {
					        	callback.didDisconnect();
					        	
					        	// a lamp disconnected, update any GUIs displaying lamps
					    		if (LampManager.getInstanceIfReady() != null) {
					    			LampManager.getInstanceIfReady().notifyListenersOfLampListUpdates();
					    		}
					        }
					    });
					}
				}
	
				@Override
				public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
					// get the ION service
					BluetoothGattService ionService = gatt.getService(ionUUID);
					
					if (ionService != null) {
						// register for notifications
						BluetoothGattCharacteristic notify = ionService.getCharacteristic(notifyCharUUID);
						gatt.setCharacteristicNotification(notify, true);
		
						BluetoothGattDescriptor descriptor = notify.getDescriptor(genericNotifyDescriptorUUID);
						descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
						gatt.writeDescriptor(descriptor);
		
						// get the ION control characteristic
						final BluetoothGattCharacteristic control = ionService.getCharacteristic(controlCharUUID);
		
						// hand the characteristic off to the Lamp object
						mCallbackHandler.post(new Runnable() {
					        public void run() {
					        	callback.didDiscoverServices(control);
					        }
					    });
					} else {
						// maybe we were just in DFU mode?
						Logger.i(this.getClass().getSimpleName(), "could not find ion service");
						
						// let's just tell the lamp object that we're disconnecting
						mCallbackHandler.post(new Runnable() {
					        public void run() {
					        	callback.didDisconnect();
					        }
					    });
					}
				}
	
				@Override
				public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
	
				}
	
				@Override
				public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
					Thread thread = new Thread() {
					    @Override
					    public void run() {
					    	// this is needed due to Android's buggy BLE implementation
					    	// we really only need this when we're talking to multiple lamps simultaneously
					    	try {
								Thread.sleep(30);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
					    	
					    	// write complete, we're ready to send more packets
					    	synchronized (transmitComplete) {
			        			transmitComplete.notify();
			        		}
					    }
					};
					
					thread.run();
				}
	
				@Override
				public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
					// this is called every time we get a notification from the lamp
					
					// get the raw bytes of the packet
					final byte[] packetBytes = characteristic.getValue();
					
					// initial notify from lamp, this means the lamp is ready
					if (packetBytes.length == 2 && packetBytes[0] == 1 && packetBytes[1] == 0) {
						mCallbackHandler.post(new Runnable() {
					        public void run() {
					        	callback.readyToBeginInit();
					        }
					    });
						return;
					}
					
					// create a lumen packet for easy access of opcode/reqid
					LumenPacket lPacket = new LumenPacket(packetBytes);
					
					// try to find a listener matching this request ID
					final OnPacketResponseListener listener = packetListenerMap.get(lPacket.getRequestId());
					
					// inspect the request ID and route accordingly
					if (lPacket.getRequestId() == NOTIFY_FROM_LAMP_REQID) {
						// this was a 0xFF packet, it was not app requested
						mCallbackHandler.post(new Runnable() {
					        public void run() {
					        	callback.onLampGenericNotify(packetBytes);
					        }
					    });
					} else if (listener != null) {
						// we found a callback matching this ID, let's hand it off
						switch (lPacket.getOpCode()) {
							case OpCode.nak:
								final NakPacket nakPacket = new NakPacket(packetBytes);
								mCallbackHandler.post(new Runnable() {
							        public void run() {
							        	listener.onNak(nakPacket.getResponseCode());
							        }
							    });
								break;
							default:
								mCallbackHandler.post(new Runnable() {
							        public void run() {
							        	listener.onAck(packetBytes);
							        }
							    });
								break;
						}
						
						// remove this callback from our listener map
						packetListenerMap.remove(lPacket.getRequestId());
					} else {
						// no callback associated with this packet
					}
				}
	
				@Override
				public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
	
				}
	
				@Override
				public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
	
				}
	
				@Override
				public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
	
				}
	
				@Override
				public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, int status) {
					mCallbackHandler.post(new Runnable() {
				        public void run() {
				        	callback.didReadRemoteRssi(rssi);
				        }
				    });
				}
			};
	
			return device.connectGatt(context, false, mGattCallback);
		} catch (Exception e) {
			// we couldn't connect for some reason
			return null;
		}
	}
	
	public void updateRssi(final BluetoothGatt mBluetoothGatt) {
		// our callback will receive the update
		mBluetoothGatt.readRemoteRssi();
	}
	
	public void enqueuePacket(final Lamp lamp, final BluetoothGattCharacteristic control, final BluetoothGatt mBluetoothGatt, final BaseOutgoingPacket outgoingPacket, final OnPacketResponseListener listener) {
		Logger.i(this.getClass().getSimpleName(), "ENQUEING PACKET");
		
		// get the next request ID
		int reqId = (nextReqId.getAndIncrement() % 255);
		
		// set the packet's request ID to the ID we just received
		outgoingPacket.setReqId(reqId);
		
		// map the packet's callback to this request ID
		if (listener != null)
			packetListenerMap.put(reqId, listener);
		
		// post this outgoing packet to the queue
		try {
			queuedPackets.put(new Runnable() {
				@Override
				public void run() {
					Logger.i(this.getClass().getSimpleName(), "SENDING PACKET");
					
					// make sure the lamp still exists and is connected
		        	if (lamp == null || lamp.getConnectivityState() == Lamp.LampState.DISCONNECTED) {
		        		// we're not connected, ignore this packet
		        		synchronized (transmitComplete) {
		        			transmitComplete.notify();
		        		}
		        		return;
		        	}
		        	
		        	// send the packet to the lamp
		        	control.setValue(outgoingPacket.toBytes());
					mBluetoothGatt.writeCharacteristic(control);
					
					// notify our listener
					mCallbackHandler.post(new Runnable() {
				        public void run() {
							if (listener != null)
								listener.onWritten();
				        }
				    });
				}
			});
		} catch (InterruptedException e) {}
	}
	
	@Override
	public void run() {
		synchronized (transmitComplete) {
			while (true) {
				try {
					Runnable r = queuedPackets.take();
					
					// grab wake lock
					mCallbackHandler.post(new Runnable() {
				        public void run() {
							WakeLockManager.getInstance(context).setPacketsInBLEQueue(true);
				        }
				    });
					
					r.run();
					
					// wait for transmitting to finish
					transmitComplete.wait();
					
					// release wake lock if we're done
					if (queuedPackets.size() == 0) {
						mCallbackHandler.post(new Runnable() {
					        public void run() {
								WakeLockManager.getInstance(context).setPacketsInBLEQueue(false);
					        }
					    });
					}
				} catch (InterruptedException e) {
					// end this thread
					break;
				}
			}
		}
	}
}
