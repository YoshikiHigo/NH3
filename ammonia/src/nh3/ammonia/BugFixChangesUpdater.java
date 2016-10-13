package nh3.ammonia;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class BugFixChangesUpdater {

	public static void main(final String[] args) {
		FBParserConfig.initialize(args);
		BugFixChangesUpdater main = new BugFixChangesUpdater();
		main.update();
	}

	private void update() {

		try {
			Class.forName("org.sqlite.JDBC");
			final String database = FBParserConfig.getInstance().getDATABASE();
			final Connection connector = DriverManager
					.getConnection("jdbc:sqlite:" + database);

			final long startrev = FBParserConfig.getInstance().getSTARTREV();
			final long endrev = FBParserConfig.getInstance().getENDREV();
			final SortedSet<Integer> revisions = new TreeSet<>();
			final Statement statement1 = connector.createStatement();
			final ResultSet results1 = statement1
					.executeQuery("select id from revisions where " + startrev
							+ " < id and id < " + endrev);
			while (results1.next()) {
				final int revision = results1.getInt(1);
				revisions.add(revision);
			}
			statement1.close();

			final String fbResult = FBParserConfig.getInstance().getFBRESULTS()
					.get(0);
			final FBParser parser = new FBParser(fbResult);
			parser.perform();
			final List<BugInstance> bugInstances = parser.getBugInstances();
			final Map<BugInstance, LocationTransition> transitions = new HashMap<>();
			for (final BugInstance instance : bugInstances) {
				final SourceLine sourceline = instance.getSourceLines().get(0);
				final String path = sourceline.sourcepath;
				final int startLine = sourceline.start;
				final int endLine = sourceline.end;
				final LocationTransition transition = new LocationTransition();
				transition.add((int) startrev, new Location(path, startLine,
						endLine, false));
				transitions.put(instance, transition);
			}

			final PreparedStatement statement2 = connector
					.prepareStatement("select "
							+ "B.id, "
							+ "B.filepath, "
							+ "(select C1.start from codes C1 where C1.id = B.beforeID), "
							+ "(select C2.end from codes C2 where C2.id = B.beforeID) "
							+ "from bugfixchanges B where B.revision = ?");
			final PreparedStatement statement3 = connector
					.prepareStatement("update bugfixchanges set warningfix = ? where id = ?");
			for (final Integer revision : revisions) {
				statement2.setInt(1, revision);
				final ResultSet results2 = statement2.executeQuery();
				while (results2.next()) {
					final int id = results2.getInt(1);
					final String filepath = results2.getString(2);
					final int startline = results2.getInt(3);
					final int endline = results2.getInt(4);

					for (final LocationTransition wlt : transitions.values()) {

						if (!wlt.hasChanged()) {

							final Location warningLocation = wlt
									.getLatestLocation();
							if (!warningLocation.path.equals(filepath)) {
								continue;
							}

							if (endline < warningLocation.startLine) {
								continue;
							}

							if (warningLocation.endLine < startline) {
								continue;
							}

							statement3.setInt(1, 1);
							statement3.setInt(2, id);
							statement3.addBatch();
						}
					}
				}

				statement3.executeBatch();

				this.updateWarningLocations(transitions, revision);
			}
			statement2.close();
			statement3.close();

		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void updateWarningLocations(
			final Map<BugInstance, LocationTransition> transitions,
			final int revision) {

		try {

			final String repository = FBParserConfig.getInstance()
					.getSVNREPOSITORY();
			final SVNDiffClient diffClient = SVNClientManager.newInstance()
					.getDiffClient();
			final Map<String, String> diffCache = new HashMap<>();

			for (final Entry<BugInstance, LocationTransition> entry : transitions
					.entrySet()) {

				final BugInstance warning = entry.getKey();
				final LocationTransition transition = entry.getValue();
				if (null == transition || transition.hasChanged()) {
					continue;
				}

				final String path = warning.getSourceLines().get(0).sourcepath;
				String diffText = diffCache.get(path);
				if (null == diffText) {
					final SVNURL fileURL = SVNURL.fromFile(new File(repository
							+ File.separator + path));
					final SVNNodeKind node = SVNRepositoryFactory.create(
							fileURL).checkPath("", revision - 1);
					if (SVNNodeKind.NONE == node) {
						continue;
					}

					final StringBuilder text = new StringBuilder();
					diffClient.doDiff(fileURL,
							SVNRevision.create(revision - 1), fileURL,
							SVNRevision.create(revision), SVNDepth.FILES, true,
							new OutputStream() {

								@Override
								public void write(int b) throws IOException {
									text.append((char) b);
								}
							});
					diffText = text.toString();
					diffCache.put(path, diffText);
				}

				final List<ChangedLocation> changedLocations = getChangedLocations(
						path, diffText);
				final Location latestWarningLocation = transition
						.getLatestLocation();
				final Location movedWarningLocation = moveWarningLocation(
						latestWarningLocation, changedLocations);
				if (!latestWarningLocation.equals(movedWarningLocation)) {
					transition.add(revision, movedWarningLocation);
				}
			}
		}

		catch (final SVNException e) {
			e.printStackTrace();
		}
	}

	private List<ChangedLocation> getChangedLocations(final String path,
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
					final String beforeLocation = tokenizer.nextToken();
					final String afterLocation = tokenizer.nextToken();
					final String suffix = tokenizer.nextToken();

					final int beforeStartLine = Integer.parseInt(beforeLocation
							.substring(1, beforeLocation.indexOf(','))) + 3;
					final int beforeEndLine = beforeStartLine
							+ Integer
									.parseInt(beforeLocation
											.substring(beforeLocation
													.indexOf(',') + 1)) - 3;
					final int afterStartLine = Integer.parseInt(afterLocation
							.substring(1, afterLocation.indexOf(','))) + 3;
					final int afterEndLine = afterStartLine
							+ Integer.parseInt(afterLocation
									.substring(afterLocation.indexOf(',') + 1))
							- 3;
					final Location before = new Location(path, beforeStartLine,
							beforeEndLine, false);
					final Location after = new Location(path, afterStartLine,
							afterEndLine, false);
					final ChangedLocation changedLocation = new ChangedLocation(
							before, after, false);
					changedLocations.add(changedLocation);
				}
			}
		}

		catch (final IOException e) {
			e.printStackTrace();
		}

		return changedLocations;
	}

	private Location moveWarningLocation(final Location warningLocation,
			final List<ChangedLocation> changedLocations) {

		int moved = 0;
		for (final ChangedLocation cl : changedLocations) {

			if (cl.before.endLine < warningLocation.startLine) {
				moved += (cl.after.endLine - cl.after.startLine)
						- (cl.before.endLine - cl.before.startLine);
			}

			else if (warningLocation.endLine < cl.before.startLine) {
				// do nothing
			}

			else {
				final String movedPath = warningLocation.path;
				final int movedStartLine = warningLocation.startLine + moved;
				final int movedEndLine = warningLocation.endLine + moved;

				final int sizeOfBeforeChange = cl.before.endLine
						- cl.before.startLine;
				final int sizeOfAfterChange = cl.after.endLine
						- cl.after.startLine;
				if (0 < sizeOfBeforeChange && 0 < sizeOfAfterChange) {
					return new Location_REPLACEMENT(movedPath, movedStartLine,
							movedEndLine, false);
				} else if (0 < sizeOfBeforeChange) {
					return new Location_DELETION(movedPath, movedStartLine,
							movedEndLine, false);
				} else if (0 < sizeOfAfterChange) {
					return new Location_ADDITION(movedPath, movedStartLine,
							movedEndLine, false);
				} else {
					return new Location_UNKNOWN(movedPath, movedStartLine,
							movedEndLine, false);
				}
			}
		}

		final Location movedWarningLocation = new Location(
				warningLocation.path, warningLocation.startLine + moved,
				warningLocation.endLine + moved, false);
		return movedWarningLocation;
	}
}
