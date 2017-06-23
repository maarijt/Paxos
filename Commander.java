/** This class implements commander.
  * The server (leader) will call p2b() if certain messages are received.
  * The commander will call send method in server to send any messages.
  */

public class Commander {
	private int id;
	private Server server;
	private Pvalue pvalue;
	private int counter;
	private int thresh;
	private boolean hasDecided;
	private boolean dead;

	public Commander(int i, Server s, Pvalue p) {
		id = i;
		server = s;
		pvalue = p;
		counter = 0;
		thresh = s.numServers / 2 + 1;
		hasDecided = false;
		dead = false;
	}

	/** Broadcast p2a messages. */
	public void sendP2a() {
		String message = "p2a_" + server.serverId + "_[" + pvalue + "]_" + id;
		System.out.println("Commander (serverId " + server.serverId + ")sending p2a: "+message);
		try {
			if(server.leader.crashP2a){
				server.sendToSubset(server.leader.subsetProcesses, message);
			}else{
				server.sendAll(message);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	/** Response to p2b messages. */
	public void p2b(int serverId, int b) {
		if (dead)
			return;

		System.out.println("In commander p2b method");
		int selfVote = 0;

		if (pvalue.b < server.acceptor.ballotNum) {
			dead = true;
			server.leader.preempted(server.acceptor.ballotNum, serverId);
			return;
		}

		if (pvalue.b >= server.acceptor.ballotNum) {
			selfVote = 1;
			server.acceptor.ballotNum = pvalue.b;
		}

		if (b == pvalue.b) {
			System.out.println("received p2b from acceptor");
			server.debugPrint("server" + server.serverId + " get a p2b vote from server" + serverId);
			counter += 1;
			server.debugPrint("server" + server.serverId + "'s numVotes: " + (counter+selfVote));

			if (counter >= thresh - selfVote && !hasDecided) {
				hasDecided = true;
				dead = true;
				String message = "decision_" + pvalue.s + "_" + pvalue.c;

				System.out.println("[Decision Made] "+message);

				server.debugPrint("server" + server.serverId + " made a decision on message " + pvalue.c + " and about to broadcast");

				server.replica.decide(pvalue.s, pvalue.c, true);

				try {
					if(server.leader.crashDecision){
						server.sendToSubset(server.leader.subsetProcesses, message);
						System.exit(1);
					}else{
						server.sendAll(message);
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
		}
		else if(pvalue.b < b){
			dead = true;
			System.out.println("Commander preempted happend");
			server.leader.preempted(b, serverId);
		}
	}
}