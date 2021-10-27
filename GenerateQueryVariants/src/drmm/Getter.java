/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package drmm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;


public class Getter {
    
    /**
     * Read the qrel file into a HashMap and return. 
     * @param qrelFile Path of the qrel file.
     * @return A HashMap with <code>qid</code> as Key and <code>KnownRelevance</code> as Value.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static HashMap<String, KnownRelevance> readQrelFile(String qrelFile) throws FileNotFoundException, IOException {

        HashMap<String, KnownRelevance> allKnownJudgement = new HashMap();

        FileInputStream fis = new FileInputStream(new File(qrelFile));

	BufferedReader br = new BufferedReader(new InputStreamReader(fis));

        String lastQid = "";
        KnownRelevance singleQueryInfo = new KnownRelevance();
	String line = null;

        System.out.println("Reading all judged documents from qrel...");
        while ((line = br.readLine()) != null) {
//            System.out.println(line);
            String qid, docid;
            int rel;
            String tokens[] = line.split("[\\s]+");
            qid = tokens[0];
            docid = tokens[2];
            rel = Integer.parseInt(tokens[3]);

            if(lastQid.equals(qid)) {
                if(rel <= 0)
                    singleQueryInfo.nonrelevant.add(docid);
                else
                    singleQueryInfo.relevant.add(docid);
            }
            else {  // a new query is read
                if(!lastQid.isEmpty()) // information about a query is there
                    allKnownJudgement.put(lastQid, singleQueryInfo);

                singleQueryInfo = new KnownRelevance();
                if(rel <= 0)
                    singleQueryInfo.nonrelevant.add(docid);
                else
                    singleQueryInfo.relevant.add(docid);
                lastQid = qid;
            }
	}

        // for the last query
        allKnownJudgement.put(lastQid, singleQueryInfo);
        System.out.println("###### : " + lastQid);

	br.close();
        System.out.println("Completed.");

        return allKnownJudgement;
    } // ends readQrelFile
    
    /**
     * Read the prerank file into a HashMap and return. 
     * @param prerankFile Path of the prerank file.
     * @return A HashMap with <code>qid</code> as Key and <code>PrerankRelevance</code> as Value.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static List<PrerankRelevance> readPrerankFile(String prerankFile) throws FileNotFoundException, IOException {

        List<PrerankRelevance> prerankData = new ArrayList<>();

        FileInputStream fis = new FileInputStream(new File(prerankFile));

	BufferedReader br = new BufferedReader(new InputStreamReader(fis));

        PrerankRelevance singleDocInfo;
	String line = null;
        
        System.out.println("Reading all pseudo relevant documents from prerank file...");
        while ((line = br.readLine()) != null) {
//            System.out.println("Line : " + line);
            singleDocInfo = new PrerankRelevance();
            
            String tokens[] = line.split("[\\s]+");
            singleDocInfo.queryid = tokens[0];
            singleDocInfo.docid = tokens[2];
            singleDocInfo.docscore = Double.parseDouble(tokens[4]);
            
            prerankData.add(singleDocInfo);            
	}
	br.close();
        System.out.println("Completed.");

        return prerankData;
    } // ends readPrerankFile

    /**
     * Returns the lucene docid of the document
     * @param docid
     * @param docidSearcher
     * @param idField
     * @return
     * @throws Exception 
     */
//    public static int getLuceneDocid(String docid, IndexSearcher docidSearcher, String idField) throws Exception {
//
//        ScoreDoc[] hits;
//        TopDocs topDocs;
//
//        TopScoreDocCollector collector = TopScoreDocCollector.create(1);
//        Query luceneDocidQuery = new TermQuery(new Term(idField, docid));
//        
//        docidSearcher.search(luceneDocidQuery, collector);
//        topDocs = collector.topDocs();
//        hits = topDocs.scoreDocs;
//        if(hits.length <= 0) {
//            System.out.println(docid+": document not found");
//            return -1;
//        }
//        else {
//            System.out.println(docid + " : " + hits[0].doc);
//            return hits[0].doc;
//        }
//    }
    
    public static ScoreDoc[] getLuceneDocid(String docid, IndexSearcher docidSearcher, String idField) throws Exception {

        ScoreDoc[] hits;
        TopDocs topDocs;

        TopScoreDocCollector collector = TopScoreDocCollector.create(1);
        Query luceneDocidQuery = new TermQuery(new Term(idField, docid));
        
        docidSearcher.search(luceneDocidQuery, collector);
        topDocs = collector.topDocs();
        hits = topDocs.scoreDocs;

        return hits;
    }
    

    /**
     * Returns the content in the field of document having luceneDocid
     * @param indexSearcher
     * @param luceneDocid
     * @param field
     * @return
     * @throws IOException 
     */
    public String getContent(IndexSearcher indexSearcher, int luceneDocid, String field) throws IOException {
        String content = indexSearcher.doc(luceneDocid).get(field);
        return content;
    }
    
    /**
     * Analyzes 'text', using 'analyzer', to be stored in 'fieldName'.
     * @param analyzer The analyzer to be used for analyzing the text
     * @param text The text to be analyzed
     * @param fieldName The name of the field in which the text is going to be stored
     * @return The analyzed text as StringBuffer
     * @throws IOException 
     */
    public static String analyzeText(Analyzer analyzer, String text, String fieldName) throws IOException {

        StringBuffer tokenizedContentBuff = new StringBuffer();

        TokenStream stream = analyzer.tokenStream(fieldName, new StringReader(text));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            tokenizedContentBuff.append(term).append(" ");
        }

        stream.end();
        stream.close();

        return tokenizedContentBuff.toString();
    } // ends analyzeText()

}

class KnownRelevance {

    String          queryid;
    List<String>    relevant;
    List<String>    nonrelevant;

    public KnownRelevance() {
        relevant = new ArrayList<>();
        nonrelevant = new ArrayList<>();
    }

    /**
     * Returns the list of relevant/non-relevant documents for a query.
     * @param relevance 0: non-relevant, Non-0: Relevant.)
     * @return A string containing list of relevant/non-relevant documents for a query.
     */
    public String toString(int relevance) {
        if(relevance <= 0)
            return nonrelevant.toString();
        else
            return relevant.toString();
    }
}

class PrerankRelevance {
    
    String    queryid;
    String    docid;
    double    docscore;
    
    public PrerankRelevance() {
        
    }
}
