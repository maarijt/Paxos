import java.io.*;
import java.net.*;
import java.util.*;


public class ServerHandler implements Runnable {
	public Socket serverSocket;// socket that listening to other servers
	public Server server;// Methods and objects within server obj will be used according to message received
	public Scanner fromServer;
	public PrintWriter toServer;

	ServerHandler(Socket _serverSocket, Server _server) throws Exception {
		serverSocket = _serverSocket;
		server = _server;
		fromServer = new Scanner(serverSocket.getInputStream());
		toServer = new PrintWriter(serverSocket.getOutputStream(), true);

	}

	public void run() {
		System.out.println("Thread started fine, gonna call sendall");
		try{
			server.sendAll("Hello from server "+server.serverId);
		}catch(Exception e){
			// System.out.println(e.get)
			e.printStackTrace();
		}
		
		while(true) {
			if (fromServer.hasNext()) {
				String inputData = fromServer.nextLine();
				System.out.println(inputData);


				if(!server.isRecovering  && server.serverSockets.size() == server.numServers -1){

					//server.leader.createScout();
					server.leader.initializeScout();
				}

				if (inputData.contains("Hello")) {
					String[] temp = inputData.split("\\s+");
					//System.out.println("recieved port is " + temp[1]);
					int incomingServerID = Integer.parseInt(temp[temp.length - 1]); 
					int tempSize = server.serverSockets.size();
					server.serverSockets.put(incomingServerID, serverSocket);

					// if socket becomes n-1, send scout for the very first time
					// if(server.serverSockets.size() == server.numServers -1){

					// 	server.leader.createScout();
					// }

					if(!server.isRecovering  && server.serverSockets.size() == server.numServers -1){

						//server.leader.createScout();
						server.leader.initializeScout();
					}


					server.activeIds.add(incomingServerID);
					server.log = new Log(server.serverId, server.activeIds);
					
					//make saving to log a synchronized action.
					server.log.save();
					
					//resend the scout everytime we save a incoming connection
					//server.leader.createScout();

				//code for getting chatlogs. change to better words if you want. Works.
				} else if (inputData.contains("Chatlog")) {
					// System.out.println("received Chatlog message");
					//toServer.println("reply~" + server.chatLog);
					toServer.println("reply_" + server.replica.tempGetDecisions());
				} else if (inputData.contains("reply")) {
					//TODO: Make this part better.
					
					if(server.recoveryDone == false){
						server.debugPrint("Recived decisions from others for recovery");
						server.recoveryDone = true;
						String[] temp = inputData.split("_");
						// System.out.println(temp2[1]);
						// if (temp2.equals("null")) {
						// 	server.chatLog = null;
						// } else {
						// 	server.chatLog = temp2[1];
						// }
						System.out.println("received decisions to update: " + temp[1]);
						
						server.replica.setDecisions(parseDecisions(temp[1]));
					}
					

					
				}
				else {
					//data from a server has arrived.
					String[] input = inputData.split("_");
					int serverId = -1;
					int ballotNum = -1;
					int commanderId = -1;
					int slotNum;
					ArrayList<Pvalue> pvalues;
					Pvalue pvalue;
					Command newCommand;

					switch (input[0]) {
						case "continue":
							//server.leader.hold = false;
							server.debugPrint("server" + server.serverId + " received continue");
							server.leader.createScout();
							break;
						case "p1a":
							// System.out.println("[Recived P1a]  from Scouts");
							serverId = Integer.parseInt(input[1]);
							ballotNum = Integer.parseInt(input[2]);

							server.acceptor.p1a(serverId, ballotNum);

							break;

						case "p1b":
							// System.out.println("[Recived P1b] from other servers");
							serverId = Integer.parseInt(input[1]);
							ballotNum = Integer.parseInt(input[2]);
							pvalues = null; // parsePvalue(input[3]);
							// System.out.println("Gonna Call scout p1b");

							server.debugPrint("server" + server.serverId + " received p1b response from " + serverId);

							server.leader.getScout().p1b(serverId, ballotNum, pvalues);

							break;

						case "p2a":
							// System.out.println("[Recived P2a] from commanders");
							// System.out.println(inputData);
							serverId = Integer.parseInt(input[1]);
							pvalues = parsePvalue(input[2]);
							pvalue = pvalues.get(0); // there is only one pvalue
							commanderId = Integer.parseInt(input[3]);

							server.acceptor.p2a(serverId, pvalue, commanderId);

							break;

						case "p2b":
							System.out.println(inputData);
							serverId = Integer.parseInt(input[1]); // of no use
							ballotNum = Integer.parseInt(input[2]);
							commanderId = Integer.parseInt(input[3]);

							server.debugPrint("server" + server.serverId + " received p2b response from " + serverId);

							if (server.leader.commanders.get(commanderId) == null)
								server.debugPrint("server" + server.serverId + " has commanders null pointer Exception");

							server.leader.commanders.get(commanderId).p2b(serverId, ballotNum);
							break;
						case "decision":
							// System.out.println("[Save Recived decision] from others");
							slotNum = Integer.parseInt(input[1]);

							newCommand = parseCommand(input[2]);
							server.replica.decide(slotNum, newCommand, false);
							break;
						case "isProposing?":
							serverId = Integer.parseInt(input[1]);
							System.out.println("server.isProposing: " + server.isProposing);
							if(!server.isProposing){
								try{
									System.out.println("sending continue to server: " +serverId);
									//server.send(serverId, "continue");
									server.send(serverId, "continue");
								}catch(Exception e){
									e.printStackTrace();
								}
								
							} else {
								//save serverId to a list
								server.debugPrint("server" + server.serverId + " added server" + serverId + " to to-send list");
								server.toSendContinue.add(serverId);
							}
							break;
					}
				}
			}
		}
	}

	private ArrayList<Pvalue> parsePvalue (String pvalueListStr) {
		server.debugPrint("server" + server.serverId + " beginning parsePvalue");

		ArrayList<Pvalue> pvalues = new ArrayList<Pvalue>();

		//when no pvalues are being sent in message
		if (pvalueListStr.length() == 2) {
			pvalues = null;
			server.debugPrint("server" + server.serverId + " finished parsePvalue");
			return pvalues;
		}
		
		//remove "[" and "]"
		String purePvalueListStr = pvalueListStr.substring(1, pvalueListStr.length() - 1);
		// System.out.println(purePvalueListStr);
		String[] entries = purePvalueListStr.split(", ");

		for (String entry : entries) {
			String[] elements = entry.split("\\|");
			// System.out.println("------------\n");
			// for(int i =0; i<elements.length; i++){
			// 	System.out.println(elements[i]);
			// }
			int b = Integer.parseInt(elements[0]);
			int slotNum = Integer.parseInt(elements[1]);
			int cid = Integer.parseInt(elements[2]);
			Command c = new Command(cid, elements[3]);

			pvalues.add(new Pvalue(b, slotNum, c));
		}
		if(pvalues.size() == 0){
			pvalues = null;
		}
		server.debugPrint("server" + server.serverId + " finished parsePvalue");

		return pvalues;
	}

	private Command parseCommand(String input){
		String elements[] = input.split("\\|");
		int id = Integer.parseInt(elements[0]);
		return new Command(id, elements[1]);
	}

	private HashMap<Integer, Command> parseDecisions(String input){
		HashMap<Integer, Command> map = new HashMap<Integer, Command>();
		String pureInput = input.substring(1, input.length()-1);
		String[] decisionsStr = pureInput.split(", ");

		for(String decisionStr : decisionsStr) {
			String[] keyValueStr = decisionStr.split("=");
			String[] commandStr = keyValueStr[1].split("\\|");
			int key = Integer.parseInt(keyValueStr[0]);
			int cid = Integer.parseInt(commandStr[0]);

			map.put(key, new Command(cid, commandStr[1]));
		}

		return map;
	}
}




