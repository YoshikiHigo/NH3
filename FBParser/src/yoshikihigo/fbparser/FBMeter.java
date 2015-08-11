package yoshikihigo.fbparser;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class FBMeter {

	public static void main(final String[] args) {

		if (3 > args.length) {
			System.err.println("3 or more command line options are required");
			System.exit(0);
		}

		final Map<String, List<BugInstance>> bugInstances = new HashMap<String, List<BugInstance>>();
		for (int i = 1; i < args.length; i++) {
			final String xmlFile = args[i];
			final FBParser parser = new FBParser(xmlFile);
			parser.perform();
			final List<BugInstance> bugs = parser.getBugInstances();
			bugInstances.put(xmlFile, bugs);
		}

		final String outputCSVFile = args[0];
		final List<VersionPair> versionPairs = new ArrayList<VersionPair>();
		for (int i = 2; i < args.length; i++) {
			final String earlierXMLFile = args[i - 1];
			final String latterXMLFile = args[i];

			final SortedSet<BugInstance> earlierBugs = new TreeSet<>(
					new BugInstance.RankLocationTypeComparator());
			earlierBugs.addAll(bugInstances.get(earlierXMLFile));
			final SortedSet<BugInstance> latterBugs = new TreeSet<>(
					new BugInstance.RankLocationTypeComparator());
			latterBugs.addAll(bugInstances.get(latterXMLFile));

			final SortedSet<BugInstance> survivingBugs = new TreeSet<>(
					new BugInstance.RankLocationTypeComparator());
			final SortedSet<BugInstance> removedBugs = new TreeSet<BugInstance>(
					new BugInstance.RankLocationTypeComparator());
			for (final BugInstance bug : earlierBugs) {

				if (latterBugs.contains(bug)) {
					survivingBugs.add(bug);
				} else {
					removedBugs.add(bug);
				}
			}

			final SortedSet<BugInstance> addedBugs = new TreeSet<>(
					new BugInstance.RankLocationTypeComparator());
			for (final BugInstance bug : latterBugs) {
				if (!earlierBugs.contains(bug)) {
					addedBugs.add(bug);
				}
			}

			final VersionPair pair = new VersionPair(earlierXMLFile,
					latterXMLFile, survivingBugs, addedBugs, removedBugs);
			versionPairs.add(pair);
		}

		try (final PrintWriter writer = new PrintWriter(new OutputStreamWriter(
				new FileOutputStream(outputCSVFile), "UTF-8"))) {

			final StringBuilder typeText = new StringBuilder();
			final StringBuilder rankText = new StringBuilder();
			final StringBuilder priorityText = new StringBuilder();
			final StringBuilder categoryText = new StringBuilder();
			typeText.append("TYPE, ");
			rankText.append("RANK, ");
			priorityText.append("PRIORITY, ");
			categoryText.append("CATEGORY, ");
			for (final BugPattern pattern : BugPattern.getBugPatterns()) {
				typeText.append(pattern.type + "[number-of-surviving-bugs],");
				typeText.append(pattern.type + "[number-of-removed-bugs],");
				typeText.append(pattern.type + "[number-of-added-bugs],");
				typeText.append(pattern.type + "[ratio-of-surviving], ");
				typeText.append(pattern.type + "[ratio-of-solved-old], ");
				typeText.append(pattern.type + "[ratio-of-solved-new], ");
				rankText.append(Integer.toString(pattern.rank) + ", ");
				rankText.append(Integer.toString(pattern.rank) + ", ");
				rankText.append(Integer.toString(pattern.rank) + ", ");
				rankText.append(Integer.toString(pattern.rank) + ", ");
				rankText.append(Integer.toString(pattern.rank) + ", ");
				rankText.append(Integer.toString(pattern.rank) + ", ");
				priorityText.append(Integer.toString(pattern.priority) + ", ");
				priorityText.append(Integer.toString(pattern.priority) + ", ");
				priorityText.append(Integer.toString(pattern.priority) + ", ");
				priorityText.append(Integer.toString(pattern.priority) + ", ");
				priorityText.append(Integer.toString(pattern.priority) + ", ");
				priorityText.append(Integer.toString(pattern.priority) + ", ");
				categoryText.append(pattern.category + ", ");
				categoryText.append(pattern.category + ", ");
				categoryText.append(pattern.category + ", ");
				categoryText.append(pattern.category + ", ");
				categoryText.append(pattern.category + ", ");
				categoryText.append(pattern.category + ", ");
			}
			typeText.deleteCharAt(typeText.length() - 1);
			rankText.deleteCharAt(rankText.length() - 1);
			priorityText.deleteCharAt(priorityText.length() - 1);
			categoryText.deleteCharAt(categoryText.length() - 1);
			writer.println(typeText);
			writer.println(rankText);
			writer.println(priorityText);
			writer.println(categoryText);

			for (final VersionPair pair : versionPairs) {

				final StringBuilder dataText = new StringBuilder();
				dataText.append(StringUtility.getName(pair.earlierXMLFile));
				dataText.append("--");
				dataText.append(StringUtility.getName(pair.latterXMLFile));
				dataText.append(", ");

				for (final BugPattern pattern : BugPattern.getBugPatterns()) {

					final int numberOfSurvivingBugs = countBugInstances(
							pair.survivingBugs, pattern.type);
					final int numberOfAddedBugs = countBugInstances(
							pair.addedBugs, pattern.type);
					final int numberOfRemovedBugs = countBugInstances(
							pair.removedBugs, pattern.type);

					float ratioOfSurviving = (float) numberOfSurvivingBugs
							/ (float) (numberOfSurvivingBugs + numberOfRemovedBugs);
					float ratioOfSolvedOld = (float) numberOfRemovedBugs
							/ (float) (numberOfSurvivingBugs + numberOfRemovedBugs);
					float ratioOfSolvednew = (float) numberOfRemovedBugs
							/ (float) (numberOfSurvivingBugs + numberOfAddedBugs);

					dataText.append(Integer.toString(numberOfSurvivingBugs));
					dataText.append(", ");
					dataText.append(Integer.toString(numberOfRemovedBugs));
					dataText.append(", ");
					dataText.append(Integer.toString(numberOfAddedBugs));
					dataText.append(", ");
					dataText.append(Float.toString(ratioOfSurviving));
					dataText.append(", ");
					dataText.append(Float.toString(ratioOfSolvedOld));
					dataText.append(", ");
					dataText.append(Float.toString(ratioOfSolvednew));
					dataText.append(", ");
				}
				writer.println(dataText.toString());
			}
		}

		catch (final IOException e) {
			e.printStackTrace();
		}
	}

	static private int countBugInstances(
			final SortedSet<BugInstance> instances, final String type) {
		int count = 0;
		for (final BugInstance instance : instances) {
			if (instance.pattrn.type.equals(type)) {
				count++;
			}
		}
		return count;
	}

	static class VersionPair {
		final String earlierXMLFile;
		final String latterXMLFile;
		final SortedSet<BugInstance> survivingBugs;
		final SortedSet<BugInstance> addedBugs;
		final SortedSet<BugInstance> removedBugs;

		VersionPair(final String earlierXMLFile, final String latterXMLFile,
				final SortedSet<BugInstance> survivingBugs,
				final SortedSet<BugInstance> addedBugs,
				final SortedSet<BugInstance> removedBugs) {
			this.earlierXMLFile = earlierXMLFile;
			this.latterXMLFile = latterXMLFile;
			this.survivingBugs = survivingBugs;
			this.addedBugs = addedBugs;
			this.removedBugs = removedBugs;
		}
	}
}
