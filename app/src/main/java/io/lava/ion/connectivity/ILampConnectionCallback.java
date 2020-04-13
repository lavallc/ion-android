package io.lava.ion.connectivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

public interface ILampConnectionCallback {
	public void didReadRemoteRssi(double rssi);
	
	public void didReadRemoteDevice(BluetoothDevice device);

	public void didConnect();

	public void didDiscoverServices(BluetoothGattCharacteristic control);
	
	public void readyToBeginInit();

	public void didDisconnect();
	
	public void onLampGenericNotify(byte[] packet);
}
