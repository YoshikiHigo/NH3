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

		final Map<BugInstance, WarningLocationTransition> transitions = new HashMap<>();
		for (final BugInstance instance : bugInstances1) {
			final SourceLine sourceline = instance.getSourceLines().get(0);
			final String path = sourceline.sourcepath;
			final int startLine = sourceline.start;
			final int endLine = sourceline.end;
			final WarningLocationTransition transition = new WarningLocationTransition();
			transition.add(startrev, new Location(path, startLine, endLine));
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

									final WarningLocationTransition transition = transitions
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

										final List<ChangedLocation> ranges = getChangedRanges(
												sourceline.sourcepath,
												text.toString());
										final Location newRange = moveBuggedArea(
												transition.getLastRange(),
												ranges);
										transition.add(number, newRange);
									}
								}

							}
						}
					});

		} catch (final SVNException e) {
			e.printStackTrace();
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

			for (final Entry<BugInstance, WarningLocationTransition> entry : transitions
					.entrySet()) {
				final BugInstance instance = entry.getKey();
				final WarningLocationTransition transition = entry.getValue();
				final Location firstRange = transition.getFirstRange();
				final Location lastRange = transition.getLastRange();

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
					} else if (lastRange instanceof Location_ADDITION) {
						writer.print("removed(addition), ");
					} else if (lastRange instanceof Location_DELETION) {
						writer.print("removed(deletion), ");
					} else if (lastRange instanceof Location_REPLACEMENT) {
						writer.print("removed(replacement), ");
					} else if (lastRange instanceof Location_UNKNOWN) {
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
					final Long revision = changedRevisions[changedRevisions.length - 1];// -
																						// 1;
					final Location range = transition.getRange(revision);
					writer.print(range.getLineRangeText());
				}
				writer.println();
			}
		} catch (final IOException e) {

		}
	}

	static private List<ChangedLocation> getChangedRanges(final String path,
			final String text) {

		final List<ChangedLocation> ranges = new ArrayList<>();

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
					final Location before = new Location(path, preStart, preEnd);
					final Location after = new Location(path, postStart,
							postEnd);
					final ChangedLocation change = new ChangedLocation(before,
							after);
					ranges.add(change);
				}
			}
		}

		catch (final IOException e) {
			e.printStackTrace();
		}

		return ranges;
	}

	static private Location moveBuggedArea(final Location buggedArea,
			final List<ChangedLocation> changedRanges) {

		int moved = 0;
		for (final ChangedLocation changedRange : changedRanges) {

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
					return new Location_REPLACEMENT(newPath, newStartLine,
							newEndLine);
				} else if (0 < changedBeforeLength) {
					return new Location_DELETION(newPath, newStartLine,
							newEndLine);
				} else if (0 < changedAfterLength) {
					return new Location_ADDITION(newPath, newStartLine,
							newEndLine);
				} else {
					return new Location_UNKNOWN(newPath, newStartLine,
							newEndLine);
				}
			}
		}

		final Location newBuggedRange = new Location(buggedArea.path,
				buggedArea.startLine + moved, buggedArea.endLine + moved);

		return newBuggedRange;
	}
}
