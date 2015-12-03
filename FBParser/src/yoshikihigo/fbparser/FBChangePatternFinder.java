package yoshikihigo.fbparser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import yoshikihigo.cpanalyzer.data.Statement;
import yoshikihigo.fbparser.db.DAO;
import yoshikihigo.fbparser.db.DAO.CHANGE_SQL;
import yoshikihigo.fbparser.db.DAO.PATTERN_SQL;
import yoshikihigo.fbparser.db.DAO.REVISION_SQL;

public class FBChangePatternFinder {

	public static void main(final String[] args) {

		FBParserConfig.initialize(args);
		final String trFile = FBParserConfig.getInstance()
				.getTRANSITIONRESULT();
		final String cpFile = FBParserConfig.getInstance().getCHANGEPATTERN();
		final String fcpFile = FBParserConfig.getInstance()
				.getFIXCHANGEPATTERN();
		final DAO dao = DAO.getInstance();

		final Map<Integer, AtomicInteger> foundPatternIDs = new HashMap<>();
		try (final PrintWriter writer = new PrintWriter(new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(cpFile), "UTF-8")))) {

			final List<String> lines = Files.readAllLines(Paths.get(trFile),
					Charset.forName("UTF-8"));
			final String trTitle = lines.get(0);
			writer.print(trTitle);
			writer.println(", CHANGEPATTERN-ID, CHANGEPATTERN-SUPPORT");

			lines.remove(0);
			lines.stream().forEach(
					line -> {
						final Line l = new Line(line);
						if (!l.status.startsWith("removed")) {
							return;
						}
						if ((l.startstartline <= 0) || (l.startendline <= 0)) {
							return;
						}

						final List<CHANGE_SQL> changes = dao.getChanges(
								l.endrev + 1, l.path);
						for (final CHANGE_SQL change : changes) {

							if (change.endline < l.startstartline) {
								continue;
							}

							if (l.startendline < change.startline) {
								continue;
							}

							final List<PATTERN_SQL> cps = dao
									.getChangePatterns(change.beforeHash,
											change.afterHash);
							for (final PATTERN_SQL cp : cps) {
								writer.print(line);
								writer.print(", ");
								writer.print(cp.id);
								writer.print(", ");
								writer.println(getChanges(cp).size());
								AtomicInteger number = foundPatternIDs
										.get(cp.id);
								if (null == number) {
									number = new AtomicInteger(0);
									foundPatternIDs.put(cp.id, number);
								}
								number.addAndGet(1);
							}
						}
					});

		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(0);
		}

		try (final Workbook book = new XSSFWorkbook();
				final OutputStream stream = new FileOutputStream(fcpFile)) {

			Cell firstCell = null;
			Cell lastCell = null;

			final Sheet sheet = book.createSheet();
			book.setSheetName(0, "change patterns");
			final Row titleRow = sheet.createRow(0);
			titleRow.createCell(0).setCellValue("PATTERN-ID");
			titleRow.createCell(1).setCellValue("FINDBUGS-SUPPORT");
			titleRow.createCell(2).setCellValue("AUTHORS");
			titleRow.createCell(3).setCellValue("BUG-FIX-AUTHORS");
			titleRow.createCell(4).setCellValue("FILES");
			titleRow.createCell(5).setCellValue("BUG-FIX-FILES");
			titleRow.createCell(6).setCellValue("SUPPORT");
			titleRow.createCell(7).setCellValue("BUG-FIX-SUPPORT");
			titleRow.createCell(8).setCellValue("BEFORE-TEXT-SUPPORT");
			titleRow.createCell(9).setCellValue("CONFIDENCE1");
			titleRow.createCell(10).setCellValue("CONFIDENCE2");
			titleRow.createCell(11).setCellValue("CONFIDENCE3");
			titleRow.createCell(12).setCellValue("COMMITS");
			titleRow.createCell(13).setCellValue("BUG-FIX-COMMIT");
			titleRow.createCell(14).setCellValue("FIRST-DATE");
			titleRow.createCell(15).setCellValue("LAST-DATE");
			titleRow.createCell(16).setCellValue("DATE-DIFFERENCE");
			titleRow.createCell(17).setCellValue("OCCUPANCY");
			titleRow.createCell(18).setCellValue("Delta-TFIDF");
			titleRow.createCell(19).setCellValue("TEXT-BEFORE-CHANGE");
			titleRow.createCell(20).setCellValue("TEXT-AFTER-CHANGE");
			titleRow.createCell(21).setCellValue("AUTHOR-LIST");
			titleRow.createCell(22).setCellValue("BUG-FIX-AUTHOR-LIST");
			titleRow.createCell(23).setCellValue("FILE-LIST");
			titleRow.createCell(24).setCellValue("BUG-FIX-FILE-LIST");

			firstCell = titleRow.getCell(0);
			lastCell = titleRow.getCell(24);

			setCellComment(
					titleRow.getCell(2),
					"Higo",
					"the number of authors that committed the change pattern in all commits",
					5, 2);
			setCellComment(
					titleRow.getCell(3),
					"Higo",
					"the number of authors commited the change pattern in bug-fix commits",
					5, 2);
			setCellComment(
					titleRow.getCell(4),
					"Higo",
					"the number of files where the change pattern appeared in all commits",
					5, 2);
			setCellComment(
					titleRow.getCell(5),
					"Higo",
					"the number of files where the change pattern appeared in bug-fix commits",
					5, 2);
			setCellComment(titleRow.getCell(6), "Higo",
					"the number of occurences of a given pattern", 4, 1);
			setCellComment(
					titleRow.getCell(7),
					"Higo",
					"the number of occurences of a given pattern in bug-fix commits",
					4, 2);
			setCellComment(
					titleRow.getCell(8),
					"Higo",
					"the number of code fragments whose texts are "
							+ "identical to before-text of a given pattern "
							+ "in the commit where the pattern appears initially",
					4, 3);
			setCellComment(titleRow.getCell(9), "Higo",
					"BUG-FIX-SUPPORT / SUPPORT", 4, 2);
			setCellComment(titleRow.getCell(10), "Higo",
					"SUPPORT / BEFORE-TEXT-SUPPORT", 4, 2);
			setCellComment(titleRow.getCell(11), "Higo",
					"BUG-FIX-SUPPORT / BEFORE-TEXT-SUPPORT", 4, 2);
			setCellComment(titleRow.getCell(12), "Higo",
					"the number of commits where the pattern appears", 4, 2);
			setCellComment(titleRow.getCell(13), "Higo",
					"the number of bug-fix commits where the pattern appears",
					4, 2);
			setCellComment(
					titleRow.getCell(17),
					"Higo",
					"average of (LOC of a given pattern changed in revision R) / "
							+ "(total LOC changed in revision R) "
							+ "for all the revisions where the pattern appears",
					4, 3);
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
							+ System.lineSeparator() + "k1: 1.2 (parameter)"
							+ System.lineSeparator() + "K: 1.2 (parameter)", 5,
					5);

			int currentRow = 1;
			final List<PATTERN_SQL> cps = dao.getFixChangePatterns();
			// Collections.sort(cps, (o1, o2) -> Integer.compare(o1.id, o2.id));
			Collections.sort(cps,
					(o1, o2) -> o1.firstdate.compareTo(o2.firstdate));
			for (final PATTERN_SQL cp : cps) {

				if (cp.beforeText.isEmpty()) {
					continue;
				}
				System.out.println(cp.id);

				final int findBugsSupport = foundPatternIDs.containsKey(cp.id) ? foundPatternIDs
						.get(cp.id).get() : 0;

				final Row dataRow = sheet.createRow(currentRow++);
				dataRow.createCell(0).setCellValue(cp.id);
				dataRow.createCell(1).setCellValue(findBugsSupport);
				dataRow.createCell(2).setCellValue(getAuthors(cp).size());
				dataRow.createCell(3).setCellValue(getAuthors(cp, true).size());
				dataRow.createCell(4).setCellValue(getFiles(cp).size());
				dataRow.createCell(5).setCellValue(getFiles(cp, true).size());
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
				dataRow.createCell(21).setCellValue(
						yoshikihigo.fbparser.StringUtility
								.concatinate(getAuthors(cp)));
				dataRow.createCell(22).setCellValue(
						yoshikihigo.fbparser.StringUtility
								.concatinate(getAuthors(cp, true)));
				dataRow.createCell(23).setCellValue(
						yoshikihigo.fbparser.StringUtility.shrink(
								yoshikihigo.fbparser.StringUtility
										.concatinate(getFiles(cp)), 10000));
				dataRow.createCell(24)
						.setCellValue(
								yoshikihigo.fbparser.StringUtility.shrink(
										yoshikihigo.fbparser.StringUtility
												.concatinate(getFiles(cp, true)),
										10000));
				lastCell = dataRow.getCell(24);

				final CellStyle style = book.createCellStyle();
				style.setWrapText(true);
				style.setFillPattern(CellStyle.SOLID_FOREGROUND);
				style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
				style.setBorderBottom(XSSFCellStyle.BORDER_THIN);
				style.setBorderLeft(XSSFCellStyle.BORDER_THIN);
				style.setBorderRight(XSSFCellStyle.BORDER_THIN);
				style.setBorderTop(XSSFCellStyle.BORDER_THIN);
				style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
				for (int column = 0; column <= 24; column++) {
					dataRow.getCell(column).setCellStyle(style);
				}

				int loc = Math.max(getLOC(cp.beforeText), getLOC(cp.afterText));
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
			sheet.setColumnWidth(21, 5120);
			sheet.setColumnWidth(22, 5120);
			sheet.setColumnWidth(23, 20480);
			sheet.setColumnWidth(24, 20480);

			sheet.setAutoFilter(new CellRangeAddress(firstCell.getRowIndex(),
					lastCell.getRowIndex(), firstCell.getColumnIndex(),
					lastCell.getColumnIndex()));
			sheet.createFreezePane(0, 1, 0, 1);

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

	private static int getCommits(final PATTERN_SQL cp, final boolean onlyBugfix) {
		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		final List<CHANGE_SQL> changesInPattern = DAO.getInstance().getChanges(
				beforeHash, afterHash);
		return (int) changesInPattern.stream()
				.filter(change -> !onlyBugfix || (onlyBugfix && change.bugfix))
				.count();
	}

	private static float getOccupancy(final PATTERN_SQL cp) {

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
			changesInRevision.stream().forEach(change -> {
				if (changesInPattern.contains(change)) {
					final AtomicInteger size = map1.get(revision);
					size.addAndGet(change.endline - change.startline + 1);
				}
				final AtomicInteger size = map2.get(revision);
				size.addAndGet(change.endline - change.startline + 1);
			});
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

	private static double getDeltaTFIDF(final PATTERN_SQL cp) {

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
		final double k1 = 1.2d; // parameter
		final double K = 1.2d; // parameter

		final double w = ((k1 + 1) * tf / (K + tf))
				* (Math.log(((n1 - df1 + 0.5) * (df2 + 0.5))
						/ ((n2 - df2 + 0.5) * (df1 + 0.5))) / Math.log(2.0));
		return w;
	}

	private static List<CHANGE_SQL> getChanges(final PATTERN_SQL cp) {

		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		return DAO.getInstance().getChanges(beforeHash, afterHash);
	}

	private static List<CHANGE_SQL> getChanges(final PATTERN_SQL cp,
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

	private static SortedSet<String> getAuthors(final PATTERN_SQL cp) {

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

	private static SortedSet<String> getAuthors(final PATTERN_SQL cp,
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

	private static SortedSet<String> getFiles(final PATTERN_SQL cp) {

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

	private static SortedSet<String> getFiles(final PATTERN_SQL cp,
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

	private static int countTextAppearances(final PATTERN_SQL cp) {

		CPAConfig.initialize(new String[] {});
		final List<Statement> pattern = StringUtility.splitToStatements(
				cp.beforeText, 1, 1);
		int count = 0;

		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		final SortedSet<REVISION_SQL> revisions = DAO.getInstance()
				.getRevisions(beforeHash, afterHash);
		final int firstRevision = revisions.first().number - 1;
		final List<List<Statement>> contents = getFileContents(firstRevision);
		for (final List<Statement> content : contents) {
			count += getCount(content, pattern);
		}

		return count;
	}

	static final private Map<Integer, List<String>> REVISION_FILEPATH_MAP = new HashMap<>();
	static final private Map<String, Cache> FILEPATH_CACHE_MAP = new HashMap<>();
	static final private Map<String, SortedSet<Integer>> FILEPATH_REVISIONS_MAP = new HashMap<>();

	static private int PREVIOUS_CHANGEPATTERN_REVISION = 0;
	static private List<List<Statement>> PREVIOUS_REVISION_CONTENTS = null;

	private static List<List<Statement>> getFileContents(final int revision) {

		if (revision == PREVIOUS_CHANGEPATTERN_REVISION) {
			return PREVIOUS_REVISION_CONTENTS;
		}

		try {
			final String repository = FBParserConfig.getInstance()
					.getREPOSITORY();
			final SVNLogClient logClient = SVNClientManager.newInstance()
					.getLogClient();
			final SVNURL url = SVNURL.fromFile(new File(repository));

			final List<String> paths = new ArrayList<>();
			FSRepositoryFactory.setup();
			logClient.doList(url, SVNRevision.create(revision),
					SVNRevision.create(revision), true, SVNDepth.INFINITY,
					SVNDirEntry.DIRENT_ALL, entry -> {
						if (entry.getKind() != SVNNodeKind.FILE) {
							return;
						}
						final String path = entry.getRelativePath();
						if (!path.endsWith(".java")) {
							return;
						}
						paths.add(path);
					});

			final SVNWCClient wcClient = SVNClientManager.newInstance()
					.getWCClient();
			final List<List<Statement>> contents = new ArrayList<>();
			for (final String path : paths) {

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
				final List<Statement> statements = StringUtility
						.splitToStatements(text.toString(), LANGUAGE.JAVA);
				contents.add(statements);
			}

			PREVIOUS_CHANGEPATTERN_REVISION = revision;
			PREVIOUS_REVISION_CONTENTS = contents;

			return contents;
		}

		catch (final SVNException exception) {
			exception.printStackTrace();
		}

		return new ArrayList<List<Statement>>();
	}

	private static int getCount(final List<Statement> statements,
			final List<Statement> pattern) {

		int count = 0;
		for (int index = 0; index < statements.size(); index++) {

			int pIndex = 0;
			while (Arrays.equals(statements.get(index + pIndex).hash,
					pattern.get(pIndex).hash)) {
				pIndex++;
				if (pattern.size() == pIndex) {
					count++;
					break;
				}
				if (statements.size() == index + pIndex) {
					break;
				}
			}
		}

		return count;
	}

	private static int[] getCacheRange(final SortedSet<Integer> revisions,
			final int revision) {

		if (revisions.isEmpty()) {
			return new int[] { revision, revision };
		}

		int cacheStartRevision = revisions.first().intValue();
		int cacheEndRevision = revisions.last().intValue() - 1;
		for (final int r : revisions) {
			if ((r <= revision) && (cacheStartRevision < r)) {
				cacheStartRevision = r;
			}
			if ((revision <= r) && (r < cacheEndRevision)) {
				cacheEndRevision = r - 1;
			}
		}
		if (revision < cacheStartRevision) {
			cacheStartRevision = revision;
		}
		if (cacheEndRevision < revision) {
			cacheEndRevision = revision;
		}

		return new int[] { cacheStartRevision, cacheEndRevision };
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

class Cache {

	final public String path;
	final private Map<int[], List<Statement>> data;

	Cache(final String path) {
		this.path = path;
		data = new HashMap<>();
	}

	public List<Statement> getCache(final int revision) {
		for (final Entry<int[], List<Statement>> entry : this.data.entrySet()) {
			final int[] range = entry.getKey();
			if ((range[0] <= revision) && (revision <= range[1])) {
				System.out.println("cashe hit! :" + revision + " " + this.path);
				return entry.getValue();
			}
		}
		return null;
	}

	public boolean addCache(final List<Statement> statements, final int[] range) {

		final Iterator<Entry<int[], List<Statement>>> iterator = this.data
				.entrySet().iterator();
		while (iterator.hasNext()) {
			final Entry<int[], List<Statement>> entry = iterator.next();
			final int[] r = entry.getKey();
			final List<Statement> s = entry.getValue();
			if (range[1] < r[0] || r[1] < range[0]) {
				continue;
			}
			if (s.equals(statements)) {
				iterator.remove();
				continue;
			}
			System.err
					.println("warning: statements are different its cached one, "
							+ range[0] + "--" + range[1] + ", " + this.path);
			// assert false : "adding cache condition was illegal.";
			return false;
		}

		System.out.println("adding cache: " + range[0] + "--" + range[1] + ", "
				+ this.path);
		this.data.put(range, statements);
		return true;
	}
}
