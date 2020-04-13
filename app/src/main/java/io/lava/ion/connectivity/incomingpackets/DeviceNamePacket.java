package io.lava.ion.connectivity.incomingpackets;

import java.util.Arrays;

public class DeviceNamePacket extends LumenPacket {
	private String name;
	
	public DeviceNamePacket(byte[] packetBytes) {
		super(packetBytes);
		
		// grab device name
		name = new String(Arrays.copyOfRange(packetBytes, 2, packetBytes.length));
	}
	
	public String getName() {
		return name;
	}
}
