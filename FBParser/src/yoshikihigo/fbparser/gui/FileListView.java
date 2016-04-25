package yoshikihigo.fbparser.gui;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
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
import javax.swing.table.TableRowSorter;

public class FileListView extends JTable implements Observer {

	final public JScrollPane scrollPane;

	public FileListView(final Map<String, List<Warning>> allWarnings) {

		super();

		this.scrollPane = new JScrollPane();
		this.scrollPane.setViewportView(this);
		this.scrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.scrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		this.scrollPane.setBorder(new TitledBorder(new LineBorder(Color.black),
				"FILE LIST"));

		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		this.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		final FileListViewModel model = new FileListViewModel(allWarnings);
		this.setModel(model);
		final RowSorter<FileListViewModel> sorter = new TableRowSorter<>(model);
		this.setRowSorter(sorter);

		this.getColumnModel().getColumn(0).setMinWidth(50);
		this.getColumnModel().getColumn(0).setMaxWidth(50);
		this.getColumnModel().getColumn(1).setMinWidth(300);
		this.getColumnModel().getColumn(2).setMinWidth(70);
		this.getColumnModel().getColumn(2).setMaxWidth(70);

		this.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {

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
							if (FileListView.this.getSelectionModel()
									.isSelectedIndex(i)) {
								SelectedEntities.<String> getInstance(
										SelectedEntities.SELECTED_PATH).add(
										path, FileListView.this);
								SelectedEntities.<Warning> getInstance(
										SelectedEntities.SELECTED_WARNING)
										.clear(FileListView.this);
							} else {
								SelectedEntities.<String> getInstance(
										SelectedEntities.SELECTED_PATH).remove(
										path, FileListView.this);
								SelectedEntities.<Warning> getInstance(
										SelectedEntities.SELECTED_WARNING)
										.clear(FileListView.this);
							}
						}
					}
				});
	}

	public void init() {
	}

	@Override
	public void update(final Observable o, final Object arg) {
	}

	@Override
	public String getToolTipText(final MouseEvent e) {
		final Point stopPoint = e.getPoint();
		final int stopRow = this.rowAtPoint(stopPoint);
		final int modelRow = this.convertRowIndexToModel(stopRow);
		final FileListViewModel model = (FileListViewModel) this.getModel();
		return model.getPath(modelRow);
	}
}
