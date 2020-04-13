package io.lava.ion.connectivity.incomingpackets;

public class MoodPacket extends LumenPacket {
	private int moodId;
	
	public MoodPacket(byte[] packetBytes) {
		super(packetBytes);
		
		// grab mood id
		moodId = packetBytes[2] & 0xFF;
	}
	
	public int getMoodId() {
		return moodId;
	}
}