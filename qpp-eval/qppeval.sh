#!/bin/bash

if [ $# -lt 2 ]
then
    echo "Usage: " $0 " <following arguments in the order>";
    echo "1. Num-top docs for qpp-estimation (e.g. 50)";
    echo "2. Method (nqc/wig/clarity/uef_nqc/uef_wig/uef_clarity)";
    exit 1;
fi

METHOD=$2

#Change this path to index/ (committed on git) after downloading the Lucene indexed
#TREC disks 4/5 index from https://rsgqglln.tkhcloudstorage.com/item/c59086c6b00d41e79d53c58ad66bc21f
INDEXDIR=/store/index/trec678/
QRELS=/store/qrels/trec-robust.qrel

cat > qpp.properties << EOF1

index.dir=$INDEXDIR
query.file=/store/query/trec-robust.xml
qrels.file=$QRELS
res.file=/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/outputs/bm25.res
res.train=/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/data/train.res
res.test=/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/data/test.res
qpp.numtopdocs=$1
qpp.method=$METHOD

EOF1

mvn exec:java@compute_all -Dexec.args="qpp.properties"
