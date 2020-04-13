package io.lava.ion.connectivity;

import io.lava.ion.connectivity.lamp.Lamp;

import java.util.ArrayList;

public interface ILampManagerListener {
	public void onLampListUpdate();
	
	public void onDfuLampListUpdate();
	
	public void singleLampSelected(Lamp lamp);
	
	public void multipleLampsSelected(ArrayList<Lamp> lamps);
	
	public void noLampsSelected();
	
	public void onMoodChange(int moodId);
	
	public void onWeatherLocationUpdate();
}
