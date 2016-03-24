package yoshikihigo.fbparser.gui;

import java.awt.Color;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import yoshikihigo.fbparser.db.DAO;

public class PastChangesView extends JTabbedPane implements Observer {

	public PastChangesView() {
		this.setBorder(new TitledBorder(new LineBorder(Color.black),
				"PAST CHANGES"));
	}

	@Override
	public void update(final Observable o, final Object arg) {

		if (o instanceof SelectedEntities) {

			final SelectedEntities selectedEntities = (SelectedEntities) o;

			if (selectedEntities.getLabel().equals(
					SelectedEntities.SELECTED_WARNING)) {

				if (selectedEntities.isSet()) {
					final Warning warning = (Warning) selectedEntities.get()
							.get(0);
					final String beforeNText = warning.pattern.beforeText;
					final String afterNText = warning.pattern.afterText;

					final List<String[]> rTexts = DAO.getInstance().getRTexts(
							beforeNText, afterNText);

					for (final String[] rText : rTexts) {
						final ChangeInstanceView before = new ChangeInstanceView(
								"BEFORE TEXT", rText[0]);
						final ChangeInstanceView after = new ChangeInstanceView(
								"AFTER TEXT", rText[1]);

						final JSplitPane panel = new JSplitPane(
								JSplitPane.VERTICAL_SPLIT);
						panel.add(before.scrollPane, JSplitPane.TOP);
						panel.add(after.scrollPane, JSplitPane.BOTTOM);
						this.addTab(Integer.toString(this.getTabCount() + 1),
								panel);
						panel.setDividerLocation((this.getHeight() - 60) / 2);
					}

				} else {
					this.removeAll();
				}
				this.repaint();
			}

			else if (selectedEntities.getLabel().equals(
					SelectedEntities.SELECTED_PATH)) {
				this.removeAll();
				this.repaint();
			}
		}
	}

	class ChangeInstanceView extends JTextArea {

		final JScrollPane scrollPane;

		public ChangeInstanceView(final String title, final String code) {
			this.scrollPane = new JScrollPane();
			this.scrollPane.setViewportView(this);
			this.scrollPane
					.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			this.scrollPane
					.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			this.scrollPane.setBorder(new TitledBorder(new LineBorder(
					Color.black), title));
			this.setText(code);
		}
	}
}
