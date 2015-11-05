package yoshikihigo.fbparser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import yoshikihigo.cpanalyzer.CPAConfig;
import yoshikihigo.cpanalyzer.LANGUAGE;
import yoshikihigo.cpanalyzer.StringUtility;
import yoshikihigo.fbparser.db.DAO;
import yoshikihigo.fbparser.db.DAO.CHANGEPATTERN_SQL;
import yoshikihigo.fbparser.db.DAO.CHANGE_SQL;
import yoshikihigo.fbparser.db.DAO.REVISION_SQL;

public class FBChangePatternFinder {

	public static void main(final String[] args) {

		FBParserConfig.initialize(args);
		final String trFile = FBParserConfig.getInstance()
				.getTRANSITIONRESULT();
		final String cpFile = FBParserConfig.getInstance().getCHANGEPATTERN();
		final String mcpFile = FBParserConfig.getInstance()
				.getFIXCHANGEPATTERN();
		final DAO dao = DAO.getInstance();

		Cell firstCell = null;
		Cell lastCell = null;

		try (final BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(trFile),
						"JISAutoDetect"));
				final PrintWriter cpWriter = new PrintWriter(
						new BufferedWriter(new OutputStreamWriter(
								new FileOutputStream(cpFile), "UTF-8")));
				final Workbook book = new XSSFWorkbook();
				final OutputStream stream = new FileOutputStream(mcpFile)) {

			final String trTitle = reader.readLine();
			cpWriter.print(trTitle);
			cpWriter.println(", CHANGEPATTERN-ID, CHANGEPATTERN-SUPPORT");

			final Set<Integer> foundCPs = new HashSet<>();
			final Set<String> foundCodes = new HashSet<>();
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

						// System.out.println("----------" + line.hash
						// + "----------");
						final List<CHANGEPATTERN_SQL> cps = dao
								.getChangePatterns(change.beforeHash,
										change.afterHash);
						for (final CHANGEPATTERN_SQL cp : cps) {
							// System.out.println(cp.id);
							cpWriter.print(lineText);
							cpWriter.print(", ");
							cpWriter.print(cp.id);
							cpWriter.print(", ");
							cpWriter.println(getChanges(cp).size());
							foundCPs.add(cp.id);
							foundCodes.add(cp.beforeText);
						}
					}
				}
			}

			{
				final Sheet sheet = book.createSheet();
				book.setSheetName(0, "change patterns");
				final Row titleRow = sheet.createRow(0);
				titleRow.createCell(0).setCellValue("CHANGE-PATTERN-ID");
				titleRow.createCell(1).setCellValue("FOUND-BY-FINDBUGS");
				titleRow.createCell(2).setCellValue("AUTHORS");
				titleRow.createCell(3).setCellValue("BUG-FIX-AUTHORS");
				titleRow.createCell(4).setCellValue("FILES");
				titleRow.createCell(5).setCellValue("BUG-FIX-FILES");
				titleRow.createCell(6).setCellValue("SUPPORT");
				setCellComment(titleRow.getCell(6), "Higo",
						"the number of occurences of a given pattern", 4, 1);
				titleRow.createCell(7).setCellValue("BUG-FIX-SUPPORT");
				setCellComment(
						titleRow.getCell(7),
						"Higo",
						"the number of occurences of a given pattern in bug-fix commits",
						4, 1);
				titleRow.createCell(8).setCellValue("BEFORE-TEXT-SUPPORT");
				setCellComment(
						titleRow.getCell(8),
						"Higo",
						"the number of code fragments whose texts are "
								+ "identical to before-text of a given pattern "
								+ "in the commit where the pattern appears initially",
						4, 2);
				titleRow.createCell(9).setCellValue("CONFIDENCE1");
				setCellComment(titleRow.getCell(9), "Higo",
						"BUG-FIX-SUPPORT / SUPPORT", 4, 1);
				titleRow.createCell(10).setCellValue("CONFIDENCE2");
				setCellComment(titleRow.getCell(10), "Higo",
						"SUPPORT / BEFORE-TEXT-SUPPORT", 4, 1);
				titleRow.createCell(11).setCellValue("CONFIDENCE3");
				setCellComment(titleRow.getCell(11), "Higo",
						"BUG-FIX-SUPPORT / BEFORE-TEXT-SUPPORT", 4, 1);
				titleRow.createCell(12).setCellValue("COMMITS");
				setCellComment(titleRow.getCell(12), "Higo",
						"the number of commits where the pattern appears", 4, 1);
				titleRow.createCell(13).setCellValue("BUG-FIX-COMMIT");
				setCellComment(
						titleRow.getCell(13),
						"Higo",
						"the number of bug-fix commits where the pattern appears",
						4, 1);
				titleRow.createCell(14).setCellValue("FIRST-DATE");
				titleRow.createCell(15).setCellValue("LAST-DATE");
				titleRow.createCell(16).setCellValue("DATE-DIFFERENCE");
				titleRow.createCell(17).setCellValue("OCCUPANCY");
				setCellComment(
						titleRow.getCell(17),
						"Higo",
						"average of (LOC of a given pattern changed in revision R) / "
								+ "(total LOC changed in revision R) "
								+ "for all the revisions where the pattern appears",
						4, 2);
				titleRow.createCell(18).setCellValue("Delta-TFIDF");
				setCellComment(
						titleRow.getCell(18),
						"Higo",
						"delta-TFIDF was calculated with the following formula"
								+ System.lineSeparator()
								+ "(k1 + 1)*tf/(K_tf) log (((N1 - df1 + 0.5)*(df2 + 0.5))/((N2 - df2 + 0.5)*(df1 + 0.5)))"
								+ System.lineSeparator()
								+ "tf : the number of occurrences of a given a pattern"
								+ System.lineSeparator()
								+ "N1: the number of all files changed in bug-fix commits"
								+ System.lineSeparator()
								+ "N2: the number of all files changed in non-bug-fix commits"
								+ System.lineSeparator()
								+ "df1: the number of files changed in bug-fix commits for a given pattern"
								+ System.lineSeparator()
								+ "df2: the number of files changed in non-bug-fix commits for a given pattern"
								+ System.lineSeparator()
								+ "k1: 1.2 (parameter)"
								+ System.lineSeparator() + "K: 1.2 (parameter)",
						5, 4);
				titleRow.createCell(19).setCellValue("TEXT-BEFORE-CHANGE");
				titleRow.createCell(20).setCellValue("TEXT-AFTER-CHANGE");
				firstCell = titleRow.getCell(0);

				int currentRow = 1;
				final List<CHANGEPATTERN_SQL> cps = dao.getFixChangePatterns();
				Collections
						.sort(cps, (o1, o2) -> Integer.compare(o1.id, o2.id));

				for (final CHANGEPATTERN_SQL cp : cps) {

					if (cp.beforeText.isEmpty()) {
						continue;
					}

					final boolean foundByFindBugs = foundCPs.contains(cp.id);

					final Row dataRow = sheet.createRow(currentRow++);
					dataRow.createCell(0).setCellValue(cp.id);
					dataRow.createCell(1).setCellValue(
							foundByFindBugs ? "YES" : "NO");
					dataRow.createCell(2).setCellValue(getAuthors(cp).size());
					dataRow.createCell(3).setCellValue(
							getAuthors(cp, true).size());
					dataRow.createCell(4).setCellValue(getFiles(cp).size());
					dataRow.createCell(5).setCellValue(
							getFiles(cp, true).size());
					final int support = getChanges(cp).size();
					final int bugfixSupport = getChanges(cp, true).size();
					final int beforeTextSupport = countTextAppearances(cp);
					dataRow.createCell(6).setCellValue(support);
					dataRow.createCell(7).setCellValue(bugfixSupport);
					dataRow.createCell(8).setCellValue(beforeTextSupport);
					dataRow.createCell(9).setCellValue(
							(float) bugfixSupport / (float) support);
					dataRow.createCell(10).setCellValue(
							(float) support / (float) beforeTextSupport);
					dataRow.createCell(11).setCellValue(
							(float) bugfixSupport / (float) beforeTextSupport);
					dataRow.createCell(12).setCellValue(getCommits(cp, false));
					dataRow.createCell(13).setCellValue(getCommits(cp, true));
					dataRow.createCell(14).setCellValue(cp.firstdate);
					dataRow.createCell(15).setCellValue(cp.lastdate);
					dataRow.createCell(16).setCellValue(
							getDayDifference(cp.firstdate, cp.lastdate));
					dataRow.createCell(17).setCellValue(getOccupancy(cp));
					dataRow.createCell(18).setCellValue(getDeltaTFIDF(cp));
					dataRow.createCell(19).setCellValue(cp.beforeText);
					dataRow.createCell(20).setCellValue(cp.afterText);
					lastCell = dataRow.getCell(20);

					final CellStyle style = book.createCellStyle();
					style.setWrapText(true);
					style.setFillPattern(CellStyle.SOLID_FOREGROUND);
					style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
					style.setBorderBottom(XSSFCellStyle.BORDER_THIN);
					style.setBorderLeft(XSSFCellStyle.BORDER_THIN);
					style.setBorderRight(XSSFCellStyle.BORDER_THIN);
					style.setBorderTop(XSSFCellStyle.BORDER_THIN);
					dataRow.getCell(0).setCellStyle(style);
					dataRow.getCell(1).setCellStyle(style);
					dataRow.getCell(2).setCellStyle(style);
					dataRow.getCell(3).setCellStyle(style);
					dataRow.getCell(4).setCellStyle(style);
					dataRow.getCell(5).setCellStyle(style);
					dataRow.getCell(6).setCellStyle(style);
					dataRow.getCell(7).setCellStyle(style);
					dataRow.getCell(8).setCellStyle(style);
					dataRow.getCell(9).setCellStyle(style);
					dataRow.getCell(10).setCellStyle(style);
					dataRow.getCell(11).setCellStyle(style);
					dataRow.getCell(12).setCellStyle(style);
					dataRow.getCell(13).setCellStyle(style);
					dataRow.getCell(14).setCellStyle(style);
					dataRow.getCell(15).setCellStyle(style);
					dataRow.getCell(16).setCellStyle(style);
					dataRow.getCell(17).setCellStyle(style);
					dataRow.getCell(18).setCellStyle(style);
					dataRow.getCell(19).setCellStyle(style);
					dataRow.getCell(20).setCellStyle(style);

					int loc = Math.max(getLOC(cp.beforeText),
							getLOC(cp.afterText));
					dataRow.setHeight((short) (loc * dataRow.getHeight()));
				}
				sheet.autoSizeColumn(0, true);
				sheet.autoSizeColumn(1, true);
				sheet.autoSizeColumn(2, true);
				sheet.autoSizeColumn(3, true);
				sheet.autoSizeColumn(4, true);
				sheet.autoSizeColumn(5, true);
				sheet.autoSizeColumn(6, true);
				sheet.autoSizeColumn(7, true);
				sheet.autoSizeColumn(8, true);
				sheet.autoSizeColumn(9, true);
				sheet.autoSizeColumn(10, true);
				sheet.autoSizeColumn(11, true);
				sheet.autoSizeColumn(12, true);
				sheet.autoSizeColumn(13, true);
				sheet.autoSizeColumn(14, true);
				sheet.autoSizeColumn(15, true);
				sheet.autoSizeColumn(16, true);
				sheet.autoSizeColumn(17, true);
				sheet.autoSizeColumn(18, true);
				sheet.setColumnWidth(19, 20480);
				sheet.setColumnWidth(20, 20480);

				sheet.setAutoFilter(new CellRangeAddress(firstCell
						.getRowIndex(), lastCell.getRowIndex(), firstCell
						.getColumnIndex(), lastCell.getColumnIndex()));
				sheet.createFreezePane(0, 1, 0, 1);
			}

			book.write(stream);

		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private static void setCellComment(final Cell cell, final String author,
			final String text, final int width, final int height) {

		final Sheet sheet = cell.getSheet();
		final Workbook workbook = sheet.getWorkbook();
		final CreationHelper helper = workbook.getCreationHelper();

		final Drawing drawing = sheet.createDrawingPatriarch();
		final ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, (short) 4,
				2, (short) (4 + width), (2 + height));
		final Comment comment = drawing.createCellComment(anchor);
		comment.setAuthor(author);
		comment.setString(helper.createRichTextString(text));
		cell.setCellComment(comment);
	}

	private static int getLOC(final String text) {

		int count = 0;
		final String newline = System.lineSeparator();
		final Matcher matcher = Pattern.compile(newline).matcher(text);
		while (matcher.find()) {
			count++;
		}
		return count + 1;
	}

	private static int getDayDifference(final String firstdate,
			final String lastdate) {

		final Calendar calendar1 = Calendar.getInstance();
		{
			final StringTokenizer tokenizer1 = new StringTokenizer(firstdate,
					" :/");
			final String year = tokenizer1.nextToken();
			final String month = tokenizer1.nextToken();
			final String date = tokenizer1.nextToken();
			final String hour = tokenizer1.nextToken();
			final String minute = tokenizer1.nextToken();
			final String second = tokenizer1.nextToken();
			calendar1.set(Integer.parseInt(year), Integer.parseInt(month),
					Integer.parseInt(date), Integer.parseInt(hour),
					Integer.parseInt(minute), Integer.parseInt(second));
		}

		final Calendar calendar2 = Calendar.getInstance();
		{
			final StringTokenizer tokenizer1 = new StringTokenizer(lastdate,
					" :/");
			final String year = tokenizer1.nextToken();
			final String month = tokenizer1.nextToken();
			final String date = tokenizer1.nextToken();
			final String hour = tokenizer1.nextToken();
			final String minute = tokenizer1.nextToken();
			final String second = tokenizer1.nextToken();
			calendar2.set(Integer.parseInt(year), Integer.parseInt(month),
					Integer.parseInt(date), Integer.parseInt(hour),
					Integer.parseInt(minute), Integer.parseInt(second));
		}

		final long difference = calendar2.getTime().getTime()
				- calendar1.getTime().getTime();
		return (int) (difference / 1000l / 60l / 60l / 24l);
	}

	private static int getCommits(final CHANGEPATTERN_SQL cp,
			final boolean onlyBugfix) {
		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		final List<CHANGE_SQL> changesInPattern = DAO.getInstance().getChanges(
				beforeHash, afterHash);
		final SortedSet<Integer> revisions = new TreeSet<>();
		for (final CHANGE_SQL change : changesInPattern) {
			if (!onlyBugfix || (onlyBugfix && change.bugfix)) {
				revisions.add(change.revision);
			}
		}

		return revisions.size();
	}

	private static float getOccupancy(final CHANGEPATTERN_SQL cp) {

		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		final List<CHANGE_SQL> changesInPattern = DAO.getInstance().getChanges(
				beforeHash, afterHash);
		final Map<REVISION_SQL, AtomicInteger> map1 = new HashMap<>();
		final Map<REVISION_SQL, AtomicInteger> map2 = new HashMap<>();

		final SortedSet<REVISION_SQL> revisions = DAO.getInstance()
				.getRevisions(beforeHash, afterHash);
		for (final REVISION_SQL revision : revisions) {
			map1.put(revision, new AtomicInteger(0));
			map2.put(revision, new AtomicInteger(0));
			List<CHANGE_SQL> changesInRevision = DAO.getInstance().getChanges(
					revision.number);
			for (final CHANGE_SQL change : changesInRevision) {
				if (changesInPattern.contains(change)) {
					AtomicInteger size = map1.get(revision);
					size.addAndGet(change.endline - change.startline + 1);
				}

				AtomicInteger size = map2.get(revision);
				size.addAndGet(change.endline - change.startline + 1);
			}
		}

		float sum = 0;
		for (final REVISION_SQL revision : revisions) {
			final AtomicInteger numerator = map1.get(revision);
			final AtomicInteger denominator = map2.get(revision);
			final float occupancy = (float) numerator.get()
					/ (float) denominator.get();
			sum += occupancy;
		}

		return sum / revisions.size();
	}

	private static double getDeltaTFIDF(final CHANGEPATTERN_SQL cp) {

		final double tf = (double) getChanges(cp).size(); // all occurrences
		final int n1 = DAO
				.getInstance()
				.count("select count(distinct filepath) from bugfixchanges where 0 < bugfix");
		final int n2 = DAO
				.getInstance()
				.count("select count(distinct filepath) from bugfixchanges where 0 = bugfix");
		final int df1 = getFiles(cp, true).size(); // the number of files
													// including bug-fix changes
		final int df2 = getFiles(cp, false).size(); // the number of files
													// including non-bug-fix
													// changes
		final double k1 = 1.2d;
		final double K = 1.2d;

		final double w = ((k1 + 1) * tf / (K + tf))
				* (Math.log(((n1 - df1 + 0.5) * (df2 + 0.5))
						/ ((n2 - df2 + 0.5) * (df1 + 0.5))) / Math.log(2.0));
		return w;
	}

	private static List<CHANGE_SQL> getChanges(final CHANGEPATTERN_SQL cp) {

		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		return DAO.getInstance().getChanges(beforeHash, afterHash);
	}

	private static List<CHANGE_SQL> getChanges(final CHANGEPATTERN_SQL cp,
			final boolean bugfix) {

		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		final List<CHANGE_SQL> changes = DAO.getInstance().getChanges(
				beforeHash, afterHash);
		final Iterator<CHANGE_SQL> iterator = changes.iterator();
		while (iterator.hasNext()) {
			final CHANGE_SQL change = iterator.next();
			if (bugfix && !change.bugfix || !bugfix && change.bugfix) {
				iterator.remove();
			}
		}

		return changes;
	}

	private static SortedSet<String> getAuthors(final CHANGEPATTERN_SQL cp) {

		final SortedSet<String> authors = new TreeSet<>();
		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		final List<CHANGE_SQL> changes = DAO.getInstance().getChanges(
				beforeHash, afterHash);
		for (final CHANGE_SQL change : changes) {
			authors.add(change.author);
		}

		return authors;
	}

	private static SortedSet<String> getAuthors(final CHANGEPATTERN_SQL cp,
			final boolean bugfix) {

		final SortedSet<String> authors = new TreeSet<>();
		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		final List<CHANGE_SQL> changes = DAO.getInstance().getChanges(
				beforeHash, afterHash);
		for (final CHANGE_SQL change : changes) {
			if ((bugfix && change.bugfix) || (!bugfix && !change.bugfix)) {
				authors.add(change.author);
			}
		}

		return authors;
	}

	private static SortedSet<String> getFiles(final CHANGEPATTERN_SQL cp) {

		final SortedSet<String> files = new TreeSet<>();
		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		final List<CHANGE_SQL> changes = DAO.getInstance().getChanges(
				beforeHash, afterHash);
		for (final CHANGE_SQL change : changes) {
			files.add(change.filepath);
		}

		return files;
	}

	private static SortedSet<String> getFiles(final CHANGEPATTERN_SQL cp,
			final boolean bugfix) {

		final SortedSet<String> files = new TreeSet<>();
		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		final List<CHANGE_SQL> changes = DAO.getInstance().getChanges(
				beforeHash, afterHash);
		for (final CHANGE_SQL change : changes) {
			if ((bugfix && change.bugfix) || (!bugfix && !change.bugfix)) {
				files.add(change.filepath);
			}
		}

		return files;
	}

	private static int countTextAppearances(final CHANGEPATTERN_SQL cp) {

		CPAConfig.initialize(new String[] { "-n" });
		final List<yoshikihigo.cpanalyzer.data.Statement> pattern = StringUtility
				.splitToStatements(cp.beforeText, 1, 1);
		int count = 0;

		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		final SortedSet<REVISION_SQL> revisions = DAO.getInstance()
				.getRevisions(beforeHash, afterHash);
		final int firstRevision = revisions.first().number - 1;
		final List<List<yoshikihigo.cpanalyzer.data.Statement>> contents = getFileContents(firstRevision);
		for (final List<yoshikihigo.cpanalyzer.data.Statement> content : contents) {
			count += getCount(content, pattern);
		}

		return count;
	}

	private static List<List<yoshikihigo.cpanalyzer.data.Statement>> getFileContents(
			final int revision) {

		try {

			final String repository = FBParserConfig.getInstance()
					.getREPOSITORY();

			final SVNLogClient logClient = SVNClientManager.newInstance()
					.getLogClient();
			final SVNURL url = SVNURL.fromFile(new File(repository));
			FSRepositoryFactory.setup();
			final SortedSet<String> filepaths = new TreeSet<String>();
			logClient.doList(url, SVNRevision.create(revision),
					SVNRevision.create(revision), true, SVNDepth.INFINITY,
					SVNDirEntry.DIRENT_ALL, new ISVNDirEntryHandler() {

						@Override
						public void handleDirEntry(final SVNDirEntry entry)
								throws SVNException {

							if (entry.getKind() == SVNNodeKind.FILE) {
								final String path = entry.getRelativePath();
								if (path.endsWith(".java")) {
									filepaths.add(path);
								}
							}
						}
					});

			final List<List<yoshikihigo.cpanalyzer.data.Statement>> contents = new ArrayList<>();
			final SVNWCClient wcClient = SVNClientManager.newInstance()
					.getWCClient();
			for (final String path : filepaths) {

				final SVNURL fileurl = SVNURL.fromFile(new File(repository
						+ System.getProperty("file.separator") + path));
				final StringBuilder text = new StringBuilder();
				wcClient.doGetFileContents(fileurl,
						SVNRevision.create(revision),
						SVNRevision.create(revision), false,
						new OutputStream() {
							@Override
							public void write(int b) throws IOException {
								text.append((char) b);
							}
						});
				final List<yoshikihigo.cpanalyzer.data.Statement> statements = StringUtility
						.splitToStatements(text.toString(), LANGUAGE.JAVA);
				contents.add(statements);
			}

			return contents;

		} catch (final SVNException exception) {
			exception.printStackTrace();
		}

		return new ArrayList<List<yoshikihigo.cpanalyzer.data.Statement>>();
	}

	private static int getCount(
			final List<yoshikihigo.cpanalyzer.data.Statement> statements,
			final List<yoshikihigo.cpanalyzer.data.Statement> pattern) {

		int count = 0;
		int pIndex = 0;
		for (int index = 0; index < statements.size(); index++) {

			if (statements.get(index).toString()
					.equals(pattern.get(pIndex).toString())) {
				pIndex++;
				if (pIndex == pattern.size()) {
					count++;
					pIndex = 0;
				}
			}

			else {
				pIndex = 0;
			}
		}

		return count;
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
