package io.lava.ion.connectivity.outgoingpackets;

import io.lava.ion.connectivity.PacketConstants.OpCode;

import java.util.ArrayList;

public class UpdateRotationPacket extends BaseOutgoingPacket {
	private int reqId;
	private ArrayList<Integer> rotation;
	
	public UpdateRotationPacket(ArrayList<Integer> rotation) {
		this.rotation = rotation;
	}

	@Override
	public void setReqId(int reqId) {
		this.reqId = reqId;
	}

	@Override
	public byte[] toBytes() {
		byte[] updateRotationPacket = new byte[rotation.size() + 2];
		
		updateRotationPacket[0] = (byte)reqId;
		updateRotationPacket[1] = (byte)OpCode.updateRotation;
		for (int i = 0; i < rotation.size(); i++) {
			updateRotationPacket[2+i] = rotation.get(i).byteValue();
		}
		
		return updateRotationPacket;
	}

}
