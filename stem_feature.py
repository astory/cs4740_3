#!/usr/bin/env python
import nltk
from nltk.stem import *
from nltk.stem.porter import *
stemmer = PorterStemmer()
a = []
def stemCorpus(wordList):
  for x in wordList:
    a.append((x,stemmer.stem(x)))
  print(a)
