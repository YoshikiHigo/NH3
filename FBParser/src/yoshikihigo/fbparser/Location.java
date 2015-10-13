package yoshikihigo.fbparser;

public class Location {

	final public String path;
	final public int startLine;
	final public int endLine;

	public Location(final String path, final int startLine, final int endLine) {
		this.path = path;
		this.startLine = startLine;
		this.endLine = endLine;
	}

	public boolean hasLineInformaltion() {
		return (0 < this.startLine) && (0 < this.endLine);
	}

	public String getLineRangeText() {
		if (this.hasLineInformaltion()) {
			return this.startLine + "--" + this.endLine;
		} else {
			return "no-line-information";
		}
	}

	@Override
	public boolean equals(final Object o) {

		if (!(o instanceof Location)) {
			return false;
		}

		final Location target = (Location) o;
		return this.path.equals(target.path)
				&& (this.startLine == target.startLine)
				&& (this.endLine == target.endLine);
	}

	@Override
	public int hashCode() {
		return this.path.hashCode() + this.startLine + this.endLine;
	}
}
