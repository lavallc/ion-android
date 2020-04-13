package io.lava.ion.connectivity.incomingpackets;

import java.util.ArrayList;

public class RotationPacket extends LumenPacket {
	private ArrayList<Integer> rotationMoodIds;
	
	public RotationPacket(byte[] packetBytes) {
		super(packetBytes);
		
		rotationMoodIds = new ArrayList<Integer>();
		
		// iterate over mood ids in rotation
		for (int i=0; i<packetBytes.length-2; i++) {
			rotationMoodIds.add(packetBytes[i+2] & 0xFF);
		}
	}
	
	public ArrayList<Integer> getRotationIds() {
		return rotationMoodIds;
	}
}