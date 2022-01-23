#!/bin/bash

# create 50:50 train-test splits of the query set (random sampling)

if [ $# -le 0 ] 
then
    echo "Usage: " $0 " <following argument>";
    echo "1. How many times you want to do 50:50 random splits.";
    echo "2. Query file path (in .xml format).";
    echo "3. Path of the collection index.";
    echo "4. Stopwords file path.";
    echo "5. SimilarityFunction for document retrieval: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity.";
    echo "6. No. of top documents to retrieve for each test query.";
    echo "7. Path of the directory to store initial retrieved documents.";
    echo "8. Word vector file path.";
    echo "9. Name of the field used for searching (default 'content'- if using available index with this project).";
    echo "10. Interaction matrix path to store.";
    echo "11. Qrel file path.";
    exit 1;
fi

numSplit=$1
queryPath=`readlink -f $2`
indexPath=`readlink -f $3`
stopFilePath=`readlink -f $4`
similarityFunction=$5

case $similarityFunction in
    1) param1=1.5
       param2=0.6 ;;
    2) param1=0.6
       param2=0.0 ;;
    3) param1=1000
       param2=0.0 ;;
esac

echo "similarity-function: "$similarityFunction" " $param1
numHits=$6
retFilePath=`readlink -f $7` # absolute directory path of the .res file
wordVecPath=`readlink -f $8`
searchField=$9
interMatrixPath=`readlink -f $10`
qrelPath=`readlink -f $11`

echo "50:50 random splits will be done : "$numSplit" times\n"

echo "#######################################################################"
echo "#################### Create Sampled Query Files #######################"
echo "#######################################################################"

python3 query_random_sample.py $queryPath ./data/ $numSplit

# create interaction matrices both for train (from qrel) and test (from top retrieved doc set) split

cd ./InteractionMatrix/

echo "#######################################################################"
echo "################# Generate Interaction Matrices #######################"
echo "#######################################################################"

x=1 # need to fix this

sh interaction.sh ../data/query_sample_$x/train-query.xml ../data/query_sample_$x/test-query.xml $indexPath $stopFilePath $similarityFunction $numHits $retFilePath $wordVecPath $searchField $interMatrixPath $qrelPath

# Train DRMM model and generate reranked test file

echo "#######################################################################"
echo "################# Test file reranked using DRMM #######################"
echo "#######################################################################"

cd ../RerankByDRMM/

# create train-test folds for drmm model

python3 create_train_test_fold.py $qrelPath $retFilePath/LMDirichlet1000.0-D10-content.res ../data/query_sample_$x/train-query.xml ../data/query_sample_$x/test-query.xml ../data/ $x

# rerank test set documents with DRMM

python3 run_model.py <run_name> <train_pair_file> <train_histogram_file> <test_file> <test_histogram_file> <reranked_file_path>

# Compute qpp scores for test set using the information from reranked file

echo "#######################################################################"
echo "####################### Compute QPP scores ############################"
echo "#######################################################################"

cd ../qpp-eval/ 

sh qppeval.sh <num_top_docs_for_qpp_estimation> <qpp_method>

# Generate query variants for test set queries (choose options for rlm and w2v)

echo "#######################################################################"
echo "############# Generate query variants automatically ###################"
echo "#######################################################################"

cd ../GenerateQueryVariants/script/
sh makeQueryVariantsTrec.sh $indexPath $queryPath <res_file_path> <similarity_function> .......

# create interaction matrices for test set query variants (from preranked files only)

cd ./InteractionMatrix/

echo "#######################################################################"
echo "########## Generate Interaction Matrices for query variants ###########"
echo "#######################################################################"

sh interaction.sh ../data/query_sample_$x/train-query.xml ../data/query_sample_$x/test-query.xml $indexPath $stopFilePath $similarityFunction $numHits $retFilePath $wordVecPath $searchField $interMatrixPath $qrelPath

# Rerank documents retrieved by query variants using the learning weights of train set

echo "#######################################################################"
echo "################# Variants docs reranked using DRMM ###################"
echo "#######################################################################"

cd ../RerankByDRMM/

# rerank test set documents with DRMM

python3 run_model.py <run_name> <train_pair_file> <train_histogram_file> <test_file> <test_histogram_file> <reranked_file_path>


# Compute qpp scores for test set using the information from reranked file

echo "#######################################################################"
echo "################# Compute QPP scores for variants #####################"
echo "#######################################################################"

cd ../qpp-eval/ 

sh qppeval.sh <num_top_docs_for_qpp_estimation> <qpp_method>

# Compute weighted information gain for test query set

echo "#######################################################################"
echo "############### Compute weighted information gain #####################"
echo "#######################################################################"

cd ../wrig/

python3 wrig.py <reranked file> <query variants based LM reranked file> <ground truth .AP file> <res file path> <no. of top documents retrieved by initial query in the initial.res file> <no. of top documents retrieved by query variant in the variant.res file> <no. of top scores to be considered for each variant> 
