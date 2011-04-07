import os, os.path
path = os.path.expanduser('~/nltk_data')
if not os.path.exists(path):
	os.mkdir(path)
import nltk.data
from nltk.corpus import senseval
#Remove the Senseval-2 data
for word in glob.glob('/home/tlevine/nltk_data/corpora/senseval/*.pos'):
	os.remove(word)
#Import the Senseval-3 data

#Stuff
#senseval.fileids()
