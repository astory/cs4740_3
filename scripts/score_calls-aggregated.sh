#!/bin/bash
#This takes
# 1 the directory with the tmp files and
# 2 the key file

tmps=$1
key=$2
#run=`echo $tmp|sed s/^.*tmp//`
echo run_id,grain,precision,recall,attempted
for tmp in $tmps/tmp*[0-9]; do
for grain in coarse mixed; do
	#Remove un-answered
	grep -v U$ $tmp > $tmp-attempted
	#Convert to a nice format
	./scoring/scorer2 $tmp-attempted EnglishLS.test/EnglishLS.test.key EnglishLS.test/EnglishLS.sensemap -g "$grain"|
	sed -e 's+^.*score for.*/tmp+"tmp+' -e 's+ using key.*$++' -e 's+(.*)++' -e 's+^.*:++' -e 's/[ %]//g' -e '/^$/d ' |tr '\n' ','|sed -e 's/,$//' -e "s/,/,$grain,/"
	echo ''
done
grain=fine
	#Fine-grained
	./scoring/scorer2 $tmp-attempted EnglishLS.test/EnglishLS.test.key|
	sed -e 's+^.*score for.*/tmp+"tmp+' -e 's+ using key.*$++' -e 's+(.*)++' -e 's+^.*:++' -e 's/[ %]//g' -e '/^$/d ' |tr '\n' ','|sed -e 's/,$//' -e "s/,/,$grain,/"
	echo ''
done|sed s/-attempted//
