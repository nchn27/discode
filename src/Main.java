import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.StringTokenizer;

import javax.swing.JFrame;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/*

INVITE LINK:
https://discordapp.com/oauth2/authorize?&client_id=619691181521109035&scope=bot&permissions=8

 */

public class Main {

	private static Queue<MessageReceivedEvent> q = new ArrayDeque<MessageReceivedEvent>();
	private static Queue<Long> timeQueue = new ArrayDeque<Long>();
	private static long nextScheduledUpdateMillis = System.currentTimeMillis();
	
	private static HashMap<User, ArrayList<String>> activeQuery = new HashMap<User, ArrayList<String>>();
	private static HashMap<User, String> query = new HashMap<User, String>();
	
	private static HashSet<User> allUsers = new HashSet<User>();
	
	private static JDA api;
	
	public static void main(String[] args) throws Exception {
		api = new JDABuilder("#####do not steal my bot please#####").build();

		api.addEventListener(new MyListener());

		JFrame frame = new JFrame("Close to turn off bot");
		frame.setSize(800, 600);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		long lastTick = System.nanoTime();
		double delta = 1e9 / 20.0;
		long lastStat = System.currentTimeMillis();
		double ticks = 0;
		while (true) {
			while (System.nanoTime() - lastTick >= delta) {
				tick();
				ticks++;

				if (System.nanoTime() - lastTick >= 1e9) {
					System.out.println("I fell behind by at least one second. Catching up...");
					lastTick = System.nanoTime();
				}
				lastTick += delta;
			}
			if (System.currentTimeMillis() - lastStat >= 5000) {
				double tps = ticks / (System.currentTimeMillis() - lastStat) * 1000.0;
				lastStat = System.currentTimeMillis();
				System.out.println("TPS: " + tps);

				ticks = 0;
			}
		}

	}

	private static void tick() {
		// maintain updates of codeforces.com and do scheduled logs/updates
		if (System.currentTimeMillis() > nextScheduledUpdateMillis) {
			System.out.println("Reloading CodeForces...");
			while (!Loader.reload())
				System.out.println("Failed Attempt to Reload CodeForces. Retrying...");
			nextScheduledUpdateMillis = System.currentTimeMillis() + 120L * 60 * 1000; // updates every 120 minutes
			System.out.println("Reloaded CodeForces.");
			System.out.println("Saving a log.");
			Loader.recordNumUsers(allUsers);
			api.getPresence().setActivity(Activity.playing(".help | New Problem Orderings!"));
		}
		
		// respond
		if (q.size() > 0)
			System.out.println("Queue is larger than 0: " + q.size());
		while (!q.isEmpty()) {
			boolean reportTime = false;
			
			long startTime = timeQueue.poll();
			MessageReceivedEvent event = q.poll();
			
			Message message = event.getMessage();
			MessageChannel channel = event.getChannel();
			String content = message.getContentRaw();
			int indexOfSpace = content.indexOf(" ");
			String pre = content.substring(0, indexOfSpace == -1 ? content.length() : indexOfSpace);
						
			StringTokenizer byCommas = new StringTokenizer("");
			if(indexOfSpace+1 < content.length() && indexOfSpace != -1) byCommas = new StringTokenizer(content.substring(indexOfSpace+1, content.length()), ",");
						
			String contentForDisplay = content.length() >= 100 ? content.substring(0,97)+"..." : content;
			
			if (pre.equals(".page")) {
				if (byCommas.countTokens() != 1) {
					channel.sendMessage("Only one argument is needed for \".page\".").queue();
					continue;
				} else {
					String page = byCommas.nextToken();
					int pageNum = -1;
					if (isNumber(page)) {
						pageNum = Integer.parseInt(page);
					} else {
						channel.sendMessage("Invalid integer.").queue();
						continue;
					}

					if (!activeQuery.containsKey(message.getAuthor())) {
						channel.sendMessage("No pages to look at.").queue();
						continue;
					}

					int maxPage = (activeQuery.get(message.getAuthor()).size() - 1) / 10 + 1;
					if (pageNum <= 0 || pageNum > maxPage) {
						channel.sendMessage("That page is outside the range.").queue();
						continue;
					}

					StringBuilder ret = new StringBuilder();
					ret.append("__Page " + pageNum + " of " + maxPage + " [" + query.get(message.getAuthor()) + "]__\n");
					ret.append(">>> ");
					for (int i = pageNum * 10 - 10; i < Math.min(pageNum * 10,
							activeQuery.get(message.getAuthor()).size()); i++) {
						ret.append(activeQuery.get(message.getAuthor()).get(i) + "\n");
					}
					
					channel.sendMessage(ret.toString()).queue();
				}
				
				allUsers.add(message.getAuthor());
			}
			if (pre.equals(".find")) {
				int[] ratingBounds = new int[0];
				
				String handleQuery = "";
				String order = "";
				
				HashSet<String> must = new HashSet<String>();
				HashSet<String> tags = new HashSet<String>();
				
				HashMap<String, String> problematicTags = new HashMap<String, String>();
				
				while (byCommas.hasMoreElements()) {
					String s = byCommas.nextToken();
					s = s.trim();
					if(s.length() >= 175) {
						problematicTags.put(s.substring(0,100) + "...", "Parameter was more than 175 characters long.");
						continue;
					}
					
					if(Main.isRatingBound(s)) {
						if(ratingBounds.length == 0) ratingBounds = Main.getRatings(s);
						else problematicTags.put(s, "More than one rating bound found.");
						continue;
					}
					
					if (s.startsWith("must have ")) {
						must.add(s.substring(10, s.length()));
						tags.add(s.substring(10, s.length()));
						continue;
					}
					
					if(s.startsWith("order by ")) {
						String orderParameter = s.substring(9,s.length());
						if(!orderParameter.equals("new") && !orderParameter.equals("old") && !orderParameter.equals("hard") && !orderParameter.equals("easy")) {
							problematicTags.put(s, "Invalid ordering type [Use \"old\", \"new\", \"hard\", or \"easy\"].");
						}
						if(order.length() == 0) order = s.substring(9, s.length());
						else problematicTags.put(s, "More than one ordering found.");
						continue;
					}
					
					if(s.startsWith("unsolved by") || s.startsWith("unattempted by") || 
							s.startsWith("solved by") || s.startsWith("unfinished by")) {
						if(new StringTokenizer(s).countTokens() == 2) {
							problematicTags.put(s, "No user specified");
							continue;
						}
						if(new StringTokenizer(s).countTokens() > 3) {
							problematicTags.put(s, "User handle should not have spaces in it");
							continue;
						}
						if(handleQuery.length() == 0) handleQuery = s; 
						else problematicTags.put(s, "More than one parameter related to specific user found.");
						continue;
					}
					
					tags.add(s);
				}

				for (String s : tags)
					if (!Problem.allTags.contains(s)) {
						problematicTags.put(s, "Not a valid parameter");
					}
				StringBuilder ret = new StringBuilder();

				ArrayList<Problem> targets = new ArrayList<Problem>();
				ArrayList<String> texts = new ArrayList<String>();
				
				if(problematicTags.size() > 0) {
					ret.append("Bad parameters:\n >>> ");
					int counter = 0;
					for(String s : problematicTags.keySet()) {
						counter++;
						if(counter > 5) break;
						ret.append(s + " - " + problematicTags.get(s) + "\n");
					}
					if(counter > 5) ret.append("...\n");
					
				} else {
					if(handleQuery != null) channel.sendTyping().queue(); //lol cause the internet takes a while
					
					ProblemSorter decision = new ProblemSorter(ratingBounds, handleQuery, tags, must, order);
					for(Problem p : Loader.getAllProblems()) {
						if(!decision.shouldCull(p)) targets.add(p);
					}
					Collections.sort(targets, decision);
					
					if(decision.emptyUser()) {
						channel.sendMessage("User has no submissions.").queue();
						continue;
					}
					
					if (targets.size() == 0)
						ret.append("No problems found!\n");
					else {
						for(int i = 0; i < targets.size(); i++) {
							texts.add((i+1) + ". " + targets.get(i).getProblemCode() + " ~ *"
									+ (targets.get(i).getRating() == Long.MAX_VALUE ? "unrated" : targets.get(i).getRating())
									+ (tags.size()>0 ? "* ~ [" + (Loader.overlap(targets.get(i).getTags(), tags)) + "/" + tags.size() + "] ~ **" : "* ~ **")
									+ targets.get(i).getName() + "** ~ " + targets.get(i).makeLink() + "\n");
						}
						
						activeQuery.put(message.getAuthor(), texts);
						query.put(message.getAuthor(), contentForDisplay);
					}
					
					ret.append("__Page 1 of " + ((targets.size() - 1) / 10 + 1) + " [" + contentForDisplay + "]__\n");
					ret.append(">>> ");
					for (int i = 0; i < Math.min(10, targets.size()); i++) {
						ret.append(texts.get(i) + "\n");
					}
				}
				
				channel.sendMessage(ret.toString()).queue();
				reportTime = true;
				allUsers.add(message.getAuthor());
			}
			if(pre.equals(".help")) {
				String help = ">>> :point_right: Use .tags to get a list of tags.\n"
						+ ":point_right: Use .page followed by an integer to look through long lists.\n"
						+ ":point_right: Use .disclaimer for any important messages.\n"
						+ ":point_right: Use .find to find problems:\n"
						+ "\t You can search with more specificity by adding a **comma-separated** list of parameters after \".find\":\n"
						+ "\t :right_facing_fist: **Lower rating bound:** a single integer, like \"1900\"\n"
						+ "\t :right_facing_fist: **Rating range:** two integers with a dash in between, like \"1200-1300\"\n"
						+ "\t :right_facing_fist: **Ordering:** Say \"order by \" + \"old\", \"new\", \"easy\", or \"hard\".\n"
						+ "\t :right_facing_fist: **Problem tags:** e.g. \"divide and conquer\" or \"implementation\"\n"
						+ "\t\t :point_right: Exclude problems without a specific tag by writing \"must have\" before it, like \"must have dp\"\n"
						+ "\t :right_facing_fist: **User-related queries:** Use one of the phrases \"unfinished by\", \"unattempted by\", \"solved by\" or \"unsolved by\" followed by a user handle "
							+"(e.g. \"solved by nchn27\")\n\n"
						+"Example (Finding problems): \".find 1400-2200, divide and conquer, must have dp, order by hard\"\n"
						+"Example (Stalking): \".find solved by nchn27, order by new\"\n"
						+"Example (Whole problem list): \".find\"";
				channel.sendMessage(help).queue();
				allUsers.add(message.getAuthor());
			}
			if(pre.equals(".disclaimer")) {
				channel.sendMessage("I only look at the first 20,000 submissions of a user, which should be enough to be accurate for a normal human being...").queue();
				allUsers.add(message.getAuthor());
			}
			if(pre.equals(".tags")) {
				ArrayList<String> texts = new ArrayList<String>();
				for(String tag : Problem.allTags) {
					texts.add(tag);
				}
				activeQuery.put(message.getAuthor(), texts);
				query.put(message.getAuthor(), contentForDisplay);
				
				StringBuilder ret = new StringBuilder();
				ret.append("__Page 1 of " + ((texts.size() - 1) / 10 + 1) + " [" + contentForDisplay + "]__\n");
				ret.append(">>> ");
				for (int i = 0; i < Math.min(10, texts.size()); i++) {
					ret.append(texts.get(i) + "\n");
				}
				
				channel.sendMessage(ret.toString()).queue();
				allUsers.add(message.getAuthor());
			}
			
			if(!reportTime) continue;
			StringBuilder timeInfo = new StringBuilder();
			timeInfo.append("\n*Time to process: ");
			timeInfo.append(((System.nanoTime() - startTime)) / 1e9);
			timeInfo.append(" s*");
			channel.sendMessage(timeInfo.toString()).queue();
		}
	}
	
	private static int[] getRatings(String s) {
		int indexOfDash = s.indexOf("-");
		if(indexOfDash == -1) return new int[] {Integer.parseInt(s)};
		return new int[] {Integer.parseInt(s.substring(0, indexOfDash)), Integer.parseInt(s.substring(indexOfDash+1, s.length()))};
	}

	private static boolean isRatingBound(String s) {
		boolean ret = true;
		int indexOfDash = s.indexOf("-");
		if(indexOfDash == 0 || indexOfDash == s.length()-1) return false;
		if(indexOfDash >= 6 || s.length() - indexOfDash - 1 >= 6) return false;
		
		for(int i = 0; i < s.length(); i++) {
			if(indexOfDash == i) ret &= s.charAt(i) == '-';
			else ret &= (s.charAt(i) >= '0' && s.charAt(i) <= '9'); 
		}
		
		return ret;
	}
	
	private static boolean isNumber(String s) {
		boolean ret = true;
		for (int i = 0; i < s.length(); i++) {
			ret &= ('0' <= s.charAt(i) && '9' >= s.charAt(i));
		}
		if (s.length() > 6)
			ret = false;
		return ret;
	}
	
	private static class MyListener extends ListenerAdapter {
		public void onMessageReceived(MessageReceivedEvent event) {
			if (event.getAuthor().isBot())
				return;
			if (!event.getMessage().getContentRaw().startsWith("."))
				return;
			
			if(!event.getTextChannel().getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_WRITE))
				return;
			
			System.out.println(event.getAuthor());
			
			q.add(event);
			timeQueue.add(System.nanoTime());
		}
	}

}
