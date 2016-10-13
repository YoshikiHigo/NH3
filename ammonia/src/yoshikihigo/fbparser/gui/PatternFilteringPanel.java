package yoshikihigo.fbparser.gui;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;

import yoshikihigo.fbparser.XLSXMerger.PATTERN;
import yoshikihigo.fbparser.db.DAO;

public class PatternFilteringPanel extends JPanel implements Observer {

	final private JTextField logKeywordField;
	final private JTextField matchedNumberField;
	final private JRadioButton includingButton;
	final private JRadioButton excludingButton;
	final private JRadioButton andButton;
	final private JRadioButton orButton;

	final private Map<String, List<Warning>> fWarnings;
	final private Map<PATTERN, List<Warning>> pWarnings;

	public PatternFilteringPanel(final Map<String, List<Warning>> fWarnings,
			final Map<PATTERN, List<Warning>> pWarnings) {

		super(new BorderLayout());

		this.setBorder(new LineBorder(Color.black));

		this.fWarnings = fWarnings;
		this.pWarnings = pWarnings;

		this.logKeywordField = new JTextField();
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

		this.add(new JLabel(" WORDS FOR FILTERING CHANGE PATTERNS "),
				BorderLayout.WEST);
		this.add(this.logKeywordField, BorderLayout.CENTER);
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

		final JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		this.add(southPanel, BorderLayout.SOUTH);
		southPanel.add(new JLabel(" METRICS FOR FILTERING CHANGE PATTERNS "));
		final JPanel matchedNumberPanel = new JPanel(new FlowLayout(
				FlowLayout.LEFT));
		southPanel.add(matchedNumberPanel);
		matchedNumberPanel.setBorder(new EtchedBorder());
		matchedNumberPanel.add(new JLabel(" UPPER LIMIT OF MATCHED NUMBER "));
		this.matchedNumberField = new JTextField(5);
		matchedNumberPanel.add(this.matchedNumberField);

		this.logKeywordField
				.addActionListener(e -> {

					this.setCursor(Cursor
							.getPredefinedCursor(Cursor.WAIT_CURSOR));

					this.logKeywordField.setEnabled(false);
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
							this.logKeywordField.getText(), " \t");
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

					this.setCursor(Cursor
							.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					this.logKeywordField.setEnabled(true);
					this.includingButton.setEnabled(true);
					this.excludingButton.setEnabled(true);
					this.andButton.setEnabled(true);
					this.orButton.setEnabled(true);
				});

		this.matchedNumberField
				.addActionListener(e -> {

					this.setCursor(Cursor
							.getPredefinedCursor(Cursor.WAIT_CURSOR));

					this.matchedNumberField.setEnabled(false);

					SelectedEntities.getInstance(
							SelectedEntities.SELECTED_WARNING).clear(this);
					SelectedEntities
							.getInstance(SelectedEntities.SELECTED_PATH).clear(
									this);

					final String text = this.matchedNumberField.getText();
					if (text.isEmpty()) {
						SelectedEntities.getInstance(
								SelectedEntities.METRICS_PATTERN).clear(this);
					}

					else {
						final int matchedNumberThreshold;
						try {
							matchedNumberThreshold = Integer.parseInt(text);
						} catch (final NumberFormatException exception) {
							JOptionPane.showMessageDialog(this,
									"Text must be int value.");
							this.matchedNumberField.setEnabled(true);
							return;
						}

						final Set<Integer> patternIDs = this.pWarnings
								.keySet()
								.stream()
								.filter(p -> this.pWarnings.get(p).size() <= matchedNumberThreshold)
								.map(p -> p.mergedID)
								.collect(Collectors.toSet());

						if (patternIDs.isEmpty()) {
							patternIDs.add(Integer.valueOf(-1));
						}
						SelectedEntities.<Integer> getInstance(
								SelectedEntities.METRICS_PATTERN).setAll(
								patternIDs, this);
					}

					this.matchedNumberField.setCursor(Cursor
							.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					this.matchedNumberField.setEnabled(true);
				});
	}

	@Override
	public void update(final Observable o, final Object arg) {
	}
}
