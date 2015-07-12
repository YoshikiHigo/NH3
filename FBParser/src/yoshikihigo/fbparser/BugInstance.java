package yoshikihigo.fbparser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BugInstance {

	final public String type;
	final public int priority;
	final public int rank;
	final public String abbrev;
	final public String category;

	final private List<SourceLine> classLocations;
	final private List<SourceLine> methodLocations;
	final private List<SourceLine> fieldLocations;
	final private List<SourceLine> localVariableLocations;

	final private List<SourceLine> sourcelines;

	public BugInstance(final String type, final int priority, final int rank,
			final String abbrev, final String category) {
		this.type = type;
		this.priority = priority;
		this.rank = rank;
		this.abbrev = abbrev;
		this.category = category;
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

	static public class LocationComparator implements Comparator<BugInstance> {

		@Override
		public int compare(final BugInstance o1, final BugInstance o2) {

			final int typeComparison = o1.type.compareTo(o2.type);
			if (0 != typeComparison) {
				return typeComparison;
			}

			final int classComparison = o1.getClassLocations().get(0)
					.compareTo(o2.getClassLocations().get(0));
			if (0 != classComparison) {
				return classComparison;
			}
			
			// final int classComparison = this.compareSortedSet(
			// o1.getClassLocations(), o2.getClassLocations());
			// if (0 != classComparison) {
			// return classComparison;
			// }

			// final int methodComparison = this.compareSortedSet(
			// o1.getMethodLocations(), o2.getMethodLocations());
			// if (0 != methodComparison) {
			// return methodComparison;
			// }
			//
			// final int fieldComparison = this.compareSortedSet(
			// o1.getFieldLocations(), o2.getFieldLocations());
			// if (0 != fieldComparison) {
			// return fieldComparison;
			// }
			//
			// final int localvariableComparison = this.compareSortedSet(
			// o1.getLocalVariableLocations(),
			// o2.getLocalVariableLocations());
			// if (0 != localvariableComparison) {
			// return localvariableComparison;
			// }

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
		text.append(this.type);
		text.append(", priority: ");
		text.append(Integer.toString(this.priority));
		text.append(", rank: ");
		text.append(Integer.toString(this.rank));
		text.append(", abbrev: ");
		text.append(this.abbrev);
		text.append(", category: ");
		text.append(this.category);
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
