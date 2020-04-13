package io.lava.ion.connectivity.incomingpackets;

public class LumenPacket {
	private int reqId;
	private int opCode;
	
	public LumenPacket(byte[] packetBytes) {
		this.reqId = packetBytes[0] & 0xFF;
		this.opCode = packetBytes[1] & 0xFF;
	}
	
	public int getOpCode() {
		return opCode;
	}
	
	public int getRequestId() {
		return reqId;
	}
}