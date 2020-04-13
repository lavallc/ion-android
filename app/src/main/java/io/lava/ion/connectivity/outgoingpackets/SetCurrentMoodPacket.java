package io.lava.ion.connectivity.outgoingpackets;

import io.lava.ion.connectivity.PacketConstants.OpCode;

public class SetCurrentMoodPacket extends BaseOutgoingPacket {
	private int reqId, moodId;
	
	public SetCurrentMoodPacket(int moodId) {
		this.moodId = moodId;
	}

	@Override
	public void setReqId(int reqId) {
		this.reqId = reqId;
	}

	@Override
	public byte[] toBytes() {
		return new byte[] {
			(byte)reqId,								// request ID
			(byte)OpCode.setCurrentMood,				// op code
			(byte)moodId								// id of the mood
		};
	}

}
