import java.util.ArrayList;
import java.util.Date;


public class Event {
	
	private String name;
	private int id;
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
	
}
