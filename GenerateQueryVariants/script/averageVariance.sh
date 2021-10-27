#!/bin/bash

cd ../

stopFilePath="/home/suchana/smart-stopwords"
if [ ! -f $stopFilePath ]
then
    echo "Please ensure that the path of the stopword-list-file is set in the .sh file."
else
    echo "Using stopFilePath="$stopFilePath
fi

if [ $# -le 3 ] 
then
    echo "Usage: " $0 " <following arguments in the order>";
    echo "1. indexPath: Path of the index."
    echo "2. queryPath: path of the query file (in proper xml format)."
    echo "3. resPath: path of the directory to store res file.";
    echo "4. SimilarityFunction: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity.";
    exit 1;
fi

indexPath=`readlink -f $1`		# absolute address of the index
queryPath=`readlink -f $2`		# absolute address of the query file
resPath=`readlink -f $3`		# absolute directory path of the .res file
resPath=$resPath"/"

queryName=$(basename $queryPath)
prop_name="qppAverageVariance.properties"

echo "Using index at: "$indexPath
echo "Using query at: "$queryPath
echo "Using directory to store .res file: "$resPath

fieldToSearch="content"

echo "Field for searching: "$fieldToSearch

similarityFunction=$4

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

queryPath=$queryPath

stopFilePath=$stopFilePath

resPath=$resPath

#numHits= 1000

similarityFunction=$similarityFunction

param1=$param1
param2=$param2

EOL
# .properties file made

java -Xmx1g -cp $CLASSPATH:dist/NeuralModelQpp.jar baselines.AverageVariance $prop_name
