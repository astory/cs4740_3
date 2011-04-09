combine=function(files,features){
#output must be a vector with values in the format paste(scores,id,sep=''), where id looks like '03.csv'
	scores.tab=read.csv(files)
	systems.tab=read.csv(systems)
}

#This takes a list of one or two elements.
# $files vector of file names
# $scores data.frame of data (optional)
score.bind=function(s) {
if (length(s$files)==1){
	#Read the file
	scores.tab=read.csv(s$files[1])
	#Use the file name as the identifier
	scores.tab$id=s$files[1]
	#Return the list of files minus the one we added
	#and the data.frame of scores
	list(
		files=NULL,
		scores=scores.tab
	)
}
else if (length(s$files)>1) {
	Recall(list(files=s$files[-1],
		scores=scores
	)))
} else if (is.null(s$files)) { s }
}
main=function(){
	score.bind(list(files=c('scores00.csv','scores00.csv','scores00.csv')))
}

