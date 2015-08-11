package yoshikihigo.fbparser;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.SortedSet;
import java.util.TreeSet;

public class FBComparator {

	public static void main(final String args[]) {

		if (3 != args.length) {
			System.err.println("the number of command line options must be 3.");
			System.exit(0);
		}

		final String version1file = args[0];
		final String version2file = args[1];
		final String path = args[2];

		final FBParser version1parser = new FBParser(version1file);
		final FBParser version2parser = new FBParser(version2file);
		version1parser.perform();
		version2parser.perform();

		final SortedSet<ClassStats> version1Classes = version1parser
				.getSummary();
		final SortedSet<ClassStats> version2Classes = version2parser
				.getSummary();
		// printBugNumber(version1Classes, version2Classes);

		final SortedSet<ClassStats> deletedClasses = new TreeSet<>();
		deletedClasses.addAll(version1Classes);
		deletedClasses.removeAll(version2Classes);
		final SortedSet<String> deletedClassNames = ClassStats
				.getClassNames(deletedClasses);

		final SortedSet<BugInstance> version1bugs = new TreeSet<BugInstance>(
				new BugInstance.RankLocationTypeComparator());
		version1bugs.addAll(version1parser.getBugInstances());
		final SortedSet<BugInstance> version2bugs = new TreeSet<BugInstance>(
				new BugInstance.RankLocationTypeComparator());
		version2bugs.addAll(version2parser.getBugInstances());
		System.out.println("bugs in version 1: " + version1bugs.size());
		System.out.println("bugs in version 2: " + version2bugs.size());

		final SortedSet<BugInstance> survivingBugs = new TreeSet<>(
				new BugInstance.RankLocationTypeComparator());
		final SortedSet<BugInstance> bugsInDeletedFiles = new TreeSet<BugInstance>(
				new BugInstance.RankLocationTypeComparator());
		final SortedSet<BugInstance> bugsRemovedByChanges = new TreeSet<BugInstance>(
				new BugInstance.RankLocationTypeComparator());

		for (final BugInstance bug : version1bugs) {

			if (version2bugs.contains(bug)) {
				survivingBugs.add(bug);
			}

			else {
				if (deletedClassNames
						.contains(bug.getClassLocations().get(0).classname)) {
					bugsInDeletedFiles.add(bug);
				} else {
					bugsRemovedByChanges.add(bug);
				}
			}
		}

		final SortedSet<BugInstance> addedBugs = new TreeSet<>(
				new BugInstance.RankLocationTypeComparator());
		for (final BugInstance bug : version2bugs) {
			if (!version1bugs.contains(bug)) {
				addedBugs.add(bug);
			}
		}

		printBugInstances(path, survivingBugs, addedBugs, bugsInDeletedFiles,
				bugsRemovedByChanges);
	}

	static private void printBugNumber(
			final SortedSet<ClassStats> version1classes,
			final SortedSet<ClassStats> version2classes) {

		{
			final int version1bugs = getTotalBugs(version1classes);
			final int version2bugs = getTotalBugs(version2classes);
			final StringBuilder text = new StringBuilder();
			text.append("bugs in the 1st file: ");
			text.append(Integer.toString(version1bugs));
			text.append(System.lineSeparator());
			text.append("bugs in the 2nd file: ");
			text.append(Integer.toString(version2bugs));
			System.out.println(text.toString());
		}

		int deleteFileBugs = 0;
		int removeBugs = 0;
		final SortedSet<String> deletedClasses = new TreeSet<>();
		for (final ClassStats c1 : version1classes) {

			if (!version2classes.contains(c1)) {
				if (0 < c1.bugs) {
					final StringBuilder text = new StringBuilder();
					text.append("deleted ");
					text.append(c1.toString());
					System.out.println(text.toString());
					deleteFileBugs += c1.bugs;
					deletedClasses.add(c1.classname);
				}
			}

			else {
				final ClassStats c2 = version2classes.tailSet(c1).first();
				if (c2.bugs < c1.bugs) {
					final StringBuilder text = new StringBuilder();
					text.append("removed bugs: ");
					text.append(c1.classname);
					text.append(", ");
					text.append(Integer.toString(c1.bugs));
					text.append(" --> ");
					text.append(Integer.toString(c2.bugs));
					System.out.println(text.toString());
					removeBugs += c1.bugs - c2.bugs;
				}
			}
		}

		final StringBuilder text = new StringBuilder();
		text.append("bugs in deleted files: ");
		text.append(Integer.toString(deleteFileBugs));
		text.append(System.lineSeparator());
		text.append("bugs removed by changes: ");
		text.append(Integer.toString(removeBugs));
		System.out.println(text.toString());
	}

	static private int getTotalBugs(final SortedSet<ClassStats> summary) {
		int sum = 0;
		for (final ClassStats cs : summary) {
			sum += cs.bugs;
		}
		return sum;
	}

	static private void printBugInstances(final String path,
			final SortedSet<BugInstance> survivingBugs,
			final SortedSet<BugInstance> addedBugs,
			final SortedSet<BugInstance> bugsInDeletedFiles,
			final SortedSet<BugInstance> bugsremovedByChanges) {

		try (final PrintWriter writer = new PrintWriter(new OutputStreamWriter(
				new FileOutputStream(path), "UTF-8"))) {

			writer.println("bug-types, surviving-bugs, added-Bugs, bugs-in-deleted-files, bugs-removed-by-changes");

			for (final BugPattern pattern : BugPattern.getBugPatterns()) {
				final String type = pattern.type;
				final int priority = pattern.priority;
				final int rank = pattern.rank;
				final String category = pattern.category;

				final int numberOfSurvivingBugs = countBugInstances(
						survivingBugs, type);
				final int numberOfAddedBugs = countBugInstances(addedBugs, type);
				final int numberOfBugsInDeletedFiles = countBugInstances(
						bugsInDeletedFiles, type);
				final int numberOfBugsRemovedByChanges = countBugInstances(
						bugsremovedByChanges, type);
				if ((0 != numberOfSurvivingBugs) || (0 != numberOfAddedBugs)
						|| (0 != numberOfBugsInDeletedFiles)
						|| (0 != numberOfBugsRemovedByChanges)) {
					final StringBuilder text = new StringBuilder();
					text.append(type);
					text.append("[RANK:");
					text.append(Integer.toString(rank));
					text.append("][PRIORITY:");
					text.append(Integer.toString(priority));
					text.append("][CATEGORY:");
					text.append(category);
					text.append("], ");
					text.append(Integer.toString(numberOfSurvivingBugs));
					text.append(", ");
					text.append(Integer.toString(numberOfAddedBugs));
					text.append(", ");
					text.append(Integer.toString(numberOfBugsInDeletedFiles));
					text.append(", ");
					text.append(Integer.toString(numberOfBugsRemovedByChanges));
					writer.println(text.toString());
				}
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
}
