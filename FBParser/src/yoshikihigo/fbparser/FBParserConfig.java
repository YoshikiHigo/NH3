package yoshikihigo.fbparser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import yoshikihigo.cpanalyzer.LANGUAGE;

public class FBParserConfig {

	static private FBParserConfig SINGLETON = null;

	static public boolean initialize(final String[] args) {

		if (null != SINGLETON) {
			return false;
		}

		final Options options = new Options();

		{
			final Option option = new Option("lang", "language", true,
					"programming language for analysis");
			option.setArgName("language");
			option.setArgs(1);
			option.setRequired(false);
			options.addOption(option);
		}

		{
			final Option source = new Option("src", "source", true,
					"directory of target source code");
			source.setArgName("sourcecode");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("tr", "transitionresult", true,
					"transition result of bugs found by FindBugs");
			source.setArgName("file");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("cp", "changepattern", true,
					"change patterns for removed bugs");
			source.setArgName("file");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("fcp", "fixchangepattern", true,
					"bug-fix change patterns");
			source.setArgName("file");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("db", "database", true,
					"change patterns found by CPAnalyzer");
			source.setArgName("file");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("bug", "bugfile", true,
					"a csv file include bug IDs");
			source.setArgName("file");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("svnrepo", "svnrepository", true,
					"svn repository of a target software");
			source.setArgName("number");
			source.setArgs(1);
			source.setRequired(false);
			options.addOption(source);
		}

		{
			final Option source = new Option("gitrepo", "gitrepository", true,
					"git repository of a target software");
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
			final Option revision = new Option("svnrev", "svnrevision", true,
					"SVN target revision");
			revision.setArgName("number");
			revision.setArgs(1);
			revision.setRequired(false);
			options.addOption(revision);
		}

		{
			final Option commit = new Option("gitcommit", "gitcommit", true,
					"GIT commit id");
			commit.setArgName("id");
			commit.setArgs(1);
			commit.setRequired(false);
			options.addOption(commit);
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

	public boolean hasLANGUAGE() {
		return this.commandLine.hasOption("lang");
	}
	
	public final Set<LANGUAGE> getLANGUAGE() {

		final Set<LANGUAGE> languages = new HashSet<>();

		if (this.commandLine.hasOption("lang")) {
			final String option = this.commandLine.getOptionValue("lang");
			final StringTokenizer tokenizer = new StringTokenizer(option, ":");
			while (tokenizer.hasMoreTokens()) {
				try {
					final String value = tokenizer.nextToken();
					final LANGUAGE language = LANGUAGE.valueOf(value
							.toUpperCase());
					languages.add(language);
				} catch (final IllegalArgumentException e) {
					System.err.println("invalid option value for \"-lang\"");
					System.exit(0);
				}
			}
		}

		else {
			for (final LANGUAGE language : LANGUAGE.values()) {
				languages.add(language);
			}
		}

		return languages;
	}

	public boolean hasSOURCE() {
		return this.commandLine.hasOption("src");
	}

	public String getSOURCE() {
		if (!this.commandLine.hasOption("src")) {
			System.err.println("option \"src\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("src");
	}

	public String getTRANSITIONRESULT() {
		if (!this.commandLine.hasOption("tr")) {
			System.err.println("option \"tr\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("tr");
	}

	public String getCHANGEPATTERN() {
		if (!this.commandLine.hasOption("cp")) {
			System.err.println("option \"cp\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("cp");
	}

	public boolean hasFIXCHANGEPATTERN() {
		return this.commandLine.hasOption("fcp");
	}

	public String getFIXCHANGEPATTERN() {
		if (!this.commandLine.hasOption("fcp")) {
			System.err.println("option \"fcp\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("fcp");
	}

	public String getDATABASE() {
		if (!this.commandLine.hasOption("db")) {
			System.err.println("option \"db\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("db");
	}

	public String getBUG() {
		if (!this.commandLine.hasOption("bug")) {
			System.err.println("option \"bug\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("bug");
	}

	public boolean hasSVNREPOSITORY() {
		return this.commandLine.hasOption("svnrepo");
	}

	public boolean hasGITREPOSITORY() {
		return this.commandLine.hasOption("gitrepo");
	}

	public String getSVNREPOSITORY() {
		if (!this.commandLine.hasOption("svnrepo")) {
			System.err.println("option \"svnrepo\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("svnrepo");
	}

	public String getGITREPOSITORY() {
		if (!this.commandLine.hasOption("gitrepo")) {
			System.err.println("option \"gitrepo\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("gitrepo");
	}

	public int getSTARTREV() {
		if (!this.commandLine.hasOption("startrev")) {
			System.err.println("option \"startrev\" is not specified.");
			System.exit(0);
		}
		return Integer.parseInt(this.commandLine.getOptionValue("startrev"));
	}

	public int getENDREV() {
		if (!this.commandLine.hasOption("endrev")) {
			System.err.println("option \"endrev\" is not specified.");
			System.exit(0);
		}
		return Integer.parseInt(this.commandLine.getOptionValue("endrev"));
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

	public boolean hasSVNREVISION() {
		return this.commandLine.hasOption("svnrev");
	}

	public int getSVNREVISION() {
		if (!this.commandLine.hasOption("svnrev")) {
			System.err.println("option \"svnrev\" is not specified.");
			System.exit(0);
		}
		return Integer.parseInt(this.commandLine.getOptionValue("svnrev"));
	}

	public boolean hasGITCOMMIT() {
		return this.commandLine.hasOption("gitcommit");
	}

	public String getGITCOMMIT() {
		if (!this.commandLine.hasOption("gitcommit")) {
			System.err.println("option \"gitcommit\" is not specified.");
			System.exit(0);
		}
		return this.commandLine.getOptionValue("gitcommit");
	}

	public boolean isVERBOSE() {
		return this.commandLine.hasOption("v");
	}

	public boolean isDEBUG() {
		return this.commandLine.hasOption("debug");
	}
}
