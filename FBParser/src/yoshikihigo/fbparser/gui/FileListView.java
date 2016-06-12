package yoshikihigo.fbparser.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

public class FileListView extends JTable implements Observer {

	class SelectionHandler implements ListSelectionListener {

		@Override
		public void valueChanged(final ListSelectionEvent e) {

			if (e.getValueIsAdjusting()) {
				return;
			}

			final int firstIndex = e.getFirstIndex();
			final int lastIndex = e.getLastIndex();
			for (int i = firstIndex; i <= lastIndex; i++) {
				final int modelIndex = FileListView.this
						.convertRowIndexToModel(i);
				final FileListViewModel model = (FileListViewModel) FileListView.this
						.getModel();
				final String path = model.getPath(modelIndex);
				if (FileListView.this.getSelectionModel().isSelectedIndex(i)) {
					SelectedEntities.<String> getInstance(
							SelectedEntities.SELECTED_PATH).add(path,
							FileListView.this);
					SelectedEntities.<Warning> getInstance(
							SelectedEntities.SELECTED_WARNING).clear(
							FileListView.this);
				} else {
					SelectedEntities.<String> getInstance(
							SelectedEntities.SELECTED_PATH).remove(path,
							FileListView.this);
					SelectedEntities.<Warning> getInstance(
							SelectedEntities.SELECTED_WARNING).clear(
							FileListView.this);
				}
			}
		}
	}

	final private SelectionHandler selectionHandler;
	final private Map<String, List<Warning>> fWarnings;
	final public JScrollPane scrollPane;

	public FileListView(final Map<String, List<Warning>> fWarnings) {

		super();

		this.fWarnings = fWarnings;

		this.scrollPane = new JScrollPane();
		this.scrollPane.setViewportView(this);
		this.scrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.scrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		this.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		final FileListViewModel model = new FileListViewModel(fWarnings);
		this.setModel(model);
		final RowSorter<FileListViewModel> sorter = new TableRowSorter<>(model);
		this.setRowSorter(sorter);

		this.getColumnModel().getColumn(0).setMinWidth(50);
		this.getColumnModel().getColumn(0).setMaxWidth(50);
		this.getColumnModel().getColumn(1).setMinWidth(300);
		this.getColumnModel().getColumn(2).setMinWidth(70);
		this.getColumnModel().getColumn(2).setMaxWidth(140);

		this.selectionHandler = new SelectionHandler();
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
					SelectedEntities.TRIVIAL_PATTERN)) {

				final FRenderer renderer = new FRenderer();
				final TableColumnModel columnModel = this.getColumnModel();
				final TableColumn[] column = new TableColumn[this.getModel()
						.getColumnCount()];
				for (int i = 0; i < column.length; i++) {
					column[i] = columnModel.getColumn(i);
					column[i].setCellRenderer(renderer);
				}

				this.repaint();
			}

			else if (selectedEntities.getLabel().equals(
					SelectedEntities.FOCUSING_PATTERN)
					|| selectedEntities.getLabel().equals(
							SelectedEntities.LOGKEYWORD_PATTERN)
					|| selectedEntities.getLabel().equals(
							SelectedEntities.PATHKEYWORD_PATTERN)) {

				this.getSelectionModel().removeListSelectionListener(
						this.selectionHandler);

				final List<Integer> focusingPatterns = SelectedEntities
						.<Integer> getInstance(
								SelectedEntities.FOCUSING_PATTERN).get();
				final List<String> focusingPaths = SelectedEntities
						.<String> getInstance(
								SelectedEntities.PATHKEYWORD_PATTERN).get();

				final TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) this
						.getRowSorter();

				if (focusingPatterns.isEmpty() && focusingPaths.isEmpty()) {
					sorter.setRowFilter(null);
				}

				else if (!focusingPatterns.isEmpty() && focusingPaths.isEmpty()) {

					final Set<String> showingPaths = getPaths(focusingPatterns);
					sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
						@Override
						public boolean include(
								Entry<? extends TableModel, ? extends Integer> entry) {
							final String path = entry.getStringValue(1);
							return showingPaths.contains(path);
						}
					});
				}

				else if (focusingPatterns.isEmpty() && !focusingPaths.isEmpty()) {
					sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
						@Override
						public boolean include(
								Entry<? extends TableModel, ? extends Integer> entry) {
							final String path = entry.getStringValue(1);
							return focusingPaths.contains(path);
						}
					});
				}

				else {
					final Set<String> showingPaths = getPaths(focusingPatterns);
					sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
						@Override
						public boolean include(
								Entry<? extends TableModel, ? extends Integer> entry) {
							final String path = entry.getStringValue(1);
							return showingPaths.contains(path)
									&& focusingPaths.contains(path);
						}
					});
				}

				this.repaint();

				this.getSelectionModel().addListSelectionListener(
						this.selectionHandler);
			}
		}
	}

	private Set<String> getPaths(final List<Integer> patternIDs) {
		final Set<String> paths = new HashSet<>();
		for (final Entry<String, List<Warning>> entry : this.fWarnings
				.entrySet()) {
			final String path = entry.getKey();
			final List<Warning> warnings = entry.getValue();
			final Set<Integer> patterns = warnings.stream()
					.map(warning -> warning.pattern.mergedID)
					.collect(Collectors.toSet());
			patterns.retainAll(patternIDs);
			if (!patterns.isEmpty()) {
				paths.add(path);
			}
		}
		return paths;
	}

	@Override
	public String getToolTipText(final MouseEvent e) {
		final Point stopPoint = e.getPoint();
		final int stopRow = this.rowAtPoint(stopPoint);
		final int modelRow = this.convertRowIndexToModel(stopRow);
		final FileListViewModel model = (FileListViewModel) this.getModel();
		return model.getPath(modelRow);
	}

	public class FRenderer extends DefaultTableCellRenderer {

		final Set<Integer> trivialPatterns;

		FRenderer() {
			super();
			this.trivialPatterns = new HashSet<>(SelectedEntities
					.<Integer> getInstance(SelectedEntities.TRIVIAL_PATTERN)
					.get());
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {

			super.getTableCellRendererComponent(table, value, isSelected,
					hasFocus, row, column);

			final int modelIndex = FileListView.this
					.convertRowIndexToModel(row);
			final FileListViewModel model = (FileListViewModel) FileListView.this
					.getModel();
			final List<Warning> warnings = model.getWarnings(modelIndex);
			final Set<Integer> patterns = warnings.stream()
					.map(warning -> warning.pattern.mergedID)
					.collect(Collectors.toSet());
			if (SelectedEntities.getInstance(
					SelectedEntities.LOGKEYWORD_PATTERN).isSet()) {
				patterns.retainAll(SelectedEntities.getInstance(
						SelectedEntities.LOGKEYWORD_PATTERN).get());
			}
			if (!isSelected) {
				if (this.trivialPatterns.containsAll(patterns)) {
					this.setBackground(Color.LIGHT_GRAY);
				} else {
					this.setBackground(table.getBackground());
				}
			}

			else {
				if (this.trivialPatterns.containsAll(patterns)) {
					this.setBackground(Color.GRAY);
				} else {
					this.setBackground(table.getSelectionBackground());
				}
			}

			return this;
		}
	}
}
