package yoshikihigo.fbparser;

public class Range {

	final public String path;
	final public int startLine;
	final public int endLine;

	public Range(final String path, final int startLine, final int endLine) {
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
}
