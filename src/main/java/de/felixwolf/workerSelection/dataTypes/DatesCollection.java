package de.felixwolf.workerSelection.dataTypes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


public class DatesCollection {

	public ArrayList<String> getWholeDates() {
		return wholeDates;
	}

	public ArrayList<String> getSpecificEvents() {
		return specificEvents;
	}

	private ArrayList<String> wholeDates = new ArrayList<>();
	private ArrayList<String> specificEvents = new ArrayList<>();

	DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

	public void addDate(Date date){
		wholeDates.add(getDateID(date));
	}

	public void addEvent(Date date, int eventID){
		specificEvents.add(getEventID(date, eventID));
	}

	public boolean containsDate(Date date){
		return wholeDates.contains(getDateID(date));
	}

	public boolean containsEvent(Date date, int eventID){
		String stringDate = getDateID(date);
		String stringEvent = getEventID(date, eventID);

		return (wholeDates.contains(stringDate) || specificEvents.contains(stringEvent));
	}




	private String getDateID(Date date){
		return dateFormat.format(date);
	}

	private String getEventID(Date date, int eventID){
		return dateFormat.format(date) + "_" + String.valueOf(eventID);
	}


	public void addDateCollection(DatesCollection differentCollection) {
		wholeDates.addAll(differentCollection.getWholeDates());
		specificEvents.addAll(differentCollection.getSpecificEvents());
	}

	public int smallestDiffToFutureDate(Date eDate) {

		// TODO

		return 100;

	}
}
