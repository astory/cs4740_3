import os, os.path
import nltk
from nltk.corpus import senseval

path = os.path.relpath('nltk_data')
nltk.data.path[0]=path

print senseval.fileids()

