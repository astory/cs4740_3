#!/usr/bin/env python
import os, os.path
import nltk
import pairing
from nltk.corpus import senseval

path = os.path.relpath('nltk_data')
nltk.data.path[0]=path

def assign_features(instance):
	context = instance['context']
	pos = instance['position']
	d={}
	d['prev_word']=context[pos-1]
	d['actual_word']=context[pos]
	d['next_word']=context[pos+1]
	return d

def build_train(instances):
	"""Builds training instances from instances tagged with senses"""
	train=[]
	for instance in instances:
		d = assign_features(instance)
		instance_senses = instance['senses']
		for sense in instance_senses:
			pair = (d,sense)
			train.append(pair)
	return train

def build_test(instances):
	"""Builds test instances from instances tagged with or without senses"""
	test=[]
	for instance in instances:
		d = assign_features(instance)
		test.append(d)
	return test

items = senseval.fileids()

tests = pairing.parse_file("EnglishLS.test/EnglishLS.test")

senses = []
for item in items:
	lexitem = ".".join(item.split(".")[0:2])
	trains=\
		[dict(context=instance.context,\
			position=instance.position,\
			senses=instance.senses)\
		for instance in senseval.instances(item)]
	train=build_train(trains)
	test=build_test(tests[lexitem])

	classifier = nltk.NaiveBayesClassifier.train(train)
	senseList = classifier.batch_classify(test)
	senses.extend(senseList)
	result = zip(senseList, [x['id_num'] for x in tests[lexitem]])

#file writing stuff.  Will not work in the initial implementation.
#requires all of words to have a sense
f = open('answers.txt')
l = []
for line in f:
  l.append(line)
for x in range(len(senses)):
  print(l[x].rstrip().rstrip('\n') + " " + senses[x])
f.close()
