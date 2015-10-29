package yoshikihigo.fbparser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import yoshikihigo.fbparser.db.DAO;
import yoshikihigo.fbparser.db.DAO.CHANGEPATTERN_SQL;
import yoshikihigo.fbparser.db.DAO.CHANGE_SQL;

public class FBChangePatternFinder {

	public static void main(final String[] args) {

		FBParserConfig.initialize(args);
		final String trFile = FBParserConfig.getInstance()
				.getTRANSITIONRESULT();
		final String cpFile = FBParserConfig.getInstance().getCHANGEPATTERN();
		final String mcpFile = FBParserConfig.getInstance()
				.getMISSINGCHANGEPATTERN();
		final DAO dao = DAO.getInstance();

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
							cpWriter.println(cp.support);
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
				titleRow.createCell(0).setCellValue("RANKING");
				titleRow.createCell(1).setCellValue("FOUND-BY-FINDBUGS");
				titleRow.createCell(2).setCellValue("CHANGE-PATTERN-ID");
				titleRow.createCell(3).setCellValue("AUTHORS");
				titleRow.createCell(4).setCellValue("FILES");
				titleRow.createCell(5).setCellValue("SUPPORT");				
				titleRow.createCell(6).setCellValue("BUG-FIX-SUPPORT");
				titleRow.createCell(7).setCellValue("BEFORE-TEXT-SUPPORT");
				titleRow.createCell(8).setCellValue(
						"CONFIDENCE1" + System.lineSeparator()
								+ "(BUG-FIX-SUPPORT/SUPPORT)");
				titleRow.createCell(9).setCellValue(
						"CONFIDENCE2" + System.lineSeparator()
								+ "(SUPPORT/BEFORE-TEXT-SUPPORT)");
				titleRow.createCell(10).setCellValue(
						"CONFIDENCE3" + System.lineSeparator()
								+ "(BUG-FIX-SUPPORT/BEFORE-TEXT-SUPPORT)");
				titleRow.createCell(11).setCellValue("TEXT-BEFORE-CHANGE");
				titleRow.createCell(12).setCellValue("TEXT-AFTER-CHANGE");

				int currentRow = 1;
				int ranking = 1;
				final List<CHANGEPATTERN_SQL> cps = dao.getFixChangePatterns();
				Collections.sort(cps, new Comparator<CHANGEPATTERN_SQL>() {

					@Override
					public int compare(final CHANGEPATTERN_SQL o1,
							final CHANGEPATTERN_SQL o2) {

						final float ratio1 = (float) o1.bugfixSupport
								/ (float) o1.beforetextSupport;
						final float ratio2 = (float) o2.bugfixSupport
								/ (float) o2.beforetextSupport;
						if (ratio1 > ratio2) {
							return -1;
						} else if (ratio2 > ratio1) {
							return 1;
						} else {
							return 0;
						}
					}
				});

				for (final CHANGEPATTERN_SQL cp : cps) {

					if (cp.beforeText.isEmpty()) {
						continue;
					}

					final boolean foundByFindBugs = foundCPs.contains(cp.id);

					final Row dataRow = sheet.createRow(currentRow++);
					dataRow.createCell(0).setCellValue(ranking++);
					dataRow.createCell(1).setCellValue(
							foundByFindBugs ? "YES" : "NO");
					dataRow.createCell(2).setCellValue(cp.id);
					dataRow.createCell(3).setCellValue(cp.authors);
					dataRow.createCell(4).setCellValue(cp.files);
					dataRow.createCell(3).setCellValue(cp.support);
					dataRow.createCell(4).setCellValue(cp.bugfixSupport);
					dataRow.createCell(5).setCellValue(cp.beforetextSupport);
					dataRow.createCell(6).setCellValue(
							(float) cp.bugfixSupport / (float) cp.support);
					dataRow.createCell(7).setCellValue(
							(float) cp.support / (float) cp.beforetextSupport);
					dataRow.createCell(8).setCellValue(
							(float) cp.bugfixSupport
									/ (float) cp.beforetextSupport);
					dataRow.createCell(9).setCellValue(cp.beforeText);
					dataRow.createCell(10).setCellValue(cp.afterText);

					final CellStyle style = book.createCellStyle();
					style.setWrapText(true);
					style.setFillPattern(CellStyle.SOLID_FOREGROUND);
					style.setFillForegroundColor(foundByFindBugs ? IndexedColors.ROSE
							.getIndex() : IndexedColors.WHITE.getIndex());
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
			}

			book.write(stream);

		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
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
