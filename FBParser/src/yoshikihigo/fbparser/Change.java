package yoshikihigo.fbparser;

public class Change {

	final public Range before;
	final public Range after;

	public Change(final Range before, final Range after) {
		this.before = before;
		this.after = after;
	}
}
