package io.lava.ion.moods;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class MoodJSONParser {
	
	private MoodJSONParser(){}//block instantiation

	public static ArrayList<MoodFromJSON> parseMoodJSONFromAssets(Context context, String JSONFileName) 
			throws JSONException{
		JSONObject json = new JSONObject(loadJSONFromAsset(context, JSONFileName));

		ArrayList<MoodFromJSON> parsedMoods = new ArrayList<MoodFromJSON>();
		
		JSONArray moods = json.getJSONArray("moods");
		for(int i = 0; i < moods.length(); i++){
			JSONObject mood = moods.getJSONObject(i);

			ArrayList<MoodConfigFromJSON> configs = getConfigs(mood);
			
			parsedMoods.add(new MoodFromJSON(mood.getString("name"), 
										 (byte)mood.getInt("id"), 
										 mood.getString("image"), 
										 mood.getString("desc"), 
										 configs));
		}
		
		return parsedMoods;
	}
	
	public static ArrayList<NotificationFromJSON> parseNotificationJSONFromAssets(Context context, String JSONFileName) 
			throws JSONException{
		JSONObject json = new JSONObject(loadJSONFromAsset(context, JSONFileName));

		ArrayList<NotificationFromJSON> parsedNotifications = new ArrayList<NotificationFromJSON>();
		
		JSONArray notifications = json.getJSONArray("notifications");
		for(int i = 0; i < notifications.length(); i++){
			JSONObject notification = notifications.getJSONObject(i);

			ArrayList<MoodConfigFromJSON> configs = getConfigs(notification);
			
			parsedNotifications.add(new NotificationFromJSON(notification.getString("name"),
										(byte)notification.getInt("id"),
										notification.getString("image"),
										notification.getString("desc"),
										configs));
		}
		
		return parsedNotifications;
	}
	
	private static String loadJSONFromAsset(Context context, String JSONFileName) {
	    String json = null;
	    try {

	        InputStream is = context.getAssets().open(JSONFileName);

	        int size = is.available();

	        byte[] buffer = new byte[size];

	        is.read(buffer);

	        is.close();

	        json = new String(buffer, "UTF-8");


	    } catch (IOException ex) {
	        ex.printStackTrace();
	        return null;
	    }
	    return json;

	}
	
	private static ArrayList<MoodConfigFromJSON> getConfigs(JSONObject mood) throws JSONException{
		JSONArray configsJSON = mood.getJSONArray("configs");
		ArrayList<MoodConfigFromJSON> configs = new ArrayList<MoodConfigFromJSON>();
		for(int j = 0; j < configsJSON.length(); j++){
			JSONObject config = configsJSON.getJSONObject(j);
			configs.add(new MoodConfigFromJSON(config.getString("name"),
									   config.getInt("id"),
									   config.getInt("min"),
									   config.getInt("max"),
									   config.getInt("default"),
									   config.getString("widget"),
									   config.getString("label")));
		}
		
		return configs;
	}
}
