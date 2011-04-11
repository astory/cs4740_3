#!/usr/bin/env python
import os
os.environ['MALTPARSERHOME']='MaltParser/malt-1.2'
import nltk
import nltk.parse.malt as malt

m = malt.MaltParser()
m.train_from_file('MaltParser/engmalt.linear.mco')

STR = "There is beauty in the bellow of the blast."

g = m.parse(STR)
print g.tree()
