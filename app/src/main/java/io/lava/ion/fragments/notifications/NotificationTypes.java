package io.lava.ion.fragments.notifications;

import java.util.ArrayList;

public class NotificationTypes {
	public static ArrayList<NotificationType> notificationTypes;
	
	public static ArrayList<NotificationType> getNotificationTypes() {
		if (notificationTypes == null) {
			notificationTypes = new ArrayList<NotificationType>();
			
			// built in apps
			notificationTypes.add(new NotificationType(0x05, "Calendar Alert"));
			notificationTypes.add(new NotificationType(0x06, "Voicemail"));
			notificationTypes.add(new NotificationType(0x08, "Alarm Clock"));
			notificationTypes.add(new NotificationType(0x0B, "Email"));
			
			// 3rd party
			notificationTypes.add(new NotificationType(0x0C, "Facebook"));
			notificationTypes.add(new NotificationType(0x0D, "Facebook Messenger"));
			notificationTypes.add(new NotificationType(0x07, "Google Hangouts"));
			notificationTypes.add(new NotificationType(0x0F, "Twitter"));
			notificationTypes.add(new NotificationType(0x10, "WhatsApp"));
			notificationTypes.add(new NotificationType(0x11, "Snapchat"));
			notificationTypes.add(new NotificationType(0x12, "Skype"));
			notificationTypes.add(new NotificationType(0x13, "Kik"));
			notificationTypes.add(new NotificationType(0x14, "Google Voice"));
			notificationTypes.add(new NotificationType(0x15, "Instagram"));
			
			// everything else
			notificationTypes.add(new NotificationType(0x02, "All Others"));
		}
		
		return notificationTypes;
	}
}
