/** This class holds the structure of a command.
  * A command consists of:
  *   a command identifier (cid), and
  *   an operation (operation), which actually is a string of chat message.
  */

public class Command {
	public int cid;
	public String operation;

	public Command(int cid, String o) {
		this.cid = cid;
		operation = o;
	}

	/** Converts a command to a printable string. */
	public String toString() {
		return "" + cid + "|" + operation;
	}

	@Override
	public int hashCode() {
		return cid;
	}

	@Override
	public boolean equals(Object c) {
		return cid == ((Command)c).cid;
	}
}