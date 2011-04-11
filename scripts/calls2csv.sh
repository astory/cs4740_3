#!/bin/bash
#Run the calls through this
#./scripts/make_calls.sh |
sed -e 's+> experiment/trainingSystem/tmp+,+' -e 's/-\([nm]\)  /-\1 -b 0 /' -e 's/-\([re]\)/-\1 1/g'| sed -e 's/   / -r 0 -e 0/'| sed -e 's/  -e/ -r 0 -e/' -e 's/-r 1  /-r 1 -e 0/'
