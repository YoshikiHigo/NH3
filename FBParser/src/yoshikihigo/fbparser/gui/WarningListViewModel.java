package yoshikihigo.fbparser.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.table.AbstractTableModel;

import yoshikihigo.fbparser.XLSXMerger.PATTERN;

public class WarningListViewModel extends AbstractTableModel {

	static final int COL_LOCATION = 0;
	static final int COL_PATTERNID = 1;

	static final String[] TITLES = new String[] { "LOCATION", "Pattern ID" };

	final private List<int[]> locations;
	final private List<PATTERN> patterns;

	public WarningListViewModel(final Map<int[], PATTERN> warnings) {
		this.locations = new ArrayList<>();
		this.patterns = new ArrayList<>();
		for (final Entry<int[], PATTERN> entry : warnings.entrySet()) {
			this.locations.add(entry.getKey());
			this.patterns.add(entry.getValue());
		}
	}

	@Override
	public int getRowCount() {
		return this.locations.size();
	}

	@Override
	public int getColumnCount() {
		return TITLES.length;
	}

	public Object getValueAt(final int row, final int col) {
		switch (col) {
		case COL_LOCATION:
			final int[] location = this.locations.get(row);
			if (location[0] == location[1]) {
				return Integer.toString(location[0]);
			} else {
				return location[0] + "--" + location[1];
			}
		case COL_PATTERNID:
			final PATTERN pattern = this.patterns.get(row);
			return pattern.mergedID;
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

	public int[] getLocation(final int row){
		return this.locations.get(row);
	}
	
	public PATTERN getPATTERN(final int row) {
		return this.patterns.get(row);
	}
}
