package yoshikihigo.fbparser.gui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import yoshikihigo.cpanalyzer.CPAConfig;
import yoshikihigo.cpanalyzer.LANGUAGE;
import yoshikihigo.cpanalyzer.data.Statement;
import yoshikihigo.fbparser.FBParserConfig;
import yoshikihigo.fbparser.StringUtility;
import yoshikihigo.fbparser.XLSXMerger.PATTERN;

public class FBWarningChecker extends JFrame {

	static public void main(final String[] args) {

		FBParserConfig.initialize(args);

		final String xlsx = FBParserConfig.getInstance().getFIXCHANGEPATTERN();

		System.out.println("Useful functions:");
		System.out
				.println(" double-clicking LEFT button on WARNINGLIST -> focusing the specified pattern");
		System.out
				.println(" double-clicking RIGHT button on WARNINGLIST -> marking the specified pattern as trivial");
		System.out.println();

		final SortedMap<String, String> files = new TreeMap<>();
		if (FBParserConfig.getInstance().hasREPOSITORY()
				&& FBParserConfig.getInstance().hasREVISION()) {

			final String repository = FBParserConfig.getInstance()
					.getREPOSITORY();
			final int revision = FBParserConfig.getInstance().getREVISION();
			// files.putAll(retrieveRevision(repository, revision));
			files.putAll(retrieveRevision2(repository, revision));
		}

		else if (FBParserConfig.getInstance().hasSOURCE()) {

			final String directory = FBParserConfig.getInstance().getSOURCE();
			files.putAll(retrieveFiles(directory));
		}

		final SortedMap<String, List<Statement>> allStatements = new TreeMap<>();
		CPAConfig.initialize(new String[] {});
		for (final Entry<String, String> entry : files.entrySet()) {
			final String path = entry.getKey();
			final String contents = entry.getValue();
			final List<Statement> statements = yoshikihigo.cpanalyzer.StringUtility
					.splitToStatements(contents, LANGUAGE.JAVA);
			allStatements.put(path, statements);
		}

		final List<PATTERN> patterns = readXLSX(xlsx);

		final SortedMap<String, List<Warning>> fWarnings = new TreeMap<>();
		final SortedMap<PATTERN, List<Warning>> pWarnings = new TreeMap<>();
		for (final Entry<String, List<Statement>> file : allStatements
				.entrySet()) {
			final String path = file.getKey();
			final List<Statement> statements = file.getValue();
			for (final PATTERN pattern : patterns) {

				if (100 <= pattern.beforeTextSupport) {
					continue;
				}

				final List<int[]> matchedCodes = findMatchedCode(statements,
						pattern.beforeTextHashs);
				if (matchedCodes.isEmpty()) {
					continue;
				}

				final List<Warning> warnings = new ArrayList<>();
				for (final int[] code : matchedCodes) {
					final Warning warning = new Warning(code[0], code[1],
							pattern);
					warnings.add(warning);
				}

				List<Warning> w1 = fWarnings.get(path);
				if (null == w1) {
					w1 = new ArrayList<>();
					fWarnings.put(path, w1);
				}
				w1.addAll(warnings);

				List<Warning> w2 = pWarnings.get(pattern);
				if (null == w2) {
					w2 = new ArrayList<>();
					pWarnings.put(pattern, w2);
				}
				w2.addAll(warnings);
			}
		}

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final Exception e) {
		}

		CPAConfig.initialize(args);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new FBWarningChecker(files, fWarnings, pWarnings);
			}
		});
	}

	static private List<int[]> findMatchedCode(
			final List<Statement> statements, final List<byte[]> pattern) {

		int pIndex = 0;
		final List<int[]> places = new ArrayList<>();
		final List<Statement> code = new ArrayList<>();
		for (int index = 0; index < statements.size(); index++) {

			if (Arrays.equals(statements.get(index).hash, pattern.get(pIndex))) {
				pIndex++;
				code.add(statements.get(index));
				if (pIndex == pattern.size()) {
					final int fromLine = code.get(0).fromLine;
					final int toLine = code.get(code.size() - 1).toLine;
					final int[] range = new int[] { fromLine, toLine };
					places.add(range);
					code.clear();
					pIndex = 0;
				}
			}

			else {
				pIndex = 0;
				code.clear();
			}
		}

		return places;
	}

	static List<PATTERN> readXLSX(final String xlsx) {

		final List<PATTERN> patterns = new ArrayList<>();
		try (final Workbook book = new XSSFWorkbook(new FileInputStream(xlsx))) {
			final Sheet sheet = book.getSheetAt(0);

			final int lastRowNumber = sheet.getLastRowNum();
			for (int rowNumber = 1; rowNumber < lastRowNumber; rowNumber++) {
				final Row row = sheet.getRow(rowNumber);
				final String beforeText = row.getCell(23).getStringCellValue();
				final String afterText = row.getCell(24).getStringCellValue();
				final PATTERN pattern = new PATTERN(beforeText, afterText);
				pattern.mergedID = (int) row.getCell(0).getNumericCellValue();
				pattern.support = (int) row.getCell(7).getNumericCellValue();
				pattern.bugfixSupport = (int) row.getCell(8)
						.getNumericCellValue();
				pattern.beforeTextSupport = (int) row.getCell(9)
						.getNumericCellValue();
				pattern.bugfixCommits = (int) row.getCell(14)
						.getNumericCellValue();
				pattern.addDate(row.getCell(15).getStringCellValue());
				pattern.addDate(row.getCell(16).getStringCellValue());
				pattern.addBugfixAuthors(row.getCell(26).getStringCellValue());
				pattern.addBugfixFiles(row.getCell(28).getStringCellValue());
				patterns.add(pattern);
			}
		}

		catch (final IOException e) {
			e.printStackTrace();
			System.exit(0);
		}

		return patterns;
	}

	static SortedMap<String, String> retrieveRevision(final String repository,
			final int revision) {

		final SortedMap<String, String> files = new TreeMap<>();

		try {
			final SVNURL repourl = StringUtility.getSVNURL(repository, "");
			FSRepositoryFactory.setup();
			final SVNLogClient logClient = SVNClientManager.newInstance()
					.getLogClient();
			final SVNWCClient wcClient = SVNClientManager.newInstance()
					.getWCClient();

			final List<String> paths = new ArrayList<>();

			logClient.doList(repourl, SVNRevision.create(revision),
					SVNRevision.create(revision), true, SVNDepth.INFINITY,
					SVNDirEntry.DIRENT_KIND, new ISVNDirEntryHandler() {

						@Override
						public void handleDirEntry(final SVNDirEntry entry)
								throws SVNException {

							if (entry.getKind() == SVNNodeKind.FILE) {
								final String path = entry.getRelativePath();
								if (path.endsWith(".java")) {
									paths.add(path);
								}
							}
						}
					});

			for (final String path : paths) {
				final SVNURL fileurl = StringUtility
						.getSVNURL(repository, path);
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
				files.put(path, text.toString());
			}

		} catch (final SVNException exception) {
			exception.printStackTrace();
		}

		return files;
	}

	static SortedMap<String, String> retrieveRevision2(final String repository,
			final int revision) {

		Path tmpDir = null;
		try {
			tmpDir = Files.createTempDirectory("FBWarningChecker");
			tmpDir.toFile().deleteOnExit();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(0);
		}

		try {
			System.out.print("retrieving the specified revision ...");
			final SVNURL url = StringUtility.getSVNURL(repository, "");
			FSRepositoryFactory.setup();
			final SVNUpdateClient client = SVNClientManager.newInstance()
					.getUpdateClient();
			client.doExport(url, tmpDir.toFile(), SVNRevision.create(revision),
					SVNRevision.create(revision), System.lineSeparator(), true,
					SVNDepth.INFINITY);
			System.out.println(" done.");

		} catch (final SVNException e) {
			e.printStackTrace();
			System.exit(0);
		}

		return retrieveFiles(tmpDir.toFile().getAbsolutePath());
	}

	static SortedMap<String, String> retrieveFiles(final String directory) {

		final List<String> paths = retrievePaths(new File(directory));
		final SortedMap<String, String> files = new TreeMap<>();

		for (final String path : paths) {
			try {
				final List<String> lines = Files.readAllLines(Paths.get(path),
						StandardCharsets.ISO_8859_1);
				final String text = String.join(System.lineSeparator(), lines);
				files.put(path.substring(directory.length() + 1), text);

			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		return files;
	}

	static List<String> retrievePaths(final File directory) {

		final List<String> paths = new ArrayList<>();

		if (directory.isFile()) {
			if (directory.getName().endsWith(".java")) {
				paths.add(directory.getAbsolutePath());
			}
		}

		else if (directory.isDirectory()) {
			for (final File child : directory.listFiles()) {
				paths.addAll(retrievePaths(child));
			}
		}

		Collections.sort(paths);

		return paths;
	}

	final private Map<String, String> files;
	final private Map<String, List<Warning>> fWarnings;
	final private Map<PATTERN, List<Warning>> pWarnings;

	public FBWarningChecker(final Map<String, String> files,
			final Map<String, List<Warning>> fWarnings,
			final Map<PATTERN, List<Warning>> pWarnings) {

		super("FBWarningChecker");

		this.files = files;
		this.fWarnings = fWarnings;
		this.pWarnings = pWarnings;

		final Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(new Dimension(d.width - 10, d.height - 60));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.getContentPane().setLayout(new GridLayout(1, 2));
		final JSplitPane leftPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		this.getContentPane().add(leftPane);
		final JSplitPane rightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		this.getContentPane().add(rightPane);

		final FileListView filelist = new FileListView(fWarnings);
		leftPane.add(filelist.scrollPane, JSplitPane.TOP);

		final TargetSourceCodeWindow sourcecode = new TargetSourceCodeWindow(
				files, fWarnings);
		leftPane.add(sourcecode.getScrollPane(), JSplitPane.BOTTOM);

		final WarningListView warninglist = new WarningListView(fWarnings,
				pWarnings);
		rightPane.add(warninglist.scrollPane, JSplitPane.TOP);

		final PastChangesView patternWindow = new PastChangesView();
		rightPane.add(patternWindow, JSplitPane.BOTTOM);

		SelectedEntities.<String> getInstance(SelectedEntities.SELECTED_PATH)
				.addObserver(filelist);
		SelectedEntities.<String> getInstance(SelectedEntities.SELECTED_PATH)
				.addObserver(sourcecode);
		SelectedEntities.<String> getInstance(SelectedEntities.SELECTED_PATH)
				.addObserver(warninglist);
		SelectedEntities.<String> getInstance(SelectedEntities.SELECTED_PATH)
				.addObserver(patternWindow);

		SelectedEntities.<Warning> getInstance(
				SelectedEntities.SELECTED_WARNING).addObserver(filelist);
		SelectedEntities.<Warning> getInstance(
				SelectedEntities.SELECTED_WARNING).addObserver(sourcecode);
		SelectedEntities.<Warning> getInstance(
				SelectedEntities.SELECTED_WARNING).addObserver(warninglist);
		SelectedEntities.<Warning> getInstance(
				SelectedEntities.SELECTED_WARNING).addObserver(patternWindow);

		SelectedEntities
				.<Warning> getInstance(SelectedEntities.TRIVIAL_PATTERN)
				.addObserver(filelist);
		SelectedEntities
				.<Warning> getInstance(SelectedEntities.TRIVIAL_PATTERN)
				.addObserver(sourcecode);
		SelectedEntities
				.<Warning> getInstance(SelectedEntities.TRIVIAL_PATTERN)
				.addObserver(warninglist);
		SelectedEntities
				.<Warning> getInstance(SelectedEntities.TRIVIAL_PATTERN)
				.addObserver(patternWindow);

		SelectedEntities.<Warning> getInstance(
				SelectedEntities.FOCUSING_PATTERN).addObserver(filelist);
		SelectedEntities.<Warning> getInstance(
				SelectedEntities.FOCUSING_PATTERN).addObserver(sourcecode);
		SelectedEntities.<Warning> getInstance(
				SelectedEntities.FOCUSING_PATTERN).addObserver(warninglist);
		SelectedEntities.<Warning> getInstance(
				SelectedEntities.FOCUSING_PATTERN).addObserver(patternWindow);

		this.setVisible(true);
	}
}
