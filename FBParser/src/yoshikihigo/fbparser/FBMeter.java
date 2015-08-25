package yoshikihigo.fbparser;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class FBMeter {

	public static void main(final String[] args) {

		FBParserConfig.initialize(args);

		final List<String> xmls = FBParserConfig.getInstance().getFBRESULTS();
		final Map<String, List<BugInstance>> bugInstances = new HashMap<String, List<BugInstance>>();
		for (String xml : xmls) {
			final FBParser parser = new FBParser(xml);
			parser.perform();
			final List<BugInstance> bugs = parser.getBugInstances();
			bugInstances.put(xml, bugs);
		}

		final List<Transition> transitions = new ArrayList<Transition>();
		for (int i = 2; i < xmls.size(); i++) {
			final String earlierXMLFile = xmls.get(i - 1);
			final String latterXMLFile = xmls.get(i);

			final Set<BugInstance> earlierBugs = new HashSet<>(
					bugInstances.get(earlierXMLFile));
			final Set<BugInstance> latterBugs = new HashSet<>(
					bugInstances.get(latterXMLFile));

			final Set<BugInstance> survivingBugs = new HashSet<>();
			final Set<BugInstance> removedBugs = new HashSet<>();
			for (final BugInstance bug : earlierBugs) {

				if (latterBugs.contains(bug)) {
					survivingBugs.add(bug);
				} else {
					removedBugs.add(bug);
				}
			}

			final Set<BugInstance> addedBugs = new HashSet<>();
			for (final BugInstance bug : latterBugs) {
				if (!earlierBugs.contains(bug)) {
					addedBugs.add(bug);
				}
			}

			final Transition pair = new Transition(earlierXMLFile,
					latterXMLFile, survivingBugs, addedBugs, removedBugs);
			transitions.add(pair);
		}

		if (FBParserConfig.getInstance().hasSURVIVINGBUGSCSV()) {
			final String csvFile = FBParserConfig.getInstance()
					.getSURVIVINGBUGSCSV();
			printInCSV(csvFile, transitions, "surviving");
		}

		if (FBParserConfig.getInstance().hasREMOVEDBUGSCSV()) {
			final String csvFile = FBParserConfig.getInstance()
					.getREMOVEDBUGSCSV();
			printInCSV(csvFile, transitions, "removed");
		}

		if (FBParserConfig.getInstance().hasADDEDBUGSCSV()) {
			final String csvFile = FBParserConfig.getInstance()
					.getADDEDBUGSCSV();
			printInCSV(csvFile, transitions, "added");
		}

		if (FBParserConfig.getInstance().hasMETRICSRESULTXLSX()) {
			final String xlsxFile = FBParserConfig.getInstance()
					.getMETRICSRESULTXLSX();
			printInXLSX(xlsxFile, transitions);
		}
	}

	static private int countBugInstances(final Set<BugInstance> instances,
			final String type) {
		int count = 0;
		for (final BugInstance instance : instances) {
			if (instance.pattern.type.equals(type)) {
				count++;
			}
		}
		return count;
	}

	static private void printInCSV(final String path,
			final List<Transition> transitions, final String bugText) {

		final SortedMap<BugPattern, StringBuilder> patternsMap = new TreeMap<>();
		for (final BugPattern pattern : BugPattern.getBugPatterns()) {
			final StringBuilder text = new StringBuilder();
			text.append(pattern.type).append(", ")
					.append(pattern.getRankText()).append(", ")
					.append(pattern.getPriorityText()).append(", ")
					.append(pattern.category);
			patternsMap.put(pattern, text);
		}

		for (final Transition transition : transitions) {
			for (final Entry<BugPattern, StringBuilder> entry : patternsMap
					.entrySet()) {
				final BugPattern pattern = entry.getKey();
				final StringBuilder text = entry.getValue();

				final Set<BugInstance> bugs;
				switch (bugText) {
				case "surviving": {
					bugs = transition.survivingBugs;
					break;
				}
				case "removed": {
					bugs = transition.removedBugs;
					break;
				}
				case "added": {
					bugs = transition.addedBugs;
					break;
				}
				default: {
					bugs = new HashSet<>();
				}
				}
				final Set<BugInstance> instances = BugInstance.getBugInstances(
						bugs, pattern);
				text.append(", ").append(Integer.toString(instances.size()));
			}
		}

		try (final PrintWriter writer = new PrintWriter(new OutputStreamWriter(
				new FileOutputStream(path), "UTF-8"))) {

			final StringBuilder title = new StringBuilder();
			title.append("TYPE, RANK, PRIORITY, CATEGORY");
			for (final Transition transition : transitions) {
				title.append(",").append(
						StringUtility.getName(transition.latterXMLFile));
			}
			writer.println(title.toString());

			for (final Entry<BugPattern, StringBuilder> entry : patternsMap
					.entrySet()) {

				final StringBuilder text = entry.getValue();
				writer.println(text.toString());
			}
		}

		catch (final IOException e) {
			e.printStackTrace();
		}
	}

	static private void printInXLSX(final String path,
			final List<Transition> transitions) {

		try (final Workbook book = new XSSFWorkbook();
				final OutputStream stream = new FileOutputStream(path)) {

			final Sheet sheet = book.createSheet();
			book.setSheetName(0, "metrics");

			final CellStyle survivingStyle = book.createCellStyle();
			survivingStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
			survivingStyle
					.setFillForegroundColor(IndexedColors.ROSE.getIndex());

			final CellStyle removingStyle = book.createCellStyle();
			removingStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
			removingStyle.setFillForegroundColor(IndexedColors.AQUA.getIndex());

			final CellStyle nocareStyle = book.createCellStyle();
			nocareStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
			nocareStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT
					.getIndex());

			final Row typeRow = sheet.createRow(0);
			final Row rankRow = sheet.createRow(1);
			final Row priorityRow = sheet.createRow(2);
			final Row categoryRow = sheet.createRow(3);
			final Row metricRow = sheet.createRow(4);

			typeRow.createCell(0).setCellValue("TYPE");
			rankRow.createCell(0).setCellValue("RANK");
			priorityRow.createCell(0).setCellValue("PRIORITY");
			categoryRow.createCell(0).setCellValue("CATETORY");
			metricRow.createCell(0).setCellValue("METRIC");

			int titleColumn = 1;

			for (final BugPattern pattern : BugPattern.getBugPatterns()) {

				typeRow.createCell(titleColumn).setCellValue(pattern.type);
				sheet.addMergedRegion(new CellRangeAddress(0, 0, titleColumn,
						titleColumn + 5));
				rankRow.createCell(titleColumn).setCellValue(
						pattern.getRankText());
				sheet.addMergedRegion(new CellRangeAddress(1, 1, titleColumn,
						titleColumn + 5));
				priorityRow.createCell(titleColumn).setCellValue(
						pattern.getPriorityText());
				sheet.addMergedRegion(new CellRangeAddress(2, 2, titleColumn,
						titleColumn + 5));
				categoryRow.createCell(titleColumn).setCellValue(
						pattern.category);
				sheet.addMergedRegion(new CellRangeAddress(3, 3, titleColumn,
						titleColumn + 5));
				metricRow.createCell(titleColumn).setCellValue(
						"#-surviving-bugs");
				metricRow.createCell(titleColumn + 1).setCellValue(
						"#-removed-bugs");
				metricRow.createCell(titleColumn + 2).setCellValue(
						"#-added-bugs");
				metricRow.createCell(titleColumn + 3).setCellValue(
						"%-surviving-bugs");
				metricRow.createCell(titleColumn + 4).setCellValue(
						"%-solved-old");
				metricRow.createCell(titleColumn + 5).setCellValue(
						"%-solved-new");

				titleColumn += 6;
			}

			int dataRow = 5;
			final Map<String, AtomicInteger> survivingBugs = new HashMap<>();
			final Map<String, AtomicInteger> removedBugs = new HashMap<>();
			final Map<String, AtomicInteger> addedBugs = new HashMap<>();
			for (final Transition pair : transitions) {

				final Row row = sheet.createRow(dataRow);
				row.createCell(0).setCellValue(
						StringUtility.getName(pair.latterXMLFile));

				int dataColumn = 1;
				for (final BugPattern pattern : BugPattern.getBugPatterns()) {

					final int numberOfSurvivingBugs = countBugInstances(
							pair.survivingBugs, pattern.type);
					final int numberOfAddedBugs = countBugInstances(
							pair.addedBugs, pattern.type);
					final int numberOfRemovedBugs = countBugInstances(
							pair.removedBugs, pattern.type);

					float ratioOfSurviving = (float) numberOfSurvivingBugs
							/ (float) (numberOfSurvivingBugs + numberOfRemovedBugs);
					float ratioOfSolvedOld = (float) numberOfRemovedBugs
							/ (float) (numberOfSurvivingBugs + numberOfRemovedBugs);
					float ratioOfSolvednew = (float) numberOfRemovedBugs
							/ (float) (numberOfSurvivingBugs + numberOfAddedBugs);

					row.createCell(dataColumn).setCellValue(
							numberOfSurvivingBugs);
					row.createCell(dataColumn + 1).setCellValue(
							numberOfRemovedBugs);
					row.createCell(dataColumn + 2).setCellValue(
							numberOfAddedBugs);
					row.createCell(dataColumn + 3).setCellValue(
							ratioOfSurviving);
					row.createCell(dataColumn + 4).setCellValue(
							ratioOfSolvedOld);
					row.createCell(dataColumn + 5).setCellValue(
							ratioOfSolvednew);

					{
						final AtomicInteger number = new AtomicInteger(
								numberOfSurvivingBugs);
						survivingBugs.put(pattern.type, number);
					}

					{
						AtomicInteger number = removedBugs.get(pattern.type);
						if (null == number) {
							number = new AtomicInteger(0);
							removedBugs.put(pattern.type, number);
						}
						number.addAndGet(numberOfRemovedBugs);
					}

					{
						AtomicInteger number = addedBugs.get(pattern.type);
						if (null == number) {
							number = new AtomicInteger(0);
							addedBugs.put(pattern.type, number);
						}
						number.addAndGet(numberOfAddedBugs);
					}

					dataColumn += 6;
				}

				dataRow += 1;
			}

			{
				final Row row = sheet.createRow(dataRow);
				row.createCell(0).setCellValue("summary");

				int dataColumn = 1;
				for (final BugPattern pattern : BugPattern.getBugPatterns()) {

					final int numberOfSurvivingBugs = survivingBugs.get(
							pattern.type).get();
					final int numberOfRemovedBugs = removedBugs.get(
							pattern.type).get();
					final int numberOfAddedBugs = addedBugs.get(pattern.type)
							.get();

					float ratioOfSurviving = (float) numberOfSurvivingBugs
							/ (float) (numberOfSurvivingBugs + numberOfRemovedBugs);
					float ratioOfSolvedOld = (float) numberOfRemovedBugs
							/ (float) (numberOfSurvivingBugs + numberOfRemovedBugs);
					float ratioOfSolvednew = (float) numberOfRemovedBugs
							/ (float) (numberOfSurvivingBugs + numberOfAddedBugs);

					row.createCell(dataColumn).setCellValue(
							numberOfSurvivingBugs);
					row.createCell(dataColumn + 1).setCellValue(
							numberOfRemovedBugs);
					row.createCell(dataColumn + 2).setCellValue(
							numberOfAddedBugs);
					row.createCell(dataColumn + 3).setCellValue(
							ratioOfSurviving);
					row.createCell(dataColumn + 4).setCellValue(
							ratioOfSolvedOld);
					row.createCell(dataColumn + 5).setCellValue("No value");

					CellStyle style = null;
					if ((numberOfSurvivingBugs + numberOfRemovedBugs) < 4) {
						style = nocareStyle;
					} else if (ratioOfSurviving <= 0.2d) {
						style = removingStyle;
					} else {
						style = survivingStyle;
					}

					sheet.getRow(0).getCell(dataColumn).setCellStyle(style);
					sheet.getRow(1).getCell(dataColumn).setCellStyle(style);
					sheet.getRow(2).getCell(dataColumn).setCellStyle(style);
					sheet.getRow(3).getCell(dataColumn).setCellStyle(style);
					for (int rowIndex = 4; rowIndex <= dataRow; rowIndex++) {
						for (int columnIndex = dataColumn; columnIndex < (dataColumn + 6); columnIndex++) {
							sheet.getRow(rowIndex).getCell(columnIndex)
									.setCellStyle(style);
						}
					}

					dataColumn += 6;
				}
			}

			book.write(stream);
		}

		catch (final IOException e) {
			e.printStackTrace();
		}
	}

	static class Transition {
		final String earlierXMLFile;
		final String latterXMLFile;
		final Set<BugInstance> survivingBugs;
		final Set<BugInstance> addedBugs;
		final Set<BugInstance> removedBugs;

		Transition(final String earlierXMLFile, final String latterXMLFile,
				final Set<BugInstance> survivingBugs,
				final Set<BugInstance> addedBugs,
				final Set<BugInstance> removedBugs) {
			this.earlierXMLFile = earlierXMLFile;
			this.latterXMLFile = latterXMLFile;
			this.survivingBugs = survivingBugs;
			this.addedBugs = addedBugs;
			this.removedBugs = removedBugs;
		}
	}
}
