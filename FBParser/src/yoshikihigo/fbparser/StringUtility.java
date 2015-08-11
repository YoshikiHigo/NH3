package yoshikihigo.fbparser;

import java.io.File;

public class StringUtility {

	static public String getName(final String path) {
		final int index = path.lastIndexOf(File.separatorChar);
		return (0 < index) ? path.substring(index + 1) : path;
	}
}
