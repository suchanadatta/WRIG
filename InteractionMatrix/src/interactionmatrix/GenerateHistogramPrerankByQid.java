/*
 * This is an implementation of matching histogram generation technique proposed
 * in the paper : "A Deep Relevance Matching Model for Ad-hoc Retrieval"
 * Here we used Log-IDF based histogram method which is built on top of
 * LCH(Log-Count-based Histogram); LCH(with IDF) performs the best as reported 
 * in the above paper.
 */

package interactionmatrix;

import static interactionmatrix.Getter.readPrerankFile;
import static interactionmatrix.TrecDocIndexer.FIELD_ID;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang.ArrayUtils;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;


/**
 *
 * @author suchana
 */

public class GenerateHistogramPrerankByQid {
    
    WordVecs                wvs;
    Properties              prop;
    TrecDocIndexer          indexer;
    IndexSearcher           searcher;
    int                     queryMaxLen;
    double                  maxIdf;
    GetStats                stats;
    String                  fieldName;
    String                  resPath;
    String                  queryFile; 
    List<QueryObject>       queries;
    List<PrerankRelevance>  prerankData;  // to contain the retrieved documents for each query
    HashMap<String, Double> oneDocIdfMap;
    Set<String>             allQueryTerms;
    HashMap<String, Double> termIdf;
    InitialRetrieval        initRet;      // reference to initial retrieval model     

    

    public GenerateHistogramPrerankByQid (String propFile) throws Exception {
        
        /* read properties file */
        prop = new Properties();
        prop.load(new FileReader(propFile));
        
        /* set indexer and searcher */
        indexer = new TrecDocIndexer(propFile);
        File indexDir = indexer.getIndexDir(); 
        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(indexDir.toPath())));

        /* construct list of query objects from .xml file */
        queryFile = prop.getProperty("queryPath");
        queries = constructQueries();
        System.out.println("Total no. of queries : " + queries.size());
        
        /* compute statistics of the collection */
        stats = new GetStats(propFile);
        
        /* calculate max_idf = log(num_docs/lowest_df)-- to normalize all cosim*idf values */
        maxIdf = Math.log((double)stats.numDocs);
        System.out.println("Max IDF value : " + maxIdf);
        
        /* set the field where to perform search */
        fieldName = prop.getProperty("searchField");
        
        /* perform initial retrieval */
        initRet = new InitialRetrieval(this);
        initRet.retrieveAll();

        /* read initial retrieved documents (pseudo-relevant) */
        prerankData = readPrerankFile(initRet.resPath);
        System.out.println("Total no. of preranked documents : " + prerankData.size());
        
        /* load word vectors */         
        System.out.println("Word vector loading started...");
        wvs = new WordVecs(prop);
        wvs.loadFromTextFile();
        System.out.println("embedding size : " + wvs.wordvecmap.size());
        System.out.println("Done.\n");
        
        /* set of all query terms */
        allQueryTerms = getAllQueryTerms();
        System.out.println("Total query terms : " + allQueryTerms.size());
        
        /* maximum length of the query */
        queryMaxLen = getMaxQueryLength();
        System.out.println("\nMaximum query length : " + queryMaxLen);
        
        /* get idfs of all query terms */
        termIdf = stats.getAllIDF(fieldName, allQueryTerms);
        System.out.println("Query terms IDF : "+ termIdf);
    }
    
    
    /**
     * Returns a list of query objects
     * @return 
     */
    public List<QueryObject> constructQueries() throws Exception {

        List<QueryObject> queries = new ArrayList<>();
        TRECQueryParser parser = new TRECQueryParser(queryFile, indexer.getAnalyzer());
        queries = parser.makeQuery();
        
        return queries;
    }
    
    
    /**
     * Returns the maximum length of the query in the query set
     * @return 
     */    
    public int getMaxQueryLength() {
        
        int queryMaxlen = 0, currLength;
        
        for(QueryObject query : queries) {
            currLength = query.title.split(" ").length;
            if (currLength > queryMaxlen) 
                queryMaxlen = currLength;
        }
        
        return queryMaxlen;        
    }
    

    /**
     * Returns a set with all the query terms
     * @return 
     */
    public Set getAllQueryTerms() {
        System.out.println("Getting all query terms...");
        Set<String> queryTerms = new HashSet<>();
        for (QueryObject query : queries) {
            List qterms = query.getQueryTerms(fieldName);
            queryTerms.addAll(qterms);
        }

        System.out.println("Done.");
        return queryTerms;
    }
    

    /**
     * Make the bin for one query-document pair
     * @param binSize
     * @param qterms
     * @param docTerms 
     * @return  
     */
    public float[] makeBin(int binSize, List<String> qterms, List<String> docTerms) throws IOException {

        float [] oneDoc = new float[0];
        WordVec qtv, dtv; // query and document word vector
        float cossim = 0.0f;
        
        oneDocIdfMap = new HashMap<>();
        oneDocIdfMap = stats.getAllIDF(fieldName, docTerms);
        
        // for each query term
        for (String qterm : qterms) {
            float oneQterm[] = new float[binSize];
            qtv = wvs.getVec(qterm);
            
            if(qtv != null) {
                // for each document term
                for (String docTerm : docTerms) {
//                    System.out.println("\nDocterm is : " + docTerm);
                    dtv = wvs.getVec(docTerm);
                    if(dtv != null) {
                        /* compute the cosine similarity between the query term and document term and then binning */
//                        System.out.println("DTV : " + dtv.word + " :::: " + dtv.vec.length);
//                        cossim = qtv.cosineSim(dtv);
//                        System.out.println("Cosim : " + cossim);
//                        int vid = (int) ((cossim+1.0) / 2 * (binSize-1)); 
//                        oneQterm[vid] += 1;
                        /* done */
                        
                        /* compute cosim * max_norm_idf; max_norm_idf = idf / log(num_docs/lowest_df);
                           where, lowest_df = 1, and then binning */
                        cossim = qtv.cosineSim(dtv);
                        cossim = (float) ((double)qtv.cosineSim(dtv) * (oneDocIdfMap.get(docTerm)) / maxIdf);
//                        System.out.println("Cosim value : " + cossim);
                        int binId = (int) ((cossim+1.0) / 2 * (binSize-1)); 
//                        System.out.println("Bin ID : " + binId);
                        oneQterm[binId] += (float)(oneDocIdfMap.get(docTerm) / maxIdf);
                    }
                }
                oneDoc = ArrayUtils.addAll(oneDoc, oneQterm);
            }
            else {
                cossim = 0.0f;
                for (int i = 0; i < oneQterm.length; i++) {
                    oneQterm[i] = cossim;
                }
                oneDoc = ArrayUtils.addAll(oneDoc, oneQterm);
            }
        }
        
        return oneDoc;
    }
    
    
    /**
     * Returns document vector of each of the pseudo-relevant document
     * @param luceneDocId 
     * @param fieldName 
     * @param indexReader
     * @return 
     */
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
        List<String> allTerms = new ArrayList<>();
        
        // for each word in the document
        while((byteRef = iterator.next()) != null) {
            String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
            allTerms.add(term);
            int docFreq = iterator.docFreq();            // df of 't'
            long termFreq = iterator.totalTermFreq();    // tf of 't'
            //System.out.println(t+": tf: "+termFreq);
            docSize += termFreq;
            // termFreq = cf, in a document; df = 1, in a document
        }

        return allTerms;
    }
    
    
    /**
     * Computes each query-document matching histogram and store for each 
     * query-id in a separate file (e.g. qid.hist) 
     * @param oneQueryList  
     * @param qid 
     * @throws java.lang.Exception 
     */
    public void makeInteractionMatrix(List<PrerankRelevance> oneQueryList, String qid) throws Exception{
        
        ScoreDoc[] hits;
        List<String> docTerms;
        
        /* setting res path */
        resPath = prop.getProperty("interMatrixPath");               
        resPath = resPath + qid + ".hist";
        System.out.println("\nResult will be stored in: " + resPath);
        PrintWriter writer = new PrintWriter(resPath.trim());

        for(PrerankRelevance oneDoc : oneQueryList){
            
            for (QueryObject query : queries) {

                if(query.id.equalsIgnoreCase(oneDoc.queryid)) {
                    System.out.println(query.id + " : " + query.title);
                    List<String> qterms = query.getQueryTerms(fieldName);

                    System.out.println("Generating histogram for doc : " + oneDoc.docid);                    
                    hits = Getter.getLuceneDocid(oneDoc.docid, searcher, FIELD_ID);
                    if (hits.length > 0) {
                        int luceneDocid = hits[0].doc;
                        docTerms = getDocumentVector(luceneDocid, fieldName, searcher.getIndexReader());
                        writer.print(oneDoc.queryid + " " + oneDoc.docid + " " + oneDoc.docscore + " " + qterms.size() + " ");
                        
                        float [] onedoc = makeBin(30, qterms, docTerms);
                        StringBuilder builder = new StringBuilder();
                        for (float f: onedoc) {
                            builder.append(f==0?0.0:Math.log10(f)).append(" ");
                        }

                        // padding w.r.t. max query length
                        if ((queryMaxLen - qterms.size()) > 0) {
                            for (int i = 0; i < (queryMaxLen - qterms.size()); i++) {
                                int count = 0;
                                while (count < 30) {
                                    builder.append("0.0").append(" ");
                                    count++;
                                }
                            }
                        }
                        writer.write(builder.toString());
                        writer.write("\n"); 
                    }                    
                }
            }           
        }
        writer.close();
    }
    
    
    /**
     * Generates query-document pair histograms
     *  
     */
    public void makeHistogramPrerankFile() throws IOException, Exception {        
        
        List<PrerankRelevance> perQueryList = null;
        String qid = "";

        for (PrerankRelevance oneDoc : prerankData) {            
            if(qid.equalsIgnoreCase("")){
                perQueryList = new ArrayList<>();
                perQueryList.add(oneDoc);
                qid = oneDoc.queryid;
            }
            else if(qid.equalsIgnoreCase(oneDoc.queryid)){
                perQueryList.add(oneDoc);
                qid = oneDoc.queryid;
            }
            else{
                makeInteractionMatrix(perQueryList, qid);
                perQueryList = new ArrayList<>();
                perQueryList.add(oneDoc);
                qid = oneDoc.queryid;
            }
        }
        makeInteractionMatrix(perQueryList, qid);
    }

    
    public static void main(String[] args) {

        if (args.length < 1) {
            args = new String[1];
            args[0] = "interaction.properties";
        }
        
        try {
            GenerateHistogramPrerankByQid calHist = new GenerateHistogramPrerankByQid(args[0]);
            
            /* generate histogram from prerank file */
            calHist.makeHistogramPrerankFile();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }    
}
