#!/usr/bin/env python
import os, os.path
import nltk
from nltk.corpus import senseval

# get the senseval 3 data from system folder
path = os.path.relpath('nltk_data')
nltk.data.path[0]=path

items = senseval.fileids()

# the feature extractor code is below here -----------------------------------
vectorSize = 10     # number of words before and after to look for
cooccur_vect = {}   # store the feature vector keys in here

# I copied pasted the english stopwords corpus from the nltk package here
# for convenience; however, there are also corpus specific stopwords here too
stopwords = ['i', 'me', 'my', 'myself', 'we', 'our', 'ours', 'ourselves',\
             'you', 'your', 'yours', 'yourself', 'yourselves', 'he', 'him',\
             'his', 'himself', 'she', 'her', 'hers', 'herself', 'it', 'its',\
             'itself', 'they', 'them', 'their', 'theirs', 'themselves',\
             'what', 'which', 'who', 'whom', 'this', 'that', 'these', 'those',\
             'am', 'is', 'are', 'was', 'were', 'be', 'been', 'being', 'have',\
             'has', 'had', 'having', 'do', 'does', 'did', 'doing', 'a', 'an',\
             'the', 'and', 'but', 'if', 'or', 'because', 'as', 'until',\
             'while', 'of', 'at', 'by', 'for', 'with', 'about', 'against',\
             'between', 'into', 'through', 'during', 'before', 'after',\
             'above', 'below', 'to', 'from', 'up', 'down', 'in', 'out', 'on',\
             'off', 'over', 'under', 'again', 'further', 'then', 'once',\
             'here', 'there', 'when', 'where', 'why', 'how', 'all', 'any',\
             'both', 'each', 'few', 'more', 'most', 'other', 'some', 'such',\
             'no', 'nor', 'not', 'only', 'own', 'same', 'so', 'than', 'too',\
             'very', 's', 't', 'can', 'will', 'just', 'don', 'should', 'now',\
             
             '.', ',', '(', ')', '{', '}', '[', ']', '-', '?', '!', "n't", "'",\
             'r1', 'r2', 'r3', 'r4', 'r5', 'r6', 'r7', 'fig.', 'ref', '20', 'b1',\
             "'ve", "'d", "'s", ":", ";", 'one', 'two', 'may', 'also', 'many', '%',\
             'would', 'could']

# common words feature extractor
def common_words(item, pos, context, dictionary):
    words = context[:pos] + context[pos+1:]
    words = [x.lower() for x in words]

    for x in stopwords:
        words = filter(lambda w : w != x, words)

    word_counts = {}
    for word in words:
        if word in word_counts:
            word_counts[word] += 1
        else:
            word_counts[word] = 1

    words = sorted(word_counts, key = word_counts.get, reverse = True)
    words = tuple(words[:vectorSize])
    # a dictionary key should be "cooccurrence%word" and
    # value should be a tuple of the most common words
    dictionary[("%s*%s%d" % ("cooccurrence",item,pos))] = words
    return dictionary
