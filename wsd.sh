#!/bin/bash
batch=batch$1
bash scripts/make_calls.sh > experiment/$batch/calls.sh
cat experiment/$batch/calls.sh|bash scripts/calls2csv.sh > experiment/$batch/calls.csv
bash experiment/$batch/calls.sh
bash scripts/score_calls.sh experiment/$batch EnglishLS.test/EnglishLS.test.key
