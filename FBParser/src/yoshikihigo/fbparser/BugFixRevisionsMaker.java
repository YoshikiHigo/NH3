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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BugFixRevisionsMaker {

	public static void main(final String[] args) {
		FBParserConfig.initialize(args);
		final BugFixRevisionsMaker main = new BugFixRevisionsMaker();
		main.make();
	}

	private void make() {

		final String BUGFIXREVISIONS_SCHEMA = "software string, "
				+ "number integer, " + "date string, " + "message string, "
				+ "author string, " + "bugfix integer, "
				+ "primary key(software, number)";
		final String database = FBParserConfig.getInstance().getDATABASE();
		final SortedSet<Integer> bugIDs = this.getBugIDs();

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
					.prepareStatement("insert into bugfixrevisions values (?, ?, ?, ?, ?, ?)");
			while (results2.next()) {
				final String software = results2.getString(1);
				final int number = results2.getInt(2);
				final String date = results2.getString(3);
				final String message = results2.getString(4);
				final String author = results2.getString(5);
				int bugfix = 0;
				final SortedSet<Integer> relatedIDs = this.extractIDs(message);
				for (final Integer id : relatedIDs) {
					if (bugIDs.contains(id)) {
						bugfix++;
					}
				}
				statement3.setString(1, software);
				statement3.setInt(2, number);
				statement3.setString(3, date);
				statement3.setString(4, message);
				statement3.setString(5, author);
				statement3.setInt(6, bugfix);
				statement3.executeUpdate();
			}
			statement2.close();
			statement3.close();

		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private SortedSet<Integer> getBugIDs() {
		final String bugFile = FBParserConfig.getInstance().getBUG();
		final SortedSet<Integer> ids = new TreeSet<>();

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

	private SortedSet<Integer> extractIDs(final String text) {
		final SortedSet<Integer> numbers = new TreeSet<>();
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
}
