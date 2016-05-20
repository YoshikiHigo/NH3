package yoshikihigo.fbparser.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import yoshikihigo.fbparser.XLSXMerger.PATTERN;

public class WarningListViewModel extends AbstractTableModel {

	static final int COL_ID = 0;
	static final int COL_LOCATION = 1;
	static final int COL_SIZE = 2;
	static final int COL_PASTCHANGES = 3;
	static final int COL_FILES = 4;
	static final int COL_COMMITS = 5;
	static final int COL_AUTHORS = 6;
	static final int COL_LASTDATE = 7;
	static final int COL_PATTERNID = 8;
	static final int COL_MATCHEDCODE = 9;

	static final String[] TITLES = new String[] { "ID", "PLACE", "SIZE",
			"SUPPORT", "FILES", "COMMITS", "AUTHORS", "LASTDATE", "Pattern ID",
			"MATCHED" };

	final List<Warning> warnings;
	final private Map<PATTERN, List<Warning>> pWarnings;

	public WarningListViewModel(final List<Warning> warnings,
			final Map<PATTERN, List<Warning>> pWarnings) {
		this.warnings = new ArrayList<>();
		this.warnings.addAll(warnings);
		this.pWarnings = pWarnings;
	}

	@Override
	public int getRowCount() {
		return this.warnings.size();
	}

	@Override
	public int getColumnCount() {
		return TITLES.length;
	}

	public Object getValueAt(final int row, final int col) {
		final Warning warning = this.warnings.get(row);
		switch (col) {
		case COL_ID: {
			return row;
		}
		case COL_LOCATION: {
			return warning;
		}
		case COL_SIZE: {
			return warning.toLine - warning.fromLine + 1;
		}
		case COL_PASTCHANGES: {
			return warning.pattern.bugfixSupport;
		}
		case COL_FILES: {
			return warning.pattern.getBugfixFiles().size();
		}
		case COL_COMMITS: {
			return warning.pattern.bugfixCommits;
		}
		case COL_AUTHORS: {
			return warning.pattern.getBugfixAuthors().size();
		}
		case COL_LASTDATE: {
			return warning.pattern.getLastDate();
		}
		case COL_PATTERNID: {
			return warning.pattern.mergedID;
		}
		case COL_MATCHEDCODE: {
			return this.pWarnings.get(warning.pattern).size();
		}

		default:
			return null;
		}
	}

	@Override
	public Class<?> getColumnClass(final int col) {
		switch (col) {
		case COL_ID:
			return Integer.class;
		case COL_LOCATION:
			return Warning.class;
		case COL_SIZE:
		case COL_PASTCHANGES:
		case COL_FILES:
		case COL_COMMITS:
		case COL_AUTHORS:
			return Integer.class;
		case COL_LASTDATE:
			return String.class;
		case COL_PATTERNID:
		case COL_MATCHEDCODE:
			return Integer.class;
		default:
			return Object.class;
		}
	}

	@Override
	public String getColumnName(final int col) {
		return TITLES[col];
	}

	public Warning getWarning(final int row) {
		return this.warnings.get(row);
	}
}
