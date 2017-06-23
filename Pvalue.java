/** This class holds the structure of a pvalue.
  * A pvalue consists of:
  *   a ballot number (b),
  *   a slot number (s) and
  *   a proposal (c), which actually is a command.
  */

public class Pvalue {
	public int b;
	public int s;
	public Command c;

	public Pvalue(int b, int s, Command c) {
		this.b = b;
		this.s = s;
		this.c = c;
	}

	public String toString() {
		return "" + b + "|" + s + "|" + c;
	}
}