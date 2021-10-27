import sys, re, os
from random import randint
import xml.etree.ElementTree as ET


if len(sys.argv) < 6:
    print('Needs 4 arguments -\n1. the qrel file\n'
                               '2. the preranked file\n'
                               '3. train query file\n'
                               '4. test query file\n'
                               '5. train-test fold path\n'
                               '6. split number')
    exit(0)

qrel_file = sys.argv[1]
prerank_file = sys.argv[2]
train_query = sys.argv[3]
test_query = sys.argv[4]
fold_path = sys.argv[5]
split_no = sys.argv[6]

#
# load qrels file
#
qrels_doc_pairs = {} # topic -> (relevant: [docId], non-relevant: [docId]) (, doc id, relevant) relevant is boolean
count_rel = 0
count_non_rel = 0

train_query_list = []
test_query_list = []

with open(qrel_file, 'r') as inputFile:
    for line in inputFile:
        parts = line.split()
        if parts[0] not in qrels_doc_pairs:
            qrels_doc_pairs[parts[0]] = ([], [])
        if float(parts[3].strip()) > 0:
            qrels_doc_pairs[parts[0]][0].append(parts[2].strip())
            count_rel+=1
        else:
            qrels_doc_pairs[parts[0]][1].append(parts[2].strip())
            count_non_rel+=1

print(len(qrels_doc_pairs), ' topics loaded')
print(count_rel, ' relevant docs loaded')
print(count_non_rel, ' non-relevant docs loaded')


def get_query_dict(query_train, query_test):
    rootElement_train = ET.parse(query_train).getroot()
    rootElement_test = ET.parse(query_test).getroot()
    for subElement in rootElement_train:
        train_query_list.append(subElement[0].text.strip())
    for subElement in rootElement_test:
        test_query_list.append(subElement[0].text.strip())

get_query_dict(train_query, test_query)
print(train_query_list)
print(test_query_list)

#
# load pre-ranked file
#
prerank_doc_pairs = {} # topic -> [docId]
count_prerank = 0
with open(prerank_file, 'r') as inputFile:
    for line in inputFile:
        parts = line.split()
        if parts[0] not in prerank_doc_pairs:
            prerank_doc_pairs[parts[0]] = []
        prerank_doc_pairs[parts[0]].append(parts[2].strip())
        count_prerank+=1
print(count_prerank, ' pre-ranked docs loaded')
# print("PRERANK DOC PAIRS : ", prerank_doc_pairs)


def create_1_0_pairs(topics):
    lines = []
    for topic in topics:
        for positive in qrels_doc_pairs[topic][0]:
            i = randint(0, len(qrels_doc_pairs[topic][1])-1)
            lines.append(topic + ' ' + positive + ' ' + qrels_doc_pairs[topic][1][i] + '\n')
    print('\t got  ', len(lines), 'train pairs')
    return lines


def writeOutFiles(train, test):
    with open(fold_path + 'drmm_fold' + split_no + '.train', 'w') as trainFile:
        trainFile.writelines(create_1_0_pairs(train))
    with open(fold_path + 'drmm_fold' + split_no + '.test', 'w') as testFile:
        lines = []
        for topic in test:
            for entry in prerank_doc_pairs[topic]:
                lines.append(topic + ' ' + entry + '\n')
        print('\t got  ', len(lines), 'test docs')
        testFile.writelines(lines)

if not os.path.exists(fold_path):
    os.makedirs(fold_path)
# train and test folds
writeOutFiles(train_query_list, test_query_list)
