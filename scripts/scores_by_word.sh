#!/bin/bash
system=$1
for word in nltk_data/corpora/senseval/*.pos
do
word=`echo $word|sed -e s+nltk_data/corpora/senseval/++ -e s+\.pos$++`
grep "^$word" $system|grep -v U$ > tmp-$word
for grain in coarse mixed; do
	./scoring/scorer2 tmp-$word EnglishLS.test/EnglishLS.test.key EnglishLS.test/EnglishLS.sensemap -g "$grain"|
        sed -e 's+^.*tmp-+"+' -e 's+ using key.*$++' -e 's+(.*)++' -e 's+^.*:++' -e 's/[ %]//g' -e '/^$/d ' |tr '\n' ','|sed -e 's/,$//' -e "s/,/,$grain,/"
        echo 
done
grain=fine
        ./scoring/scorer2 tmp-$word EnglishLS.test/EnglishLS.test.key |
        sed -e 's+^.*tmp-+"+' -e 's+ using key.*$++' -e 's+(.*)++' -e 's+^.*:++' -e 's/[ %]//g' -e '/^$/d ' |tr '\n' ','|sed -e 's/,$//' -e "s/,/,$grain,/"
        echo 
done
