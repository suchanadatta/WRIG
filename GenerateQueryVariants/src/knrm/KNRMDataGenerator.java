/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package knrm;

import static neural.common.CommonVariables.FIELD_FULL_BOW;
import neural.common.EnglishAnalyzerSmartStopWords;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author suchana
 */

public class KNRMDataGenerator {
    
    Properties         prop;
    String             indexPath;
    File               indexFile;
    boolean            boolIndexExists;
    String             queryPath;
    File               queryFile;
    String             qrelPath;
    String             embeddingPath;
    String             resPath;
    String             stopFilePath;
    String             fieldToSearch;
    IndexReader        indexReader;
    IndexSearcher      indexSearcher;
    String             qrelPrerank;
    Analyzer           analyzer;
    List<String>       stopWordList;
    HashMap<String, String> queryHashMap;
    int                queryMaxLength;
    int                docMaxLength;
    int                simFuncChoice;
    float              param1, param2;
    HashMap<String, List<Double>> embeddingHashMap;
    FileWriter         resFileWriter;
    CreateMatrixFromQrel cmfq;
    CreateMatrixFromPrerank cmfp;
    
    
    public KNRMDataGenerator(Properties prop) throws IOException {
        
        this.prop = prop;
        /* property file loaded */
        
        /* setting the analyzer with English Analyzer with Smart stopword list */
        EnglishAnalyzerSmartStopWords engAnalyzer;
        stopFilePath = prop.getProperty("stopFilePath");
        if (null == stopFilePath)
            engAnalyzer = new neural.common.EnglishAnalyzerSmartStopWords();
        else
            engAnalyzer = new neural.common.EnglishAnalyzerSmartStopWords(stopFilePath);
        analyzer = engAnalyzer.setAndGetEnglishAnalyzerWithSmartStopword();
        /* analyzer set: analyzer */
        
        /* index path setting */
        indexPath = prop.getProperty("indexPath");
        System.out.println("indexPath set to: " + indexPath);
        indexFile = new File(prop.getProperty("indexPath"));
        Directory indexDir = FSDirectory.open(indexFile.toPath());

        if (!DirectoryReader.indexExists(indexDir)) {
            System.err.println("Index doesn't exists in "+indexPath);
            boolIndexExists = false;
            System.exit(1);
        }
        fieldToSearch = prop.getProperty("fieldToSearch", FIELD_FULL_BOW);
        System.out.println("Searching field for retrieval: " + fieldToSearch);
        /* index path set */
        
        /* setting indexReader and indexSearcher */
        indexReader = DirectoryReader.open(FSDirectory.open(indexFile.toPath()));
        indexSearcher = new IndexSearcher(indexReader);
        setSimilarityFunction(simFuncChoice, param1, param2);
        /* indexReader and searcher set */
        
        /* setting query path */
        queryPath = prop.getProperty("queryPath");
        System.out.println("queryPath set to: " + queryPath);
        queryFile = new File(queryPath);
        /* query path set */
        
        /* setting qrel path */
        qrelPath = prop.getProperty("qrelPath");
        System.out.println("qrelPath/prerank set to: " + qrelPath);
        /* qrel path set */
        
        /* setting embedding path */
        embeddingPath = prop.getProperty("embeddingPath");
        System.out.println("embeddingPath set to: " + embeddingPath);
        /* embedding path set */
        
        /* choose the qrel/prerank file */
        qrelPrerank = prop.getProperty("qrelPrerank");
        if (qrelPrerank.equalsIgnoreCase("qrel"))
            System.out.println("File using for training : " + qrelPrerank);
        else
            System.out.println("File using for testing : " + qrelPrerank);
        
        /* setting maximum terms to chosse from doc for the metrix */
        docMaxLength = Integer.parseInt(prop.getProperty("docMaxLength"));
        System.out.println("Maximum terms will be chosen from document : " + docMaxLength);
        
        /* setting res path */
        if(null == prop.getProperty("resPath"))
            resPath = "/home/suchana/";
        else
            resPath = prop.getProperty("resPath");
        resPath = resPath + "knrm-" + queryFile.getName() + "-term" + docMaxLength + "-" + qrelPrerank + ".matrix";
        resFileWriter = new FileWriter(resPath.trim());
        System.out.println("Result will be stored in: "+resPath);
        /* res path set */ 
        
        cmfq = new CreateMatrixFromQrel(this);
//        cmfp = new CreateMatrixFromPrerank(this);
    }
    
    
    /**
     * Sets indexSearcher.setSimilarity() with parameter(s)
     * @param choice similarity function selection flag
     * @param param1 similarity function parameter 1
     * @param param2 similarity function parameter 2
     */
    private void setSimilarityFunction(int choice, float param1, float param2) {

            switch(choice) {
            case 0:
                indexSearcher.setSimilarity(new DefaultSimilarity());
                System.out.println("Similarity function set to DefaultSimilarity");
                break;
            case 1:
                indexSearcher.setSimilarity(new BM25Similarity(param1, param2));
                System.out.println("Similarity function set to BM25Similarity"
                    + " with parameters: " + param1 + " " + param2);
                break;
            case 2:
                indexSearcher.setSimilarity(new LMJelinekMercerSimilarity(param1));
                System.out.println("Similarity function set to LMJelinekMercerSimilarity"
                    + " with parameter: " + param1);
                break;
            case 3:
                indexSearcher.setSimilarity(new LMDirichletSimilarity(param1));
                System.out.println("Similarity function set to LMDirichletSimilarity"
                    + " with parameter: " + param1);
                break;
        }
    } // ends setSimilarityFunction()
    
    
    public void createQueryHashMap(String queryFile) throws FileNotFoundException, IOException {
        
        String fileContent, qid, qtext = null, analyzedQuery = null; 
        Pattern p_qid, p_qtext;
        Matcher m_qid, m_qtext;
        queryHashMap = new HashMap<>();
        
        System.out.println("Create query hash map from this file : " + queryFile);
        File queryMap = new File(queryFile.trim());
        BufferedReader br = new BufferedReader(new FileReader(queryMap));
        String line = br.readLine();
        StringBuilder sb = new StringBuilder(); 
        while(line != null){ 
            sb.append(line).append("\n");
            line = br.readLine();
        } 
        fileContent = sb.toString();
        fileContent = fileContent.replaceAll("\"", "").replaceAll("", "").replaceAll("\n", "").replaceAll("\r", "");
//        System.out.println("file content : " + fileContent);
        
        p_qid = Pattern.compile("<num>(.+?)</num>");
        m_qid = p_qid.matcher(fileContent);
        p_qtext = Pattern.compile("<title>(.+?)</title>");
        m_qtext = p_qtext.matcher(fileContent);
        
        while (m_qid.find()) {
            
            qid = m_qid.group(1).trim().replaceAll("\\s{2,}", "");
            System.out.print("query id : " + qid + "\t");
            
            if (m_qtext.find()){
                qtext = m_qtext.group(1).trim().replaceAll("\\s{2,}", " ");
//                System.out.println("parsed query : " + qtext);
                analyzedQuery = analyzeText(analyzer, qtext, "qtext").toString().trim().replaceAll("\\s{2,}", " ").replace(".", "");
                System.out.println("analyzed query : " + analyzedQuery);
            }
            queryHashMap.put(qid, analyzedQuery);
//            System.out.println("query map size : " + queryHashMap.size());
        }
//        System.out.println("query...." + queryHashMap.get("401"));
    }
    
    
    public static StringBuffer analyzeText(Analyzer analyzer, String text, String fieldName) throws IOException {

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

        return tokenizedContentBuff;
    }
    
    
    public int getMaxQueryLength(HashMap<String, String> queryMap) {
        
        int queryMaxlen = 0, currLength;
        
        for(Map.Entry<String, String> query : queryHashMap.entrySet()) {
            currLength = query.getValue().split(" ").length;
            if (currLength > queryMaxlen) 
                queryMaxlen = currLength;
        }
        return queryMaxlen;        
    }

    
    public void createKnrmSimMatrix() throws IOException, Exception {
        
        // create the <qid, query> hashmap
        createQueryHashMap(queryPath);
        System.out.println("query map size : " + queryHashMap.size());
        
        // calculate the maximum length of the query, used for padding
        queryMaxLength = getMaxQueryLength(queryHashMap);
        System.out.println("Maximum query length : " + queryMaxLength);
        
        switch(qrelPrerank.toLowerCase()) {
            case "qrel":
                System.out.println("==== Generate query-doc similarity matrix for training ====");
                cmfq.createSimilarityMatrix(queryHashMap, queryMaxLength);
                break;
            case "prerank":
                System.out.println("==== Generate query-doc similarity matrix for testing ====");
//                cmfp.createSimilarityMatrix();
                break;                 
            }        
    }   
    
    
    public static void main(String[] args) throws IOException, Exception {

        String usage = "java KNRMDataGenerator <properties-file>\n"
                + "Properties file must contain the following fields:\n"
                + "1. Path of the index.\n"
                + "2. Path of the TREC query.xml file.\n"
                + "3. Path of the qrel(train)/prerank(test) file.\n"
                + "4. Path of the w2v embedding file.\n"
                + "5. Path to store the data generator file.\n"
                + "6. Choose (1) for training or (2) for tesing : 1. qrel or 2. prerank (input in text - qrel/prerank).\n"
                + "7. Maximum no. of terms to choose from document."; 
        
        Properties prop = new Properties();
        if(1 != args.length) {
            System.out.println("Usage: " + usage);
            args = new String[1];
            args[0] = "generateQueryVariants-query.xml.properties";
            System.exit(1);
        }
        
        prop.load(new FileReader(args[0]));
        KNRMDataGenerator dataGenerate = new KNRMDataGenerator(prop);
        dataGenerate.createKnrmSimMatrix();
    }
}
