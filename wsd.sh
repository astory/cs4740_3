#!/bin/bash
batch=batch$1
key=EnglishLS.test/EnglishLS.test.key
mkdir experiment/$batch
bash scripts/make_calls.sh $key $batch> experiment/$batch/calls.sh
cat experiment/$batch/calls.sh|bash scripts/calls2csv.sh > experiment/$batch/calls.csv
bash experiment/$batch/calls.sh
bash scripts/score_calls.sh experiment/$batch $key
bash scripts/score_calls-aggregated experiment/$batch $key| sed 's/-attempted//' > experiment/$batch/scores.csv
