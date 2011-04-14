#!/bin/bash
#Run the calls through this
#./scripts/make_calls.sh |

#Header
echo 'classifier,bootstrap,colocation,cooccurrence,base_word,dependency_parsing,run_id'
#Fill in empty flags
sed -e '/#!/d' -e 's+> experiment/[^/]*/tmp+,+'|
sed -e 's/  / -z 0 /g'|
sed -e 's/  / -z 0 /g'|
sed -e 's/\(-[ablrepz]\) \([-,]\)/\1 1 \2/g'|
sed -e 's/\(-[ablrepz]\) \([-,]\)/\1 1 \2/g'|

#Format as csv
sed -e 's+^./classify.py -++' -e 's/-[ablrepz]*/,/g' -e 's/ //g'
