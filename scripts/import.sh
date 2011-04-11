#!/bin/bash
#This takes the directory of split instances and the resulting directory as arguments
train="$1"
dir="$2"

#Convert to Senseval-2--like format
for instance in `grep -l ^\<instance "$train"/*`
do
word=`sed -n 's/<instance id="\([^.]*\.[a-z]\)\..*$/\1/p' "$instance"`
cat "$instance" >> $dir/$word.pos
done
