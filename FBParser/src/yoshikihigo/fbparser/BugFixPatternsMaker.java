package yoshikihigo.fbparser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BugFixPatternsMaker {

	public static void main(final String[] args) {
		FBParserConfig.initialize(args);
		BugFixPatternsMaker main = new BugFixPatternsMaker();
		main.make();
	}

	private void make() {
		final String BUGFIXPATTERNS_SCHEMA = "id integer primary key, "
				+ "beforeHash blob, " + "afterHash blob, "
				+ "changetype integer, " + "difftype integer, "
				+ "support integer, " + "confidence real, "
				+ "authors integer, " + "files integer, " + "nos integer, "
				+ "bugfix integer, warningfix integer";
		final String database = FBParserConfig.getInstance().getDATABASE();

		try {
			Class.forName("org.sqlite.JDBC");
			final Connection connector = DriverManager
					.getConnection("jdbc:sqlite:" + database);

			final Statement statement1 = connector.createStatement();
			statement1
					.executeUpdate("drop index if exists index_beforeHash_bugfixpatterns");
			statement1
					.executeUpdate("drop index if exists index_afterHash_bugfixpatterns");
			statement1
					.executeUpdate("drop index if exists index_beforeHash_afterHash_bugfixpatterns");
			statement1.executeUpdate("drop table if exists bugfixpatterns");
			statement1.executeUpdate("create table bugfixpatterns ("
					+ BUGFIXPATTERNS_SCHEMA + ")");
			statement1
					.executeUpdate("create index index_beforeHash_bugfixpatterns on bugfixpatterns(beforeHash)");
			statement1
					.executeUpdate("create index index_afterHash_bugfixpatterns on bugfixpatterns(afterHash)");
			statement1
					.executeUpdate("create index index_beforeHash_afterHash_bugfixpatterns on bugfixpatterns(beforeHash, afterHash)");
			statement1.close();

			final Statement statement2 = connector.createStatement();
			final ResultSet results2 = statement2
					.executeQuery("select id, "
							+ "beforeHash, "
							+ "afterHash, "
							+ "changetype, "
							+ "difftype, "
							+ "support, "
							+ "confidence, "
							+ "authors, "
							+ "files, "
							+ "nos, "
							+ "(select sum(bugfix) from bugfixchanges C where (C.beforeHash = P.beforeHash) and (C.afterHash = P.afterHash)), "
							+ "(select sum(warningfix) from bugfixchanges C where (C.beforeHash = P.beforeHash) and (C.afterHash = P.afterHash)) "
							+ "from patterns P");
			final PreparedStatement statement3 = connector
					.prepareStatement("insert into bugfixpatterns values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			while (results2.next()) {
				final int id = results2.getInt(1);
				final byte[] beforeHash = results2.getBytes(2);
				final byte[] afterHash = results2.getBytes(3);
				final int changetype = results2.getInt(4);
				final int difftype = results2.getInt(5);
				final int support = results2.getInt(6);
				final float confidence = results2.getFloat(7);
				final int authors = results2.getInt(8);
				final int files = results2.getInt(9);
				final int nos = results2.getInt(10);
				final int bugfix = results2.getInt(11);
				final int warningfix = results2.getInt(12);
				statement3.setInt(1, id);
				statement3.setBytes(2, beforeHash);
				statement3.setBytes(3, afterHash);
				statement3.setInt(4, changetype);
				statement3.setInt(5, difftype);
				statement3.setInt(6, support);
				statement3.setFloat(7, confidence);
				statement3.setInt(8, authors);
				statement3.setInt(9, files);
				statement3.setInt(10, nos);
				statement3.setInt(11, bugfix);
				statement3.setInt(12, warningfix);
				statement3.executeUpdate();
			}
			statement2.close();
			statement3.close();

		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
