#!/bin/bash

cd ../

stopFilePath="/home/suchana/smart-stopwords"
if [ ! -f $stopFilePath ]
then
    echo "Please ensure that the path of the stopword-list-file is set in the .sh file."
else
    echo "Using stopFilePath="$stopFilePath
fi

if [ $# -le 7 ] 
then
    echo "Usage: " $0 " <following arguments in the order>";
    echo "1. Path of the index.";
    echo "2. Path of the query.xml file."
    echo "3. Path of the directory to store res file."
    echo "4. Number of expansion documents.";
    echo "5. Number of expansion terms: from pseudo relevant set.";
    echo "6. RM3-QueryMix (0.0-1.0): to weight between P(w|R) and P(w|Q).";
    echo "7. SimilarityFunction: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity.";
    echo "8. Path of the WRIG(LR) file.";
    exit 1;
fi

indexPath=`readlink -f $1`		# absolute address of the index
queryPath=`readlink -f $2`		# absolute address of the query file
resPath=`readlink -f $3`		# absolute directory path of the .res file
resPath=$resPath"/"
wrigPath=`readlink -f $8`

queryName=$(basename $queryPath)
prop_name="uefSingle.properties"

echo "Using index at: "$indexPath
echo "Using query at: "$queryPath
echo "Using directory to store .res file: "$resPath

fieldToSearch="content"
fieldForFeedback="content"

echo "Field for searching: "$fieldToSearch
echo "Field for feedback: "$fieldForFeedback

similarityFunction=$7

case $similarityFunction in
    1) param1=1.5
       param2=0.6 ;;
    2) param1=0.6
       param2=0.0 ;;
    3) param1=1000
       param2=0.0 ;;
esac

echo "similarity-function: "$similarityFunction" " $param1

# making the .properties file
cat > $prop_name << EOL

indexPath=$indexPath

fieldToSearch=$fieldToSearch

fieldForFeedback=$fieldForFeedback

queryPath=$queryPath

stopFilePath=$stopFilePath

resPath=$resPath

wrigPath=$wrigPath

numHits= 1000
#numHits= 500

similarityFunction=$similarityFunction

param1=$param1
param2=$param2

# Number of documents
numFeedbackDocs=$4

# Number of terms chosen for feedback
numFeedbackTerms=$5

rm3.queryMix=$6


EOL
# .properties file made

java -Xmx1g -cp $CLASSPATH:dist/NeuralModelQpp.jar baselines.UEFSingle $prop_name
