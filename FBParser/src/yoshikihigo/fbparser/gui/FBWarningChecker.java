package yoshikihigo.fbparser.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

import yoshikihigo.cpanalyzer.CPAConfig;
import yoshikihigo.cpanalyzer.LANGUAGE;
import yoshikihigo.cpanalyzer.data.Statement;
import yoshikihigo.fbparser.FBChangePatternFinderWithoutFB;
import yoshikihigo.fbparser.FBParserConfig;
import yoshikihigo.fbparser.StringUtility;
import yoshikihigo.fbparser.XLSXMerger.PATTERN;
import yoshikihigo.fbparser.db.DAO;
import yoshikihigo.fbparser.db.DAO.PATTERN_SQL;

public class FBWarningChecker extends JFrame {

	static public void main(final String[] args) {

		FBParserConfig.initialize(args);

		if (!FBParserConfig.getInstance().hasLANGUAGE()) {
			System.out
					.println("\"-lang\" option is required to specify target languages");
			System.exit(0);
		}

		System.out.println("Useful functions:");
		System.out
				.println(" double-clicking LEFT button on WARNINGLIST -> focusing the specified pattern");
		System.out
				.println(" double-clicking RIGHT button on WARNINGLIST -> marking the specified pattern as trivial");
		System.out
				.println(" double-clicking LEFT button on the header of PASTCHANGES -> showing up commit information");
		System.out.println();

		final SortedMap<String, String> files = new TreeMap<>();
		if (FBParserConfig.getInstance().hasSVNREPOSITORY()
				&& FBParserConfig.getInstance().hasSVNREVISION()) {

			final String repository = FBParserConfig.getInstance()
					.getSVNREPOSITORY();
			final int revision = FBParserConfig.getInstance().getSVNREVISION();
			// files.putAll(retrieveRevision(repository, revision));
			files.putAll(retrieveSVNFiles(repository, revision));
		}

		else if (FBParserConfig.getInstance().hasGITREPOSITORY()
				&& FBParserConfig.getInstance().hasGITCOMMIT()) {

			final String gitrepo = FBParserConfig.getInstance()
					.getGITREPOSITORY();
			final String gitcommit = FBParserConfig.getInstance()
					.getGITCOMMIT();
			files.putAll(retrieveGITFiles(gitrepo, gitcommit));
		}

		else if (FBParserConfig.getInstance().hasSOURCE()) {

			final String directory = FBParserConfig.getInstance().getSOURCE();
			files.putAll(retrieveLocalFiles(directory));
		}

		final Set<LANGUAGE> languages = FBParserConfig.getInstance()
				.getLANGUAGE();
		final SortedMap<String, List<Statement>> allStatements = new TreeMap<>();
		CPAConfig.initialize(new String[] {});
		for (final Entry<String, String> entry : files.entrySet()) {
			final String path = entry.getKey();
			final String contents = entry.getValue();
			for (final LANGUAGE lang : languages) {
				if (lang.isTarget(path)) {
					final List<Statement> statements = yoshikihigo.cpanalyzer.StringUtility
							.splitToStatements(contents, lang);
					allStatements.put(path, statements);
					break;
				}
			}
		}

		List<PATTERN> patterns = null;
		if (FBParserConfig.getInstance().hasFIXCHANGEPATTERN()) {
			final String xlsx = FBParserConfig.getInstance()
					.getFIXCHANGEPATTERN();
			patterns = readXLSX(xlsx);
		} else {
			patterns = getPatternsFromDB();
		}

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

		if (pattern.isEmpty()) {
			return Collections.emptyList();
		}

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

	static List<PATTERN> getPatternsFromDB() {

		final List<PATTERN> patterns = new ArrayList<>();
		final List<PATTERN_SQL> patternsSQL = DAO.getInstance()
				.getFixChangePatterns();

		for (final PATTERN_SQL patternSQL : patternsSQL) {

			if (patternSQL.confidence < 1) {
				continue;
			}

			if (patternSQL.support < 2) {
				continue;
			}

			final String beforeText = patternSQL.beforeNText;
			final String afterText = patternSQL.afterNText;

			if (beforeText.isEmpty() || afterText.isEmpty()) {
				continue;
			}

			final PATTERN pattern = new PATTERN(beforeText, afterText);
			pattern.mergedID = patternSQL.id;
			pattern.support = patternSQL.support;
			pattern.bugfixSupport = patternSQL.support;
			pattern.beforeTextSupport = 0;
			pattern.addDate(patternSQL.firstdate);
			pattern.addDate(patternSQL.lastdate);
			pattern.bugfixCommits = FBChangePatternFinderWithoutFB.getCommits(
					patternSQL, true);
			pattern.addBugfixAuthors(FBChangePatternFinderWithoutFB.getAuthors(
					patternSQL, true));
			pattern.addBugfixFiles(FBChangePatternFinderWithoutFB.getFiles(
					patternSQL, true));
			patterns.add(pattern);
		}

		return patterns;
	}

	static SortedMap<String, String> retrieveSVNFiles(final String repository,
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

		return retrieveLocalFiles(tmpDir.toFile().getAbsolutePath());
	}

	static SortedMap<String, String> retrieveLocalFiles(final String directory) {

		final Set<LANGUAGE> languages = FBParserConfig.getInstance()
				.getLANGUAGE();
		final List<String> paths = retrievePaths(new File(directory), languages);
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

	static List<String> retrievePaths(final File directory,
			final Set<LANGUAGE> languages) {

		final List<String> paths = new ArrayList<>();

		if (directory.isFile()) {
			for (final LANGUAGE lang : languages) {
				if (lang.isTarget(directory.getName())) {
					paths.add(directory.getAbsolutePath());
					break;
				}
			}
		}

		else if (directory.isDirectory()) {
			for (final File child : directory.listFiles()) {
				paths.addAll(retrievePaths(child, languages));
			}
		}

		Collections.sort(paths);

		return paths;
	}

	static SortedMap<String, String> retrieveGITFiles(final String repository,
			final String revision) {

		final SortedMap<String, String> fileMap = new TreeMap<>();

		final String gitrepo = FBParserConfig.getInstance().getGITREPOSITORY();
		final Set<LANGUAGE> languages = FBParserConfig.getInstance()
				.getLANGUAGE();

		try (final FileRepository repo = new FileRepository(gitrepo + "/.git");
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
						break;
					}
				}
			}

			for (final String file : files) {
				final TreeWalk nodeWalk = TreeWalk.forPath(reader, file, tree);
				final byte[] data = reader.open(nodeWalk.getObjectId(0))
						.getBytes();
				final String text = new String(data, "utf-8");
				fileMap.put(file, text);
			}

		} catch (final IOException e) {
			e.printStackTrace();
		}

		return fileMap;
	}

	public FBWarningChecker(final Map<String, String> files,
			final Map<String, List<Warning>> fWarnings,
			final Map<PATTERN, List<Warning>> pWarnings) {

		super("FBWarningChecker");

		final Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(new Dimension(d.width - 10, d.height - 60));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.getContentPane().setLayout(new BorderLayout());
		final PatternFilteringPanel patternFilteringPanel = new PatternFilteringPanel(
				fWarnings, pWarnings);
		this.getContentPane().add(patternFilteringPanel, BorderLayout.NORTH);
		final JSplitPane mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		this.getContentPane().add(mainPanel, BorderLayout.CENTER);
		final JSplitPane leftPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainPanel.setLeftComponent(leftPane);
		final JSplitPane rightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainPanel.setRightComponent(rightPane);
		final JPanel fileListPanel = new JPanel(new BorderLayout());
		leftPane.setTopComponent(fileListPanel);
		fileListPanel.setBorder(new TitledBorder(new LineBorder(Color.black),
				"FILE LIST"));

		final FilePathKeywordField pathKeywordField = new FilePathKeywordField(
				fWarnings);
		fileListPanel.add(pathKeywordField, BorderLayout.NORTH);
		final FileListView filelist = new FileListView(fWarnings);
		fileListPanel.add(filelist.scrollPane, BorderLayout.CENTER);

		final TargetSourceCodeWindow sourcecode = new TargetSourceCodeWindow(
				files, fWarnings);
		leftPane.add(sourcecode.getScrollPane(), JSplitPane.BOTTOM);

		final WarningListView warninglist = new WarningListView(fWarnings,
				pWarnings);
		rightPane.add(warninglist.scrollPane, JSplitPane.TOP);

		final PastChangesView patternWindow = new PastChangesView();
		rightPane.add(patternWindow, JSplitPane.BOTTOM);

		SelectedEntities.getInstance(SelectedEntities.SELECTED_PATH)
				.addObserver(filelist);
		SelectedEntities.getInstance(SelectedEntities.SELECTED_PATH)
				.addObserver(sourcecode);
		SelectedEntities.getInstance(SelectedEntities.SELECTED_PATH)
				.addObserver(warninglist);
		SelectedEntities.getInstance(SelectedEntities.SELECTED_PATH)
				.addObserver(patternWindow);
		SelectedEntities.getInstance(SelectedEntities.SELECTED_PATH)
				.addObserver(patternFilteringPanel);
		SelectedEntities.getInstance(SelectedEntities.SELECTED_PATH)
				.addObserver(pathKeywordField);

		SelectedEntities.getInstance(SelectedEntities.SELECTED_WARNING)
				.addObserver(filelist);
		SelectedEntities.getInstance(SelectedEntities.SELECTED_WARNING)
				.addObserver(sourcecode);
		SelectedEntities.getInstance(SelectedEntities.SELECTED_WARNING)
				.addObserver(warninglist);
		SelectedEntities.getInstance(SelectedEntities.SELECTED_WARNING)
				.addObserver(patternWindow);
		SelectedEntities.getInstance(SelectedEntities.SELECTED_WARNING)
				.addObserver(patternFilteringPanel);
		SelectedEntities.getInstance(SelectedEntities.SELECTED_WARNING)
				.addObserver(pathKeywordField);

		SelectedEntities.getInstance(SelectedEntities.TRIVIAL_PATTERN)
				.addObserver(filelist);
		SelectedEntities.getInstance(SelectedEntities.TRIVIAL_PATTERN)
				.addObserver(sourcecode);
		SelectedEntities.getInstance(SelectedEntities.TRIVIAL_PATTERN)
				.addObserver(warninglist);
		SelectedEntities.getInstance(SelectedEntities.TRIVIAL_PATTERN)
				.addObserver(patternWindow);
		SelectedEntities.getInstance(SelectedEntities.TRIVIAL_PATTERN)
				.addObserver(patternFilteringPanel);
		SelectedEntities.getInstance(SelectedEntities.TRIVIAL_PATTERN)
				.addObserver(pathKeywordField);

		SelectedEntities.getInstance(SelectedEntities.FOCUSING_PATTERN)
				.addObserver(filelist);
		SelectedEntities.getInstance(SelectedEntities.FOCUSING_PATTERN)
				.addObserver(sourcecode);
		SelectedEntities.getInstance(SelectedEntities.FOCUSING_PATTERN)
				.addObserver(warninglist);
		SelectedEntities.getInstance(SelectedEntities.FOCUSING_PATTERN)
				.addObserver(patternWindow);
		SelectedEntities.getInstance(SelectedEntities.FOCUSING_PATTERN)
				.addObserver(patternFilteringPanel);
		SelectedEntities.getInstance(SelectedEntities.FOCUSING_PATTERN)
				.addObserver(pathKeywordField);

		SelectedEntities.getInstance(SelectedEntities.LOGKEYWORD_PATTERN)
				.addObserver(filelist);
		SelectedEntities.getInstance(SelectedEntities.LOGKEYWORD_PATTERN)
				.addObserver(sourcecode);
		SelectedEntities.getInstance(SelectedEntities.LOGKEYWORD_PATTERN)
				.addObserver(warninglist);
		SelectedEntities.getInstance(SelectedEntities.LOGKEYWORD_PATTERN)
				.addObserver(patternWindow);
		SelectedEntities.getInstance(SelectedEntities.LOGKEYWORD_PATTERN)
				.addObserver(patternFilteringPanel);
		SelectedEntities.getInstance(SelectedEntities.LOGKEYWORD_PATTERN)
				.addObserver(pathKeywordField);

		SelectedEntities.getInstance(SelectedEntities.METRICS_PATTERN)
				.addObserver(filelist);
		SelectedEntities.getInstance(SelectedEntities.METRICS_PATTERN)
				.addObserver(sourcecode);
		SelectedEntities.getInstance(SelectedEntities.METRICS_PATTERN)
				.addObserver(warninglist);
		SelectedEntities.getInstance(SelectedEntities.METRICS_PATTERN)
				.addObserver(patternWindow);
		SelectedEntities.getInstance(SelectedEntities.METRICS_PATTERN)
				.addObserver(patternFilteringPanel);
		SelectedEntities.getInstance(SelectedEntities.METRICS_PATTERN)
				.addObserver(pathKeywordField);

		SelectedEntities.getInstance(SelectedEntities.PATHKEYWORD_PATTERN)
				.addObserver(filelist);
		SelectedEntities.getInstance(SelectedEntities.PATHKEYWORD_PATTERN)
				.addObserver(sourcecode);
		SelectedEntities.getInstance(SelectedEntities.PATHKEYWORD_PATTERN)
				.addObserver(warninglist);
		SelectedEntities.getInstance(SelectedEntities.PATHKEYWORD_PATTERN)
				.addObserver(patternWindow);
		SelectedEntities.getInstance(SelectedEntities.PATHKEYWORD_PATTERN)
				.addObserver(patternFilteringPanel);
		SelectedEntities.getInstance(SelectedEntities.PATHKEYWORD_PATTERN)
				.addObserver(pathKeywordField);

		this.setVisible(true);
		mainPanel.setDividerLocation(mainPanel.getWidth() / 2);
	}
}
