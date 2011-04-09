#!/bin/bash
#Run this like so
#score2csv predictions key
./scorer2 $1 $2  -v|sed -n -e 's/^score for //p'|sed 's/: /,/'
