package yoshikihigo.fbparser.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.table.AbstractTableModel;

import yoshikihigo.fbparser.XLSXMerger.PATTERN;

public class FileListViewModel extends AbstractTableModel {

	static final int COL_PATH = 0;
	static final int COL_WARNINGS = 1;

	static final String[] TITLES = new String[] { "PATH", "WARNINGS" };

	final private List<String> paths;
	final private List<Map<int[], PATTERN>> warnings;

	public FileListViewModel(final Map<String, Map<int[], PATTERN>> files) {
		this.paths = new ArrayList<>();
		this.warnings = new ArrayList<>();
		for (final Entry<String, Map<int[], PATTERN>> file : files.entrySet()) {
			final String path = file.getKey();
			final Map<int[], PATTERN> warning = file.getValue();
			this.paths.add(path);
			this.warnings.add(warning);
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
		case COL_PATH:
			return this.paths.get(row);
		case COL_WARNINGS:
			return this.warnings.get(row).size();
		default:
			return null;
		}
	}

	@Override
	public Class<?> getColumnClass(final int col) {
		switch (col) {
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

	public Map<int[], PATTERN> getWarnings(final int row) {
		return this.warnings.get(row);
	}
}
