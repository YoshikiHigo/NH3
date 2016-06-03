package yoshikihigo.fbparser.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import yoshikihigo.fbparser.FBParserConfig;
import yoshikihigo.fbparser.StringUtility;
import yoshikihigo.fbparser.db.DAO;
import yoshikihigo.fbparser.db.DAO.CHANGE_SQL;

public class PastChangesView extends JTabbedPane implements Observer {

	public PastChangesView() {
		this.setBorder(new TitledBorder(new LineBorder(Color.black),
				"PAST CHANGES"));

		this.addChangeListener(e -> {
			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			final PastChange pastChange = (PastChange) this
					.getSelectedComponent();
			if (null != pastChange) {
				pastChange.loadCode(this.getHeight());
			}

			this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		});
	}

	@Override
	public void update(final Observable o, final Object arg) {

		if (o instanceof SelectedEntities) {

			final SelectedEntities selectedEntities = (SelectedEntities) o;

			if (selectedEntities.getLabel().equals(
					SelectedEntities.SELECTED_WARNING)) {

				this.removeAll();

				if (selectedEntities.isSet()) {
					final Warning warning = (Warning) selectedEntities.get()
							.get(0);
					final String beforeNText = warning.pattern.beforeText;
					final String afterNText = warning.pattern.afterText;

					final List<CHANGE_SQL> changes = DAO.getInstance()
							.getChanges(beforeNText, afterNText);

					for (final CHANGE_SQL change : changes) {
						final PastChange pastChange = new PastChange(change);
						this.addTab(Integer.toString(this.getTabCount() + 1),
								pastChange);
					}
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
}

class PastChange extends JPanel {

	final private CHANGE_SQL change;
	private JSplitPane srcPane;

	PastChange(final CHANGE_SQL change) {
		super(new BorderLayout());
		this.change = change;

		final JLabel label1 = new JLabel("Revision: " + change.revision
				+ ", Author: " + change.author + ", Path: " + change.filepath);
		final JLabel label2 = new JLabel("Commit log: " + change.message);
		final JPanel labelPanel = new JPanel(new BorderLayout());
		labelPanel.add(label1, BorderLayout.NORTH);
		labelPanel.add(label2, BorderLayout.CENTER);
		this.add(labelPanel, BorderLayout.NORTH);

		this.srcPane = null;

		labelPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				if (e.getClickCount() == 2) {
					final JFrame frame = new JFrame("Commit Information");
					frame.setSize(500, 600);
					final JTextArea text = new JTextArea();
					text.append("Revision: ");
					text.append(Integer.toString(change.revision));
					text.append(System.lineSeparator());
					text.append(System.lineSeparator());
					text.append("Author: ");
					text.append(change.author);
					text.append(System.lineSeparator());
					text.append(System.lineSeparator());
					text.append("File: ");
					text.append(change.filepath);
					text.append(System.lineSeparator());
					text.append(System.lineSeparator());
					text.append("Log: ");
					text.append(change.message);
					text.setEditable(false);
					text.setLineWrap(true);
					frame.getContentPane().add(text);
					frame.setVisible(true);
				}
			}
		});
	}

	void loadCode(final int height) {

		if (null != this.srcPane) {
			return;
		}

		final String beforeText = this.getText(this.change.filepath,
				this.change.revision - 1);
		final String afterText = this.getText(this.change.filepath,
				this.change.revision);

		final ChangeInstanceView beforeView = new ChangeInstanceView(
				"BEFORE TEXT", beforeText);
		final ChangeInstanceView afterView = new ChangeInstanceView(
				"AFTER TEXT", afterText);

		this.srcPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		this.srcPane.add(beforeView.scrollPane, JSplitPane.TOP);
		this.srcPane.add(afterView.scrollPane, JSplitPane.BOTTOM);
		this.add(this.srcPane, BorderLayout.CENTER);
		this.srcPane.setDividerLocation((height - 100) / 2);

		beforeView.addHighlight(this.change.beforeStartLine,
				this.change.beforeEndLine);
		afterView.addHighlight(this.change.afterStartLine,
				this.change.afterEndLine);

		beforeView.displayAt(this.change.beforeEndLine);
		afterView.displayAt(this.change.afterEndLine);
	}

	private String getText(final String path, final int revision) {

		final String repository = FBParserConfig.getInstance().getREPOSITORY();
		final SVNURL url = StringUtility.getSVNURL(repository, path);
		FSRepositoryFactory.setup();
		SVNWCClient wcClient = SVNClientManager.newInstance().getWCClient();

		final StringBuilder text = new StringBuilder();
		try {
			wcClient.doGetFileContents(url, SVNRevision.create(revision),
					SVNRevision.create(revision), false, new OutputStream() {
						@Override
						public void write(int b) throws IOException {
							text.append((char) b);
						}
					});
		} catch (final SVNException | NullPointerException e) {
			e.printStackTrace();
			return "";
		}

		return text.toString();
	}

	class ChangeInstanceView extends JTextArea {

		final JScrollPane scrollPane;

		public ChangeInstanceView(final String title, final String code) {

			super();

			final Insets margin = new Insets(5, 50, 5, 5);
			this.setMargin(margin);

			final TargetSourceCodeUI sourceCodeUI = new TargetSourceCodeUI(
					this, margin);
			this.setUI(sourceCodeUI);
			this.setTabSize(2);

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

		private void addHighlight(final int startline, final int endline) {

			final DefaultHighlightPainter painter = new DefaultHighlightPainter(
					new Color(200, 0, 0, 50));

			try {

				int startOffset = 0;
				int endOffset = 0;

				if (0 < startline) {
					startOffset = super.getLineStartOffset(startline - 1);
				}

				if (0 < endline) {
					endOffset = super.getLineEndOffset(endline - 1);
				}

				this.getHighlighter().addHighlight(startOffset, endOffset,
						painter);

			} catch (final BadLocationException e) {
				e.printStackTrace();
			}
		}

		public void displayAt(final int line) {
			int offset = 0;
			try {
				offset = super.getLineEndOffset(line);
			} catch (final BadLocationException e) {
				e.printStackTrace();
			}
			this.setCaretPosition(offset);
		}
	}
}