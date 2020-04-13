package io.lava.ion.moods;

public class MoodConfigFromJSON {
	private String name;
	private int id;
	private int min, max, defaultVal, current;
	private String widget;
	private String label;

	public MoodConfigFromJSON(String name, int id, int min, int max, int defaultVal, String widget, String label) {
		this.name = name;
		this.id = id;
		this.min = min;
		this.max = max;
		this.defaultVal = defaultVal;
		this.widget = widget;
		this.label = label;
		current = defaultVal;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	public int getDefault() {
		return defaultVal;
	}

	public String getWidget() {
		return widget;
	}

	public String getLabel() {
		return label;
	}

	public int getCurrent() {
		return current;
	}
	
	public void setCurrent(int current) {
		this.current = current;
	}
}
