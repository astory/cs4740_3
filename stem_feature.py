#!/usr/bin/env python
import nltk
from nltk.stem import *
from nltk.stem.porter import *
stemmer = PorterStemmer()
a = []
def stemCorpus(context,position,dictionary):
  dictionary["stem"] = stemmer.stem(context[position])
  return dictionary
