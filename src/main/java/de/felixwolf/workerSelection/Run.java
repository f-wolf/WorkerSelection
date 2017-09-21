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


	public static void main(String[] args) {

		String inputPath = "input.xlsx";
		int columnsWithText = 4;
		int impactOfPrefEvent = 3;
		boolean shuffle = true;

		ArrayList<Task> allTasks = new ArrayList<Task>();
		ArrayList<Event> allEvents = new ArrayList<Event>();
		ArrayList<Worker> allWorkers = new ArrayList<Worker>();

		// read excel file
		ExcelReader excelReader = new ExcelReader(inputPath);
		allTasks = excelReader.readTasks();
		LOGGER.info("All tasks are read");

		allEvents = excelReader.readEvents();
		LOGGER.info("All events are read");
		int biggestCounter = excelReader.getBiggestCounter(); // has to be after readEvents
		allWorkers = excelReader.readWorkers(biggestCounter);
		LOGGER.info("Workers are read");
		int coolDownTime = excelReader.readCoolDown();
		System.out.println("The excel file is read");


		ArrayList<String[]> theOutput = new ArrayList<String[]>();
		Analysor ana = new Analysor();
		
		// create the jobList
		ArrayList<Integer> jobList = new ArrayList<Integer>(); // set positions
		jobList = ana.createJobList(allEvents, allTasks, columnsWithText);
		
		// create legend and add to output
		String [] legend = ana.createLegend(jobList, allTasks, columnsWithText);
		//System.out.println(Arrays.toString(legend));
		theOutput.add(legend);
		
		// calculate the week of year for the first event
		// -> enables break after each week
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
			//System.out.println(" ");
			//System.out.println(eDate + " -- " + eID);
			
			// tools
			Reducor red0 = new Reducor();
			PreferenceSelection pSelect = new PreferenceSelection();

			// new line?
			int currentWeek = ana.calcWeekOfYear(e);
			if (currentWeek != lastWeek) {
				// System.out.println("Diff with weeks");
				lastWeek = currentWeek;
				String[] emtpty = { "", "" };
				theOutput.add(emtpty);
			}
			
			// write the basic info to the output string
			output = ana.creatEventInfo(e, jobList.size());
			

			for (int taskID : e.getEventTasks()) {
				// System.out.println(" ");
				
				//System.out.println("new main.java.de.felixwolf.workerSelection.dataTypes.Task: " + taskID);
				int worker = -1;
				
				ArrayList<Worker> selectionGroup0 = allWorkers;
				//System.out.println("size g0: " + selectionGroup0.size());
				// remove workers from selection group who are not suitable (wrong task)
				ArrayList<Worker> selectionGroup1 = red0.reduceGroupTask(taskID, selectionGroup0);
				//System.out.println("size g1: " + selectionGroup1.size());
				
				// remove workers from selection group who have no time
				ArrayList<Worker> selectionGroup2 = red0.reduceGroupDate(e, selectionGroup1);
				//System.out.println("size g2: " + selectionGroup2.size());
				
				// remove workers who don't want to work with somebody who is already on the list
				ArrayList<Worker> selectionGroup3 = red0.reduceGroupWorksWithout(eDate, workerList, selectionGroup2, coolDownTime);
				//System.out.println("size g3: " + selectionGroup3.size());
				
				// remove workers who are already on the list with the same task
				ArrayList<Worker> selectionGroup4 = red0.reduceAlreadyDidSameTask(workerList, taskID, jobList, selectionGroup3);
				//System.out.println("size g4: " + selectionGroup4.size());
				
				//System.out.println("start" + Arrays.toString(workerList));
				
				// if somebody is already doing a job, give him another one if it is different
				worker = pSelect.alreadyActiveWorker(workerList, taskID, jobList, selectionGroup4);
				if(worker != -1){
					// write information
					workerList = addWorker2workerList(worker, taskID, workerList, jobList);
					//System.out.println("alreadyActive" + Arrays.toString(workerList));

					continue;
				}

				// remove workers who have worked in the coolDownPhase
				ArrayList<Worker> selectionGroup5 = red0.reduceGroupCoolDown(eDate, coolDownTime, selectionGroup4);
				//System.out.println("size g5: " + selectionGroup5.size());
				
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
					//System.out.println("datePref" + Arrays.toString(workerList));

					continue;
				}
				

				
				//System.out.println(eDate + ": size of group: " + selectionGroup5.size());
				
				// find next worker with ranking
				RankingSelection rSelect = new RankingSelection(e, selectionGroup5);
				rSelect.counterRanking();
				rSelect.eventRanking(impactOfPrefEvent);
				rSelect.lastActiveRaking();
				rSelect.nextPreferedDateRanking(coolDownTime, 1000);
				worker = rSelect.bestWorker(shuffle);
				
				//System.out.println("ranked0: " + Arrays.toString(workerList));
				workerList = addWorker2workerList(worker, taskID, workerList, jobList);
				//System.out.println("ranked: " + Arrays.toString(workerList));
				//System.out.println(" ");
				
				
				
			}// end task

			
			/*
			 * workerList
			 * -> output
			 *  -> update workers
			 */
			
			//System.out.println(eDate + ": " + Arrays.toString(workerList));
			
			for(Worker w:allWorkers){
				for(int i = 0; i < workerList.length; i++){
					if(workerList[i] == w.getId()){
						output[i] = w.getName();
						w.addNewActivity(eDate, e);
					}
					else if(workerList[i]!= 0 && workerList[i] == w.getWorksWithout()){
						w.setLastDate(eDate);
					}
					if(workerList[i] == -2){
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

		// write Excel

		ExcelWriter writer;
		try {
			writer = new ExcelWriter();
			writer.writeResult(theOutput);
			writer.writeWorkers(allWorkers);
			writer.finishFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		System.out.println("done");

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

	public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
		// date1 is before date2
		long diffInMillies = date2.getTime() - date1.getTime();
		return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
	}

}
