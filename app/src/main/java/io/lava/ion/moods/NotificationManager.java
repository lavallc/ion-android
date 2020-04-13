package io.lava.ion.moods;

import java.util.ArrayList;

import org.json.JSONException;

import android.content.Context;

public class NotificationManager {

	// singleton instance
	private static NotificationManager instance;

	public static NotificationManager getInstance(Context context) {
		if (instance == null)
			instance = new NotificationManager(context);

		return instance;
	}

	private NotificationManager(Context context) {
		try {
			notifications = MoodJSONParser.parseNotificationJSONFromAssets(context, "json/ion-json-descriptors/ion.json");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private ArrayList<NotificationFromJSON> notifications;
	
	public ArrayList<NotificationFromJSON> getNotifications(){
		return notifications;
	}
}
