#!/bin/bash

cd ../

stopFilePath="/home/suchana/smart-stopwords"
if [ ! -f $stopFilePath ]
then
    echo "Please ensure that the path of the stopword-list-file is set in the .sh file."
else
    echo "Using stopFilePath: "$stopFilePath
fi

if [ $# -le 4 ] 
then
    echo "Usage: " $0 " <following arguments in the order>";
    echo "1. Path of the index.";
    echo "2. Path of the TREC query.xml file."
    echo "3. Path of the directory to store .res file."
    echo "4. SimilarityFunction: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity."
    echo "5. No. of top documents to be used."
    echo "6. No. of top terms to be used.";
    exit 1;
fi

indexPath=`readlink -f $1`              # absolute address of the index
indexpath=$indexPath"/"
queryPath=`readlink -f $2`		# absolute address of the query file
resPath=`readlink -f $3`		# absolute directory path of the .res file
resPath=$resPath"/"
queryName=$(basename $queryPath)
numTopDocs=$5
numTopTerms=$6

prop_name="retrieval-score-analyzer.properties"

echo "Using index at: "$indexPath
echo "Using query at: "$queryPath
echo "Using directory to store .res file: "$resPath

fieldToSearch="content"
fieldForFeedback="content"

echo "Field for searching: "$fieldToSearch

similarityFunction=$4

case $similarityFunction in
    1) param1=1.5
       param2=0.6 ;;
    2) param1=0.6
       param2=0.0 ;;
    3) param1=1500
       param2=0.0 ;;
esac

echo "similarity-function: "$similarityFunction" " $param1

# making the .properties file for TREC queries
cat > $prop_name << EOL

indexPath=$indexPath

fieldToSearch=$fieldToSearch

fieldForFeedback=$fieldForFeedback

queryPath=$queryPath

stopFilePath=$stopFilePath

resPath=$resPath

numTopDocs=$numTopDocs

numTopTerms=$numTopTerms

similarityFunction=$similarityFunction

param1=$param1
param2=$param2

variantGenerate=$variantGenerate

variantLength=$variantLength

EOL
# .properties file made


java -Xmx1g -cp $CLASSPATH:dist/NeuralModelQpp.jar model.aware.baseline.TermDistributionAnalyzer $prop_name
