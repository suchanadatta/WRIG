/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drmm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Debasis
 */
public class TrecDocIndexer {

    Properties prop;
    File indexDir;
    IndexWriter writer;
    Analyzer analyzer;
    List<String> stopwords;
    int pass;

    static final public String FIELD_ID = "docid";
    static final public String FIELD_ID_AANALYZE = "docidanalyze";
    static final public String FIELD_ANALYZED_CONTENT = "content";  // Standard analyzer w/o stopwords.
    static final public String DATE_CREATED = "datecreated";
    static final public String DATE_COMPLETED = "datecompleted";
    static final public String DATE_REVISED = "revised";
    static final public String ARTICLE_TITLE = "articletitle";
    static final public String PAGE_NUM = "pagenum";
    static final public String AUTHOR_LIST = "authlist";
    static final public String MEDLINE_JOURN = "medlinejourm";
    static final public String CHEMICAL_LIST = "chemlist";
    static final public String MESH_HEADING = "meshheading";
    static final public String INTERVENTION = "intervention";
    static final public String ELIGIBILITY = "eligibilty";
    static final public String KEYWORD_LIST = "keywords";

    static final public String PUBMED_DATA = "pubmeddata";
    static final public String ABSTRACT_TEXT = "abstract";
    static final public String ALL_STR = "words";

    protected List<String> buildStopwordList(String stopwordFileName) {
        List<String> stopwords = new ArrayList<>();
        String stopFile = prop.getProperty(stopwordFileName);
        String line;

        try (FileReader fr = new FileReader(stopFile);
                BufferedReader br = new BufferedReader(fr)) {
            while ((line = br.readLine()) != null) {
                stopwords.add(line.trim());
            }
            br.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stopwords;
    }

    Analyzer constructAnalyzer() {
        Analyzer eanalyzer = new EnglishAnalyzer(
                StopFilter.makeStopSet(buildStopwordList("stopfile"))); // default analyzer
        return eanalyzer;
    }

    public TrecDocIndexer(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));
        analyzer = constructAnalyzer();
        String indexPath = prop.getProperty("index");
        indexDir = new File(indexPath);
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public Properties getProperties() {
        return prop;
    }

    void processAll() throws Exception {
        System.out.println("Indexing TREC collection...");

        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        writer = new IndexWriter(FSDirectory.open(indexDir.toPath()), iwcfg);

        indexAll();

        writer.close();
    }

    public File getIndexDir() {
        return indexDir;
    }

    void indexAll() throws Exception {
        if (writer == null) {
            System.err.println("Skipping indexing... Index already exists at " + indexDir.getName() + "!!");
            return;
        }

        File topDir = new File(prop.getProperty("coll"));
        indexDirectory(topDir);
    }

    private void indexDirectory(File dir) throws Exception {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                System.out.println("Indexing directory " + f.getName());
                indexDirectory(f);  // recurse
            } else {
                indexFile(f);
            }
        }
    }

    Document constructDoc(String id, String created, String completed, String revised, String title, String abst, String pagenum, String authors, String medlineinfo, String chemicals, String meshs, String PubmedDate, String keyw) throws IOException {
        Document doc = new Document();
        doc.add(new Field(FIELD_ID, id, Field.Store.YES, Field.Index.NOT_ANALYZED));
        // For the 1st pass, use a standard analyzer to write out
        // the words (also store the term vector)
        doc.add(new Field(DATE_CREATED, created, Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.YES));
        doc.add(new Field(DATE_COMPLETED, completed, Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.YES));
        doc.add(new Field(DATE_REVISED, revised, Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.YES));
        doc.add(new Field(ARTICLE_TITLE, title, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        doc.add(new Field(PAGE_NUM, pagenum, Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.YES));
        doc.add(new Field(AUTHOR_LIST, authors, Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.YES));
        doc.add(new Field(MEDLINE_JOURN, medlineinfo, Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.YES));
        doc.add(new Field(CHEMICAL_LIST, chemicals, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        doc.add(new Field(MESH_HEADING, meshs, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        doc.add(new Field(PUBMED_DATA, PubmedDate, Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.YES));
        doc.add(new Field(ABSTRACT_TEXT, abst, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        doc.add(new Field(KEYWORD_LIST, keyw, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));

        System.out.println("Indexing " + id);

        return doc;
    }

    void indexFile(File file) throws Exception {
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String line;
        Document doc;

        System.out.println("Indexing file: " + file.getName());
        String fname = file.getName();
        StringBuffer txtbuff = new StringBuffer();
        while ((line = br.readLine()) != null) {
            txtbuff.append(line).append("\n");
        }
        String content = txtbuff.toString();
        org.jsoup.nodes.Document jdoc = Jsoup.parse(content);
        Elements docElts = jdoc.select("PubmedArticleSet");
        Element docElt = docElts.get(0);
        Elements Articles = docElt.select("PubmedArticle");
//        System.out.println(" Number of Articles " + Articles.size());

        for (int i = 0; i < Articles.size(); i++) {
            String authors = "";
            String chemicals = "";
            String delim = ",";
            String meshs = "";
            String abst = "";
            ArrayList<String> authort = new ArrayList<String>();
            ArrayList<String> chemicalis = new ArrayList<String>();
            ArrayList<String> meshlis = new ArrayList<String>();

            String PMID = Articles.get(i).select("PMID").first().text();
            try {
//            System.out.println("The pmid is " + PMID);
                Elements authorsil = Articles.get(i).select("AuthorList");
                if (authorsil != null) {
                    Elements authorsi = authorsil.select("Author");
                    for (Element author : authorsi) {
                        authors = author.text();
                        authort.add(authors);
                        authors = "";
                    }
                    authors = String.join(delim, authort);

                }
                Element abstarct = Articles.get(i).select("AbstractText").first();
                if (abstarct != null) {
                    abst = abstarct.text();
                    //       	System.out.println("The abstratc is "+abst);

                }

                Elements medlineinfo = Articles.get(i).select("MedlineJournalInfo");
                Elements chemicalElements = Articles.get(i).select("Chemical");
                if (chemicalElements != null) {
                    for (Element element : chemicalElements) {
                        chemicalis.add(element.text());
                        chemicals = "";
                    }
                    chemicals = String.join(delim, chemicalis);
                }
                Elements MeshHeadingElements = Articles.get(i).select("MeshHeading");
                if (MeshHeadingElements != null) {
                    for (Element element : MeshHeadingElements) {
                        meshs = element.text();
                        meshlis.add(meshs);
                        meshs = "";
                    }
                    meshs = String.join(delim, meshlis);
                }
                Element KeywordL = Articles.get(i).select("KeywordList").first();
                Element PubmedDate = Articles.get(i).select("PubmedData").first();
                Element created = Articles.get(i).select("DateCreated").first();
                Element completed = Articles.get(i).select("DateCompleted").first();
                Element revised = Articles.get(i).select("DateRevised").first();
                Element title = Articles.get(i).select("ArticleTitle").first();
                Element pagenum = Articles.get(i).select("pagenum").first();
                String pageNumText = "";
                String creat = "";
                String compe = "";
                String revi = "";
                String medline = "";
                String Pubm = "";
                String keyw = "";
                if (pagenum != null) {
                    pageNumText = pagenum.text();
                }
                if (created != null) {
                    creat = created.text();
                }
                if (completed != null) {
                    compe = completed.text();
                }
                if (revised != null) {
                    revi = revised.text();
                }
                if (medlineinfo != null) {
                    medline = medlineinfo.text();
                }
                if (PubmedDate != null) {
                    Pubm = PubmedDate.text();
                }
                if (KeywordL != null) {
                    keyw = KeywordL.text();
                }
                doc = constructDoc(PMID, creat, compe, revi, title.text(), abst, pageNumText, authors, medline, chemicals, meshs, Pubm, keyw);
                writer.addDocument(doc);
                /*   org.jsoup.nodes.Document jdoc = Jsoup.parse(content);
        Elements Articles = jdoc.select("clinical_study");
   

        for (int i = 0; i < Articles.size(); i++) {
            String text_b = "";
            String delim = ",";
            String meshs = "";
            String abst = "";
            ArrayList<String> eligiList = new ArrayList<String>();
            ArrayList<String> meshlis = new ArrayList<String>();

            String PMID = Articles.get(i).select("id_info").first().text();
	    try{
     //       System.out.println("The id number is " +PMID);
            Elements authorsil = Articles.get(i).select("eligibility");
	    if(authorsil != null)
	{
	     Elements authorsi =authorsil.select("criteria");
            for (Element text_block : authorsi) {
                text_b = text_block.text();
                eligiList.add(text_b);
                text_b = "";
            }
            text_b = String.join(delim, eligiList);
	//    System.out.println("The value of text_block of  eligibility "+text_b);
	  
	}
            Element abstarct = Articles.get(i).select("detailed_description").first();
            if (abstarct != null) {
		abst=abstarct.text();
	    }

            Elements MeshHeadingElements = Articles.get(i).select("mesh_term");
            if (MeshHeadingElements != null) {
                for (Element element : MeshHeadingElements) {
                    meshs = element.text();
                    meshlis.add(meshs);
                    meshs = "";
                }
                meshs = String.join(delim, meshlis);
            }

            Element intervent = Articles.get(i).select("intervention").first();
            Element brief_summary = Articles.get(i).select("brief_summary").first();
            Element title = Articles.get(i).select("brief_title").first();
            String brief_summ="";
	    String Inte="";
	    String art_title="";	
            if(title != null)
		art_title= title.text();
	    if(brief_summary != null)
		brief_summ=brief_summary.text();
	    if(intervent != null)
		Inte=intervent.text();
	    abst=abst+" "+brief_summ+" "+Inte+" "+text_b;
	  doc = constructDoc(fname,PMID,abst,art_title,meshs);
                  writer.addDocument(doc);
 }*/
            } catch (Exception ex) {
                System.out.println("The document which caused error are " + PMID);
                
            }
        } //*/
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java TrecDocIndexer <prop-file>");
            args[0] = "/home/irlab/Documents/share/sonal/git_luc4ir-master/TRECPMExperiments/luc4ir-master/index.properties";
        }

        try {
            TrecDocIndexer indexer = new TrecDocIndexer(args[0]);
            indexer.processAll();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
