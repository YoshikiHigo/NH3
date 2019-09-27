### What's Ammonia

NH3(Ammonia) is a tool to help you find latent buggy code in your source code.
Ammonia collects changes that we conducted to fix bugs in the past from your git repository.
Then, Ammonia find latent buggy code in the target source code based on the collected changes.
At this moment, Ammonia is designed to work on only Java source code.


### Use Ammonia

You can download Ammonia's exedutable files and a sample target from the following link:
https://github.com/YoshikiHigo/NH3/releases/tag/v1.0


### Build Ammonia

To build Ammonia, you need another project MPAnalyzer, which can be accessed here:
https://github.com/YoshikiHigo/MPAnalyzer

Both Ammonia and MPAnalyzer are being developed with Eclipse and their repositories include Eclipse's setting files.
Neither NH3 nor MPAnalyzer uses build tools such as maven and gradle.
Consequently, we recommend to use Eclipse to build them.

After building the tools, we probably want to run the tools.
Sample commands to run Ammonia and MPAnalyzer are introduced in README.txt in the following package:
https://github.com/YoshikiHigo/NH3/releases/tag/v1.0
