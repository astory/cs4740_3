#!/bin/bash
train="$1"
mkdir "$train".split/ > /dev/null || echo "Output directory exists. Delete it."
cd "$train".split
csplit -n 5 -f "$train".instance -s ../"$train" '/^<instance/' '{*}'
cd ..
