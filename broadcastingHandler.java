import java.io.*;
import java.net.*;
import java.util.*;

public class broadcastingHandler implements Runnable {
	private Server server;

	public broadcastingHandler(Server _server) throws Exception {
		
		server = _server;		
	}

	public void run() {
		while(true){
			System.out.println("in broadcastingHandler");
			try{
				Thread.sleep(2000);
			}catch(Exception e){

			}
			if(server.serverSockets.values().size() > 0){
				for(Socket curSocket : server.serverSockets.values()){
					if(curSocket!=null){
						try{											
							PrintWriter toServer = new PrintWriter(curSocket.getOutputStream(), true);
							toServer.println("Aloha from server "+server.serverId);
						}catch(Exception e){
							//
						}
						
					}
				}
			}
			
			// for(int i = 0; i<server.connServers.size(); i++){
			
			// 	if(server.connServers.get(i)!=null){
			// 		try{
						
						
			// 			Socket tempSocket = server.connServers.get(i);
			// 			PrintWriter toServer = new PrintWriter(tempSocket.getOutputStream(), true);
			// 			toServer.println("Hello again from server "+server.serverId);
			// 		}catch(Exception e){
			// 			//
			// 		}
					
			// 	}
			// }
		}
		
	}
}