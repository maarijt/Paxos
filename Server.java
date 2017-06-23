import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Server represents a process start by master, and contains all the data utilized by all diff threads
 */
public class Server{
	
	public static final int STARTING_PORT = 20000;

	/*Basic Server Info*/
	public static int serverId;//index of server = id of that server. 
	public static int numServers;//the total number of servers
	public static int port;

	public static Replica replica;
	public static Leader leader;
	public static Acceptor acceptor;
	public static Socket masterSocket;
	public static ServerSocket masterServerSocket;
	public static ArrayList<Socket> connServers;
	public static ArrayList<Integer> activeIds;
	public static boolean isRecovering;
	public static HashMap<Integer, Socket> serverSockets;
	public static Log log;
	public static String chatLog;

	//handle crash
	public  boolean crashAfterP1b;
	public  boolean crashAfterP1b_loop;
	public  boolean crashAfterP2b;
	public  boolean crashAfterP2b_loop;
	public boolean crash;

	public boolean crashP1a;
    public boolean crashP1a_loop;
	public Thread masterHandlerThread;
	public boolean isProposing;
	public ArrayList<Integer> toSendContinue;

	public ServerSocket localServerSocket;

	private File debugLog;
	private FileOutputStream fop;
	public boolean recoveryDone;

	public Server(int id, int n, int port){
		this.serverId = id;
		this.numServers = n;
		this.port = port;
		connServers = new ArrayList<Socket>();
		serverSockets = new HashMap<Integer, Socket>();
		replica = new Replica(this);
		leader = new Leader(this);
		acceptor = new Acceptor(this);
		log = new Log();
		activeIds = new ArrayList<Integer>();
		crashAfterP2b = false;
		crashAfterP1b = false;
		crashAfterP2b_loop = false;
		crashAfterP1b_loop = false;
		crash = false;

		crashP1a = false;
		crashP1a_loop = false;

		isProposing = false;
		toSendContinue = new ArrayList<Integer>();
		recoveryDone = false;


	}

	public static void recoveryProtocol(Server server) {
		//This function saves the complete server object in a log file.
		//Check log file for more info.
		try {
			System.out.println(server.serverId);
      		File logfile = new File("logs/log_" + Integer.toString(server.serverId) + ".ser");
	    	if (logfile.exists()) {
	    		//server is recovering.
	    		server.debugPrint("########Server"+server.serverId+" recovers");
	    		server.isRecovering = true;
	        	log.load(server.serverId);
		       	serverId = log.serverId;
		       	activeIds = log.activeIds;
	      	} else if (logfile.createNewFile()){
	      		//server started fresh.
	      		server.isRecovering = false;
	        	System.out.println("Created logfile " + logfile.getPath());
	        	server.log = new Log(server.serverId, server.activeIds);
	        	server.log.save();
	      	} else {
	      		//lol something weird happened. Tough luck bro.
	        	System.out.println("Error creating logfile " + logfile.getPath());
	      	}
	    	} catch (Exception e) {
	      		System.out.println("Found no logfile.");
	      		e.printStackTrace();
	    }
	}
	/**/
	public static void main(String[] args){
		//TODO: read a value in txt file to check whether recovering or not, also change it accordingly
		//TODONE. The function recoverProtocol does this.



		int id = Integer.parseInt(args[0]);
		int n = Integer.parseInt(args[1]);
		int port = Integer.parseInt(args[2]);

		//Initialize server
		Server server = new Server(id, n, port);


		// A thread for broadcasting info[test purpose]
		
		// try{
		// 	Thread broadcastingThread = new Thread(new broadcastingHandler(server));
		// 	broadcastingThread.start();
		// }catch(Exception e){
		// 	e.printStackTrace();
		// }
		


		//Socket for Master: bind the port for communication with
		try{
			//time to start a thread responsible for server to server communication.
			//starts a serversocket at startingport+id so if id is 0, starts at 20000 and so on.
			System.out.println("Starting serverthread at: " + (STARTING_PORT+id) +" listening to other servers" );
			
			
			//server socket to connect to master. streams handled
			server.masterServerSocket = new ServerSocket(server.port);
			masterSocket = server.masterServerSocket.accept();
			

			Thread masterHandler  = new Thread(new MasterHandler(masterSocket, server));
			masterHandler.start();
			server.masterHandlerThread = masterHandler;
			//Scanner inFromClient = new Scanner(masterSocket.getInputStream());
			//PrintWriter toClient = new PrintWriter(masterSocket.getOutputStream(), true);

			//check if recovering. if so, load values of chatlog and stuff from there.
			

			//server socket to communicate with other servers
			recoveryProtocol(server);
			Thread serverthread = new Thread(new ServerThread(server, STARTING_PORT+id));
			serverthread.start();

			
			
			//Build outgoing tcp sockets: check if other servers up and connect to them.
			buildOutgoingConnection(server);

			//scout should be initilaized after the outgoing connections have been built
			// if(server.serverSockets.size() == server.numServers-1){
			// 	System.out.println("******build scout here");
			// 	server.leader.createScout();
			// }
			

			//Check if recovering. if so, send chatlog message. ServerHandler handles this part.
			if (server.isRecovering) {
				server.debugPrint("Sending chatLog to all alive serves");
				sendAll("Chatlog");
			}
			
			while(true){
				if(server.crash || server.crashAfterP2b_loop || server.crashAfterP1b_loop || server.crashP1a_loop){
					server.debugPrint("########Server"+server.serverId+" crash");
					server.localServerSocket.close();
					masterServerSocket.close();
					masterSocket.close();
					System.exit(1);
				}
				//System.out.println("impossiblesssss");
				// try{
				// 	Thread.sleep(10);
				// }catch(Exception e){

				// }
				
			}

			// //System.exit(1);
			


		} catch(Exception e) {
			e.printStackTrace();
		}
	} 


	public static void buildOutgoingConnection(Server server){
		try{
			if(!server.isRecovering){
				System.out.println("here");
				for (int conns = 0; conns < server.serverId; conns++) {
					System.out.println("Attempting to connect to: " + (STARTING_PORT+conns));
					try{
						Socket otherServer = new Socket(InetAddress.getLocalHost(), STARTING_PORT+conns);
					
					server.connServers.add(otherServer);
					server.serverSockets.put(conns, otherServer);
					activeIds.add(conns);
					log = new Log(server.serverId, server.activeIds);
					log.save();

					//add a server handler for this socket to receive data from other servers
					Thread serverHandler = new Thread(new ServerHandler(otherServer, server));
					serverHandler.start();

					}catch(Exception e){
						e.printStackTrace();
						System.out.println("XXxxxxxxxxxxxxxx");
					}
				}
			}else{
				System.out.println("Recovering. " + activeIds.size());
				for (Integer i : activeIds) {
					System.out.println("Just recovered. Attempting to connect to: " + (STARTING_PORT+i));
					Socket otherServer = new Socket(InetAddress.getLocalHost(), STARTING_PORT+i);
					//server.connServers.add(otherServer);
					serverSockets.put(i, otherServer);
					
					Thread serverHandler = new Thread(new ServerHandler(otherServer, server));
					serverHandler.start();
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	/**
	 * [sendToSubset description]
	 * @param subset [description]
	 */
	public void sendToSubset(ArrayList<Integer> subset, String command) throws Exception{
		debugPrint("Server"+serverId+" in sendToSubset function");
		for(int i = 0; i < subset.size(); i++){
			if(serverSockets.containsKey(subset.get(i))){
				Socket destSocket  = serverSockets.get(subset.get(i));
				PrintWriter toServer = new PrintWriter(destSocket.getOutputStream(), true);
		 		toServer.println(command);
			}
		}
		// crash = true;
		//debugPrint("########Server"+serverId+" crash");
		crashP1a_loop = true;
		// localServerSocket.close();
		// masterServerSocket.close();
		// masterSocket.close();
		// System.exit(1);
	}




	public static void sendAll(String command) throws Exception{

		for(int i = 0; i < numServers; i++){
			if(serverSockets.containsKey(i)){
				Socket destSocket  = serverSockets.get(i);
				PrintWriter toServer = new PrintWriter(destSocket.getOutputStream(), true);
		 		toServer.println(command);
			}
		}
	}
	public static void send(int id, String command) throws Exception{
		//Socket servertoSend = connServers.get(id);
		try{
			Socket servertoSend = serverSockets.get(id);
			PrintWriter toServer = new PrintWriter(servertoSend.getOutputStream(), true);
			//System.out.println(command);
			toServer.println(command);
		}catch(Exception e){
			System.out.println("socket has been interruped");
			serverSockets.remove(id);
		}
	}


	
	public static void sendToMaster(String outPut) throws Exception{
		PrintWriter pw = new PrintWriter(masterSocket.getOutputStream(), true);
		pw.println(outPut);
	}
	/**
	 * Handles the message from master/client
	 * @param  inputData [description]
	 * @param  pw        [description]
	 * @throws Exception [description]
	 */
	public  void masterHandler(String inputData, PrintWriter pw) throws Exception{
		System.out.println(inputData);
		debugPrint("!!!Server"+serverId+" received input from master: "+inputData);
		String[] input = inputData.split("\\s+");
		
		if(inputData.indexOf("crash")>=0){			
			// if(input.length > 1){
			// 	//handle crash command: crashP1a id id...,  crashP2a id id... and crashDecision id id...
			// 	switch (input[0]){
			// 		case:
			// 	}

			// }else{
				//handle crash command: crash, crashAfterP1b, crashAfterP2b
				switch (input[0]){
					case "crash":
						this.crash = true;
						//System.exit(1);
						break;
					case "crashAfterP1b":
						this.crashAfterP1b = true;
						System.out.println(crashAfterP1b);
						break;
					case "crashAfterP2b":
						this.crashAfterP2b = true;
						System.out.println(crashAfterP2b);
						break;
					case "crashP1a":
						//set flag to true
						this.crashP1a = true;
						this.leader.subsetProcesses = parseCrashProcessesArray(inputData);
						//set subset to use
						debugPrint("*********crashP1a received");

						
						break;
					case "crashP2a":
						//set flag to true
						this.leader.crashP2a = true;
						//set subset to use
						debugPrint("*********crashP2a received");
						this.leader.subsetProcesses = parseCrashProcessesArray(inputData);
						break;
					case "crashDecision":
						//set flag to true
						this.leader.crashDecision = true;
						//set subset to use
						this.leader.subsetProcesses = parseCrashProcessesArray(inputData);
						debugPrint("*********crashDecision received");
						break;
					default:
						System.out.println("unrecognized crash");
				}
			//}
		}else{
			int messageId = -1;
			String message;
			switch (input[0]){
				case "msg":
					//call replica or leader to deal with request
					messageId = Integer.parseInt(input[1]);
					message = input[2];
					debugPrint("MSG "+inputData);
					replica.propose(new Command(messageId, message));

					break;
				case "get":
					//get local chat log, each message separated by comma
					sendToMaster(replica.getChatLog());

					break;
				default:
					throw new Exception("Invalid data input: " + inputData);
     
			}
		}
			
		//pw.println("ack 0\n");
	}
	public ArrayList<Integer> parseCrashProcessesArray(String inputData){
		String[] input = inputData.split("\\s+");
		ArrayList<Integer> processesSet = new ArrayList<Integer>();
		for(int i = 1; i<input.length; i++){
			processesSet.add(Integer.parseInt(input[i]));
		}
		return processesSet;
	}
	public void debugPrint(String s) {
		try(FileWriter fw = new FileWriter("debugLog", true);
		    BufferedWriter bw = new BufferedWriter(fw);
		    PrintWriter out = new PrintWriter(bw))
		{
		    out.println(s);
		} catch (Exception e) {
		    System.out.println(e.getMessage());
		}
	}
}
