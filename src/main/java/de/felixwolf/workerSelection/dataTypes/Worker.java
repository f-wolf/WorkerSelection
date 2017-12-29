package de.felixwolf.workerSelection.dataTypes;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Class for the worker objects
 */
public class Worker {
	
	private String name;
	private int id;
	private ArrayList<Integer> possibleTasks = new ArrayList<Integer>();
	private ArrayList<Integer> excludedEvents = new ArrayList<Integer>();
	private ArrayList<Integer> preferedEvents = new ArrayList<Integer>();
	private DatesCollection excludedDates = new DatesCollection();
	private DatesCollection preferedDates = new DatesCollection();
	private int worksWith = 0;
	private ArrayList<Integer> worksWithout = new ArrayList<Integer>();
	private int [] counter;
	private HashMap<Date, Integer> allActivities = new HashMap<Date, Integer>();
	private Date lastDate = null;


	/**
	 * The worker was selected to do a task. This method is used to add the specific task to the activity list.
	 * This is done to keep track of the amount of assignments and to always know the last assignment date.
	 * @param date
	 * @param e
	 */
	public void addNewActivity(Date date, Event e){
		allActivities.put(date, e.getId());
		lastDate = date;
		
		ArrayList<Integer> counters = e.getCounters();
		
		for(int c:counters){
			counter[c]++;
		}
	}
	
	public Date getLastDate(){
		return lastDate;
	}
	
	public void setLastDate(Date lastDate) {
		this.lastDate = lastDate;
	}
	
	// getters and setters
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

	public ArrayList<Integer> getPossibleTasks() {
		return possibleTasks;
	}

	public void setPossibleTasks(ArrayList<Integer> possibleTasks) {
		this.possibleTasks = possibleTasks;
	}

	public ArrayList<Integer> getExcludedEvents() {
		return excludedEvents;
	}

	public void setExcludedEvents(ArrayList<Integer> excludedEvents) {
		this.excludedEvents = excludedEvents;
	}

	public ArrayList<Integer> getPreferedEvents() {
		return preferedEvents;
	}

	public void setPreferedEvents(ArrayList<Integer> preferedEvents) {
		this.preferedEvents = preferedEvents;
	}

	public DatesCollection getExcludedDates() {
		return excludedDates;
	}

	public void setExcludedDates(DatesCollection excludedDates) {
		this.excludedDates = excludedDates;
	}

	public DatesCollection getPreferedDates() {
		return preferedDates;
	}

	public void setPreferedDates(DatesCollection preferedDates) {
		this.preferedDates = preferedDates;
	}

	public int getWorksWith() {
		return worksWith;
	}

	public void setWorksWith(int worksWith) {
		this.worksWith = worksWith;
	}

	public ArrayList<Integer> getWorksWithout() {
		return worksWithout;
	}

	public void setWorksWithout(ArrayList<Integer> worksWithout) {
		this.worksWithout = worksWithout;
	}

	public int[] getCounter() {
		return counter;
	}

	public void setCounter(int[] counter) {
		this.counter = counter;
	}

	public HashMap<Date, Integer> getAllActivities() {
		return allActivities;
	}
}
