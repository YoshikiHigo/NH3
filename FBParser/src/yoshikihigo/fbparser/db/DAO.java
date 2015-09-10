package yoshikihigo.fbparser.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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

			final PreparedStatement statement = this.connector
					.prepareStatement("select id, support, confidence from patterns where beforeHash = ? and afterHash = ?");
			statement.setBytes(1, beforeHash);
			statement.setBytes(2, afterHash);
			final ResultSet result = statement.executeQuery();

			while (result.next()) {
				final int changepatternID = result.getInt(1);
				final int support = result.getInt(2);
				final float confidence = result.getFloat(3);

				final CHANGEPATTERN_SQL changepattern = new CHANGEPATTERN_SQL(
						changepatternID, support, confidence);
				changepatterns.add(changepattern);
			}
			
		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return changepatterns;
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

		public CHANGEPATTERN_SQL(final int id, final int support,
				final float confidence) {
			this.id = id;
			this.support = support;
			this.confidence = confidence;
		}
	}
}
