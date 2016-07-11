package yoshikihigo.fbparser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
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
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
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

public class FBChangePatternFinderWithoutFB {

	public static void main(final String[] args) {

		FBParserConfig.initialize(args);
		final String fcpFile = FBParserConfig.getInstance()
				.getFIXCHANGEPATTERN();
		final DAO dao = DAO.getInstance();

		try (final Workbook book = new XSSFWorkbook();
				final OutputStream stream = new FileOutputStream(fcpFile)) {

			Cell firstCell = null;
			Cell lastCell = null;

			final Sheet sheet = book.createSheet();
			book.setSheetName(0, "change patterns");
			final Row titleRow = sheet.createRow(0);
			titleRow.createCell(0).setCellValue("MERGED-PATTERN-ID");
			titleRow.createCell(1).setCellValue("PATTERN-ID");
			titleRow.createCell(2).setCellValue("FINDBUGS-SUPPORT");
			titleRow.createCell(3).setCellValue("AUTHORS");
			titleRow.createCell(4).setCellValue("BUG-FIX-AUTHORS");
			titleRow.createCell(5).setCellValue("FILES");
			titleRow.createCell(6).setCellValue("BUG-FIX-FILES");
			titleRow.createCell(7).setCellValue("SUPPORT");
			titleRow.createCell(8).setCellValue("BUG-FIX-SUPPORT");
			titleRow.createCell(9).setCellValue("BEFORE-TEXT-SUPPORT");
			titleRow.createCell(10).setCellValue("CONFIDENCE1");
			titleRow.createCell(11).setCellValue("CONFIDENCE2");
			titleRow.createCell(12).setCellValue("CONFIDENCE3");
			titleRow.createCell(13).setCellValue("COMMITS");
			titleRow.createCell(14).setCellValue("BUG-FIX-COMMIT");
			titleRow.createCell(15).setCellValue("FIRST-DATE");
			titleRow.createCell(16).setCellValue("LAST-DATE");
			titleRow.createCell(17).setCellValue("DATE-DIFFERENCE");
			titleRow.createCell(18).setCellValue("OCCUPANCY");
			titleRow.createCell(19).setCellValue("Delta-PFCF");
			titleRow.createCell(20).setCellValue("RANK-of-\"G\"");
			titleRow.createCell(21).setCellValue("RANK-of-\"F-G\"");
			titleRow.createCell(22).setCellValue("RANK-of-\"R\"");
			titleRow.createCell(23).setCellValue("TEXT-BEFORE-CHANGE");
			titleRow.createCell(24).setCellValue("TEXT-AFTER-CHANGE");
			titleRow.createCell(25).setCellValue("AUTHOR-LIST");
			titleRow.createCell(26).setCellValue("BUG-FIX-AUTHOR-LIST");
			titleRow.createCell(27).setCellValue("FILE-LIST");
			titleRow.createCell(28).setCellValue("BUG-FIX-FILE-LIST");

			final int bugfixCommits = (int) DAO.getInstance().getRevisions()
					.stream().filter(revision -> revision.bugfix).count();
			titleRow.createCell(29).setCellValue(bugfixCommits);
			final int nonbugfixCommits = (int) DAO.getInstance().getRevisions()
					.stream().filter(revision -> !revision.bugfix).count();
			titleRow.createCell(30).setCellValue(nonbugfixCommits);

			firstCell = titleRow.getCell(0);
			lastCell = titleRow.getCell(28);

			setCellComment(
					titleRow.getCell(3),
					"Higo",
					"the number of authors that committed the change pattern in all commits",
					5, 2);
			setCellComment(
					titleRow.getCell(4),
					"Higo",
					"the number of authors commited the change pattern in bug-fix commits",
					5, 2);
			setCellComment(
					titleRow.getCell(5),
					"Higo",
					"the number of files where the change pattern appeared in all commits",
					5, 2);
			setCellComment(
					titleRow.getCell(6),
					"Higo",
					"the number of files where the change pattern appeared in bug-fix commits",
					5, 2);
			setCellComment(titleRow.getCell(7), "Higo",
					"the number of occurences of a given pattern", 4, 1);
			setCellComment(
					titleRow.getCell(8),
					"Higo",
					"the number of occurences of a given pattern in bug-fix commits",
					4, 2);
			setCellComment(
					titleRow.getCell(9),
					"Higo",
					"the number of code fragments whose texts are "
							+ "identical to before-text of a given pattern "
							+ "in the commit where the pattern appears initially",
					4, 3);
			setCellComment(titleRow.getCell(10), "Higo",
					"BUG-FIX-SUPPORT / SUPPORT", 4, 2);
			setCellComment(titleRow.getCell(11), "Higo",
					"SUPPORT / BEFORE-TEXT-SUPPORT", 4, 2);
			setCellComment(titleRow.getCell(12), "Higo",
					"BUG-FIX-SUPPORT / BEFORE-TEXT-SUPPORT", 4, 2);
			setCellComment(titleRow.getCell(13), "Higo",
					"the number of commits where the pattern appears", 4, 2);
			setCellComment(titleRow.getCell(14), "Higo",
					"the number of bug-fix commits where the pattern appears",
					4, 2);
			setCellComment(
					titleRow.getCell(18),
					"Higo",
					"average of (LOC of a given pattern changed in revision R) / "
							+ "(total LOC changed in revision R) "
							+ "for all the revisions where the pattern appears",
					4, 3);
			setCellComment(
					titleRow.getCell(19),
					"Higo",
					"delta-CFPF was calculated with the following formula"
							+ System.lineSeparator()
							+ "pf*(cf1 - cf2)"
							+ System.lineSeparator()
							+ "pf: pattern frequency, which is calculated as support / before-text-support"
							+ System.lineSeparator()
							+ "cf1: bug-fix commit frequensy, which is calculated as bug-fix commits / all bug-fix commits"
							+ System.lineSeparator()
							+ "cf2: non-bug-fix commit frequency, which is calculated as non-bug-fix commits / all non-bug-fix commits",
					5, 5);

			int currentRow = 1;
			final List<PATTERN_SQL> cps = dao.getFixChangePatterns();
			// Collections.sort(cps, (o1, o2) -> Integer.compare(o1.id, o2.id));
			Collections.sort(cps,
					(o1, o2) -> o1.firstdate.compareTo(o2.firstdate));
			for (final PATTERN_SQL cp : cps) {

				if (cp.beforeNText.isEmpty()) {
					continue;
				}

				if (cp.afterNText.isEmpty()) {
					continue;
				}

				if (cp.confidence < 0.5f) {
					continue;
				}

				if (cp.support < 2) {
					continue;
				}

				final int findBugsSupport = 0;

				final Row dataRow = sheet.createRow(currentRow++);
				dataRow.createCell(0).setCellValue(cp.id);
				dataRow.createCell(1).setCellValue(cp.id);
				dataRow.createCell(2).setCellValue(findBugsSupport);
				dataRow.createCell(3).setCellValue(getAuthors(cp).size());
				dataRow.createCell(4).setCellValue(getAuthors(cp, true).size());
				dataRow.createCell(5).setCellValue(getFiles(cp).size());
				dataRow.createCell(6).setCellValue(getFiles(cp, true).size());
				final int support = getChanges(cp).size();
				final int bugfixSupport = getChanges(cp, true).size();
				final int beforeTextSupport = 1;// countTextAppearances(cp);
				dataRow.createCell(7).setCellValue(support);
				dataRow.createCell(8).setCellValue(bugfixSupport);
				dataRow.createCell(9).setCellValue(beforeTextSupport);
				dataRow.createCell(10).setCellValue(
						(float) bugfixSupport / (float) support);
				dataRow.createCell(11).setCellValue(
						(float) support / (float) beforeTextSupport);
				dataRow.createCell(12).setCellValue(
						(float) bugfixSupport / (float) beforeTextSupport);
				dataRow.createCell(13).setCellValue(getCommits(cp));
				dataRow.createCell(14).setCellValue(getCommits(cp, true));
				dataRow.createCell(15).setCellValue(cp.firstdate);
				dataRow.createCell(16).setCellValue(cp.lastdate);
				dataRow.createCell(17).setCellValue(
						getDayDifference(cp.firstdate, cp.lastdate));
				dataRow.createCell(18).setCellValue(getOccupancy(cp));
				dataRow.createCell(19).setCellValue(
						getDeltaCFPF(cp, beforeTextSupport));
				dataRow.createCell(20).setCellValue("no data");
				dataRow.createCell(21).setCellValue("no date");
				dataRow.createCell(22).setCellValue("no date");
				dataRow.createCell(23).setCellValue(
						cp.beforeNText.length() > 32767 ? cp.beforeNText
								.substring(0, 32767) : cp.beforeNText);
				dataRow.createCell(24).setCellValue(
						cp.afterNText.length() > 32767 ? cp.afterNText
								.substring(0, 32767) : cp.afterNText);
				dataRow.createCell(25).setCellValue(
						yoshikihigo.fbparser.StringUtility
								.concatinate(getAuthors(cp)));
				dataRow.createCell(26).setCellValue(
						yoshikihigo.fbparser.StringUtility
								.concatinate(getAuthors(cp, true)));
				dataRow.createCell(27).setCellValue(
						yoshikihigo.fbparser.StringUtility.shrink(
								yoshikihigo.fbparser.StringUtility
										.concatinate(getFiles(cp)), 10000));
				dataRow.createCell(28)
						.setCellValue(
								yoshikihigo.fbparser.StringUtility.shrink(
										yoshikihigo.fbparser.StringUtility
												.concatinate(getFiles(cp, true)),
										10000));
				lastCell = dataRow.getCell(28);

				final CellStyle style = book.createCellStyle();
				style.setWrapText(true);
				style.setFillPattern(CellStyle.SOLID_FOREGROUND);
				style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
				style.setBorderBottom(XSSFCellStyle.BORDER_THIN);
				style.setBorderLeft(XSSFCellStyle.BORDER_THIN);
				style.setBorderRight(XSSFCellStyle.BORDER_THIN);
				style.setBorderTop(XSSFCellStyle.BORDER_THIN);
				style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
				for (int column = 0; column <= 28; column++) {
					dataRow.getCell(column).setCellStyle(style);
				}

				int loc = Math.max(getLOC(cp.beforeNText),
						getLOC(cp.afterNText));
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
			sheet.autoSizeColumn(19, true);
			sheet.autoSizeColumn(20, true);
			sheet.autoSizeColumn(21, true);
			sheet.autoSizeColumn(22, true);
			sheet.setColumnWidth(23, 20480);
			sheet.setColumnWidth(24, 20480);
			sheet.setColumnWidth(25, 5120);
			sheet.setColumnWidth(26, 5120);
			sheet.setColumnWidth(27, 20480);
			sheet.setColumnWidth(28, 20480);

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

	private static int getCommits(final PATTERN_SQL cp) {
		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		return (int) DAO.getInstance().getChanges(beforeHash, afterHash)
				.stream().map(change -> change.revision).distinct().count();
	}

	private static int getCommits(final PATTERN_SQL cp, final boolean bugfix) {
		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		final List<CHANGE_SQL> changesInPattern = DAO.getInstance().getChanges(
				beforeHash, afterHash);
		return (int) changesInPattern.stream()
				.filter(change -> bugfix == change.bugfix).count();
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
					revision.id);
			changesInRevision.stream().forEach(
					change -> {
						if (changesInPattern.contains(change)) {
							final AtomicInteger size = map1.get(revision);
							size.addAndGet(change.beforeEndLine
									- change.beforeStartLine + 1);
						}
						final AtomicInteger size = map2.get(revision);
						size.addAndGet(change.beforeEndLine
								- change.beforeStartLine + 1);
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

	private static double getDeltaCFPF(final PATTERN_SQL cp,
			final int beforeTextSupport) {
		final double pf = (double) getChanges(cp).size()
				/ (double) beforeTextSupport;
		final long count1 = DAO.getInstance().getRevisions().stream()
				.filter(revision -> revision.bugfix).count();
		final double cf1 = (double) getCommits(cp, true)
				/ (double) (0 < count1 ? count1 : 1);
		final long count2 = DAO.getInstance().getRevisions().stream()
				.filter(revision -> !revision.bugfix).count();
		final double cf2 = (double) getCommits(cp, false)
				/ (double) (0 < count2 ? count2 : 1);
		final double pfcf = pf * (cf1 - cf2);
		return pfcf;
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
				cp.beforeNText, 1, 1);
		int count = 0;

		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		final SortedSet<REVISION_SQL> revisions = DAO.getInstance()
				.getRevisions(beforeHash, afterHash);
		List<List<Statement>> contents = Collections.emptyList();
		if (FBParserConfig.getInstance().hasSVNREPOSITORY()) {
			final int firstRevision = Integer.valueOf(revisions.first().id) - 1;
			contents = getSVNFileContents(firstRevision);
		} else if (FBParserConfig.getInstance().hasGITREPOSITORY()) {
			final String firstRevision = revisions.first().id;
			contents = getGITFileContents(firstRevision);
		}
		for (final List<Statement> content : contents) {
			count += getCount(content, pattern);
		}

		return count;
	}

	static final private Map<Integer, List<String>> REVISION_FILEPATH_MAP = new HashMap<>();
	static final private Map<String, Cache> FILEPATH_CACHE_MAP = new HashMap<>();
	static final private Map<String, SortedSet<Integer>> FILEPATH_REVISIONS_MAP = new HashMap<>();

	static private String PREVIOUS_CHANGEPATTERN_REVISION = "";
	static private List<List<Statement>> PREVIOUS_REVISION_CONTENTS = null;

	private static List<List<Statement>> getSVNFileContents(final int revision) {

		if (Integer.toString(revision).equals(PREVIOUS_CHANGEPATTERN_REVISION)) {
			return PREVIOUS_REVISION_CONTENTS;
		}

		final String repository = FBParserConfig.getInstance()
				.getSVNREPOSITORY();
		final List<String> paths = new ArrayList<>();
		try {

			final SVNLogClient logClient = SVNClientManager.newInstance()
					.getLogClient();
			final SVNURL url = SVNURL.fromFile(new File(repository));
			FSRepositoryFactory.setup();
			final long revNumber = Long.valueOf(revision);
			logClient.doList(url, SVNRevision.create(revNumber),
					SVNRevision.create(revNumber), true, SVNDepth.INFINITY,
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
		} catch (final SVNException | NullPointerException e) {
		}

		final SVNWCClient wcClient = SVNClientManager.newInstance()
				.getWCClient();
		final List<List<Statement>> contents = new ArrayList<>();
		for (final String path : paths) {
			try {
				final SVNURL fileurl = SVNURL.fromFile(new File(repository
						+ System.getProperty("file.separator") + path));
				final StringBuilder text = new StringBuilder();
				final long revNumber = Long.valueOf(revision);
				wcClient.doGetFileContents(fileurl,
						SVNRevision.create(revNumber),
						SVNRevision.create(revNumber), false,
						new OutputStream() {
							@Override
							public void write(int b) throws IOException {
								text.append((char) b);
							}
						});
				final List<Statement> statements = StringUtility
						.splitToStatements(text.toString(), LANGUAGE.JAVA);
				contents.add(statements);
			} catch (final SVNException | NullPointerException e) {
			}
		}

		PREVIOUS_CHANGEPATTERN_REVISION = Integer.toString(revision);
		PREVIOUS_REVISION_CONTENTS = contents;

		return contents;
	}

	private static List<List<Statement>> getGITFileContents(
			final String revision) {

		if (revision.equals(PREVIOUS_CHANGEPATTERN_REVISION)) {
			return PREVIOUS_REVISION_CONTENTS;
		}

		final String gitrepo = FBParserConfig.getInstance().getGITREPOSITORY();
		final Set<LANGUAGE> languages = FBParserConfig.getInstance()
				.getLANGUAGE();

		final List<List<Statement>> contents = new ArrayList<>();
		try (final FileRepository repo = new FileRepository(new File(gitrepo
				+ "/.git"));
				final ObjectReader reader = repo.newObjectReader();
				final TreeWalk treeWalk = new TreeWalk(reader);
				final RevWalk revWalk = new RevWalk(reader)) {

			final ObjectId rootId = repo.resolve(revision);
			revWalk.markStart(revWalk.parseCommit(rootId));
			final RevCommit commit = revWalk.next();
			final RevTree tree = commit.getTree();
			treeWalk.addTree(tree);
			treeWalk.setRecursive(true);
			final List<String> files = new ArrayList<>();
			while (treeWalk.next()) {
				final String path = treeWalk.getPathString();
				for (final LANGUAGE language : languages) {
					if (language.isTarget(path)) {
						files.add(path);
					}
					break;
				}
			}

			for (final String file : files) {
				final TreeWalk nodeWalk = TreeWalk.forPath(reader, file, tree);
				final byte[] data = reader.open(nodeWalk.getObjectId(0))
						.getBytes();
				final String text = new String(data, "utf-8");
				final List<Statement> statements = StringUtility
						.splitToStatements(text.toString(), LANGUAGE.JAVA);
				contents.add(statements);
			}

		} catch (final IOException e) {
			e.printStackTrace();
		}

		PREVIOUS_CHANGEPATTERN_REVISION = revision;
		PREVIOUS_REVISION_CONTENTS = contents;

		return contents;
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
