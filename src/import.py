#!/usr/bin/env python
#This sets up Python to load Senseval 3 data for nltk
import os, os.path,glob
path = os.path.expanduser('~/nltk_data')
if not os.path.exists(path):
	os.mkdir(path)
import nltk.data
from nltk.corpus import senseval
#Remove the Senseval-2 data
for word in glob.glob('~/nltk_data/corpora/senseval/*.pos'):
	os.remove(word)

