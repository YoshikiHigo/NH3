package yoshikihigo.fbparser;

import java.io.File;
import java.util.SortedSet;

public class StringUtility {

	static public String getName(final String path) {
		final int index = path.lastIndexOf(File.separatorChar);
		return (0 < index) ? path.substring(index + 1) : path;
	}

	static public String concatinate(final SortedSet<String> names) {
		final StringBuilder text = new StringBuilder();
		for (final String name : names) {
			text.append(name);
			text.append(System.lineSeparator());
		}
		return text.toString();
	}
}
