package io.lava.ion.fragments.notifications;

public class NotificationType {
	private int notificationId;
	private String notificationTitle;
	
	public NotificationType(int notificationId, String notificationTitle) {
		this.notificationId = notificationId;
		this.notificationTitle = notificationTitle;
	}
	
	public int getNotificationId() {
		return notificationId;
	}
	
	public String getNotificationTitle() {
		return notificationTitle;
	}
}
