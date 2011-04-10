#!/usr/bin/env python
import os, os.path
import nltk
import pairing
from nltk.corpus import senseval
from optparse import OptionParser

CUTOFF_PROB = .5
BOOTSTRAP_CUTOFF_PROB = .8
# NOTE: Do not bootstrap (i.e., reps = 0) unless probabilities are on!
BOOTSTRAP_REPS = 5

USE_PROBS = True

CLASSIFIER=nltk.NaiveBayesClassifier
#CLASSIFIER=nltk.DecisionTreeClassifier #does not provide a probability measure
#CLASSIFIER=nltk.MaxentClassifier #much slower, prints lots of crap

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

def classify(train,test):
	classifier = CLASSIFIER.train(train)
	rawSenseList = classifier.batch_classify(test)
	probDistList = classifier.batch_prob_classify(test) if USE_PROBS\
		else [-1 for x in rawSenseList] #just a placeholder, should not be read
	return\
		[dict(sense=sense,prob=prob.prob(sense) if USE_PROBS else 1)\
		for sense,prob in zip(rawSenseList, probDistList)]

def bootstrap(train, test, classified):
	"""Bootstraps the classified test data onto the training
	train: list of (feature_dict, sense) pairs
	test:  list of (feature_dict)
	classified: list of (sense,prob) pairs matching up with test

	returns: a list of (feature_dict, sense) pairs containing all of train, with
	possibly more appended to it
	"""
	for result,test_inst in zip(classified, test):
		if result['prob'] > BOOTSTRAP_CUTOFF_PROB\
				and (test_inst, result['sense']) not in train:
			train.append((test_inst, result['sense']))
	return train

def batch_classify(items, tests):
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

		# TODO(astory): make dynamic?
		for i in range(BOOTSTRAP_REPS):
			classified = classify(train,test)
			train = bootstrap(train, test, classified)

		senses.extend(classify(train,test))
	return senses

if __name__ == '__main__':
	# command line options
	parser = OptionParser()
	parser.add_option("-i", "--fin", dest="fin",
					  help="Name of file containing test data")
	parser.add_option("-o", "--fout", dest="fout",
					  help="Name of file to output sense tags")
	parser.add_option("-d", "--dir", dest="dir",
					  help="Directory to look for nltk data")
	parser.add_option("-c", "--classifier", dest="classifier",
					  help="Choose which classifier to use")
	parser.add_option("-p", "--use_probs", dest="use_probs",
					  help="Enable probabilities rather than 0, 1 decisions")
	parser.add_option("-u", "--cutoff_prob", dest="cutoff_prob",
					  help="Choose decision probability cutoff")
	parser.add_option("-b", "--bootstrap", dest="bootstrap",
					  help="Enable bootstrapping")

	# feature extractor options
	parser.add_option("-l", "--colocation", dest="colocation",
					  help="Enable colocation feature extractor")
	parser.add_option("-r", "--cooccurrence", dest="cooccurrence",
					  help="Enable cooccurrence feature extractor")
	parser.add_option("-t", "--stemming", dest="stemming",
					  help="Enable stemming feature extractor")
	parser.add_option("-s", "--sentence_len", dest="sentence_len",
					  help="Enable sentence length feature extractor")
	parser.add_option("-e", "--pos", dest="pos",
					  help="Enable pos feature extractor")

	(options, args) = parser.parse_args()

	if options.dir is None:
			path = os.path.relpath('nltk_data')
	else:
			path = os.path.relpath(options.dir)
	nltk.data.path[0]=path

	items = senseval.fileids()
	tests = pairing.parse_file("EnglishLS.test/EnglishLS.test")
	senses = batch_classify(items, tests)

	f = open('answers.txt')
	l = []
	for line in f:
	  l.append(line)
	for x in range(len(senses)):
	  print(l[x].rstrip().rstrip('\n') + " " +\
		(senses[x]['sense'] if senses[x]['prob'] > CUTOFF_PROB else 'U'))
	f.close()
