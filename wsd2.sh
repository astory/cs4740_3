#!/bin/bash
batch=batch$1
key=EnglishLS.test/EnglishLS.test.key
mkdir experiment/$batch
bash scripts/make_calls-smp2.sh $key $batch> experiment/$batch/calls2.sh
cat experiment/$batch/calls2.sh|bash scripts/calls2csv.sh > experiment/$batch/calls2.csv
bash experiment/$batch/calls2.sh
bash scripts/score_calls2.sh experiment/$batch $key
