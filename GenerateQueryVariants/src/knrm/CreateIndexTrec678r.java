/*
 * parse and index TREC678R collection
 * index has 3-fields : 1. "docid" - raw document, 2. "docidanalyze" - analyzed docid, 3. "content" - analyzed doc content
 */

package knrm;

/**
 *
 * @author suchana
 */
 
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import jdk.nashorn.internal.parser.JSONParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
 

public class CreateIndexTrec678r{
    
    String                collectionPath;
    String                indexPath;
    String                stopWordPath;
    List<String>          stopWordList;
    EnglishAnalyzer       analyzer;
    static IndexWriter    writer;
    static int            docPerFile, fileCount, totalDocs;
    JSONParser            jsonParser;
        
    
    public CreateIndexTrec678r(String collectionPath, String indexPath) throws IOException {

        this.collectionPath = collectionPath;
        this.indexPath = indexPath; 
        stopWordPath = "/home/suchana/smart-stopwords";
        
        //for using default stopwordlist
//        analyzer = new EnglishAnalyzer();                                       //org.apache.lucene.analysis.en.EnglishAnalyzer; this uses default stopword list
        //for using external stopword list
        stopWordList = getStopwordList(stopWordPath); 
        analyzer = new EnglishAnalyzer(StopFilter.makeStopSet(stopWordList)); // org.apache.lucene.analysis.core.StopFilter
        
        Directory dir;                                                          // org.apache.lucene.store.Directory

        // FSDirectory.open(file-path-of-the-directory)
        dir = FSDirectory.open((new File(this.indexPath)).toPath());          // org.apache.lucene.store.FSDirectory

        IndexWriterConfig iwc;                                                // org.apache.lucene.index.IndexWriterConfig
        iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);                   // other options: APPEND, CREATE_OR_APPEND

        writer = new IndexWriter(dir, iwc);
        totalDocs = 0;
    }
    
    
    public List<String> getStopwordList(String stopwordPath) {
        
        List<String> stopwords = new ArrayList<>();
        String line;

        try {
            System.out.println("Stopword Path: "+ stopwordPath);
            FileReader fr = new FileReader(stopwordPath);
            BufferedReader br = new BufferedReader(fr);
            while ((line = br.readLine()) != null)
                stopwords.add(line.trim());
            br.close();
            fr.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Error: \n" + "StandardAnalyzerWithSmartStopword: setAnalyzer()\n" + "Stopword file not found in: "+stopwordPath);
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Error: \n" + "StandardAnalyzerWithSmartStopword: setAnalyzer()\n" + "IOException occurs");
            System.exit(1);
        }
        return stopwords;
    }
    
    
    public void createTrecIndex(String collectionPath) throws FileNotFoundException, IOException, NullPointerException {
        
        System.out.println("Indexing started...");
        File colFile = new File(collectionPath);
        if(colFile.isDirectory())
            collectionDirectory(colFile);
        else
            indexFile(colFile);
    }
    

    public void collectionDirectory(File colDir) throws FileNotFoundException, IOException, NullPointerException {
        
        File[] files = colDir.listFiles();
        for (File file : files) {
            System.out.println("Dumping file : " + file);
            fileCount++;
            docPerFile = 0;
            if (file.isDirectory()) {
                System.out.println("It has subdirectories...\n");
                collectionDirectory(file);  // calling this function recursively to access all the subfolders in the directory
            }
            else
                indexFile(file);
        }
    }
    
    
    public void indexFile(File colFile) throws FileNotFoundException, IOException {
        
        String fileContent, analyzed_docid, analyzed_content;
        String parseContent, docNo = null;
        Pattern p_docno, p_text;
        Matcher m_docno, m_text;
        Document doc; // org.apache.lucene.document.Document
        
        BufferedReader br = new BufferedReader(new FileReader(colFile));
        String line = br.readLine();
        StringBuilder sb = new StringBuilder(); 
        while(line != null){ 
            sb.append(line).append("\n");
            line = br.readLine();
        } 
        fileContent = sb.toString();
//        System.out.println("file content : " + fileContent);
        fileContent = fileContent.replaceAll("\"", "").replaceAll("", "").replaceAll("\n", "").replaceAll("\r", "");
//        System.out.println("file content : " + fileContent);
        
        p_docno = Pattern.compile("<DOCNO>(.+?)</DOCNO>");
        m_docno = p_docno.matcher(fileContent);
        p_text = Pattern.compile("<TEXT>(.+?)</TEXT>");
        m_text = p_text.matcher(fileContent);
        while (m_docno.find()) {
            doc = new Document();
            docNo = m_docno.group(1).trim().replaceAll("\\s{2,}", "");
            System.out.println("doc no : " + docNo);
            doc.add(new Field("docid", docNo, Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
            analyzed_docid = analyzeText(analyzer, docNo.replaceAll("[^a-zA-Z0-9]", ""), "docidanalyze").toString();
            System.out.println("analyzed doc no : " + analyzed_docid);
            doc.add(new Field("docidanalyze", analyzed_docid, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.NO));
            
            if (m_text.find()){
                parseContent = m_text.group(1).replaceAll("\\s{2,}", " ").replace(".", " ").replace("'", " ").trim();
//                System.out.println("parsed content : " + parseContent);
                analyzed_content = analyzeText(analyzer, parseContent, "content").toString();
//                System.out.println("analyzed content : " + analyzed_content);
                doc.add(new Field("content", analyzed_content, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
                docPerFile++;
                
                writer.addDocument(doc);
                System.out.println("Indexed doc no. : " + ++totalDocs + "\n");
            }
        }
        System.out.println("Total no. of articles in the file : " + docPerFile);
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
    
    
    public static void main(String[] args) throws IOException, FileNotFoundException, NullPointerException, ParserConfigurationException {

        String collectionPath, indexPath;
//        if(args.length!=2) {
//            System.out.println("Usage: java knrm.CreateIndexTrec678r <collection-path> <dump-file-path>");
//            exit(0);
//        }
        args = new String[2];
        args[0] = "/store/collection/trec678rb/documents/";
//        args[0] = "/home/suchana/NetBeansProjects/NeuralModelQpp/test_data/foo_collection/";
        args[1] = "/home/suchana/NetBeansProjects/NeuralModelQpp/test_data/trec678_docidanalyze_index_1/";
        
        collectionPath = args[0];
        indexPath = args[1];
                        
        CreateIndexTrec678r createIndex = new CreateIndexTrec678r(collectionPath, indexPath);
        createIndex.createTrecIndex(collectionPath);
        writer.close();
        System.out.println("Complete dumping... : Total no. of files parsed : " + fileCount);
        System.out.println("Complete indexing... : Total indexed documents : " + totalDocs);
    }
}