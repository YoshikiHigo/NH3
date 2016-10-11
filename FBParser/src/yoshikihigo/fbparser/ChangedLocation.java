package yoshikihigo.fbparser;

public class ChangedLocation {

	final public Location before;
	final public Location after;
	final public boolean bugfix;

	public ChangedLocation(final Location before, final Location after,
			final boolean bugfix) {
		this.before = before;
		this.after = after;
		this.bugfix = bugfix;
	}
}
