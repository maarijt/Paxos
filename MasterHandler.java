import java.io.*;
import java.net.*;
import java.util.*;


public class MasterHandler implements Runnable {

	public Socket masterSocket;// socket that listening to other servers
	public Server server;// Methods and objects within server obj will be used according to message received
	public Scanner fromMaster;
	public PrintWriter toMaster;

	public MasterHandler(Socket _masterSocket, Server _server) throws Exception{
		masterSocket = _masterSocket;
		server = _server;
		fromMaster = new Scanner(masterSocket.getInputStream());
		toMaster = new PrintWriter(masterSocket.getOutputStream(), true);

	}

	public void run(){
		while(true) {
			if (fromMaster.hasNext()) {
				//System.out.println("___"+server.crashAfterP1b_loop);
				String command = fromMaster.nextLine();
				try{
					server.masterHandler(command, toMaster);
				}catch(Exception e){
					e.printStackTrace();
				}
				
			}

		}
	}

	public void sendToMaster(String ackStr){
		try{
			toMaster.println(ackStr);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}