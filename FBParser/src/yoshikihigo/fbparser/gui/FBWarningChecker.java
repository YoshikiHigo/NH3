package yoshikihigo.fbparser.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.tmatesoft.svn.core.wc.SVNWCClient;

import yoshikihigo.cpanalyzer.CPAConfig;
import yoshikihigo.cpanalyzer.LANGUAGE;
import yoshikihigo.cpanalyzer.data.Statement;
import yoshikihigo.fbparser.FBParserConfig;
import yoshikihigo.fbparser.XLSXMerger.PATTERN;

public class FBWarningChecker extends JFrame {

	static public void main(final String[] args) {

		FBParserConfig.initialize(args);

		final String xlsx = FBParserConfig.getInstance().getFIXCHANGEPATTERN();

		final Map<String, String> files = new HashMap<>();
		if (FBParserConfig.getInstance().hasREPOSITORY()
				&& FBParserConfig.getInstance().hasREVISION()) {

			final String repository = FBParserConfig.getInstance()
					.getREPOSITORY();
			final int revision = FBParserConfig.getInstance().getREVISION();
			files.putAll(retrieveRevision(repository, revision));
		}

		else if (FBParserConfig.getInstance().hasSOURCE()) {

			final String directory = FBParserConfig.getInstance().getSOURCE();
			files.putAll(retrieveFiles(directory));
		}

		final Map<String, List<Statement>> allStatements = new HashMap<>();
		CPAConfig.initialize(new String[] {});
		for (final Entry<String, String> entry : files.entrySet()) {
			final String path = entry.getKey();
			final String contents = entry.getValue();
			final List<Statement> statements = yoshikihigo.cpanalyzer.StringUtility
					.splitToStatements(contents, LANGUAGE.JAVA);
			allStatements.put(path, statements);
		}

		final List<PATTERN> patterns = readXLSX(xlsx);

		final Map<String, List<Warning>> allWarnings = new HashMap<>();
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
				List<Warning> warnings = allWarnings.get(path);
				if (null == warnings) {
					warnings = new ArrayList<>();
					allWarnings.put(path, warnings);
				}
				for (final int[] code : matchedCodes) {
					final Warning warning = new Warning(code[0], code[1],
							pattern);
					warnings.add(warning);
				}
			}
		}

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final Exception e) {
		}

		CPAConfig.initialize(args);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new FBWarningChecker(files, allWarnings);
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
				patterns.add(pattern);
			}
		}

		catch (final IOException e) {
			e.printStackTrace();
			System.exit(0);
		}

		return patterns;
	}

	static Map<String, String> retrieveRevision(final String repository,
			final int revision) {

		final Map<String, String> files = new HashMap<>();

		try {
			final SVNURL url = SVNURL.fromFile(new File(repository));
			FSRepositoryFactory.setup();
			final SVNLogClient logClient = SVNClientManager.newInstance()
					.getLogClient();
			final SVNWCClient wcClient = SVNClientManager.newInstance()
					.getWCClient();

			final List<String> paths = new ArrayList<>();

			logClient.doList(url, SVNRevision.create(revision),
					SVNRevision.create(revision), true, SVNDepth.INFINITY,
					SVNDirEntry.DIRENT_ALL, new ISVNDirEntryHandler() {

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
				files.put(path, text.toString());
			}

		} catch (final SVNException exception) {
			exception.printStackTrace();
		}

		return files;
	}

	static Map<String, String> retrieveFiles(final String directory) {

		final List<String> paths = retrievePaths(new File(directory));
		final Map<String, String> files = new HashMap<>();

		for (final String path : paths) {
			try {
				final List<String> lines = Files.readAllLines(Paths.get(path),
						StandardCharsets.ISO_8859_1);
				final String text = String.join(System.lineSeparator(), lines);
				files.put(path, text);

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

		return paths;
	}

	final private Map<String, String> files;
	final private Map<String, List<Warning>> warnings;

	public FBWarningChecker(final Map<String, String> files,
			final Map<String, List<Warning>> warnings) {

		super("FBWarningChecker");

		this.files = files;
		this.warnings = warnings;

		final Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(new Dimension(d.width - 10, d.height - 60));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		final FileListView filelist = new FileListView(warnings);
		this.getContentPane().add(filelist.scrollPane, BorderLayout.WEST);

		final SourceCodeWindow sourcecode = new SourceCodeWindow(files,
				warnings);
		this.getContentPane().add(sourcecode.getScrollPane(),
				BorderLayout.CENTER);

		final WarningListView warninglist = new WarningListView(warnings);

		final PatternWindow beforeText = new PatternWindow(
				PatternWindow.TYPE.BEFORE);
		final PatternWindow afterText = new PatternWindow(
				PatternWindow.TYPE.AFTER);
		final JSplitPane patternWindow = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT);
		patternWindow.add(beforeText.scrollPane, JSplitPane.TOP);
		patternWindow.add(afterText.scrollPane, JSplitPane.BOTTOM);
		final JSplitPane rightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		rightPane.add(warninglist.scrollPane, JSplitPane.TOP);
		rightPane.add(patternWindow, JSplitPane.BOTTOM);
		this.getContentPane().add(rightPane, BorderLayout.EAST);

		SelectedEntities.<String> getInstance(SelectedEntities.SELECTED_PATH)
				.addObserver(filelist);
		SelectedEntities.<String> getInstance(SelectedEntities.SELECTED_PATH)
				.addObserver(sourcecode);
		SelectedEntities.<String> getInstance(SelectedEntities.SELECTED_PATH)
				.addObserver(warninglist);
		SelectedEntities.<String> getInstance(SelectedEntities.SELECTED_PATH)
				.addObserver(beforeText);
		SelectedEntities.<String> getInstance(SelectedEntities.SELECTED_PATH)
				.addObserver(afterText);

		SelectedEntities.<Warning> getInstance(
				SelectedEntities.SELECTED_WARNING).addObserver(filelist);
		SelectedEntities.<Warning> getInstance(
				SelectedEntities.SELECTED_WARNING).addObserver(sourcecode);
		SelectedEntities.<Warning> getInstance(
				SelectedEntities.SELECTED_WARNING).addObserver(warninglist);
		SelectedEntities.<Warning> getInstance(
				SelectedEntities.SELECTED_WARNING).addObserver(beforeText);
		SelectedEntities.<Warning> getInstance(
				SelectedEntities.SELECTED_WARNING).addObserver(afterText);

		this.setVisible(true);
	}
}
