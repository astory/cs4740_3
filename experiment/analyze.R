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



import.instances=function(){
	library(foreign)
	#Read the identifiers for the different systems
	systems=read.csv(calls)
	systems$run_id=paste('tmp',systems$run_id,'.csv',sep='')

#	ids=paste(filehead,systems$run_id,'.csv',sep='')
	ids=list.files()[grep('tmp[0-9]*.csv',list.files())]

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

stepping=function(scores){
	fit=glm(correct~word+colocation*cooccurrence,data=scores,family='binomial')
	#stepwise
#	step(fit)
	#best-subsets requires the response at the end
	#stuff=import()
	#bestglm(cbind(stuff[-4],stuff[4]))
}

mymean=function(x){
	if (is.numeric(x)){
		mean(x)
	} else {
		x[1]
	}
}

plot.aggregated=function(ag){
	ag$boostrap=as.factor(ag$bootstrap)
	p <- ggplot(ag,aes(colocation,precision,group=classifier))
	makelines=aggregate(ag,list(ag$classifier,ag$cooccurrence,ag$colocation),mymean)
	print(makelines)
	print(p+geom_point(aes(colour = classifier))+ 
		geom_line(data=subset(makelines,cooccurrence==0),aes(linetype='Cooccurrence Off'))+
		geom_line(data=subset(makelines,cooccurrence==1),aes(linetype='Ooocurrence On'))
	)
}
plot.lowerorder=function(ag){
	#Let's look at the residuals of those lines
	ag$resid1.odds=exp(glm(precision~factor(colocation)*classifier*cooccurrence,data=ag,family='binomial')$residuals)
	ag$resid1=lm(precision~factor(colocation)*classifier*cooccurrence,data=ag,family='binomial')$residuals

	p <- ggplot(ag,aes(colocation,resid1,group=base_word))
	print(p+geom_point(aes(colour = classifier)))

	p <- ggplot(ag,aes(bootstrap,resid1,group=base_word))
	print(p+geom_point(aes(colour = factor(base_word))))
}
main=function(){
	library(ggplot2)
	pdf('plots.pdf')
	ag=import.aggregated()
	plot.aggregated(ag)
	plot.lowerorder(ag)

	
	dev.off()
}
