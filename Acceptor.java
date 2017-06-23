/** This class implements acceptor.
  * The server will call p1a() and p2a() accordingly if certain messages are received.
  * The acceptor will call send method in server to send any messages.
  */

import java.util.List;
import java.util.ArrayList;

public class Acceptor {
	private Server server;
	public int ballotNum;
	private List<Pvalue> accepted;

	public Acceptor(Server s) {
		server = s;
		ballotNum = Integer.MIN_VALUE;
		accepted = new ArrayList<Pvalue>();
	}

	/** Response to p1a messages. */
	public synchronized void p1a(int sId, int b) {
		ballotNum = Math.max(ballotNum, b);

		String message = "p1b_" + server.serverId + "_" + ballotNum +"_" + accepted;
		server.debugPrint("server" + server.serverId + " sending p1b to " + sId + " with ballotNum:" + ballotNum);
		server.debugPrint("server" + server.serverId + " sending p1b to " + sId + " with accepted:" + accepted);
		
		// System.out.println("in acceptor p1a");

		try {
			server.send(sId, message);	// pseudo-send
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		//System.out.println("value of crashAfterP1b in p1a "+server.crashAfterP1b);
		if(server.crashAfterP1b){
			System.out.println("***Have received crashAfterP1b***, changing value of crashAfterP1b_loop");
			server.crashAfterP1b_loop = true;
		}
	}

	/** Response to p2a messages. */
	public synchronized void p2a(int sId, Pvalue p, int commanderId) {
		if (p.b >= ballotNum) {
			ballotNum = p.b;
			accepted.add(p);
		}

		String message = "p2b_" + server.serverId + "_" + ballotNum + "_" + commanderId;
		server.debugPrint("server" + server.serverId + " sending p2b to " + sId + " with ballotNum:" + ballotNum);

		try {
			server.send(sId, message);	// pseudo-send
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		if(server.crashAfterP2b){
			System.out.println("***received crashAfterP2b***, changing value of crashAfterP2b_loop");
			server.crashAfterP2b_loop = true;
		}
	}
}