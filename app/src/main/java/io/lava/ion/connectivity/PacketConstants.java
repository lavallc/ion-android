package io.lava.ion.connectivity;

public final class PacketConstants {

	public static class OpCode {
		public final static int ack = 0x01;
		public final static int nak = 0x02;
		public final static int init = 0x03;
		public final static int setDeviceName = 0x04;
		public final static int getDeviceSettings = 0x05;
		public final static int setDeviceSettings = 0x06;
		public final static int setWeather = 0x07;
		public final static int setTime = 0x08;
		public final static int setMoodConfig = 0x09;
		public final static int getMoodConfig = 0x0A;
		public final static int saveMoodConfigs = 0x0B;
		public final static int restoreMoodConfigs = 0x0C;
		public final static int setNotificationConfig = 0x0D;
		public final static int getNotificationConfig = 0x0E;
		public final static int setCurrentMood = 0x0F;
		public final static int getCurrentMood = 0x10;
		public final static int triggerNotification = 0x11;
		public final static int updateRotation = 0x12;
		public final static int getRotation = 0x13;
		public final static int enterDFUMode = 0x14;
		public final static int showNotification = 0x15;
		public final static int rawUpdate = 0x16;
		public final static int beginBond = 0x17;
		public final static int clearNotification = 0x18;
		public final static int setRawSettings = 0x19;
	}
	
	public static class DeviceTypes {
		public final static int ios = 0x01;
		public final static int android = 0x02;
		public final static int ionode = 0x03;
	}
	
	public static class NakCodes {
		public final static int invalidOpCode = 0x01;
		public final static int unknownDeviceType = 0x02;
		public final static int invalidDeviceName = 0x03;
		public final static int invalidDeviceSettings = 0x04;
		public final static int invalidWeather = 0x05;
		public final static int invalidTime = 0x06;
		public final static int invalidMoodConfig = 0x07;
		public final static int unknownMood = 0x08;
		public final static int unknownMoodConfig = 0x09;
		public final static int invalidNotificationConfig = 0x0A;
		public final static int unknownNotification = 0x0B;
		public final static int invalidRotation = 0x0C;
		public final static int bondFailed = 0x0D;
		public final static int noBond = 0x0E;
		public final static int noInit = 0x0F;
	}
	
	public static class WeatherCodes {
		public final static int clear = 0x01;
		public final static int clouds = 0x02;
		public final static int rain = 0x03;
		public final static int snow = 0x04;
		public final static int thunderstorm = 0x05;
		public final static int hazy = 0x06;
	}
	
	public static class NotificationIds {
		public final static int catchAllOthers = 0x02;
		public final static int sms = 0x03;
		public final static int calendar = 0x05;
		public final static int voicemail = 0x06;
		public final static int googleHangouts = 0x07;
		public final static int alarmClock = 0x08;
		public final static int incomingCall = 0x09;
		public final static int missedCall = 0x0A;
		public final static int email = 0x0B;
		public final static int facebook = 0x0C;
		public final static int facebookMessenger = 0x0D;
		public final static int googlePlus = 0x0E;
		public final static int twitter = 0x0F;
		public final static int whatsApp = 0x10;
		public final static int snapchat = 0x11;
		public final static int skype = 0x12;
		public final static int kik = 0x13;
		public final static int googleVoice = 0x14;
		public final static int instagram = 0x15;
	}
	
}