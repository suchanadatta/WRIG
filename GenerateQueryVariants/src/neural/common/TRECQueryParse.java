package neural.common;


import common.EnglishAnalyzerWithSmartStopword;
import static neural.common.CommonVariables.FIELD_BOW;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;
import java.util.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;


public class TRECQueryParse extends DefaultHandler {

    StringBuffer        buff;      // Accumulation buffer for storing the current topic
    String              queryFilePath;
    TRECQuery           query;
    Analyzer            analyzer;
    StandardQueryParser queryParser;
    String              fieldToSearch;  // field name of the index to be searched
    
    public List<TRECQuery>  queries;
    final static String[] tags = {"num", "title", "desc", "narr"};

    /**
     * Constructor: 
     *      fieldToSearch is initialized with 'content';
     *      analyzer is set to EnglishAnalyzer();
     * @param queryFilePath Absolute path of the query file
     * @throws SAXException 
     */
    public TRECQueryParse(String queryFilePath) throws SAXException {
       this.queryFilePath = queryFilePath;
       this.fieldToSearch = FIELD_BOW;
       buff = new StringBuffer();
       queries = new LinkedList<>();
       analyzer = new EnglishAnalyzer();
    }

    /**
     * Constructor: fieldToSearch is initialized with 'content'
     * @param queryFilePath Absolute path of the query file
     * @param analyzer Analyzer to be used for analyzing the query fields
     * @throws SAXException 
     */
    public TRECQueryParse(String queryFilePath, Analyzer analyzer) throws SAXException {
       this.queryFilePath = queryFilePath;
       this.analyzer = analyzer;
       this.fieldToSearch = FIELD_BOW;
       buff = new StringBuffer();
       queries = new LinkedList<>();
       queryParser = new StandardQueryParser(this.analyzer);
    }

    /**
     * Constructor:
     * @param queryFilePath Absolute path of the query file
     * @param analyzer Analyzer to be used for analyzing the query fields
     * @param fieldToSearch Field of the index to be searched
     * @throws SAXException 
     */
    public TRECQueryParse(String queryFilePath, Analyzer analyzer, String fieldToSearch) throws SAXException {
       this.queryFilePath = queryFilePath;
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
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxParserFactory.newSAXParser();

        saxParser.parse(queryFilePath, this);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("top"))
            query = new TRECQuery();
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //System.out.println(buff);
        if (qName.equalsIgnoreCase("title")) {
            query.qtitle = buff.toString().trim();
            buff.setLength(0);
        }
        else if (qName.equalsIgnoreCase("desc")) {
            query.qdesc = buff.toString().trim();
            buff.setLength(0);
        }
        else if (qName.equalsIgnoreCase("num")) {
            query.qid = buff.toString().trim();
            buff.setLength(0);
        }
        else if (qName.equalsIgnoreCase("narr")) {
            query.qnarr = buff.toString().trim();
            buff.setLength(0);
        }
        else if (qName.equalsIgnoreCase("top")) {
            queries.add(query);
        }        
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        buff.append(new String(ch, start, length));
    }

    public Query getAnalyzedQuery(TRECQuery trecQuery) throws Exception {

        trecQuery.qtitle = trecQuery.qtitle.replaceAll("-", " ");
        Query luceneQuery = queryParser.parse(trecQuery.qtitle.replaceAll("/", " ")
            .replaceAll("\\?", " ").replaceAll("\"", " ").replaceAll("\\&", " "), fieldToSearch);
        trecQuery.luceneQuery = luceneQuery;
        
        return luceneQuery;
    }

    public Query getAnalyzedQuery(TRECQuery trecQuery, int queryFieldFlag) throws Exception {

        String queryString = "";
        trecQuery.qtitle = trecQuery.qtitle.replaceAll("-", " ");
        queryString = trecQuery.qtitle.replaceAll("/", " ")
            .replaceAll("\\?", " ").replaceAll("\"", " ").replaceAll("\\&", " ");

        if(queryFieldFlag == 2) {
            trecQuery.qdesc = trecQuery.qdesc.replaceAll("-", " ");
            queryString += " ";
            queryString += trecQuery.qdesc.replaceAll("/", " ").replaceAll(":", " ")
            .replaceAll("\\?", " ").replaceAll("\"", " ").replaceAll("\\&", " ");
        }

        Query luceneQuery = queryParser.parse(queryString, fieldToSearch);
        trecQuery.luceneQuery = luceneQuery;

        return luceneQuery;
    }

    public Query getAnalyzedQuery(String queryString) throws Exception {

//        queryString = queryString.replaceAll("-", " ");
//        Query luceneQuery = queryParser.parse(queryString.replaceAll("/", " ")
//            .replaceAll("\\?", " ").replaceAll("\"", " ").replaceAll("\\&", " "), fieldToSearch);

        Query luceneQuery = new TermQuery(new Term(fieldToSearch, queryString));
        return luceneQuery;
    }

    public Query getAnalyzedQuery(TRECQuery trecQuery, String field) throws Exception {

        trecQuery.qtitle = trecQuery.qtitle.replaceAll("-", " ");
        Query luceneQuery = queryParser.parse(trecQuery.qtitle.replaceAll("/", " ")
            .replaceAll("\\?", " ").replaceAll("\"", " ").replaceAll("\\&", " "), field);
        trecQuery.luceneQuery = luceneQuery;

        return luceneQuery;
    }

    public List<TRECQuery> constructQueries() throws Exception {

        queryFileParse();
        return queries;
    }

    public static void main(String[] args) {

        if (args.length < 1) {
            args = new String[1];
            System.err.println("usage: java TRECQueryParser <input xml file>");
            //args[0] = "/home/suchana/causal_ir/query.xml";
        }

        try {

            EnglishAnalyzerWithSmartStopword obj;
            obj = new EnglishAnalyzerWithSmartStopword("/home/suchana/NetBeansProjects/rm3idf-master/src/resources/smart-stopwords");
            Analyzer analyzer = obj.setAndGetEnglishAnalyzerWithSmartStopword();
            
            TRECQueryParse queryParser = new TRECQueryParse(args[0], analyzer);
            queryParser.queryFileParse();
            
            for (TRECQuery query : queryParser.queries) {
                System.out.print(query.qid + " ");
                System.out.println("Title: "+query.qtitle);
                Query luceneQuery;
                luceneQuery = queryParser.getAnalyzedQuery(query);
                System.out.println(luceneQuery.toString(queryParser.fieldToSearch));
//                System.out.println("Parsed2: "+query.queryFieldAnalyze(analyzer, query.qtitle));    // this statement analyze the query text as simple text
//
//                luceneQuery = queryParser.getAnalyzedQuery(query, 2);
//                System.out.println("Parsed3: "+luceneQuery.toString(queryParser.fieldToSearch));
//
//                luceneQuery = queryParser.getAnalyzedQuery(query.qtitle);
//                System.out.println("Parsed4: "+luceneQuery.toString(queryParser.fieldToSearch));
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
