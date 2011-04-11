#!/usr/bin/env python

from BeautifulSoup import BeautifulStoneSoup
import unicodedata
import re
import sys

sample_doc = """<lexelt item="activate.v">
<instance id="activate.v.bnc.00008457" docsrc="BNC">
<context>
They were seen to succeed in living a sexually pure life as part of this .  There was some extension of the pietistic ideal .  Sodalities , such as the Legion of Mary , Opus Dei , and Christian Life communities , have partially extended this form of commitment to some , particularly more middle - class laity , and continue to have an important role in <head>activating</head> laity for what are judged to be religious goals both personally and socially . But generally speaking the ideal has always tended to accentuate the gap between the clerical world view and the lay world view within catholicism .  Even though the gap between clerical and lay religious intellectuals has closed ,  with clergy being left behind in some areas ,  the clerics remain the true cognoscenti  in religious matters ,  and are expected to be so by the laity .
</context>
</instance>
<instance id="activate.v.bnc.02016514" docsrc="BNC">
<context>
This pattern persists up to 9.5d.p.c .   (  c )   when both stripes of expression are at their strongest .  At 10.0d.p.c .   ( d )   Krox - 20 expression begins to be downregulated in r3 and is followed ,  at 10.5d.p.c .  (  e )   by the downregulation of expression in r5 ;  f - j   ( lateral views ) and f ' j '  (   dorsal views )  show that retinoic acid treatment alters this pattern .  The gene is not initially <head>activated</head> in r3 at 8.25 d.p.c . as shown in f  and f '  .  At 8.5 d.p.c .
</context>
</instance>
</lexelt>"""

targeter = re.compile(r"""<head>\w+<\/head>""")
cleaner = re.compile(r"""<\/?head>""")

def normalize(s):
	return unicodedata.normalize('NFKD',s).encode('ascii','ignore')

def parse_text(doc):
	"""Parse an xml string containing one or more lexical elements (one for each
	target word), with multiple instances, each with one context.
	
	Returns a dictionary, with the keys the lexical elements ("activate.v") and
	the values a tuple, with the first element the id number for that instance
	and the second element a list of the words."""
	soup = BeautifulStoneSoup(doc)

	words = {}

	instances = soup('instance')
	for i in instances:
		id = normalize(i['id'])
		parts = id.split('.')
		lexelt = '.'.join(parts[0:2])
		id_num = parts[3]
		context_raw = i.contents[1].renderContents()[1:]
		context = context_raw.split()
		position = -1
		for i in range(0,len(context)):
			if targeter.match(context[i]):
				position = i
				context[i] = cleaner.sub("",context[i])
				break
		
		if position is -1:
			raise Exception("Error:  did not find target word")
		if lexelt not in words:
			words[lexelt] = []
		words[lexelt].append(dict(id_num=id_num,context=context,position=position))
	return words

def parse_filehandle(filehandle):
	return parse_text(filehandle.read())

def parse_file(filename):
	return parse_filehandle(open(filename))

parse_text(sample_doc)
