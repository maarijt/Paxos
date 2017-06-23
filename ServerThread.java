import java.io.*;
import java.net.*;
import java.util.*;

public class ServerThread implements Runnable {
	public Server server;
	public ServerSocket servConn;
	public int port;
	public Scanner fromServer;
	public PrintWriter toServer;

	//This thread listens for incoming connections from the servers. Once it
	//gets a connection, it starts a server handler for that server. 
	ServerThread(Server _server, int _port) {
		server = _server;
		port = _port;
		
	}


	public void run() {
		try {
			servConn = new ServerSocket();
			servConn.setReuseAddress(true);
			servConn.bind(new InetSocketAddress(port));
			server.localServerSocket = servConn;
			while(true) {
				Socket connServer = servConn.accept();
				server.connServers.add(connServer);
				
				PrintWriter pw = new PrintWriter(connServer.getOutputStream(), true);
				pw.println("Server "+server.serverId+" get your connection");
				System.out.println("Connection accepted from: " + connServer.getLocalPort());
				// System.out.println("arraylist size before: " + server.connServers.size());
				// // server.connServers.add(connServer);
				// server.incoming.add(connServer);
				// System.out.println("arraylist size after: " + server.connServers.size());
				Thread serverHandler = new Thread(new ServerHandler(connServer, server));
				serverHandler.start();
				

			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}