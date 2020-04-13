package io.lava.ion.connectivity.incomingpackets;

public class NakPacket extends LumenPacket {
	private int responseCode;
	
	public NakPacket(byte[] packetBytes) {
		super(packetBytes);
		responseCode = packetBytes[2] & 0xFF;
	}
	
	public int getResponseCode() {
		return responseCode;
	}
}
