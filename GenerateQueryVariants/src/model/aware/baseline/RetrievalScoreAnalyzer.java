/*
 * Reference : https://dl.acm.org/doi/10.1145/3209978.3210041
 * Creates the input for the first component - Retrieval Score Analyzer
 */

package model.aware.baseline;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import static neural.common.CommonVariables.FIELD_FULL_BOW;
import neural.common.EnglishAnalyzerSmartStopWords;
import neural.common.TRECQuery;
import neural.common.TRECQueryParse;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author suchana
 */

public class RetrievalScoreAnalyzer {
    
    Properties       prop;
    String           stopFilePath;
    Analyzer         analyzer;
    String           indexPath;
    File             indexFile;
    boolean          boolIndexExists;
    String           fieldToSearch; 
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
    int              numFeedbackDocs;
    long             docCount;  // number of documents in the collection
    
    
    public RetrievalScoreAnalyzer(Properties prop) throws IOException, Exception {

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
        
        /* numFeedbackDocs = number of top documents to select */
        numFeedbackDocs = Integer.parseInt(prop.getProperty("numFeedbackDocs"));
        
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
        
        if(null == prop.getProperty("resPath"))
            resPath = "/home/suchana/";
        else
            resPath = prop.getProperty("resPath");
        resPath = resPath + "retrieval_score_analyzer_trec.res";
    } // ends setRunName_ResFileName()    
    
    
    /**
    * find score(C,Q) = \prod (q in Q) CF(q) / |C|
    * = \sum (log(1 + CF(q) / |C|)
    */
    public float findCollectionScore(Query luceneQuery) throws IOException{
        
        String[] qTerms = luceneQuery.toString(fieldToSearch).split(" ");        
        Fields fields = MultiFields.getFields(indexReader);
        Terms term = fields.terms(fieldToSearch);
        float colScore = 0;
        
        for(String qt : qTerms){
            
            TermsEnum iterator = term.iterator();
            BytesRef byteRef = null;
            
            while((byteRef = iterator.next()) != null) {
                //* for each word in the collection
                String t = new String(byteRef.bytes, byteRef.offset, byteRef.length);
                if(t.equalsIgnoreCase(qt)){
                    System.out.println("T : " + byteRef.utf8ToString());
                    colScore += (float)Math.log(1 + ((float)iterator.totalTermFreq() / (float)docCount)); 
                    System.out.println("F : " + colScore);
                }
            }
        }     
        
        return colScore;
    }
    
    
    public void makeRetrievalScoreAnalyzer() throws Exception {
        
        TopDocs topRetDocs;
        ScoreDoc[] hits;
        TopScoreDocCollector collector;
        float colScore;
        
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
            
            colScore = findCollectionScore(luceneQuery);
            
            StringBuffer resBuffer;
            
            resFileWriter = new FileWriter(resPath, true);

            resBuffer = new StringBuffer();
            resBuffer.append(query.qid).append(" ").append(colScore).append(" ");
            for (int i = 0; i < hits_length; ++i) {
                resBuffer.append(hits[i].score).append(" ");
            }
            resBuffer.append("\n");
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
                + "5. No. of feedback documents to be used.";
              
                
        Properties prop = new Properties();

        if(1 != args.length) {
            System.out.println("Usage: " + usage);
            args = new String[1];
            args[0] = "retrieval-score-analyzer.properties";
            System.exit(1);
        }
        prop.load(new FileReader(args[0]));
        RetrievalScoreAnalyzer rsa = new RetrievalScoreAnalyzer(prop);

        rsa.makeRetrievalScoreAnalyzer();         
    } // ends main()     
}

