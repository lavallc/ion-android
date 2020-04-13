package io.lava.ion.connectivity.outgoingpackets;

import io.lava.ion.connectivity.PacketConstants.OpCode;

public class SetTimePacket extends BaseOutgoingPacket {
	private int reqId, hr24, min, sec;
	
	public SetTimePacket(int hr24, int min, int sec) {
		this.hr24 = hr24;
		this.min = min;
		this.sec = sec;
	}

	@Override
	public void setReqId(int reqId) {
		this.reqId = reqId;
	}

	@Override
	public byte[] toBytes() {
		return new byte[] {
			(byte)reqId,						// request ID
			(byte)OpCode.setTime,				// op code
			(byte)hr24,							// current hour
			(byte)min,							// current minute
			(byte)sec							// current second
		};
	}

}
