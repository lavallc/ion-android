package io.lava.ion.fragments.notifications;

import io.lava.ion.connectivity.incomingpackets.NotificationConfigPacket;

public class NotificationInfo {
	private NotificationType notification;
	private NotificationConfigPacket config;
	
	public NotificationInfo(NotificationType notification, NotificationConfigPacket config) {
		this.notification = notification;
		this.config = config;
	}
	
	public NotificationType getNotificationType() {
		return notification;
	}
	
	public NotificationConfigPacket getNotificationConfig() {
		return config;
	}
}