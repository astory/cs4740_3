#!/bin/bash
batch=batch$1
key=EnglishLS.test/EnglishLS.test.key
mkdir experiment/$batch
bash scripts/make_calls-smp4.sh $key $batch> experiment/$batch/calls4.sh
cat experiment/$batch/calls4.sh|bash scripts/calls2csv.sh > experiment/$batch/calls4.csv
bash experiment/$batch/calls4.sh
bash scripts/score_calls4.sh experiment/$batch $key
