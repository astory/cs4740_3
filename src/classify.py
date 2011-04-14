#!/usr/bin/env python
import os, os.path, sys
import nltk
import colocation
import cooccurrence
import pairing
import parser as dep_parser
from nltk.corpus import senseval
from optparse import OptionParser

CUTOFF_PROB = .5
BOOTSTRAP_CUTOFF_PROB = .8
# NOTE: Do not bootstrap (i.e., reps = 0) unless probabilities are on!
BOOTSTRAP_REPS = 5

USE_PROBS = False
COLOCATION_WINDOW = 0
USE_COOCCURRENCE = False
USE_BASE_WORD = False
USE_PARSING = False

CLASSIFIER=nltk.NaiveBayesClassifier
#CLASSIFIER=nltk.DecisionTreeClassifier #does not provide a probability measure
#CLASSIFIER=nltk.MaxentClassifier #much slower, prints lots of crap

def assign_features(item, instance):
	print >> sys.stderr, "classifying an instance"
	context = instance['context']
	pos = instance['position']
	d={}
	if USE_BASE_WORD:
		d['actual_word']=context[pos]
	d = colocation.colocation(COLOCATION_WINDOW,pos, context, d)
	if USE_COOCCURRENCE:
		d = cooccurrence.cooccurrence(item, pos, context, d)
	if USE_PARSING:
		try:
			d = dep_parser.parse(pos, context, d)
		except:
			pass
	return d

def build_train(item, instances):
	"""Builds training instances from instances tagged with senses"""
	train=[]
	for instance in instances:
		d = assign_features(item, instance)
		instance_senses = instance['senses']
		for sense in instance_senses:
			pair = (d,sense)
			train.append(pair)
	return train

def build_test(item, instances):
	"""Builds test instances from instances tagged with or without senses"""
	test=[]
	for instance in instances:
		d = assign_features(item, instance)
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
		print >> sys.stderr, "classifying %s" % item
		lexitem = ".".join(item.split(".")[0:2])
		trains=\
			[dict(context=instance.context,\
				position=instance.position,\
				senses=instance.senses)\
			for instance in senseval.instances(item)]
		train=build_train(item, trains)
		test=build_test(item, tests[lexitem])

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
	parser.add_option("-d", "--dir", dest="dir", default="nltk_data",
					  help="Directory to look for nltk data")
	parser.add_option("-n", "--naive", action="store_const",
					  const=nltk.NaiveBayesClassifier, dest="classifier",
					  help="Use the naive Bayes classifier")
	parser.add_option("-t", "--tree", action="store_const",
					  const=nltk.DecisionTreeClassifier, dest="classifier",
					  help="Use the decision tree classifier, implies no\
					  probability measurements")
#	parser.add_option("-m", "--maxentropy", action="store_const",
#					  const=nltk.MaxentClassifier, dest="classifier", help="Use\
#					  the maximum entropy classifier")
	parser.add_option("-p", "--use_probs", dest="use_probs", default=False,
					  action="store_true", help="Enable probability based\
					  confidence measurements")
	parser.add_option("-c", "--cutoff_prob", dest="cutoff_prob", default=.5,
					  action="store", help="Unknown probability cutoff")
	parser.add_option("-b", "--bootstrap", dest="bootstrap", default=0,
					  type="int", action="store",
					  help="Number of bootstrapping iterations, defaults to 0,\
					  a value > 0 implies -p, and precludes the use of -t")
	parser.add_option("-o", "--bootstrap_cutoff", dest="bootstrap_cutoff",
					  default=.8, action="store", help="Bootstrapping\
					  probability cutoff")

	# feature extractor options
	parser.add_option("-l", "--colocation", dest="colocation",default=0,
					  type="int", action="store",
					  help="Colocation window size, default=0")
	parser.add_option("-r", "--cooccurrence", dest="cooccurrence", default=False,
					  action="store_true",
					  help="Enable cooccurrence feature extractor")
	parser.add_option("-e", "--base", dest="base_word", default=False,
					  action="store_true",
					  help="Enable base word feature extractor")
	parser.add_option("-s", "--sentence_len", dest="sentence_len", default=False,
					  action="store_true",
					  help="Enable sentence length feature extractor")
	parser.add_option("-a", "--parse", dest="parse", default=False,
					  action="store_true",
					  help="Enable dependency parsing")

	(options, args) = parser.parse_args()

	nltk.data.path.append(os.path.relpath(options.dir))

	USE_PROBS = options.use_probs
	COLOCATION_WINDOW = options.colocation
	USE_COOCCURRENCE = options.cooccurrence
	USE_BASE_WORD = options.base_word
	USE_PARSING = options.parse

	CLASSIFIER=options.classifier
	CUTOFF_PROB=options.cutoff_prob
	BOOTSTRAP_CUTOFF_PROB=options.bootstrap_cutoff
	BOOTSTRAP_REPS=options.bootstrap
	if BOOTSTRAP_REPS > 0:
		USE_PROBS = True
	if CLASSIFIER == nltk.DecisionTreeClassifier and USE_PROBS:
		raise Exception("Decision tree classifier does not support probability\
		measures")
	if CLASSIFIER == None:
		raise Exception("No classifier specified, use -n -t or -m")

	parses = open("parses.pickle", 'r')
	dep_parser.all_of_them = dep_parser.load(parses)
	parses.close()

	print >> sys.stderr, "Gathering Items"
	items = senseval.fileids()
	print >> sys.stderr, "Gathering Tests"
	tests = pairing.parse_file("EnglishLS.test/EnglishLS.test")

	print >> sys.stderr, "Classifying"
	senses = batch_classify(items, tests)

#	parses = open("parses.pickle", 'w')
#	dep_parser.pickle(parses)
#	parses.close()


	f = open('answers.txt')
	l = []
	for line in f:
	  l.append(line)
	for x in range(len(senses)):
	  print(l[x].rstrip().rstrip('\n') + " " +\
		(senses[x]['sense'] if senses[x]['prob'] > CUTOFF_PROB else 'U'))
	f.close()
