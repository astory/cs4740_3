#!/bin/bash
#This takes
# 1 the directory with the tmp files and
# 2 the key file

tmps=$1
key=$2
#run=`echo $tmp|sed s/^.*tmp//`

for tmp in $tmps/tmp[0-9]*; do
	#Convert to a nice format
	./scoring/scorer2 $tmp $key  -v|
	sed -n -e 's/^score for //p'|
	sed -e 's/: /,/' -e 's/^.*_/"/' -e 's/.bnc./.bnc","/' > $tmp.csv
done
