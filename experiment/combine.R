#This takes a list of one or two elements.
# $files vector of file names
# $scores data.frame of data (optional)
score.bind=function(s) {if (length(s$files)==0) s$scores else{
	scores.new=read.csv(s$files[1],header=F,col.names=c('word','instance','correct'))
	scores.new$id=s$files[1]
	Recall(list(files=s$files[-1],
		scores=rbind(scores.new,s$scores)))
}}

#This takes a comma-separated list of file names
combine=function(...){
	score.bind(list(files=c(...)))
}

main=function(){
	combine(paste('random_data/',c('randomscores01.csv', 'randomscores02.csv', 'randomscores03.csv', 'randomscores04.csv',
	'randomscores05.csv', 'randomscores06.csv', 'randomscores07.csv', 'randomscores08.csv'),sep=''))
}
