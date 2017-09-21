package de.felixwolf.workerSelection.selection;

import de.felixwolf.workerSelection.dataTypes.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;


public class PreferenceSelection {
	
	public int alreadyActiveWorker(int[] workerList, int taskID, ArrayList<Integer> jobList, ArrayList<Worker> selectionGroup){
		// returns id of already active worker for a second task
		int selectedWorker = -1;

		//boolean foundWorker = false;
		for (Worker w : selectionGroup) {
			for (int w2 = 0; w2 < workerList.length; w2++) {
				int workerID = w.getId();
				if (workerList[w2] == workerID) {
					// the worker is already active on that day
					if (jobList.get(w2) != taskID) {
						// doing a different task
						return workerID;
					}
				}
			}
		}
		return selectedWorker;
	}
	
	public int datePreference(Event e, ArrayList<Worker> selectionGroup){
		
		for(Worker w:selectionGroup){
			//System.out.println("worker: " + w.getId());
			int workerID = w.getId();
			
			if(preferesWholeDate(e, w)){
				//System.out.println(w.getId() + " prefers whole date");
				return workerID;
			}
			
			if(preferesSpecificEvent(e, w)){
				//System.out.println(w.getId() + " prefers specific date");
				return workerID;
			}
			 
		}
		return -1;
	}
	
	private boolean preferesWholeDate(Event e, Worker w){
		DatesCollection preferedDates = w.getPreferedDates();
		Date eDate = e.getDate();

		if(preferedDates != null && preferedDates.containsDate(eDate)){
			return true;
		}
		return false;
	}
		
	private boolean preferesSpecificEvent(Event e, Worker w) {
		DatesCollection preferedDates = w.getPreferedDates();
		Date eDate = e.getDate();
		int eID = e.getId();

		if(preferedDates != null && preferedDates.containsEvent(eDate, eID)){
			return true;
		}

		return false;
	}
		
		
		
		

		
		
	private int calcDayOfYear(Date date){
		int dateDdayOfYear = 0;
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		dateDdayOfYear = (cal.get(Calendar.DAY_OF_YEAR));
		return dateDdayOfYear;
	}		
		
}

/*
for (main.java.de.felixwolf.workerSelection.dataTypes.Worker w : selectionGroup3) {

	// Does the worker prefer the date?
	// get data from worker
	main.java.de.felixwolf.workerSelection.dataTypes.DatesCollection preferedDates = w.getPreferedDates();
	HashMap<Date, Integer> preferedSpecificEvents = preferedDates
			.getSpecificEvents();
	ArrayList<Date> preferedWholeDates = preferedDates
			.getWholeDates();
	// System.out.println(preferedWholeDates.size());
	// specific event?

	boolean wantsToWorkSpecific = false;
	Date key = new Date();
	for (Date prefDate : preferedSpecificEvents.keySet()) {
		if (getDateDiff(prefDate, eDate, TimeUnit.DAYS) == 0) {
			wantsToWorkSpecific = true;
			key = prefDate;
			break;
		}
	}

	if (wantsToWorkSpecific) {
		int eventId = e.getId();
		if (eventId == preferedSpecificEvents.get(key)) {
			// System.out.println(eventId);
			// System.out.println(w.getName() +
			// " wants to work at this event");
			// place on list
			for (int j = 0; j < jobList.size(); j++) {
				if (jobList.get(j) == taskID && control[j]) {
					//System.out.println("placement of someone who wants to work at this specific event");
					control[j] = false;
					output[j] = w.getName();
					workerList[j] = w.getId();
					foundWorker = true;
					w.addNewActivity(eDate, e.getId());
					break;
				}
			}
		}
	}
	if (foundWorker) {
		break;
	}

	// system for whole days

	boolean wantsToWorkWholeDay = false;
	for (Date prefDate : preferedWholeDates) {
		if (getDateDiff(prefDate, eDate, TimeUnit.DAYS) == 0) {
			wantsToWorkWholeDay = true;
			break;
		}
	}

	if (wantsToWorkWholeDay) {
		// System.out.println("wants to work today");
		// place on list
		for (int j = 0; j < jobList.size(); j++) {
			if (jobList.get(j) == taskID && control[j]) {
				//System.out.println("someone wants to work at that date");
				control[j] = false;
				output[j] = w.getName();
				workerList[j] = w.getId();
				foundWorker = true;
				w.addNewActivity(eDate, e.getId());
				break;
			}
		}
	}
	
	*/
