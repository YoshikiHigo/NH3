package yoshikihigo.fbparser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BugInstance {

	final public BugPattern pattern;
	final public int rank;
	final public int priority;
	final public String hash;

	final private List<SourceLine> sourcelines;

	public BugInstance(final BugPattern pattern, final int rank,
			final int priority, final String hash) {
		this.pattern = pattern;
		this.rank = rank;
		this.priority = priority;
		this.hash = hash;
		this.sourcelines = new ArrayList<>();
	}

	public void addSourceLine(final SourceLine sourceline) {
		this.sourcelines.add(sourceline);
	}

	public List<SourceLine> getSourceLines() {
		return new ArrayList<SourceLine>(this.sourcelines);
	}

	@Override
	public int hashCode() {
		return this.hash.hashCode();
	}

	@Override
	public boolean equals(final Object o) {

		if (!(o instanceof BugInstance)) {
			return false;
		}

		final BugInstance target = (BugInstance) o;
		return this.hash.equals(target.hash);
	}

	static public class RankLocationTypeComparator implements
			Comparator<BugInstance> {

		@Override
		public int compare(final BugInstance o1, final BugInstance o2) {

			final int rankComparison = Integer.valueOf(o1.rank).compareTo(
					o2.rank);
			if (0 != rankComparison) {
				return rankComparison;
			}

			final int classComparison = o1.getSourceLines().get(0)
					.compareTo(o2.getSourceLines().get(0));
			if (0 != classComparison) {
				return classComparison;
			}

			final int typeComparison = o1.pattern.type
					.compareTo(o2.pattern.type);
			if (0 != typeComparison) {
				return typeComparison;
			}

			return 0;
		}
	}

	@Override
	public String toString() {
		final StringBuilder text = new StringBuilder();
		text.append("[BugInstance] type: ");
		text.append(this.pattern.type);
		text.append(", rank: ");
		text.append(Integer.toString(this.rank));
		text.append(", priority: ");
		text.append(Integer.toString(this.priority));
		text.append(", category: ");
		text.append(this.pattern.category);
		text.append(System.lineSeparator());
		for (final SourceLine sourceline : this.sourcelines) {
			text.append(sourceline.toString());
			text.append(System.lineSeparator());
		}
		return text.toString();
	}
}
