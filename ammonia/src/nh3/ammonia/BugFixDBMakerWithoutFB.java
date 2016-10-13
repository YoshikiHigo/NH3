package nh3.ammonia;

public class BugFixDBMakerWithoutFB {

	public static void main(final String[] args) {
		BugFixRevisionsMaker.main(args);
		BugFixChangesMaker.main(args);
		BugFixPatternsMaker.main(args);
	}
}
