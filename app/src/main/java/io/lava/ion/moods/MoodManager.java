package io.lava.ion.moods;

import java.util.ArrayList;

import org.json.JSONException;

import android.content.Context;

public class MoodManager {

	// singleton instance
	private static MoodManager instance;

	public static MoodManager getInstance(Context context) {
		if (instance == null)
			instance = new MoodManager(context);

		return instance;
	}

	private MoodManager(Context context) {
		try {
			moods = MoodJSONParser.parseMoodJSONFromAssets(context, "json/ion-json-descriptors/ion.json");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private ArrayList<MoodFromJSON> moods;
	
	public ArrayList<MoodFromJSON> getMoods(){
		return moods;
	}
	
	public ArrayList<Integer> getAllMoodIds() {
		ArrayList<Integer> mIds = new ArrayList<Integer>();
		
		for (MoodFromJSON m : moods) {
			mIds.add(m.getId());
		}
		
		return mIds;
	}
	
	public MoodFromJSON getMoodById(int id) {
		for (MoodFromJSON m : moods) {
			if (m.getId() == id)
				return m;
		}
		
		return null;
	}
}
