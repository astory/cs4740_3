#!/bin/bash
#This extracts character and word counts from a series of files of one instance each
#Run this script like so
#sentence_length.sh <files>
#For example
#sentence_length.sh EnglishLS.train.instance0139*

#Extract the sentence
function sentence {
#This takes one argument--instance file
cat "$1"|
#Extract the id number
sed 's/<instance id="\([^"]*\)".*$/|ID<head>:\1/'|
#Separate by sentence
sed 's/ [?.!]  /\n/g'|
#Extract the relevant sentence and ID
grep '<head>'|
#Remove tags
sed 's/<[^<]*>//g'
}

#Print the id number
function id {
sentence "$1"|sed -n 's/^|ID://p'
}

function word {
#Print a pipe-delimited row with id number,
id "$1"|tr '\n' '|'
#sentence length in characters and
sentence "$1"|tail -n 1|wc -c|tr '\n' '|'
#sentence length in words
sentence "$1"|tail -n 1|wc -w
}

#Print a row with id and sentence 
#sentence|sed 's/|ID://'|tr '\n' '|'

echo 'occurrance|characterCount|wordCount'
while (( "$#" )); do
word "$1"
shift
done
