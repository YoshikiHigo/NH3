package yoshikihigo.fbparser.gui;

import java.awt.Color;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

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
import yoshikihigo.fbparser.db.DAO;
import yoshikihigo.fbparser.db.DAO.CHANGE_SQL;

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

					final List<CHANGE_SQL> changes = DAO.getInstance()
							.getChanges(beforeNText, afterNText);

					for (final CHANGE_SQL change : changes) {

						final String beforeText = this.getText(change.filepath,
								change.revision - 1);
						final String afterText = this.getText(change.filepath,
								change.revision);

						final ChangeInstanceView beforeView = new ChangeInstanceView(
								"BEFORE TEXT", beforeText);
						final ChangeInstanceView afterView = new ChangeInstanceView(
								"AFTER TEXT", afterText);

						final JSplitPane panel = new JSplitPane(
								JSplitPane.VERTICAL_SPLIT);
						panel.add(beforeView.scrollPane, JSplitPane.TOP);
						panel.add(afterView.scrollPane, JSplitPane.BOTTOM);
						this.addTab(Integer.toString(this.getTabCount() + 1),
								panel);
						panel.setDividerLocation((this.getHeight() - 60) / 2);

						beforeView.addHighlight(change.beforeStartLine,
								change.beforeEndLine);
						afterView.addHighlight(change.afterStartLine,
								change.afterEndLine);

						beforeView.displayAt(change.beforeEndLine);
						afterView.displayAt(change.afterEndLine);
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

	private String getText(final String path, final int revision) {

		final String repository = FBParserConfig.getInstance().getREPOSITORY();
		SVNURL fileurl;
		try {
			fileurl = SVNURL.fromFile(new File(repository
					+ System.getProperty("file.separator") + path));
		} catch (final SVNException e) {
			e.printStackTrace();
			return "";
		}

		FSRepositoryFactory.setup();
		SVNWCClient wcClient = SVNClientManager.newInstance().getWCClient();

		final StringBuilder text = new StringBuilder();
		try {
			wcClient.doGetFileContents(fileurl, SVNRevision.create(revision),
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
