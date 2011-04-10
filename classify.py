#!/usr/bin/env python
import os, os.path
import nltk
import pairing
from nltk.corpus import senseval

path = os.path.relpath('nltk_data')
nltk.data.path[0]=path

items = senseval.fileids()

items = items[:1]

tests = pairing.parse_file("EnglishLS.test/EnglishLS.test")


for item in items:
	train=[]
	length = len(senseval.instances(item))
	for instance in senseval.instances(item):
		pos = instance.position
		context = instance.context
		senses = instance.senses

		d={}
		d['prev_word']=context[pos-1]
		d['actual_word']=context[pos]
		d['next_word']=context[pos+1]
		for sense in senses:
			pair = (d,sense)
			train.append(pair)
			(feature_set, label) = pair

	test=[]
	lexitem = ".".join(item.split(".")[0:2])
	for instance in tests[lexitem]:
		context = instance['context']
		pos = instance['position']

		d={}
		d['prev_word']=context[pos-1]
		d['actual_word']=context[pos]
		d['next_word']=context[pos+1]
		test.append(d)

#train2 = [
#    (dict(a=1,b=1,c="tr"), 'y'),
#   (dict(a=1,b=1,c="l"), 'x'),
#    (dict(a=1,b=1,c="m"), 'y'),
#    (dict(a=0,b=1,c="try"), 'x'),
#    (dict(a=0,b=1,c="tr"), 'y'),
#    (dict(a=0,b=0,c="catch"), 'y'),
#    (dict(a=0,b=1,c="fo"), 'x'),
#    (dict(a=0,b=0,c="ll"), 'x'),
#    (dict(a=0,b=1,c="87"), 'y'),
#    ]
#test = [
#    (dict(a=1,b=0,c="r")), # unseen
#    (dict(a=1,b=0,c="e")), # unseen
#    (dict(a=0,b=1,c="d")), # seen 3 times, labels=y,y,x
#    (dict(a=0,b=1,c="d")), # seen 1 time, label=x
#    ]

classifier = nltk.NaiveBayesClassifier.train(train)
senseList = classifier.batch_classify(test)
result = zip(senseList, [x['id_num'] for x in tests[lexitem]])
print result

#file writing stuff.  Will not work in the initial implementation.
#requires all of words to have a sense
f = open('answers.txt')
out = open('responses.txt', 'w')
l = []
for line in f:
  l.append(line)
for x in range(len(senseList)):
  out.write(l[x].rstrip().rstrip('\n') + " " + senseList[x] + '\n')
f.close()
out.close()
print senseList
print classifier.show_most_informative_features()
