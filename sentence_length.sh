#!/bin/bash
instance="$1"
#Extract the sentence
function sentence {
cat "$instance"|
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
sentence|sed -n 's/^|ID://p'|tr '\n' '|'
}

#Print a pipe-delimited row with id number and sentence length in characters
id
#Count the number of characters
sentence|tail -n 1|wc -c

#Print a row with id and sentence 
#sentence|sed 's/|ID://'|tr '\n' '|'
