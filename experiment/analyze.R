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

plot.aggregated=function(ag){

}

main=function(){
	plot.aggregated(import.aggregated())
#	foo=stepping(import())
#	print('Best two-feature system:')
#	print(foo$call)
#	print(paste('Percentage correct under the best two-feature system: ',round(100*sum(round(foo$fitted.values)==foo$y)/length(foo$y)),'%',sep=''))
}
