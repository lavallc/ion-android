package io.lava.ion.connectivity.outgoingpackets;

import io.lava.ion.connectivity.PacketConstants.OpCode;

public class SetMoodConfigPacket extends BaseOutgoingPacket {
	private int reqId, moodId, configId;
	private int configVal;
	
	public SetMoodConfigPacket(int moodId, int configId, int configVal) {
		this.moodId = moodId;
		this.configId = configId;
		this.configVal = configVal;
	}

	@Override
	public void setReqId(int reqId) {
		this.reqId = reqId;
	}

	@Override
	public byte[] toBytes() {
		return new byte[] {
			(byte) reqId,						// request ID
			(byte) OpCode.setMoodConfig,		// op code
			(byte) moodId,						// mood id
			(byte) configId,					// config id
			(byte) (configVal), 				// 4 bytes for the value
			(byte) (configVal >> 8), 
			(byte) (configVal >> 16),
			(byte) (configVal >> 24)
		};
	}

}
