package yoshikihigo.fbparser;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

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

import yoshikihigo.cpanalyzer.data.Statement;

public class XLSXMerger {

	public static void main(final String[] args) {

		for (String arg : args) {
			System.out.println(arg);
		}

		if (args.length < 2) {
			System.err.println("two or more arguments are required.");
			System.exit(0);
		}

		final String mergedXLSXPath = args[0];

		final Map<SIMPLE_PATTERN, PATTERN> patterns = new HashMap<>();
		final List<Integer> allBugfixCommits = new ArrayList<>();
		final List<Integer> allNonbugfixCommits = new ArrayList<>();
		for (int index = 1; index < args.length; index++) {
			readXLSX(patterns, allBugfixCommits, allNonbugfixCommits, args[index]);
		}

		final int allBugfixCommitNumber = allBugfixCommits.stream().mapToInt(value -> value.intValue()).sum();
		final int allNonbugfixCommitNumber = allNonbugfixCommits.stream().mapToInt(value -> value.intValue()).sum();

		final List<Integer> bugfixFileData = new ArrayList<>();
		final List<Integer> fileDiffData = new ArrayList<>();
		final List<Integer> dayDiffData = new ArrayList<>();
		for (final PATTERN pattern : patterns.values()) {
			final int bugfixFiles = pattern.bugfixFiles.size();
			if (1 < bugfixFiles) {
				bugfixFileData.add(bugfixFiles);
			}
			final int fileDiff = pattern.files.size() - pattern.bugfixFiles.size();
			if (0 < fileDiff) {
				fileDiffData.add(fileDiff);
			}
			final int dayDifference = getDayDifference(pattern.getFirstDate(), pattern.getLastDate());
			if (0 < dayDifference) {
				dayDiffData.add(dayDifference);
			}
		}
		Collections.sort(bugfixFileData);
		Collections.sort(fileDiffData);
		Collections.sort(dayDiffData);

		writeXLSX(patterns, allBugfixCommitNumber, allNonbugfixCommitNumber, bugfixFileData, fileDiffData, dayDiffData,
				mergedXLSXPath);
	}

	private static void readXLSX(final Map<SIMPLE_PATTERN, PATTERN> patterns, final List<Integer> allBugfixCommits,
			final List<Integer> allNonbugfixCommits, final String xlsxPath) {

		final String period = StringUtility.removeExtension(StringUtility.getName(xlsxPath));

		try (final Workbook book = new XSSFWorkbook(new FileInputStream(xlsxPath))) {
			final Sheet sheet = book.getSheetAt(0);

			{
				final Row titleRow = sheet.getRow(0);
				final int bugfixCommit = (int) titleRow.getCell(25).getNumericCellValue();
				allBugfixCommits.add(bugfixCommit);
				final int nonbugfixCommit = (int) titleRow.getCell(26).getNumericCellValue();
				allNonbugfixCommits.add(nonbugfixCommit);
			}

			final int lastRowNumber = sheet.getLastRowNum();
			for (int rowNumber = 1; rowNumber < lastRowNumber; rowNumber++) {
				final Row row = sheet.getRow(rowNumber);
				final String beforeText = row.getCell(19).getStringCellValue();
				final String afterText = row.getCell(20).getStringCellValue();
				final SIMPLE_PATTERN p = new SIMPLE_PATTERN(beforeText, afterText);
				PATTERN pattern = patterns.get(p);
				if (null == pattern) {
					pattern = new PATTERN(beforeText, afterText);
					patterns.put(p, pattern);
				}

				pattern.addPeriod(period);

				final String id = period + ": " + Integer.toString((int) row.getCell(0).getNumericCellValue());
				pattern.addID(id);

				final int findbugsSupport = (int) row.getCell(1).getNumericCellValue();
				pattern.findbugsSupport += findbugsSupport;

				final int support = (int) row.getCell(6).getNumericCellValue();
				pattern.support += support;
				final int bugfixSupport = (int) row.getCell(7).getNumericCellValue();
				pattern.bugfixSupport += bugfixSupport;
				final int beforeTextSupport = (int) row.getCell(8).getNumericCellValue();
				if (null == pattern.beforeTextSupportPeriod) {
					pattern.beforeTextSupport = beforeTextSupport;
					pattern.beforeTextSupportPeriod = period;
				}

				final int commits = (int) row.getCell(12).getNumericCellValue();
				pattern.commits += commits;
				final int bugfixCommits = (int) row.getCell(13).getNumericCellValue();
				pattern.bugfixCommits += bugfixCommits;

				final String firstDate = row.getCell(14).getStringCellValue();
				pattern.addDate(firstDate);
				final String lastDate = row.getCell(15).getStringCellValue();
				pattern.addDate(lastDate);

				final double occupancy = row.getCell(17).getNumericCellValue();
				pattern.addOccupancy(period, occupancy);
				final double deltaCFPF = row.getCell(18).getNumericCellValue();
				pattern.addDeltaCFPF(period, deltaCFPF);

				final String authorText = row.getCell(21).getStringCellValue();
				pattern.addAuthors(authorText);
				final String bugfixAuthorText = row.getCell(22).getStringCellValue();
				pattern.addBugfixAuthors(bugfixAuthorText);

				final String fileText = row.getCell(23).getStringCellValue();
				pattern.addFiles(fileText);
				final String bugfixFileText = row.getCell(24).getStringCellValue();
				pattern.addBugfixFiles(bugfixFileText);
			}
		}

		catch (final IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private static void writeXLSX(final Map<SIMPLE_PATTERN, PATTERN> patterns, final int allBugfixCommits,
			final int allNonbugfixCommits, final List<Integer> bugfixFileData, final List<Integer> fileDiffData,
			final List<Integer> dayDifferenceData, final String xlsxPath) {

		try (final Workbook book = new XSSFWorkbook(); final OutputStream stream = new FileOutputStream(xlsxPath)) {

			Cell firstCell = null;
			Cell lastCell = null;

			final Sheet sheet = book.createSheet();
			book.setSheetName(0, "merged change patterns");
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

			firstCell = titleRow.getCell(0);
			lastCell = titleRow.getCell(28);

			setCellComment(titleRow.getCell(2), "Higo", "number of periods detected or not detected by FindBugs", 4, 1);
			setCellComment(titleRow.getCell(3), "Higo",
					"the number of authors that committed the change pattern in all commits", 5, 1);
			setCellComment(titleRow.getCell(4), "Higo",
					"the number of authors commited the change pattern in bug-fix commits", 5, 1);
			setCellComment(titleRow.getCell(5), "Higo",
					"the number of files where the change pattern appeared in all commits", 5, 1);
			setCellComment(titleRow.getCell(6), "Higo",
					"the number of files where the change pattern appeared in bug-fix commits", 5, 1);
			setCellComment(titleRow.getCell(7), "Higo", "the number of occurences of a given pattern", 4, 1);
			setCellComment(titleRow.getCell(8), "Higo",
					"the number of occurences of a given pattern in bug-fix commits", 4, 1);
			setCellComment(titleRow.getCell(9), "Higo",
					"the number of code fragments whose texts are " + "identical to before-text of a given pattern "
							+ "in the commit where the pattern appears initially",
					4, 2);
			setCellComment(titleRow.getCell(10), "Higo", "BUG-FIX-SUPPORT / SUPPORT", 4, 1);
			setCellComment(titleRow.getCell(11), "Higo", "SUPPORT / BEFORE-TEXT-SUPPORT", 4, 1);
			setCellComment(titleRow.getCell(12), "Higo", "BUG-FIX-SUPPORT / BEFORE-TEXT-SUPPORT", 4, 1);
			setCellComment(titleRow.getCell(13), "Higo", "the number of commits where the pattern appears", 4, 1);
			setCellComment(titleRow.getCell(14), "Higo", "the number of bug-fix commits where the pattern appears", 4,
					1);
			setCellComment(titleRow.getCell(18), "Higo",
					"average of (LOC of a given pattern changed in revision R) / "
							+ "(total LOC changed in revision R) " + "for all the revisions where the pattern appears",
					4, 2);
			setCellComment(titleRow.getCell(19), "Higo",
					"delta-CFPF was calculated with the following formula" + System.lineSeparator() + "pf*(cf1 - cf2)"
							+ System.lineSeparator()
							+ "pf: pattern frequency, which is calculated as support / before-text-support"
							+ System.lineSeparator()
							+ "cf1: bug-fix commit frequensy, which is calculated as bug-fix commits / all bug-fix commits"
							+ System.lineSeparator()
							+ "cf2: non-bug-fix commit frequency, which is calculated as non-bug-fix commits / all non-bug-fix commits",
					4, 5);
			setCellComment(titleRow.getCell(20), "Higo", "Ranking of the number of bug-fix files", 3, 1);
			setCellComment(titleRow.getCell(21), "Higo", "Ranking of difference between files and bug-fix files", 4, 1);
			setCellComment(titleRow.getCell(22), "Higo", "Ranking of date difference", 3, 1);

			int currentRow = 1;
			final List<PATTERN> patternlist = new ArrayList<>(patterns.values());
			Collections.sort(patternlist, (o1, o2) -> Integer.compare(o1.bugfixCommits, o2.bugfixCommits));
			for (final PATTERN cp : patternlist) {

				if (cp.beforeText.isEmpty() || cp.afterText.isEmpty()) {
					continue;
				}

				final Row dataRow = sheet.createRow(currentRow++);
				dataRow.createCell(0).setCellValue(dataRow.getRowNum());
				dataRow.createCell(1).setCellValue(cp.getIDsText());
				dataRow.createCell(2).setCellValue(cp.findbugsSupport);
				dataRow.createCell(3).setCellValue(cp.getAuthors().size());
				dataRow.createCell(4).setCellValue(cp.getBugfixAuthors().size());
				dataRow.createCell(5).setCellValue(cp.getFiles().size());
				dataRow.createCell(6).setCellValue(cp.getBugfixFiles().size());
				dataRow.createCell(7).setCellValue(cp.support);
				dataRow.createCell(8).setCellValue(cp.bugfixSupport);
				dataRow.createCell(9).setCellValue(cp.beforeTextSupport);
				dataRow.createCell(10).setCellValue((float) cp.bugfixSupport / (float) cp.support);
				dataRow.createCell(11).setCellValue((float) cp.support / (float) cp.beforeTextSupport);
				dataRow.createCell(12).setCellValue((float) cp.bugfixSupport / (float) cp.beforeTextSupport);
				dataRow.createCell(13).setCellValue(cp.commits);
				dataRow.createCell(14).setCellValue(cp.bugfixCommits);
				dataRow.createCell(15).setCellValue(cp.getFirstDate());
				dataRow.createCell(16).setCellValue(cp.getLastDate());
				dataRow.createCell(17).setCellValue(getDayDifference(cp.getFirstDate(), cp.getLastDate()));
				dataRow.createCell(18).setCellValue(cp.getMaxOccuapncy());
				dataRow.createCell(19).setCellValue(cp.getDeltaCFPF(allBugfixCommits, allNonbugfixCommits));

				dataRow.createCell(20).setCellValue(getRank(bugfixFileData, cp.bugfixFiles.size(), 1));
				dataRow.createCell(21)
						.setCellValue(getRank(fileDiffData, (cp.files.size() - cp.bugfixFiles.size()), 0));
				dataRow.createCell(22).setCellValue(
						getRank(dayDifferenceData, getDayDifference(cp.getFirstDate(), cp.getLastDate()), 0));

				dataRow.createCell(23).setCellValue(
						cp.beforeText.length() > 32767 ? cp.beforeText.substring(0, 32767) : cp.beforeText);
				dataRow.createCell(24)
						.setCellValue(cp.afterText.length() > 32767 ? cp.afterText.substring(0, 32767) : cp.afterText);
				dataRow.createCell(25).setCellValue(StringUtility.concatinate(cp.getAuthors()));
				dataRow.createCell(26).setCellValue(StringUtility.concatinate(cp.getBugfixAuthors()));
				dataRow.createCell(27).setCellValue(StringUtility.concatinate(cp.getFiles()));
				dataRow.createCell(28).setCellValue(StringUtility.concatinate(cp.getBugfixFiles()));
				lastCell = dataRow.getCell(28);

				setCellComment(dataRow.getCell(9), "Higo", cp.beforeTextSupportPeriod, 1, 1);
				setCellComment(dataRow.getCell(18), "Higo", cp.getOccupanciesText(), 3, cp.getPeriods().size());
				setCellComment(dataRow.getCell(19), "Higo", cp.getDeltaCFPFsText(), 3, cp.getPeriods().size());

				final CellStyle style = book.createCellStyle();
				style.setWrapText(true);
				style.setFillPattern(CellStyle.SOLID_FOREGROUND);
				style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
				style.setBorderBottom(XSSFCellStyle.BORDER_THIN);
				style.setBorderLeft(XSSFCellStyle.BORDER_THIN);
				style.setBorderRight(XSSFCellStyle.BORDER_THIN);
				style.setBorderTop(XSSFCellStyle.BORDER_THIN);
				style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
				for (int column = 0; column <= 25; column++) {
					dataRow.getCell(column).setCellStyle(style);
				}

				final int[] locs = new int[] { cp.getIDs().size(), cp.getAuthors().size(), cp.getFiles().size(),
						StringUtility.getLOC(cp.beforeText), StringUtility.getLOC(cp.afterText) };
				Arrays.sort(locs);
				dataRow.setHeight((short) (locs[locs.length - 1] * dataRow.getHeight()));
			}

			sheet.autoSizeColumn(0, true);
			sheet.setColumnWidth(1, 5120);
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
			sheet.setColumnWidth(24, 5120);
			sheet.setColumnWidth(25, 5120);
			sheet.setColumnWidth(26, 20480);
			sheet.setColumnWidth(27, 20480);
			sheet.setColumnWidth(28, 20480);

			sheet.setAutoFilter(new CellRangeAddress(firstCell.getRowIndex(), lastCell.getRowIndex(),
					firstCell.getColumnIndex(), lastCell.getColumnIndex()));
			sheet.createFreezePane(0, 1, 0, 1);

			book.write(stream);

		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private static String getRank(final List<Integer> data, final int value, final int lowestValue) {

		if (value == lowestValue) {
			return "NONE";
		}

		int matchedIndex = 0;
		for (int index = 0; index < data.size(); index++) {
			if (data.get(index) == value) {
				matchedIndex = index;
				break;
			}
		}

		// System.out.println(matchedIndex + " : " + data.size());

		final float position = (float) matchedIndex / (float) data.size();
		if (position < 0.334) {
			return "LOW";
		} else if (position < 0.667) {
			return "MIDDLE";
		} else {
			return "HIGH";
		}
	}

	private static void setCellComment(final Cell cell, final String author, final String text, final int width,
			final int height) {

		final Sheet sheet = cell.getSheet();
		final Workbook workbook = sheet.getWorkbook();
		final CreationHelper helper = workbook.getCreationHelper();

		final Drawing drawing = sheet.createDrawingPatriarch();
		final ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, (short) 4, 2, (short) (4 + width), (2 + height));
		final Comment comment = drawing.createCellComment(anchor);
		comment.setAuthor(author);
		comment.setString(helper.createRichTextString(text));
		cell.setCellComment(comment);
	}

	private static int getDayDifference(final String firstdate, final String lastdate) {

		try {
			final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			final Date date1 = dateFormat.parse(firstdate);
			final Date date2 = dateFormat.parse(lastdate);
			final long difference = date2.getTime() - date1.getTime();
			return (int) (difference / 1000l / 60l / 60l / 24l);
		}

		catch (java.text.ParseException e) {
			e.printStackTrace();
		}

		return 0;
	}

	static public class SIMPLE_PATTERN {

		final public String beforeText;
		final public String afterText;
		final public List<String> beforeTextPattern;
		final public List<String> afterTextPattern;

		public SIMPLE_PATTERN(final String beforeText, final String afterText) {
			this.beforeText = beforeText;
			this.afterText = afterText;
			this.beforeTextPattern = StringUtility.split(beforeText);
			this.afterTextPattern = StringUtility.split(afterText);
		}

		@Override
		final public boolean equals(final Object o) {

			if (!(o instanceof SIMPLE_PATTERN)) {
				return false;
			}

			final SIMPLE_PATTERN target = (SIMPLE_PATTERN) o;
			return this.beforeText.equals(target.beforeText) && this.afterText.equals(target.afterText);
		}

		@Override
		final public int hashCode() {
			return this.beforeText.hashCode() + this.afterText.hashCode();
		}
	}

	static public class PATTERN extends SIMPLE_PATTERN {

		final public List<byte[]> beforeTextHashs;
		final public List<byte[]> afterTextHashs;
		final public List<String> periods;
		final public List<String> ids;
		public int mergedID;
		public int findbugsSupport;
		public int support;
		public int bugfixSupport;
		public int beforeTextSupport;
		public String beforeTextSupportPeriod;
		public int commits;
		public int bugfixCommits;
		private String firstDate;
		private String lastDate;
		final public Map<String, Double> occupancies;
		final public Map<String, Double> deltaCFPFs;
		final private SortedSet<String> files;
		final private SortedSet<String> bugfixFiles;
		final private SortedSet<String> authors;
		final private SortedSet<String> bugfixAuthors;

		public PATTERN(final String beforeText, final String afterText) {
			super(beforeText, afterText);
			this.beforeTextHashs = StringUtility.split(beforeText).stream().map(text -> Statement.getMD5(text))
					.collect(Collectors.toList());
			this.afterTextHashs = StringUtility.split(afterText).stream().map(text -> Statement.getMD5(text))
					.collect(Collectors.toList());
			this.periods = new ArrayList<>();
			this.ids = new ArrayList<>();
			this.mergedID = 0;
			this.findbugsSupport = 0;
			this.support = 0;
			this.bugfixSupport = 0;
			this.beforeTextSupport = 0;
			this.beforeTextSupportPeriod = null;
			this.commits = 0;
			this.bugfixCommits = 0;
			this.firstDate = null;
			this.lastDate = null;
			this.occupancies = new HashMap<>();
			this.deltaCFPFs = new HashMap<>();
			this.files = new TreeSet<>();
			this.bugfixFiles = new TreeSet<>();
			this.authors = new TreeSet<>();
			this.bugfixAuthors = new TreeSet<>();
		}

		public void addPeriod(final String period) {
			this.periods.add(period);
		}

		public List<String> getPeriods() {
			return new ArrayList<String>(this.periods);
		}

		public void addID(final String id) {
			this.ids.add(id);
		}

		public String getIDsText() {
			return yoshikihigo.fbparser.StringUtility.concatinate(this.ids);
		}

		public List<String> getIDs() {
			return new ArrayList<String>(this.ids);
		}

		public void addFiles(final String fileText) {
			this.files.addAll(StringUtility.split(fileText));
		}

		public SortedSet<String> getFiles() {
			return new TreeSet<String>(this.files);
		}

		public void addBugfixFiles(final String fileText) {
			this.bugfixFiles.addAll(StringUtility.split(fileText));
		}

		public SortedSet<String> getBugfixFiles() {
			return new TreeSet<String>(this.bugfixFiles);
		}

		public void addAuthors(final String authorText) {
			this.authors.addAll(StringUtility.split(authorText));
		}

		public SortedSet<String> getAuthors() {
			return new TreeSet<String>(this.authors);
		}

		public void addBugfixAuthors(final String authorText) {
			this.bugfixAuthors.addAll(StringUtility.split(authorText));
		}

		public SortedSet<String> getBugfixAuthors() {
			return new TreeSet<String>(this.bugfixAuthors);
		}

		public void addDate(final String date) {
			if ((null == this.firstDate) || (0 < this.firstDate.compareTo(date))) {
				this.firstDate = date;
			}
			if ((null == this.lastDate) || (0 < date.compareTo(this.lastDate))) {
				this.lastDate = date;
			}
		}

		public String getFirstDate() {
			return this.firstDate;
		}

		public String getLastDate() {
			return this.lastDate;
		}

		public void addOccupancy(final String period, final Double occupancy) {
			this.occupancies.put(period, occupancy);
		}

		public Double getMaxOccuapncy() {
			double max = 0d;
			for (final Double occupancy : this.occupancies.values()) {
				if (max < occupancy) {
					max = occupancy;
				}
			}
			return max;
		}

		public String getOccupanciesText() {
			final StringBuilder text = new StringBuilder();
			for (final Entry<String, Double> entry : this.occupancies.entrySet()) {
				text.append(entry.getKey());
				text.append(": ");
				text.append(entry.getValue());
				text.append(System.lineSeparator());
			}
			return text.toString();
		}

		public void addDeltaCFPF(final String period, final Double deltaCFPF) {
			this.deltaCFPFs.put(period, deltaCFPF);
		}

		public Double getDeltaCFPF(final int allBugfixCommits, final int allNonbugfixCommits) {
			final double pf = (double) this.support / (double) this.beforeTextSupport;
			final double cf1 = (double) this.bugfixCommits / (double) allBugfixCommits;
			final double cf2 = (double) (this.commits - this.bugfixCommits) / (double) allNonbugfixCommits;
			final double pfcf = pf * (cf1 - cf2);
			return pfcf;
		}

		public String getDeltaCFPFsText() {
			final StringBuilder text = new StringBuilder();
			for (final Entry<String, Double> entry : this.deltaCFPFs.entrySet()) {
				text.append(entry.getKey());
				text.append(": ");
				text.append(entry.getValue());
				text.append(System.lineSeparator());
			}
			return text.toString();
		}
	}
}
