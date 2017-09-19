package de.felixwolf.workerSelection.selection;

import de.felixwolf.workerSelection.dataTypes.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;


public class Reducor {
	
	public ArrayList<Worker> reduceGroupTask(int taskID, ArrayList<Worker> selectionGroup){
		ArrayList<Worker> newGroup = new ArrayList<Worker>(); 
		
		// create kickList
		ArrayList<Integer> kickList = new ArrayList<Integer>();
		for (Worker w : selectionGroup) {
			if (!w.getPossibleTasks().contains(taskID)) {
				// cannot do the task
				kickList.add(selectionGroup.indexOf(w));
			}
		}
		// apply kickList
		newGroup = kicking(kickList, selectionGroup);
		return newGroup;
		
	}
	
	public ArrayList<Worker> reduceGroupDate(Event e, ArrayList<Worker> selectionGroup){
		/*
		 * Remove all workers who have excluded the event e from the list
		 */
		ArrayList<Worker> newGroup = new ArrayList<Worker>(); 
		
		// create kickList
		ArrayList<Integer> kickList = new ArrayList<Integer>();
		for (Worker w : selectionGroup) {
			//System.out.println(e.getDate() + ", " +  e.getId() + ": " + w.getName());
			// get data from worker w
			DatesCollection excludedDates = w.getExcludedDates();
			ArrayList<Integer> excludedWholeDates = excludedDates.getWholeDates();
			HashMap<Integer, Integer> excludedSpecificEvents = excludedDates.getSpecificEvents();
			ArrayList<Integer> excludedEvents = w.getExcludedEvents();
			//System.out.println(excludedEvents);
			// data from event: event.date = date
			int eventId = e.getId();
			int overWrittenID = e.getOverWrittenID();
			Date eDate = e.getDate();
			int eDateDayOfYear = calcDayOfYear(eDate);
			
			// worker does not work at those events
			//if (excludedEvents.contains(eventId) || excludedEvents.contains(overWrittenID)) {
			if (excludedEvents.contains(eventId)) {
				//System.out.println("EventKick");
				kickList.add(selectionGroup.indexOf(w));
				//System.out.println("kicked (event): " + w.getName());
			}
			
			// worker does not work at that date
			if (excludedWholeDates.contains(eDateDayOfYear)) {
				kickList.add(selectionGroup.indexOf(w));
				//System.out.println("kicked (date): " + w.getName());
			}
			
			// worker does not work at this specific event
			if (excludedSpecificEvents.containsKey(eDateDayOfYear)) {
				int excludedEventID = excludedSpecificEvents.get(eDateDayOfYear);
				if (excludedEventID == eventId) {
					kickList.add(selectionGroup.indexOf(w));
					//System.out.println("kicked (sDate): " + w.getName());
				}
			}

		}
		
		// apply kickList
		newGroup = kicking(kickList, selectionGroup);
		return newGroup;
		
	}
	
	public ArrayList<Worker> reduceGroupWorksWithout(Date eDate, int [] workerList, ArrayList<Worker> selectionGroup, int cooldown){
		ArrayList<Worker> newGroup = new ArrayList<Worker>(); 
		
		//System.out.println("Current workerList: " + Arrays.toString(workerList));
		
		// create kickList
		ArrayList<Integer> kickList = new ArrayList<Integer>();
		for (Worker w : selectionGroup) {
			if (w.getWorksWithout() > 0) {
				//System.out.println("potential blocking: " + w.getId() + " does not work with: " + w.getWorksWithout());
				// System.out.println("block " + w.getWorksWithout());
				// System.out.println(Arrays.toString(workerList));
				for (int w2 : workerList) {
					// System.out.println(w2);
					if (w2 == w.getWorksWithout()) {
						// w2 is already working, so w is kicked out
						//System.out.println("kick: " + w.getId());
						kickList.add(selectionGroup.indexOf(w));
						
						Calendar c1 = Calendar.getInstance();
						c1.setTime(eDate);
						c1.add(Calendar.DATE, -(cooldown - 4));
						Date backDated = c1.getTime();
						
						w.setLastDate(backDated);
						break;
					}
					
				}
			}

		}
		
		// apply kickList
		newGroup = kicking(kickList, selectionGroup);
		return newGroup;
	}

	
	public ArrayList<Worker> reduceGroupCoolDown(Date eDate, int coolDownTime, ArrayList<Worker> selectionGroup){
		ArrayList<Worker> newGroup = new ArrayList<Worker>(); 
		
		// create kickList
		ArrayList<Integer> kickList = new ArrayList<Integer>();
		for (Worker w : selectionGroup) {
			// was active in the last x days
			Date lastActive = w.getLastDate();
			
			//System.out.println(w.getId() + " " + lastActive);
			if (lastActive != null) {
				long diff = getDateDiff(lastActive, eDate, TimeUnit.DAYS);
				if (diff < coolDownTime) {
					kickList.add(selectionGroup.indexOf(w));
					/*
					System.out.println("kicked: " + w.getId());
					System.out.println("eDate: " + eDate);
					System.out.println("lastActive: " + lastActive);
					*/
				}
			}
		}
		
		// apply kickList
		newGroup = kicking(kickList, selectionGroup);
		return newGroup;
	}
	
	
	
	
	
	
	
	
	
	
	
	private ArrayList<Worker> kicking(ArrayList<Integer> kickList, ArrayList<Worker> oldGroup){
		
		ArrayList<Worker> newGroup = new ArrayList<Worker>(); 
		
		// create kickList
		for (int wi = 0; wi < oldGroup.size(); wi++) {
			if (!kickList.contains(wi)) {
				newGroup.add(oldGroup.get(wi));
			}
		}
		return newGroup;
	}
	
	private int calcDayOfYear(Date date){
		int dateDdayOfYear = 0;
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		dateDdayOfYear = (cal.get(Calendar.DAY_OF_YEAR));
		return dateDdayOfYear;
	}
	
	private long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
		// date1 is before date2
		long diffInMillies = date2.getTime() - date1.getTime();
		return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
	}

	public ArrayList<Worker> reduceAlreadyDidSameTask(int[] workerList, int taskID, ArrayList<Integer> jobList,
			ArrayList<Worker> selectionGroup) {
		
		ArrayList<Integer> kickList = new ArrayList<Integer>();
		for (Worker w : selectionGroup) {
			for (int w2 = 0; w2 < workerList.length; w2++) {
				int workerID = w.getId();
				if (workerList[w2] == workerID) {
					// the worker is already active on that day
					if (jobList.get(w2) == taskID) {
						// doing the same task
						kickList.add(selectionGroup.indexOf(w));
					}
				}
			}
		}
		ArrayList<Worker> newGroup = kicking(kickList, selectionGroup);
		return newGroup;

	}
	
	/*
	 * Phase 2 finished
	 */

	/*
	 * // remove workers who are in the coolDownPeriode
	 * ArrayList<Integer> kickList2 = new ArrayList<Integer>();
	 * for(main.java.de.felixwolf.workerSelection.dataTypes.Worker w:selectionGroup2){
	 * 
	 * // was active in the last x days Date lastActive =
	 * w.getLastDate();
	 * 
	 * if(lastActive != null){ long diff = getDateDiff(lastActive,
	 * date, TimeUnit.DAYS); if(diff > coolDownTime){
	 * kickList2.add(selectionGroup1.indexOf(w)); } } }
	 * 
	 * // kicking ArrayList<main.java.de.felixwolf.workerSelection.dataTypes.Worker> selectionGroup3 = new
	 * ArrayList<main.java.de.felixwolf.workerSelection.dataTypes.Worker>(); for(int wi = 0; wi <
	 * selectionGroup2.size(); wi++){ if(!kickList2.contains(wi)){
	 * selectionGroup3.add(selectionGroup2.get(wi)); } }
	 */

	/*
	 * Phase 3 finished => ranking
	 */
	
}
