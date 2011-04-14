#!/bin/bash
#Run this in the root directory like so
#wsd.sh [test data key] 
key=$1
batch=$2

#Edit the nested for loops to select a series of feature combinations
run=3000
echo '#!/bin/bash'
for classifier in -pn -t; do
  for bootstrap in '' '-b 1' '-b 4' ; do #Fix how a lack of bootstrapping for trees doesn't work well here
    for colocation in '-l 0' '-l 1' '-l 2' '-l 3'; do # -l NUM is the colocation distance, 0 is "don't use it"
      for coocurrence in '' -r; do # -r enables cooccurrence usage
        for base_word in '' -e; do # -e enables base word usage
          for dependency in '' -a; do

#Run the actual system
#echo "./classify.py $classifier $bootstrap $colocation $coocurrence $base_word -i EnglishLS.train/EnglishLS.subtest > experiment/trainingSystem/tmp$run"|
echo "./classify.py $classifier $bootstrap $colocation $coocurrence $base_word $dependency > experiment/$batch/tmp$run"|
#Remove runs with bootstrapping and trees
grep -v '\-t \-b' 
run=$((run+1))

          done
        done
      done
    done
  done
done
