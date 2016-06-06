package yoshikihigo.fbparser;

public class BugFixDBMaker2 {

	public static void main(final String[] args) {
		BugFixRevisionsMaker.main(args);
		BugFixChangesMaker.main(args);
		BugFixPatternsMaker.main(args);
	}
}
