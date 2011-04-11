tmp=$1
key=$2
run=`echo $tmp|sed s/^.*tmp//`

#Convert to a nice format
./scoring/scorer2 $tmp $key  -v|sed -n -e 's/^score for //p'|
sed -e 's/: /,/' -e 's/^.*_/"/' -e 's/.bnc./.bnc","/' > experiment/trainingSystem/trainingSystem_$run.csv
