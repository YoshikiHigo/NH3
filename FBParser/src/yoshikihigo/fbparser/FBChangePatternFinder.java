package yoshikihigo.fbparser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.StringTokenizer;

import yoshikihigo.fbparser.db.DAO;
import yoshikihigo.fbparser.db.DAO.CHANGEPATTERN_SQL;
import yoshikihigo.fbparser.db.DAO.CHANGE_SQL;

public class FBChangePatternFinder {

	public static void main(final String[] args) {

		FBParserConfig.initialize(args);
		final String trFile = FBParserConfig.getInstance()
				.getTRANSITIONRESULT();
		final String cpFile = FBParserConfig.getInstance().getCHANGEPATTERN();
		final DAO dao = DAO.getInstance();

		try (final BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(trFile),
						"JISAutoDetect"));
				final PrintWriter writer = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(cpFile),
								"UTF-8")))) {

			final String trTitle = reader.readLine();
			writer.print(trTitle);
			writer.println(", CHANGEPATTERN-ID, CHANGEPATTERN-SUPPORT");

			while (true) {
				final String lineText = reader.readLine();
				if (null == lineText) {
					break;
				}
				final Line line = new Line(lineText);
				if (line.status.startsWith("removed")
						&& (0 < line.startstartline) && (0 < line.startendline)) {
					final List<CHANGE_SQL> changes = dao.getChanges(
							line.endrev + 1, line.path);
					for (final CHANGE_SQL change : changes) {

						if (change.endline < line.startstartline) {
							continue;
						}

						if (line.startendline < change.startline) {
							continue;
						}

						System.out.println("----------" + line.hash
								+ "----------");
						final List<CHANGEPATTERN_SQL> cps = dao
								.getChangePatterns(change.beforeHash,
										change.afterHash);
						for (final CHANGEPATTERN_SQL cp : cps) {
							System.out.println(cp.id);
							writer.print(lineText);
							writer.print(", ");
							writer.print(cp.id);
							writer.print(", ");
							writer.println(cp.support);
						}
					}
				}
			}

		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
}

class Line {

	final String hash;
	final String type;
	final int rank;
	final int priority;
	final String status;
	final long startrev;
	final long endrev;
	final String path;
	final int startstartline;
	final int startendline;
	final int endstartline;
	final int endendline;

	Line(final String lineText) {
		final StringTokenizer tokenizer = new StringTokenizer(lineText, ", ");
		this.hash = tokenizer.nextToken();
		this.type = tokenizer.nextToken();
		this.rank = Integer.parseInt(tokenizer.nextToken());
		this.priority = Integer.parseInt(tokenizer.nextToken());
		this.status = tokenizer.nextToken();
		this.startrev = Long.parseLong(tokenizer.nextToken());
		this.endrev = Long.parseLong(tokenizer.nextToken());
		this.path = tokenizer.nextToken();
		final String startpos = tokenizer.nextToken();
		final String endpos = tokenizer.nextToken();
		if (startpos.equals("no-line-information")) {
			this.startstartline = 0;
			this.startendline = 0;
		} else {
			this.startstartline = Integer.parseInt(startpos.substring(0,
					startpos.indexOf('-')));
			this.startendline = Integer.parseInt(startpos.substring(startpos
					.lastIndexOf('-') + 1));
		}
		if (endpos.equals("no-line-information")) {
			this.endstartline = 0;
			this.endendline = 0;
		} else {
			this.endstartline = Integer.parseInt(endpos.substring(0,
					endpos.indexOf('-')));
			this.endendline = Integer.parseInt(endpos.substring(endpos
					.lastIndexOf('-') + 1));
		}
	}
}
