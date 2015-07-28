package yoshikihigo.fbparser;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

public class FBComparator {

	public static void main(final String args[]) {

		if (2 != args.length) {
			System.err.println("the number of command line options must be 2.");
			System.exit(0);
		}

		final String version1file = args[0];
		final String version2file = args[1];

		final FBParser version1parser = new FBParser(version1file);
		final FBParser version2parser = new FBParser(version2file);
		version1parser.perform();
		version2parser.perform();

		final SortedSet<ClassStats> version1summary = version1parser
				.getSummary();
		final SortedSet<ClassStats> version2summary = version2parser
				.getSummary();

		{
			final int version1bugs = getTotalBugs(version1summary);
			final int version2bugs = getTotalBugs(version2summary);
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
		for (final ClassStats cs1 : version1summary) {

			if (!version2summary.contains(cs1)) {
				if (0 < cs1.bugs) {
					final StringBuilder text = new StringBuilder();
					text.append("deleted ");
					text.append(cs1.toString());
					System.out.println(text.toString());
					deleteFileBugs += cs1.bugs;
					deletedClasses.add(cs1.classname);
				}
			}

			else {
				final ClassStats cs2 = version2summary.tailSet(cs1).first();
				if (cs2.bugs < cs1.bugs) {
					final StringBuilder text = new StringBuilder();
					text.append("removed bugs: ");
					text.append(cs1.classname);
					text.append(", ");
					text.append(Integer.toString(cs1.bugs));
					text.append(" --> ");
					text.append(Integer.toString(cs2.bugs));
					System.out.println(text.toString());
					removeBugs += cs1.bugs - cs2.bugs;
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

		final SortedSet<BugInstance> version1buginstances = new TreeSet<BugInstance>(
				new BugInstance.LocationComparator());
		version1buginstances.addAll(version1parser.getBugInstances());
		final SortedSet<BugInstance> version2buginstances = new TreeSet<BugInstance>(
				new BugInstance.LocationComparator());
		version2buginstances.addAll(version2parser.getBugInstances());

		System.out.println("---------- bugs in the 1st version ----------");
		printBugInstances(version1buginstances);
		System.out.println();
		System.out.println("---------- bugs in the 2st version ----------");
		printBugInstances(version2buginstances);
		System.out.println();

		final SortedSet<BugInstance> deletedFileBugs = new TreeSet<BugInstance>(
				new BugInstance.LocationComparator());
		final SortedSet<BugInstance> removedBugs = new TreeSet<BugInstance>(
				new BugInstance.LocationComparator());
		for (final BugInstance buginstance : version1buginstances) {
			if (!version2buginstances.contains(buginstance)) {
				if (deletedClasses.contains(buginstance.getClassLocations()
						.get(0).classname)) {
					deletedFileBugs.add(buginstance);
				} else {
					removedBugs.add(buginstance);
				}
			}
		}
		System.out.println("---------- bugs in deleted files ----------");
		printBugInstances(deletedFileBugs);
		System.out.println();
		System.out
				.println("---------- bugs removed by code changes ----------");
		printBugInstances(removedBugs);
		System.out.println();
	}

	static private int getTotalBugs(final SortedSet<ClassStats> summary) {
		int sum = 0;
		for (final ClassStats cs : summary) {
			sum += cs.bugs;
		}
		return sum;
	}

	static private void printBugInstances(
			final SortedSet<BugInstance> buginstances) {

		final SortedMap<String, AtomicInteger> countMap = new TreeMap<>();
		final Map<String, Integer> rankMap = new HashMap<>();
		final Map<String, Integer> priorityMap = new HashMap<>();
		final Map<String, String> categoryMap = new HashMap<>();

		for (final BugInstance instance : buginstances) {

			AtomicInteger counts = countMap.get(instance.type);
			if (null == counts) {
				counts = new AtomicInteger(0);
				countMap.put(instance.type, counts);
			}
			counts.incrementAndGet();

			rankMap.put(instance.type, instance.rank);
			priorityMap.put(instance.type, instance.priority);
			categoryMap.put(instance.type, instance.category);
		}

		for (final Entry<String, AtomicInteger> entry : countMap.entrySet()) {
			final StringBuilder text = new StringBuilder();
			text.append("[");
			text.append(entry.getKey());
			text.append("][RANK:");
			text.append(Integer.toString(rankMap.get(entry.getKey())));
			text.append("][PRIORITY:");
			text.append(Integer.toString(priorityMap.get(entry.getKey())));
			text.append("][");
			text.append(categoryMap.get(entry.getKey()));
			text.append("]: ");
			text.append(Integer.toString(entry.getValue().get()));
			System.out.println(text.toString());
		}
	}
}
