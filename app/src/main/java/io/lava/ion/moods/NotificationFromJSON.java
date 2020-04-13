package io.lava.ion.moods;

import java.util.ArrayList;

public class NotificationFromJSON {
	private String name;
	private int id;
	private String imageName;
	private String description;
	private ArrayList<MoodConfigFromJSON> configs;

	public NotificationFromJSON(String name, int id, String imageName, String description, ArrayList<MoodConfigFromJSON> configs) {
		this.name = name;
		this.id = id;
		this.imageName = imageName;
		this.description = description;
		this.configs = configs;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}

	public String getImageName() {
		return imageName;
	}

	public String getDescription() {
		return description;
	}

	public ArrayList<MoodConfigFromJSON> getConfigs() {
		return configs;
	}
}
