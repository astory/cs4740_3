How the system works from end to end
=====================

The system is built in python and bash.
Optimal systems are identified with R.
Some dependencies that you might not have.
* nltk (Python library)
* csplit
* beautifulsoup (python xml parser)

Converting to pseudo--Senseval-2 format
-------------------------

Read from .train to .pos files

     cd EnglishLS.test/
     ./import.sh EnglishLS.test

This creates two directories
* EnglishLS.test.split
* senseval2_format

The former contains one file per instance within the corpus, and the latter
contains one file per word type of interest, with one of these files containing
each of the instances for the corresponding word.

Because we automated all of the tests we wrote the output to files as we tested
it.  The results are included in the folder experiment/allruns.  The calls.csv
file shows the features that were used in each test and the output for each
test.  The output was formatted in a way that the scorer would accept.

Running

First, there's some weird system state that you need to manipulate to get
maltparse to work.  Run `./parser.py` to set this state.

Then, run `./classify.py`.  It will read in the training and test files; be sure
to read it's --help or -h text for flags - running it without flags will fail.

If you want to use the dependency parser, you will have to install and configure
(possibly with edits to files) MaltParse yourself - it was too large to fit in
the file size requirement.  Our setup included a MaltParse directory at root,
containing engmalt.linear.mco, the training file, and a directory malt-1.2, with
the java binaries in it.  If you mimic that setup, it should work without
modification.

The example outputs can be found at experiment/allruns.  The file calls.csv describes the parameters used to generate each file.  The tmp# files are our actual output.
