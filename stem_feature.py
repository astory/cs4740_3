#!/usr/bin/env python
import nltk
from nltk.stem import *
from nltk.stem.porter import *
stemmer = PorterStemmer()
a = []
def stemCorpus(inputList):
  for x in inputList:
    wordList = x.split("\t")
    word = wordList[0]
    a.append((x + "\t" + stemmer.stem(word)))
  return a
