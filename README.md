How the system works from end to end
=====================

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


Feature extraction
----------------------------
Run something on the .pos files


Format for scorer
------------------------------


Output from scorer
---------------------------
For a particular system combination with identification number 04,
get the score from this system and save it in a nice format.

score2csv [predictions file 04] [keys file] > score04.csv



Identification of optimal feature combination
----------------------------------
I need to figure out how to run R scripts from the command line.
This will run stepwise regression and identify a few possibilities.
We may consider saving some of the training set as an intermediary
test set so that we can identify a few optimal systems and try
them on the test set.
