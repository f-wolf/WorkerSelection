package de.felixwolf.workerSelection.selection;

import de.felixwolf.workerSelection.dataTypes.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


public class Analysor {

	
	public int calcWeekOfYear(Event e){
		
		Date firstDate = e.getDate();
		Calendar firstCal = Calendar.getInstance();
		firstCal.setTime(firstDate);
		firstCal.setFirstDayOfWeek(Calendar.FRIDAY);
		int weekOfYear = firstCal.get(Calendar.WEEK_OF_YEAR);
		return weekOfYear;
	}
	
	public ArrayList<Integer> createJobList(ArrayList<Event> allEvents, ArrayList<Task> allTasks, int columnsWithText){
		
		ArrayList<Integer> jobList = new ArrayList<Integer>();
		
		for (int i = 0; i < columnsWithText; i++) {
			jobList.add(-1);
		}
		
		// find the maximum of each task
		for (Task t0 : allTasks) {
			int maxCount = 0;
			int id = t0.getId();
			for (Event e0 : allEvents) {
				ArrayList<Integer> tasksForE0 = e0.getEventTasks();
				// System.out.println(tasksForE0);
				int firstIndex = tasksForE0.indexOf(id);
				int lastIndex = tasksForE0.lastIndexOf(id);
				if (lastIndex >= 0) {
					lastIndex++;
				}
				// System.out.println(firstIndex + " " + lastIndex);
				// System.out.println(" ");
				int diff = lastIndex - firstIndex;
				if (diff > maxCount) {
					maxCount = diff;
				}
			}
			t0.setMaxPerDay(maxCount);
			for (int i = 0; i < maxCount; i++) {
				jobList.add(id);
			}
			// System.out.println(id + ": " + maxCount);
		}
		return jobList;
	}
	
	public String [] createLegend(ArrayList<Integer> jobList, ArrayList<Task> allTasks, int columnsWithText){
		
		String[] legend = new String[jobList.size()];
		legend[0] = Settings.getLegendDay();
		legend[1] = Settings.getLegendDate();
		legend[2] = Settings.getLegendComment();
		legend[3] = Settings.getLegendTime();
		for (int i = columnsWithText; i < jobList.size(); i++) {
			for (Task t : allTasks) {
				if (jobList.get(i) == t.getId()) {
					legend[i] = t.getName();
				}
			}
		}
		return legend;
	}
	
	public String[] creatEventInfo(Event e, int size){
		String[] output = new String[size];
		Date eDate = e.getDate();
		
		// write day
		DateFormat dayFormat = new SimpleDateFormat("EEEE");
		String dayName = dayFormat.format(eDate);
		output[0] = dayName.substring(0, 2);

		// write date
		DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
		output[1] = dateFormat.format(eDate);

		// write comment
		output[2] = e.getComment();

		// write time
		DateFormat timeFormat = new SimpleDateFormat("HH:mm");
		TimeZone timeZone = Settings.getTimeZone();
		timeFormat.setTimeZone(timeZone);
		output[3] = timeFormat.format(eDate);
		output[4] = "";
		
		return output;
	}
}
