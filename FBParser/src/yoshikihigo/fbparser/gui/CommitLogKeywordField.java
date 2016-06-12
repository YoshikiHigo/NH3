package yoshikihigo.fbparser.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
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
import javax.swing.border.EtchedBorder;

import yoshikihigo.fbparser.XLSXMerger.PATTERN;
import yoshikihigo.fbparser.db.DAO;

public class CommitLogKeywordField extends JPanel implements Observer {

	final private JTextField field;
	final private JRadioButton includingButton;
	final private JRadioButton excludingButton;
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
		this.includingButton = new JRadioButton("INCLUDING (INC)", true);
		this.excludingButton = new JRadioButton("EXCLUDING (EXC)", false);
		final ButtonGroup ieGroup = new ButtonGroup();
		ieGroup.add(this.includingButton);
		ieGroup.add(this.excludingButton);
		this.andButton = new JRadioButton("AND", true);
		this.orButton = new JRadioButton("OR", false);
		final ButtonGroup aoGroup = new ButtonGroup();
		aoGroup.add(this.andButton);
		aoGroup.add(this.orButton);

		this.add(new JLabel("WORDS FOR FILTERING CHANGE PATTERNS"),
				BorderLayout.WEST);
		this.add(this.field, BorderLayout.CENTER);
		final JPanel buttonPanel = new JPanel(new FlowLayout());
		final JPanel iePanel = new JPanel(new GridLayout(1, 2));
		buttonPanel.add(iePanel);
		iePanel.setBorder(new EtchedBorder());
		iePanel.add(this.includingButton);
		iePanel.add(this.excludingButton);
		final JPanel aoPanel = new JPanel(new GridLayout(1, 2));
		buttonPanel.add(aoPanel);
		aoPanel.setBorder(new EtchedBorder());
		aoPanel.add(this.andButton);
		aoPanel.add(this.orButton);
		this.add(buttonPanel, BorderLayout.EAST);

		this.field
				.addActionListener(e -> {

					this.field.setCursor(Cursor
							.getPredefinedCursor(Cursor.WAIT_CURSOR));

					this.field.setEnabled(false);
					this.includingButton.setEnabled(false);
					this.excludingButton.setEnabled(false);
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

						final List<String> words = new ArrayList<>();
						while (tokenizer.hasMoreTokens()) {
							words.add(tokenizer.nextToken());
						}

						final Set<Integer> allPatternIDs = DAO.getInstance()
								.getFixChangePatterns().stream()
								.map(pattern -> pattern.id)
								.collect(Collectors.toSet());

						final List<Set<Integer>> includingPatternIDs = words
								.stream()
								.map(word -> DAO.getInstance()
										.getFixChangePatterns(word).stream()
										.map(pattern -> pattern.id)
										.collect(Collectors.toSet()))
								.collect(Collectors.toList());

						final Set<Integer> keyPatternIDs = new HashSet<>();
						if (this.includingButton.isSelected()) {

							if (this.andButton.isSelected()) {
								keyPatternIDs.addAll(includingPatternIDs.get(0));
								includingPatternIDs.stream().forEach(
										p -> keyPatternIDs.retainAll(p));
							} else if (this.orButton.isSelected()) {
								includingPatternIDs.stream().forEach(
										p -> keyPatternIDs.addAll(p));
							}

							if (keyPatternIDs.isEmpty()) {
								keyPatternIDs.add(Integer.valueOf(-1));
							}
							SelectedEntities.<Integer> getInstance(
									SelectedEntities.LOGKEYWORD_PATTERN)
									.setAll(keyPatternIDs, this);
						}

						else if (this.excludingButton.isSelected()) {
							final List<Set<Integer>> excludingPatternIDs = new ArrayList<>();
							for (final Set<Integer> ids : includingPatternIDs) {
								final Set<Integer> excluding = new HashSet<>(
										allPatternIDs);
								excluding.removeAll(ids);
								excludingPatternIDs.add(excluding);
							}

							if (this.andButton.isSelected()) {
								keyPatternIDs.addAll(excludingPatternIDs.get(0));
								excludingPatternIDs.stream().forEach(
										p -> keyPatternIDs.retainAll(p));
							} else if (this.orButton.isSelected()) {
								excludingPatternIDs.stream().forEach(
										p -> keyPatternIDs.addAll(p));
							}

							if (keyPatternIDs.isEmpty()) {
								keyPatternIDs.add(Integer.valueOf(-1));
							}
							SelectedEntities.<Integer> getInstance(
									SelectedEntities.LOGKEYWORD_PATTERN)
									.setAll(keyPatternIDs, this);
						}
					}

					this.field.setCursor(Cursor
							.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					this.field.setEnabled(true);
					this.includingButton.setEnabled(true);
					this.excludingButton.setEnabled(true);
					this.andButton.setEnabled(true);
					this.orButton.setEnabled(true);
				});
	}

	@Override
	public void update(final Observable o, final Object arg) {
	}
}
