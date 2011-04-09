#!/usr/bin/env python
import nltk
from nltk import pos_tag, word_tokenize
def pos_tagger(context,position,dictionary):
  dictionary["pos"] = pos_tag(word_tokenize(context[position]))
  return dictionary
