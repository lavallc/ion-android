package io.lava.ion.connectivity.lamp;


public class MoodConfig {
	int moodId, configId, configVal;
	
	public MoodConfig(int moodId, int configId, int configVal) {
		this.moodId = moodId;
		this.configId = configId;
		this.configVal = configVal;
	}
	
	public int getMoodId() {
		return moodId;
	}
	
	public int getConfigId() {
		return configId;
	}
	
	public int getConfigVal() {
		return configVal;
	}
}
