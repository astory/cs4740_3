#This takes a list of one or two elements.
# $files vector of file names
# $scores data.frame of data (optional)
score.bind=function(s) {if (length(s$files)==0) s else{
	scores.new=read.csv(s$files[1],header=F,col.names=c('word','instance','correct'))
	scores.new$id=s$files[1]
	Recall(list(files=s$files[-1],
		scores=rbind(scores.new,s$scores)))
}}

#This takes a comma-separated list of file names
combine=function(...){
	score.bind(list(files=c(...)))
}

