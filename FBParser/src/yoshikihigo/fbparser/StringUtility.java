package yoshikihigo.fbparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtility {

	static public String getName(final String path) {
		final int index = path.lastIndexOf(File.separatorChar);
		return (0 < index) ? path.substring(index + 1) : path;
	}
	
	static public String removeExtension(final String name){
		final int dotIndex = name.lastIndexOf('.');
		if(0 < dotIndex){
			return name.substring(0, dotIndex);
		}else{
			return name;
		}
	}

	static public String concatinate(final Collection<String> names) {
		final StringBuilder text = new StringBuilder();
		final String[] array = names.toArray(new String[0]);
		for (int index = 0; index < array.length; index++) {
			text.append(array[index]);
			if (index < (array.length - 1)) {
				text.append(System.lineSeparator());
			}
		}
		return text.toString();
	}

	static public List<String> split(final String nameText) {

		final List<String> names = new ArrayList<>();
		try (final BufferedReader reader = new BufferedReader(new StringReader(
				nameText))) {
			while (true) {
				final String line = reader.readLine();
				if (null == line) {
					break;
				}
				names.add(line);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return names;
	}

	static public int getLOC(final String text) {

		int count = 0;
		final String newline = System.lineSeparator();
		final Matcher matcher = Pattern.compile(newline).matcher(text);
		while (matcher.find()) {
			count++;
		}
		return count + 1;
	}
}
