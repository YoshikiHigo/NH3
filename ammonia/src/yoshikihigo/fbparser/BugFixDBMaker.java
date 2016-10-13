package yoshikihigo.fbparser;

public class BugFixDBMaker {

	public static void main(final String[] args) {
		BugFixRevisionsMaker.main(args);
		BugFixChangesMaker.main(args);
		//BugFixChangesUpdater.main(args);
		BugFixPatternsMaker.main(args);
	}
}
