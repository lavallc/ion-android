package io.lava.ion.connectivity.outgoingpackets;

public abstract class BaseOutgoingPacket {
	public abstract void setReqId(int reqId);
	public abstract byte[] toBytes();
}
