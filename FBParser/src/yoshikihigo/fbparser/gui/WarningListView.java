package yoshikihigo.fbparser.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
	final private Map<String, List<Warning>> allWarnings;
	final private Map<PATTERN, AtomicInteger> matchedNumbers;
	final public JScrollPane scrollPane;

	public WarningListView(final Map<String, List<Warning>> allWarnings,
			final Map<PATTERN, AtomicInteger> matchedNumbers) {

		super();

		this.allWarnings = allWarnings;
		this.matchedNumbers = matchedNumbers;
		this.scrollPane = new JScrollPane();
		this.scrollPane.setViewportView(this);
		this.scrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.scrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		this.scrollPane.setBorder(new TitledBorder(new LineBorder(Color.black),
				"WARNING List"));

		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		this.setWarnings(new ArrayList<Warning>());
		// this.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		this.selectionHandler = new SelectionHandler();
		this.getSelectionModel()
				.addListSelectionListener(this.selectionHandler);
	}

	public void setWarnings(final List<Warning> warnings) {

		this.getSelectionModel().removeListSelectionListener(
				this.selectionHandler);
		this.getSelectionModel().clearSelection();

		final WarningListViewModel model = new WarningListViewModel(warnings,
				this.matchedNumbers);
		this.setModel(model);
		final RowSorter<WarningListViewModel> sorter = new TableRowSorter<>(
				model);
		this.setRowSorter(sorter);

		this.getColumnModel().getColumn(0).setMinWidth(40);
		this.getColumnModel().getColumn(0).setMaxWidth(40);
		this.getColumnModel().getColumn(1).setMinWidth(70);
		this.getColumnModel().getColumn(1).setMaxWidth(90);
		this.getColumnModel().getColumn(2).setMinWidth(50);
		this.getColumnModel().getColumn(2).setMaxWidth(50);
		this.getColumnModel().getColumn(3).setMinWidth(70);
		this.getColumnModel().getColumn(3).setMaxWidth(70);
		this.getColumnModel().getColumn(4).setMinWidth(55);
		this.getColumnModel().getColumn(4).setMaxWidth(55);
		this.getColumnModel().getColumn(5).setMinWidth(70);
		this.getColumnModel().getColumn(5).setMaxWidth(70);
		this.getColumnModel().getColumn(6).setMinWidth(70);
		this.getColumnModel().getColumn(6).setMaxWidth(70);
		this.getColumnModel().getColumn(7).setMinWidth(80);
		this.getColumnModel().getColumn(7).setMaxWidth(140);
		this.getColumnModel().getColumn(8).setMinWidth(70);
		this.getColumnModel().getColumn(8).setMaxWidth(70);
		this.getColumnModel().getColumn(9).setMinWidth(70);
		this.getColumnModel().getColumn(9).setMaxWidth(70);

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
					final List<Warning> warnings = this.allWarnings.get(path);
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
}
