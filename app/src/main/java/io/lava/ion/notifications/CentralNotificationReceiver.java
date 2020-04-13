package io.lava.ion.notifications;

import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.PacketConstants;

public class CentralNotificationReceiver {
	private static CentralNotificationReceiver instance;
	
	private CentralNotificationReceiver() {
		// singleton, block instantiation
	}
	
	public static CentralNotificationReceiver getInstance() {
		if (instance == null) {
			instance = new CentralNotificationReceiver();
		}
		return instance;
	}
	
	public void onAlarm() {
		forwardNotification(PacketConstants.NotificationIds.alarmClock);
	}
	
	public void onPhoneRinging() {
		forwardNotification(PacketConstants.NotificationIds.incomingCall);
	}
	
	public void onMissedCall() {
		forwardNotification(PacketConstants.NotificationIds.missedCall);
	}
	
	public void onSMS() {
		forwardNotification(PacketConstants.NotificationIds.sms);
	}
	
	public void onVoicemail() {
		forwardNotification(PacketConstants.NotificationIds.voicemail);
	}
	
	public void onFacebook() {
		forwardNotification(PacketConstants.NotificationIds.facebook);
	}
	
	public void onFacebookMessage() {
		forwardNotification(PacketConstants.NotificationIds.facebookMessenger);
	}
	
	public void onTwitter() {
		forwardNotification(PacketConstants.NotificationIds.twitter);
	}
	
	public void onWhatsApp() {
		forwardNotification(PacketConstants.NotificationIds.whatsApp);
	}
	
	public void onSnapchat() {
		forwardNotification(PacketConstants.NotificationIds.snapchat);
	}
	
	public void onEmail() {
		forwardNotification(PacketConstants.NotificationIds.email);
	}
	
	public void onInstagram() {
		forwardNotification(PacketConstants.NotificationIds.instagram);
	}
	
	public void onSkype() {
		forwardNotification(PacketConstants.NotificationIds.skype);
	}
	
	public void onKik() {
		forwardNotification(PacketConstants.NotificationIds.kik);
	}
	
	public void onHangouts() {
		forwardNotification(PacketConstants.NotificationIds.googleHangouts);
	}
	
	public void onGoogleVoice() {
		forwardNotification(PacketConstants.NotificationIds.googleVoice);
	}
	
	public void onGooglePlus() {
		forwardNotification(PacketConstants.NotificationIds.googleVoice);
	}
	
	public void onCalendar() {
		forwardNotification(PacketConstants.NotificationIds.calendar);
	}
	
	public void onCatchAllOthers() {
		forwardNotification(PacketConstants.NotificationIds.catchAllOthers);
	}
	
	private void forwardNotification(int notificationId) {
		if (LampManager.getInstanceIfReady() != null) {
			LampManager.getInstanceIfReady().sendNotificationToConnectedLamps(notificationId);
		}
	}
}
