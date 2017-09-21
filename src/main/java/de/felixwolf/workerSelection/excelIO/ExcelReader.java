package de.felixwolf.workerSelection.excelIO;

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
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import de.felixwolf.workerSelection.dataTypes.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelReader {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExcelReader.class);

	String path;
	Workbook workbook;

	private Sheet sheet_tasks;
	private Sheet sheet_period;
	private Sheet sheet_regEvents;
	private Sheet sheet_specEvents;
	private Sheet sheet_workers;
	private Sheet sheet_settings;

	Sheet sheet_events;
	DateFormat format;
	DateFormat justDateformat = new SimpleDateFormat("dd.MM.yyyy");

	//WorkbookSettings newSetting; //todo ?
	private int biggestCounter;
	
	private Date rangeStart = null;
	private Date rangeEnd = null;

	public ExcelReader(String path) {
		this.path = path;

		try {

			if(isXLSX(path)){
				workbook = new XSSFWorkbook(new File(path));
			}
			else {
				workbook = new HSSFWorkbook(POIFSFileSystem.create(new File(path)));
			}

			//Workbook.getWorkbook(new File(path));

			sheet_tasks = workbook.getSheet("Tasks");
			sheet_period = workbook.getSheet("Period");
			sheet_regEvents = workbook.getSheet("RegularEvents");
			sheet_specEvents = workbook.getSheet("SpecialEvents");
			sheet_workers = workbook.getSheet("Workers");
			sheet_settings = workbook.getSheet("Settings");



			sheet_events = workbook.getSheetAt(1);
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		TimeZone gmtZone = TimeZone.getTimeZone("CEST");
		format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
		format.setTimeZone(gmtZone);
		justDateformat.setTimeZone(gmtZone);

		/*
		newSetting = new WorkbookSettings();
		Locale l = new Locale("de");
		newSetting.setLocale(l);
		*/

		biggestCounter = 0;
	}

	private boolean isXLSX(String path){

		String [] splittedPath = path.split("\\.");

		String fileExtension = splittedPath[splittedPath.length-1];
		fileExtension = fileExtension.toLowerCase();

		switch (fileExtension){
			case "xlsx": return true;
			case "xls": return false;
			default: LOGGER.error("Wrong input file type: " + fileExtension);
			LOGGER.error("Please check the input file. The program will terminate now");
			System.exit(65);
		}
		return false;
	}


	public ArrayList<Task> readTasks() {
		// puts all main.java.de.felixwolf.workerSelection.dataTypes.Task into an ArrayList
		ArrayList<Task> allTasks = new ArrayList<Task>();

		int rowNum = 1;
		boolean tasksLeftToRead = true;

		do{
			Row taskRow = sheet_tasks.getRow(rowNum);
			if(taskRow == null){
				// empty row
				tasksLeftToRead = false;
				break;
			}

			Task newTask = new Task();

			// read task id
			int column = 0;
			Cell idCell = taskRow.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			if(idCell == null){
				LOGGER.warn("Empty id cell in non empty task row " + rowNum + 1 + ". No task is created for this row."); // TODO: +1 functional?
				continue;
			}
			int taskId;
			try {
				taskId = (int) idCell.getNumericCellValue();
			} catch (Exception e){
				LOGGER.error("Task ID in row " + taskRow +1 + " could not be read. No task is created for this row.");
				continue;
			}
			newTask.setId(taskId);

			// read task name
			column++;
			Cell nameCell = taskRow.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			if(nameCell == null){
				LOGGER.warn("No name was given for the task with the ID " + taskId + ". The ID is used as name.");
				newTask.setName(String.valueOf(taskId));
			}
			else {
				newTask.setName(nameCell.getStringCellValue());
			}

			allTasks.add(newTask);
			rowNum++;

		}while (tasksLeftToRead);

		return allTasks;
	}




	public ArrayList<Event> readEvents() {
		// puts all Events into an ArrayList

		// TimeZone gmtZone = TimeZone.getTimeZone("GMT");
		// SimpleDateFormat format = new SimpleDateFormat("dd.mm.yyyy hh:mm");
		// format.setTimeZone(gmtZone);

		// 1) get the exclusion data
		DatesCollection exclusionData = readExclusionData();
		LOGGER.debug("exclusion data was obtained");


		// 2) get date range
		readDateRange();
		LOGGER.debug("date range was obtained");

		// 3) derive regular events
		ArrayList<Event> regularEvents = readRegularEvents(exclusionData);
		LOGGER.debug("Regular events were read");

		// 4) add special events
		ArrayList<Event> specialEvents = readSpecialEvents();
		LOGGER.debug("Special events were read");


		// 5) merge events: combine both list and overwrite regular event if special event at same time
		ArrayList<Event> allEvents = mergeEvents(regularEvents, specialEvents);
		LOGGER.debug("Events were merged");

		// sort the events
		Collections.sort(allEvents, new Comparator<Event>() {
			public int compare(Event e1, Event e2) {
				return e1.getDate().compareTo(e2.getDate());
			}
		});

		return allEvents;
	}



	private DatesCollection readExclusionData(){

		DatesCollection excludedDates = new DatesCollection();

		int rowNum = 9;
		boolean dataLeftToRead = true;

		do{
			Row row = sheet_period.getRow(rowNum);
			if(row == null){
				// empty row
				break;
			}

			// read event date
			int column = 1;
			Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			if(cell == null){
				LOGGER.warn("Empty date cell in non empty excluded date row " + rowNum + 1 + ". No excluded date is created for this row."); // TODO: +1 functional?
				rowNum++;
				continue;
			}

			DatesCollection datesOfRow = null;
			try {
				datesOfRow = readCellOfDates(cell);
			} catch (ParseException e) {
				LOGGER.error("An exclusion date was erroneous. It can be found in line " + rowNum + 1); // TODO printed correctly?
				LOGGER.error(e.getMessage());
				e.printStackTrace();
			}
			excludedDates.addDateCollection(datesOfRow);

			rowNum++;

		}while (dataLeftToRead);

		return excludedDates;
	}

	private void readDateRange() {

		Row startRow = sheet_period.getRow(2);
		try {
			rangeStart = startRow.getCell(1).getDateCellValue();
		} catch (Exception e){
			LOGGER.error("The entered start date could not be parsed. Please correct it");
			System.exit(65);
		}

		Row endRow = sheet_period.getRow(3);
		try {
			rangeEnd = endRow.getCell(1).getDateCellValue();
		} catch (Exception e){
			LOGGER.error("The entered end date could not be parsed. Please correct it");
			System.exit(65);
		}

		// get the range
		//System.out.println(rangeStart);
		//System.out.println(rangeEnd);
		//System.out.println("Time diff: " + getDateDiff(rangeStart, rangeEnd, TimeUnit.DAYS));
	}


	private ArrayList<Event> readRegularEvents(DatesCollection excludedDates) {

		ArrayList<Event> regularEvents = new ArrayList<>();

		int rowNum = 1;
		boolean eventsLeftToBeRead = true;

		do{
			Row eventRow = sheet_regEvents.getRow(rowNum);

			if(eventRow == null){
				// empty row
				eventsLeftToBeRead = false;
				break;
			}

			// get id
			Cell idCell = eventRow.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			if(idCell == null){
				// TODO message
				LOGGER.warn("The regular event in line " + rowNum + " has no ID.");
				break;
			}
			int regEventID = (int) idCell.getNumericCellValue();

			//LOGGER.debug("current eventID " + regEventID);

			String weekday = eventRow.getCell(2).getStringCellValue(); //sheet_regEvents.getCell(4 + i, 11).getContents();

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

			if (runner.after(rangeEnd)) {
				System.err.println("ER, readEvents: Range smaller than one week or writing error, write day in English");
				break;
			}

			do {
				//System.out.println("loop0: " + runner);
				// exclusion date or specific event exclusion?
				if (excludedDates.containsDate(runner) || excludedDates.containsEvent(runner, regEventID)) {
					c.add(Calendar.DATE, 7);
					runner = c.getTime();
					continue;
				}

				// System.out.println("runner: " + runner);
				// create event and add to allEvents
				Event event0 = new Event();
				event0.setName(eventRow.getCell(1).getStringCellValue()); //sheet_regEvents.getCell(4 + i, 9).getContents());
				event0.setId(regEventID);

				// get date and time
				// get start time in hours and minutes
				TimeZone tz = TimeZone.getTimeZone("CEST");
				Date time = eventRow.getCell(3).getDateCellValue(); //((DateCell) sheet_regEvents.getCell(4 + i, 12)).getDate();
				Calendar t = Calendar.getInstance(TimeZone.getTimeZone("CEST"));
				t.setTimeZone(tz);
				t.setTime(time);

				// System.out.println("time: " + time );

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

				event0.setComment(eventRow.getCell(6).getStringCellValue()); //sheet_regEvents.getCell(4 + i, 13).getContents());

				// extract tasks for event
				String tasksString = eventRow.getCell(4).getStringCellValue(); // sheet_regEvents.getCell(4 + i, 14).getContents();
				ArrayList<String> tasksStringList = breakUpaString(tasksString, ";"); // TODO
				ArrayList<Integer> eventTasks = new ArrayList<Integer>();

				for (String s : tasksStringList) {
					// System.out.println(s);
					eventTasks.add(Integer.parseInt(s));
				}
				// sort the tasks
				Collections.sort(eventTasks);
				event0.setEventTasks(eventTasks);

				// extract counters for event
				Cell cellOfIntegers = eventRow.getCell(5);
				ArrayList<Integer> eventCounters = readCellOfIntegers(cellOfIntegers);

				// sort the counters
				Collections.sort(eventCounters);

				if(eventCounters.get(eventCounters.size() - 1) > biggestCounter){
					biggestCounter = eventCounters.get(eventCounters.size() - 1);
				}

				event0.setActiveCounters(eventCounters);

				regularEvents.add(event0);

				// add seven days
				c.add(Calendar.DATE, 7);
				runner = c.getTime();

			} while (runner.before(rangeEnd));

			rowNum++;

		} while (eventsLeftToBeRead);

		return regularEvents;
	}




	private ArrayList<Event>  readSpecialEvents() {

		ArrayList<Event> specialEvents = new ArrayList<>();

		int rowNum = 1;
		boolean eventsLeftToBeRead = true;

		do {
			Row eventRow = sheet_specEvents.getRow(rowNum);

			if (eventRow == null) {
				// empty row
				eventsLeftToBeRead = false;
				break;
			}

			// get id
			Cell idCell = eventRow.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			if (idCell == null) {
				LOGGER.warn("The special event in line " + rowNum + " has no ID.");
				break;
			}

			Event event1 = new Event();

			// set ID and name
			int specEventID = (int) idCell.getNumericCellValue();
			event1.setId(specEventID);
			String eventName = eventRow.getCell(1).getStringCellValue();
			event1.setName(eventName);

			// get date and time
			Calendar cFinal2 = Calendar.getInstance(TimeZone.getTimeZone("CEST"));
			Date date = eventRow.getCell(2).getDateCellValue();

			if (rangeStart != null && rangeEnd != null && (date.before(rangeStart) || date.after(rangeEnd))) {
				LOGGER.info("Event " + eventName + " with the id " + specEventID + " is outside of the defined range. It is ignored.");
				continue;
			}


			// get start time in hours and minutes
			Date time2 = eventRow.getCell(3).getDateCellValue();
			Calendar t2 = Calendar.getInstance(TimeZone.getTimeZone("GMT")); // Timezone
			// ...
			// is
			// deletable
			t2.setTime(time2);
			// System.out.println("time2: " + time2 );

			// System.out.println("date2: " + date2);
			cFinal2.setTime(date);
			cFinal2.add(Calendar.HOUR, 3); // simple fix for java calendar bug. See: https://stackoverflow.com/questions/25239362/why-gregoriancalendar-changes-day-when-setting-hour-of-day-to-0-in-utc

			cFinal2.set(Calendar.HOUR_OF_DAY, t2.get(11));
			cFinal2.set(Calendar.MINUTE, t2.get(12));
			cFinal2.set(Calendar.SECOND, t2.get(13));

			// System.out.println("t2: " +t2.getTime() + " " + t2.get(12));
			// System.out.println("cf2: " + cFinal2.getTime());
			Date theDate2 = cFinal2.getTime();
			// System.out.println(format.format(theDate2));

			event1.setDate(theDate2);

			event1.setComment(eventRow.getCell(6).getStringCellValue());
			// extract tasks for event
			Cell cellOfTasks = eventRow.getCell(4);
			ArrayList<Integer> eventTasks = readCellOfIntegers(cellOfTasks);

			// sort the tasks and add them to the event
			Collections.sort(eventTasks);
			event1.setEventTasks(eventTasks);

			// extract counters for event // TODO check
			Cell cellOfIntegers = eventRow.getCell(5);
			ArrayList<Integer> eventCounters = readCellOfIntegers(cellOfIntegers);

			// sort the counters
			Collections.sort(eventCounters);

			if(eventCounters.get(eventCounters.size() - 1) > biggestCounter){
				biggestCounter = eventCounters.get(eventCounters.size() - 1);
			}

			// sort the counters
			Collections.sort(eventCounters);
			event1.setActiveCounters(eventCounters);

			specialEvents.add(event1);
			rowNum++;
		} while (eventsLeftToBeRead);

		return specialEvents;
	}

	/**
	 * Method to merge the two event lists. Regular events are overwritten by special events if they have the same date & time.
	 * @param regularEvents
	 * @param specialEvents
	 * @return
	 */
	ArrayList<Event> mergeEvents(ArrayList<Event> regularEvents, ArrayList<Event> specialEvents){

		HashMap<String, Event> eventsMap = new HashMap<>();

		DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");

		// first place the regular events in the Hashmap
		for(Event regEvent:regularEvents){
			String key = dateFormat.format(regEvent.getDate());
			eventsMap.put(key, regEvent);
		}

		// the special events are placed in hashmap
		for(Event specEvent:specialEvents){
			String key = dateFormat.format(specEvent.getDate());
			if(eventsMap.containsKey(key)){
				// if there is already an event at the time, then retrieve its id
				Event regEventToBeOverwritten = eventsMap.get(key);
				int idToBeOverwritten = regEventToBeOverwritten.getId();
				specEvent.setOverWrittenID(idToBeOverwritten);
			}
			eventsMap.put(key, specEvent);
		}

		ArrayList<Event> allEvents = new ArrayList<>(eventsMap.values());
		return allEvents;
	}


	private DatesCollection readCellOfDates(Cell cell)  throws ParseException{

		if(cell == null){
			return null;
		}

		DatesCollection datesOfCell = new DatesCollection();

		if (cell.getCellTypeEnum() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)){
			// The cell contains a single date
			datesOfCell.addDate(cell.getDateCellValue());
		}
		else {
			String stringOfDates = cell.getStringCellValue();

			if(stringOfDates.isEmpty()){
				return null;
			}

			// string preprocessing: remove all spaces and replace
			stringOfDates = stringOfDates.replaceAll("\\s+","");
			stringOfDates = stringOfDates.replaceAll("–", "-");
			String [] datesArray = stringOfDates.split(";");

			for(int i = 0; i < datesArray.length; i++){

				String stringDate = datesArray[i];

				if(stringDate.contains("-")){
					// this is a date range
					String [] range = stringDate.split("-");

					if(range.length != 2){
						throw new ParseException("Wrong range input: " + stringDate, 0);
					}

					Date tempDate;
					Date end;

					try {
						tempDate = justDateformat.parse(range[0]);
						end = justDateformat.parse(range[1]);
					} catch (ParseException e){
						throw new ParseException("One input of the range was not a date: " + stringDate, i + 1);
					}

					while (tempDate.before(end)) {

						datesOfCell.addDate(tempDate);

						Calendar cal = Calendar.getInstance();
						cal.setTime(tempDate);
						cal.add(Calendar.DATE, 1); // minus number would
						tempDate = cal.getTime();
					}
					datesOfCell.addDate(end);
				}

				else if(stringDate.contains("|")){
					// this is a specific event

					String [] eventInfo = stringDate.split("\\|");

					if(eventInfo.length != 2){
						throw new ParseException("Wrong specific event input: " + stringDate, 0);
					}

					Date date;
					try {
						date = justDateformat.parse(eventInfo[0]);
					} catch (ParseException e){
						throw new ParseException("The date of the following event could not be parsed: " + stringDate, i + 1);
					}

					int eventID;
					try {
						eventID = Integer.parseInt(eventInfo[1]);
					} catch (NumberFormatException e){
						throw new ParseException("The eventID could not be parsed: " + stringDate, i + 1);
					}

					datesOfCell.addEvent(date, eventID);

				}

				else {
					// this is an ordinary date
					Date date;
					try {
						date = justDateformat.parse(stringDate);
					} catch (ParseException e) {
						throw new ParseException("The erroneous input was '" + stringDate + "'.", i + 1);
					}
					datesOfCell.addDate(date);
				}
			}
		}
		return datesOfCell;
	}


	private ArrayList<Integer> readCellOfIntegers(Cell cell){

		// TODO error handling

		/*
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
		 */
		ArrayList<Integer> intsOfCell = new ArrayList<>();

		if(cell.getCellTypeEnum().equals(CellType.NUMERIC)){
			int singleInt = (int) cell.getNumericCellValue();
			intsOfCell.add(singleInt);
			return intsOfCell;
		}

		String cellStr = cell.getStringCellValue();
		cellStr = cellStr.replaceAll("\\s+","");
		cellStr = cellStr.replaceAll("–", "-");
		String[] splitted = cellStr.split(";");

		for(String str:splitted){

			if(str.contains("-")){
				String[] range = str.split("-");
				int rangeStart = Integer.parseInt(range[0]);
				int rangeEnd = Integer.parseInt(range[1]);

				for(int i = rangeStart; i <= rangeEnd; i++){
					intsOfCell.add(i);
				}
			}

			else {
				intsOfCell.add(Integer.parseInt(str));
			}
		}
		return intsOfCell;
	}






	public ArrayList<Worker> readWorkers(int biggestCounter) {
		// read the information for the workers and put them into the arrayList
		ArrayList<Worker> allWorkers = new ArrayList<Worker>();

		int rowNum = 1;
		boolean workersLeftToBeRead = true;

		do {
			Row workerRow = sheet_workers.getRow(rowNum);

			if (workerRow == null) {
				// empty row
				workersLeftToBeRead = false;
				break;
			}

			Worker worker1 = new Worker();

			Cell workerIDcell = workerRow.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			if(workerIDcell == null){
				LOGGER.warn("Empty worker id cell in non empty row " + rowNum + 1 + ". Skipping row");
				rowNum++;
				break;
			}

			int workerID = (int) workerIDcell.getNumericCellValue();
			worker1.setId(workerID);
			String workerName = workerRow.getCell(1).getStringCellValue();
			worker1.setName(workerName);
			//System.out.println("worker: " + worker1.getName());

			// extract tasks for worker
			Cell tasksCell = workerRow.getCell(2);
			ArrayList<Integer> possibleTasks = readCellOfIntegers(tasksCell);
			worker1.setPossibleTasks(possibleTasks);

			// prefered events
			Cell preferedEventsCell = workerRow.getCell(3);
			ArrayList<Integer> preferedEvents = readCellOfIntegers(preferedEventsCell);
			worker1.setPreferedEvents(preferedEvents);

			// excluded events
			Cell excludedEventsCell = workerRow.getCell(4);
			ArrayList<Integer> excludedEvents = readCellOfIntegers(excludedEventsCell);
			worker1.setExcludedEvents(excludedEvents);

			// prefered dates
			Cell preferedDatesCell = workerRow.getCell(5, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			DatesCollection preferedDates = null;
			try {
				preferedDates = readCellOfDates(preferedDatesCell);
			} catch (ParseException e) {
				LOGGER.error("A prefered date of " + workerName + " could not be read.");
				LOGGER.error(e.getMessage());
				e.printStackTrace();
				System.exit(65);
			}
			worker1.setPreferedDates(preferedDates);

			// excluded dates
			Cell excludedDatesCell = workerRow.getCell(6);
			DatesCollection excludedDates = null;
			try {
				excludedDates = readCellOfDates(excludedDatesCell);
			} catch (ParseException e) {
				LOGGER.error("An excluded date of " + workerName + " could not be read.");
				LOGGER.error(e.getMessage());
				e.printStackTrace();
				System.exit(65);
			}
			worker1.setExcludedDates(excludedDates);

			// works with and without
			Cell worksWithCell = workerRow.getCell(7, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			if(worksWithCell != null){
				worker1.setWorksWith((int) worksWithCell.getNumericCellValue());
			}

			Cell worksWithoutCell = workerRow.getCell(8, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			if(worksWithoutCell != null){
				worker1.setWorksWithout((int) worksWithoutCell.getNumericCellValue());
			}

			// create counter
			int [] counter = new int [biggestCounter + 1];
			Arrays.fill(counter, 0);

			// read counter
			Cell counterCell = workerRow.getCell(9, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

			if(counterCell != null){
				counter[0] = (int) counterCell.getNumericCellValue();
				worker1.setCounter(counter);
				//System.out.println("*** " + Integer.parseInt(counterString));
			}

			else{
				worker1.setCounter(counter);
			}

			// read last date
			Date lastDate = workerRow.getCell(10).getDateCellValue();
			worker1.setLastDate(lastDate);

			allWorkers.add(worker1);



			rowNum++;
		} while (workersLeftToBeRead);

		return allWorkers;
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
		return (int) sheet_settings.getRow(1).getCell(1).getNumericCellValue();
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
