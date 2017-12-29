package de.felixwolf.workerSelection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import de.felixwolf.workerSelection.excelIO.*;
import de.felixwolf.workerSelection.dataTypes.*;
import de.felixwolf.workerSelection.selection.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Run {

	private static final Logger LOGGER = LoggerFactory.getLogger(Run.class);

	static String inputPath = "input.xlsx";
	static int columnsWithText = 4;

	static ArrayList<Task> allTasks = new ArrayList<Task>();
	static ArrayList<Event> allEvents = new ArrayList<Event>();
	static ArrayList<Worker> allWorkers = new ArrayList<Worker>();
	static ArrayList<String[]> theOutput = new ArrayList<String[]>();
	static int coolDownTime;


	public static void main(String[] args) {

		Settings.init(inputPath);
		readInputFile();
		processData();
		writeOutputFile();
	}

	private static void readInputFile(){
		ExcelReader excelReader = new ExcelReader(inputPath);
		allTasks = excelReader.readTasks();
		allEvents = excelReader.readEvents();
		int biggestCounter = excelReader.getBiggestCounter(); // has to be after readEvents
		allWorkers = excelReader.readWorkers(biggestCounter);
		coolDownTime = Settings.getCoolDownTime();
		LOGGER.info("The excel file is read. Please make sure that all data was read in completely");
	}

	private static void processData(){
		Analysor ana = new Analysor();

		// create the jobList
		ArrayList<Integer> jobList = new ArrayList<Integer>(); // set positions
		jobList = ana.createJobList(allEvents, allTasks, columnsWithText);

		// create legend and add to output
		String [] legend = ana.createLegend(jobList, allTasks, columnsWithText);
		theOutput.add(legend);

		// calculate the week of year for the first event
		// -> creates an empty line after each week
		int lastWeek = ana.calcWeekOfYear(allEvents.get(0));

		// selection
		for (Event e : allEvents) {
			//System.out.println(e.getDate());
			// control arrays
			boolean[] control = new boolean[jobList.size()];
			String[] output = new String[jobList.size()];
			int[] workerList = new int[jobList.size()];
			Arrays.fill(control, true);
			for (int i = 0; i < columnsWithText; i++) {
				control[i] = false;
			}

			// event information
			Date eDate = e.getDate();
			int eID	= e.getId();

			// tools
			Reducor red0 = new Reducor();
			PreferenceSelection pSelect = new PreferenceSelection();

			// new line?
			int currentWeek = ana.calcWeekOfYear(e);
			if (currentWeek != lastWeek) {
				lastWeek = currentWeek;
				String[] emtpty = { "", "" };
				theOutput.add(emtpty);
			}

			// write the basic info to the output string
			output = ana.creatEventInfo(e, jobList.size());

			for (int taskID : e.getEventTasks()) {

				int worker = -1;
				// create pool of all workers and remove the unsuitable workers in the following steps
				ArrayList<Worker> selectionGroup0 = allWorkers;

				// remove workers from selection group who are not suitable (wrong task)
				ArrayList<Worker> selectionGroup1 = red0.reduceGroupTask(taskID, selectionGroup0);

				// remove workers from selection group who have no time
				ArrayList<Worker> selectionGroup2 = red0.reduceGroupDate(e, selectionGroup1);

				// remove workers who don't want to work with somebody who is already on the list
				ArrayList<Worker> selectionGroup3 = red0.reduceGroupWorksWithout(eDate, workerList, selectionGroup2, coolDownTime);

				// remove workers who are already on the list with the same task
				ArrayList<Worker> selectionGroup4 = red0.reduceAlreadyDidSameTask(workerList, taskID, jobList, selectionGroup3);

				// if somebody is already doing a job, give him another one if it is different
				worker = pSelect.alreadyActiveWorker(workerList, taskID, jobList, selectionGroup4);
				if(worker != -1){
					// write information
					workerList = addWorker2workerList(worker, taskID, workerList, jobList);
					continue;
				}

				// remove workers who have worked in the coolDownPhase
				ArrayList<Worker> selectionGroup5 = red0.reduceGroupCoolDown(eDate, coolDownTime, selectionGroup4);

				if(selectionGroup5.size() == 0){
					// nobody left
					workerList = addWorker2workerList(-2, taskID, workerList, jobList);
					continue;
				}

				// does a worker prefer the specific date or event
				worker = pSelect.datePreference(e, selectionGroup5);
				if(worker != -1){
					// write information
					workerList = addWorker2workerList(worker, taskID, workerList, jobList);
					continue;
				}

				// find next worker with ranking
				RankingSelection rSelect = new RankingSelection(e, selectionGroup5);
				rSelect.counterRanking();
				rSelect.eventRanking();
				rSelect.lastActiveRaking();
				rSelect.nextPreferedDateRanking(coolDownTime, 1000);
				// shuffle == true -> if two or more workers have the same rank, one is selected randomly; otherwise first
				worker = rSelect.bestWorker();

				workerList = addWorker2workerList(worker, taskID, workerList, jobList);

			}// end task


			/*
			 * workerList
			 * -> output
			 *  -> update workers
			 */
			// each output array represents a line of the output file
			for(Worker w:allWorkers){
				for(int i = 0; i < workerList.length; i++){
					if(workerList[i] == w.getId()){
						output[i] = w.getName();
						w.addNewActivity(eDate, e);
					}
					/*
					// This code sets the last active date to the current date for a worker whose "worksWithout partner"
					// was active. This makes the assignment for the worker more unlikely. Therefore it is not active.

					else if(workerList[i]!= 0 && w.getWorksWithout().contains(workerList[i])){
						w.setLastDate(eDate);
					}
					*/
					else if(workerList[i] == -2){
						output[i] = "nobody available";
					}
				}
			}

			// clean the output
			for (int i = 0; i < output.length; i++) {
				if (output[i] == "" || output[i] == null) {
					output[i] = " ";
					break;
				}
			}
			theOutput.add(output);
		} // end event
	}

	private static void writeOutputFile(){
		// write Excel

		ExcelWriter writer;
		try {
			writer = new ExcelWriter();
			writer.writeResult(theOutput);
			writer.writeWorkers(allWorkers);
			writer.finishFile();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		LOGGER.info("Finished!");

	}

	private static int[] addWorker2workerList(int workerID, int taskID,
			int[] workerList, ArrayList<Integer> jobList) {

		for(int i = 0; i < jobList.size(); i++){
			if(jobList.get(i) == taskID && workerList[i] == 0){
				workerList[i] = workerID;
				break;
			}
		}
		return workerList;
	}

}
