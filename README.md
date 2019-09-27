## What's Ammonia

NH3(Ammonia) is a tool to help you find latent buggy code in your source code.
Ammonia collects changes that you conducted to fix bugs in the past from your git repository.
Then, Ammonia find latent buggy code in the target source code based on the collected changes.
At this moment, Ammonia is designed to work on only Java source code.


## Use Ammonia

You can download Ammonia's exedutable files and a sample target from the following link:
https://github.com/YoshikiHigo/NH3/releases/tag/v1.0


## Build Ammonia

To build Ammonia, you need another project MPAnalyzer, which can be accessed here:
https://github.com/YoshikiHigo/MPAnalyzer

Both Ammonia and MPAnalyzer are being developed with Eclipse and their repositories include Eclipse's setting files.
Neither NH3 nor MPAnalyzer use build tools such as maven or gradle.
Consequently, we recommend to use Eclipse to build them.
If you import the projects into your Eclipse workspace, the projects will be automatically built.


## Sample commands to run Ammonia

The followings are some sample commands to run Ammonia.
By using Ammonia, you can conduct manual analysis for latent buggy code by doing 1~5.


### 1. preparing a git repository on your local storage.

```sh
git clone git@github.com:apache/ant.git ant.git
```

In this example, we use Apache Ant as a target.


### 2. extracting code changes from each commit.

```
java -jar ChangeExtractor.jar -db ANT.db -soft ant -gitrepo ant.git -f
```

The main class of ChangeExtractor.jar is <https://github.com/YoshikiHigo/MPAnalyzer/blob/master/src/yoshikihigo/cpanalyzer/ChangeExtractor.java>.
This command extracts code changes from the target repository.
This command will takes about 3 minutes.


### 3. making change patterns.

```
java -jar ChangePatternMaker.jar -db ANT.db -f
```

The main class of ChangePatternMaker.jar is <https://github.com/YoshikiHigo/MPAnalyzer/blob/master/src/yoshikihigo/cpanalyzer/ChangePatternMaker.java>.
This command makes change patterns based on the extracted code changes.
This command will takes about 20 seconds.


### 4. adding bugfix information to change patterns.

```
java -jar BugFixDBMaker.jar -db ANT.db -bug bugIDs-2016-07-11.csv
```

The main class of BugFixDBMaker is <https://github.com/YoshikiHigo/NH3/blob/master/src/nh3/ammonia/BugFixDBMaker.java>.
This command adds bugfix information to each code change and change pattern.
You need to prepare a csv file including keywords of bug-fixing commits.
The sample css file (bugIDs-2016-07-11.csv) is included in the package:
https://github.com/YoshikiHigo/NH3/releases/tag/v1.0
Each line of the csv file means a bug ID that was corrected from Bugzilla's database(
https://bz.apache.org/bugzilla/). 
This command will takes 3 minutes.


### 5. finding latent buggy code.

```
java -jar Ammonia.jar -gitrepo ant.git -db ANT.db -gitcommit 1de4dfa58f198e1590294951183ff61210d48549 -lang Java
```

The main class of Ammonia.jar is <https://github.com/YoshikiHigo/NH3/blob/master/src/nh3/ammonia/gui/Ammonia.java>.
This command launches a GUI window for manual analysis.
This command will takes 10 seconds before the GUI window shows up.
The commit specified with "-gitcommit" is the target of bug identification.

=========================================

For 2. and 3., you can find a list of available options in the following URL.
https://github.com/YoshikiHigo/MPAnalyzer/blob/master/src/yoshikihigo/cpanalyzer/CPAConfig.java

For 4. and 5., you can find a list of available options in the following URL.
https://github.com/YoshikiHigo/NH3/blob/master/src/nh3/ammonia/FBParserConfig.java

=========================================

The source code of MPAnalyzer (the source code of 2. and 3.) is availeble at:
https://github.com/YoshikiHigo/MPAnalyzer

The source code of NH3 (the source code of 4. and 5.) is availeble at:
https://github.com/YoshikiHigo/NH3

=========================================

MPAnalyzer and NH3 use SQLite as a database manager.





