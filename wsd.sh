#!/bin/bash
#Run this like so
#wsd.sh [test data key] 
key=$1
tmp=tmp-`date '+%s'`

# NOTE:  probability cutoff is also assignable

mkdir experiment/trainingSystem

#Edit the nested for loops to select a series of feature combinations
run=1
for classifier in -m -n -t; do
  for bootstrap in '' '-b 10' '-b 100' ; do #Fix how a lack of bootstrapping for trees doesn't work well here
    for colocation in '-l 0' '-l 1' '-l 2'; do # -l NUM is the colocation distance, 0 is "don't use it"
      for coocurrence in '' -r; do # -r enables cooccurrence usae
        for base_word in '' -e; do # -e enables base word usage
#Run the actual system
./classify.py $classifier $bootstrap $colocation $coocurrence $unstemmed $dependecyParsing $featureX $featureY > $tmp
#Convert to a nice format
./scoring/scorer2 $tmp $key  -v|sed -n -e 's/^score for //p'|
sed -e 's/: /,/' -e 's/^.*_/"/' -e 's/.bnc./.bnc","/' > experiment/trainingSystem/trainingSystem_$run.csv
rm $tmp
run=$((run+1))
        done
      done
    done
  done
done
