package de.felixwolf.workerSelection.dataTypes;

import java.util.ArrayList;
import java.util.Date;

/**
 * The event class is used to collect all information about the events.
 */


public class Event {
	
	private String name;
	private int id;
	// if a regular event is replaced by a special event, the overwrittenID of the special event is the ID of the replaced regular event
	private int overWrittenID = -1;
	private Date date;
	private String comment;
	private ArrayList<Integer> eventTasks = new ArrayList<Integer>();
	private ArrayList<Integer> activeCounters = new ArrayList<Integer>();
	
	// getters and setters

	public ArrayList<Integer> getCounters() {
		return activeCounters;
	}

	public void setActiveCounters(ArrayList<Integer> activeCounters) {
		this.activeCounters = activeCounters;
	}

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

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public ArrayList<Integer> getEventTasks() {
		return eventTasks;
	}

	public void setEventTasks(ArrayList<Integer> eventTasks) {
		this.eventTasks = eventTasks;
	}

	public int getOverWrittenID() {
		return overWrittenID;
	}

	public void setOverWrittenID(int overWrittenID) {
		this.overWrittenID = overWrittenID;
	}
}
