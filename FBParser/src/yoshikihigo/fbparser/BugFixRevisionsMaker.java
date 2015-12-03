package yoshikihigo.fbparser;

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
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class BugFixRevisionsMaker {

	public static void main(final String[] args) {
		FBParserConfig.initialize(args);
		final BugFixRevisionsMaker main = new BugFixRevisionsMaker();
		main.make();
	}

	private void make() {

		final String BUGFIXREVISIONS_SCHEMA = "software string, "
				+ "number integer, " + "date string, " + "message string, "
				+ "author string, " + "bugfix integer, " + "info string, "
				+ "primary key(software, number)";
		final String database = FBParserConfig.getInstance().getDATABASE();
		final SortedMap<String, String> bugIDs = this.getBugIDs();

		try {
			Class.forName("org.sqlite.JDBC");
			final Connection connector = DriverManager
					.getConnection("jdbc:sqlite:" + database);

			final Statement statement1 = connector.createStatement();
			statement1
					.executeUpdate("drop index if exists index_number_bugfixrevisions");
			statement1
					.executeUpdate("drop index if exists index_bugfix_bugfixrevisions");
			statement1.executeUpdate("drop table if exists bugfixrevisions");
			statement1.executeUpdate("create table bugfixrevisions ("
					+ BUGFIXREVISIONS_SCHEMA + ")");
			statement1
					.executeUpdate("create index index_number_bugfixrevisions on bugfixrevisions(number)");
			statement1
					.executeUpdate("create index index_bugfix_bugfixrevisions on bugfixrevisions(bugfix)");
			statement1.close();

			final Statement statement2 = connector.createStatement();
			final ResultSet results2 = statement2
					.executeQuery("select software, number, date, message, author from revisions");
			final PreparedStatement statement3 = connector
					.prepareStatement("insert into bugfixrevisions values (?, ?, ?, ?, ?, ?, ?)");
			while (results2.next()) {
				final String software = results2.getString(1);
				final int number = results2.getInt(2);
				final String date = results2.getString(3);
				final String message = results2.getString(4);
				final String author = results2.getString(5);

				int bugfix = 0;
				final StringBuilder urls = new StringBuilder();
				for (final Entry<String, String> entry : bugIDs.entrySet()) {
					final String id = entry.getKey();
					if (message.contains(id)) {
						bugfix++;
						final String url = entry.getValue();
						urls.append(url);
						urls.append(System.lineSeparator());
					}
				}

				statement3.setString(1, software);
				statement3.setInt(2, number);
				statement3.setString(3, date);
				statement3.setString(4, message);
				statement3.setString(5, author);
				statement3.setInt(6, bugfix);
				statement3.setString(7, urls.toString());
				statement3.executeUpdate();
			}
			statement2.close();
			statement3.close();

		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private SortedMap<String, String> getBugIDs() {
		final String bugFile = FBParserConfig.getInstance().getBUG();
		final SortedMap<String, String> ids = new TreeMap<>();

		try (final BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(bugFile),
						"JISAutoDetect"))) {
			reader.readLine();
			while (true) {
				final String lineText = reader.readLine();
				if (null == lineText) {
					break;
				}

				final StringTokenizer tokenizer = new StringTokenizer(lineText,
						" ,");
				final String id = tokenizer.nextToken();
				final String url = tokenizer.nextToken();
				ids.put(id, url);
			}
		}

		catch (final IOException e) {
			e.printStackTrace();
		}

		return ids;
	}
}
