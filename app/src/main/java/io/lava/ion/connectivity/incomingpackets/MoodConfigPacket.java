package io.lava.ion.connectivity.incomingpackets;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class MoodConfigPacket extends LumenPacket {
	private int moodId, configId;
	private int configVal;
	
	public MoodConfigPacket(byte[] packetBytes) {
		super(packetBytes);
		
		// grab parameters
		moodId = packetBytes[2] & 0xFF;
		configId = packetBytes[3] & 0xFF;
		ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(packetBytes, 4, packetBytes.length));
		bb.order(ByteOrder.LITTLE_ENDIAN);
		configVal = bb.getInt();
	}
	
	public int getMoodId() {
		return moodId;
	}
	
	public int getConfigId() {
		return configId;
	}
	
	public int getConfigVal() {
		return configVal;
	}
}