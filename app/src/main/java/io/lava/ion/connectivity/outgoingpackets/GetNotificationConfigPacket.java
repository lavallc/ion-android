package io.lava.ion.connectivity.outgoingpackets;

import io.lava.ion.connectivity.PacketConstants.OpCode;

public class GetNotificationConfigPacket extends BaseOutgoingPacket {
	private int reqId, notificationId;
	
	public GetNotificationConfigPacket(int notificationId) {
		this.notificationId = notificationId;
	}

	@Override
	public void setReqId(int reqId) {
		this.reqId = reqId;
	}

	@Override
	public byte[] toBytes() {
		return new byte[] {
			(byte)reqId,							// request ID
			(byte)OpCode.getNotificationConfig,		// op code
			(byte)notificationId					// (notification Ids listed above)
		};
	}

}
