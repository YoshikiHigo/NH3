package yoshikihigo.fbparser.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class WarningListViewModel extends AbstractTableModel {

	static final int COL_LOCATION = 0;
	static final int COL_PATTERNID = 1;

	static final String[] TITLES = new String[] { "LOCATION", "Pattern ID" };

	final private List<Warning> warnings;

	public WarningListViewModel(final List<Warning> warnings) {
		this.warnings = new ArrayList<>();
		this.warnings.addAll(warnings);
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
		case COL_LOCATION:
			if (warning.fromLine == warning.toLine) {
				return Integer.toString(warning.fromLine);
			} else {
				return warning.fromLine + "--" + warning.toLine;
			}
		case COL_PATTERNID:
			return warning.pattern.mergedID;
		default:
			return null;
		}
	}

	@Override
	public Class<?> getColumnClass(final int col) {
		switch (col) {
		case COL_LOCATION:
			return String.class;
		case COL_PATTERNID:
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
