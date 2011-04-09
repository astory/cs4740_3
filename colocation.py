import os, os.path
import nltk
from nltk.corpus import senseval

path = os.path.relpath('nltk_data')
nltk.data.path[0]=path

items = senseval.fileids()
print items

items = items[:1]
windowSize=4

def colocation(windowSize, pos, context):
    contextResult= {}
    #going forward
    forward= context[:(pos)]
    f= forward[(-windowSize/2):]
    #going backward    
    backward= context[pos+1:]
    b= backward[:windowSize/2]
    for item in f:
        #key= "pre"+str(len(f)-f.index(item))+"-word"
        key= item
        value= nltk.pos_tag(item)
        contextResult[key]=value
    for item in b:
        #key= "fol"+str(b.index(item)+1)+"-word"
        key= item
        value= nltk.pos_tag(item)
        contextResult[key]=value
    return contextResult
        

for item in items:
    totalResult= []
    for instance in senseval.instances(item)[:10]:
	    pos = instance.position
	    context = instance.context
	    senses = instance.senses
	    print context
	    print context[pos]
	    d= colocation(windowSize, pos, context)
	    print d
		
		


    


    

