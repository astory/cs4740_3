#!/bin/bash
#Run this like so
#wsd.sh [test data key] 
key=$1

#Edit the nested for loops to select a series of feature combinations
run=1
for classifier in -n -t; do
  for bootstrap in '' '-b 4' '-b 16' '-b 64'; do #Fix how a lack of bootstrapping for trees doesn't work well here
    for colocation in '-l 0' '-l 1' '-l 2' '-l 3'; do # -l NUM is the colocation distance, 0 is "don't use it"
      for coocurrence in '' -r; do # -r enables cooccurrence usae
        for base_word in '' -e; do # -e enables base word usage
#Run the actual system
echo "./classify.py $classifier $bootstrap $colocation $coocurrence $base_word -i EnglishLS.train/EnglishLS.subtest > experiment/trainingSystem/tmp$run"|
#Remove runs with bootstrapping and trees
grep -v '\-t \-b' 
run=$((run+1))
        done
      done
    done
  done
done
