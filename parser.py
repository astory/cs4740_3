#!/usr/bin/env python
import os
os.environ['MALTPARSERHOME']='MaltParser/malt-1.2'
import sys
import nltk
import nltk.parse.malt as malt
import pickle as p

m = malt.MaltParser()
#malt.demo() # this is sometimes needed?  I don't get it.
m.train_from_file('MaltParser/engmalt.linear.mco')

STR2 = "Do you know what it is ,  and where I can get one ?  We suspect you had seen the Terrex Autospade ,  which is made by Wolf Tools .  It is quite a hefty spade , with bicycle - type handlebars and a sprung lever at the rear , which you step on to activate it . Used correctly ,  you should n't have to bend your back during general digging ,  although it wo n't lift out the soil and put in a barrow if you need to move it !  If gardening tends to give you backache ,  remember to take plenty of rest periods during the day ,  and never try to lift more than you can easily cope with ."

all_of_them = {}

def parse(pos, context, d):
	flat_context = " ".join(context)
	if flat_context in all_of_them:
		l = all_of_them[l]
	else:
		tree = m.parse(flat_context)
		l = tree.nodelist
	if pos in range(len(l)):
		node = l[pos]
		deps = node['deps']
		if len(deps) > 0:
			dep = l[deps[0]]['word']
		else:
			dep = ""
		parent = l[node['head']]['word']
		d['dependency'] = dep
		d['has_dependencies'] = len(deps)>0
		d['dependency_count'] = len(deps)
		d['parent'] = parent
	else:
		print >> sys.stderr, 'Warning, index %d out of range in nodelist' % pos
		print >> sys.stderr, l
		d['dependency'] = ""
		d['has_dependencies'] = False
		d['dependency_count'] = False
		d['parent'] = ""
	all_of_them[flat_context] = l
	return d

def pickle(file):
	print >> sys.stderr, "pickling"
	p.dump(all_of_them, file)

def load(file):
	all_of_them = p.load(file)
	return all_of_them

if __name__ == "__main__":
	#Uncomment the line below after the first run
	open('/tmp/malt_train.conll', 'w').close(); malt.demo()
	
	pos = 56
	context = STR2.split()
	d = parse(pos, context, {})
	print d
	path = os.path.relpath("temp_pickle")
	f = open(path, 'w')
	pickle(f)
	f.close()
