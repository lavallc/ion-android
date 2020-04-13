package io.lava.ion.connectivity;

import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.firmware.DFULamp;
import android.bluetooth.BluetoothDevice;

public interface ILampDiscoveryDelegate {
	public void didDiscoverLamp(Lamp lamp);
	
	public void didSeeLamp(BluetoothDevice device, double rssi);
	
	public boolean isLampKnown(String macAddress);
	
	public void didDiscoverDfuLamp(DFULamp lamp);
	
	public void didSeeDfuLamp(BluetoothDevice device, double rssi);
	
	public boolean isDfuLampKnown(String macAddress);
}
