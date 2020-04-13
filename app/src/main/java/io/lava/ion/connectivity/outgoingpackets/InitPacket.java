package io.lava.ion.connectivity.outgoingpackets;

import io.lava.ion.connectivity.PacketConstants.DeviceTypes;
import io.lava.ion.connectivity.PacketConstants.OpCode;

public class InitPacket extends BaseOutgoingPacket {
	private int reqId;
    private byte[] macAddress;

    public InitPacket(String privateMacAddress) {
        String noColonMacAddress = privateMacAddress.replace(":", "");

        macAddress = new byte[6];
        for (int i = 0; i < noColonMacAddress.length(); i += 2) {
            macAddress[i / 2] = (byte) ((Character.digit(noColonMacAddress.charAt(i), 16) << 4)
                    + Character.digit(noColonMacAddress.charAt(i+1), 16));
        }
    }

	@Override
	public void setReqId(int reqId) {
		this.reqId = reqId;
	}

	@Override
	public byte[] toBytes() {
        byte[] initPacket = new byte[9];

        initPacket[0] = (byte)reqId;
        initPacket[1] = (byte)OpCode.init;
        initPacket[2] = (byte)DeviceTypes.android;
        for (int i = 0; i < 6; i++) {
            initPacket[3+i] = macAddress[i];
        }

        return initPacket;
	}

}
