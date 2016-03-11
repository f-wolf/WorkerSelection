import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;


public class RankingSelection {
	
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
		//System.out.println("size of final selectionGroup: " + workers.size());
		//System.out.println(" ");
	}
	
	
	public void counterRanking(){
		// adds the number of active day to the score
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
	
	public void eventRanking(int impactOfPrefEvent){
		// reduces the score by the impact value if the event is prefered
		// therefore the worker is more likely to get picked
		for(Worker w:workers){
			int wID = w.getId();
			
			ArrayList<Integer> preferedEvents = w.getPreferedEvents();
			if(preferedEvents.contains(e.getId())){
				ranking.put(wID, ranking.get(wID) - impactOfPrefEvent);
			}
		}
	}
	
	public void lastActiveRaking(double factor){
		// - diff^2 * factor
		System.err.println("funtion not created yet");
	}
	
	public int bestWorker(boolean shuffle){
		
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
	
	
	
	
	/*
	HashMap<Integer, Integer> ranking = new HashMap<>();
	// put all workers into the hashmap with their count for initial
	// ranking
	for (Worker w : selectionGroup3) {
		ranking.put(w.getId(), w.getCounter());
	}

	/*
	 * Ranking algorithms
	 */

	/*
	 * End of ranking algorithms
	 

	// select worker with lowest score
	int nextWorkerID = 0;
	Worker nextWorker = new Worker();
	int lowestScore = Integer.MAX_VALUE;
	for (Worker w : selectionGroup3) {
		int score = ranking.get(w.getId());
		if (score < lowestScore) {
			lowestScore = score;
			nextWorkerID = w.getId();
			nextWorker = w;
		}
	}
	// place nextworker onto the list

	// System.out.println("nextWorker: " + nextWorkerID);
	Worker w0 = nextWorker;
	for (int j = 0; j < jobList.size(); j++) {
		if (jobList.get(j) == taskID && control[j]) {
			//System.out.println("placing worker with lowest score: "+ taskID + " j: " + j);
			control[j] = false;
			// System.out.println(Arrays.toString(output));
			output[j] = w0.getName();
			// System.out.println("Name: " + w0.getName());
			//System.out.println(Arrays.toString(output));
			workerList[j] = w0.getId();
			// System.out.println(Arrays.toString(workerList));
			foundWorker = true;
			w0.addNewActivity(eDate, e.getId());
			break;
		}
	}
	*/
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
