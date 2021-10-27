#!/bin/bash
# generates interaction matrices between query and pseudo-relevant documents (for test split)
# generates interaction matrices between query and qrels (for train split)

if [ $# -le 10 ] 
then
    echo "Usage: " $0 " <following arguments in the order>";
    echo "1. Train query file (in .xml format).";
    echo "2. Test query file (in .xml format).";
    echo "3. Path of the lucene index."
    echo "4. Stopwords file."
    echo "5. SimilarityFunction: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity.";
    echo "6. No. of top documents to retrieve.";
    echo "7. Path of the directory to store initial retrieved documents.";
    echo "8. Word vector file path.";
    echo "9. Name of the field used for searching (default 'content'- if using available index with this project).";
    echo "10. Interaction matrix path.";
    echo "11. Qrel file path.";
    exit 1;
fi

trainQueryPath=`readlink -f $1`
testQueryPath=`readlink -f $2`
indexPath=`readlink -f $3`
stopFilePath=`readlink -f $4`
numHits=$6
retFilePath=`readlink -f $7` # absolute directory path of the .res file
retFilePath=$retFilePath"/"
wordVecPath=`readlink -f $8`
searchField=$9
interMatrixPath=`readlink -f $10`
interMatrixPath=$interMatrixPath"/"
qrelPath=`readlink -f $11`

echo "Using training query file at: "$trainQueryPath
echo "Using test query file at: "$testQueryPath
echo "Using index at : "$indexPath
echo "Using stop file at : "$stopFilePath
echo "Store initial retrieved file at : "$retFilePath
echo "Using word2vec file at : "$wordVecPath 
echo "Store interaction matrices at : "$interMatrixPath
echo "Qrel data used from : "$qrelPath

similarityFunction=$5

case $similarityFunction in
    1) param1=1.5
       param2=0.6 ;;
    2) param1=0.6
       param2=0.0 ;;
    3) param1=1000
       param2=0.0 ;;
esac

echo "similarity-function: "$similarityFunction" " $param1"\n"

# making the .properties file
cat > interaction.properties << EOL

trainQueryPath=$trainQueryPath

testQueryPath=$testQueryPath

indexPath=$indexPath

stopFilePath=$stopFilePath

similarityFunction=$similarityFunction

param1=$param1
param2=$param2

numHits=$numHits

retFilePath=$retFilePath

wordVecPath=$wordVecPath

searchField=$searchField

interMatrixPath=$interMatrixPath

qrelPath=$qrelPath

EOL
# .properties file made

# create matching histogram for judged documents

echo "#######################################################################"
echo "################# Generate Interaction for Qrel #######################"
echo "#######################################################################"

java -Xmx3g -cp $CLASSPATH:dist/InteractionMatrix.jar interactionmatrix.GenerateHistogramQrelFile

# create matching histogram for preranked documents

echo "#######################################################################"
echo "################ Generate Interaction for Prerank #####################"
echo "#######################################################################"

java -Xmx3g -cp $CLASSPATH:dist/InteractionMatrix.jar interactionmatrix.GenerateHistogramPrerankFile
