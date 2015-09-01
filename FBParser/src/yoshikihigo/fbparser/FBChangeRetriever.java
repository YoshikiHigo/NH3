package yoshikihigo.fbparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class FBChangeRetriever {

	public static void main(final String[] args) {

		FBParserConfig.initialize(args);

		final String fbResult = FBParserConfig.getInstance().getFBRESULT();
		final long startrev = FBParserConfig.getInstance().getSTARTREV();
		final long endrev = FBParserConfig.getInstance().getENDREV();
		final String repository = FBParserConfig.getInstance().getREPOSITORY();

		final FBParser parser = new FBParser(fbResult);
		parser.perform();
		final List<BugInstance> bugInstances = parser.getBugInstances();

		// remove bugs that do not contain line information
		for (int index = 0; index < bugInstances.size(); index++) {
			final BugInstance instance = bugInstances.get(index);
			if (0 == instance.getSourceLines().get(0).start) {
				bugInstances.remove(index);
				index--;
			}
		}

		final Map<BugInstance, RangeTransition> transitions = new HashMap<>();
		for (final BugInstance instance : bugInstances) {
			final SourceLine sourceline = instance.getSourceLines().get(0);
			final int startLine = sourceline.start;
			final int endLine = sourceline.end;
			final RangeTransition transition = new RangeTransition();
			transition.add(startrev, new Range(startLine, endLine));
			transitions.put(instance, transition);
		}

		try {

			final SVNURL url = SVNURL.fromFile(new File(repository));
			FSRepositoryFactory.setup();
			final SVNRepository svnRepository = FSRepositoryFactory.create(url);
			final SVNDiffClient diffClient = SVNClientManager.newInstance()
					.getDiffClient();

			svnRepository.log(null, startrev + 1, endrev, true, true,
					new ISVNLogEntryHandler() {
						@Override
						public void handleLogEntry(final SVNLogEntry logEntry)
								throws SVNException {
							for (final Object key : logEntry.getChangedPaths()
									.keySet()) {
								final String path = (String) key;
								final long number = logEntry.getRevision();

								for (final BugInstance instance : bugInstances) {

									final RangeTransition transition = transitions
											.get(instance);
									if (transition.hasDisappeared()) {
										continue;
									}

									final SourceLine sourceline = instance
											.getSourceLines().get(0);
									if (path.endsWith(sourceline.sourcepath)) {

										final StringBuilder text = new StringBuilder();
										final SVNURL fileURL = SVNURL
												.fromFile(new File(repository
														+ File.separator
														+ sourceline.sourcepath));
										diffClient.doDiff(fileURL,
												SVNRevision.create(number - 1),
												fileURL,
												SVNRevision.create(number),
												SVNDepth.INFINITY, true,
												new OutputStream() {

													@Override
													public void write(int b)
															throws IOException {
														text.append((char) b);
													}
												});

										final List<int[]> ranges = getChangedRanges(text
												.toString());
										final Range newRange = moveBuggedArea(
												transition.getLatestRange(),
												ranges);
										transition.add(number, newRange);
									}
								}

							}
						}
					});

			for (final BugInstance instance : bugInstances) {
				System.out.println(instance.pattern.type + " : "
						+ instance.getSourceLines().get(0).sourcepath);
				final RangeTransition transition = transitions.get(instance);
				final Long[] revisions = transition.getChangedRevisions();
				for (final Long number : revisions) {
					final Range range = transition.getRange(number);
					if (null == range) {
						System.out.println(number + " : disappeared");
					} else {
						System.out.println(number + " : " + range.startLine
								+ "--" + range.startLine);
					}
				}
			}

		}

		catch (final SVNException e) {

		}
	}

	static private List<int[]> getChangedRanges(final String text) {

		final List<int[]> ranges = new ArrayList<>();

		try (final BufferedReader reader = new BufferedReader(new StringReader(
				text))) {
			while (true) {

				final String line = reader.readLine();

				if (null == line) {
					break;
				}

				else if (line.startsWith("@@") && line.endsWith("@@")) {
					final StringTokenizer tokenizer = new StringTokenizer(line);
					final String prefix = tokenizer.nextToken();
					final String preRange = tokenizer.nextToken();
					final String postRange = tokenizer.nextToken();
					final String suffix = tokenizer.nextToken();

					final int preStart = Integer.parseInt(preRange.substring(1,
							preRange.indexOf(','))) + 3;
					final int preEnd = preStart
							+ Integer.parseInt(preRange.substring(preRange
									.indexOf(',') + 1)) - 3;
					final int postStart = Integer.parseInt(postRange.substring(
							1, postRange.indexOf(','))) + 3;
					final int postEnd = postStart
							+ Integer.parseInt(postRange.substring(postRange
									.indexOf(',') + 1)) - 3;
					final int[] range = new int[4];
					range[0] = preStart;
					range[1] = preEnd;
					range[2] = postStart;
					range[3] = postEnd;
					ranges.add(range);
				}
			}
		}

		catch (final IOException e) {
			e.printStackTrace();
		}

		return ranges;
	}

	static private Range moveBuggedArea(final Range buggedArea,
			final List<int[]> changedRanges) {

		int moved = 0;
		for (final int[] changedRange : changedRanges) {

			if (changedRange[1] < buggedArea.startLine) {
				moved += (changedRange[3] - changedRange[2])
						- (changedRange[1] - changedRange[0]);
			}

			else if (buggedArea.endLine < changedRange[0]) {
				// do nothing
			}

			else {
				return null;
			}
		}

		final Range newBuggedRange = new Range(buggedArea.startLine + moved,
				buggedArea.endLine + moved);

		return newBuggedRange;
	}
}
