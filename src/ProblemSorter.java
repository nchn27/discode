import java.util.Comparator;
import java.util.HashSet;
import java.util.StringTokenizer;

public class ProblemSorter implements Comparator<Problem> {
	
	private int[] ratingBound;
	private String query;
	private HashSet[] attemptedProblems;
	private HashSet<String> allTags;
	private HashSet<String> musts;
	private String order;
	
	public ProblemSorter(int[] ratingBound, String handleQuery, HashSet<String> allTags, HashSet<String> musts, String order) {
		this.ratingBound = ratingBound;
		this.allTags = allTags;
		this.musts = musts;
		this.order = order;
		
		if(handleQuery.length() > 0) {
			StringTokenizer st = new StringTokenizer(handleQuery);
			query = st.nextToken(); 
			st.nextToken();
			String handle = st.nextToken();
			
			attemptedProblems = Loader.getAttemptedProblems(handle);
		}
	}
	
	public boolean shouldCull(Problem c) {
		if(ratingBound.length > 0) {
			if(c.getRating() < ratingBound[0]) return true;
			if(ratingBound.length > 1 && c.getRating() > ratingBound[1]) return true;
		}
		
		if(query != null) {
			if(query.equals("unsolved")) {
				if(attemptedProblems[0].contains(c)) return true;
			}
			if(query.equals("unfinished")) {
				if(!attemptedProblems[1].contains(c)) return true;
			}
			if(query.equals("solved")) {
				if(!attemptedProblems[0].contains(c)) return true;
			}
			if(query.equals("unattempted")) {
				if(attemptedProblems[0].contains(c) || attemptedProblems[1].contains(c)) return true;
			}
		}
		
		if(Loader.overlap(musts, c.getTags()) != musts.size()) return true;
		
		return false;
	}
	
	public int compare(Problem a, Problem b) {
		//tags take precedence
		int tag1 = Loader.overlap(a.getTags(), allTags);
		int tag2 = Loader.overlap(b.getTags(), allTags);
		if(tag1 != tag2) return -Integer.compare(tag1, tag2);
		
		//special problems go last
		if(a.getRating() == Long.MAX_VALUE && b.getRating() != Long.MAX_VALUE) {
			return 1;
		}
		if(a.getRating() != Long.MAX_VALUE && b.getRating() == Long.MAX_VALUE) {
			return -1;
		}
		
		//orderings 
		if(order.equals("easy"))
			return Long.compare(a.getRating(), b.getRating());
		if(order.equals("hard"))
			return Long.compare(b.getRating(), a.getRating());
		if(order.equals("new")) {
			return b.compareTo(a);
		}
		if(order.equals("old")) {
			return a.compareTo(b);
		}
		//default ordering:
		return Long.compare(a.getRating(), b.getRating());
	}
	
	public boolean emptyUser() {
		return query != null && attemptedProblems[0].size() == 0 && attemptedProblems[1].size() == 0;
	}
}
