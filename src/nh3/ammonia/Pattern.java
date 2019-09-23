package nh3.ammonia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import yoshikihigo.cpanalyzer.data.Statement;

public class Pattern extends SimplePattern implements Comparable<Pattern> {


  final public List<byte[]> beforeTextHashs;
  final public List<byte[]> afterTextHashs;
  final public List<String> periods;
  final public List<String> ids;
  final public List<String> dates;
  public int mergedID;
  public int findbugsSupport;
  public int support;
  public int bugfixSupport;
  public int beforeTextSupport;
  public String beforeTextSupportPeriod;
  public int commits;
  public int bugfixCommits;
  final public Map<String, Double> occupancies;
  final public Map<String, Double> deltaCFPFs;
  final private SortedSet<String> files;
  final private SortedSet<String> bugfixFiles;
  final private SortedSet<String> authors;
  final private SortedSet<String> bugfixAuthors;

  public Pattern(final String beforeText, final String afterText) {
    super(beforeText, afterText);
    this.beforeTextHashs = StringUtility.split(beforeText)
        .stream()
        .map(text -> Statement.getMD5(text))
        .collect(Collectors.toList());
    this.afterTextHashs = StringUtility.split(afterText)
        .stream()
        .map(text -> Statement.getMD5(text))
        .collect(Collectors.toList());
    this.periods = new ArrayList<>();
    this.ids = new ArrayList<>();
    this.dates = new ArrayList<>();
    this.mergedID = 0;
    this.findbugsSupport = 0;
    this.support = 0;
    this.bugfixSupport = 0;
    this.beforeTextSupport = 0;
    this.beforeTextSupportPeriod = null;
    this.commits = 0;
    this.bugfixCommits = 0;
    this.occupancies = new HashMap<>();
    this.deltaCFPFs = new HashMap<>();
    this.files = new TreeSet<>();
    this.bugfixFiles = new TreeSet<>();
    this.authors = new TreeSet<>();
    this.bugfixAuthors = new TreeSet<>();
  }

  public void addPeriod(final String period) {
    this.periods.add(period);
  }

  public List<String> getPeriods() {
    return new ArrayList<String>(this.periods);
  }

  public void addID(final String id) {
    this.ids.add(id);
  }

  public String getIDsText() {
    return nh3.ammonia.StringUtility.concatinate(this.ids);
  }

  public List<String> getIDs() {
    return new ArrayList<String>(this.ids);
  }

  public void addFiles(final String fileText) {
    this.files.addAll(StringUtility.split(fileText));
  }

  public void addFiles(final Collection<String> files) {
    this.files.addAll(files);
  }

  public SortedSet<String> getFiles() {
    return new TreeSet<String>(this.files);
  }

  public void addBugfixFiles(final String fileText) {
    this.bugfixFiles.addAll(StringUtility.split(fileText));
  }

  public void addBugfixFiles(final Collection<String> files) {
    this.bugfixFiles.addAll(files);
  }

  public SortedSet<String> getBugfixFiles() {
    return new TreeSet<String>(this.bugfixFiles);
  }

  public void addAuthors(final String authorText) {
    this.authors.addAll(StringUtility.split(authorText));
  }

  public void addAuthors(final Collection<String> authors) {
    this.authors.addAll(authors);
  }

  public SortedSet<String> getAuthors() {
    return new TreeSet<>(this.authors);
  }

  public void addBugfixAuthors(final String authorText) {
    this.bugfixAuthors.addAll(StringUtility.split(authorText));
  }

  public void addBugfixAuthors(final Collection<String> authors) {
    this.bugfixAuthors.addAll(authors);
  }

  public SortedSet<String> getBugfixAuthors() {
    return new TreeSet<>(this.bugfixAuthors);
  }

  public void addDate(final String date) {
    this.dates.add(date);
  }

  public String getFirstDate() {
    Collections.sort(this.dates);
    return this.dates.get(0);
  }

  public String getLastDate() {
    Collections.sort(this.dates);
    return this.dates.get(this.dates.size() - 1);
  }

  public void addOccupancy(final String period, final Double occupancy) {
    this.occupancies.put(period, occupancy);
  }

  public Double getMaxOccuapncy() {
    double max = 0d;
    for (final Double occupancy : this.occupancies.values()) {
      if (max < occupancy) {
        max = occupancy;
      }
    }
    return max;
  }

  public String getOccupanciesText() {
    final StringBuilder text = new StringBuilder();
    for (final Entry<String, Double> entry : this.occupancies.entrySet()) {
      text.append(entry.getKey());
      text.append(": ");
      text.append(entry.getValue());
      text.append(System.lineSeparator());
    }
    return text.toString();
  }

  public void addDeltaCFPF(final String period, final Double deltaCFPF) {
    this.deltaCFPFs.put(period, deltaCFPF);
  }

  public Double getDeltaCFPF(final int allBugfixCommits, final int allNonbugfixCommits) {
    final double pf = (double) this.support / (double) this.beforeTextSupport;
    final double cf1 = (double) this.bugfixCommits / (double) allBugfixCommits;
    final double cf2 = (double) (this.commits - this.bugfixCommits) / (double) allNonbugfixCommits;
    final double pfcf = pf * (cf1 - cf2);
    return pfcf;
  }

  public String getDeltaCFPFsText() {
    final StringBuilder text = new StringBuilder();
    for (final Entry<String, Double> entry : this.deltaCFPFs.entrySet()) {
      text.append(entry.getKey());
      text.append(": ");
      text.append(entry.getValue());
      text.append(System.lineSeparator());
    }
    return text.toString();
  }

  @Override
  public int compareTo(final Pattern target) {
    return Integer.compare(this.mergedID, target.mergedID);
  }
}
