package io.lava.ion.connectivity.outgoingpackets;

import io.lava.ion.connectivity.PacketConstants.OpCode;

public class ShowNotificationPacket extends BaseOutgoingPacket {
	private int reqId, pattern, hue, brightness, saturation, speed, duration;
	private boolean sticky;

	public ShowNotificationPacket(int pattern, int hue, int brightness, int saturation, int speed, int duration, boolean sticky) {
		this.pattern = pattern;
		this.hue = hue;
		this.brightness = brightness;
		this.saturation = saturation;
		this.speed = speed;
		this.duration = duration;
		this.sticky = sticky;
	}
	
	@Override
	public void setReqId(int reqId) {
		this.reqId = reqId;
	}

	@Override
	public byte[] toBytes() {
		return new byte[] {
			(byte) reqId,								// request ID
			(byte) OpCode.showNotification,				// op code
			(byte) pattern,								// notification pattern
			(byte) (hue),
			(byte) (hue >> 8), 
			(byte) brightness,							// speed of notification
			(byte) saturation,
			(byte) speed,
			(byte) duration,
			(byte) (sticky ? 0x01 : 0x00),				// is this a sticky notification
		};
	}

}