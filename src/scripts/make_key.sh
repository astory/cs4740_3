#!/bin/bash
#Run this inside a directory with files of instances to make keys
#grep -h '^<answer' *|sed -e 's/<answer instance="\(.*\)\.bnc/\1 \1.bnc/' -e 's/"//' -e 's+/>++' -e 's/senseid="\([^"]*\)"/\1/g'

#Get the instances in order to make keys later
#grep -h '^<answer' *|sed 's/<answer instance="\([^"]*\)".*$/\1/'|sort -u

#Select a subset and make the key files
sort -R EnglishLS.train.key > tmp-random
sed -n '1,1500 p' tmp-random|sort > EnglishLS.subtest.key
sed    '1,1500 d' tmp-random|sort > EnglishLS.subtrain.key
rm tmp-random

#Make the senseval-3 format data
for subset in EnglishLS.subtrain EnglishLS.subtest;do
	rm $subset
	for i in `cat $subset.key |sed -e 's/^[^ ]* //' -e 's/ .*$//'`
		do cat `grep -l $i EnglishLS.train.split/*` >> $subset
	done
done

