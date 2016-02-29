package yoshikihigo.fbparser.gui;

import yoshikihigo.fbparser.XLSXMerger.PATTERN;

public class Warning {

	final public int fromLine;
	final public int toLine;
	final public PATTERN pattern;
	
	public Warning(final int fromLine, final int toLine, final PATTERN pattern){
		this.fromLine = fromLine;
		this.toLine = toLine;
		this.pattern = pattern;
	}
}
