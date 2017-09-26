package de.felixwolf.workerSelection.dataTypes;

/**
 * Class for the task objects
 */
public class Task {
	
	private String name;
	private int id;
	private int maxPerDay;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getMaxPerDay() {
		return maxPerDay;
	}

	public void setMaxPerDay(int maxPerDay) {
		this.maxPerDay = maxPerDay;
	}
	
}
