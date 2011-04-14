#library(leaps)
calls='./calls.csv'
scores_file='scores.csv'
#This takes a comma-separated list of file names
combine=function(...){
	#This takes a list of one or two elements.
	# $files vector of file names
	# $scores data.frame of data (optional)
	score.bind=function(s) {if (length(s$files)==0) s$scores else{
		scores.new=read.csv(s$files[1],header=F)
		names(scores.new)=c('word','sense','correct')
		scores.new$run_id=s$files[1]
		bound=rbind(s$scores,scores.new)
		Recall(list(files=s$files[-1],scores=bound))}}
	score.bind(list(files=c(...)))
}



import.instances=function(run_ids){
	#Read the identifiers for the different systems
	systems=read.csv(calls)
	systems$run_id=paste('tmp',systems$run_id,'.csv',sep='')

#	ids=paste(filehead,systems$run_id,'.csv',sep='')
	ids=paste(run_ids,'.csv',sep='')

	#Combine the systems
	scores=combine(ids)
	scores$correct=as.logical(scores$correct)

	#Add the information about feature use and
	#return the data.frame with scores and feature turnings-on
	merge(scores,systems,by='run_id',all=F)
}


import.aggregated=function(){
        #Read the identifiers for the different systems
        systems=read.csv(calls)
	systems$run_id=paste('tmp',systems$run_id,sep='')
	scores=read.csv(scores_file)
	merge(scores,systems,by='run_id',all=F)
}

mymean=function(x){
	if (is.numeric(x)){
		mean(x)
	} else {
		x[1]
	}
}

plot.aggregated=function(ag){
	makelines=aggregate(ag,list(ag$classifier,ag$cooccurrence,ag$colocation),mymean)

	p <- ggplot(ag,aes(colocation,f,group=classifier))
	print(p+geom_point(aes(colour = classifier))+ 
		geom_line(data=subset(makelines,cooccurrence==0),aes(linetype='Cooccurrence Off'))+
		geom_line(data=subset(makelines,cooccurrence==1),aes(linetype='Ooocurrence On'))
	)


	p <- ggplot(ag,aes(colocation,precision,group=classifier))
	print(p+geom_point(aes(colour = classifier))+ 
		geom_line(data=subset(makelines,cooccurrence==0),aes(linetype='Cooccurrence Off'))+
		geom_line(data=subset(makelines,cooccurrence==1),aes(linetype='Ooocurrence On'))
	)


	p <- ggplot(ag,aes(colocation,recall,group=classifier))
	print(p+geom_point(aes(colour = classifier))+ 
		geom_line(data=subset(makelines,cooccurrence==0),aes(linetype='Cooccurrence Off'))+
		geom_line(data=subset(makelines,cooccurrence==1),aes(linetype='Ooocurrence On'))
	)
}

plot.lowerorder=function(ag){
	#Let's look at the residuals of those lines
	ag$resid.f=lm(f~factor(colocation)*classifier*cooccurrence,data=ag)$residuals
	p <- ggplot(ag,aes(bootstrap,resid.f,group=base_word))
	print(p+geom_point(aes(colour = factor(base_word))))
	
	ag$resid.precision=lm(precision~factor(colocation)*classifier*cooccurrence,data=ag)$residuals
	p <- ggplot(ag,aes(bootstrap,resid.precision,group=base_word))
	print(p+geom_point(aes(colour = factor(base_word))))
	
	ag$resid.recall=lm(recall~factor(colocation)*classifier*cooccurrence,data=ag)$residuals
	p <- ggplot(ag,aes(bootstrap,resid.recall,group=base_word))
	print(p+geom_point(aes(colour = factor(base_word))))
}


blah=function(){
	library(ggplot2)
	pdf('plots.pdf')
	ag=import.aggregated()
	ag$f=f(ag)

	measures=c('precision','recall','f')
	ag.fine=subset(ag,grain=='fine')
	ag.mixed=subset(ag,grain=='mixed')
	ag.coarse=subset(ag,grain=='coarse')
	write.csv(cbind(ag.fine,ag.mixed[measures],ag.coarse[measures])[order(ag.mixed$f,decreasing=T),],'finemixedcoarse.csv',row.names=F)


	write.csv(ag[rowSums(ag[c(
		"bootstrap","colocation","cooccurrence","base_word","dependency_parsing"
	)])<=1,],row.names=F,file='onefeature.csv')

	plot(f~bootstrap,data=ag[rowSums(ag[c(
                "bootstrap","colocation","cooccurrence","base_word","dependency_parsing"
        )])==ag$bootstrap,])

	plot(f~colocation,data=ag[rowSums(ag[c(
                "bootstrap","colocation","cooccurrence","base_word","dependency_parsing"
        )])==ag$colocation,])
	
	ag$bootstrap=as.factor(ag$bootstrap)
	ag$colocation=as.factor(ag$colocation)
	ag$base_word=as.factor(ag$base_word)


	write.csv(ag[order(ag$f),],row.names=F,file='findbrokenness.csv')

	plot.aggregated(ag)
	plot.lowerorder(ag)

	
	dev.off()
}


f=function(ag) 2*ag$precision*ag$recall/(ag$precision+ag$recall)

plot.step.import=function(sub='mixed'){
	ag=import.aggregated()
	offon=c('Off','On')
	if (!is.na(sub)) ag=subset(ag,grain==sub)
	ag$f=f(ag)
	ag$cooccurrence=factor(ag$cooccurrence)
	ag$base_word=factor(ag$base_word)
	ag$dependency_parsing=factor(ag$dependency_parsing)
	levels(ag$classifier)=c('Naive Bayes','Decision tree')
	levels(ag$base_word)<-offon
	levels(ag$cooccurrence)<-offon
	levels(ag$dependency_parsing)<-offon
	ag
}

#Most significant terms from the stepwise regressio
plot.step0a=function(){
	ag=plot.step.import()
	ggplot(ag,aes(colocation,f,group=c(dependency_parsing)))+
	geom_point(aes(color=dependency_parsing),position=position_jitter(w=0.1, h=0))+
	opts(title = expression("Dependency parsing is only helpful when colocation is off"))
}
plot.step0b=function(){
	ag=plot.step.import()
	ggplot(ag,aes(dependency_parsing,f,group=c(cooccurrence)))+
	geom_point(aes(color=cooccurrence),position=position_jitter(w=0.1, h=0))+
	opts(title = expression("Dependency parsing is only helpful when cooccurrence is off"))
}

plot.step1=function(){
	ag=plot.step.import()
	ggplot(ag,aes(colocation,f,group=c(cooccurrence)))+
	geom_point(aes(color=cooccurrence),position=position_jitter(w=0.1, h=0))+
	opts(title = expression("The optimal colocation window is between 2 and 4 words"))
#	opts(title = expression("Performance of all of our systems"))+
#	scale_x_continuous('Colocation window size') + scale_y_continuous('Mixed-grained F-measure')
}

#Select optimal colocation window and keep looking
plot.step2=function(){
	ag=subset(plot.step.import(),colocation>=2&colocation<=4)
	ggplot(ag,aes(classifier,f,group=c(base_word,dependency_parsing)))+
	geom_point(aes(colour=base_word,shape=dependency_parsing),position=position_jitter(w=0.1, h=0))+
	opts(title=expression('Performance by classifier'))
#	opts(title=expression('Performance of the systems with colocation windows between 2 and 4'))+
#	scale_x_continuous('Classifier') + scale_y_continuous('Mixed-grained F-measure')
}

#Little effect of bootstrapping or base-word handling. Bootstrapping seems to plateau at 2 iterations.
plot.step3=function(){
	ag=subset(plot.step.import(),colocation>=2&colocation<=4&cooccurrence=='On'&classifier=='Naive Bayes'&dependency_parsing=='Off')
	ggplot(ag,aes(bootstrap,f,group=base_word,label=colocation))+
	geom_text(aes(colour=base_word))+
	opts(title=expression('Effect of the remaining features'))
	#opts(title=expression('Performance of the systems with colocation windows between 2 and 4, cooccurrence handling on, Naive Bayes classifier and dependency parsing off'))+
#	scale_x_continuous('Bootstrap iterations') + scale_y_continuous('Mixed-grained F-measure')
}

plot.special1=function(){
#colocation:cooccurrence1:dependency_parsing  
}

rank.scores=function(){
#This demonstrates that the ranking of scores was very similar for different score grains and that the ranking was identical for the first dozen feature combinations
	foo=plot.step.import()[order(plot.step.import()$f,decreasing=T),]
	data.frame(
		coarse=subset(foo,grain=='coarse')$run_id,
		fine=subset(foo,grain=='fine')$run_id,
		mixed=subset(foo,grain=='mixed')$run_id
	)
#head(foo)[c('run_id','f','precision','recall','classifier','attempted','bootstrap','colocation','cooccurrence','base_word','dependency_parsing')]
}

stepping=function(){
	pdf('plots.pdf')
	print(plot.step0b())
	print(plot.step1())
	print(plot.step2())
	print(plot.step3())
	dev.off()
	#step(lm(f~classifier*bootstrap*colocation*cooccurrence*base_word*dependency_parsing,data=plot.step.import()))
}


plot.baseline=function(){
	ag=subset(plot.step.import(sub=NA),
		bootstrap==0&colocation==0&cooccurrence=='Off'&base_word=='Off'&dependency_parsing=='Off')
	ggplot(ag,aes(grain,f,group=classifier))+
	geom_line(aes(color=classifier))+
	opts(title = expression("Baseline performance"))
}

byword=function(file){
	a=read.csv(file,row.names=as.character(1:171))
	#ggplot(a,corpus_size,f,group=grain)+
#	ggplot(subset(a,grain=='mixed'),aes(corpus_size,f,label=word))+
#	geom_text()
}

main=function(){
	stepping()
# 	pdf('baseline.pdf');plot.baseline();dev.off()
	byword('../byword_tmp28')
}
