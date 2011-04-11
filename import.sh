#!/bin/bash
#This takes the ??.train file as the sole argument
startdir=`pwd`
cd `ls -d "$1" |sed 's+/.*$++'`

import.py
train="$1"
mkdir "$train".split 
cd "$train".split
csplit -n 5 -f "$train".instance -s ../"$train" '/^<instance/' '{*}'

cd $startdir

#Convert to Senseval-2--like format
mkdir senseval2_format
mkdir subtest_set
mkdir subtrain_set
for instance in `grep -l ^\<instance "$train".split/*`
do
word=`sed -n 's/<instance id="\([^.]*\.[a-z]\)\..*$/\1/p' "$instance"`
cat "$instance" >> senseval2_format/$word.pos
if [ $((($RANDOM+1)%4)) = 3 ]
	then cat "$instance" >> subtest_set/$word.pos
else
	cat "$instance" >> subtrain_set/$word.pos
fi
done
