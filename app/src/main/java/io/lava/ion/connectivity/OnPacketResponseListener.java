package io.lava.ion.connectivity;

public abstract class OnPacketResponseListener {
	// ack is used for both ack packets and successful individual responses
	public abstract void onAck(byte[] response);

	public abstract void onNak(int nakCode);
	
	public abstract void onWritten();
}
