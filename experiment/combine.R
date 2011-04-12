#library(leaps)

#This takes a comma-separated list of file names
combine=function(...){
	#This takes a list of one or two elements.
	# $files vector of file names
	# $scores data.frame of data (optional)
	score.bind=function(s) {if (length(s$files)==0) s$scores else{
		scores.new=read.csv(s$files[1],header=T)
		scores.new$id=s$files[1]
		bound=rbind(s$scores,scores.new)
		Recall(list(files=s$files[-1],scores=bound))}}
	score.bind(list(files=c(...)))
}



import=function(){
	#Read the identifiers for the different systems
	systems=read.csv('system_combinations.csv')[1:8,] #,colClasses=c('character','logical','logical','logical'))	

	ids=paste('random_data/randomscores',systems$id[-5],'.csv',sep='')
	systems$id[-5]=ids

	#Combine the systems
	scores=combine(ids)
	scores$correct=as.logical(scores$correct)

	#Add the information about feature use and
	#return the data.frame with scores and feature turnings-on
	merge(scores,systems,by='id',all=F)
}

stepping=function(scores){
	fit=glm(correct~word+colocation*cooccurrence,data=scores,family='binomial')
	#stepwise
	step(fit)
	#best-subsets requires the response at the end
	#stuff=import()
	#bestglm(cbind(stuff[-4],stuff[4]))
}

main=function(){
	foo=stepping(import())
	print('Best two-feature system:')
	print(foo$call)
	print(paste('Percentage correct under the best two-feature system: ',round(100*sum(round(foo$fitted.values)==foo$y)/length(foo$y)),'%',sep=''))
}
