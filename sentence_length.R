train.features=read.csv('sentenceLength.train.txt',sep='|')
train.senses=read.csv('EnglishLS.train/EnglishLS.train.key',sep=' ',header=F,col.names=c('word','occurrance','sense1','sense2'))
train=merge(train.features,train.senses)

test.features=read.csv('sentenceLength.test.txt',sep='|')
test.senses=read.csv('EnglishLS.test/EnglishLS.test.key',sep=' ',header=F,col.names=c('word','occurrance','sense1','sense2'))

test=merge(test.features,test.senses)

#Logit doesn't say much. Outliers are influential.
#fit=glm(sense1~word,data=train,family='binomial')
#predict(fit,test)
fit=glm(sense1~word,data=subset(train,word=='activate.v'),2),family='binomial')
#hist(predict(fit,test))

#Clustering within a word
activate=subset(train,word=='activate.v')
sunflowerplot(fanny(activate$wordCount,2)$clustering,activate$sense1)
mosaicplot(~fanny(activate$wordCount,2)$clustering+activate$sense1)
sunflowerplot(fanny(activate$characterCount,2)$clustering,activate$sense1)
mosaicplot(~fanny(activate$characterCount,3)$clustering+activate$sense1)

