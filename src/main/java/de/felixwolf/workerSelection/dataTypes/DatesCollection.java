package de.felixwolf.workerSelection.dataTypes;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/** Class for the storage of collections of dates and events.
 *
 */

public class DatesCollection {

	private final String EVENTSEPERATOR = "_";
	private DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

	private ArrayList<String> wholeDates = new ArrayList<>();
	private ArrayList<String> specificEvents = new ArrayList<>();

	public ArrayList<String> getWholeDates() {
		return wholeDates;
	}

	public ArrayList<String> getSpecificEvents() {
		return specificEvents;
	}

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

	/**
	 * Method to combine two date collections into one
	 * @param differentCollection
	 */
	public void addDateCollection(DatesCollection differentCollection) {
		wholeDates.addAll(differentCollection.getWholeDates());
		specificEvents.addAll(differentCollection.getSpecificEvents());
	}

	/**
	 * Method which returns the smallest time difference in days from a given date "curDate" to all future dates in
	 * the collection. Returns -1 if no date in the collection is after the given date
	 * @param curDate
	 * @return
	 */
	public int smallestDiffToFutureDate(Date curDate) {

		// set hour and smaller time units to zero
		Calendar cal = Calendar.getInstance();
		cal.setTime(curDate);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		curDate = cal.getTime();

		int smallestDiffInit = 10000;
		int smallestDiff = smallestDiffInit;

		for(String wholeDateStr:wholeDates){
			Date potFutureDate = getDateFromStringDate(wholeDateStr);
			if(potFutureDate.after(curDate)){
				int timediff = (int) getDateDiff(curDate, potFutureDate, TimeUnit.DAYS);
				if (timediff < smallestDiff){
					smallestDiff = timediff;
				}
			}
		}

		for(String specEventStr:specificEvents){
			Date potFutureDate = getDateFromStringEvent(specEventStr);
			if(potFutureDate.after(curDate)){
				int timediff = (int) getDateDiff(curDate, potFutureDate, TimeUnit.DAYS);
				if (timediff < smallestDiff){
					smallestDiff = timediff;
				}
			}
		}

		if(smallestDiff == smallestDiffInit){
			// the dates collection does not contain a date in the near future
			return -1;
		}

		return smallestDiff;
	}

	/**
	 * Method to calculate the time difference between two dates
	 * @param date1
	 * @param date2
	 * @param timeUnit
	 * @return
	 */
	private long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
		long diffInMillies = date2.getTime() - date1.getTime();
		return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
	}

	private String getDateID(Date date){
		return dateFormat.format(date);
	}

	private Date getDateFromStringDate(String stringDate){
		try {
			return dateFormat.parse(stringDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Date getDateFromStringEvent(String stringEvent){

		String [] split = stringEvent.split(EVENTSEPERATOR);
		try {
			return dateFormat.parse(split[0]);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getEventID(Date date, int eventID){
		return dateFormat.format(date) + EVENTSEPERATOR + String.valueOf(eventID);
	}
}
