package yoshikihigo.fbparser.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import yoshikihigo.fbparser.XLSXMerger.PATTERN;

public class WarningListView extends JTable implements Observer {

	class SelectionHandler implements ListSelectionListener {

		@Override
		public void valueChanged(final ListSelectionEvent e) {

			if (e.getValueIsAdjusting()) {
				return;
			}

			WarningListView.this.setCursor(Cursor
					.getPredefinedCursor(Cursor.WAIT_CURSOR));

			final int firstIndex = e.getFirstIndex();
			final int lastIndex = e.getLastIndex();
			for (int i = firstIndex; i <= lastIndex; i++) {
				final int modelIndex = WarningListView.this
						.convertRowIndexToModel(i);
				final WarningListViewModel model = (WarningListViewModel) WarningListView.this
						.getModel();
				final Warning warning = model.getWarning(modelIndex);
				if (WarningListView.this.getSelectionModel().isSelectedIndex(i)) {
					SelectedEntities.<Warning> getInstance(
							SelectedEntities.SELECTED_WARNING).add(warning,
							WarningListView.this);
				} else {
					SelectedEntities.<Warning> getInstance(
							SelectedEntities.SELECTED_WARNING).remove(warning,
							WarningListView.this);
				}
			}

			WarningListView.this.setCursor(Cursor
					.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	final private SelectionHandler selectionHandler;
	final private Map<String, List<Warning>> fWarnings;
	final private Map<PATTERN, List<Warning>> pWarnings;
	final public JScrollPane scrollPane;

	public WarningListView(final Map<String, List<Warning>> fWarnings,
			final Map<PATTERN, List<Warning>> pWarnings) {

		super();

		this.fWarnings = fWarnings;
		this.pWarnings = pWarnings;
		this.scrollPane = new JScrollPane();
		this.scrollPane.setViewportView(this);
		this.scrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.scrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		this.scrollPane.setBorder(new TitledBorder(new LineBorder(Color.black),
				"WARNING LIST"));

		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		this.setWarnings(new ArrayList<Warning>());
		// this.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		this.selectionHandler = new SelectionHandler();
		this.getSelectionModel()
				.addListSelectionListener(this.selectionHandler);

		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
					if (2 == e.getClickCount()) {
						for (final int index : WarningListView.this
								.getSelectedRows()) {
							final int modelIndex = WarningListView.this
									.convertRowIndexToModel(index);
							final WarningListViewModel model = (WarningListViewModel) WarningListView.this
									.getModel();
							final Warning warning = model.warnings
									.get(modelIndex);
							final int id = warning.pattern.mergedID;

							final SelectedEntities<Integer> trivialPatterns = SelectedEntities
									.<Integer> getInstance(SelectedEntities.TRIVIAL_PATTERN);
							if (trivialPatterns.contains(id)) {
								trivialPatterns
										.remove(id, WarningListView.this);
							} else {
								trivialPatterns.add(id, WarningListView.this);
							}
						}
						WarningListView.this.repaint();
					}
				}
			}
		});
	}

	public void setWarnings(final List<Warning> warnings) {

		this.getSelectionModel().removeListSelectionListener(
				this.selectionHandler);
		this.getSelectionModel().clearSelection();

		final WarningListViewModel model = new WarningListViewModel(warnings,
				this.pWarnings);
		this.setModel(model);
		final RowSorter<WarningListViewModel> sorter = new TableRowSorter<>(
				model);
		this.setRowSorter(sorter);

		final WRenderer renderer = new WRenderer();
		final TableColumnModel columnModel = this.getColumnModel();
		final TableColumn[] column = new TableColumn[model.getColumnCount()];
		for (int i = 0; i < column.length; i++) {
			column[i] = columnModel.getColumn(i);
			column[i].setCellRenderer(renderer);
		}

		this.getColumnModel().getColumn(0).setMinWidth(40);
		this.getColumnModel().getColumn(1).setMinWidth(70);
		this.getColumnModel().getColumn(2).setMinWidth(50);
		this.getColumnModel().getColumn(3).setMinWidth(70);
		this.getColumnModel().getColumn(4).setMinWidth(55);
		this.getColumnModel().getColumn(5).setMinWidth(70);
		this.getColumnModel().getColumn(6).setMinWidth(70);
		this.getColumnModel().getColumn(7).setMinWidth(80);
		this.getColumnModel().getColumn(8).setMinWidth(70);
		this.getColumnModel().getColumn(9).setMinWidth(70);

		this.getSelectionModel()
				.addListSelectionListener(this.selectionHandler);
	}

	public void init() {
	}

	@Override
	public void update(final Observable o, final Object arg) {

		if (o instanceof SelectedEntities) {

			final SelectedEntities selectedEntities = (SelectedEntities) o;

			if (selectedEntities.getLabel().equals(
					SelectedEntities.SELECTED_PATH)) {

				if (selectedEntities.isSet()) {
					final String path = (String) selectedEntities.get().get(0);
					final List<Warning> warnings = this.fWarnings.get(path);
					Collections.sort(warnings);
					this.setWarnings(warnings);
				} else {
					this.setWarnings(new ArrayList<Warning>());
				}

				this.repaint();
			}
		}
	}

	// @Override
	// public String getToolTipText(final MouseEvent e) {
	// final Point stopPoint = e.getPoint();
	// final int stopRow = this.rowAtPoint(stopPoint);
	// final int modelRow = this.convertRowIndexToModel(stopRow);
	// final WarningListViewModel model = (WarningListViewModel)
	// this.getModel();
	// return model.getPath(modelRow);
	// }

	public class WRenderer extends DefaultTableCellRenderer {

		WRenderer() {
			super();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {

			super.getTableCellRendererComponent(table, value, isSelected,
					hasFocus, row, column);

			final int modelIndex = WarningListView.this
					.convertRowIndexToModel(row);
			final WarningListViewModel model = (WarningListViewModel) WarningListView.this
					.getModel();
			final Warning warning = model.warnings.get(modelIndex);
			final int id = warning.pattern.mergedID;

			final SelectedEntities<Integer> trivialPatterns = SelectedEntities
					.<Integer> getInstance(SelectedEntities.TRIVIAL_PATTERN);
			if (!isSelected) {
				if (trivialPatterns.contains(id)) {
					this.setBackground(Color.LIGHT_GRAY);
				} else {
					this.setBackground(table.getBackground());
				}
			}

			else {
				if (trivialPatterns.contains(id)) {
					this.setBackground(Color.GRAY);
				} else {
					this.setBackground(table.getSelectionBackground());
				}
			}

			return this;
		}
	}
}
