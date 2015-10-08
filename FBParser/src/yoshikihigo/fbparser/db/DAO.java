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

	public List<CHANGE_SQL> getChanges(final long revision, final String path) {

		final StringBuilder sqlText = new StringBuilder();
		sqlText.append("select id, beforeHash, afterHash, ");
		sqlText.append("(select start from codes where id = beforeID), ");
		sqlText.append("(select end from codes where id = beforeID) ");
		sqlText.append("from changes where revision = ");
		sqlText.append(revision);
		sqlText.append(" and filepath = \'");
		sqlText.append(path);
		sqlText.append("\'");

		final List<CHANGE_SQL> changes = new ArrayList<>();

		try {
			final Statement revisionStatement = this.connector
					.createStatement();
			final ResultSet result = revisionStatement.executeQuery(sqlText
					.toString());

			while (result.next()) {
				final int changeID = result.getInt(1);
				final byte[] beforeHash = result.getBytes(2);
				final byte[] afterHash = result.getBytes(3);
				final int startline = result.getInt(4);
				final int endline = result.getInt(5);
				final CHANGE_SQL change = new CHANGE_SQL(changeID, beforeHash,
						afterHash, startline, endline);
				changes.add(change);
			}

			revisionStatement.close();
		}

		catch (final SQLException e) {
			e.printStackTrace();
		}

		return changes;
	}

	public List<CHANGEPATTERN_SQL> getChangePatterns(final byte[] beforeHash,
			final byte[] afterHash) {

		final List<CHANGEPATTERN_SQL> changepatterns = new ArrayList<>();

		try {

			final String sqlText = "select id, support, confidence, "
					+ "(select text from Codes where hash = beforeHash), "
					+ "(select text from Codes where hash = afterHash) "
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
				final String beforeText = result.getString(4);
				final String afterText = result.getString(5);

				final CHANGEPATTERN_SQL changepattern = new CHANGEPATTERN_SQL(
						changepatternID, support, confidence, beforeHash,
						afterHash, beforeText, afterText);
				changepatterns.add(changepattern);
			}

			statement.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return changepatterns;
	}

	public List<CHANGEPATTERN_SQL> getFixChangePatterns() {

		final List<CHANGEPATTERN_SQL> changepatterns = new ArrayList<>();

		try {

			final Statement statement1 = this.connector.createStatement();
			final String sql = "select id, support, confidence, beforeHash, afterHash, "
					+ "(select C1.text from codes C1 where C1.hash = beforeHash), "
					+ "(select C2.text from codes C2 where C2.hash = afterHash) "
					+ "from patterns order by support desc";
			final ResultSet result1 = statement1.executeQuery(sql);
			while (result1.next()) {
				final int changepatternID = result1.getInt(1);
				final int support = result1.getInt(2);
				final float confidence = result1.getFloat(3);
				final byte[] beforeHash = result1.getBytes(4);
				final byte[] afterHash = result1.getBytes(5);
				final String beforeText = result1.getString(6);
				final String afterText = result1.getString(7);

				final CHANGEPATTERN_SQL changepattern = new CHANGEPATTERN_SQL(
						changepatternID, support, confidence, beforeHash,
						afterHash, beforeText, afterText);
				changepatterns.add(changepattern);
			}
			statement1.close();

			final Set<Integer> bugIDs = this.getBugIDs();
			final PreparedStatement statement2 = this.connector
					.prepareStatement("select C.revision, (select R.message from revisions R where R.number = C.revision) from changes C where C.beforeHash = ? and C.afterHash = ?");
			CODE: for (final Iterator<CHANGEPATTERN_SQL> iterator = changepatterns
					.iterator(); iterator.hasNext();) {
				CHANGEPATTERN_SQL cp = iterator.next();
				statement2.setBytes(1, cp.beforeHash);
				statement2.setBytes(2, cp.afterHash);
				final ResultSet result2 = statement2.executeQuery();
				while (result2.next()) {
					final int revision = result2.getInt(1);
					final String message = result2.getString(2);
					final Set<Integer> issueIDs = this.getIssueID(message);
					for (final Integer issueID : issueIDs) {
						if (bugIDs.contains(issueID)) {
							cp.revisions.add(revision);
							continue CODE;
						}
					}
				}
				iterator.remove();
			}
			statement2.close();

			final PreparedStatement statement3 = this.connector
					.prepareStatement("select count(*) from changes where revision = ? and beforeHash = ? and afterHash = ?");
			for (final CHANGEPATTERN_SQL cp : changepatterns) {
				for (final Integer revision : cp.revisions) {
					statement3.setInt(1, revision);
					statement3.setBytes(2, cp.beforeHash);
					statement3.setBytes(3, cp.afterHash);
					final ResultSet result3 = statement3.executeQuery();
					assert result3.next() : "SQL execution failure.";
					final int count = result3.getInt(1);
					cp.bugfixSupport += count;
				}
			}

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return changepatterns;
	}

	public List<CODE_SQL> getFixedCodes() {

		final List<CODE_SQL> codes = new ArrayList<>();

		try {

			final Statement statement1 = this.connector.createStatement();
			final String sql = "select text, hash, count(hash) from codes group by hash order by count(hash) desc";
			final ResultSet result1 = statement1.executeQuery(sql);
			while (result1.next()) {
				final String text = result1.getString(1);
				final byte[] hash = result1.getBytes(2);
				final int support = result1.getInt(3);
				final CODE_SQL code = new CODE_SQL(support, hash, text);
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

	public static class CHANGE_SQL {

		final public int id;
		final public byte[] beforeHash;
		final public byte[] afterHash;
		final public int startline;
		final public int endline;

		public CHANGE_SQL(final int id, final byte[] beforeHash,
				final byte[] afterHash, final int startline, final int endline) {
			this.id = id;
			this.beforeHash = beforeHash;
			this.afterHash = afterHash;
			this.startline = startline;
			this.endline = endline;
		}
	}

	public static class CHANGEPATTERN_SQL {

		final public int id;
		final public int support;
		final public float confidence;
		final public byte[] beforeHash;
		final public byte[] afterHash;
		final public String beforeText;
		final public String afterText;
		final public List<Integer> revisions;
		public int bugfixSupport;

		public CHANGEPATTERN_SQL(final int id, final int support,
				final float confidence, final byte[] beforeHash,
				final byte[] afterHash, final String beforeText,
				final String afterText) {
			this.id = id;
			this.support = support;
			this.confidence = confidence;
			this.beforeHash = beforeHash;
			this.afterHash = afterHash;
			this.beforeText = beforeText;
			this.afterText = afterText;
			this.revisions = new ArrayList<>();
			this.bugfixSupport = 0;
		}
	}

	public static class CODE_SQL {

		final public int support;
		final public byte[] hash;
		final public String text;

		public CODE_SQL(final int support, final byte[] hash, final String text) {
			this.support = support;
			this.hash = hash;
			this.text = text;
		}
	}
}
