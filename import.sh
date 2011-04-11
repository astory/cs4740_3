#!/bin/bash
#This takes the ??.train file as the sole argument
train="$1"

#Convert to Senseval-2--like format
mkdir senseval2_format
mkdir senseval2_format-subtest_set
mkdir senseval2_format-subtrain_set
for instance in `grep -l ^\<instance "$train".split/*`
do
word=`sed -n 's/<instance id="\([^.]*\.[a-z]\)\..*$/\1/p' "$instance"`
cat "$instance" >> senseval2_format/$word.pos
done
