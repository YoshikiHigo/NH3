package yoshikihigo.fbparser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BugInstance {

	final public BugPattern pattrn;

	final private List<SourceLine> classLocations;
	final private List<SourceLine> methodLocations;
	final private List<SourceLine> fieldLocations;
	final private List<SourceLine> localVariableLocations;

	final private List<SourceLine> sourcelines;

	public BugInstance(final BugPattern pattern) {
		this.pattrn = pattern;
		this.classLocations = new ArrayList<>();
		this.methodLocations = new ArrayList<>();
		this.fieldLocations = new ArrayList<>();
		this.localVariableLocations = new ArrayList<>();
		this.sourcelines = new ArrayList<>();
	}

	public void addSourceLine(final SourceLine sourceline) {
		this.sourcelines.add(sourceline);
	}

	public List<SourceLine> getSourceLines() {
		return new ArrayList<SourceLine>(this.sourcelines);
	}

	public void addClassLocation(final SourceLine sourceline) {
		this.classLocations.add(sourceline);
	}

	public List<SourceLine> getClassLocations() {
		return this.classLocations;
	}

	public void addMethodLocation(final SourceLine sourceline) {
		this.methodLocations.add(sourceline);
	}

	public List<SourceLine> getMethodLocations() {
		return this.methodLocations;
	}

	public void addFieldLocation(final SourceLine sourceline) {
		this.fieldLocations.add(sourceline);
	}

	public List<SourceLine> getFieldLocations() {
		return this.fieldLocations;
	}

	public void addLocalVariableLocation(final SourceLine sourceline) {
		this.localVariableLocations.add(sourceline);
	}

	public List<SourceLine> getLocalVariableLocations() {
		return this.localVariableLocations;
	}

	static public class RankLocationTypeComparator implements
			Comparator<BugInstance> {

		@Override
		public int compare(final BugInstance o1, final BugInstance o2) {

			final int rankComparison = Integer.valueOf(o1.pattrn.rank)
					.compareTo(o2.pattrn.rank);
			if (0 != rankComparison) {
				return rankComparison;
			}

			final int classComparison = o1.getClassLocations().get(0)
					.compareTo(o2.getClassLocations().get(0));
			if (0 != classComparison) {
				return classComparison;
			}

			final int typeComparison = o1.pattrn.type.compareTo(o2.pattrn.type);
			if (0 != typeComparison) {
				return typeComparison;
			}

			return 0;
		}
	}

	static public class LocationTypeComparator implements
			Comparator<BugInstance> {

		@Override
		public int compare(final BugInstance o1, final BugInstance o2) {

			final int classComparison = o1.getClassLocations().get(0)
					.compareTo(o2.getClassLocations().get(0));
			if (0 != classComparison) {
				return classComparison;
			}

			final int typeComparison = o1.pattrn.type.compareTo(o2.pattrn.type);
			if (0 != typeComparison) {
				return typeComparison;
			}

			return 0;
		}

		private int compareSortedSet(final List<SourceLine> s1,
				final List<SourceLine> s2) {

			for (int index = 0; true; index++) {

				if ((s1.size() == index) && (s2.size() == index)) {
					return 0;
				}

				else if ((s1.size() == index) && (s2.size() > index)) {
					return -1;
				}

				else if ((s1.size() > index) && (s2.size() == index)) {
					return 1;
				}

				else {
					int comparison = s1.get(index).compareTo(s2.get(index));
					if (0 != comparison) {
						return comparison;
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		final StringBuilder text = new StringBuilder();
		text.append("[BugInstance] type: ");
		text.append(this.pattrn.type);
		text.append(", priority: ");
		text.append(Integer.toString(this.pattrn.priority));
		text.append(", rank: ");
		text.append(Integer.toString(this.pattrn.rank));
		text.append(", category: ");
		text.append(this.pattrn.category);
		text.append(System.lineSeparator());
		for (final SourceLine sourceline : this.classLocations) {
			final String sourcelineText = SourceLine.makeText("class",
					sourceline);
			text.append(sourcelineText);
			text.append(System.lineSeparator());
		}
		for (final SourceLine sourceline : this.methodLocations) {
			final String sourcelineText = SourceLine.makeText("method",
					sourceline);
			text.append(sourcelineText);
			text.append(System.lineSeparator());
		}
		for (final SourceLine sourceline : this.fieldLocations) {
			final String sourcelineText = SourceLine.makeText("field",
					sourceline);
			text.append(sourcelineText);
			text.append(System.lineSeparator());
		}
		for (final SourceLine sourceline : this.localVariableLocations) {
			final String sourcelineText = SourceLine.makeText("localvariable",
					sourceline);
			text.append(sourcelineText);
			text.append(System.lineSeparator());
		}
		return text.toString();
	}
}
