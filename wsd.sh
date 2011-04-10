#!/bin/bash
#Run this like so
#wsd.sh [test data key] 
key=$1
tmp=tmp-`date '+%s'`

for flags in no te ce ne; do
	echo $flags
	./classify.py $flags > $tmp
	#Convert to a nice format
	./scoring/scorer2 $tmp $key  -v|sed -n -e 's/^score for //p'|sed -e 's/: /,/' -e 's/^.*_/"/' -e 's/.bnc./.bnc","/' 
	rm $tmp
done
