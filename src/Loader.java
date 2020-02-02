import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import net.dv8tion.jda.api.entities.User;

public class Loader {
	
	private Loader(){}
	
	private static HashMap<Long, Contest> prevContests = new HashMap<Long, Contest>();
	private static HashMap<Long, Contest> upcomingContests = new HashMap<Long, Contest>();
	private static ArrayList<Problem> allProblems = new ArrayList<Problem>();
		
	public static boolean reload() {
		prevContests.clear();
		upcomingContests.clear();
		allProblems.clear();
		
		//LOAD CONTESTS:
		JSONObject jsonObject = null;
		try {
			URL url = new URL("https://codeforces.com/api/contest.list");
			HttpURLConnection test = (HttpURLConnection) url.openConnection();
			test.setRequestMethod("GET");
			
			BufferedReader br = new BufferedReader(new InputStreamReader(test.getInputStream()));
			JSONParser parser = new JSONParser();
			jsonObject = (JSONObject) parser.parse(br);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		if(jsonObject == null) return false;
		
		String status = (String) jsonObject.get("status");
		if(!status.equals("OK")) return false;
		
		JSONArray contests = (JSONArray) jsonObject.get("result");
		
		for(int i = 0; i < contests.size(); i++) {
			Contest contest = new Contest((JSONObject) contests.get(i));
			if(contest.getRelativeTimeSeconds() < 0) { 
				upcomingContests.put(contest.getId(), contest);
			} else {
				prevContests.put(contest.getId(), contest);
			}
		}
		
		//LOAD PROBLEMS:
		jsonObject = null;
		try {
			URL url = new URL("https://codeforces.com/api/problemset.problems");
			HttpURLConnection test = (HttpURLConnection) url.openConnection();
			test.setRequestMethod("GET");
			
			BufferedReader br = new BufferedReader(new InputStreamReader(test.getInputStream()));
			JSONParser parser = new JSONParser();
			jsonObject = (JSONObject) parser.parse(br);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		if(jsonObject == null) return false;
		
		status = (String) jsonObject.get("status");
		if(!status.equals("OK")) return false;
		
		JSONObject result = (JSONObject) jsonObject.get("result");
		JSONArray problems = (JSONArray) result.get("problems");
		
		for(int i = 0; i < problems.size(); i++) {
			Problem problem = new Problem((JSONObject) problems.get(i));
			prevContests.get(problem.getContestID()).addProblem(problem);
			allProblems.add(problem);
		}
		Collections.sort(allProblems);
		
		return true;
	}
	
	//size 2 - index 0 = solved, index 1 = unsolved
	public static HashSet<Problem>[] getAttemptedProblems(String handle) {
		JSONObject jsonObject = null;
		try {
			URL url = new URL("https://codeforces.com/api/user.status?handle=" + handle + "&from=1&count=20000");
			HttpURLConnection test = (HttpURLConnection) url.openConnection();
			test.setRequestMethod("GET");
			
			BufferedReader br = new BufferedReader(new InputStreamReader(test.getInputStream()));
			JSONParser parser = new JSONParser();
			jsonObject = (JSONObject) parser.parse(br);
		} catch (Exception e) {
			e.printStackTrace();
			return new HashSet[] {new HashSet<Problem>(), new HashSet<Problem>()};
		}
		
		if(jsonObject == null) return new HashSet[] {new HashSet<Problem>(), new HashSet<Problem>()};
		
		String status = (String) jsonObject.get("status");
		if(!status.equals("OK")) return new HashSet[] {new HashSet<Problem>(), new HashSet<Problem>()};
		
		JSONArray submissions = (JSONArray) jsonObject.get("result");

		HashSet<Problem> solved = new HashSet<Problem>();
		HashSet<Problem> failed = new HashSet<Problem>();
		
		for(int i = 0; i < submissions.size(); i++) {
			JSONObject submission = (JSONObject) submissions.get(i);
			JSONObject problem = (JSONObject) submission.get("problem");
			
			if(!problem.containsKey("contestId") || !problem.containsKey("index")) continue;

			long contestId = (long) problem.get("contestId");
			String index = (String) problem.get("index");

			Problem p = null;
			if(prevContests.containsKey(contestId) && prevContests.get(contestId).getProblems().containsKey(index)) {
				p = prevContests.get(contestId).getProblems().get(index);
			}
			if(p == null) continue;

			String verdict = (String) submission.get("verdict");
			if(verdict.equals("OK")) {
				solved.add(p);
			} else {
				failed.add(p);
			}
		}

		for(Problem p : solved) failed.remove(p);

		return new HashSet[] {solved, failed};
	}
	
	public static void recordNumUsers(Set<User> users) {
		try {
			File f = new File(System.getProperty("user.dir") + "/discode-log");
			f.mkdir();
			PrintWriter pw = new PrintWriter(f.getPath() + "/discode-log" + System.currentTimeMillis() + ".txt");
			for(User u : users) {
				pw.println(u);
			}
			pw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static int overlap(HashSet<String> tags, HashSet<String> otherTags) {
		if(otherTags == null) return 0;
		int ret = 0;
		for(String s : otherTags) {
			if(tags.contains(s)) ret++;
		}
		return ret;
	}
	
	public static HashMap<Long, Contest> getPrevContests() {
		return prevContests;
	}

	public static HashMap<Long, Contest> getUpcomingContests() {
		return upcomingContests;
	}

	public static ArrayList<Problem> getAllProblems() { return allProblems; }
}
