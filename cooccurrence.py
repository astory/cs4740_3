#!/usr/bin/env python
import os, os.path
import nltk
from nltk.corpus import senseval

# get the senseval 3 data from system folder
path = os.path.relpath('nltk_data')
nltk.data.path[0]=path

items = senseval.fileids()

# the feature extractor code is below here -----------------------------------
windowSize = 10     # number of words before and after to look for
vectorSize = 12     # size of your feature vector for cooccurrence
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

def feature_keys(ind):
    """This function must be called if one wants to get the words used in the
       feature vectors for the training data.

       Upon supplying an index (use the instance id), it returns a list of words.
       This is the keys that are used in the cooccurrence feature vectors."""

    # check to see if the feature vector we are looking for exists
    try:
        return cooccur_vect[ind]
    except KeyError:
        # does not exist, we have to make it
        # first we must make a list of common words using this sense data
        word_list = []
        heads = []
        for instance in senseval.instances(ind):
            # first load the cases for item and get the most common words
            heads = heads + [instance.context[instance.position].lower()]
            word_list.extend([x.lower() for x in instance.context])
            
        # remove the stop words
        for x in stopwords + heads:
            word_list = filter(lambda w: w != x, word_list)
        
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

# the actual cooccurrence function
def cooccurrence(item, pos, context, dictionary):
    inc_words = context[pos - windowSize/2:pos] + context[(pos + 1):(pos + 1 + windowSize / 2)]
    common = feature_keys(item)
    # a dictionary key should be "cooccurrence%word" and
    # value should be a tuple of 0's or 1's
    value = []
    for word in common:
        if word in inc_words:
            value = value + [1]
        else:
            value = value + [0]
    dictionary[("%s*%s%d" % ("cooccurrence",item,pos))] = tuple(value)
    return dictionary
