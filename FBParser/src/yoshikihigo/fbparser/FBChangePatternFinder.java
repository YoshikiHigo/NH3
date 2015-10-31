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
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
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
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
				setCellComment(titleRow.getCell(5), "Higo",
						"the number of occurences of a given pattern", 3, 1);
				titleRow.createCell(6).setCellValue("BUG-FIX-SUPPORT");
				setCellComment(
						titleRow.getCell(6),
						"Higo",
						"the number of occurences of a given pattern in bug-fix commits",
						3, 2);
				titleRow.createCell(7).setCellValue("BEFORE-TEXT-SUPPORT");
				setCellComment(
						titleRow.getCell(7),
						"Higo",
						"the number of code fragments whose texts are "
								+ "identical to before-text of a given pattern "
								+ "in the commit where the pattern appears initially",
						4, 2);
				titleRow.createCell(8).setCellValue("CONFIDENCE1");
				setCellComment(titleRow.getCell(8), "Higo",
						"BUG-FIX-SUPPORT / SUPPORT", 3, 1);
				titleRow.createCell(9).setCellValue("CONFIDENCE2");
				setCellComment(titleRow.getCell(9), "Higo",
						"SUPPORT / BEFORE-TEXT-SUPPORT", 3, 1);
				titleRow.createCell(10).setCellValue("CONFIDENCE3");
				setCellComment(titleRow.getCell(10), "Higo",
						"BUG-FIX-SUPPORT / BEFORE-TEXT-SUPPORT", 3, 1);
				titleRow.createCell(11).setCellValue("FIRST-DATE");
				titleRow.createCell(12).setCellValue("LAST-DATE");
				titleRow.createCell(13).setCellValue("DATE-DIFFERENCE");
				titleRow.createCell(14).setCellValue("OCCUPANCY");
				setCellComment(
						titleRow.getCell(14),
						"Higo",
						"average of (LOC of a given pattern changed in revision R) / "
								+ "(total LOC changed in revision R) "
								+ "for all the revisions where the pattern appears",
						4, 2);
				titleRow.createCell(15).setCellValue("TEXT-BEFORE-CHANGE");
				titleRow.createCell(16).setCellValue("TEXT-AFTER-CHANGE");

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
					dataRow.createCell(5).setCellValue(cp.support);
					dataRow.createCell(6).setCellValue(cp.bugfixSupport);
					dataRow.createCell(7).setCellValue(cp.beforetextSupport);
					dataRow.createCell(8).setCellValue(
							(float) cp.bugfixSupport / (float) cp.support);
					dataRow.createCell(9).setCellValue(
							(float) cp.support / (float) cp.beforetextSupport);
					dataRow.createCell(10).setCellValue(
							(float) cp.bugfixSupport
									/ (float) cp.beforetextSupport);
					dataRow.createCell(11).setCellValue(cp.firstdate);
					dataRow.createCell(12).setCellValue(cp.lastdate);
					dataRow.createCell(13).setCellValue(
							getDayDifference(cp.firstdate, cp.lastdate));
					dataRow.createCell(14).setCellValue(getOccupancy(cp));
					dataRow.createCell(15).setCellValue(cp.beforeText);
					dataRow.createCell(16).setCellValue(cp.afterText);

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
					dataRow.getCell(11).setCellStyle(style);
					dataRow.getCell(12).setCellStyle(style);
					dataRow.getCell(13).setCellStyle(style);
					dataRow.getCell(14).setCellStyle(style);
					dataRow.getCell(15).setCellStyle(style);
					dataRow.getCell(16).setCellStyle(style);

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
				sheet.setColumnWidth(15, 20480);
				sheet.setColumnWidth(16, 20480);
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

	private static float getOccupancy(final CHANGEPATTERN_SQL cp) {

		final byte[] beforeHash = cp.beforeHash;
		final byte[] afterHash = cp.afterHash;
		final List<CHANGE_SQL> changesInPattern = DAO.getInstance().getChanges(
				beforeHash, afterHash);
		final Map<REVISION_SQL, AtomicInteger> map1 = new HashMap<>();
		final Map<REVISION_SQL, AtomicInteger> map2 = new HashMap<>();

		final List<REVISION_SQL> revisions = DAO.getInstance().getRevisions(
				beforeHash, afterHash);
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
