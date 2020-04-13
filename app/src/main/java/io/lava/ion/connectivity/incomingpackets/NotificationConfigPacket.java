package io.lava.ion.connectivity.incomingpackets;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class NotificationConfigPacket extends LumenPacket {
	private int notificationId, pattern, hue, brightness, saturation, speed, duration;
	private boolean enabled, sticky;
	
	public NotificationConfigPacket(byte[] packetBytes) {
		super(packetBytes);
		
		// grab parameters
		notificationId = packetBytes[2] & 0xFF;
		enabled = packetBytes[3] != 0 ? true : false;
		pattern = packetBytes[4] & 0xFF;
		
		ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(packetBytes, 5, 7));
		bb.order(ByteOrder.LITTLE_ENDIAN);
		hue = bb.getShort();
		
		brightness = packetBytes[7] & 0xFF;
		saturation = packetBytes[8] & 0xFF;
		speed = packetBytes[9] & 0xFF;
		duration = packetBytes[10] & 0xFF;
		
		sticky = packetBytes[11] != 0 ? true : false;
	}
	
	public NotificationConfigPacket(int notificationId, boolean enabled, int pattern, int hue, int brightness, int saturation, int speed, int duration, boolean sticky) {
		super((new byte[] {(byte)0x00, (byte)0x00}));
		
		this.notificationId = notificationId;
		this.enabled = enabled;
		this.pattern = pattern;
		this.hue = hue;
		this.brightness = brightness;
		this.saturation = saturation;
		this.speed = speed;
		this.duration = duration;
		this.sticky = sticky;
	}
	
	public int getNotificationId() {
		return notificationId;
	}
	
	public boolean getEnabled() {
		return enabled;
	}
	
	public int getNotificationType() {
		return pattern;
	}
	
	public int getNotificationHue() {
		return hue;
	}
	
	public int getNotificationBrightness() {
		return brightness;
	}
	
	public int getNotificationSaturation() {
		return saturation;
	}
	
	public int getNotificationDuration() {
		return duration;
	}
	
	public int getNotificationSpeed() {
		return speed;
	}
	
	public boolean getNotificationSticky() {
		return sticky;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}