#!/usr/bin/env python
import os, os.path
import nltk
from nltk.corpus import senseval

# get the senseval 3 data from system folder
path = os.path.relpath('nltk_data')
nltk.data.path[0]=path

items = senseval.fileids()
print items

items = items[:1] # maybe change this later
windowSize = 4

# size of your feature vector for cooccurrence
vectorSize = 12

# I copied pasted the english stopwords corpus from the nltk package here
# for convenience (I also added in some punctuation)
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
             '.', ',', '(', ')', '{', '}', '[', ']', '-', '?', '!', "n't"]

def feature_keys(ind):
    """ run this to get a global instance of your feature keyset"""

    # check to see if global variable cooccur_vect exists
    try:
        cooccur_vect
    except NameError:
        global cooccur_vect
        cooccur_vect = {}

    # check to see if the feature vector we are looking for exists
    try:
        return cooccur_vect[ind]
    except KeyError:
        global cooccur_vect
        # does not exist, we have to make it
        for item in items:
            # first we must make a list of common words using this sense data
            for instance in senseval.instances(item)[:1]: ## AAAAAHHH
                # first load the cases for item and get the most common words
                word_list = [x.lower() for x in instance.context]
                temp_list = word_list
                
                # remove all the stopwords
                for x in stopwords:
                    word_list = filter(lambda w: w != x, word_list)

                # remove the head word
                head = instance.context[instance.position].lower()
                word_list = filter(lambda w: w != head, word_list)
                
                word_counts = {}
                for word in word_list:
                    if word in word_counts:
                        word_counts[word] += 1
                    else:
                        word_counts[word] = 1

                # sort the list in descending order and truncate to get most common
                cooccur_vect[ind] = sorted(word_counts, key = word_counts.get, reverse = True)
                cooccur_vect[ind] = cooccur_vect[ind][:vectorSize]
        
        return cooccur_vect[ind]



# see if our features are already computed
try:
    common
except NameError:
    common = {}     

# call this to get a list of feature keys. The parameter is the item you are looking for
def get_features(ind):
    # let's see if common is already set
    try:
        common
    except NameError:
        common = {}

    # this must mean we're running training data, generate common
    if common[ind] is None:
        # look through the corpus and get vectorSize most common words
        common = {}
        for item in items:
            # first we must make a list of common words using this sense data
            for instance in senseval.instances(item)[:1]: ## AAAAAHHH
                # first load the cases for item and get the most common words
                word_list = [x.lower() for x in instance.context]
                temp_list = word_list
                
                # remove all the stopwords
                for x in stopwords:
                    word_list = filter(lambda w: w != x, word_list)

                # remove the head word
                head = instance.context[instance.position].lower()
                word_list = filter(lambda w: w != head, word_list)
                
                word_counts = {}
                for word in word_list:
                    if word in word_counts:
                        word_counts[word] += 1
                    else:
                        word_counts[word] = 1

                # sort the list in descending order and truncate to get most common
                common[item] = sorted(word_counts, key = word_counts.get, reverse = True)
                common[item] = common[item][:vectorSize]
    else

def cooccurrence(windowSize, pos, context, vect_keys):
    pass

for item in items:
    totalResult = []
    


    # okay, now we can make the vectors for this item
    for instance in senseval.instances(item)[:10]:
        pos = instance.position
        context = instance.context
        instance.senses
        print context
        print context[pos]
        d = cooccurrence(windowSize, pos, context, common)
        print d
