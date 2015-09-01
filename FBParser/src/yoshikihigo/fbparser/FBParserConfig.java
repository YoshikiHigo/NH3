package yoshikihigo.fbparser;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class FBParserConfig {

	static private FBParserConfig SINGLETON = null;

	static public boolean initialize(final String[] args) {

		if (null != SINGLETON) {
			return false;
		}

		final Options options = new Options();

		{
			final Option source = new Option("src", "source", true,
					"directory of target source code");
			source.setArgName("sourcecode");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("repository", "repository", true,
					"repository of a target software");
			source.setArgName("number");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("startrev", "startrev", true,
					"revision number of the given XML file");
			source.setArgName("number");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("endrev", "endrev", true,
					"revision number of ending border");
			source.setArgName("number");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("fbresult", "fbresult", true,
					"a findbug's result on a target version");
			source.setArgName("xml file(findbug's result)");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("fbresults", "fbresults", true,
					"findbug's results on target versions");
			source.setArgName("xml files(findbug's results)");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("survivingbugscsv",
					"survivingbugscsv", true, "surviving bugs in CSV format");
			source.setArgName("csvfile");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("removedbugscsv",
					"removedbugscsv", true, "removed bugs in CSV format");
			source.setArgName("csvfile");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("addedbugscsv", "addedbugscsv",
					true, "added bugs in CSV format");
			source.setArgName("csvfile");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("metricsresultxlsx",
					"metricsresultxlsx", true, "metrics results in XLSX format");
			source.setArgName("xlsxfile");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option verbose = new Option("v", "verbose", false,
					"verbose output for progressing");
			verbose.setRequired(false);
			options.addOption(verbose);
		}

		{
			final Option debug = new Option("debug", "debug", false,
					"print some informlation for debugging");
			debug.setRequired(false);
			options.addOption(debug);
		}

		try {
			final CommandLineParser parser = new PosixParser();
			final CommandLine commandLine = parser.parse(options, args);
			SINGLETON = new FBParserConfig(commandLine);
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(0);
		}

		return true;
	}

	static public FBParserConfig getInstance() {

		if (null == SINGLETON) {
			System.err.println("Config is not initialized.");
			System.exit(0);
		}

		return SINGLETON;
	}

	private final CommandLine commandLine;

	private FBParserConfig(final CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	public String getSOURCE() {
		if (!this.commandLine.hasOption("src")) {
			System.err.println("option \"src\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("src");
	}

	public String getREPOSITORY() {
		if (!this.commandLine.hasOption("repository")) {
			System.err.println("option \"repository\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("repository");
	}

	public long getSTARTREV() {
		if (!this.commandLine.hasOption("startrev")) {
			System.err.println("option \"startrev\" is not specified.");
			System.exit(0);
		}
		return Long.parseLong(this.commandLine.getOptionValue("startrev"));
	}

	public long getENDREV() {
		if (!this.commandLine.hasOption("endrev")) {
			System.err.println("option \"endrev\" is not specified.");
			System.exit(0);
		}
		return Long.parseLong(this.commandLine.getOptionValue("endrev"));
	}

	public String getFBRESULT() {
		if (!this.commandLine.hasOption("fbresult")) {
			System.err.println("option \"fbresult\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("fbresult");
	}

	public List<String> getFBRESULTS() {
		if (!this.commandLine.hasOption("fbresults")) {
			System.err.println("option \"fbresults\" is not specified.");
			System.exit(0);
		}
		final List<String> versions = new ArrayList<String>();
		final StringTokenizer tokenizer = new StringTokenizer(
				this.commandLine.getOptionValue("fbresults"), ";");
		while (tokenizer.hasMoreTokens()) {
			final String version = tokenizer.nextToken();
			versions.add(version);
		}
		return versions;
	}

	public String getSURVIVINGBUGSCSV() {
		if (!this.commandLine.hasOption("survivingbugscsv")) {
			System.err.println("option \"survivingbugscsv\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("survivingbugscsv");
	}

	public boolean hasSURVIVINGBUGSCSV() {
		return this.commandLine.hasOption("survivingbugscsv");
	}

	public String getREMOVEDBUGSCSV() {
		if (!this.commandLine.hasOption("removedbugscsv")) {
			System.err.println("option \"removedbugscsv\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("removedbugscsv");
	}

	public boolean hasREMOVEDBUGSCSV() {
		return this.commandLine.hasOption("removedbugscsv");
	}

	public String getADDEDBUGSCSV() {
		if (!this.commandLine.hasOption("addedbugscsv")) {
			System.err.println("option \"addedbugscsv\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("addedbugscsv");
	}

	public boolean hasADDEDBUGSCSV() {
		return this.commandLine.hasOption("addedbugscsv");
	}

	public String getMETRICSRESULTXLSX() {
		if (!this.commandLine.hasOption("metricsresultxlsx")) {
			System.err
					.println("option \"metricsresultxlsx\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("metricsresultxlsx");
	}

	public boolean hasMETRICSRESULTXLSX() {
		return this.commandLine.hasOption("metricsresultxlsx");
	}

	public boolean isVERBOSE() {
		return this.commandLine.hasOption("v");
	}

	public boolean isDEBUG() {
		return this.commandLine.hasOption("debug");
	}
}
