package yoshikihigo.fbparser.db;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import yoshikihigo.fbparser.FBParserConfig;

public class DAO {

	static private DAO SINGLETON = null;

	static public DAO getInstance() {
		if (null == SINGLETON) {
			SINGLETON = new DAO();
		}
		return SINGLETON;
	}

	static public void deleteInstance() throws Exception {
		if (null != SINGLETON) {
			SINGLETON.clone();
			SINGLETON = null;
		}
	}

	private Connection connector;

	private DAO() {

		try {
			Class.forName("org.sqlite.JDBC");
			final String database = FBParserConfig.getInstance().getDATABASE();
			this.connector = DriverManager.getConnection("jdbc:sqlite:"
					+ database);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void close() {
		try {
			this.connector.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public SortedSet<REVISION_SQL> getRevisions() {
		final SortedSet<REVISION_SQL> revisions = new TreeSet<>();

		try {

			final Statement statement = this.connector.createStatement();
			final ResultSet result = statement
					.executeQuery("select distinct revision, bugfix from bugfixchanges");
			while (result.next()) {
				final int number = result.getInt(1);
				final boolean bugfix = 0 < result.getInt(2);
				final REVISION_SQL revision = new REVISION_SQL(number, bugfix);
				revisions.add(revision);
			}

			statement.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return revisions;

	}

	public SortedSet<REVISION_SQL> getRevisions(final byte[] beforeHash,
			final byte[] afterHash) {

		final SortedSet<REVISION_SQL> revisions = new TreeSet<>();

		try {

			final String text = "select distinct revision, bugfix from bugfixchanges "
					+ "where beforeHash = ? and afterHash = ?";
			final PreparedStatement statement = this.connector
					.prepareStatement(text);
			statement.setBytes(1, beforeHash);
			statement.setBytes(2, afterHash);
			final ResultSet result = statement.executeQuery();

			while (result.next()) {
				final int number = result.getInt(1);
				final boolean bugfix = 0 < result.getInt(2);
				final REVISION_SQL revision = new REVISION_SQL(number, bugfix);
				revisions.add(revision);
			}

			statement.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return revisions;
	}

	public List<CHANGE_SQL> getChanges(final byte[] beforeHash,
			final byte[] afterHash) {

		final List<CHANGE_SQL> changes = new ArrayList<>();

		try {

			final String text = "select id, revision, filepath, "
					+ "(select start from codes where id = beforeID), "
					+ "(select end from codes where id = beforeID), "
					+ "(select start from codes where id = afterID),"
					+ "(select end from codes where id = afterID), "
					+ "(select author from revisions where number = revision), "
					+ "(select message from revisions where number = revision), "
					+ "bugfix "
					+ "from bugfixchanges where beforeHash = ? and afterHash = ?";
			final PreparedStatement statement = this.connector
					.prepareStatement(text);
			statement.setBytes(1, beforeHash);
			statement.setBytes(2, afterHash);
			final ResultSet results = statement.executeQuery();

			while (results.next()) {
				final int changeID = results.getInt(1);
				final int revision = results.getInt(2);
				final String filepath = results.getString(3);
				final int startline = results.getInt(4);
				final int endline = results.getInt(5);
				final int afterStartLine = results.getInt(6);
				final int afterEndLine = results.getInt(7);
				final String author = results.getString(8);
				final String message = results.getString(9);
				final int bugfix = results.getInt(10);
				final CHANGE_SQL change = new CHANGE_SQL(changeID, beforeHash,
						afterHash, revision, filepath, startline, endline,
						afterStartLine, afterEndLine, author, message,
						0 < bugfix);
				changes.add(change);
			}

			statement.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return changes;
	}

	public List<CHANGE_SQL> getChanges(final String beforeNText,
			final String afterNText) {

		final List<CHANGE_SQL> changes = new ArrayList<>();

		try {

			final String sqlText = "select C1.id, C1.revision, C1.filepath, "
					+ "C1.beforeHash, C1.afterHash, C2.start, C2.end, C3.start, C3.end, "
					+ "R.author, R.message, C1.bugfix from bugfixchanges C1 "
					+ "inner join codes C2 on C1.beforeID = C2.id "
					+ "inner join codes C3 on C1.afterID = C3.id "
					+ "inner join revisions R on C1.revision = R.number "
					+ "where C2.nText = ? and C3.nText = ?";
			final PreparedStatement statement = this.connector
					.prepareStatement(sqlText);
			statement.setString(1, beforeNText);
			statement.setString(2, afterNText);
			final ResultSet results = statement.executeQuery();

			while (results.next()) {
				final int changeID = results.getInt(1);
				final int revision = results.getInt(2);
				final String filepath = results.getString(3);
				final byte[] beforeHash = results.getBytes(4);
				final byte[] afterHash = results.getBytes(5);
				final int startline = results.getInt(6);
				final int endline = results.getInt(7);
				final int afterStartLine = results.getInt(8);
				final int afterEndLine = results.getInt(9);
				final String author = results.getString(10);
				final String message = results.getString(11);
				final int bugfix = results.getInt(12);
				if (0 < bugfix) {
					final CHANGE_SQL change = new CHANGE_SQL(changeID,
							beforeHash, afterHash, revision, filepath,
							startline, endline, afterStartLine, afterEndLine,
							author, message, true);
					changes.add(change);
				}
			}

			statement.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return changes;
	}

	public List<CHANGE_SQL> getChanges(final long revision) {

		final String text = "select id, beforeHash, afterHash, filepath, "
				+ "(select start from codes where id = beforeID), "
				+ "(select end from codes where id = beforeID), "
				+ "(select start from codes where id = afterID), "
				+ "(select end from codes where id = afterID), "
				+ "(select author from revisions where number = revision), "
				+ "(select message from revisions where number = revision), "
				+ "bugfix " + "from bugfixchanges where revision = " + revision;

		final List<CHANGE_SQL> changes = new ArrayList<>();

		try {
			final Statement statement = this.connector.createStatement();
			final ResultSet results = statement.executeQuery(text);

			while (results.next()) {
				final int changeID = results.getInt(1);
				final byte[] beforeHash = results.getBytes(2);
				final byte[] afterHash = results.getBytes(3);
				final String filepath = results.getString(4);
				final int startline = results.getInt(5);
				final int endline = results.getInt(6);
				final int afterStartLine = results.getInt(7);
				final int afterEndLine = results.getInt(8);
				final String author = results.getString(9);
				final String message = results.getString(10);
				final int bugfix = results.getInt(11);
				final CHANGE_SQL change = new CHANGE_SQL(changeID, beforeHash,
						afterHash, (int) revision, filepath, startline,
						endline, afterStartLine, afterEndLine, author, message,
						0 < bugfix);
				changes.add(change);
			}

			statement.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return changes;

	}

	public List<CHANGE_SQL> getChanges(final long revision, final String path) {

		final String text = "select id, beforeHash, afterHash, filepath, "
				+ "(select start from codes where id = beforeID), "
				+ "(select end from codes where id = beforeID), "
				+ "(select start from codes where id = afterID), "
				+ "(select end from codes where id = afterID), "
				+ "(select author from revisions where number = revision), "
				+ "(select message from revisions where number = revision), "
				+ "bugfix " + "from bugfixchanges where revision = " + revision
				+ " and filepath = \'" + path + "\'";

		final List<CHANGE_SQL> changes = new ArrayList<>();

		try {
			final Statement revisionStatement = this.connector
					.createStatement();
			final ResultSet results = revisionStatement.executeQuery(text);

			while (results.next()) {
				final int changeID = results.getInt(1);
				final byte[] beforeHash = results.getBytes(2);
				final byte[] afterHash = results.getBytes(3);
				final String filepath = results.getString(4);
				final int startline = results.getInt(5);
				final int endline = results.getInt(6);
				final int afterStartLine = results.getInt(7);
				final int afterEndLine = results.getInt(8);
				final String author = results.getString(9);
				final String message = results.getString(10);
				final int bugfix = results.getInt(11);
				final CHANGE_SQL change = new CHANGE_SQL(changeID, beforeHash,
						afterHash, (int) revision, filepath, startline,
						endline, afterStartLine, afterEndLine, author, message,
						0 < bugfix);
				changes.add(change);
			}

			revisionStatement.close();
		}

		catch (final SQLException e) {
			e.printStackTrace();
		}

		return changes;
	}

	public List<PATTERN_SQL> getChangePatterns(final byte[] beforeHash,
			final byte[] afterHash) {

		final List<PATTERN_SQL> changepatterns = new ArrayList<>();

		try {

			final String sqlText = "select id, support, confidence, firstdate, lastdate, "
					+ "(select nText from codes where hash = beforeHash), "
					+ "(select nText from codes where hash = afterHash) "
					+ "from patterns where beforeHash = ? and afterHash = ?";
			final PreparedStatement statement = this.connector
					.prepareStatement(sqlText);
			statement.setBytes(1, beforeHash);
			statement.setBytes(2, afterHash);
			final ResultSet result = statement.executeQuery();

			while (result.next()) {
				final int changepatternID = result.getInt(1);
				final int support = result.getInt(2);
				final float confidence = result.getFloat(3);
				final String firstdate = result.getString(4);
				final String lastdate = result.getString(5);
				final String beforeNText = result.getString(6);
				final String afterNText = result.getString(7);

				final PATTERN_SQL changepattern = new PATTERN_SQL(
						changepatternID, support, confidence, firstdate,
						lastdate, beforeHash, afterHash, beforeNText,
						afterNText);
				changepatterns.add(changepattern);
			}

			statement.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return changepatterns;
	}

	public List<PATTERN_SQL> getFixChangePatterns() {

		final List<PATTERN_SQL> changepatterns = new ArrayList<>();

		try {

			final Statement statement = this.connector.createStatement();
			final String sql = "select id, "
					+ "support, confidence, "
					+ "firstdate, lastdate, "
					+ "beforeHash, afterHash, "
					+ "(select C1.nText from codes C1 where C1.hash = beforeHash), "
					+ "(select C2.nText from codes C2 where C2.hash = afterHash) "
					+ "from bugfixpatterns where 0 < bugfix order by support desc";
			final ResultSet result2 = statement.executeQuery(sql);
			while (result2.next()) {
				final int changepatternID = result2.getInt(1);
				final int support = result2.getInt(2);
				final float confidence = result2.getFloat(3);
				final String firstdate = result2.getString(4);
				final String lastdate = result2.getString(5);
				final byte[] beforeHash = result2.getBytes(6);
				final byte[] afterHash = result2.getBytes(7);
				final String beforeNText = result2.getString(8);
				final String afterNText = result2.getString(9);

				final PATTERN_SQL changepattern = new PATTERN_SQL(
						changepatternID, support, confidence, firstdate,
						lastdate, beforeHash, afterHash, beforeNText,
						afterNText);
				changepatterns.add(changepattern);
			}
			statement.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return changepatterns;
	}

	public int count(final String sql) {

		int count = 0;

		try {
			final Statement statement = this.connector.createStatement();
			final ResultSet results = statement.executeQuery(sql);
			if (results.next()) {
				count = results.getInt(1);
			}

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return count;
	}

	public List<CODE_SQL> getFixedCodes() {

		final List<CODE_SQL> codes = new ArrayList<>();

		try {

			final Statement statement1 = this.connector.createStatement();
			final String sql = "select nText, hash, count(hash) from codes group by hash order by count(hash) desc";
			final ResultSet result1 = statement1.executeQuery(sql);
			while (result1.next()) {
				final String nText = result1.getString(1);
				final byte[] hash = result1.getBytes(2);
				final int support = result1.getInt(3);
				final CODE_SQL code = new CODE_SQL(support, hash, nText);
				codes.add(code);
			}
			statement1.close();

			final Set<Integer> bugIDs = this.getBugIDs();
			final PreparedStatement statement2 = this.connector
					.prepareStatement("select (select R.message from revisions R where R.number = C.revision) from changes C where C.beforeHash = ?");
			CODE: for (final Iterator<CODE_SQL> iterator = codes.iterator(); iterator
					.hasNext();) {
				CODE_SQL code = iterator.next();
				statement2.setBytes(1, code.hash);
				final ResultSet result2 = statement2.executeQuery();
				while (result2.next()) {
					final String message = result2.getString(1);
					final Set<Integer> issueIDs = this.getIssueID(message);
					for (final Integer issueID : issueIDs) {
						if (bugIDs.contains(issueID)) {
							continue CODE;
						}
					}
				}
				iterator.remove();
			}

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return codes;
	}

	private Set<Integer> getIssueID(final String text) {
		final Set<Integer> numbers = new HashSet<>();
		final Matcher matcher = Pattern.compile("[0-9]{3,5}").matcher(text);
		while (matcher.find()) {
			final int startIndex = matcher.start();
			final int endIndex = matcher.end();
			final String numberText = text.substring(startIndex, endIndex);
			final Integer number = Integer.parseInt(numberText);
			numbers.add(number);
		}
		return numbers;
	}

	private Set<Integer> getBugIDs() {
		final String bugFile = FBParserConfig.getInstance().getBUG();
		final Set<Integer> ids = new HashSet<>();

		try (final BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(bugFile),
						"JISAutoDetect"))) {
			reader.readLine();
			while (true) {
				final String lineText = reader.readLine();
				if (null == lineText) {
					break;
				}
				final Integer id = Integer.parseInt(lineText);
				ids.add(id);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return ids;
	}

	public static class REVISION_SQL implements Comparable<REVISION_SQL> {

		final public int number;
		final public boolean bugfix;

		public REVISION_SQL(final int number, final boolean bugfix) {
			this.number = number;
			this.bugfix = bugfix;
		}

		@Override
		public int hashCode() {
			return this.number;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof REVISION_SQL)) {
				return false;
			}

			final REVISION_SQL target = (REVISION_SQL) o;
			return this.hashCode() == target.hashCode();
		}

		@Override
		public int compareTo(final REVISION_SQL target) {
			if (this.number < target.number) {
				return -1;
			} else if (this.number > target.number) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	public static class CHANGE_SQL {

		final public int id;
		final public byte[] beforeHash;
		final public byte[] afterHash;
		final public int revision;
		final public String filepath;
		final public int beforeStartLine;
		final public int beforeEndLine;
		final public int afterStartLine;
		final public int afterEndLine;
		final public String author;
		final public String message;
		final public boolean bugfix;

		public CHANGE_SQL(final int id, final byte[] beforeHash,
				final byte[] afterHash, final int revision,
				final String filepath, final int startline, final int endline,
				final int afterStartLine, final int afterEndLine,
				final String author, final String message, final boolean bugfix) {
			this.id = id;
			this.beforeHash = beforeHash;
			this.afterHash = afterHash;
			this.revision = revision;
			this.filepath = filepath;
			this.beforeStartLine = startline;
			this.afterStartLine = afterStartLine;
			this.afterEndLine = afterEndLine;
			this.beforeEndLine = endline;
			this.author = author;
			this.message = message;
			this.bugfix = bugfix;
		}

		@Override
		public int hashCode() {
			return this.id;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof CHANGE_SQL)) {
				return false;
			}

			final CHANGE_SQL target = (CHANGE_SQL) o;
			return this.hashCode() == target.hashCode();
		}
	}

	public static class PATTERN_SQL {

		final public int id;
		final public int support;
		final public float confidence;
		final public String firstdate;
		final public String lastdate;
		final public byte[] beforeHash;
		final public byte[] afterHash;
		final public String beforeNText;
		final public String afterNText;

		public PATTERN_SQL(final int id, final int support,
				final float confidence, final String firstdate,
				final String lastdate, final byte[] beforeHash,
				final byte[] afterHash, final String beforeNText,
				final String afterNText) {
			this.id = id;
			this.support = support;
			this.confidence = confidence;
			this.firstdate = firstdate;
			this.lastdate = lastdate;
			this.beforeHash = beforeHash;
			this.afterHash = afterHash;
			this.beforeNText = beforeNText;
			this.afterNText = afterNText;
		}
	}

	public static class CODE_SQL {

		final public int support;
		final public byte[] hash;
		final public String nText;

		public CODE_SQL(final int support, final byte[] hash, final String nText) {
			this.support = support;
			this.hash = hash;
			this.nText = nText;
		}
	}
}
