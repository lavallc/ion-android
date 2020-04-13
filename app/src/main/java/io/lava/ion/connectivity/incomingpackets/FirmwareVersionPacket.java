package io.lava.ion.connectivity.incomingpackets;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class FirmwareVersionPacket extends LumenPacket {
	private int protocolVersion, firmwareVersion;
	private boolean bonded;
	
	public FirmwareVersionPacket(byte[] packetBytes) {
		super(packetBytes);

		// grab firmware version
		ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(packetBytes, 2, packetBytes.length-1));
		bb.order(ByteOrder.LITTLE_ENDIAN);
		protocolVersion = bb.getShort();
		firmwareVersion = bb.getShort();
		bonded = packetBytes[6] != 0 ? true : false;
	}
	
	public int getProtocolVersion() {
		return protocolVersion;
	}
	
	public int getFirmwareVersion() {
		return firmwareVersion;
	}
	
	public boolean isBonded() {
		return bonded;
	}
}
