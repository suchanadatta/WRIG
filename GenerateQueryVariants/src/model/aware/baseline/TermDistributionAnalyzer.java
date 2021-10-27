/*
 * Reference : https://dl.acm.org/doi/10.1145/3209978.3210041
 * Creates the input for the second component - Term Distribution Analyzer
 */

package model.aware.baseline;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import static neural.common.CommonVariables.FIELD_BOW;
import static neural.common.CommonVariables.FIELD_FULL_BOW;
import neural.common.DocumentVector;
import neural.common.EnglishAnalyzerSmartStopWords;
import neural.common.PerTermStat;
import neural.common.TRECQuery;
import neural.common.TRECQueryParse;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
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

public class TermDistributionAnalyzer {
    
    Properties       prop;
    String           stopFilePath;
    Analyzer         analyzer;
    String           indexPath;
    File             indexFile;
    boolean          boolIndexExists;
    String           fieldToSearch;
    String           fieldForFeedback; // field, to be used for feedback
    int              simFuncChoice;
    float            param1, param2;
    IndexReader      indexReader;
    IndexSearcher    indexSearcher;
    String           queryPath;
    File             queryFile;
    TRECQueryParse   trecQueryparser;
    List<TRECQuery>  queries;
    FileWriter       resFileWriter;
    String           runName;
    String           resPath;
    int              numTopDocs;
    int              numTopTerms;
    long             docCount;     // number of documents in the collection
    float            mixingLambda; // mixing weight, used for doc-col weight distribution
    long             vocSize;      // vocabulary size
    
    /**
     * Hashmap of Vectors of all feedback documents, keyed by luceneDocId.
     */
    HashMap<Integer, DocumentVector> feedbackDocumentVectors;
    /**
     * HashMap of cumulative count of all feedback terms, keyed by the term.
     */
    HashMap<String, Long> topTermStats;
    /**
     * HashMap of PerTermStat of all feedback terms, keyed by the term.
     */
    HashMap<String, PerTermStat> feedbackTermStats;
    
    
    public TermDistributionAnalyzer(Properties prop) throws IOException, Exception {

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
        
        docCount = indexReader.maxDoc();        // total number of documents in the index
//        System.out.println("Total no. of docs in the collection : " + docCount);
        
        /* setting query path */
        queryPath = prop.getProperty("queryPath");
        System.out.println("queryPath set to: " + queryPath);
        queryFile = new File(queryPath);
        /* query path set */
        
        /* constructing TREC queries */
        trecQueryparser = new TRECQueryParse(queryPath, analyzer, fieldToSearch);
        queries = constructQueries();
        /* constructed TREC query */
        
        /* numTopDocs = number of top documents to select */
        numTopDocs = Integer.parseInt(prop.getProperty("numTopDocs"));
        
        /* numFeedbackTerms = number of top terms to select */
        numTopTerms = Integer.parseInt(prop.getProperty("numTopTerms"));
        
        /* setting mixing Lambda */
        if(param1>0.99)
            mixingLambda = 0.6f; // suggested in the reference paper
        else
            mixingLambda = param1;
        
        vocSize = getVocabularySize();
        
        /* setting res path */
        resPath = prop.getProperty("resPath");
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
     * Returns the vocabulary size of the collection for the field 'fieldForFeedback'.
     * @return vocSize : Total number of terms in the vocabulary
     * @throws IOException IOException
     */
    private long getVocabularySize() throws IOException {

        Fields fields = MultiFields.getFields(indexReader);
        Terms terms = fields.terms(fieldForFeedback);
        if(null == terms) {
            System.err.println("Field: "+fieldForFeedback);
            System.err.println("Error buildCollectionStat(): terms Null found");
        }
        vocSize = terms.getSumTotalTermFreq();  // total number of terms in the index in that field

        return vocSize;                         // total number of terms in the index in that field
    }
    
    /**
     * Sets the following variables with feedback statistics: to be used consequently.<p>
     * {@link #feedbackDocumentVectors},<p> 
     * {@link #feedbackTermStats}, <p>
     * @param topDocs
     * @param analyzedQuery
     * @throws IOException 
     */
    public void setFeedbackStats(TopDocs topDocs, String[] analyzedQuery) throws IOException {

        feedbackDocumentVectors = new HashMap<>();
        feedbackTermStats = new HashMap<>();
        ScoreDoc[] hits;
        int hits_length;
        hits = topDocs.scoreDocs;
        hits_length = hits.length;  // number of documents retrieved in the first retrieval
        
        for (int i = 0; i < Math.min(numTopDocs, hits_length); i++) {
            // for each feedback document
            int luceneDocId = hits[i].doc;
            Document d = indexSearcher.doc(luceneDocId);
            DocumentVector docV = new DocumentVector(fieldForFeedback);
            docV = docV.getDocumentVector(luceneDocId, indexReader);
            if(docV == null)
                continue;
            feedbackDocumentVectors.put(luceneDocId, docV); // the feedback document vector is added in the list
            
            for (Map.Entry<String, PerTermStat> entrySet : docV.docPerTermStat.entrySet()) {
            // for each term of that feedback document
                String key = entrySet.getKey();
                PerTermStat value = entrySet.getValue();
                
                if(null == feedbackTermStats.get(key)) {
                // this feedback term is not already put in the hashmap, hence to be added;
                    Term termInstance = new Term(fieldForFeedback, key);
                    long cf = indexReader.totalTermFreq(termInstance);  // CF: Returns the total number of occurrences of term across all documents (the sum of the freq() for each doc that has this term).
                    long df = indexReader.docFreq(termInstance);        // DF: Returns the number of documents containing the term

                    feedbackTermStats.put(key, new PerTermStat(key, cf, df));
                }
            } // ends for each term of that feedback document
        } // ends for each feedback document
    }

    
    public float return_Smoothed_MLE_Log(String t, DocumentVector dv) throws IOException {
        
        float smoothedMLEofTerm = 1;
        PerTermStat docPTS;
        docPTS = dv.docPerTermStat.get(t);
        PerTermStat colPTS = feedbackTermStats.get(t);

        if (colPTS != null) 
            smoothedMLEofTerm = 
                ((docPTS!=null)?(mixingLambda * (float)docPTS.getCF() / (float)dv.getDocSize()):(0)) /
                ((feedbackTermStats.get(t)!=null)?((1.0f-mixingLambda)*(float)feedbackTermStats.get(t).getCF()/(float)vocSize):0);
     
        return (float)Math.log(1+smoothedMLEofTerm);

    } // ends return_Smoothed_MLE_Log()
    
    
    // sort a map by its value (order = false (if descending))
    public Map<String, Long> mapSortByValue(boolean order, Map map) { 
    
    //convert HashMap into List   
        List<Entry<String, Long>> list = new LinkedList<>(map.entrySet());  
        //sorting the list elements  
        Collections.sort(list, (Entry<String, Long> o1, Entry<String, Long> o2) -> {
            if (order) {
                //compare two object and return an integer
                return o1.getValue().compareTo(o2.getValue());}
            else
                return o2.getValue().compareTo(o1.getValue());
        }); 
        
        Map<String, Long> sortedMap = new LinkedHashMap<>();  
        for (Entry<String, Long> entry : list) {  
            sortedMap.put(entry.getKey(), entry.getValue());  
        } 
        
        return sortedMap;
    }
    
    
    public void makeTermDistributionAnalyzer() throws Exception {
        
        TopDocs topRetDocs;
        ScoreDoc[] hits;
        TopScoreDocCollector collector;
        long cumCount = 0;
        DecimalFormat df = new DecimalFormat("#.#####");

        
        for (TRECQuery query : queries) {
            
            int termCount = 0;
            resPath = resPath + query.qid + ".sem.dist";
            resFileWriter = new FileWriter(resPath.trim());
            System.out.println("Result will be stored in: " + resPath);
            
            topTermStats = new HashMap<>();
            
            collector = TopScoreDocCollector.create(numTopDocs);
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
            
            setFeedbackStats(topRetDocs, luceneQuery.toString(fieldToSearch).split(" "));
            
            for (Map.Entry<Integer, DocumentVector> docSet : feedbackDocumentVectors.entrySet()){
                
                // for each document in the top doc set
                DocumentVector docV = docSet.getValue();
                for (Map.Entry<String, PerTermStat> entrySet : docV.docPerTermStat.entrySet()) {
                    
                    // for each term of that feedback document
                    String key = entrySet.getKey();
                    PerTermStat value = entrySet.getValue();
                    if(null == topTermStats.get(key))
                        topTermStats.put(key, docV.getTf(key, docV));
                    else{
                        cumCount = topTermStats.get(key);
                        cumCount += docV.getTf(key, docV);
                        topTermStats.put(key, cumCount);
                    }
                } // ends for each term of that feedback document
            }
//            System.out.println("######### : " + topTermStats);
            
            // sort terms by the cmulative count and choose top 'numTopTerms'
            topTermStats = (HashMap<String, Long>) mapSortByValue(false, topTermStats);
//            System.out.println("********** ::: " + topTermStats);
            
            StringBuffer resBuffer;
            resFileWriter = new FileWriter(resPath, true);
            resBuffer = new StringBuffer();
            
            for (Map.Entry<String, Long> topTerm : topTermStats.entrySet()) {
                // MLE for the collection
//                resBuffer.append(df.format(Math.log(1+(float)feedbackTermStats.get(topTerm.getKey()).getCF()/(float)vocSize))).append(" ");
                // for each term find MLE for each top doc
                for (Map.Entry<Integer, DocumentVector> docSet : feedbackDocumentVectors.entrySet()){
                    // for each document in the top doc set
                    DocumentVector docV = docSet.getValue();
                    resBuffer.append(df.format(return_Smoothed_MLE_Log(topTerm.getKey(), docV))).append(" ");
                }
                resBuffer.append("\n");
                termCount++;
                if(termCount >= numTopTerms)
                    break;
            }
            resFileWriter.write(resBuffer.toString());
            resFileWriter.close();
        } // ends for each query
    }
            
    
    public static void main(String[] args) throws IOException, Exception {

        String usage = "java RetrievalScoreAnalyzer <properties-file>\n"
                + "Properties file must contain the following fields:\n"
                + "1. Path of the index.\n"
                + "2. Path of the query.xml file.\n"
                + "3. Path of the directory to store res file.\n"
                + "4. SimilarityFunction: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity.\n"
                + "5. No. of top documents to be used.\n"
                + "6. No. of top terms to be used.";              
                
        Properties prop = new Properties();

        if(1 != args.length) {
            System.out.println("Usage: " + usage);
            args = new String[1];
            args[0] = "term-distribution-analyzer.properties";
            System.exit(1);
        }
        prop.load(new FileReader(args[0]));
        TermDistributionAnalyzer tda = new TermDistributionAnalyzer(prop);

        tda.makeTermDistributionAnalyzer();         
    } // ends main()     
}