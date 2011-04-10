#!/usr/bin/env python
import nltk
return_list_list = []
buffer = []
window = []
sense_word_seen = 0
words_past_sense_word = 0

def data_extend (filename, word_list, window_size):
  f = open(filename)
  for line in f:
    line = line.rstrip().rstrip('/n')
    l = line.split(' ')
    for word in l:
      word,sense = word.rsplit('/')
      window.append(word,sense)
      if sense_word_seen:
        words_past_sense_word +=1
      if word_list.contains(word):
        sense_word_seen = 1
      if len(window)>=(2*window_size+1):
        x,s = window.pop([0])
        if x=='.':
          buffer = []
        else:
          buffer.append(x,s)
        if words_past_sense_word >= window_size:
          if word=='.':
            pos = len(buffer) + len(window) - words_past_sense_word - 1
            w1,pos1 = '',''
            if pos>=len(buffer):
              w1,pos1 = window[(pos - len(buffer))]
            else:
              w1,pos1 = buffer[pos]
            new_list = buffer + window
            final_list = []
            for n in new_list:
                final_list.append((n.rsplit())[0])
            return_list_list + ((final_list),pos,pos1)
            words_past_sense_word = 0
            sense_word_seen = 0
              
              
