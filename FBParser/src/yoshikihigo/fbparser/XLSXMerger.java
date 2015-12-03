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
		for (int index = 1; index < args.length; index++) {
			readXLSX(patterns, args[index]);
		}

		writeXLSX(patterns, mergedXLSXPath);
	}

	private static void readXLSX(final Map<SIMPLE_PATTERN, PATTERN> patterns,
			final String xlsxPath) {

		final String period = yoshikihigo.fbparser.StringUtility
				.removeExtension(yoshikihigo.fbparser.StringUtility
						.getName(xlsxPath));

		try (final Workbook book = new XSSFWorkbook(new FileInputStream(
				xlsxPath))) {
			final Sheet sheet = book.getSheetAt(0);
			final int lastRowNumber = sheet.getLastRowNum();
			for (int rowNumber = 1; rowNumber < lastRowNumber; rowNumber++) {
				final Row row = sheet.getRow(rowNumber);
				final String beforeText = row.getCell(19).getStringCellValue();
				final String afterText = row.getCell(20).getStringCellValue();
				final SIMPLE_PATTERN p = new SIMPLE_PATTERN(beforeText,
						afterText);
				PATTERN pattern = patterns.get(p);
				if (null == pattern) {
					pattern = new PATTERN(beforeText, afterText);
					patterns.put(p, pattern);
				}

				pattern.addPeriod(period);

				final String id = period
						+ ": "
						+ Integer.toString((int) row.getCell(0)
								.getNumericCellValue());
				pattern.addID(id);

				final int findbugsSupport = (int) row.getCell(1)
						.getNumericCellValue();
				pattern.findbugsSupport += findbugsSupport;

				final int support = (int) row.getCell(6).getNumericCellValue();
				pattern.support += support;
				final int bugfixSupport = (int) row.getCell(7)
						.getNumericCellValue();
				pattern.bugfixSupport += bugfixSupport;
				final int beforeTextSupport = (int) row.getCell(8)
						.getNumericCellValue();
				pattern.setBeforeTextSupport(beforeTextSupport, period);

				final int commits = (int) row.getCell(12).getNumericCellValue();
				pattern.commits += commits;
				final int bugfixCommits = (int) row.getCell(13)
						.getNumericCellValue();
				pattern.bugfixCommits += bugfixCommits;

				final String firstDate = row.getCell(14).getStringCellValue();
				pattern.addDate(firstDate);
				final String lastDate = row.getCell(15).getStringCellValue();
				pattern.addDate(lastDate);

				final double occupancy = row.getCell(17).getNumericCellValue();
				pattern.addOccupancy(period, occupancy);
				final double deltaTFIDF = row.getCell(18).getNumericCellValue();
				pattern.addDeltaTFIDF(period, deltaTFIDF);

				final String authorText = row.getCell(21).getStringCellValue();
				pattern.addAuthors(authorText);
				final String bugfixAuthorText = row.getCell(22)
						.getStringCellValue();
				pattern.addBugfixAuthors(bugfixAuthorText);

				final String fileText = row.getCell(23).getStringCellValue();
				pattern.addFiles(fileText);
				final String bugfixFileText = row.getCell(24)
						.getStringCellValue();
				pattern.addBugfixFiles(bugfixFileText);
			}
		}

		catch (final IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private static void writeXLSX(final Map<SIMPLE_PATTERN, PATTERN> patterns,
			final String xlsxPath) {

		try (final Workbook book = new XSSFWorkbook();
				final OutputStream stream = new FileOutputStream(xlsxPath)) {

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
			titleRow.createCell(19).setCellValue("Delta-TFIDF");
			titleRow.createCell(20).setCellValue("TEXT-BEFORE-CHANGE");
			titleRow.createCell(21).setCellValue("TEXT-AFTER-CHANGE");
			titleRow.createCell(22).setCellValue("AUTHOR-LIST");
			titleRow.createCell(23).setCellValue("BUG-FIX-AUTHOR-LIST");
			titleRow.createCell(24).setCellValue("FILE-LIST");
			titleRow.createCell(25).setCellValue("BUG-FIX-FILE-LIST");

			firstCell = titleRow.getCell(0);
			lastCell = titleRow.getCell(25);

			setCellComment(titleRow.getCell(2), "Higo",
					"number of periods detected or not detected by FindBugs",
					4, 1);
			setCellComment(
					titleRow.getCell(3),
					"Higo",
					"the number of authors that committed the change pattern in all commits",
					5, 1);
			setCellComment(
					titleRow.getCell(4),
					"Higo",
					"the number of authors commited the change pattern in bug-fix commits",
					5, 1);
			setCellComment(
					titleRow.getCell(5),
					"Higo",
					"the number of files where the change pattern appeared in all commits",
					5, 1);
			setCellComment(
					titleRow.getCell(6),
					"Higo",
					"the number of files where the change pattern appeared in bug-fix commits",
					5, 1);
			setCellComment(titleRow.getCell(7), "Higo",
					"the number of occurences of a given pattern", 4, 1);
			setCellComment(
					titleRow.getCell(8),
					"Higo",
					"the number of occurences of a given pattern in bug-fix commits",
					4, 1);
			setCellComment(
					titleRow.getCell(9),
					"Higo",
					"the number of code fragments whose texts are "
							+ "identical to before-text of a given pattern "
							+ "in the commit where the pattern appears initially",
					4, 2);
			setCellComment(titleRow.getCell(10), "Higo",
					"BUG-FIX-SUPPORT / SUPPORT", 4, 1);
			setCellComment(titleRow.getCell(11), "Higo",
					"SUPPORT / BEFORE-TEXT-SUPPORT", 4, 1);
			setCellComment(titleRow.getCell(12), "Higo",
					"BUG-FIX-SUPPORT / BEFORE-TEXT-SUPPORT", 4, 1);
			setCellComment(titleRow.getCell(13), "Higo",
					"the number of commits where the pattern appears", 4, 1);
			setCellComment(titleRow.getCell(14), "Higo",
					"the number of bug-fix commits where the pattern appears",
					4, 1);
			setCellComment(
					titleRow.getCell(18),
					"Higo",
					"average of (LOC of a given pattern changed in revision R) / "
							+ "(total LOC changed in revision R) "
							+ "for all the revisions where the pattern appears",
					4, 2);
			setCellComment(
					titleRow.getCell(19),
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
					4);

			int currentRow = 1;
			final List<PATTERN> patternlist = new ArrayList<>(patterns.values());
			Collections.sort(patternlist, (o1, o2) -> Integer.compare(
					o1.bugfixCommits, o2.bugfixCommits));
			for (final PATTERN cp : patternlist) {

				if (cp.beforeText.isEmpty()) {
					continue;
				}

				final Row dataRow = sheet.createRow(currentRow++);
				dataRow.createCell(0).setCellValue(dataRow.getRowNum());
				dataRow.createCell(1).setCellValue(cp.getIDsText());
				dataRow.createCell(2).setCellValue(cp.findbugsSupport);
				dataRow.createCell(3).setCellValue(cp.getAuthors().size());
				dataRow.createCell(4)
						.setCellValue(cp.getBugfixAuthors().size());
				dataRow.createCell(5).setCellValue(cp.getFiles().size());
				dataRow.createCell(6).setCellValue(cp.getBugfixFiles().size());
				dataRow.createCell(7).setCellValue(cp.support);
				dataRow.createCell(8).setCellValue(cp.bugfixSupport);
				dataRow.createCell(9).setCellValue(cp.getBeforeTextSupport());
				dataRow.createCell(10).setCellValue(
						(float) cp.bugfixSupport / (float) cp.support);
				dataRow.createCell(11).setCellValue(
						(float) cp.support / (float) cp.getBeforeTextSupport());
				dataRow.createCell(12).setCellValue(
						(float) cp.bugfixSupport
								/ (float) cp.getBeforeTextSupport());
				dataRow.createCell(13).setCellValue(cp.commits);
				dataRow.createCell(14).setCellValue(cp.commits);
				dataRow.createCell(15).setCellValue(cp.getFirstDate());
				dataRow.createCell(16).setCellValue(cp.getLastDate());
				dataRow.createCell(17).setCellValue(
						getDayDifference(cp.getFirstDate(), cp.getLastDate()));
				dataRow.createCell(18).setCellValue(cp.getMaxOccuapncy());
				dataRow.createCell(19).setCellValue(cp.getMaxDeltaTFIDF());
				dataRow.createCell(20).setCellValue(cp.beforeText);
				dataRow.createCell(21).setCellValue(cp.afterText);
				dataRow.createCell(22).setCellValue(
						yoshikihigo.fbparser.StringUtility.concatinate(cp
								.getAuthors()));
				dataRow.createCell(23).setCellValue(
						yoshikihigo.fbparser.StringUtility.concatinate(cp
								.getBugfixAuthors()));
				dataRow.createCell(24).setCellValue(
						yoshikihigo.fbparser.StringUtility.concatinate(cp
								.getFiles()));
				dataRow.createCell(25).setCellValue(
						yoshikihigo.fbparser.StringUtility.concatinate(cp
								.getBugfixFiles()));
				lastCell = dataRow.getCell(25);

				setCellComment(dataRow.getCell(9), "Higo",
						cp.getBeforeTextSupportPeriod(), 1, 1);
				setCellComment(dataRow.getCell(18), "Higo",
						cp.getOccupanciesText(), 3, cp.getPeriods().size());
				setCellComment(dataRow.getCell(19), "Higo",
						cp.getDeltaTFIDFsText(), 3, cp.getPeriods().size());

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

				final int[] locs = new int[] {
						cp.getIDs().size(),
						cp.getAuthors().size(),
						cp.getFiles().size(),
						yoshikihigo.fbparser.StringUtility
								.getLOC(cp.beforeText),
						yoshikihigo.fbparser.StringUtility.getLOC(cp.afterText) };
				Arrays.sort(locs);
				dataRow.setHeight((short) (locs[locs.length - 1] * dataRow
						.getHeight()));
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
			sheet.setColumnWidth(20, 20480);
			sheet.setColumnWidth(21, 5120);
			sheet.setColumnWidth(22, 5120);
			sheet.setColumnWidth(23, 20480);
			sheet.setColumnWidth(24, 20480);
			sheet.setColumnWidth(25, 20480);

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

	private static int getDayDifference(final String firstdate,
			final String lastdate) {

		try {
			final SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yyyy/MM/dd HH:mm:ss");
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

	static class SIMPLE_PATTERN {

		final public String beforeText;
		final public String afterText;

		SIMPLE_PATTERN(final String beforeText, final String afterText) {
			this.beforeText = beforeText;
			this.afterText = afterText;
		}

		@Override
		final public boolean equals(final Object o) {

			if (!(o instanceof SIMPLE_PATTERN)) {
				return false;
			}

			final SIMPLE_PATTERN target = (SIMPLE_PATTERN) o;
			return this.beforeText.equals(target.beforeText)
					&& this.afterText.equals(target.afterText);
		}

		@Override
		final public int hashCode() {
			return this.beforeText.hashCode() + this.afterText.hashCode();
		}
	}

	static class PATTERN extends SIMPLE_PATTERN {

		final List<String> periods;
		final List<String> ids;
		int findbugsSupport;
		int support;
		int bugfixSupport;
		private int beforeTextSupport;
		private String beforeTextSupportPeriod;
		int commits;
		int bugfixCommits;
		private String firstDate;
		private String lastDate;
		final Map<String, Double> occupancies;
		final Map<String, Double> deltaTFIDFs;
		final private SortedSet<String> files;
		final private SortedSet<String> bugfixFiles;
		final private SortedSet<String> authors;
		final private SortedSet<String> bugfixAuthors;

		PATTERN(final String beforeText, final String afterText) {
			super(beforeText, afterText);
			this.periods = new ArrayList<>();
			this.ids = new ArrayList<>();
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
			this.deltaTFIDFs = new HashMap<>();
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
			this.files.addAll(yoshikihigo.fbparser.StringUtility
					.split(fileText));
		}

		public SortedSet<String> getFiles() {
			return new TreeSet<String>(this.files);
		}

		public void addBugfixFiles(final String fileText) {
			this.bugfixFiles.addAll(yoshikihigo.fbparser.StringUtility
					.split(fileText));
		}

		public SortedSet<String> getBugfixFiles() {
			return new TreeSet<String>(this.bugfixFiles);
		}

		public void addAuthors(final String authorText) {
			this.authors.addAll(yoshikihigo.fbparser.StringUtility
					.split(authorText));
		}

		public SortedSet<String> getAuthors() {
			return new TreeSet<String>(this.authors);
		}

		public void addBugfixAuthors(final String authorText) {
			this.bugfixAuthors.addAll(yoshikihigo.fbparser.StringUtility
					.split(authorText));
		}

		public SortedSet<String> getBugfixAuthors() {
			return new TreeSet<String>(this.bugfixAuthors);
		}

		public void setBeforeTextSupport(final int beforeTextSupport,
				final String beforeTextSupportPeriod) {
			if (null == this.beforeTextSupportPeriod) {
				this.beforeTextSupport = beforeTextSupport;
				this.beforeTextSupportPeriod = beforeTextSupportPeriod;
			}
		}

		public int getBeforeTextSupport() {
			return this.beforeTextSupport;
		}

		public String getBeforeTextSupportPeriod() {
			return this.beforeTextSupportPeriod;
		}

		public void addDate(final String date) {
			if ((null == this.firstDate)
					|| (0 < this.firstDate.compareTo(date))) {
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
			for (final Entry<String, Double> entry : this.occupancies
					.entrySet()) {
				text.append(entry.getKey());
				text.append(": ");
				text.append(entry.getValue());
				text.append(System.lineSeparator());
			}
			return text.toString();
		}

		public void addDeltaTFIDF(final String period, final Double deltaTFIDF) {
			this.deltaTFIDFs.put(period, deltaTFIDF);
		}

		public Double getMaxDeltaTFIDF() {
			double max = 0d;
			for (final Double deltaTFIDF : this.deltaTFIDFs.values()) {
				if (max < deltaTFIDF) {
					max = deltaTFIDF;
				}
			}
			return max;
		}

		public String getDeltaTFIDFsText() {
			final StringBuilder text = new StringBuilder();
			for (final Entry<String, Double> entry : this.deltaTFIDFs
					.entrySet()) {
				text.append(entry.getKey());
				text.append(": ");
				text.append(entry.getValue());
				text.append(System.lineSeparator());
			}
			return text.toString();
		}
	}
}
