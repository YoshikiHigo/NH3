package yoshikihigo.fbparser;

import java.util.SortedSet;
import java.util.TreeSet;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class ClassStats implements Comparable<ClassStats> {

	static public SortedSet<String> getClassNames(
			final SortedSet<ClassStats> classes) {
		final SortedSet<String> names = new TreeSet<>();
		for (final ClassStats c : classes) {
			names.add(c.classname);
		}
		return names;
	}

	static public ClassStats makeClassStats(final Node node) {
		if (node.getNodeName().equals("ClassStats")) {
			final NamedNodeMap nnMap = node.getAttributes();
			final String classname = nnMap.getNamedItem("class").getNodeValue();
			final int bugs = Integer.parseInt(nnMap.getNamedItem("bugs")
					.getNodeValue());
			return new ClassStats(classname, bugs);
		}
		return null;
	}

	final public String classname;
	final public int bugs;

	private ClassStats(final String classname, final int bugs) {
		this.classname = classname;
		this.bugs = bugs;
	}

	@Override
	public int compareTo(final ClassStats target) {
		return this.classname.compareTo(target.classname);
	}

	@Override
	public String toString() {
		final StringBuilder text = new StringBuilder();
		text.append("class name: ");
		text.append(this.classname);
		text.append(", bugs: ");
		text.append(Integer.toString(this.bugs));
		return text.toString();
	}
}
