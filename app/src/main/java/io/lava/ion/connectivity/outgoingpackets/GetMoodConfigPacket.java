package io.lava.ion.connectivity.outgoingpackets;

import io.lava.ion.connectivity.PacketConstants.OpCode;

public class GetMoodConfigPacket extends BaseOutgoingPacket {
	private int reqId, moodId, configId;
	
	public GetMoodConfigPacket(int moodId, int configId) {
		this.moodId = moodId;
		this.configId = configId;
	}

	@Override
	public void setReqId(int reqId) {
		this.reqId = reqId;
	}

	@Override
	public byte[] toBytes() {
		return new byte[] {
			(byte)reqId,							// request ID
			(byte)OpCode.getMoodConfig,				// op code
			(byte)moodId,							// mood id
			(byte)configId							// mood config id
		};
	}

}
