/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package neuralqpp;

import static neural.common.CommonVariables.FIELD_BOW;
import common.EnglishAnalyzerWithSmartStopword;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Query;

/**
 *
 * @author suchana
 */

public class UQVQueryParse {
    
    String queryVariantFilePath;
    Analyzer analyzer;
    StringBuffer buff;      // Accumulation buffer for storing the current topic
    QueryVariant query;
    public List<QueryVariant>  queries;
    StandardQueryParser queryParser;
    String fieldToSearch;  // field name of the index to be searched
    
    
    /**
     * Constructor: fieldToSearch is initialized with 'content'
     * @param queryVariantFilePath Absolute path of the query file
     * @param analyzer Analyzer to be used for analyzing the query fields 
     */
    public UQVQueryParse(String queryVariantFilePath, Analyzer analyzer) {
       this.queryVariantFilePath = queryVariantFilePath;
       this.analyzer = analyzer;
       this.fieldToSearch = FIELD_BOW;
       buff = new StringBuffer();
       queries = new LinkedList<>();
       queryParser = new StandardQueryParser(this.analyzer);
    }
    
    
    /**
     * Constructor:
     * @param queryVariantFilePath Absolute path of the query variant file
     * @param analyzer Analyzer to be used for analyzing the query fields
     * @param fieldToSearch Field of the index to be searched
     */
    public UQVQueryParse(String queryVariantFilePath, Analyzer analyzer, String fieldToSearch) {
       this.queryVariantFilePath = queryVariantFilePath;
       this.analyzer = analyzer;
       this.fieldToSearch = fieldToSearch;
       buff = new StringBuffer();
       queries = new LinkedList<>();
       queryParser = new StandardQueryParser(this.analyzer);
    }
    
    
    /**
     * Parses the query file from xml format using SAXParser;
     * 'queries' list gets initialized with the queries 
     * (with title, desc, narr and qid in different place holders)
     * @throws Exception 
     */
    public void queryFileParse() throws Exception {        
        String fileContent;
        System.out.println("Read query variants from path : " + queryVariantFilePath);
        File qVariant = new File(queryVariantFilePath);
        BufferedReader br = new BufferedReader(new FileReader(qVariant));
        String line = br.readLine();
        while(line != null){ 
            query = new QueryVariant();
//            System.out.println("line: " + line);
            String[] words = line.split("-");
//            System.out.println("qid : " + words[0]);
            query.qid = words[0];
//            System.out.println("title : " + words[2].replaceAll(";", "").replaceAll("\\d", "").trim());
            query.qtitle = words[2].replaceAll(";", "").replaceAll("\\d", "").trim();
            queries.add(query);
            line = br.readLine();            
        }
    }
    
    public Query getAnalyzedQuery(QueryVariant queryVariant) throws Exception {

        queryVariant.qtitle = queryVariant.qtitle.replaceAll("-", " ");
        Query luceneQuery = queryParser.parse(queryVariant.qtitle.replaceAll("/", " ")
            .replaceAll("\\?", " ").replaceAll("\"", " ").replaceAll("\\&", " "), fieldToSearch);
        queryVariant.luceneQuery = luceneQuery;
        
        return luceneQuery;
    }
    
    public static void main(String[] args) {

        if (args.length < 1) {
            args = new String[1];
            System.err.println("usage: java UQVQueryParse <input query variant file>");
            args[0] = "/home/suchana/NetBeansProjects/NeuralModelQpp/query_variant.txt";
        }
//        String path = "/home/suchana/NetBeansProjects/NeuralModelQpp/query_variant.txt";

        try {
            EnglishAnalyzerWithSmartStopword obj;
            obj = new EnglishAnalyzerWithSmartStopword("/home/suchana/smart-stopwords");
            Analyzer analyzer = obj.setAndGetEnglishAnalyzerWithSmartStopword();
            
            UQVQueryParse variantParser = new UQVQueryParse(args[0], analyzer);
//            UQVQueryParse variantParser = new UQVQueryParse(path, analyzer);
            variantParser.queryFileParse();
            
            for (QueryVariant query : variantParser.queries) {
                System.out.print(query.qid + " ");
                System.out.println("Title: "+query.qtitle);
                Query luceneQuery;
                luceneQuery = variantParser.getAnalyzedQuery(query);
                System.out.println(luceneQuery.toString(variantParser.fieldToSearch));
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
