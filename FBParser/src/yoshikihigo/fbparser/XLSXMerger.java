package yoshikihigo.fbparser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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

//				System.out.print(support + ", ");
//				System.out.print(bugfixSupport + ", ");
//				System.out.print(beforeTextSupport + ", ");
//				System.out.print(commits + ", ");
//				System.out.print(bugfixCommits + ", ");
//				System.out.print(firstDate + ", ");
//				System.out.print(lastDate + ", ");
//				System.out.print(occupancy + ", ");
//				System.out.print(deltaTFIDF + ", ");
//				System.out.println();
			}
		}

		catch (final IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
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

		final private SortedSet<String> files;
		final private SortedSet<String> authors;
		int support;
		int bugfixSupport;
		int beforeTextSupport;
		int commits;
		int bugfixCommits;
		private String firstDate;
		private String lastDate;
		final List<Double> occupancies;
		final List<Double> deltaTFIDFs;

		PATTERN(final String beforeText, final String afterText) {
			super(beforeText, afterText);
			this.files = new TreeSet<>();
			this.authors = new TreeSet<>();
			this.support = 0;
			this.bugfixSupport = 0;
			this.beforeTextSupport = 0;
			this.commits = 0;
			this.bugfixCommits = 0;
			this.firstDate = null;
			this.lastDate = null;
			this.occupancies = new ArrayList<>();
			this.deltaTFIDFs = new ArrayList<>();
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
					|| (0 < date.compareTo(this.firstDate))) {
				this.firstDate = date;
			}
			if ((null == this.lastDate) || (0 < this.lastDate.compareTo(date))) {
				this.lastDate = date;
			}
		}

		public void addOccupancy(final Double occupancy) {
			this.occupancies.add(occupancy);
		}

		public void addDeltaTFIDF(final Double deltaTFIDF) {
			this.deltaTFIDFs.add(deltaTFIDF);
		}
	}
}
