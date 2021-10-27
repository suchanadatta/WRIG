/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package neuralqpp;

import static neural.common.CommonVariables.FIELD_BOW;
import static neural.common.CommonVariables.FIELD_FULL_BOW;
import neural.common.EnglishAnalyzerSmartStopWords;
import neural.common.TRECQuery;
import neural.common.TRECQueryParse;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author suchana
 */

public class GenerateQueryVariantsClueWeb {
    
    Properties       prop;
    String           stopFilePath;
    Analyzer         analyzer;
    String           indexPath;
    File             indexFile;
    boolean          boolIndexExists;
    String           fieldToSearch; 
    String           fieldForFeedback;
    int              simFuncChoice;
    float            param1, param2;
    IndexReader      indexReader;
    IndexSearcher    indexSearcher;
    String           queryPath;
    File             queryFile;
    TRECQueryParse   trecQueryparser;
    List<TRECQuery>  queries;
    String           qtermNN;
    String           varGen;
    int              varLen;
    FileWriter       resFileWriter;
    String           runName;
    String           resPath;
    int              numFeedbackDocs;
    CreateQueryVariantsRLM qvrlm;
    CreateQueryVariantsW2V qvwv;
    
    
    public GenerateQueryVariantsClueWeb(Properties prop) throws IOException, Exception {

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
        fieldForFeedback = prop.getProperty("fieldForFeedback", FIELD_BOW);
        //System.out.println("Searching field for retrieval: " + fieldToSearch);
        //System.out.println("Field for Feedback: " + fieldForFeedback);
        /* index path set */

        simFuncChoice = Integer.parseInt(prop.getProperty("similarityFunction"));
        if (null != prop.getProperty("param1"))
            param1 = Float.parseFloat(prop.getProperty("param1"));
        if (null != prop.getProperty("param2"))
            param2 = Float.parseFloat(prop.getProperty("param2"));

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
        
        /* constructing TREC queries */
        trecQueryparser = new TRECQueryParse(queryPath, analyzer, fieldToSearch);
        queries = constructQueries();
        /* constructed TREC query */
        
        qtermNN = prop.getProperty("qtermNN");
        System.out.println("Nearest Neighbour path set to : " + qtermNN);
        
        /* numFeedbackDocs = number of top documents to select */
        numFeedbackDocs = Integer.parseInt(prop.getProperty("numFeedbackDocs"));
        
        /* choose the variant generation method */
        varGen = prop.getProperty("variantGenerate");
        System.out.println("Generate query variants using : " + varGen);
        
        /* choose the maximum length of a variant */
        varLen = Integer.parseInt(prop.getProperty("variantLength"));
        System.out.println("Maximum length of a query variant : " + varLen);
        
//        qvrlm = new CreateQueryVariantsRLM(this);
        qvwv = new CreateQueryVariantsW2V(this);
        
        /* setting res path */
        setRunName_ResFileName();
        resFileWriter = new FileWriter(resPath.trim());
        System.out.println("Result will be stored in: "+resPath);
        /* res path set */     
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
    
    
    /**
     * Parses the query from the file and makes a List<TRECQuery> 
     *  containing all the queries (RAW query read)
     * @return A list with the all the queries
     * @throws Exception 
     */
    private List<TRECQuery> constructQueries() throws Exception {

        trecQueryparser.queryFileParse();
        return trecQueryparser.queries;
    } // ends constructQueries()
    
    
    /**
     * Sets runName and resPath variables depending on similarity functions.
     */
    private void setRunName_ResFileName() {
        
        Similarity s = indexSearcher.getSimilarity(true);
        runName = queryFile.getName() + "-" + s.toString() + "-TD" + numFeedbackDocs + "-" + varGen.toUpperCase();
        runName = runName.replace(" ", "").replace("(", "").replace(")", "").replace("00000", "");

        if(null == prop.getProperty("resPath"))
            resPath = "/home/suchana/";
        else
            resPath = prop.getProperty("resPath");
        resPath = resPath + runName + ".variants";
    } // ends setRunName_ResFileName()
    
    public List<String> getDocumentVector(int luceneDocId, String fieldName, IndexReader indexReader) throws IOException {

        int docSize = 0;

        if(indexReader==null) {
            System.out.println("Error: null == indexReader in showDocumentVector(int,IndexReader)");
            System.exit(1);
        }

        // t vector for this document and field, or null if t vectors were not indexed
        Terms terms = indexReader.getTermVector(luceneDocId, fieldName);
        if(null == terms) {
            System.err.println("Error getDocumentVector(): Term vectors not indexed: "+luceneDocId);
            return null;
        }

        TermsEnum iterator = terms.iterator();
        BytesRef byteRef = null;

        List<String> all_terms = new ArrayList<>();
        //* for each word in the document
        while((byteRef = iterator.next()) != null) {
            String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
            all_terms.add(term);
            int docFreq = iterator.docFreq();            // df of 't'
            long termFreq = iterator.totalTermFreq();    // tf of 't'
            //System.out.println(t+": tf: "+termFreq);
            docSize += termFreq;
            //* termFreq = cf, in a document; df = 1, in a document
        }
//        System.out.println("Individual : " + all_terms);
        return all_terms;
    }    
    
    public void makeQueryVariants() throws Exception {
        
        TopDocs topRetDocs;
        ScoreDoc[] hits;
        TopScoreDocCollector collector;
        List<String> queryVariantList = new ArrayList<>();
        List<String> topRetTerms;
        
        for (TRECQuery query : queries) {
            
            collector = TopScoreDocCollector.create(numFeedbackDocs);
            Query luceneQuery = trecQueryparser.getAnalyzedQuery(query);
            System.out.println("\n" + query.qid +": Initial query: " + luceneQuery.toString(fieldToSearch));
            
            /* initial retrieval performed */
            indexSearcher.search(luceneQuery, collector);
            topRetDocs = collector.topDocs();
            hits = topRetDocs.scoreDocs;
            if(hits == null)
                System.out.println("Nothing found");

            int hits_length = hits.length;
            System.out.println("Total docs retrieved : " + topRetDocs.totalHits + "\tSelected top docs : " + hits_length);
            
            topRetTerms = new ArrayList<>();
            StringBuffer resBuffer;            
            resFileWriter = new FileWriter(resPath, true);
            
            for (int i = 0; i < hits_length; ++i) {
                int docId = hits[i].doc;
//                System.out.println("lucenedoc-id : " + docId);
                topRetTerms.addAll(getDocumentVector(docId, fieldToSearch, indexReader));
//                System.out.println("ekhane : " + topRetTerms);
            }

            /* res file in TREC format with doc text (7 columns) */
//            resBuffer = new StringBuffer();
//            for (int i = 0; i < hits_length; ++i) {
//                int docId = hits[i].doc;
//                Document d = indexSearcher.doc(docId);
//                resBuffer.append(query.qid).append("\tQ0\t").
//                append(d.get(FIELD_ID)).append("\t").
//                append((i+1)).append("\t").
//                append(hits[i].score).append("\t").
//                append(runName).append("\n");
////                append(d.get(FIELD_BOW)).append("\n");
//            }
//            resFileWriter.write(resBuffer.toString());
//            resFileWriter.close();
            
            switch(varGen.toLowerCase()) {
                case "rlm":
                    System.out.println("Generate variants using - RLM");
                    resBuffer = new StringBuffer();
                    queryVariantList = qvrlm.createVariantsRLMClueWeb(topRetTerms, luceneQuery, varLen, topRetDocs);
                    resBuffer.append(query.qid).append(" ::: ").append(luceneQuery.toString(fieldToSearch)).append("\n");
                    for (String variant : queryVariantList)
                        resBuffer.append(query.qid).append("\t").
                                append(variant).append("\n");
                    resFileWriter.write(resBuffer.toString());
                    resFileWriter.close();
                    break;
                case "w2v":
                    System.out.println("Generate variants using - W2V");
                    resBuffer = new StringBuffer();
                    queryVariantList = qvwv.createVariantsW2VClueWeb(luceneQuery, varLen);
                    resBuffer.append(query.qid).append(" ::: ").append(luceneQuery.toString(fieldToSearch)).append("\n");
                    for (String variant : queryVariantList)
                        resBuffer.append(query.qid).append("\t").
                                append(variant).append("\n");
                    resFileWriter.write(resBuffer.toString());
                    resFileWriter.close();
                    break;                 
            }
        } // ends for each query
    }
            
    
    public static void main(String[] args) throws IOException, Exception {

        String usage = "java GenerateQueryVariants <properties-file>\n"
                + "Properties file must contain the following fields:\n"
                + "1. Path of the index.\n"
                + "2. Path of the TREC query.xml file.\n"
                + "3. Path of the directory to store res file.\n"
                + "4. SimilarityFunction: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity.\n"
                + "5. Path of the Nearest Neighbour file (valid path if variants to be generated using W2V; otherwise 0).\n"
                + "6. No. of feedback documents to be used\n"
                + "7. Select variants generation method : 1. RLM, 2. W2V (input in text i.e - rlm/w2v)";               
                
        Properties prop = new Properties();

        if(1 != args.length) {
            System.out.println("Usage: " + usage);
            args = new String[1];
            args[0] = "generateQueryVariants-query.xml.properties";
            System.exit(1);
        }
        prop.load(new FileReader(args[0]));
        GenerateQueryVariantsClueWeb gqvc = new GenerateQueryVariantsClueWeb(prop);

        gqvc.makeQueryVariants();         
    } // ends main()     
}
