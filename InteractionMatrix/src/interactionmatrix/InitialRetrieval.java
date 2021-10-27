/*
 * Performs initial retrieval for each query and prepares initial result file
 * Initial retrieved documents are used for histogram generation
 */

package interactionmatrix;

import static interactionmatrix.TrecDocIndexer.FIELD_ID;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.AfterEffectB;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BasicModelIF;
import org.apache.lucene.search.similarities.DFRSimilarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.NormalizationH2;
import org.apache.lucene.search.similarities.Similarity;

/**
 *
 * @author suchana
 */

public class InitialRetrieval {
    
    Properties                    prop; 
    IndexSearcher                 searcher;
    String                        fieldToSearch;
    int                           simFuncChoice;
    float                         param1, param2;
    List<QueryObject>             queries;
    int                           numHits;
    String                        resPath;
    String                        queryFile;
    String                        runName;
    TRECQueryParser               parser;
    FileWriter                    resFileWriter;
    GenerateHistogramPrerankByQid ghpf;
    GenerateHistogramPrerankFile  ghp;
    
    
    public InitialRetrieval(GenerateHistogramPrerankByQid ghpf) throws IOException {
        
        this.ghpf = ghpf;
        this.prop = ghpf.prop;
        this.searcher = ghpf.searcher;
        this.fieldToSearch = ghpf.fieldName;
        this.queryFile = ghpf.queryFile;
        this.queries = ghpf.queries;
        
        simFuncChoice = Integer.parseInt(prop.getProperty("similarityFunction"));
        if (null != prop.getProperty("param1"))
            param1 = Float.parseFloat(prop.getProperty("param1"));
        if (null != prop.getProperty("param2"))
            param2 = Float.parseFloat(prop.getProperty("param2"));        
        setSimilarityFunction(simFuncChoice, param1, param2);
        
        numHits = Integer.parseInt(prop.getProperty("numHits"));
        
        /* setting res path */
        setRunName_ResFileName();
        resFileWriter = new FileWriter(resPath);
        System.out.println("Result will be stored in: "+resPath);
        /* res path set */
        
        parser = new TRECQueryParser(queryFile, ghpf.indexer.getAnalyzer());
    }
    
    public InitialRetrieval(GenerateHistogramPrerankFile ghp) throws IOException {
        
        this.ghp = ghp;
        this.prop = ghp.prop;
        this.searcher = ghp.searcher;
        this.fieldToSearch = ghp.fieldName;
        this.queryFile = ghp.queryFile;
        this.queries = ghp.queries;
        
        simFuncChoice = Integer.parseInt(prop.getProperty("similarityFunction"));
        if (null != prop.getProperty("param1"))
            param1 = Float.parseFloat(prop.getProperty("param1"));
        if (null != prop.getProperty("param2"))
            param2 = Float.parseFloat(prop.getProperty("param2"));        
        setSimilarityFunction(simFuncChoice, param1, param2);
        
        numHits = Integer.parseInt(prop.getProperty("numHits"));
        
        /* setting res path */
        setRunName_ResFileName();
        resFileWriter = new FileWriter(resPath);
        System.out.println("Result will be stored in: "+resPath);
        /* res path set */
        
        parser = new TRECQueryParser(queryFile, ghp.indexer.getAnalyzer());
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
                searcher.setSimilarity(new DefaultSimilarity());
                System.out.println("Similarity function set to DefaultSimilarity");
                break;
            case 1:
                searcher.setSimilarity(new BM25Similarity(param1, param2));
                System.out.println("Similarity function set to BM25Similarity"
                    + " with parameters: " + param1 + " " + param2);
                break;
            case 2:
                searcher.setSimilarity(new LMJelinekMercerSimilarity(param1));
                System.out.println("Similarity function set to LMJelinekMercerSimilarity"
                    + " with parameter: " + param1);
                break;
            case 3:
                searcher.setSimilarity(new LMDirichletSimilarity(param1));
                System.out.println("Similarity function set to LMDirichletSimilarity"
                    + " with parameter: " + param1);
                break;
            case 4:
                searcher.setSimilarity(new DFRSimilarity(new BasicModelIF(), new AfterEffectB(), new NormalizationH2()));
                System.out.println("Similarity function set to DFRSimilarity with default parameters");
                break;
        }
    } // ends setSimilarityFunction()  
    
    
    /**
     * Sets runName and resPath variables depending on similarity functions.
     */
    private void setRunName_ResFileName() {

        Similarity s = searcher.getSimilarity(true);
        runName = s.toString() + "-D" + numHits + "-" + fieldToSearch;
        runName = runName.replace(" ", "").replace("(", "").replace(")", "").replace("00000", "");
        if(null == prop.getProperty("retFilePath"))
            resPath = "/home/suchana/";
        else
            resPath = prop.getProperty("retFilePath");
        resPath = resPath + runName + ".res";
    } // ends setRunName_ResFileName()
    
    
    public void retrieveAll() throws Exception {

        ScoreDoc[] hits;
        TopDocs topDocs;
        TopScoreDocCollector collector;
        
        System.out.println("###############################################################"
                       + "\n############## Initial Retrieval Module Started ###############"
                       + "\n###############################################################");

        for (QueryObject query : queries) {           
            
            collector = TopScoreDocCollector.create(numHits);
            Query luceneQuery = parser.getAnalyzedQuery(query, fieldToSearch);

            System.out.println("\n" + query.id +" :: " + luceneQuery.toString());

            /* PRF - initial retrieval performed */
            searcher.search(luceneQuery, collector);
            topDocs = collector.topDocs();
            hits = topDocs.scoreDocs;
            if(hits == null)
                System.out.println("Nothing found");
            System.out.println("Total docs retrieved : " + topDocs.totalHits);
            /* PRF */

            StringBuffer resBuffer;

            resFileWriter = new FileWriter(resPath, true);

            /* res file in TREC format (6 columns) */
            resBuffer = new StringBuffer();
            for (int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);
                resBuffer.append(query.id).append("\tQ0\t").
                append(d.get(FIELD_ID)).append("\t").
                append((i+1)).append("\t").
                append(hits[i].score).append("\t").
                append(runName).append("\n");
            }
            resFileWriter.write(resBuffer.toString());
            resFileWriter.close();
        } // ends for each query
        
        System.out.println("\n########### Initial Retrieval Done ###########");
    } // ends retrieveAll
}
