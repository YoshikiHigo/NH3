package yoshikihigo.fbparser.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.table.AbstractTableModel;

public class FileListViewModel extends AbstractTableModel {

	static final int COL_ID = 0;
	static final int COL_PATH = 1;
	static final int COL_WARNINGS = 2;

	static final String[] TITLES = new String[] { "ID", "PATH", "WARNINGS" };

	final private List<String> paths;
	final private List<List<Warning>> fWarnings;

	public FileListViewModel(final Map<String, List<Warning>> fWarnings) {
		this.paths = new ArrayList<>();
		this.fWarnings = new ArrayList<>();
		for (final Entry<String, List<Warning>> entry : fWarnings.entrySet()) {
			final String path = entry.getKey();
			final List<Warning> warnings = entry.getValue();
			this.paths.add(path);
			this.fWarnings.add(warnings);
		}
	}

	@Override
	public int getRowCount() {
		return this.paths.size();
	}

	@Override
	public int getColumnCount() {
		return TITLES.length;
	}

	public Object getValueAt(final int row, final int col) {
		switch (col) {
		case COL_ID:
			return row;
		case COL_PATH:
			return this.paths.get(row);
		case COL_WARNINGS: {
			final SelectedEntities<Integer> focusingPatterns = SelectedEntities
					.getInstance(SelectedEntities.FOCUSING_PATTERN);
			if (focusingPatterns.isSet()) {
				return (int) this.fWarnings
						.get(row)
						.stream()
						.filter(warning -> focusingPatterns
								.contains(warning.pattern.mergedID)).count();
			} else {
				return this.fWarnings.get(row).size();
			}
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
		case COL_PATH:
			return String.class;
		case COL_WARNINGS:
			return Integer.class;
		default:
			return Object.class;
		}
	}

	@Override
	public String getColumnName(final int col) {
		return TITLES[col];
	}

	public String getPath(final int row) {
		return this.paths.get(row);
	}

	public List<Warning> getWarnings(final int row) {
		return this.fWarnings.get(row);
	}
}
