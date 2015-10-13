package yoshikihigo.fbparser;

public class ChangedLocation {

	final public Location before;
	final public Location after;

	public ChangedLocation(final Location before, final Location after) {
		this.before = before;
		this.after = after;
	}
}
