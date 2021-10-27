/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package knrm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;


/**
 *
 * @author suchana
 */

public class CreateMatrixFromQrel {
    
    KNRMDataGenerator knrmdg;
    WordVecs          wvecs;
    IndexReader       indexReader;
    IndexSearcher     indexSearcher;
    String            embeddingPath;
    String            qrelPath;
    HashMap<String, Double> tfIdfMap;
    HashMap<String, String> queryHashMap;
    HashMap<String, Double> qtermIdfMap;
    int               totalDocs;
    int               docMaxLength;
    File              embedding;
    FileWriter        resFileWriter;
    StringBuffer      resbuffer;
           
    
    public CreateMatrixFromQrel(KNRMDataGenerator knrmdg) throws IOException {
        
        this.knrmdg = knrmdg;
        this.indexReader = knrmdg.indexReader;
        this.indexSearcher = knrmdg.indexSearcher;
        this.embeddingPath = knrmdg.embeddingPath;
        this.qrelPath = knrmdg.qrelPath;
        this.docMaxLength = knrmdg.docMaxLength;
        this.queryHashMap = knrmdg.queryHashMap;
        this.resFileWriter = knrmdg.resFileWriter;
        embedding = new File(embeddingPath.trim());
        wvecs = new WordVecs(knrmdg, this);
        qtermIdfMap = new HashMap<String, Double>();
    }
    
    
    private static TopDocs searchById(String content, IndexSearcher searcher) throws Exception {
        
        QueryParser qp = new QueryParser("docidanalyze", new StandardAnalyzer());
        Query idQuery = qp.parse(content);
//        System.out.println("QP : " + idQuery);
        TopDocs hits = searcher.search(idQuery, 10);
        return hits;
    }
    
    
    public double getIdf(String term) throws IOException {
        
        Fields fields = MultiFields.getFields(indexReader);
        Term termInstance = new Term("content", term);
        long df = indexReader.docFreq(termInstance);       // DF: Returns the number of documents containing the term
        double idf = Math.log((float)(totalDocs)/(float)(df+1));
//        System.out.println("IDF value : " + idf);
        return idf;
    }
    
    
    public double getTf(String term, String[] doc) {
        
        double count = 0;            
        
        // calculate term frequency
        for (String s : doc) {
            if (s.equalsIgnoreCase(term))
                count++;
        }
//        System.out.println("COUNT : " + count);
//        System.out.println("doc length : " + doc.length);
        double tf = count / (double)doc.length;
//        System.out.println("TF value : " + tf);
        return tf;
    }
    
    
    public HashMap<String, Double> calculateTfIdfScore(String docid) throws Exception{
        
        HashMap<String, Double> tfIdfMap = new HashMap<>();
        int docSize;
        String doc = null;
                 
        // Search by analyzed docid
        TopDocs docsRet = searchById(docid, indexSearcher);
        System.out.println("No. of retrieved document :: " + docsRet.totalHits);
        for (ScoreDoc sd : docsRet.scoreDocs) {
            Document d = indexSearcher.doc(sd.doc);
            doc = String.format(d.get("content"));
//            System.out.println(doc);
        }
        
        if(doc != null){
            // total no. of terms in the document
            docSize = doc.split(" ").length;
            String[] totalTerms = doc.split(" ");

            // calculate tf-idf of each term in the doc
            for(String term : totalTerms){
    //            System.out.println("Current term : " + term);
                double tf = getTf(term, totalTerms);
                double idf = getIdf(term);  
                tfIdfMap.put(term, tf*idf);
            }
            return tfIdfMap;
        }
        else
            return null;
    }
    
    
    public void getCosineSimilarity(String queryID, List<String> topTerms, HashMap<String, String> queryHashMap, 
            String judge, String docId) throws IOException {
        
        resbuffer = new StringBuffer();
        System.out.println("terms chosen for cosine-sim : " + topTerms);
        int count, count1;
        float dotProduct, magnitude1, magnitude2, cosineSimilarity = 0;
//        System.out.println("query id : " + queryID); 
        
        String[] qTerms  = queryHashMap.get(queryID).split(" ");
        resbuffer.append(queryID).append(" ").append(docId).append(" ").
                        append(judge).append(" ").append(qTerms.length).append(" ");
//        for(int i = 0; i < qTerms.length; i++)
//            resbuffer.append(" ").append("1");
        
        for(String qTerm : qTerms){
            WordVec wvt = wvecs.wordvecmap.get(qTerm.trim());
            if(wvt == null){
                cosineSimilarity = 0;
                count = 0;
                while(count < docMaxLength){
                    resbuffer.append(cosineSimilarity).append(" ");
                    count++;
                }
            }
            else{
                String queryTerm = wvt.word;
                float[] queryVec = wvt.vec;
                for(String dTerm : topTerms) {
                    dotProduct = 0;
                    magnitude1 = 0;
                    magnitude2 = 0;
                    cosineSimilarity = 0;
                    WordVec wvd = wvecs.wordvecmap.get(dTerm.trim());
                    if(wvd != null) {
                        String docTerm = wvd.word;
                        float[] docVec = wvd.vec;
                        for (int i = 0; i < queryVec.length; i++) {
                            dotProduct += queryVec[i] * docVec[i];  //a.b
    //                        System.out.println("dot product : " + dotProduct);
                            magnitude1 += Math.pow(queryVec[i], 2);  //(a^2)
    //                        System.out.println("mag 1 : " + magnitude1);
                            magnitude2 += Math.pow(docVec[i], 2); //(b^2)
    //                        System.out.println("mag 2 : " + magnitude2);
                        }

                        magnitude1 = (float)Math.sqrt(magnitude1);//sqrt(a^2)
                        magnitude2 = (float)Math.sqrt(magnitude2);//sqrt(b^2)

                        if (magnitude1 != 0 | magnitude2 != 0) {
                            cosineSimilarity = dotProduct / (magnitude1 * magnitude2);
                        } 
                        else {
                            cosineSimilarity = 0;
                        }
                    }
                    else {
                        cosineSimilarity = 0;
                    }
                    System.out.println("qt : " + qTerm + "\tdt : " + dTerm + "\tcosim : " + cosineSimilarity);
                    resbuffer.append(cosineSimilarity).append(" ");
                } 
            }
            if(topTerms.size() < docMaxLength){
                cosineSimilarity = 0;
                count = 0;
                while(count < docMaxLength - topTerms.size()){
                    resbuffer.append(cosineSimilarity).append(" ");
                    count++;
                }
            }
        }
        if (qTerms.length < 5) {  //5 is max query length here, should be taken through command line
            count1 = 0;
            while (count1 < (5 - qTerms.length)) {
                cosineSimilarity = 0;
                count = 0;
                while(count < docMaxLength){
                    resbuffer.append(cosineSimilarity).append(" ");
                    count++;
                }
                count1++;                
            }
        }
        resbuffer.append("\n");
        resFileWriter.write(resbuffer.toString());
    }
    
    
    public void createSimilarityMatrix(HashMap<String, String> queryHashMap, int queryMaxLength) throws FileNotFoundException, IOException, Exception {
        
        List<String> topScoreTerms;  
        File qrelPFile = new File(qrelPath.trim());
        
        totalDocs = indexReader.maxDoc();
        System.out.println("total no. of docs in the collection : " + totalDocs);
        
        // create qterms idf map (needed only DRMM)
//        for(Map.Entry<String, String> qtermSet : queryHashMap.entrySet()) {
//            String qterms[] = qtermSet.getValue().split(" ");
//            for(String oneTerm : qterms) {
//                qtermIdfMap.put(oneTerm, getIdf(oneTerm));
//            }
//        }
               
        // load embedding
        wvecs.loadFromTextFile();
        System.out.println("embedding size : " + wvecs.wordvecmap.size());
        
        // read qrel file line by line
        BufferedReader br = new BufferedReader(new FileReader(qrelPFile));
        String line = br.readLine();
        while(line != null){ 
            String[] qrelValues = line.split(" ");
            System.out.println("======= for query id : " + qrelValues[0] + "========");
            // ================== for debugging ============
//            if(Integer.parseInt(qrelValues[0]) > 328){
                String docid = qrelValues[2].toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
                tfIdfMap = new HashMap<>();
                tfIdfMap = calculateTfIdfScore(docid);
    //            System.out.println("map : " + tfIdfMap.entrySet());
                if(tfIdfMap != null){
                    // sort the tf-idf map in descending order
                    List<Entry<String, Double>> listOfEntry = new LinkedList<>(tfIdfMap.entrySet());               
                    //Sort listOfEntry using Collections.sort() by passing customized Comparator         
                    Collections.sort(listOfEntry, (Entry<String, Double> o1, Entry<String, Double> o2) -> o2.getValue().compareTo(o1.getValue()));         
                    //Insert all elements of listOfEntry into new LinkedHashMap which maintains insertion order         
                    Map<String, Double> sortedMap = new LinkedHashMap<>();         
                    listOfEntry.stream().forEach((entry) -> {
                        sortedMap.put(entry.getKey(), entry.getValue());
                    });

                    // calculate query-doc term cosine similarity
                    int count = 0;
                    topScoreTerms = new ArrayList<>();
        //            System.out.println("SORTED : " + sortedMap.entrySet());
                    for(Map.Entry<String, Double> termSet : sortedMap.entrySet()){
        //                System.out.println("&&&& : " + termSet.getKey());
                        if(count < Math.min(sortedMap.size(), docMaxLength)) // need to correct//
                            topScoreTerms.add(termSet.getKey());
                        count++;
                    } 
        //            System.out.println("terms chosen for cosine-sim : " + topScoreTerms);
                    if(queryHashMap.get(qrelValues[0].trim()) != null) // ensures no doc is included from test set
                        getCosineSimilarity(qrelValues[0], topScoreTerms, queryHashMap, qrelValues[3], qrelValues[2]);
                }
//            }
            line = br.readLine(); 
        }
        resFileWriter.close();
    }    
}
