package io.lava.ion.connectivity.lamp;

import io.lava.ion.connectivity.incomingpackets.NotificationConfigPacket;

public abstract class NotificationConfigCallback {
	public abstract void onSuccess(NotificationConfigPacket notificationConfig);
	
	public abstract void onFail();
}
