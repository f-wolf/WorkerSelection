package de.felixwolf.workerSelection.selection;

import de.felixwolf.workerSelection.dataTypes.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * The ranking selection class is used to select a worker from the pool of workers who are suitable for the job.
 *
 * The ranking is used to balance the different factors which are taken into account for the selection. The different
 * factors include: number of activities, the time difference to the last activity, whether the event is preferred and
 * the time difference to the next preferred date.
 *
 * The ranking is stored as integer value for all workers. Adding a positive value to the ranking makes the selection
 * more unlikely, because the worker with the lowest ranking number is selected.
 */

public class RankingSelection {

	private static final Logger LOGGER = LoggerFactory.getLogger(RankingSelection.class);

	private Event e;
	private ArrayList<Worker> workers;
	private HashMap<Integer, Integer> ranking;
		
	public RankingSelection(Event e, ArrayList<Worker> selectionGroup){
		this.e = e;
		this.workers = selectionGroup;
		this.ranking = new HashMap<>();
		
		for(Worker w:workers){
			ranking.put(w.getId(), 0);
		}
	}

	/**
	 * 	Adds the number of active day to the score
	 */
	public void counterRanking(){
		for(Worker w:workers){
			int wID = w.getId();
			
			ArrayList<Integer> counters = e.getCounters();
			int [] wCounter = w.getCounter();
			
			int additionalScore = 0;
			for(int c:counters){
				additionalScore += wCounter[c]; 
			}
			
			ranking.put(wID, ranking.get(wID) + additionalScore);
		}
	}

	/**
	 * Reduces the score by the impact value if the event is preferred.
	 * Therefore, the worker is more likely to get selected.
	 */
	public void eventRanking(){
		int impactOfPrefEvent = Settings.getImpactOfPrefEvent();

		for(Worker w:workers){
			int wID = w.getId();
			
			ArrayList<Integer> preferedEvents = w.getPreferedEvents();
			if(preferedEvents.contains(e.getId())){
				ranking.put(wID, ranking.get(wID) - impactOfPrefEvent);
			}
		}
	}

	/*
	 * Calculates the natural logarithm of the cubed difference
	 * This value is subtracted from the score and makes the selection more likely
	 */
	public void lastActiveRaking(){
		Date eDate = e.getDate();
		
		for(Worker w:workers){
			int wID = w.getId();
			Date lastActive = w.getLastDate();
			long diff = 1;
			if(lastActive != null){
				diff = getDateDiff(lastActive, eDate, TimeUnit.DAYS);
				if(diff == 0){
					// avoid problems with log(0)
					diff = 1;
				}
			}
			double cubed = Math.pow(diff, 3);
			double natLog = Math.log(cubed);
			int scoreDiff = (int) Math.round(natLog);
			
			ranking.put(wID, ranking.get(wID) - scoreDiff);
		}
	}

	/*
     * make the selection more unlikely if the worker is close to an upcoming preferred event
     */
	public void nextPreferedDateRanking(int preFreeze, int freezeForce){
		Date eDate = e.getDate();

		for(Worker w:workers){
			//System.out.println(" ");
			//System.out.println("New worker: " + w.getName());
			int wID = w.getId();
			DatesCollection preferedDates = w.getPreferedDates();

			if(preferedDates == null){
				continue;
			}

			int minDiff = preferedDates.smallestDiffToFutureDate(eDate);

			if(minDiff == -1){
				// all preferred dates are in the past.
				continue;
			}

			// if the person is in the preFreeze phase avoid selecting him
			if(minDiff <= preFreeze){
				ranking.put(wID, ranking.get(wID) + freezeForce);
				continue;
			}
			
			// event is still to far away -> preferred selection not necessary
			else if(minDiff > 2 * preFreeze){
				continue;
			}
			
			// selection gets more likely when the preFreeze phase approaches
			else{
				double cubed = Math.pow(minDiff, 3);
				double natLog = Math.log(cubed);
				int scoreDiff = (int) Math.round(natLog);
				ranking.put(wID, ranking.get(wID) - scoreDiff);
			}
		}
	}

	/**
	 * Returns the id of the best worker
	 * @return
	 */
	public int bestWorker(){

		boolean shuffle = Settings.getShuffle();
		
		// find lowest score
		int lowestScore = 999999;
		for (Worker w : workers) {
			int score = ranking.get(w.getId());
			if (score < lowestScore) {
				lowestScore = score;
			}
		}
		
		// selection of workers with lowest score
		ArrayList<Integer> workersWithLowestScore = new ArrayList<Integer>();
		for (Worker w : workers) {
			int score = ranking.get(w.getId());
			if (score == lowestScore) {
				workersWithLowestScore.add(w.getId());
			}
		}
		
		if(shuffle){
			Collections.shuffle(workersWithLowestScore);
		}
		return workersWithLowestScore.get(0);
	}
	
	private long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
		// date1 is before date2
		long diffInMillies = date2.getTime() - date1.getTime();
		return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
	}
	
}
