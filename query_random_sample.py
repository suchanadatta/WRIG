import sys, re, os
import xml.etree.ElementTree as ET
import random


class QueryInstance:
    def __init__(self, element):
        self.qid = element[0].text
        self.qtitle = element[1].text
        self.qdesc = element[2].text
        self.qnarr = element[3].text


class RandomSample:
    def __init__(self, query, outDir, iter):
        self.query = query
        self.outDir = outDir
        self.iter = iter
        self.query_list = {}

    def get_query_dict(self):
        rootElement = ET.parse(self.query).getroot()
        for subElement in rootElement:
            query_instance = QueryInstance(subElement)
            self.query_list[query_instance.qid] = query_instance

    def get_query_samples(self):
        self.get_query_dict()
        total_query_list = list(self.query_list.keys())
        train_sample_list = random.sample(list(self.query_list.keys()), int(len(self.query_list)/2))
        print('train query set : ', train_sample_list)
        self.create_sample_file(train_sample_list, 'train')
        test_sample_list = list(set(total_query_list) - set(train_sample_list))
        print('test query set : ', test_sample_list)
        self.create_sample_file(test_sample_list, 'test')

    def create_sample_file(self, sample_list, type):
        if not os.path.exists(outDir):
            os.makedirs(outDir)
            with open(outDir + type + '-query.xml', 'w') as f:
                f.write('<topics>\n\n')
                for item in sample_list:
                    f.write('<top>\n')
                    f.write('<num>' + self.query_list.get(item).qid + '</num>\n')
                    f.write('<title>' + self.query_list.get(item).qtitle + '</title>\n')
                    f.write('<desc>' + self.query_list.get(item).qdesc + '</desc>\n')
                    f.write('<narr>' + self.query_list.get(item).qnarr + '</narr>\n')
                    f.write('</top>\n\n')
                f.write('</topics>')
        else:
            with open(outDir + type + '-query.xml', 'w') as f:
                f.write('<topics>\n\n')
                for item in sample_list:
                    f.write('<top>\n')
                    f.write('<num>' + self.query_list.get(item).qid + '</num>\n')
                    f.write('<title>' + self.query_list.get(item).qtitle + '</title>\n')
                    f.write('<desc>' + self.query_list.get(item).qdesc + '</desc>\n')
                    f.write('<narr>' + self.query_list.get(item).qnarr + '</narr>\n')
                    f.write('</top>\n\n')
                f.write('</topics>')


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print ('usage: python query_random_sample.py '
               '<query file>'
               '<sampled output file path>'
               '<no. of 50:50 random splits')
        sys.exit(0)
    query = sys.argv[1]
    outFilePath = sys.argv[2]
    numSplits = int(sys.argv[3])

    for i in range(1, numSplits+1):
        outDir = outFilePath + 'query_sample_' + str(i) +'/'
        print('query splits will be stored in : ', outDir)
        RandomSample(query, outDir, i).get_query_samples()
    print('=========== Created 50:50 splits - ' + str(numSplits) + ' times ============\n')

