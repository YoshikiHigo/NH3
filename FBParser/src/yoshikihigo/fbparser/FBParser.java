package yoshikihigo.fbparser;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class FBParser {

	public static void main(String[] args) {

		final FBParser parser = new FBParser(args[0]);
		parser.perform();

		try (final BufferedWriter writer = new BufferedWriter(
				(2 <= args.length) ? new FileWriter(args[1]) : new PrintWriter(
						System.out))) {
			for (final ClassStats cs : parser.getSummary()) {
				if (0 < cs.bugs) {
					writer.write(cs.toString());
					writer.newLine();
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	final public String path;
	final private List<BugInstance> buginstances;
	final private SortedSet<ClassStats> summary;

	public FBParser(final String path) {
		this.path = path;
		this.buginstances = new ArrayList<>();
		this.summary = new TreeSet<>();
	}

	public List<BugInstance> getBugInstances() {
		return new ArrayList<BugInstance>(this.buginstances);
	}

	public SortedSet<ClassStats> getSummary() {
		return new TreeSet<ClassStats>(this.summary);
	}

	public void perform() {

		if (0 < this.summary.size()) {
			return;
		}

		final File file = new File(this.path);
		final Document document = read(file);

		if (document.hasChildNodes()) {

			final Node bugCollection = document.getFirstChild();
			assert bugCollection.getNodeName().equals("BugCollection") : "input file has illegal format.";

			if (bugCollection.hasChildNodes()) {

				for (Node cn1 = bugCollection.getFirstChild(); null != cn1; cn1 = cn1
						.getNextSibling()) {

					if (cn1.getNodeName().equals("Project")) {
					}

					else if (cn1.getNodeName().equals("BugInstance")) {

						final NamedNodeMap nnMap1 = cn1.getAttributes();
						final Node typeNode = nnMap1.getNamedItem("type");
						final Node priorityNode = nnMap1
								.getNamedItem("priority");
						final Node rankNode = nnMap1.getNamedItem("rank");
						final Node abbrevNode = nnMap1.getNamedItem("abbrev");
						final Node categoryNode = nnMap1
								.getNamedItem("category");
						final Node instanceHashNode = nnMap1
								.getNamedItem("instanceHash");

						final String type = typeNode.getNodeValue();
						BugPattern pattern = BugPattern.getBugPattern(type);
						if (null == pattern) {
							final String category = categoryNode.getNodeValue();
							pattern = new BugPattern(type, category);
							BugPattern.addBugPattern(pattern);
						}

						final int priority = Integer.parseInt(priorityNode
								.getNodeValue());
						final int rank = Integer.parseInt(rankNode
								.getNodeValue());
						final String instanceHash = instanceHashNode
								.getNodeValue();
						final BugInstance instance = new BugInstance(pattern,
								rank, priority, instanceHash);
						pattern.addBugInstance(this.path, instance);

						for (Node cn2 = cn1.getFirstChild(); null != cn2; cn2 = cn2
								.getNextSibling()) {

							if (cn2.getNodeName().equals("SourceLine")) {
								final SourceLine sourceline = getSourceLine(
										cn2, null);
								if (null != sourceline.sourcepath) {
									instance.addSourceLine(sourceline);
								}
							}
						}

						if (!instance.getSourceLines().isEmpty()) {
							this.buginstances.add(instance);
						}
					}

					else if (cn1.getNodeName().equals("FindBugsSummary")) {
						for (Node cn2 = cn1.getFirstChild(); null != cn2; cn2 = cn2
								.getNextSibling()) {
							if (cn2.getNodeName().equals("PackageStats")) {
								for (Node cn3 = cn2.getFirstChild(); null != cn3; cn3 = cn3
										.getNextSibling()) {
									if (cn3.getNodeName().equals("ClassStats")) {
										final ClassStats cs = ClassStats
												.makeClassStats(cn3);
										this.summary.add(cs);
									}
								}
							}
						}
					}
				}
			}
		}

		// buginstances.sort(new BugInstance.LocationComparator());
		// for (final BugInstance buginstance : buginstances) {
		// print(buginstance);
		// }
	}

	public static Document read(final File file) {

		DocumentBuilder documentBuilder = null;
		try {
			documentBuilder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			System.exit(0);
		}

		Document document = null;
		try {
			if (file.getName().endsWith(".xml")) {
				document = documentBuilder.parse(new BufferedInputStream(
						new FileInputStream(file)));
			} else if (file.getName().endsWith(".gz")) {
				document = documentBuilder.parse(new GZIPInputStream(
						new BufferedInputStream(new FileInputStream(file))));
			} else {
				System.err
						.println("option \"fbresults\" must be .xml or .gz file.");
				System.exit(0);
			}
		} catch (final SAXException | IOException e) {
			e.printStackTrace();
			System.exit(0);
		}

		return document;
	}

	private static SourceLine getSourceLine(final Node sourcelineNode,
			final String name) {
		final NamedNodeMap nnMap = sourcelineNode.getAttributes();
		final Node classnameNode = nnMap.getNamedItem("classname");
		final Node startNode = nnMap.getNamedItem("start");
		final Node endNode = nnMap.getNamedItem("end");
		final Node sourcepathNode = nnMap.getNamedItem("sourcepath");
		final SourceLine sourceline = new SourceLine(
				classnameNode.getNodeValue(),
				(null != startNode) ? Integer.parseInt(startNode.getNodeValue())
						: 0,
				(null != endNode) ? Integer.parseInt(endNode.getNodeValue())
						: 0,
				(null != sourcepathNode) ? sourcepathNode.getNodeValue() : null,
				name);
		return sourceline;
	}

	private static String makeText(final String element,
			final SourceLine sourceline) {
		final StringBuilder text = new StringBuilder();
		text.append("  ");
		text.append(element);
		text.append(": ");
		text.append(sourceline.name);
		if (0 < sourceline.start) {
			text.append(", ");
			text.append(Integer.toString(sourceline.start));
		}
		if (0 < sourceline.end) {
			text.append("--");
			text.append(Integer.toString(sourceline.end));
		}
		return text.toString();
	}
}
