/** This class implements leader.
  * The server will call functions in leader accordingly if certain messages are received.
  * The leader will call send method in server to send any messages.
  */
import java.util.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class Leader{
	private Server server;
	private int ballotNum;
	private int selfId;
	private boolean active;
	private Map<Integer, Command> proposals;
	public Scout scout;
	public int commanderIdCounter;
	public  Map<Integer, Commander> commanders;
	public boolean hold;

	public boolean scoutInitialized;
	
	//to handle crashP1a & crashP2a & crashDecision
	// public boolean crashP1a;
	public boolean crashP2a;
	public boolean crashDecision;
	public ArrayList<Integer> subsetProcesses;

	
	/**
	 * Constructor
	 * @param [server] [pass in the server object to get necessary information from it]
	 */
	public Leader(Server server) {
		this.server = server;
		selfId = server.serverId;
		ballotNum = selfId;
		active = false;
		proposals = new HashMap<Integer, Command>();
		commanderIdCounter = 0;
		commanders = new HashMap<Integer, Commander>();
		hold = true;

		scoutInitialized = false;
		
		//to handle crashP1a & crashP2a & crashDecision
		//crashP1a = false;
		crashP2a = false;
		crashDecision = false;
		subsetProcesses = null;
	}

	/**
	 * create scout to send p1a to acceptors
	 */
	public void createScout(){
		scout = new Scout(server, ballotNum);

		scout.sendP1a();
	}

	public void initializeScout(){
		if(scoutInitialized==false){
			scoutInitialized = true;
			createScout();
		}
	}

	public Scout getScout() {
		return scout;
	}

	/** Respond to propose messages. */
	public synchronized void propose(int s, Command p) {
		System.out.println("in leader propose, leader state active is: "+ active);
		

		if (!proposals.containsKey(s)) {
			proposals.put(s, p);
			System.out.println("Current Leader State: "+active);
			server.debugPrint("server" + server.serverId + " in propose(), active = " + active);
			server.debugPrint("Command saved in proposals is "+ p+", with ballotNum "+s+", and Current proposals is "+proposals.toString());
			
			removeDuplicates();
			server.debugPrint("Leader proposals after removeDuplicates is: "+ proposals);
			if (active) {
				server.debugPrint("server" + server.serverId + " changing server.isProposing to true");
				server.isProposing = true;
				System.out.println("leader gonna create new Commander");
				Pvalue pvalue = new Pvalue(ballotNum, s, p);
				server.debugPrint("server" + server.serverId + " in propose proposing: " + pvalue);
				Commander newCommander = new Commander(commanderIdCounter, server, pvalue);
				commanders.put(commanderIdCounter, newCommander);
				newCommander.sendP2a();
				commanderIdCounter += 1;
			}
		} else {
			System.out.println("the SlotNum "+s+" you proposed has already in proposal");
			server.debugPrint("the SlotNum "+s+" you proposed has already in proposal");
		}
	}

	/** Respond to adopted messages. */
	public synchronized void adopted(int b, List<Pvalue> pvalues) {
		server.debugPrint("server" + server.serverId + " in adopted()");

		updateProposals(pmax(pvalues));

		if (proposals.size() != 0) {
			server.debugPrint("" + server.serverId + " changing server.isProposing to true");
			server.isProposing = true;
		}

		server.debugPrint("server" + server.serverId + " leader proposals: " + proposals.toString());

		for (Map.Entry<Integer, Command> e : proposals.entrySet()) {
			//only command is not in decision map
			if(!server.replica.decisions.containsValue(e.getValue()) ){//&& !server.replica.decisions.containsKey(e.getKey())
				Pvalue pvalue;
				if(server.replica.decisions.containsKey(e.getKey())){
					pvalue = new Pvalue(ballotNum, server.replica.minSlotNum, e.getValue());
				}else{
				    pvalue = new Pvalue(ballotNum, e.getKey(), e.getValue());
				}
				server.debugPrint("server" + server.serverId + " in adopted proposing: " + pvalue);
				Commander newCommander = new Commander(commanderIdCounter, server, pvalue);
				commanders.put(commanderIdCounter, newCommander);
				newCommander.sendP2a();
				commanderIdCounter += 1;
			}
			
		}

		active = true;
		System.out.println("Leader state is Acitve now");
		server.debugPrint("Server"+server.serverId+" leader state becomes active");
	}

	/** Respond to preempted messages. */
	public synchronized void preempted(int b, int serverId) {
		System.out.println("~~~~~~PREEMPTED Happended");
		System.out.println("preempted by ballotNum: " + b + " from " + serverId);
		System.out.println("ballotNum of self: " + ballotNum);

		server.debugPrint("server" + server.serverId + " has ballotNum:" + ballotNum);
		server.debugPrint("server" + server.serverId + " preempted by ballotNum: " + b + " from " + serverId);
		if (b > ballotNum) {
			active = false;
			ballotNum = (b / 10 + 1) * 10 + selfId;

			if (serverId != selfId) {
				try{
					//
					server.debugPrint("server" + server.serverId + " asking server" + (b % 10) + " isProposing");
					server.send(b % 10, "isProposing?_" + selfId);
					System.out.println("sent isProposing");

					//check whether a socket connection still alive
					// int socketId = b % 10;
					// Socket sock = server.serverSockets.get(socketId);
					


				}catch(Exception e){
					e.printStackTrace();
				}
			}
			else if (!server.isProposing) {
				ballotNum = (b / 10 + 1) * 10 + selfId;
				scout = new Scout(server, ballotNum);
			}

			// server.acceptor.ballotNum = ballotNum;
		}
	}

	/** Apply the pmax operation.
	  * pvalues is a set of pvalues.
	  * Return a set of proposals (a map).
	  */
	private synchronized Map<Integer, Command> pmax(List<Pvalue> pvalues) {

		Map<Integer, Command> result = new HashMap<Integer, Command>();
		Map<Integer, Integer> maxB = new HashMap<Integer, Integer>();

		for (Pvalue p : pvalues)
			if (!maxB.containsKey(p.s) || p.b > maxB.get(p.s)) {
				result.put(p.s, p.c);
				maxB.put(p.s, p.b);
			}

		return result;
	}

	/** Circle plus two sets of proposals.
	  * p is the set returned from pmax().
	  */
	private void updateProposals(Map<Integer, Command> p) {
		Map<Integer, Command> oldProposals = new HashMap<Integer, Command>(proposals);

		proposals = p;

		for (Map.Entry<Integer, Command> e : oldProposals.entrySet())
			if (!p.containsKey(e.getKey()))
				proposals.put(e.getKey(), e.getValue());

		//removeDuplicates();
	}

	private void removeDuplicates() {
		server.debugPrint("server" + server.serverId + " server.replica.decisions: " + server.replica.decisions.toString());

		for (Map.Entry<Integer, Command> e : server.replica.decisions.entrySet()){
			if (proposals.containsKey(e.getKey())){
				if(server.replica.decisions.containsValue(e.getValue())){
					proposals.remove(e.getKey());
				}else{
					proposals.put(server.replica.minSlotNum, e.getValue());
					proposals.remove(e.getKey());
				}
				
			}
			// if (proposals.containsValue(e.getValue())){
			// 	int pKey = proposals.getKey(e.getValue());
			// 	proposals.remove(pKey);

			// }else{
			// 	if(proposals.containsKey(e.getKey())){				
			// 		proposals.put(server.replica.minSlotNum, e.getValue());
			// 		proposals.remove(e.getKey());
			// 	}
			// }
				
		}

			

	}
}