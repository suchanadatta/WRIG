#!/bin/bash

cd ../

stopFilePath="/home/suchana/smart-stopwords"
if [ ! -f $stopFilePath ]
then
    echo "Please ensure that the path of the stopword-list-file is set in the .sh file."
else
    echo "Using stopFilePath: "$stopFilePath
fi

if [ $# -le 7 ] 
then
    echo "Usage: " $0 " <following arguments in the order>";
    echo "1. Path of the index.";
    echo "2. Path of the TREC query.xml file."
    echo "3. Path of the qrel(train)/prerank(test) file."
    echo "4. Path of the w2v embedding file."
    echo "5. Path to store the data generator file."
    echo "6. Choose (1) for training or (2) for tesing : 1. qrel or 2. prerank (input in text - qrel/prerank)."
    echo "7. Maximum no. of terms to choose from document."
    echo "8. SimilarityFunction: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity. (valid if prerank, else 0)";
    exit 1;
fi

indexPath=`readlink -f $1`              # absolute address of the index
indexPath=$indexPath"/"
queryPath=`readlink -f $2`		# absolute address of the query file
qrelPath=`readlink -f $3`		# absolute directory path of the qrel file
qrelPath=$qrelPath"/"
queryName=$(basename $queryPath)
embeddingPath=`readlink -f $4`          # absolute directory path of the embedding file
resPath=`readlink -f $5`		# absolute directory path of the .res file
resPath=$resPath"/"
qrelPrerank=$6
docMaxLength=$7

prop_name="KnrmDataGenerator-"$queryName".properties"

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

# making the .properties file for TREC queries
cat > $prop_name << EOL

indexPath=$indexPath

queryPath=$queryPath

qrelPath=$qrelPath

stopFilePath=$stopFilePath

fieldToSearch=$fieldToSearch

similarityFunction=$similarityFunction

param1=$param1
param2=$param2

embeddingPath=$embeddingPath

resPath=$resPath

qrelPrerank=$qrelPrerank

docMaxLength=$docMaxLength

EOL
# .properties file made


java -Xmx4g -cp $CLASSPATH:dist/NeuralModelQpp.jar knrm.KNRMDataGenerator $prop_name
