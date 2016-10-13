package yoshikihigo.fbparser;

public class SourceLine implements Comparable<SourceLine> {

	final public String classname;
	final public int start;
	final public int end;
	final public String sourcepath;
	final public String name;

	public SourceLine(final String classname, final int start, final int end,
			final String sourcepath, final String name) {
		this.classname = classname;
		this.start = start;
		this.end = end;
		this.sourcepath = sourcepath;
		this.name = name;
	}

	@Override
	public int compareTo(final SourceLine target) {

		final int classnameComparison = this.classname
				.compareTo(target.classname);
		if (0 != classnameComparison) {
			return classnameComparison;
		}

		else if (this.start < target.start) {
			return -1;
		} else if (this.start > target.start) {
			return 1;
		}

		else if (this.end < target.end) {
			return -1;
		} else if (this.end > target.end) {
			return 1;
		}

		else {
			return 0;
		}
	}

	static public String makeText(final String element,
			final SourceLine sourceline) {
		final StringBuilder text = new StringBuilder();
		text.append("  ");
		text.append(element);
		text.append(": ");
		text.append(sourceline.name);
		if (0 < sourceline.start) {
			text.append(", ");
			text.append(Integer.toString(sourceline.start));
		}
		if (0 < sourceline.end) {
			text.append("--");
			text.append(Integer.toString(sourceline.end));
		}
		return text.toString();
	}

}
