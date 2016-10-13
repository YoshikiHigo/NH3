package nh3.ammonia.gui;

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

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

public class FilePathKeywordField extends JPanel implements Observer {

	final private Map<String, List<Warning>> fWarnings;

	final private JTextField field;
	final private JRadioButton includingButton;
	final private JRadioButton excludingButton;
	final private JRadioButton andButton;
	final private JRadioButton orButton;

	public FilePathKeywordField(final Map<String, List<Warning>> fWarnings) {

		super(new BorderLayout());

		this.fWarnings = fWarnings;

		this.field = new JTextField();
		this.includingButton = new JRadioButton("INC", true);
		this.excludingButton = new JRadioButton("EXC", false);
		this.andButton = new JRadioButton("AND", true);
		this.orButton = new JRadioButton("OR", false);

		final ButtonGroup buttonGroup1 = new ButtonGroup();
		buttonGroup1.add(this.includingButton);
		buttonGroup1.add(this.excludingButton);
		final ButtonGroup buttonGroup2 = new ButtonGroup();
		buttonGroup2.add(this.andButton);
		buttonGroup2.add(this.orButton);

		this.add(new JLabel(" WORDS FOR FILTERING "), BorderLayout.WEST);
		this.add(this.field, BorderLayout.CENTER);
		final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		this.add(buttonPanel, BorderLayout.EAST);
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

		this.field.addActionListener(e -> this.filterFiles());
	}

	private void filterFiles() {

		this.field.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		SelectedEntities.getInstance(SelectedEntities.SELECTED_PATH)
				.clear(this);

		final List<String> words = this.getWords(this.field.getText());
		if (words.isEmpty()) {
			SelectedEntities.getInstance(SelectedEntities.PATHKEYWORD_PATTERN)
					.clear(this);
			return;
		}

		final Set<String> paths = this.fWarnings.keySet();
		final List<List<String>> pathListList = new ArrayList<>();
		for (final String word : words) {
			final List<String> pathList = new ArrayList<>();
			for (final String path : paths) {
				if (this.includingButton.isSelected() && path.contains(word)) {
					pathList.add(path);
				} else if (this.excludingButton.isSelected()
						&& !path.contains(word)) {
					pathList.add(path);
				}
			}
			pathListList.add(pathList);
		}

		final Set<String> matchedPaths = new HashSet<>();
		matchedPaths.addAll(pathListList.get(0));
		if (this.andButton.isSelected()) {
			pathListList.stream().forEach(list -> matchedPaths.retainAll(list));
		} else if (this.orButton.isSelected()) {
			pathListList.stream().forEach(list -> matchedPaths.addAll(list));
		}

		if (matchedPaths.isEmpty()) {
			matchedPaths.add("DUMMYDUMMYDUMMY");
		}

		SelectedEntities.<String> getInstance(
				SelectedEntities.PATHKEYWORD_PATTERN)
				.setAll(matchedPaths, this);

		this.field.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	private List<String> getWords(final String text) {
		final List<String> words = new ArrayList<>();
		final StringTokenizer tokenizer = new StringTokenizer(text, " \t");
		while (tokenizer.hasMoreTokens()) {
			final String word = tokenizer.nextToken();
			words.add(word);
		}
		return words;
	}

	@Override
	public void update(final Observable o, final Object arg) {
	}
}
