package nh3.ammonia;

public class BugFixDBMaker {

  public static void main(final String[] args) {
    BugFixRevisionsMaker.main(args);
    BugFixChangesMaker.main(args);
    BugFixPatternsMaker.main(args);
  }
}
