#!/bin/bash
#Run this like so
#wsd.sh [test data key] 
key=$1

#Edit the nested for loops to select a series of feature combinations
run=1
for classifier in -m -n -t; do
  for bootstrap in '' '-b 4' '-b 16' ; do #Fix how a lack of bootstrapping for trees doesn't work well here
    for colocation in '-l 0' '-l 1' '-l 2'; do # -l NUM is the colocation distance, 0 is "don't use it"
      for coocurrence in '' -r; do # -r enables cooccurrence usae
        for base_word in '' -e; do # -e enables base word usage
#Run the actual system
echo "./classify.py $classifier $bootstrap $colocation $coocurrence $base_word > experiment/trainingSystem/tmp$run" > trainingSystem_calls.sh
#Convert to a nice format
#./scoring/scorer2 $tmp $key  -v|sed -n -e 's/^score for //p'|
#sed -e 's/: /,/' -e 's/^.*_/"/' -e 's/.bnc./.bnc","/' > experiment/trainingSystem/trainingSystem_$run.csv
#Save the system combination
#echo "$run,./classify.py $classifier $bootstrap $colocation $coocurrence $base_word">experiment/trainingSystem/calls.csv

run=$((run+1))
        done
      done
    done
  done
done
