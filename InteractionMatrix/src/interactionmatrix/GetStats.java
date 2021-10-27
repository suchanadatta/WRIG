/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interactionmatrix;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author suchana
 */

public class GetStats {
    
    TrecDocIndexer indexer;
    IndexReader reader;
    IndexSearcher searcher;
    int numDocs;

    public GetStats(String propFile) throws Exception {
        indexer = new TrecDocIndexer(propFile);
        File indexDir = indexer.getIndexDir();
        System.out.println("Running queries against index: " + indexDir.getPath());

        reader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));
        searcher = new IndexSearcher(reader);
        
        // total number of documents in the collection
        numDocs = reader.numDocs();
        System.out.println("Total docs : " + numDocs);
    }
    
    public void getAllDF(String fieldName) throws IOException {

        Fields fields = MultiFields.getFields(reader);

        Terms terms = fields.terms(fieldName);

        TermsEnum termsEnum = terms.iterator();

        while (termsEnum.next() != null) {
            String term = termsEnum.term().utf8ToString();
            Term termInstance = new Term(fieldName, term);
            int df = reader.docFreq(termInstance);
            double idf = Math.log((double)(numDocs)/(double)(df+1));
            System.out.println(termsEnum.term().utf8ToString() + " " + (df));
        }
    }

    public double getIDF(String fieldName, String term) throws IOException {

        Term termInstance = new Term(fieldName, term);
        int df = reader.docFreq(termInstance);
        double idf = Math.log((double)(numDocs)/(double)(df+1));
        return idf;
    }

    /**
     * Given a list of terms, returns a hashmap with the term-idf as key-value pair
     * @param fieldName
     * @param terms
     * @return
     * @throws IOException 
     */
    public HashMap getAllIDF(String fieldName, List<String> terms) throws IOException {

        return getAllIDF(fieldName, new HashSet<>(terms));
    }

    /**
     * Given a set of terms, returns a hashmap with the term-idf as key-value pair
     * @param fieldName
     * @param terms
     * @return
     * @throws IOException 
     */
    public HashMap getAllIDF(String fieldName, Set<String> terms) throws IOException {

//        System.out.println("\nGetting all query term IDFs...");
        HashMap<String, Double> idfs = new HashMap<>();
        for(String term : terms) {
            Term termInstance = new Term(fieldName, term);
            int df = reader.docFreq(termInstance);
            double idf = Math.log((double)(numDocs)/(double)(df+1));
            idfs.put(term, idf);
        }
//        System.out.println("Done.");
        return idfs;
    }
}
