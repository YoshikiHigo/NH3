package yoshikihigo.fbparser.gui;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Document;
import javax.swing.text.Element;

import yoshikihigo.fbparser.XLSXMerger.PATTERN;

public class SourceCodeWindow extends JTextArea implements Observer {

	private static final int TAB_SIZE = 4;

	final private SourceCodeUI sourceCodeUI;

	final private JScrollPane scrollPane;

	final private Map<String, String> contents;
	final private Map<String, Map<int[], PATTERN>> warnings;

	public SourceCodeWindow(final Map<String, String> contents,
			final Map<String, Map<int[], PATTERN>> warnings) {

		super();

		Insets margin = new Insets(5, 50, 5, 5);
		this.setMargin(margin);

		this.sourceCodeUI = new SourceCodeUI(this, margin);

		this.contents = contents;
		this.warnings = warnings;

		this.setUI(this.sourceCodeUI);
		this.setTabSize(TAB_SIZE);

		this.scrollPane = new JScrollPane();
		this.scrollPane.setViewportView(this);

		this.scrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.scrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		this.scrollPane.setBorder(new TitledBorder(new LineBorder(Color.black),
				"SOURCE CODE"));
	}

	private void addHighlight(final Map<int[], PATTERN> warnings) {

		final DefaultHighlightPainter densePainter = new DefaultHighlightPainter(
				new Color(0, 0, 200, 50));
		final DefaultHighlightPainter middlePainter = new DefaultHighlightPainter(
				new Color(0, 200, 0, 50));
		final DefaultHighlightPainter scatteredPainter = new DefaultHighlightPainter(
				new Color(200, 0, 0, 50));
		final DefaultHighlightPainter uninterestingPainter = new DefaultHighlightPainter(
				new Color(180, 180, 180, 125));

		try {

			for (final Entry<int[], PATTERN> warning : warnings.entrySet()) {

				int fromOffset = 0;
				int toOffset = 0;

				final int fromLine = warning.getKey()[0];
				final int toLine = warning.getKey()[1] + 1;

				if (0 < fromLine) {
					fromOffset = super.getLineStartOffset(fromLine - 1);
				} else if (0 == fromLine) {
					fromOffset = 0;
				} else {
					System.err.println("Error Happened in SourceCodeWindow.");
				}

				if (0 < toLine) {
					toOffset = super.getLineStartOffset(toLine - 1);
				} else if (0 == toLine) {
					toOffset = 0;
				} else {
					System.err.println("Error Happened in SourceCodeWindow.");
				}

				this.getHighlighter().addHighlight(fromOffset, toOffset,
						densePainter);
			}

		} catch (BadLocationException e) {
			System.err.println(e.getMessage());
		}
	}

	public JScrollPane getScrollPane() {
		return this.scrollPane;
	}

	public void update(Observable o, Object arg) {

		if (o instanceof SelectedEntities) {

			final SelectedEntities selectedEntities = (SelectedEntities) o;

			if (selectedEntities.getLabel().equals(
					SelectedEntities.SELECTED_PATH)) {

				this.setText("");
				this.setUI(null);

				if (selectedEntities.isSet()) {
					this.setUI(this.sourceCodeUI);
					final String path = (String) selectedEntities.get().get(0);
					final String text = this.contents.get(path);
					this.setText(text);
					this.setCaretPosition(0);

					final Map<int[], PATTERN> warning = this.warnings.get(path);
					this.addHighlight(warning);
				}

				this.repaint();
			}

			else if (selectedEntities.getLabel().equals(
					SelectedEntities.SELECTED_LOCATION)) {

				if (selectedEntities.isSet()) {
					final Integer location = (Integer) selectedEntities.get()
							.get(0);
					this.displayAt(location);
				}
			}
		}
	}

	public void displayAt(final int line) {

		final Document doc = this.getDocument();
		final Element root = doc.getDefaultRootElement();
		try {
			Element elem = root.getElement(Math.max(1, line - 2));
			Rectangle rect = this.modelToView(elem.getStartOffset());
			Rectangle vr = this.getScrollPane().getViewport().getViewRect();
			rect.setSize(10, vr.height);
			this.scrollRectToVisible(rect);
			this.setCaretPosition(elem.getStartOffset());
		} catch (BadLocationException e) {
			System.err.println(e.getMessage());
		}
	}
}
