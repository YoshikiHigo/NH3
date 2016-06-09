package yoshikihigo.fbparser.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import yoshikihigo.fbparser.XLSXMerger.PATTERN;
import yoshikihigo.fbparser.db.DAO;

public class CommitLogKeywordField extends JPanel implements Observer {

	final private JTextField field;
	final private JRadioButton andButton;
	final private JRadioButton orButton;

	final private Map<String, List<Warning>> fWarnings;
	final private Map<PATTERN, List<Warning>> pWarnings;

	public CommitLogKeywordField(final Map<String, List<Warning>> fWarnings,
			final Map<PATTERN, List<Warning>> pWarnings) {

		super(new BorderLayout());

		this.fWarnings = fWarnings;
		this.pWarnings = pWarnings;

		this.field = new JTextField();
		this.andButton = new JRadioButton("AND", true);
		this.orButton = new JRadioButton("OR", false);
		final ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(this.andButton);
		buttonGroup.add(this.orButton);

		this.add(new JLabel("Keywords for filtering change patterns"),
				BorderLayout.WEST);
		this.add(this.field, BorderLayout.CENTER);
		final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		buttonPanel.add(this.andButton);
		buttonPanel.add(this.orButton);
		this.add(buttonPanel, BorderLayout.EAST);

		this.field
				.addActionListener(e -> {

					this.field.setCursor(Cursor
							.getPredefinedCursor(Cursor.WAIT_CURSOR));
					this.field.setEnabled(false);
					this.andButton.setEnabled(false);
					this.orButton.setEnabled(false);

					SelectedEntities.getInstance(
							SelectedEntities.SELECTED_WARNING).clear(this);
					SelectedEntities
							.getInstance(SelectedEntities.SELECTED_PATH).clear(
									this);

					final StringTokenizer tokenizer = new StringTokenizer(
							this.field.getText(), " \t");
					if (0 == tokenizer.countTokens()) {
						SelectedEntities.getInstance(
								SelectedEntities.LOGKEYWORD_PATTERN)
								.clear(this);
					} else {

						final List<String> keywords = new ArrayList<>();
						while (tokenizer.hasMoreTokens()) {
							keywords.add(tokenizer.nextToken());
						}
						final List<Set<Integer>> patterns = keywords
								.stream()
								.map(keyword -> DAO.getInstance()
										.getFixChangePatterns(keyword).stream()
										.map(pattern -> pattern.id)
										.collect(Collectors.toSet()))
								.collect(Collectors.toList());

						final Set<Integer> keyPatterns = new HashSet<>();
						if (this.andButton.isSelected()) {
							keyPatterns.addAll(patterns.get(0));
							patterns.stream().forEach(
									p -> keyPatterns.retainAll(p));
						} else if (this.orButton.isSelected()) {
							patterns.stream().forEach(
									p -> keyPatterns.addAll(p));
						}

						if (keyPatterns.isEmpty()) {
							keyPatterns.add(Integer.valueOf(-1));
						}
						SelectedEntities.<Integer> getInstance(
								SelectedEntities.LOGKEYWORD_PATTERN).setAll(
								keyPatterns, this);
					}

					this.field.setCursor(Cursor
							.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					this.field.setEnabled(true);
					this.andButton.setEnabled(true);
					this.orButton.setEnabled(true);
				});
	}

	@Override
	public void update(final Observable o, final Object arg) {
	}
}
