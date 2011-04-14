#!/bin/bash
#This takes the ??.train file as the sole argument
startdir=`pwd`
train="$1"
mkdir "$train".split 
cd "$train".split
csplit -n 5 -f "$train".instance -s ../"$train" '/^<instance/' '{*}'

cd $startdir

