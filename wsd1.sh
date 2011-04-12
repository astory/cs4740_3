#!/bin/bash
batch=batch$1
key=EnglishLS.test/EnglishLS.test.key
mkdir experiment/$batch
bash scripts/make_calls-smp1.sh $key $batch> experiment/$batch/calls1.sh
cat experiment/$batch/calls1.sh|bash scripts/calls2csv.sh > experiment/$batch/calls1.csv
bash experiment/$batch/calls1.sh
bash scripts/score_calls1.sh experiment/$batch $key
