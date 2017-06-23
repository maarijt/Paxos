import java.io.*;
import java.net.*;
import java.util.*;


public class Scout{
	private Server server;
	private int ballotNum;
	private List<Pvalue> pvalues;
	private int counter;
	private int thresh;
	private boolean hasAdopted;
	private boolean dead;

	/**
	 * Constructor
	 * @param server [description]
	 */
	public Scout(Server s, int b){
		server = s;
		ballotNum = b;
		pvalues = new ArrayList<Pvalue>();
		counter = 0;
		thresh = s.numServers / 2 + 1;
		hasAdopted = false;
		dead = false;

		server.debugPrint("server" + server.serverId + "'s scout with ballotNum: " + ballotNum);
	}

	/**
	 * function: send P1a to all acceptors through tcp connections
	 */
	public void sendP1a(){
		System.out.println("Scout sending p1a to gather ballotNum from all acceptors!");
		String message = "p1a_" + server.serverId + "_" + ballotNum;
		if(server.crashP1a){
			server.debugPrint("crashP1a already true");
			try{
				server.sendToSubset(server.leader.subsetProcesses, message);
				System.exit(1);
			}catch(Exception e){
				System.out.println(e.getMessage());
				server.debugPrint(e.getMessage());
			}
		}else{

			try {
				server.sendAll(message);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		
	}

	public void p1b(int serverId, int b, ArrayList<Pvalue> r) {
		server.debugPrint("server" + server.serverId + " get a p1b vote from server" + serverId + " with ballotNum: " + b);
		server.debugPrint("server" + server.serverId + "'s scout is " + (dead ? "dead" : "alive"));

		if (dead)
			return;

		//System.out.println("scout handling p1b");
		// System.out.println("scout ballotNum is:"+ballotNum+" ,received ballotNum in p1b: "+b
		// 	+" ,acceptor ballotNum is "+server.acceptor.ballotNum);

		int selfVote = 0;

		server.debugPrint("server" + server.serverId + " get a p1b vote from server" + serverId + " with ballotNum: " + b);
		server.debugPrint("server" + server.serverId + "'s own ballotNum: " + ballotNum);

		if (ballotNum < server.acceptor.ballotNum) {
			dead = true;
			server.leader.preempted(server.acceptor.ballotNum, serverId);
			return;
		}

		if (ballotNum >= server.acceptor.ballotNum) {
			selfVote = 1;
			server.acceptor.ballotNum = ballotNum;
		}

		if (ballotNum == b) {
			counter += 1;
			server.debugPrint("server" + server.serverId + "'s numVotes: " + (counter+selfVote));
			//if no pvalues being sent to scout, the parsed pvalue would be null
			if(r!=null){
				for (Pvalue p : r) {
					pvalues.add(p);
				}
			}

			// if (counter >= thresh && !hasAdopted){
			if (counter >= thresh - selfVote && !hasAdopted){
				hasAdopted = true;
				dead = true;
				//System.out.println("Gonna change leader state to ACTIVE");
				System.out.println("counter is: "+counter+", thresh is: "+(thresh-(ballotNum >= server.acceptor.ballotNum ? 1 : 0)));
				server.leader.adopted(ballotNum, pvalues);
			}
			
			
		}
		else if(ballotNum < b){
			dead = true;
			server.leader.preempted(b, serverId);
		}
	}

}