package yoshikihigo.fbparser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BugFixChangesMaker {

	public static void main(final String[] args) {
		FBParserConfig.initialize(args);
		BugFixChangesMaker main = new BugFixChangesMaker();
		main.make();
	}

	private void make() {
		final String BUGFIXCHANGES_SCHEMA = "software string, "
				+ "id integer, " + "filepath string, " + "beforeID integer, "
				+ "beforeHash blob, " + "afterID integer, "
				+ "afterHash blob, " + "revision string, " + "date string, "
				+ "changetype integer, " + "difftype integer, "
				+ "bugfix integer, " + "warningfix integer, "
				+ "primary key(software, id)";
		final String database = FBParserConfig.getInstance().getDATABASE();

		try {
			Class.forName("org.sqlite.JDBC");
			final Connection connector = DriverManager
					.getConnection("jdbc:sqlite:" + database);

			final Statement statement1 = connector.createStatement();
			statement1
			.executeUpdate("drop index if exists index_revision_bugfixchanges");
			statement1
					.executeUpdate("drop index if exists index_beforeHash_bugfixchanges");
			statement1
					.executeUpdate("drop index if exists index_afterHash_bugfixchanges");
			statement1
					.executeUpdate("drop index if exists index_beforeHash_afterHash_bugfixchanges");
			statement1
					.executeUpdate("drop index if exists index_bugfix_bugfixchanges");
			statement1
					.executeUpdate("drop index if exists index_warningfix_bugfixchanges");
			statement1.executeUpdate("drop table if exists bugfixchanges");
			statement1.executeUpdate("create table bugfixchanges ("
					+ BUGFIXCHANGES_SCHEMA + ")");
			statement1
			.executeUpdate("create index index_revision_bugfixchanges on bugfixchanges(revision)");
			statement1
					.executeUpdate("create index index_beforeHash_bugfixchanges on bugfixchanges(beforeHash)");
			statement1
					.executeUpdate("create index index_afterHash_bugfixchanges on bugfixchanges(afterHash)");
			statement1
					.executeUpdate("create index index_beforeHash_afterHash_bugfixchanges on bugfixchanges(beforeHash, afterHash)");
			statement1
					.executeUpdate("create index index_bugfix_bugfixchanges on bugfixchanges(bugfix)");
			statement1
					.executeUpdate("create index index_warningfix_bugfixchanges on bugfixchanges(warningfix)");
			statement1.close();

			final Statement statement2 = connector.createStatement();
			final ResultSet results2 = statement2
					.executeQuery("select C.software, "
							+ "C.id, "
							+ "C.filepath, "
							+ "C.beforeID, "
							+ "C.beforeHash, "
							+ "C.afterID, "
							+ "C.afterHash, "
							+ "C.revision, "
							+ "C.date, "
							+ "C.changetype, "
							+ "C.difftype, "
							+ "(select R.bugfix from bugfixrevisions R where R.id = C.revision) "
							+ "from changes C");
			final PreparedStatement statement3 = connector
					.prepareStatement("insert into bugfixchanges values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			while (results2.next()) {
				final String software = results2.getString(1);
				final int id = results2.getInt(2);
				final String filepath = results2.getString(3);
				final int beforeID = results2.getInt(4);
				final byte[] beforeHash = results2.getBytes(5);
				final int afterID = results2.getInt(6);
				final byte[] afterHash = results2.getBytes(7);
				final String revision = results2.getString(8);
				final String date = results2.getString(9);
				final int changetype = results2.getInt(10);
				final int difftype = results2.getInt(11);
				final int bugfix = results2.getInt(12);
				final int warningfix = 0;
				statement3.setString(1, software);
				statement3.setInt(2, id);
				statement3.setString(3, filepath);
				statement3.setInt(4, beforeID);
				statement3.setBytes(5, beforeHash);
				statement3.setInt(6, afterID);
				statement3.setBytes(7, afterHash);
				statement3.setString(8, revision);
				statement3.setString(9, date);
				statement3.setInt(10, changetype);
				statement3.setInt(11, difftype);
				statement3.setInt(12, bugfix);
				statement3.setInt(13, warningfix);
				statement3.executeUpdate();
			}
			statement2.close();
			statement3.close();

		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
