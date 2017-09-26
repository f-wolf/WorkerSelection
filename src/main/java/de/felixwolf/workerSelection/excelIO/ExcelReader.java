package de.felixwolf.workerSelection.excelIO;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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

import de.felixwolf.workerSelection.dataTypes.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The excel reader is used to read all input information from the input file.
 */
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

    private TimeZone gmtZone = TimeZone.getTimeZone("CEST");
    private DateFormat justDateformat = new SimpleDateFormat("dd.MM.yyyy");

    private int biggestCounter = 0;
	
	private Date rangeStart = null;
	private Date rangeEnd = null;

	public ExcelReader(String path) {
		this.path = path;

		try {

			if(isXLSX(path)){
				workbook = new XSSFWorkbook(new File(path));
			}
			else {
				InputStream inp = new FileInputStream(path);
				workbook = WorkbookFactory.create(inp);
			}

		} catch (Exception e) {
			LOGGER.error("Error reading the input file. Please make sure it is a valid excel file.");
			e.printStackTrace();
		}

		sheet_tasks = workbook.getSheet("Tasks");
		sheet_period = workbook.getSheet("Period");
		sheet_regEvents = workbook.getSheet("RegularEvents");
		sheet_specEvents = workbook.getSheet("SpecialEvents");
		sheet_workers = workbook.getSheet("Workers");
		sheet_settings = workbook.getSheet("Settings");

        justDateformat.setTimeZone(gmtZone);
	}

    /**
     * Method to determine the type of the input file
     * @param path
     * @return
     */
	private boolean isXLSX(String path){

		String [] splitPath = path.split("\\.");

		String fileExtension = splitPath[splitPath.length-1];
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

    /**
     * This method reads all tasks from the tasks sheet of the input file and returns them as an ArrayList.
     * The tasks are read until the first line without an ID
     * @return
     */
	public ArrayList<Task> readTasks() {
		// puts all tasks into an ArrayList
		ArrayList<Task> allTasks = new ArrayList<Task>();

		int rowNum = 1;
		boolean tasksLeftToRead = true;

		do{
			Row taskRow = sheet_tasks.getRow(rowNum);
			if(taskRow == null){
				// empty row
				break;
			}

			// read task id
			int column = 0;
			Cell idCell = taskRow.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			int taskId = -1;

			try {
				taskId = readCellOfId(idCell);
			} catch (NullPointerException e){
				// The cell appears to be empty -> all tasks are read in.
				break;
			} catch (ParseException e) {
				// The cell content could not be parsed -> program will exit
				LOGGER.error("The task ID in line " + String.valueOf(rowNum + 1) + " could not be parsed. Please correct the input");
				e.printStackTrace();
				System.exit(65);
			}

			Task newTask = new Task();
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

		LOGGER.info("All tasks are read in. The last task is from line " + rowNum);

		return allTasks;
	}

    /**
     * Method to read all event related data. The method call several subroutines for the different types of event
     * related data. The combined data is returned as ArrayList
     * @return
     */
	public ArrayList<Event> readEvents() {
		// puts all Events into an ArrayList

		// 1) get the exclusion data
		DatesCollection exclusionData = readExclusionData();

		// 2) get date range
		readDateRange();

		// 3) derive regular events
		ArrayList<Event> regularEvents = readRegularEvents(exclusionData);

		// 4) add special events
		ArrayList<Event> specialEvents = readSpecialEvents();

		// 5) merge events: combine both list and overwrite regular event if special event at same time
		ArrayList<Event> allEvents = mergeEvents(regularEvents, specialEvents);

		// sort the events
		Collections.sort(allEvents, new Comparator<Event>() {
			public int compare(Event e1, Event e2) {
				return e1.getDate().compareTo(e2.getDate());
			}
		});

		return allEvents;
	}

    /**
     * Method to read the exclusion data. The exclusion data is used to cancel regular events. If a potential event is
     * in the exclusion data either as date or event, it will not be created.
     * @return
     */
	private DatesCollection readExclusionData(){

		DatesCollection excludedDates = new DatesCollection();

		int rowNum = 9;
		boolean dataLeftToRead = true;

		do{
			Row row = sheet_period.getRow(rowNum);
			if(row == null){
				// No more data is expected
				break;
			}

			// read event date
			int column = 1;
			Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			if(cell == null){
				// No more data is expected
				break;
			}

			DatesCollection datesOfRow = null;
			try {
				datesOfRow = readCellOfDates(cell, "Exclusion data, row " + String.valueOf(rowNum + 1));
			} catch (ParseException e) {
				LOGGER.error("An exclusion date was erroneous. It can be found in line " + String.valueOf(rowNum + 1));
				LOGGER.info("The program will be terminated. Please correct the error,");
				LOGGER.error(e.getMessage());
				e.printStackTrace();
				System.exit(65);
			}
			excludedDates.addDateCollection(datesOfRow);

			rowNum++;
		}while (dataLeftToRead);

		LOGGER.info("All exclusion dates are read in. The last exclusion date is from line " + rowNum);

		return excludedDates;
	}

    /**
     * Reads the date range for all events. The date range is used for the creation of the regular events. Special events
     * outside of the date range are not considered.
     */
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

		LOGGER.info("The date range is read in.");
		// get the range
	}

    /**
     * Reads the input for the regular events. Regular events are repeated weekly
     * @param excludedDates
     * @return
     */
	private ArrayList<Event> readRegularEvents(DatesCollection excludedDates) {

		ArrayList<Event> regularEvents = new ArrayList<>();

		int rowNum = 1;
		boolean eventsLeftToBeRead = true;

		do{
			Row eventRow = sheet_regEvents.getRow(rowNum);

			if(eventRow == null){
				// empty row -> no more input is expected
				break;
			}

			// get id
			Cell idCell = eventRow.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			int regEventID = -1;
			try {
				regEventID = readCellOfId(idCell);
			} catch (NullPointerException e){
				// The cell appears to be empty -> all regular events are read in.
				break;
			} catch (ParseException e) {
				// The cell content could not be parsed -> program will exit
				LOGGER.error("The regular event ID in line " + String.valueOf(rowNum + 1) + " could not be parsed. Please correct the input");
				e.printStackTrace();
				System.exit(65);
			}

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
				// exclusion date or specific event exclusion?
				if (excludedDates.containsDate(runner) || excludedDates.containsEvent(runner, regEventID)) {
					c.add(Calendar.DATE, 7);
					runner = c.getTime();
					continue;
				}

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

				Calendar cFinal = Calendar.getInstance(TimeZone.getTimeZone("CEST"));
				cFinal.setTimeZone(tz);
				cFinal.setTime(runner);
				cFinal.set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY));
				cFinal.set(Calendar.MINUTE, t.get(Calendar.MINUTE));
				Date theDate = cFinal.getTime();
				event0.setDate(theDate);

				// set regular event comment
				Cell regEventCommentCell = eventRow.getCell(6, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                event0.setComment(readCellOfComment(regEventCommentCell));

				// extract tasks for event
				Cell cellOfRegEventTasks = eventRow.getCell(4);
				ArrayList<Integer> eventTasks = null;
				try {
					eventTasks = readCellOfIntegers(cellOfRegEventTasks);
				} catch (ParseException e) {
					LOGGER.error("The list of tasks for the regular event in line " + String.valueOf(rowNum + 1) + " could not be read");
					e.printStackTrace();
					System.exit(65);
				}
				event0.setEventTasks(eventTasks);

				// extract counters for event
				Cell cellOfIntegers = eventRow.getCell(5);
				ArrayList<Integer> eventCounters = null;
				try {
					eventCounters = readCellOfIntegers(cellOfIntegers);
				} catch (ParseException e) {
					LOGGER.error("The list of counters for the regular event in line " + String.valueOf(rowNum + 1) + " could not be read");
					e.printStackTrace();
					System.exit(65);
				}

				if(eventCounters.get(eventCounters.size() - 1) > biggestCounter){
					biggestCounter = eventCounters.get(eventCounters.size() - 1);
				}

				event0.setActiveCounters(eventCounters);

				regularEvents.add(event0);

				// add seven days to get the next date of the regular event
				c.add(Calendar.DATE, 7);
				runner = c.getTime();

			} while (runner.before(rangeEnd));

			rowNum++;

		} while (eventsLeftToBeRead);

		LOGGER.info("All regular events are read in. The last regular event is from line " + rowNum);
		return regularEvents;
	}

    /**
     * Reads the input for special events. Special events occur only one.
     * @return
     */
	private ArrayList<Event>  readSpecialEvents() {

		ArrayList<Event> specialEvents = new ArrayList<>();

		int rowNum = 1;
		boolean eventsLeftToBeRead = true;

		do {
			Row eventRow = sheet_specEvents.getRow(rowNum);

			if (eventRow == null) {
				// empty row -> no more special events are expected
				break;
			}

			// get id
			Cell idCell = eventRow.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			int specEventID = -1;
			try {
				specEventID = readCellOfId(idCell);
			} catch (NullPointerException e){
				// The cell appears to be empty -> all special events are read in.
				break;
			} catch (ParseException e) {
				// The cell content could not be parsed -> program will exit
				LOGGER.error("The special event ID in line " + String.valueOf(rowNum + 1) + " could not be parsed. Please correct the input");
				e.printStackTrace();
				System.exit(65);
			}

			Event event1 = new Event();
			// set ID and name
			event1.setId(specEventID);
			String eventName = eventRow.getCell(1).getStringCellValue();
			event1.setName(eventName);

			// get date and time
			Calendar cFinal2 = Calendar.getInstance(TimeZone.getTimeZone("CEST"));
			Date date = eventRow.getCell(2).getDateCellValue();

			String locationInfo = "special event " + eventName + " in line " + String.valueOf(rowNum + 1);
			if(!isDateInRange(date, locationInfo)){
				continue;
			}

			// get start time in hours and minutes
			Date time2 = eventRow.getCell(3).getDateCellValue();
			Calendar t2 = Calendar.getInstance(TimeZone.getTimeZone("GMT")); // Timezone
			// ...
			// is
			// deletable
			t2.setTime(time2);
			cFinal2.setTime(date);
			cFinal2.add(Calendar.HOUR, 3); // simple fix for java calendar bug. See: https://stackoverflow.com/questions/25239362/why-gregoriancalendar-changes-day-when-setting-hour-of-day-to-0-in-utc
			cFinal2.set(Calendar.HOUR_OF_DAY, t2.get(11));
			cFinal2.set(Calendar.MINUTE, t2.get(12));
			cFinal2.set(Calendar.SECOND, t2.get(13));
			Date theDate2 = cFinal2.getTime();
			event1.setDate(theDate2);

			// set special event comment
            Cell specEventCommentCell = eventRow.getCell(6, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            event1.setComment(readCellOfComment(specEventCommentCell));

			// extract tasks for event
			Cell cellOfTasks = eventRow.getCell(4);
			ArrayList<Integer> eventTasks = null;
			try {
				eventTasks = readCellOfIntegers(cellOfTasks);
			} catch (ParseException e) {
				LOGGER.error("The list of tasks for the special event in line " + String.valueOf(rowNum + 1) + " could not be read");
				e.printStackTrace();
				System.exit(65);
			}
			event1.setEventTasks(eventTasks);

			// extract counters for event
			Cell cellOfIntegers = eventRow.getCell(5);
			ArrayList<Integer> eventCounters = null;
			try {
				eventCounters = readCellOfIntegers(cellOfIntegers);
			} catch (ParseException e) {
				LOGGER.error("The list of counters for the special event in line " + String.valueOf(rowNum + 1) + " could not be read");
				e.printStackTrace();
				System.exit(65);
			}

			if(eventCounters.get(eventCounters.size() - 1) > biggestCounter){
				biggestCounter = eventCounters.get(eventCounters.size() - 1);
			}

			// sort the counters
			Collections.sort(eventCounters);
			event1.setActiveCounters(eventCounters);

			specialEvents.add(event1);
			rowNum++;
		} while (eventsLeftToBeRead);

		LOGGER.info("All special events are read in. The last special event is from line " + rowNum);

		return specialEvents;
	}

	/**
	 * Method to merge the two event lists. Regular events are overwritten by special events if they have the same date & time.
	 * @param regularEvents
	 * @param specialEvents
	 * @return
	 */
	private ArrayList<Event> mergeEvents(ArrayList<Event> regularEvents, ArrayList<Event> specialEvents){

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

	/**
	 * Method to read the the id of a given cell.
	 * @param idCell 						The cell which is to be read
	 * @return 								The id as integer
	 * @throws ParseException				Thrown if content cannot be parsed
	 * @throws NullPointerException			Thrown if cell appears to be empty
	 */
	private int readCellOfId(Cell idCell) throws ParseException, NullPointerException {

		int id = 0;

		if(idCell == null){
			throw new NullPointerException();
		}
		if(!idCell.getCellTypeEnum().equals(CellType.NUMERIC)){
			String cellContent;
			try{
				cellContent = idCell.getStringCellValue();
			} catch (Exception e){
				throw new ParseException("The cell does not contain a numeric value.", 0);
			}

			if(cellContent.matches("\\s+")){
				// the cell content consists of only of spaces -> can be considered as empty
				throw new NullPointerException();
			}
			cellContent = cellContent.replaceAll("\\s+", "");
			try {
				id = Integer.parseInt(cellContent);
			} catch (NumberFormatException e){
				LOGGER.error("'" + cellContent + "' could not be parsed. ");
				e.printStackTrace();
				throw new ParseException("The cell does not contain a numeric value.", 0);
			}
			return id;
		}

		try {
			id = (int) idCell.getNumericCellValue();
		} catch (Exception e){
			LOGGER.debug("id parse exception");
			e.printStackTrace();
			throw new ParseException("The cell does not contain a numeric value.", 0);
		}
		return id;
	}

    /**
     * Method to read the comment cells. Returns an empty string for empty cells
     * This methods avoids errors which could occur if the program tried to read an empty cell
     * @param commentCell
     * @return
     */
	private String readCellOfComment(Cell commentCell){

        String comment = " ";

        if(commentCell != null){
            try{
                comment = commentCell.getStringCellValue();
            } catch (Exception e){
                // do nothing, comment stays empty
            }
        }
        return comment;
    }

    /**
     * Method to read cells which can contain more than one date. All dates are returned as one datesCollection
     * @param cell
     * @param location
     * @return
     * @throws ParseException
     */
	private DatesCollection readCellOfDates(Cell cell, String location)  throws ParseException{

		if(cell == null){
			return null;
		}

		DatesCollection datesOfCell = new DatesCollection();

		if (cell.getCellTypeEnum() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)){
			// The cell contains a single date

			Date singgleDate = cell.getDateCellValue();
			isDateInRange(singgleDate, location);
			datesOfCell.addDate(singgleDate);
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
						tempDate = parseSingleDateString(range[0], location);
						end = parseSingleDateString(range[1], location);
					} catch (ParseException e){
						throw new ParseException("One input of the range was not a date: " + stringDate, i + 1);
					}
					isDateInRange(tempDate, location);
					isDateInRange(end, location);
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
						date = parseSingleDateString(eventInfo[0], location);
					} catch (ParseException e){
						throw new ParseException("The date of the following event could not be parsed: " + stringDate, i + 1);
					}
					isDateInRange(date, location);

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
						date = parseSingleDateString(stringDate, location);
					} catch (ParseException e) {
						throw new ParseException("The erroneous input was '" + stringDate + "'.", i + 1);
					}
					isDateInRange(date, location);
					datesOfCell.addDate(date);
				}
			}
		}
		return datesOfCell;
	}

	/** Method to parse a single date given as String.
	 *
	 * Dateformat.parse ignores all characters after the date is parsed. This can lead to undesired effects if the user
	 * makes an input error. E.g. typing a comma instead of semicolon: "12.12.2018, 14.12.2018" would be parsed as only
	 * the 12.12.2018 and the second date would get lost.
	 *
	 * @param dateString String of date formatted as dd.mm.yyyy
	 * @return The date
	 * @throws ParseException
	 */
	private Date parseSingleDateString(String dateString, String location) throws ParseException {

		if(!dateString.matches("\\d{2}\\.\\d{2}\\.\\d{4}")){
			LOGGER.warn("The date input '" + dateString + "' does not match the default date format of 'dd.mm.yyyy'! " +
					"Error possible, See: " + location);
		}

		return justDateformat.parse(dateString);
	}

    /**
     * Tests whether the given date is in the date range and warns the user. The warning is useful to identify typos in
     * dates which are nonetheless correct dates (e.g. wrong year)
     * @param date
     * @param location
     * @return
     */
	private boolean isDateInRange(Date date, String location){

		if (rangeStart != null && rangeEnd != null && (date.before(rangeStart) || date.after(rangeEnd))){
			LOGGER.warn(date + " is outside of the defined range. It can be found at " + location);
			return false;
		}
		return true;
	}

    /**
     * Method to read a cell of several integer values. The values are returned as ArrayList
     * @param cell
     * @return
     * @throws ParseException
     */
	private ArrayList<Integer> readCellOfIntegers(Cell cell) throws ParseException {

		ArrayList<Integer> intsOfCell = new ArrayList<>();

		if(cell.getCellTypeEnum().equals(CellType.NUMERIC)){
			// there is only a single integer value in the cell
			int singleInt = (int) cell.getNumericCellValue();
			intsOfCell.add(singleInt);
			return intsOfCell;
		}

		String cellStr;
		try {
			cellStr = cell.getStringCellValue();
		} catch (Exception e){
			e.printStackTrace();
			throw new ParseException("The cell content could not be read as String", 0);
		}

		cellStr = cellStr.replaceAll("\\s+","");
		cellStr = cellStr.replaceAll("–", "-");
		String[] split = cellStr.split(";");

		for(String str:split){

			if(str.contains("-")){
				String[] range = str.split("-");

				if(range.length != 2){
					throw new ParseException("The range " + str + "' does not contain two values.", 0);
				}

				int rangeStart;
				int rangeEnd;

				try {
					rangeStart = Integer.parseInt(range[0]);
					rangeEnd = Integer.parseInt(range[1]);
				} catch (NumberFormatException e){
					throw new ParseException("At least one value of the range '" + str + "' could not be parsed.", 0);
				}

				if(rangeEnd < rangeStart){
					throw new ParseException("The end date of the range '" + str + "' is before the start date.", 0);
				}

				for(int i = rangeStart; i <= rangeEnd; i++){
					intsOfCell.add(i);
				}
			}

			else {

				int singleInt;
				try {
					singleInt = Integer.parseInt(str);
				} catch (NumberFormatException e){
					throw new ParseException(str + " could not be parsed.", 0);
				}
				intsOfCell.add(singleInt);
			}
		}
		Collections.sort(intsOfCell);
		return intsOfCell;
	}

    /**
     * Method to read all workers. The data is saved in worker objects and returned in an ArrayList
     * @param biggestCounter
     * @return
     */
	public ArrayList<Worker> readWorkers(int biggestCounter) {
		// read the information for the workers and put them into the arrayList
		ArrayList<Worker> allWorkers = new ArrayList<Worker>();

		int rowNum = 1;
		boolean workersLeftToBeRead = true;

		do {
			Row workerRow = sheet_workers.getRow(rowNum);
			if (workerRow == null) {
				// empty row -> no more workers are expected
				break;
			}

			// get id
			Cell workerIDcell = workerRow.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			int workerID = -1;
			try {
				workerID = readCellOfId(workerIDcell);
			} catch (NullPointerException e){
				// The cell appears to be empty -> all workers are read in.
				break;
			} catch (ParseException e) {
				// The cell content could not be parsed -> program will exit
				LOGGER.error("The worker ID in line " + String.valueOf(rowNum + 1) + " could not be parsed. Please correct the input");
				e.printStackTrace();
				System.exit(65);
			}

			Worker worker1 = new Worker();
			worker1.setId(workerID);
			String workerName = workerRow.getCell(1).getStringCellValue();
			worker1.setName(workerName);
			//System.out.println("worker: " + worker1.getName());

			// extract tasks for worker
			Cell tasksCell = workerRow.getCell(2);
			ArrayList<Integer> possibleTasks = null;
			try {
				possibleTasks = readCellOfIntegers(tasksCell);
			} catch (ParseException e) {
				LOGGER.error("The list of possible task for " + workerName + " in line " + String.valueOf(rowNum + 1) + " could not be read");
				e.printStackTrace();
				System.exit(65);
			}
			worker1.setPossibleTasks(possibleTasks);

			// prefered events
			Cell preferedEventsCell = workerRow.getCell(3);
			ArrayList<Integer> preferredEvents = null;
			try {
				preferredEvents = readCellOfIntegers(preferedEventsCell);
			} catch (ParseException e) {
				LOGGER.error("The list of preferred events for " + workerName + " in line " + String.valueOf(rowNum + 1) + " could not be read");
				e.printStackTrace();
				System.exit(65);
			} catch (NullPointerException e){
				preferredEvents = new ArrayList<Integer>();
			}
			worker1.setPreferedEvents(preferredEvents);

			// excluded events
			Cell excludedEventsCell = workerRow.getCell(4);
			ArrayList<Integer> excludedEvents = null;
			try {
				excludedEvents = readCellOfIntegers(excludedEventsCell);
			} catch (ParseException e) {
				LOGGER.error("The list of excluded events for " + workerName + " in line " + String.valueOf(rowNum + 1) + " could not be read");
				e.printStackTrace();
				System.exit(65);
			} catch (NullPointerException e){
				excludedEvents = new ArrayList<Integer>();
			}
			worker1.setExcludedEvents(excludedEvents);

			// prefered dates
			Cell preferedDatesCell = workerRow.getCell(5, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			DatesCollection preferedDates = null;
			String locationInfo = "preferred dates of " + workerName;
			try {
				preferedDates = readCellOfDates(preferedDatesCell, locationInfo);
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
			locationInfo = "excluded dates of " + workerName;
			try {
				excludedDates = readCellOfDates(excludedDatesCell, locationInfo);
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

		LOGGER.info("All workers are read in. The last worker is from line " + rowNum);
		return allWorkers;
	}

    /**
     * Reads the cool down time of the settings sheet. The cool down time is the minimum time length in days of inactivity
     * after the last assignment.
     * @return
     */
	public int readCoolDown() {
		return (int) sheet_settings.getRow(1).getCell(1).getNumericCellValue();
	}

	public int getBiggestCounter(){
		return biggestCounter;
	}
}
