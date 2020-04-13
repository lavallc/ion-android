package io.lava.ion.connectivity.outgoingpackets;

import io.lava.ion.connectivity.PacketConstants.OpCode;

import java.nio.charset.Charset;

public class SetDeviceNamePacket extends BaseOutgoingPacket {
	private int reqId;
	private String name;
	
	public SetDeviceNamePacket(String name) {
		this.name = name;
	}

	@Override
	public void setReqId(int reqId) {
		this.reqId = reqId;
	}

	@Override
	public byte[] toBytes() {
		byte[] nameArr = name.getBytes(Charset.forName("UTF-8"));
		
		byte[] setDeviceNamePacket = new byte[nameArr.length + 2];
		
		setDeviceNamePacket[0] = (byte)reqId;
		setDeviceNamePacket[1] = (byte)OpCode.setDeviceName;
		for (int i = 0; i < nameArr.length; i++) {
			setDeviceNamePacket[2+i] = nameArr[i];
		}
		
		return setDeviceNamePacket;
	}

}
