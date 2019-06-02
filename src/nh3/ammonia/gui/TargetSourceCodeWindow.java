package nh3.ammonia.gui;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
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

public class TargetSourceCodeWindow extends JTextArea implements Observer {

  private static final int TAB_SIZE = 2;
  private static final Color WARNING = new Color(180, 180, 180, 125);
  private static final Color SWARNING = new Color(0, 200, 0, 50);

  final private TargetSourceCodeUI sourceCodeUI;

  final private JScrollPane scrollPane;

  final private Map<String, String> contents;
  final private Map<String, List<Warning>> warnings;

  public TargetSourceCodeWindow(final Map<String, String> contents,
      final Map<String, List<Warning>> warnings) {

    super();

    Insets margin = new Insets(5, 50, 5, 5);
    this.setMargin(margin);

    this.sourceCodeUI = new TargetSourceCodeUI(this, margin);

    this.contents = contents;
    this.warnings = warnings;

    this.setUI(this.sourceCodeUI);
    this.setTabSize(TAB_SIZE);

    this.scrollPane = new JScrollPane();
    this.scrollPane.setViewportView(this);

    this.scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    this.scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

    this.scrollPane.setBorder(new TitledBorder(new LineBorder(Color.black), "SOURCE CODE"));
  }

  private int getFromOffset(final int line) throws BadLocationException {
    return 0 < line ? super.getLineStartOffset(line - 1) : 0;
  }

  private int getToOffset(final int line) throws BadLocationException {
    return 0 < line ? super.getLineEndOffset(line - 1) : 0;
  }

  private void setHighlights(final Warning selectedWarning, final List<Warning> warnings) {

    this.getHighlighter()
        .removeAllHighlights();

    for (final Warning warning : warnings) {
      final DefaultHighlightPainter painter =
          new DefaultHighlightPainter(warning.equals(selectedWarning) ? SWARNING : WARNING);
      try {
        final int fromOffset = this.getFromOffset(warning.fromLine);
        final int toOffset = this.getToOffset(warning.toLine);
        this.getHighlighter()
            .addHighlight(fromOffset, toOffset, painter);
      } catch (final BadLocationException e) {
        e.printStackTrace();
      }
    }
  }

  public JScrollPane getScrollPane() {
    return this.scrollPane;
  }

  public void update(Observable o, Object arg) {

    if (o instanceof SelectedEntities) {

      final SelectedEntities selectedEntities = (SelectedEntities) o;

      if (selectedEntities.getLabel()
          .equals(SelectedEntities.SELECTED_PATH)) {

        this.setText("");

        if (selectedEntities.isSet()) {
          this.setUI(this.sourceCodeUI);
          final String path = (String) selectedEntities.get()
              .get(0);
          final String text = this.contents.get(path);
          this.setText(text);
          this.setCaretPosition(0);

          final List<Warning> warnings = this.warnings.get(path);
          this.setHighlights(null, warnings);
        }

        this.repaint();
      }

      else if (selectedEntities.getLabel()
          .equals(SelectedEntities.SELECTED_WARNING)) {

        final Warning selectedWarning = selectedEntities.isSet() ? (Warning) selectedEntities.get()
            .get(0) : null;
        if (SelectedEntities.getInstance(SelectedEntities.SELECTED_PATH)
            .isSet()) {
          final String path = (String) SelectedEntities.getInstance(SelectedEntities.SELECTED_PATH)
              .get()
              .get(0);
          final List<Warning> warnings = this.warnings.get(path);
          this.setHighlights(selectedWarning, warnings);
        }

        this.repaint();

        if (null != selectedWarning) {
          this.displayAt(selectedWarning.fromLine);
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
      Rectangle vr = this.getScrollPane()
          .getViewport()
          .getViewRect();
      rect.setSize(10, vr.height);
      this.scrollRectToVisible(rect);
      this.setCaretPosition(elem.getStartOffset());
    } catch (BadLocationException e) {
      System.err.println(e.getMessage());
    }
  }
}
