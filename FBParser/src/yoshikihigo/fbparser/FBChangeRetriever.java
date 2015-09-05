package yoshikihigo.fbparser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

		final List<String> fbResults = FBParserConfig.getInstance()
				.getFBRESULTS();
		final String fbResult1 = fbResults.get(0);
		final String fbResult2 = fbResults.get(1);
		final long startrev = FBParserConfig.getInstance().getSTARTREV();
		final long endrev = FBParserConfig.getInstance().getENDREV();
		final String repository = FBParserConfig.getInstance().getREPOSITORY();

		final FBParser parser1 = new FBParser(fbResult1);
		parser1.perform();
		final List<BugInstance> bugInstances1 = parser1.getBugInstances();

		final Map<BugInstance, RangeTransition> transitions = new HashMap<>();
		for (final BugInstance instance : bugInstances1) {
			final SourceLine sourceline = instance.getSourceLines().get(0);
			final String path = sourceline.sourcepath;
			final int startLine = sourceline.start;
			final int endLine = sourceline.end;
			final RangeTransition transition = new RangeTransition();
			transition.add(startrev, new Range(path, startLine, endLine));
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

								for (final BugInstance instance : bugInstances1) {

									final RangeTransition transition = transitions
											.get(instance);
									if (null == transition
											|| transition.hasDisappeared()) {
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

										final List<Change> ranges = getChangedRanges(
												sourceline.sourcepath,
												text.toString());
										final Range newRange = moveBuggedArea(
												transition.getLastRange(),
												ranges);
										transition.add(number, newRange);
									}
								}

							}
						}
					});

			for (final BugInstance instance : bugInstances1) {
				System.out.println(instance.pattern.type + " : "
						+ instance.getSourceLines().get(0).sourcepath);
				final RangeTransition transition = transitions.get(instance);
				final Long[] revisions = transition.getChangedRevisions();
				for (final Long number : revisions) {
					final Range range = transition.getRange(number);
					if (!range.hasLineInformaltion()) {
						System.out.println(number + " : no line information");
					} else if (range instanceof Range_ADDITION) {
						System.out.println(number
								+ " : bugged code was changed (addition)");
					} else if (range instanceof Range_DELETION) {
						System.out.println(number
								+ " : bugged code was changed (deletion)");
					} else if (range instanceof Range_REPLACEMENT) {
						System.out.println(number
								+ " : bugged code was changed (replacement)");
					} else if (range instanceof Range_UNKNOWN) {
						System.out.println(number
								+ " : bugged code was changed (unknown)");
					} else {
						System.out.println(number + " : " + range.startLine
								+ "--" + range.startLine);
					}
				}
			}

		}

		catch (final SVNException e) {

		}

		final FBParser parser2 = new FBParser(fbResult2);
		parser2.perform();
		final List<BugInstance> bugInstances2 = parser2.getBugInstances();

		final String trFile = FBParserConfig.getInstance()
				.getTRANSITIONRESULT();
		try (final PrintWriter writer = new PrintWriter(new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(trFile), "UTF-8")))) {

			writer.print("HASH, ");
			writer.print("BUGTYPE, ");
			writer.print("RANK, ");
			writer.print("PRIORITY, ");
			writer.print("STATUS, ");
			writer.print("START-REV, ");
			writer.print("END-REV, ");
			writer.print("FILE, ");
			writer.print("START-POSITION, ");
			writer.print("END-POSITION");
			writer.println();

			for (final Entry<BugInstance, RangeTransition> entry : transitions
					.entrySet()) {
				final BugInstance instance = entry.getKey();
				final RangeTransition transition = entry.getValue();
				final Range firstRange = transition.getFirstRange();
				final Range lastRange = transition.getLastRange();

				writer.print(instance.hash);
				writer.print(",");
				writer.print(instance.pattern.type);
				writer.print(",");
				writer.print(Integer.toString(instance.rank));
				writer.print(",");
				writer.print(Integer.toString(instance.priority));
				writer.print(",");

				boolean surviving = false;
				for (final BugInstance instance2 : bugInstances2) {
					if (instance.hash.equals(instance2.hash)) {
						writer.write("surviving(hash), ");
						surviving = true;
						break;
					}
				}

				if (!surviving) {
					if (!lastRange.hasLineInformaltion()) {
						writer.print("removed(unknown), ");
					} else if (lastRange instanceof Range_ADDITION) {
						writer.print("removed(addition), ");
					} else if (lastRange instanceof Range_DELETION) {
						writer.print("removed(deletion), ");
					} else if (lastRange instanceof Range_REPLACEMENT) {
						writer.print("removed(replacement), ");
					} else if (lastRange instanceof Range_UNKNOWN) {
						writer.print("removed(unknown), ");
					} else {
						writer.print("surviving(tracking), ");
						surviving = true;
					}
				}

				Long[] changedRevisions = transition.getChangedRevisions();
				writer.print(Long.toString(startrev));
				writer.print(", ");
				if (surviving) {
					writer.print(Long.toString(endrev));
				} else {
					writer.print(Long
							.toString(changedRevisions[changedRevisions.length - 1] - 1));
				}
				writer.print(", ");

				writer.print(instance.getSourceLines().get(0).sourcepath);
				writer.print(", ");

				writer.print(firstRange.getLineRangeText());
				writer.print(", ");

				if (surviving) {
					writer.print(lastRange.getLineRangeText());
				} else {
					final Long revision = changedRevisions[changedRevisions.length - 1] - 1;
					final Range range = transition.getRange(revision);
					writer.print(range.getLineRangeText());
				}
				writer.println();
			}
		} catch (final IOException e) {

		}
	}

	static private List<Change> getChangedRanges(final String path,
			final String text) {

		final List<Change> ranges = new ArrayList<>();

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
					final Range before = new Range(path, preStart, preEnd);
					final Range after = new Range(path, postStart, postEnd);
					final Change change = new Change(before, after);
					ranges.add(change);
				}
			}
		}

		catch (final IOException e) {
			e.printStackTrace();
		}

		return ranges;
	}

	static private Range moveBuggedArea(final Range buggedArea,
			final List<Change> changedRanges) {

		int moved = 0;
		for (final Change changedRange : changedRanges) {

			if (changedRange.before.endLine < buggedArea.startLine) {
				moved += (changedRange.after.endLine - changedRange.after.startLine)
						- (changedRange.before.endLine - changedRange.before.startLine);
			}

			else if (buggedArea.endLine < changedRange.before.startLine) {
				// do nothing
			}

			else {
				final String newPath = buggedArea.path;
				final int newStartLine = buggedArea.startLine + moved;
				final int newEndLine = buggedArea.endLine + moved;

				final int changedBeforeLength = changedRange.before.endLine
						- changedRange.before.startLine;
				final int changedAfterLength = changedRange.after.endLine
						- changedRange.after.startLine;
				if (0 < changedBeforeLength && 0 < changedAfterLength) {
					return new Range_REPLACEMENT(newPath, newStartLine,
							newEndLine);
				} else if (0 < changedBeforeLength) {
					return new Range_DELETION(newPath, newStartLine, newEndLine);
				} else if (0 < changedAfterLength) {
					return new Range_ADDITION(newPath, newStartLine, newEndLine);
				} else {
					return new Range_UNKNOWN(newPath, newStartLine, newEndLine);
				}
			}
		}

		final Range newBuggedRange = new Range(buggedArea.path,
				buggedArea.startLine + moved, buggedArea.endLine + moved);

		return newBuggedRange;
	}
}
