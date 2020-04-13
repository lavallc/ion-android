package io.lava.ion.services.weather;

import io.lava.ion.connectivity.LampManager;


public class WeatherData {
	// singleton instance
	private static WeatherData instance;
	
	int currentTemp, currentConditions, futureTemp, futureConditions, sunrise24Hr, sunriseMin, sunset24Hr, sunsetMin;

	String locationString = "Weather location unavailable";
	
	private long lastUpdated = 0;
	
	public static WeatherData getInstance() {
		if (instance == null) {
			instance = new WeatherData();
		}

		return instance;
	}

	// This is simply to block other classes from instantiating
	private WeatherData() {
	}
	
	public boolean dataIsValid() {
		// has the data been updated in the last 15 minutes?
		long now = System.currentTimeMillis() / 1000L;
		return ((now - lastUpdated) < (60 * 15));
	}
	
	public int getCurrentTemp() {
		return currentTemp;
	}
	
	public int getCurrentConditions() {
		return currentConditions;
	}
	
	public int getFutureTemp() {
		return futureTemp;
	}
	
	public int getFutureConditions() {
		return futureConditions;
	}
	
	public int getSunrise24Hr() {
		return sunrise24Hr;
	}
	
	public int getSunriseMin() {
		return sunriseMin;
	}
	
	public int getSunset24Hr() {
		return sunset24Hr;
	}
	
	public int getSunsetMin() {
		return sunsetMin;
	}
	
	public String getLocationString() {
		return locationString;
	}
	
	public void updateLocation(String location) {
		locationString = location;
		
		if (LampManager.getInstanceIfReady() != null) {
			// tell the lamp manager that the weather was just updated
			LampManager.getInstanceIfReady().notifyWeatherLocationChange();
		}
	}
	
	public void updateData(int currentTemp, int currentConditions, int futureTemp, int futureConditions, int sunrise24Hr, int sunriseMin, int sunset24Hr, int sunsetMin) {
		this.currentTemp = currentTemp;
		this.currentConditions = currentConditions;
		this.futureTemp = futureTemp;
		this.futureConditions = futureConditions;
		this.sunrise24Hr = sunrise24Hr;
		this.sunriseMin = sunriseMin;
		this.sunset24Hr = sunset24Hr;
		this.sunsetMin = sunsetMin;
		
		lastUpdated = System.currentTimeMillis() / 1000L;
	}
}