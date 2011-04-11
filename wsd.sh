#!/bin/bash
#Run this like so
#wsd.sh [test data key] 
key=$1
tmp=tmp-`date '+%s'`

mkdir experiment/trainingSystem

#Edit the nested for loops to select a series of feature combinations
run=1
for classifier in memm bayes tree; do
  for bootstrap in off on; do #Fix how a lack of bootstrapping for trees doesn't work well here
    for colocation in off on; do
      for coocurrence in off on; do
        for unstemmed in off on; do
          for dependencyParsing in off on; do
            for featureX in off; do
              for featureY in off; do

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
    done
  done
done
