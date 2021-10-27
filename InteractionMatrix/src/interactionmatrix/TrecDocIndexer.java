/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package interactionmatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

/**
 *
 * @author suchana
 */

public class TrecDocIndexer {

    Properties prop;
    File indexDir;
    Analyzer analyzer;


    static final public String FIELD_ID = "docid";
    static final public String FIELD_ID_AANALYZE = "docidanalyze";
    static final public String FIELD_ANALYZED_CONTENT = "content";  


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
                StopFilter.makeStopSet(buildStopwordList("stopFilePath"))); 
        return eanalyzer;
    }

    public TrecDocIndexer(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));
        analyzer = constructAnalyzer();
        String indexPath = prop.getProperty("indexPath");
        indexDir = new File(indexPath);
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public File getIndexDir() {
        return indexDir;
    }
}
