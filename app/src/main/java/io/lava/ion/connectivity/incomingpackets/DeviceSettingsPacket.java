package io.lava.ion.connectivity.incomingpackets;

public class DeviceSettingsPacket extends LumenPacket {
	private boolean knockEnabled, leashEnabled, notificationsEnabled, quietEnabled;
	private int shuffleTime, quietHourStart, quietMinuteStart, quietHourEnd, quietMinuteEnd;
	
	public DeviceSettingsPacket(byte[] packetBytes) {
		super(packetBytes);
		
		// grab booleans
		knockEnabled = packetBytes[2] != 0 ? true : false;
		quietEnabled = packetBytes[3] != 0 ? true : false;
		quietHourStart = packetBytes[4] & 0xFF;
		quietMinuteStart = packetBytes[5] & 0xFF;
		quietHourEnd = packetBytes[6] & 0xFF;
		quietMinuteEnd = packetBytes[7] & 0xFF;
		shuffleTime = packetBytes[8] & 0xFF;
		notificationsEnabled = packetBytes[9] != 0 ? true : false;
		leashEnabled = packetBytes[10] != 0 ? true : false;
	}
	
	public boolean getKnockEnabled() {
		return knockEnabled;
	}
	
	public boolean getShuffleEnabled() {
		return (shuffleTime > 0);
	}
	
	public int getShuffleTime() {
		return shuffleTime;
	}
	
	public boolean getLeashEnabled() {
		return leashEnabled;
	}
	
	public boolean getNotificationsEnabled() {
		return notificationsEnabled;
	}
	
	public boolean getQuietEnabled() {
		return quietEnabled;
	}
	
	public int getQuietHourStart() {
		return quietHourStart;
	}
	
	public int getQuietMinuteStart() {
		return quietMinuteStart;
	}
	
	public int getQuietHourEnd() {
		return quietHourEnd;
	}
	
	public int getQuietMinuteEnd() {
		return quietMinuteEnd;
	}
}
