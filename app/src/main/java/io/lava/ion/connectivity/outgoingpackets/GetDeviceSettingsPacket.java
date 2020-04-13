package io.lava.ion.connectivity.outgoingpackets;

import io.lava.ion.connectivity.PacketConstants.OpCode;

public class GetDeviceSettingsPacket extends BaseOutgoingPacket {
	private int reqId;

	@Override
	public void setReqId(int reqId) {
		this.reqId = reqId;
	}

	@Override
	public byte[] toBytes() {
		return new byte[] {
			(byte)reqId,							// request ID
			(byte)OpCode.getDeviceSettings			// op code
		};
	}

}
