import java.util.HashMap;

import org.json.simple.JSONObject;

public class Contest implements Comparable<Contest> {
	
	private String phase; 
	private long relativeTimeSeconds; 
	private long durationSeconds; 
	private String name; 
	private boolean frozen; 
	private long id; 
	private String type; 
	private long startTimeSeconds; 
	private HashMap<String, Problem> problems;
	
	public Contest(JSONObject contest) {
		phase = (String) contest.get("phase");
		relativeTimeSeconds = (long) contest.get("relativeTimeSeconds");
		durationSeconds = (long) contest.get("durationSeconds");
		name = (String) contest.get("name");
		frozen = (boolean) contest.get("frozen");
		id = (long) contest.get("id");
		type = (String) contest.get("type");
		startTimeSeconds = (long) contest.get("startTimeSeconds");
		problems = new HashMap<String, Problem>();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CONTEST:\n");
		sb.append("\tid: " + id + "\n");
		sb.append("\tname: " + name + "\n");
		sb.append("\tphase: " + phase + "\n");
		sb.append("\ttype: " + type + "\n");
		sb.append("\trelativeTimeSeconds: " + relativeTimeSeconds + "\n");
		sb.append("\tstartTimeSeconds: " + startTimeSeconds + "\n");
		sb.append("\tdurationSeconds: " + durationSeconds + "\n");
		sb.append("\tfrozen: " + frozen + "\n");
		for(String s : problems.keySet()) {
			sb.append("\t");
			sb.append(problems.get(s));
		}
		
		return sb.toString();
	}
	
	public HashMap<String, Problem> getProblems() {
		return problems;
	}
	
	public String getPhase() {
		return phase;
	}

	public long getRelativeTimeSeconds() {
		return relativeTimeSeconds;
	}

	public long getDurationSeconds() {
		return durationSeconds;
	}

	public String getName() {
		return name;
	}

	public boolean isFrozen() {
		return frozen;
	}

	public long getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public long getStartTimeSeconds() {
		return startTimeSeconds;
	}

	public void addProblem(Problem problem) {
		problems.put(problem.getIndex(), problem);
	}
	
	public int getContestID() {
		return (int) id;
	}
	
	public int hashCode() {
		return (int) id;
	}
	
	/*
	 * sorts by recency
	 */
	public int compareTo(Contest c) {
		return Long.compare(id, c.id);
	}
}
