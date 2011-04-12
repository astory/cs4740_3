#!/bin/bash
#Run the calls through this
#./scripts/make_calls.sh |

#Header
echo 'classifier,bootstrap,colocation,cooccurrence,base_word,run_id'
#Fill in empty flags
sed -e 's+> experiment/trainingSystem/tmp+,+' -e 's/-\([nmt]\)  /-\1 -b 0 /' -e 's/-\([re]\)/-\1 1/g'| sed -e 's/   / -r 0 -e 0/'| sed -e 's/  -e/ -r 0 -e/' -e 's/-r 1  /-r 1 -e 0/'|
#Format as csv
sed -e 's+^./classify.py -++' -e 's/ -[ablrep]* /,/g' -e 's/ //g'
