#!/bin/bash
#Run this inside the EnglishLS.train directory to set things up for importing to python
#It could take the EnglishLS.train file within that directory as the sole argument
../import.py
train="$1"
mkdir "$train".split 
cd "$train".split
#csplit -n 5 -f "$train".instance -s ../"$train" '/^<instance/' '{*}'
cd ..

#Convert to Senseval-2--like format
mkdir senseval2_format
for instance in `grep -l ^\<instance "$train".split/*`
do
word=`sed -n 's/<instance id="\([^.]*\.[a-z]\)\..*$/\1/p' "$instance"`
cat "$instance" >> senseval2_format/$word.pos
done
