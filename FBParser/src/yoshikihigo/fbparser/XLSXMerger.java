package yoshikihigo.fbparser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.StringTokenizer;
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

				final String found = row.getCell(1).getStringCellValue();
				pattern.addFoundByFindbugs(found);

				final int support = (int) row.getCell(6).getNumericCellValue();
				pattern.support += support;
				final int bugfixSupport = (int) row.getCell(7)
						.getNumericCellValue();
				pattern.bugfixSupport += bugfixSupport;
				final int beforeTextSupport = (int) row.getCell(8)
						.getNumericCellValue();
				pattern.beforeTextSupport += beforeTextSupport;

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
				pattern.addOccupancy(occupancy);
				final double deltaTFIDF = row.getCell(18).getNumericCellValue();
				pattern.addDeltaTFIDF(deltaTFIDF);

				final String authorText = row.getCell(21).getStringCellValue();
				pattern.addAuthors(authorText);

				final String fileText = row.getCell(22).getStringCellValue();
				pattern.addFiles(fileText);

				// System.out.print(support + ", ");
				// System.out.print(bugfixSupport + ", ");
				// System.out.print(beforeTextSupport + ", ");
				// System.out.print(commits + ", ");
				// System.out.print(bugfixCommits + ", ");
				// System.out.print(firstDate + ", ");
				// System.out.print(lastDate + ", ");
				// System.out.print(occupancy + ", ");
				// System.out.print(deltaTFIDF + ", ");
				// System.out.println();
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
			titleRow.createCell(0).setCellValue("PATTERN-ID");
			titleRow.createCell(1).setCellValue("FOUND-BY-FINDBUGS");
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
			titleRow.createCell(22).setCellValue("FILE-LIST");

			firstCell = titleRow.getCell(0);
			lastCell = titleRow.getCell(22);

			setCellComment(
					titleRow.getCell(2),
					"Higo",
					"the number of authors that committed the change pattern in all commits",
					5, 1);
			setCellComment(
					titleRow.getCell(3),
					"Higo",
					"the number of authors commited the change pattern in bug-fix commits",
					5, 1);
			setCellComment(
					titleRow.getCell(4),
					"Higo",
					"the number of files where the change pattern appeared in all commits",
					5, 1);
			setCellComment(
					titleRow.getCell(5),
					"Higo",
					"the number of files where the change pattern appeared in bug-fix commits",
					5, 1);
			setCellComment(titleRow.getCell(6), "Higo",
					"the number of occurences of a given pattern", 4, 1);
			setCellComment(
					titleRow.getCell(7),
					"Higo",
					"the number of occurences of a given pattern in bug-fix commits",
					4, 1);
			setCellComment(
					titleRow.getCell(8),
					"Higo",
					"the number of code fragments whose texts are "
							+ "identical to before-text of a given pattern "
							+ "in the commit where the pattern appears initially",
					4, 2);
			setCellComment(titleRow.getCell(9), "Higo",
					"BUG-FIX-SUPPORT / SUPPORT", 4, 1);
			setCellComment(titleRow.getCell(10), "Higo",
					"SUPPORT / BEFORE-TEXT-SUPPORT", 4, 1);
			setCellComment(titleRow.getCell(11), "Higo",
					"BUG-FIX-SUPPORT / BEFORE-TEXT-SUPPORT", 4, 1);
			setCellComment(titleRow.getCell(12), "Higo",
					"the number of commits where the pattern appears", 4, 1);
			setCellComment(titleRow.getCell(13), "Higo",
					"the number of bug-fix commits where the pattern appears",
					4, 1);
			setCellComment(
					titleRow.getCell(17),
					"Higo",
					"average of (LOC of a given pattern changed in revision R) / "
							+ "(total LOC changed in revision R) "
							+ "for all the revisions where the pattern appears",
					4, 2);
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
				dataRow.createCell(0).setCellValue(" --- ");
				dataRow.createCell(1).setCellValue(cp.getFoundByFindBugs());
				dataRow.createCell(2).setCellValue(cp.getAuthors().size());
				dataRow.createCell(3).setCellValue(cp.getAuthors().size());
				dataRow.createCell(4).setCellValue(cp.getFiles().size());
				dataRow.createCell(5).setCellValue(cp.getFiles().size());
				dataRow.createCell(6).setCellValue(cp.support);
				dataRow.createCell(7).setCellValue(cp.bugfixSupport);
				dataRow.createCell(8).setCellValue(cp.beforeTextSupport);
				dataRow.createCell(9).setCellValue(
						(float) cp.bugfixSupport / (float) cp.support);
				dataRow.createCell(10).setCellValue(
						(float) cp.support / (float) cp.beforeTextSupport);
				dataRow.createCell(11)
						.setCellValue(
								(float) cp.bugfixSupport
										/ (float) cp.beforeTextSupport);
				dataRow.createCell(12).setCellValue(cp.commits);
				dataRow.createCell(13).setCellValue(cp.commits);
				dataRow.createCell(14).setCellValue(cp.getFirstDate());
				dataRow.createCell(15).setCellValue(cp.getLastDate());
				dataRow.createCell(16).setCellValue(
						getDayDifference(cp.getFirstDate(), cp.getLastDate()));
				dataRow.createCell(17).setCellValue(cp.getMaxOccuapncy());
				dataRow.createCell(18).setCellValue(cp.getMaxDeltaTFIDF());
				dataRow.createCell(19).setCellValue(cp.beforeText);
				dataRow.createCell(20).setCellValue(cp.afterText);
				dataRow.createCell(21).setCellValue(
						yoshikihigo.fbparser.StringUtility.concatinate(cp
								.getAuthors()));
				dataRow.createCell(22).setCellValue(
						yoshikihigo.fbparser.StringUtility.concatinate(cp
								.getFiles()));
				lastCell = dataRow.getCell(22);

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
				dataRow.getCell(21).setCellStyle(style);
				dataRow.getCell(22).setCellStyle(style);

				int loc = Math
						.max(yoshikihigo.fbparser.StringUtility
								.getLOC(cp.beforeText),
								yoshikihigo.fbparser.StringUtility
										.getLOC(cp.afterText));
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
			sheet.setColumnWidth(22, 20480);

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

	static class SIMPLE_PATTERN {

		final public String beforeText;
		final public String afterText;

		SIMPLE_PATTERN(final String beforeText, final String afterText) {
			this.beforeText = beforeText;
			this.afterText = afterText;
		}

		@Override
		final public boolean equals(final Object o) {

			if (!(o instanceof PATTERN)) {
				return false;
			}

			final PATTERN target = (PATTERN) o;
			return this.beforeText.equals(target.beforeText)
					&& this.afterText.equals(target.afterText);
		}

		@Override
		final public int hashCode() {
			return this.beforeText.hashCode() + this.afterText.hashCode();
		}
	}

	static class PATTERN extends SIMPLE_PATTERN {

		int[] foundByFindbugs;
		int support;
		int bugfixSupport;
		int beforeTextSupport;
		int commits;
		int bugfixCommits;
		private String firstDate;
		private String lastDate;
		final List<Double> occupancies;
		final List<Double> deltaTFIDFs;
		final private SortedSet<String> files;
		final private SortedSet<String> authors;

		PATTERN(final String beforeText, final String afterText) {
			super(beforeText, afterText);
			this.foundByFindbugs = new int[2];
			this.foundByFindbugs[0] = 0;
			this.foundByFindbugs[1] = 0;
			this.support = 0;
			this.bugfixSupport = 0;
			this.beforeTextSupport = 0;
			this.commits = 0;
			this.bugfixCommits = 0;
			this.firstDate = null;
			this.lastDate = null;
			this.occupancies = new ArrayList<>();
			this.deltaTFIDFs = new ArrayList<>();
			this.files = new TreeSet<>();
			this.authors = new TreeSet<>();
		}

		public void addFoundByFindbugs(final String found) {
			if (found.equalsIgnoreCase("YES")) {
				this.foundByFindbugs[0]++;
			} else if (found.equalsIgnoreCase("NO")) {
				this.foundByFindbugs[1]++;
			} else {
				assert false : "illegal parameter: " + found;
			}
		}

		public String getFoundByFindBugs() {
			final StringBuilder text = new StringBuilder();
			if (0 < this.foundByFindbugs[0]) {
				text.append("YES: ");
				text.append(this.foundByFindbugs[0]);
				if (0 < this.foundByFindbugs[1]) {
					text.append(", ");
				}
			}
			if (0 < this.foundByFindbugs[1]) {
				text.append("NO: ");
				text.append(this.foundByFindbugs[1]);
			}
			return text.toString();
		}

		public void addFiles(final String fileText) {
			try (final BufferedReader reader = new BufferedReader(
					new StringReader(fileText))) {
				while (true) {
					final String line = reader.readLine();
					if (null == line) {
						break;
					}
					this.files.add(line);
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		public SortedSet<String> getFiles() {
			return new TreeSet<String>(this.files);
		}

		public void addAuthors(final String authorText) {
			try (final BufferedReader reader = new BufferedReader(
					new StringReader(authorText))) {
				while (true) {
					final String line = reader.readLine();
					if (null == line) {
						break;
					}
					this.authors.add(line);
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		public SortedSet<String> getAuthors() {
			return new TreeSet<String>(this.authors);
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

		public void addOccupancy(final Double occupancy) {
			this.occupancies.add(occupancy);
		}

		public Double getMaxOccuapncy() {
			double max = 0d;
			for (final Double occupancy : this.occupancies) {
				if (max < occupancy) {
					max = occupancy;
				}
			}
			return max;
		}

		public void addDeltaTFIDF(final Double deltaTFIDF) {
			this.deltaTFIDFs.add(deltaTFIDF);
		}

		public Double getMaxDeltaTFIDF() {
			double max = 0d;
			for (final Double deltaTFIDF : this.deltaTFIDFs) {
				if (max < deltaTFIDF) {
					max = deltaTFIDF;
				}
			}
			return max;
		}
	}
}
