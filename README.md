How the system works from end to end
=====================

The system is built in python and bash.
Optimal systems are identified with R.
Some dependencies that you might not have.
* nltk (Python library)
* csplit



Converting to pseudo--Senseval-2 format
-------------------------

Read from .train to .pos files

     cd EnglishLS.test/
     ./import.sh EnglishLS.test

This creates two directories
* EnglishLS.test.split
* senseval2_format

The former contains one file per instance within the corpus,
and the latter contains one file per word type of interest,
with one of these files containing each of the instances for
the corresponding word.



Identification of optimal feature combination
----------------------------------
I need to figure out how to run R scripts from the command line.
This will run stepwise regression and identify a few possibilities.
We may consider saving some of the training set as an intermediary
test set so that we can identify a few optimal systems and try
them on the test set.
