/** This class implements replica.
  * The server will call propose() and decide() accordingly if certain messages are received.
  * The replica will call send method in server to send any messages.
  */

import java.util.*;
import java.util.HashMap;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Replica {
	private Server server;	// the server in which the relica lives
	private int slotNum;
	public int minSlotNum;	// holds next slot number to be used, s' in paper
	private Map<Integer, Command> proposals;
	public Map<Integer, Command> decisions;
	private String logName;
	private int fileCounter;
	public Replica(Server s) {
		server = s;
		slotNum = 1;
		minSlotNum = 1;
		proposals = new HashMap<Integer, Command>();
		decisions = new HashMap<Integer, Command>();
		logName = "chatLog" + server.serverId + ".txt";

		fileCounter = 0;
	}

	/** Propose a command. */
	public void propose(Command p) {
		System.out.println("in replica propose");

		if (!decisions.containsValue(p)) {
			proposals.put(minSlotNum, p);

			/** "Send" leader the propose. */
			server.leader.propose(minSlotNum, p);

			minSlotNum += 1;
		}
	}

	/** Perform the decision(s).
	  * Re-propose different commands with same slot numbers, if any.
	  */
	public void decide(int s, Command c, boolean isSelf){
		System.out.println("Replica deciding");
		decisions.put(s, c);

		server.debugPrint("server" + server.serverId + " decided " + c.cid + " for slotNum " + s);
		server.debugPrint("server" + server.serverId + "'s slotNum when decided: " + slotNum);

		minSlotNum = minSlotNum <= s ? s + 1 : minSlotNum;

		while (decisions.containsKey(slotNum)) {
			if (proposals.containsKey(slotNum)) {
				Command oldCommand = proposals.get(slotNum);

				if (!c.equals(oldCommand))
					propose(oldCommand);
			}

			perform(c, isSelf);
		}
	}

	/** Perform a decision. */

	private void perform(Command c, boolean isSelf){
		server.debugPrint("server" + server.serverId + " performing decision " + c.cid);
		server.debugPrint("server" + server.serverId + "'s decisions set is: " + decisions.toString());
		
		if (getKey(c) < slotNum){
			System.out.println("In perform if!!!");
			server.debugPrint("NO ACKKKKKKK");
			slotNum += 1;
		}
		else {
			//writeChatLog(c.operation);
			System.out.println("In perform else!!!");
			if (isSelf) {
				String message = "ack " + c.cid + " " + slotNum;

				try{
					if(!server.leader.crashDecision){
						File replicaFile = new File("replicaLog_"+ server.serverId +"_" + fileCounter+".txt");
						fileCounter+=1;
						replicaFile.createNewFile();
						FileOutputStream fop = new FileOutputStream(replicaFile, false); 
						byte[] contentInBytes  = message.getBytes();
						fop.write(contentInBytes);
						fop.flush();
						fop.close();
						server.sendToMaster(message);
					}
						
				}catch(Exception e){
					e.printStackTrace();
				}
				server.debugPrint("server" + server.serverId + " changing server.isProposing to false");
				server.isProposing = false;
				server.debugPrint("server" + server.serverId + " toSendContinue size:" + server.toSendContinue.size());
				for(int i : server.toSendContinue){
					try{
						server.debugPrint("server" + server.serverId + " sending continue to server" + i);
						server.send(i, "continue");
					}catch(Exception e){
						e.printStackTrace();
					}
					

				}
				server.toSendContinue.clear();
			}

			slotNum += 1;
		}
	}

	/** Get the minimum slot number of a command if it has been decided.
	  * Otherwise, return slotNum.
	  */
	private int getKey(Command c) {
		int minSlot = slotNum;

		for (Map.Entry<Integer, Command> e : decisions.entrySet())
			if (c.equals(e.getValue()))
				minSlot = Math.min(minSlot, e.getKey());

		return minSlot;
	}

	public String tempGetDecisions(){
		return decisions.toString();
	}

	/** Write the message to its chat log. */

	private void writeChatLog(String s){

		PrintWriter writer;
		try{
			writer = new PrintWriter(logName);
			writer.println(s);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public String getChatLog() {
		String chatLog = "";

		if (decisions.size() == 0)
			return "";

		for (Command c : decisions.values())
			chatLog += c.operation + ",";

		return "chatLog " + chatLog.substring(0, chatLog.length() - 1);
	}

	public void setDecisions(HashMap<Integer, Command> decisionsMap) {
		decisions = decisionsMap;
		int tempSlotNum = -1;
		for(Map.Entry<Integer, Command> e : decisions.entrySet() ){
			if(e.getKey() > tempSlotNum){
				tempSlotNum = e.getKey();
			}
		}
		//ugly code for now
		this.minSlotNum = -1;
		for(int i = 1; i<=tempSlotNum; i++){
			if(!decisions.containsKey(i)){
				this.minSlotNum = i;
				break;
			}
		}
		if(this.minSlotNum == -1){
			this.minSlotNum = tempSlotNum+1;
		}
		this.slotNum = this.minSlotNum -1;
		// slotNum = tempSlotNum+1;
		// minSlotNum = slotNum;
		server.debugPrint("Decision saved, recovery finished, sending scouts out");
		server.leader.initializeScout();

	}
}