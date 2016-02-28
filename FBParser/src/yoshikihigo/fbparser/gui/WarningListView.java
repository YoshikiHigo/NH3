package yoshikihigo.fbparser.gui;

import java.awt.Color;
import java.util.HashMap;
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
import javax.swing.table.TableRowSorter;

import yoshikihigo.fbparser.XLSXMerger.PATTERN;

public class WarningListView extends JTable implements Observer {

	class SelectionHandler implements ListSelectionListener {

		@Override
		public void valueChanged(final ListSelectionEvent e) {

			if (e.getValueIsAdjusting()) {
				return;
			}

			final int firstIndex = e.getFirstIndex();
			final int lastIndex = e.getLastIndex();
			for (int i = firstIndex; i <= lastIndex; i++) {
				final int modelIndex = WarningListView.this
						.convertRowIndexToModel(i);
				final WarningListViewModel model = (WarningListViewModel) WarningListView.this
						.getModel();
				final int[] location = model.getLocation(modelIndex);
				final PATTERN pattern = model.getPATTERN(modelIndex);
				if (WarningListView.this.getSelectionModel().isSelectedIndex(i)) {
					SelectedEntities.<Integer> getInstance(
							SelectedEntities.SELECTED_LOCATION).add(
							location[0], WarningListView.this);
					SelectedEntities.<PATTERN> getInstance(
							SelectedEntities.SELECTED_PATTERN).add(pattern,
							WarningListView.this);
				} else {
					SelectedEntities.<Integer> getInstance(
							SelectedEntities.SELECTED_LOCATION).remove(
							location[0], WarningListView.this);
					SelectedEntities.<PATTERN> getInstance(
							SelectedEntities.SELECTED_PATTERN).remove(pattern,
							WarningListView.this);
				}
			}
		}
	}

	final private SelectionHandler selectionHandler;
	final private Map<String, Map<int[], PATTERN>> files;
	final public JScrollPane scrollPane;

	public WarningListView(final Map<String, Map<int[], PATTERN>> files) {

		super();

		this.files = files;
		this.scrollPane = new JScrollPane();
		this.scrollPane.setViewportView(this);
		this.scrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.scrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		this.scrollPane.setBorder(new TitledBorder(new LineBorder(Color.black),
				"WARNING List"));

		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		this.setWarnings(new HashMap<int[], PATTERN>());
		// this.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		this.selectionHandler = new SelectionHandler();
		this.getSelectionModel()
				.addListSelectionListener(this.selectionHandler);
	}

	public void setWarnings(final Map<int[], PATTERN> warnings) {

		this.getSelectionModel().removeListSelectionListener(
				this.selectionHandler);
		this.getSelectionModel().clearSelection();

		final WarningListViewModel model = new WarningListViewModel(warnings);
		this.setModel(model);
		final RowSorter<WarningListViewModel> sorter = new TableRowSorter<>(
				model);
		this.setRowSorter(sorter);

		// this.getColumnModel().getColumn(0).setMinWidth(120);
		// this.getColumnModel().getColumn(0).setMaxWidth(120);

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
					final Map<int[], PATTERN> warnings = this.files.get(path);
					this.setWarnings(warnings);
				} else {
					this.setWarnings(new HashMap<int[], PATTERN>());
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
