import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import jxl.*;
import jxl.read.biff.BiffException;

public class ExcelReader {

	String path;
	Workbook workbook;
	Sheet sheet_general;
	Sheet sheet_events;
	Sheet sheet_workers;
	DateFormat format;
	WorkbookSettings newSetting;
	int biggestCounter;
	
	private Date rangeStart = null;
	private Date rangeEnd = null;

	public ExcelReader(String path) {
		this.path = path;

		try {
			workbook = Workbook.getWorkbook(new File(path));
			sheet_general = workbook.getSheet(0);
			sheet_events = workbook.getSheet(1);
			sheet_workers = workbook.getSheet(2);
		} catch (BiffException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		TimeZone gmtZone = TimeZone.getTimeZone("GMT");
		format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
		format.setTimeZone(gmtZone);
		
		newSetting = new WorkbookSettings();
		Locale l = new Locale("de");
		newSetting.setLocale(l);
		
		biggestCounter = 0;

	}

	public ArrayList<Task> readTasks() {
		// puts all Task into an ArrayList
		ArrayList<Task> allTasks = new ArrayList<Task>();
		int countTasks = Integer.parseInt(sheet_general.getCell(1, 5).getContents());
		for (int i = 0; i < countTasks; i++) {

			Task task1 = new Task();
			task1.setName(sheet_general.getCell(4 + i, 4).getContents());
			task1.setId(Integer.parseInt(sheet_general.getCell(4 + i, 5).getContents()));
			allTasks.add(task1);

		}

		return allTasks;
	}

	public ArrayList<Event> readEvents() {
		// puts all Events into an ArrayList

		ArrayList<Event> allEvents = new ArrayList<Event>();
		ArrayList<Integer> exclusionDates = new ArrayList<Integer>();
		HashMap<Integer, Integer> exclusionMap = new HashMap<Integer, Integer>();

		// TimeZone gmtZone = TimeZone.getTimeZone("GMT");
		// SimpleDateFormat format = new SimpleDateFormat("dd.mm.yyyy hh:mm");
		// format.setTimeZone(gmtZone);

		// get the exclusion dates
		DatesCollection exclusionData = null;
		try {
			exclusionData = readDates(sheet_general,4, 18);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			System.err.println("Error reading the exclusion dates for the time range -> K1");
			e.printStackTrace();
		}
		exclusionDates = exclusionData.getWholeDates();
		//System.out.println(exclusionDates);
		exclusionMap = exclusionData.getSpecificEvents();
		
		// put regular events into arrayList (without the exclusion dates)
		// get the range
		rangeStart = ((DateCell) sheet_general.getCell(4, 0)).getDate();
		//System.out.println(rangeStart);
		rangeEnd = ((DateCell) sheet_general.getCell(4, 1)).getDate();
		//System.out.println(rangeEnd);
		//System.out.println("Time diff: " + getDateDiff(rangeStart, rangeEnd, TimeUnit.DAYS));

		// # of regular events during the week
		int countRegDays = Integer.parseInt(sheet_general.getCell(1, 10).getContents());
		
		// add the regular events
		for (int i = 0; i < countRegDays; i++) {
			String weekday = sheet_general.getCell(4 + i, 11).getContents();

			Calendar c = Calendar.getInstance();
			DateFormat format2 = new SimpleDateFormat("EEEE");

			Date runner = rangeStart;
			c.setTime(runner);
			c.add(Calendar.HOUR, 3);
			String currentWeekday = format2.format(runner);

			// find the first date of the current weekday
			int catchStuckinLoopCounter = 0;
			while (!(currentWeekday.toLowerCase()).equals(weekday.toLowerCase())) {
				c.add(Calendar.DATE, 1);
				runner = c.getTime();
				c.setTime(runner);
				currentWeekday = format2.format(runner);
				catchStuckinLoopCounter++;
				if(catchStuckinLoopCounter > 8){
					System.err.println("ExcelReader, ReadEvents: Stuck in while-loop. Language difference between system and input file?");
				}
			}
			
			// one day has to be added; don't know why
			//c.add(Calendar.DATE, 1);
			//runner = c.getTime();
			
			if (runner.after(rangeEnd)) {
				System.err.println("ER, readEvents: Range smaller than one week or writing error, write day in English");
				break;
			}
			//System.out.println("before loop: " + runner);
			
			do {
				//System.out.println("loop0: " + runner);
				int runnerDayOfYear = calcDayOfYear(runner); 
				// exclusion date?
				if (exclusionDates.contains(runnerDayOfYear)) {
					c.add(Calendar.DATE, 7);
					runner = c.getTime();
					continue;
				}

				// special exclusion?
				int id = Integer.parseInt(sheet_general.getCell(4 + i, 10).getContents());
				
				if (exclusionMap.containsKey(runnerDayOfYear)) {
					if (exclusionMap.get(runnerDayOfYear) == id) {
						c.add(Calendar.DATE, 7);
						runner = c.getTime();
						continue;
					}
				}

				// System.out.println("runner: " + runner);
				// create event and add to allEvents
				Event event0 = new Event();
				event0.setName(sheet_general.getCell(4 + i, 9).getContents());
				event0.setId(id);

				// get date and time
				// get start time in hours and minutes
				TimeZone tz = TimeZone.getTimeZone("CEST");
				Date time = ((DateCell) sheet_general.getCell(4 + i, 12)).getDate();
				Calendar t = Calendar.getInstance(TimeZone.getTimeZone("CEST"));
				t.setTimeZone(tz);
				t.setTime(time);
				
				// System.out.println("time: " + time );

				// System.out.println(time);
			    
				Calendar cFinal = Calendar.getInstance(TimeZone.getTimeZone("CEST"));
				cFinal.setTimeZone(tz);
				cFinal.setTime(runner);
				//System.out.println("cf, runner set: " + cFinal.getTime());
				//cFinal.set(Calendar.HOUR_OF_DAY, t.get(11));
				//cFinal.set(Calendar.MINUTE, t.get(12));
				//System.out.println(cFinal.get(Calendar.HOUR_OF_DAY));
				//System.out.println(t.get(Calendar.HOUR_OF_DAY));
				cFinal.set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY));
				//System.out.println("cf, hour set: " + cFinal.getTime());
				cFinal.set(Calendar.MINUTE, t.get(Calendar.MINUTE));
				//System.out.println("cf, time set: " + cFinal.getTime());
				Date theDate = cFinal.getTime();
				//System.out.println(format.format(theDate));
				event0.setDate(theDate);

				event0.setComment(sheet_general.getCell(4 + i, 13).getContents());

				// extract tasks for event
				String tasksString = sheet_general.getCell(4 + i, 14).getContents();
				ArrayList<String> tasksStringList = breakUpaString(tasksString, ";");
				ArrayList<Integer> eventTasks = new ArrayList<Integer>();

				for (String s : tasksStringList) {
					// System.out.println(s);
					eventTasks.add(Integer.parseInt(s));
				}
				// sort the tasks
				Collections.sort(eventTasks);
				event0.setEventTasks(eventTasks);
				
				// extract counters for event
				String countersString = sheet_general.getCell(4 + i, 15).getContents();
				ArrayList<String> countersStringList = breakUpaString(countersString, ";");
				ArrayList<Integer> eventCounters = new ArrayList<Integer>();

				for (String s : countersStringList) {
					// System.out.println(s);
					int counter = Integer.parseInt(s);
					eventCounters.add(counter);
					if(counter > biggestCounter){
						biggestCounter = counter;
					}
				}
				// sort the counters
				Collections.sort(eventCounters);
				event0.setActiveCounters(eventCounters);
				
				allEvents.add(event0);

				// add seven days
				c.add(Calendar.DATE, 7);
				runner = c.getTime();

			} while (runner.before(rangeEnd));

		}

		// add the special events to the arrayList
		int countSpecialEvents = Integer.parseInt(sheet_general.getCell(1, 23).getContents());
		for (int i = 0; i < countSpecialEvents; i++) {

			Event event1 = new Event();
			event1.setName(sheet_events.getCell(1, 1 + i).getContents());
			event1.setId(Integer.parseInt(sheet_events.getCell(2, 1 + i).getContents()));
			System.out.println("event: " + event1.getName());

			// get date and time
			// get start time in hours and minutes
			Date time2 = ((DateCell) sheet_events.getCell(4, 1 + i)).getDate();
			Calendar t2 = Calendar.getInstance(TimeZone.getTimeZone("CEST")); // Timezone
																				// ...
																				// is
																				// deletable
			t2.setTime(time2);
			// System.out.println("time2: " + time2 );

			// System.out.println(time);
			Calendar cFinal2 = Calendar.getInstance(TimeZone.getTimeZone("CEST"));
			Date date2 = ((DateCell) sheet_events.getCell(3, 1 + i)).getDate();

			// System.out.println("date2: " + date2);
			cFinal2.setTime(date2);
			// int month = cal.get(Calendar.MONTH); better?
			cFinal2.set(Calendar.HOUR_OF_DAY, t2.get(11));
			cFinal2.set(Calendar.MINUTE, t2.get(12));
			cFinal2.set(Calendar.SECOND, t2.get(13));
			// System.out.println("t2: " +t2.getTime() + " " + t2.get(12));
			// System.out.println("cf2: " + cFinal2.getTime());
			Date theDate2 = cFinal2.getTime();
			// System.out.println(format.format(theDate2));
			event1.setDate(theDate2);

			event1.setComment(sheet_events.getCell(5, 1 + i).getContents());
			// extract tasks for event
			String tasksString = sheet_events.getCell(6, 1 + i).getContents();
			ArrayList<String> tasksStringList = breakUpaString(tasksString, ";");
			ArrayList<Integer> eventTasks = new ArrayList<Integer>();
			//System.out.println(event1.getId() + ": " + tasksString);
			for (String s : tasksStringList) {
				// System.out.println(s);
				eventTasks.add(Integer.parseInt(s));
			}

			// sort the tasks and add them to the event
			Collections.sort(eventTasks);
			event1.setEventTasks(eventTasks);
			
			// extract counters for event
			String countersString = sheet_events.getCell(7, 1 + i).getContents();
			ArrayList<String> countersStringList = breakUpaString(countersString, ";");
			ArrayList<Integer> eventCounters = new ArrayList<Integer>();
			
			for (String s : countersStringList) {
				// System.out.println(s);
				int counter = Integer.parseInt(s);
				eventCounters.add(counter);
				if(counter > biggestCounter){
					biggestCounter = counter;
				}
			}
			// sort the counters
			Collections.sort(eventCounters);
			event1.setActiveCounters(eventCounters);
			

			// check if a regular Event is at the same time
			int deleteIndex = -1;
			for (Event aEvent : allEvents) {
				if (aEvent.getDate().equals(theDate2)) {
					deleteIndex = allEvents.indexOf(aEvent);
					event1.setOverWrittenID(aEvent.getId());
					break;
				}
			}
			if (deleteIndex > -1) {
				allEvents.remove(deleteIndex);
				deleteIndex = -1;
			}

			allEvents.add(event1);
		}

		// sort the arrayList
		Collections.sort(allEvents, new Comparator<Event>() {
			public int compare(Event e1, Event e2) {
				return e1.getDate().compareTo(e2.getDate());
			}
		});

		return allEvents;
	}

	public ArrayList<Worker> readWorkers(int biggestCounter) {
		// read the information for the workers and put them into the arrayList
		ArrayList<Worker> allWorkers = new ArrayList<Worker>();
		
		int workersCount = Integer.parseInt(sheet_general.getCell(1, 31).getContents());
		for (int i = 0; i < workersCount; i++) {
			
			Worker worker1 = new Worker();
			worker1.setName(sheet_workers.getCell(1, 1 + i).getContents());
			worker1.setId(Integer.parseInt(sheet_workers.getCell(2, 1 + i).getContents()));
			System.out.println("worker: " + worker1.getName());
			
			// extract tasks for worker
			String tasksString = sheet_workers.getCell(3, 1 + i).getContents();
			ArrayList<String> tasksStringList = breakUpaString(tasksString, ";");
			ArrayList<Integer> possibleTasks = new ArrayList<Integer>();

			for (String s : tasksStringList) {
				// System.out.println(s);
				possibleTasks.add(Integer.parseInt(s));
			}

			worker1.setPossibleTasks(possibleTasks);
			
			// prefered Events
			String prefEventsString = sheet_workers.getCell(4, 1 + i).getContents();
			ArrayList<String> prefEventsStringList = breakUpaString(prefEventsString, ";");
			ArrayList<Integer> preferedEvents = new ArrayList<Integer>();
			
			for (String s : prefEventsStringList) {
				// System.out.println(s);
				if(s.isEmpty()){
					preferedEvents.add(-1);
				}
				else if(s.contains("-")){
					ArrayList<String> prefEventRange = breakUpaString(s, "-");
					if(prefEventRange.size() == 2) {
						int prefEventRangeStart = Integer.parseInt(prefEventRange.get(0));
						int prefEventRangeEnd = Integer.parseInt(prefEventRange.get(1));
						if(prefEventRangeStart > prefEventRangeEnd){
							System.err.println("Preferred ID range start is bigger than end -> this will result in an error");
						}
						while (prefEventRangeStart <= prefEventRangeEnd){
							preferedEvents.add(prefEventRangeStart);
							prefEventRangeStart++;
						}
					}
					else{
						System.err.println(worker1.getName() + ": Found a separator in preferred EventIds, but not 2 numbers");
					}
				}
				else{
					preferedEvents.add(Integer.parseInt(s));
				}
			}
			worker1.setPreferedEvents(preferedEvents);
			
			// excluded Events
			String excludedEventsString = sheet_workers.getCell(5, 1 + i ).getContents();
			ArrayList<String> excludedEventsStringList = breakUpaString(excludedEventsString, ";");
			ArrayList<Integer> excludedEvents = new ArrayList<Integer>();

			for (String s : excludedEventsStringList) {
				// System.out.println(s);
				if(s.isEmpty()){
					excludedEvents.add(-1);
				}
				else if(s.contains("-")){
					ArrayList<String> excluEventRange = breakUpaString(s, "-");
					if(excluEventRange.size() == 2) {
						int excluEventRangeStart = Integer.parseInt(excluEventRange.get(0));
						int excluEventRangeEnd = Integer.parseInt(excluEventRange.get(1));
						if(excluEventRangeStart > excluEventRangeEnd){
							System.err.println("Excluded ID range start is bigger than end -> this will result in an error");
						}
						while (excluEventRangeStart <= excluEventRangeEnd){
							excludedEvents.add(excluEventRangeStart);
							excluEventRangeStart++;
						}
					}
					else{
						System.err.println(worker1.getName() + ": Found a separator in excluded EventIds, but not 2 numbers");
					}
				}
				else{
					excludedEvents.add(Integer.parseInt(s));
				}
			}
			worker1.setExcludedEvents(excludedEvents);
			
			// preferred Dates
			DatesCollection preferedDates = null;
			try {
				preferedDates = readDates(sheet_workers, 6, 1 + i);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				System.err.println("Error reading the prefered dates for " + worker1.getName() + ", ID: " + worker1.getId());
				e.printStackTrace();
			}
			worker1.setPreferedDates(preferedDates);
			
			// excluded Dates
			DatesCollection excludedDates = null;
			try {
				excludedDates = readDates(sheet_workers, 7, 1 + i);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				System.err.println("Error reading the excluded dates for " + worker1.getName() + ", ID: " + worker1.getId());
				e.printStackTrace();
			}
			worker1.setExcludedDates(excludedDates);
			
			// works with and without
			String worksWith = sheet_workers.getCell(8,  1 + i).getContents();
			if(!worksWith.isEmpty()){
				worker1.setWorksWith(Integer.parseInt(worksWith));
			}
			String worksWithout = sheet_workers.getCell(9, 1 + i ).getContents();
			if(!worksWithout.isEmpty()){
				worker1.setWorksWithout(Integer.parseInt(worksWithout));
			}
			
			// create counter
			int [] counter = new int [biggestCounter + 1];
			Arrays.fill(counter, 0);
			
			// read counter
			String counterString = sheet_workers.getCell(10, 1 + i).getContents();
			if(!counterString.isEmpty()){
				counter[0] = Integer.parseInt(counterString);
				worker1.setCounter(counter);
				//System.out.println("*** " + Integer.parseInt(counterString));
			}
			else{
				worker1.setCounter(counter);
			}
			
			// read last date
			Date lastDate = ((DateCell) sheet_workers.getCell(11, 1 + i)).getDate();
			worker1.setLastDate(lastDate);
			
			allWorkers.add(worker1);
			
		}
		
	
		
		return allWorkers;
	}
	
	public DatesCollection readDates(Sheet sheet0, int col, int row) throws ParseException{
		// takes the coordinates of the input zero based
		// return the Dates of the Cell
		
		DatesCollection theDates = new DatesCollection();
		ArrayList<Integer> wholeDates = new ArrayList<Integer>();
		HashMap<Integer, Integer> specificDates = new HashMap<Integer, Integer>();
		
		String temp1 = sheet0.getCell(col, row).getContents();
		//System.out.println("ER, readDates: temp1: " + temp1);

		if (temp1.isEmpty()) {
			//System.out.println("There are no days to exclude");
		} else {
			boolean singleDate = false;
			ArrayList<String> stringDates = new ArrayList<String>();
			if (temp1.contains(";")) {
				// there are several dates
				stringDates = breakUpaString(temp1, ";");
			} else {
				// there is just one date
				stringDates.add(temp1);
				singleDate = true;
			}

			for (String stringDate : stringDates) {
				if (stringDate.contains("-")) {
					// a date range
					ArrayList<String> range = new ArrayList<String>();
					range = breakUpaString(stringDate, "-");

					Date start = extractDateFromString(range.get(0));
					Date end = extractDateFromString(range.get(1));
					Date date = start;

					while (date.before(end)) {
						//wholeDates.add(date);
						Calendar cal = Calendar.getInstance();
						cal.setTime(date);
						wholeDates.add(cal.get(Calendar.DAY_OF_YEAR));
						cal.add(Calendar.DATE, 1); // minus number would
													// decrement the days
						date = cal.getTime();
					}
					//wholeDates.add(end);
					Calendar cal = Calendar.getInstance();
					cal.setTime(end);
					wholeDates.add(cal.get(Calendar.DAY_OF_YEAR));
					//System.out.println("WholeDates0: " + wholeDates);

				} else if (!stringDate.contains("|")) {
					// a single day
					Date date = new Date();
					int dateDdayOfYear = 0;
					if(singleDate){
						date = ((DateCell) sheet0.getCell(col, row)).getDate();
						dateDdayOfYear = calcDayOfYear(date);
					}
					else{
						
						
						
						date  =  extractDateFromString(stringDate);
						dateDdayOfYear = calcDayOfYear(date);
				
					}
					//Date date = ((DateCell) sheet0.getCell(col, row)).getDate();
					//Date date  =  extractDateFromString(stringDate);
					if (!wholeDates.contains(dateDdayOfYear)) {
						wholeDates.add(dateDdayOfYear);
						//System.out.println("WholeDates1: " + wholeDates);
					} else {
						System.out.println("The date is already excluded");
					}

				} else {
					// String contains "|" => a specific event

					ArrayList<String> splits = new ArrayList<String>();
					splits = breakUpaString(stringDate, "|");

					Date date = extractDateFromString(splits.get(0));
					int dateDdayOfYear = calcDayOfYear(date);

					if (!wholeDates.contains(dateDdayOfYear)) {
						int id = Integer.parseInt(splits.get(1));
						specificDates.put(dateDdayOfYear, id);

					} else {
						System.out.println("The date is already excluded");
					}
				}
			}

		}// end else, string not empty
		//System.out.println("WholeDates: " + wholeDates);
		theDates.setWholeDates(wholeDates);
		theDates.setSpecificEvents(specificDates);
		
		return theDates;
	}
	
	

	private Date extractDateFromString(String s) throws ParseException {
		// returns a date from a String
		//System.out.println("coo coo :" + s);
		// clean the string
		String s2 = "";
		if (s.contains(" ")) {
			//System.out.println("Date extraction from string: string cleaning");
			s2 = s.replace(" ", "");
		} 
		else{
			s2 = s;
		}
		
		SimpleDateFormat format3 = new SimpleDateFormat("dd.MM.yyyy");
		// https://stackoverflow.com/questions/4216745/java-string-to-date-conversion
		Date date = new Date();
		date = format3.parse(s2);
		
		// test if the date is inside the time frame
		if (rangeStart != null && rangeEnd != null && (date.before(rangeStart) || date.after(rangeEnd))){
			System.err.println("WARNING: date outside of specified date range: " + s);
		}
		
		
		return date;
	}

	private ArrayList<String> breakUpaString(String unserperatedString, String seperator) {
		ArrayList<String> tempArrayList1 = new ArrayList<String>();
		ArrayList<String> tempArrayList2 = new ArrayList<String>();
		CharSequence cs = seperator;

		while (unserperatedString.contains(cs)) {
			tempArrayList1.add(unserperatedString.substring(0,
					unserperatedString.indexOf(seperator)));
			unserperatedString = unserperatedString
					.substring(unserperatedString.indexOf(seperator) + 1);
		}
		tempArrayList1.add(unserperatedString);

		// cleanup
		for (String str : tempArrayList1) {
			//System.out.println(str);
			String str2;
			/*
			if (str.contains(" ")) {
				str2 = str.replace(" ", "");
			} else {
				str2 = str;
			}
			*/
			
			str2 = str.replaceAll("\\s+","");
			//System.out.println(str2);
			tempArrayList2.add(str2);

		}

		return tempArrayList2;
	}

	public int readCoolDown() {
		String coolDownString = sheet_general.getCell(4, 20).getContents();
		int coolDown = Integer.parseInt(coolDownString);
		return coolDown;
	}
	
	public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
	    long diffInMillies = date2.getTime() - date1.getTime();
	    return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
	}

	private int calcDayOfYear(Date date){
		int dateDdayOfYear = 0;
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		dateDdayOfYear = (cal.get(Calendar.DAY_OF_YEAR));
		return dateDdayOfYear;
	}
	
	public int getBiggestCounter(){
		return biggestCounter;
	}
	
	
	
}
