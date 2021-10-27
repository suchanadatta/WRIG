/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package neuralqpp;

import static neural.common.CommonVariables.FIELD_BOW;
import static neural.common.CommonVariables.FIELD_FULL_BOW;
import static neural.common.CommonVariables.FIELD_ID;
import neural.common.EnglishAnalyzerSmartStopWords;
import neural.common.TRECQuery;
import neural.common.TRECQueryParse;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import static java.lang.Integer.min;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
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


public class QueryVariantNQC {
    
    Properties      prop;
    String          indexPath;
    String          queryPath, queryVariantPath;    // path of the query file
    File            queryFile, queryVariantFile;   // the query file
    String          stopFilePath;
    IndexReader     indexReader;
    IndexSearcher   indexSearcher;
    String          resPath;                 // path of the res file
    FileWriter      resFileWriter;           // the res file writer
    FileWriter      baselineFileWriter;      // the res file writer
    int             numHits;                 // number of document to retrieveWithExpansionTermsFromFile
    String          runName;                 // name of the run
    List<TRECQuery> queries;
    List<QueryVariant> queryVariants;
    File            indexFile;               // place where the index is stored
    Analyzer        analyzer;                // the analyzer
    boolean         boolIndexExists;         // boolean flag to indicate whether the index exists or not
    String          fieldToSearch;           // the field in the index to be searched
    String          fieldForFeedback;        // field, to be used for feedback
    TRECQueryParse  trecQueryparser;
    UQVQueryParse   uqvVariantParser;
    DSDQueryParse   dsdVariantParser;
    int             simFuncChoice;
    float           param1, param2;
    long            vocSize;                 // vocabulary size
    HashMap<String, TopDocs> allTopDocsFromFileHashMap;     // For feedback from file, to contain all topdocs from file
    private TopDocs topDocs;
    
    
    public QueryVariantNQC(Properties prop) throws IOException, Exception {

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
        
        /* setting query variant path */
        queryVariantPath = prop.getProperty("queryVariantPath");
        System.out.println("queryVariantPath set to: " + queryVariantPath);
        queryVariantFile = new File(queryVariantPath);
        /* query variant path set */
        
        /* constructing TREC queries */
        trecQueryparser = new TRECQueryParse(queryPath, analyzer, fieldToSearch);
        queries = constructQueries();
        /* constructed TREC query */
        
        /* constructing TREC UQV query variants */
//        uqvVariantParser = new UQVQueryParse(queryVariantPath, analyzer, fieldToSearch);
//        queryVariants = constructUQVVariants();
//        for (QueryVariant query : queryVariants) {
//            System.out.print(query.qid + " ");
//            System.out.println("Title: "+query.qtitle);
//            Query luceneQuery;
//            luceneQuery = uqvVariantParser.getAnalyzedQuery(query);
//            System.out.println(luceneQuery.toString());
//        }
        /* constructed TREC UQV query variants */
        
        /* constructing TREC DSD query variants */
        dsdVariantParser = new DSDQueryParse(queryVariantPath, analyzer, fieldToSearch);
        queryVariants = constructDSDVariants();
        for (QueryVariant query : queryVariants) {
            System.out.print(query.qid + " ");
            System.out.println("Title: "+query.qtitle);
            Query luceneQuery;
            luceneQuery = dsdVariantParser.getAnalyzedQuery(query);
            System.out.println(luceneQuery.toString(dsdVariantParser.fieldToSearch));
        }
        /* constructed TREC DSD query variants */
        
        numHits = Integer.parseInt(prop.getProperty("numHits").trim());
        System.out.println("No. of top docs retrieved : " + numHits);

        /* setting res path */
        setRunName_ResFileName();
        resFileWriter = new FileWriter(resPath);
        System.out.println("Result will be stored in: "+resPath);
        /* res path set */
    }
    
    
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
     * Parses the query from the UQV file and makes a list of query variants
     * @return A list with the all the query variants
     * @throws Exception 
     */
    private List<QueryVariant> constructUQVVariants() throws Exception {

        uqvVariantParser.queryFileParse();
        return uqvVariantParser.queries;
    } // ends constructUQVVariants()
    
    
    /**
     * Parses the query from the DSD file and makes a list of query variants
     * @return A list with the all the query variants
     * @throws Exception 
     */
    private List<QueryVariant> constructDSDVariants() throws Exception {

        dsdVariantParser.queryFileParse();
        return dsdVariantParser.queries;
    } // ends constructDSDVariants()
    
    
    /**
     * Sets runName and resPath variables depending on similarity functions.
     */
    private void setRunName_ResFileName() {

        Similarity s = indexSearcher.getSimilarity(true);
        runName = s.toString();
        runName += "-" + fieldToSearch;
        runName = runName.replace(" ", "").replace("(", "").replace(")", "").replace("00000", "");
        if(null == prop.getProperty("resPath"))
            resPath = "/home/suchana/";
        else
            resPath = prop.getProperty("resPath");
        resPath = resPath + queryFile.getName() + "-TD" + numHits + "-" + runName + ".res";
    } // ends setRunName_ResFileName()
    
    
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
    
    
    public double computeVariance(ScoreDoc[] hits){
        
        double sumTopDocsScore = 0.0f;
        double avgTopDocsScore;
        double variance = 0.0f;
        
        int min = min(hits.length, numHits);
        for (int i = 0; i < min; ++i) {
            sumTopDocsScore += hits[i].score;
//            System.out.println("sumscore : " + sumTopDocsScore);
        }
        avgTopDocsScore = sumTopDocsScore/min;
//        System.out.println("mu : " + avgTopDocsScore);
        
        for (int i = 0; i < min; ++i) {
            variance += Math.pow((hits[i].score - avgTopDocsScore), 2);
//            System.out.println("variance : " + variance);
        }
        variance = Math.sqrt(variance / min);
//        System.out.println("final : " + variance);
       
        return variance;        
    } 
    
    
    /* initial retrieval with query variants */
    public void retrieveDocs() throws Exception {
        
        TopDocs topRetDocsQueryVariant;
        ScoreDoc[] hitsQueryVariant;
        TopScoreDocCollector collcetorQueryVariant;
        
        for(QueryVariant queryVar : queryVariants) {
                            
            collcetorQueryVariant = TopScoreDocCollector.create(numHits);
//            Query luceneQueryVariant = uqvVariantParser.getAnalyzedQuery(queryVar);
            Query luceneQueryVariant = dsdVariantParser.getAnalyzedQuery(queryVar);
            System.out.println("\n" + queryVar.qid +": Query variant: " + luceneQueryVariant.toString(fieldToSearch));          

            indexSearcher.search(luceneQueryVariant, collcetorQueryVariant);
            topRetDocsQueryVariant = collcetorQueryVariant.topDocs();
            hitsQueryVariant = topRetDocsQueryVariant.scoreDocs;
            if(hitsQueryVariant == null)
                System.out.println("Nothing found");

            int hits_variant = hitsQueryVariant.length;
            System.out.println("Total docs retrieved : " + topRetDocsQueryVariant.totalHits + "\tSelected top docs : " + hits_variant);     
            
            StringBuffer resBuffer;
            
            resFileWriter = new FileWriter(resPath, true);

            /* res file in TREC format with doc text (7 columns) */
            resBuffer = new StringBuffer();
            for (int i = 0; i < hits_variant; ++i) {
                int docId = hitsQueryVariant[i].doc;
                Document d = indexSearcher.doc(docId);
                resBuffer.append(queryVar.qid).append("\tQ0\t").
                append(d.get(FIELD_ID)).append("\t").
                append((i+1)).append("\t").
                append(hitsQueryVariant[i].score).append("\t").
                append(runName).append("\n");
//                append(d.get(FIELD_BOW)).append("\n");
            }
            resFileWriter.write(resBuffer.toString());
            resFileWriter.close();
        } // ends for each query
    }
        
    
    /* compute del(Q,Q') = v(Q) - v(Q') / v(Q) */
    public void calculateAverageNQC() throws Exception {
        
        TopDocs topRetDocs, topRetDocsQueryVariant;
        ScoreDoc[] hits, hitsQueryVariant;
        TopScoreDocCollector collector, collcetorQueryVariant;
        double del_v_Q = 0.0f;
        
        for (TRECQuery query : queries) {
            
            int numQueryVariant = 0;
            collector = TopScoreDocCollector.create(numHits);
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
            
            // calculate variance for initial query
            double variance_Q = computeVariance(hits);
            System.out.println("Variance of the initial query : " + variance_Q);
            
            for(QueryVariant queryVar : queryVariants) {
                
                if (queryVar.qid.equalsIgnoreCase(query.qid)){
                    collcetorQueryVariant = TopScoreDocCollector.create(numHits);
                    Query luceneQueryVariant = uqvVariantParser.getAnalyzedQuery(queryVar);
                    System.out.println("\n" + queryVar.qid +": Query variant: " + luceneQueryVariant.toString(fieldToSearch));          
                    
                    indexSearcher.search(luceneQueryVariant, collcetorQueryVariant);
                    topRetDocsQueryVariant = collcetorQueryVariant.topDocs();
                    hitsQueryVariant = topRetDocsQueryVariant.scoreDocs;
                    if(hitsQueryVariant == null)
                        System.out.println("Nothing found");
                    
                    int hits_variant = hitsQueryVariant.length;
                    System.out.println("Total docs retrieved : " + topRetDocsQueryVariant.totalHits + "\tSelected top docs : " + hits_variant);
                    
                    // calculate variance for query variant of the initial query
                    double variance_QV = computeVariance(hitsQueryVariant);
                    System.out.println("Variance of the query variant : " + variance_QV);
                    
                    // calculate relative variance w.r.t. the initial query variance
                    del_v_Q += (variance_QV - variance_Q)/variance_Q;
                    System.out.println("delvq : " + del_v_Q);
                    numQueryVariant++;
                } 
            }
            del_v_Q = del_v_Q/numQueryVariant;
            System.out.println("Average del v Q : " + del_v_Q);
            resFileWriter = new FileWriter(resPath, true);
            resFileWriter.write(query.qid + "\t" + del_v_Q + "\n");
            resFileWriter.close();
        } // ends for each query      
    }
    

    public static void main(String[] args) throws IOException, Exception {

        String usage = "java RelevanceBasedLanguageModel <properties-file>\n"
                + "Properties file must contain the following fields:\n"
                + "1. stopFilePath: path of the stopword file\n"
                + "2. indexPath: Path of the index\n"
                + "3. queryPath: path of the trec query file (in proper xml format)\n"
                + "4. resPath: path of the directory to store res file\n"
                + "5. similarityFunction: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity\n"
                + "6. queryVariantPath : Path to the UQV query variants file\n";               
                
        Properties prop = new Properties();

        if(1 != args.length) {
            System.out.println("Usage: " + usage);
            args = new String[1];
            args[0] = "fcrlm-0.4-query_test.xml.D-10.topical-10.causal-10.properties";
            System.exit(1);
        }
        prop.load(new FileReader(args[0]));
        QueryVariantNQC nqc = new QueryVariantNQC(prop);

        nqc.retrieveDocs();         // queryFile-query variant file(e.g. TREC UQV.txt) and get initial ret set of docs
//        nqc.calculateAverageNQC();  // calculate del_v(Q,Q’) = (v(Q)-v(Q’))/v(Q), where Q is TREC standard queries and Q' 
                                    // are query variants of Q obtained from TREC UQV.txt queries (available at : Kurland)
    } // ends main()    
}
