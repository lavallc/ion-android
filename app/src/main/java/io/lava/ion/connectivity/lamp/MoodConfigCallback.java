package io.lava.ion.connectivity.lamp;

public abstract class MoodConfigCallback {
	public abstract void onSuccess(MoodConfig moodConfig);
	
	public abstract void onFail();
}
