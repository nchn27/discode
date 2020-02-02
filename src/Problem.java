import java.util.HashSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Problem implements Comparable<Problem> {
	
	private long contestID;
	private String name;
	private long rating;
	private String index;
	private String type;
	private double points;
	private HashSet<String> tags;
	
	public static final HashSet<String> allTags = new HashSet<String>();
	
	public Problem(JSONObject problem) {
		contestID = (long) problem.get("contestId");
		name = (String) problem.get("name");
		if(problem.containsKey("rating")) rating = (long) problem.get("rating"); else rating = Long.MAX_VALUE;
		index = (String) problem.get("index");
		type = (String) problem.get("type");
		if(problem.containsKey("points")) points = (double) problem.get("points"); else points = -1.0;
		tags = new HashSet<String>();
		JSONArray array = (JSONArray) problem.get("tags");
		for(int i = 0; i < array.size(); i++) tags.add((String) array.get(i)); 
		allTags.addAll(tags);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PROBLEM:\n");
		sb.append("\tcontestID: " + contestID + "\n");
		sb.append("\tname: " + name + "\n");
		sb.append("\trating: " + rating + "\n");
		sb.append("\ttype: " + type + "\n");
		sb.append("\tpoints: " + points + "\n");
		sb.append("\ttags: ");
		for(String s : tags) {
			sb.append(s + "; ");
		}
		sb.append("\n");
		
		return sb.toString();
	}

	public String makeLink() {
		return "<https://codeforces.com/problemset/problem/" + contestID + "/" + index + "/>";
	}
	
	public String getProblemCode() {
		return contestID + " " + index;
	}
	
	public long getContestID() {
		return contestID;
	}

	public String getName() {
		return name;
	}

	public long getRating() {
		return rating;
	}

	public String getIndex() {
		return index;
	}

	public String getType() {
		return type;
	}

	public double getPoints() {
		return points;
	}

	public HashSet<String> getTags() {
		return tags;
	}
	
	/*
	 * Sorts by recency
	 */
	public int compareTo(Problem p) {
		if(contestID == p.contestID) return index.compareTo(p.index);
		else return Long.compare(contestID, p.contestID);
	}
}
