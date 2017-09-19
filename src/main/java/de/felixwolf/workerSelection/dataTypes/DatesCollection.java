package de.felixwolf.workerSelection.dataTypes;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


public class DatesCollection {
	
	private ArrayList<Integer> wholeDates = new ArrayList<Integer>();
	HashMap<Integer, Integer> specificEvents = new HashMap<Integer, Integer>();
	
	
	
	public ArrayList<Integer> getWholeDates() {
		return wholeDates;
	}
	public void setWholeDates(ArrayList<Integer> wholeDates) {
		this.wholeDates = wholeDates;
	}
	public HashMap<Integer, Integer> getSpecificEvents() {
		return specificEvents;
	}
	public void setSpecificEvents(HashMap<Integer, Integer> specificEvents) {
		this.specificEvents = specificEvents;
	} 
	
	
	
	
	
}
