/** This class holds the structure of a proposal.
  * A proposal consists of:
  *   a slot number (slotNum), and
  *   a command (command).
  */

public class Proposal {
	public int slotNum;
	public Command command;

	public Proposal(int s, Command c) {
		slotNum = s;
		command = c;
	}
}