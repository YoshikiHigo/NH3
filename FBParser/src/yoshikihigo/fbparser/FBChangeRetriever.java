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
		final String startrevFBResult = fbResults.get(0);
		final String endrevFBResult = fbResults.get(1);
		final int startrev = FBParserConfig.getInstance().getSTARTREV();
		final int endrev = FBParserConfig.getInstance().getENDREV();
		final String repository = FBParserConfig.getInstance().getREPOSITORY();

		final FBParser startrevParser = new FBParser(startrevFBResult);
		startrevParser.perform();
		final List<BugInstance> startrevBugInstances = startrevParser
				.getBugInstances();

		final Map<BugInstance, LocationTransition> transitions = new HashMap<>();
		for (final BugInstance instance : startrevBugInstances) {
			final SourceLine sourceline = instance.getSourceLines().get(0);
			final String path = sourceline.sourcepath;
			final int startLine = sourceline.start;
			final int endLine = sourceline.end;

			assert null != path : "variable \"path\" must not be null.";
			assert 0 < startLine : "variable \"startLine\" must no be 0.";
			assert 0 < endLine : "variable \"endLine\" must no be 0.";

			final LocationTransition transition = new LocationTransition();
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
								final int number = (int) logEntry.getRevision();

								for (final BugInstance instance : startrevBugInstances) {

									final LocationTransition transition = transitions
											.get(instance);
									if ((null == transition)
											|| transition.hasChanged()) {
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

										final List<ChangedLocation> locations = getChangedLocations(
												sourceline.sourcepath,
												text.toString());
										final Location changedWarningLocation = moveWarningLocation(
												transition.getLatestLocation(),
												locations);
										transition.add(number,
												changedWarningLocation);
									}
								}

							}
						}
					});

		} catch (final SVNException e) {
			e.printStackTrace();
		}

		final FBParser endrevParser = new FBParser(endrevFBResult);
		endrevParser.perform();
		final List<BugInstance> endrevBugInstances = endrevParser
				.getBugInstances();

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

			for (final Entry<BugInstance, LocationTransition> entry : transitions
					.entrySet()) {
				final BugInstance instance = entry.getKey();
				final LocationTransition transition = entry.getValue();
				final Location initialLocation = transition
						.getInitialLocation();
				final Location latestLocation = transition.getLatestLocation();

				writer.print(instance.hash);
				writer.print(",");
				writer.print(instance.pattern.type);
				writer.print(",");
				writer.print(Integer.toString(instance.rank));
				writer.print(",");
				writer.print(Integer.toString(instance.priority));
				writer.print(",");

				boolean surviving = false;
				for (final BugInstance endrevBugInstance : endrevBugInstances) {
					if (instance.hash.equals(endrevBugInstance.hash)) {
						writer.write("surviving(hash), ");
						surviving = true;
						break;
					}
				}

				if (!surviving) {
					if (!latestLocation.hasLineInformaltion()) {
						writer.print("removed(unknown), ");
					} else if (latestLocation instanceof Location_ADDITION) {
						writer.print("removed(addition), ");
					} else if (latestLocation instanceof Location_DELETION) {
						writer.print("removed(deletion), ");
					} else if (latestLocation instanceof Location_REPLACEMENT) {
						writer.print("removed(replacement), ");
					} else if (latestLocation instanceof Location_UNKNOWN) {
						writer.print("removed(unknown), ");
					} else {
						writer.print("surviving(tracking), ");
						surviving = true;
					}
				}

				Integer[] changedRevisions = transition.getChangedRevisions();
				writer.print(Integer.toString(startrev));
				writer.print(", ");
				if (surviving) {
					writer.print(Integer.toString(endrev));
				} else {
					writer.print(Integer
							.toString(changedRevisions[changedRevisions.length - 1] - 1));
				}
				writer.print(", ");

				writer.print(instance.getSourceLines().get(0).sourcepath);
				writer.print(", ");

				writer.print(initialLocation.getLineRangeText());
				writer.print(", ");

				if (surviving) {
					writer.print(latestLocation.getLineRangeText());
				} else {
					final Integer revision = changedRevisions[changedRevisions.length - 1] - 1;
					final Location location = transition.getLocation(revision);
					writer.print(location.getLineRangeText());
				}
				writer.println();
			}
		} catch (final IOException e) {

		}
	}

	static private List<ChangedLocation> getChangedLocations(final String path,
			final String text) {

		final List<ChangedLocation> changedLocations = new ArrayList<>();

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
					final String beforeLocationText = tokenizer.nextToken();
					final String afterLocationText = tokenizer.nextToken();
					final String suffix = tokenizer.nextToken();

					final int beforeStartLine = Integer
							.parseInt(beforeLocationText.substring(1,
									beforeLocationText.indexOf(','))) + 3;
					final int beforeEndLine = beforeStartLine
							+ Integer
									.parseInt(beforeLocationText
											.substring(beforeLocationText
													.indexOf(',') + 1)) - 3;
					final int afterStartLine = Integer
							.parseInt(afterLocationText.substring(1,
									afterLocationText.indexOf(','))) + 3;
					final int afterEndLine = afterStartLine
							+ Integer
									.parseInt(afterLocationText
											.substring(afterLocationText
													.indexOf(',') + 1)) - 3;
					final Location beforeLocation = new Location(path,
							beforeStartLine, beforeEndLine);
					final Location afterLocation = new Location(path,
							afterStartLine, afterEndLine);
					final ChangedLocation changedLocation = new ChangedLocation(
							beforeLocation, afterLocation);
					changedLocations.add(changedLocation);
				}
			}
		}

		catch (final IOException e) {
			e.printStackTrace();
		}

		return changedLocations;
	}

	static private Location moveWarningLocation(final Location warningLocation,
			final List<ChangedLocation> changedLocations) {

		int movedLOC = 0;
		for (final ChangedLocation changedLocation : changedLocations) {

			if (changedLocation.before.endLine < warningLocation.startLine) {
				movedLOC += (changedLocation.after.endLine - changedLocation.after.startLine)
						- (changedLocation.before.endLine - changedLocation.before.startLine);
			}

			else if (warningLocation.endLine < changedLocation.before.startLine) {
				// do nothing
			}

			else {
				final String path = warningLocation.path;
				final int movedStartLine = warningLocation.startLine + movedLOC;
				final int movedEndLine = warningLocation.endLine + movedLOC;

				final int changedBeforeLength = changedLocation.before.endLine
						- changedLocation.before.startLine;
				final int changedAfterLength = changedLocation.after.endLine
						- changedLocation.after.startLine;
				if ((0 < changedBeforeLength) && (0 < changedAfterLength)) {
					return new Location_REPLACEMENT(path, movedStartLine,
							movedEndLine);
				} else if (0 < changedBeforeLength) {
					return new Location_DELETION(path, movedStartLine,
							movedEndLine);
				} else if (0 < changedAfterLength) {
					return new Location_ADDITION(path, movedStartLine,
							movedEndLine);
				} else {
					return new Location_UNKNOWN(path, movedStartLine,
							movedEndLine);
				}
			}
		}

		final Location changedWarningLocation = new Location(
				warningLocation.path, warningLocation.startLine + movedLOC,
				warningLocation.endLine + movedLOC);

		return changedWarningLocation;
	}
}
