#!/bin/bash
#This takes
# 1 the directory with the tmp files and
# 2 the key file

tmps=$1
key=$2
#run=`echo $tmp|sed s/^.*tmp//`
echo run_id,precision,recall,attempted
for tmp in $tmps/tmp*[0-9]; do
	#Convert to a nice format
	./scoring/scorer2 $tmp EnglishLS.test/EnglishLS.test.key |sed -e 's+^.*score for.*/tmp+"tmp+' -e 's+ using key.*$++' -e 's+(.*)++' -e 's+^.*:++' -e 's/[ %]//g' -e '/^$/d ' |tr '\n' ','|sed 's/,$//'
	echo ''
done 
