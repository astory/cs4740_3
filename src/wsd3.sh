#!/bin/bash
batch=batch$1
key=EnglishLS.test/EnglishLS.test.key
mkdir experiment/$batch
bash scripts/make_calls-smp3.sh $key $batch> experiment/$batch/calls3.sh
cat experiment/$batch/calls3.sh|bash scripts/calls2csv.sh > experiment/$batch/calls3.csv
bash experiment/$batch/calls3.sh
bash scripts/score_calls3.sh experiment/$batch $key
