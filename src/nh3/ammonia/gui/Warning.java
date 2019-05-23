package nh3.ammonia.gui;

import nh3.ammonia.XLSXMerger.PATTERN;

public class Warning implements Comparable<Warning> {

	final public int fromLine;
	final public int toLine;
	final public PATTERN pattern;

	public Warning(final int fromLine, final int toLine, final PATTERN pattern) {
		this.fromLine = fromLine;
		this.toLine = toLine;
		this.pattern = pattern;
	}

	@Override
	public int compareTo(final Warning target) {
		if (this.fromLine < target.fromLine) {
			return -1;
		} else if (this.fromLine > target.fromLine) {
			return 1;
		} else if (this.toLine < target.toLine) {
			return -1;
		} else if (this.toLine > target.toLine) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	public String toString() {
		if (this.fromLine == this.toLine) {
			return Integer.toString(this.fromLine);
		} else {
			return this.fromLine + "--" + this.toLine;
		}
	}
}
