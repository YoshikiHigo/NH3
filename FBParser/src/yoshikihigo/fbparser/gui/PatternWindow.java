package yoshikihigo.fbparser.gui;

import java.awt.Color;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import yoshikihigo.fbparser.XLSXMerger.PATTERN;

public class PatternWindow extends JTextArea implements Observer {

	enum TYPE {
		BEFORE, AFTER;
	}

	final TYPE type;
	final JScrollPane scrollPane;

	public PatternWindow(final TYPE type) {
		this.type = type;

		this.scrollPane = new JScrollPane();
		this.scrollPane.setViewportView(this);

		this.scrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.scrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		switch (type) {
		case BEFORE:
			this.scrollPane.setBorder(new TitledBorder(new LineBorder(
					Color.black), "BEFORE TEXT"));
			break;
		case AFTER:
			this.scrollPane.setBorder(new TitledBorder(new LineBorder(
					Color.black), "AFTER TEXT"));
			break;
		}
	}

	@Override
	public void update(final Observable o, final Object arg) {

		if (o instanceof SelectedEntities) {

			final SelectedEntities selectedEntities = (SelectedEntities) o;

			if (selectedEntities.getLabel().equals(
					SelectedEntities.SELECTED_PATTERN)) {

				if (selectedEntities.isSet()) {
					final PATTERN pattern = (PATTERN) selectedEntities.get()
							.get(0);
					switch (this.type) {
					case BEFORE:
						this.setText(pattern.beforeText);
						break;
					case AFTER:
						this.setText(pattern.afterText);
						break;
					}
				} else {
					this.setText("");
				}
				this.repaint();
			}

			else if (selectedEntities.getLabel().equals(
					SelectedEntities.SELECTED_PATTERN)) {
				this.setText("");
			}
		}
	}
}
