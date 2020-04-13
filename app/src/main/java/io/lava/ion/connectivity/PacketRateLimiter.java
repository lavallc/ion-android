package io.lava.ion.connectivity;

import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.connectivity.outgoingpackets.BaseOutgoingPacket;

import java.util.LinkedList;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

public class PacketRateLimiter {
	public static final int TYPE_LOSSLESS = 1;
	public static final int TYPE_LOSSY = 2;
	
	private LinkedList<BaseOutgoingPacket> waitingPacketQueue;
	
	private int lossy;
	private boolean waitingOnResponse;
	
	private BLEController controller;
	private BluetoothGattCharacteristic control;
	private BluetoothGatt mBluetoothGatt;
	private Lamp lamp;
	
	public PacketRateLimiter(final Lamp lamp, final BLEController controller, final BluetoothGattCharacteristic control, final BluetoothGatt mBluetoothGatt, int lossy) {
		waitingPacketQueue = new LinkedList<BaseOutgoingPacket>();
		this.controller = controller;
		this.control = control;
		this.mBluetoothGatt = mBluetoothGatt;
		this.lossy = lossy;
		this.waitingOnResponse = false;
		this.lamp = lamp;
		
		if (lossy != TYPE_LOSSLESS && lossy != TYPE_LOSSY) {
			throw new IllegalArgumentException("Invalid limiter type.");
		}
	}
	
	public void enqueue(BaseOutgoingPacket outgoingPacket) {
		if (lossy == TYPE_LOSSY) {
			if (waitingPacketQueue.size() == 0)
				waitingPacketQueue.add(outgoingPacket);
			else
				waitingPacketQueue.set(0, outgoingPacket);
		} else {
			waitingPacketQueue.add(outgoingPacket);
		}
		
		if (!waitingOnResponse) {
			waitingOnResponse = true;
			sendNextWaitingPacket();
		}
	}
	
	private void sendNextWaitingPacket() {
		controller.enqueuePacket(lamp, control, mBluetoothGatt, waitingPacketQueue.pop(), new OnPacketResponseListener() {
			@Override
			public void onAck(byte[] response) {

			}

			@Override
			public void onNak(int nakCode) {
				
			}

			// sometimes we don't get notify's when rapidly sending lots of packets
			// best to limit ourselves based on when our packets are written
			@Override
			public void onWritten() {
				finishTransmit();
			}
		});
	}

	private void finishTransmit() {
		if (!waitingPacketQueue.isEmpty()) {
			sendNextWaitingPacket();
		} else {
			waitingOnResponse = false;
		}
	}
	
	public void clear() {
		waitingPacketQueue.clear();
	}
}
