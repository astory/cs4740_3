import os, os.path
import nltk
from nltk import pos_tag, word_tokenize
from nltk.corpus import senseval

path = os.path.relpath('nltk_data')
nltk.data.path[0]=path

items = senseval.fileids()
items = items[:1]

def colocation(windowSize, pos, context,dictionary):
    if windowSize<=0:
        return dictionary
    #going forward
    forward= context[:(pos)]
    f= forward[(-windowSize/2):]
    #going backward    
    backward= context[pos+1:]
    b= backward[:windowSize/2]
    for item in f:
        key= "pre"+str(len(f)-f.index(item))+"-word"
        value= item
        dictionary[key]=value
        key= "pre"+str(len(f)-f.index(item))+"-pos"
        text = nltk.word_tokenize(item)
        value= nltk.pos_tag(text)[0][1]
        dictionary[key]=value
    for item in b:
        key= "fol"+str(b.index(item)+1)+"-word"
        value= item
        dictionary[key]=value
        key= "fol"+str(b.index(item)+1)+"-pos"
        text = nltk.word_tokenize(item)
        value= nltk.pos_tag(text)[0][1]
        dictionary[key]=value
    return dictionary
        
if __name__=="__main__":
    for item in items:
        totalResult= []
        windowSize=4
        dictionary={}
        for instance in senseval.instances(item)[:10]:
                pos = instance.position
                context = instance.context
                senses = instance.senses
                #print context
                #print context[pos]
                d= colocation(windowSize, pos, context,dictionary)
                print d
                    
		


    


    

