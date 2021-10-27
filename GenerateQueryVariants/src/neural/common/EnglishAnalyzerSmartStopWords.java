/**
 * SET THE ANALYZER WITH ENGLISH-ANALYZER AND SMART-STOPWORD LIST
 * 
 * @variables:
 *      Analyzer    analyzer;       // the analyzer
 *      String      stopwordPath;   // path of the smart-stopword file
 * 
 * @constructors:
 *      // Assumed that the smart-stopword file is present in: <a href=build/classes/resources/smart-stopwords>stopword-path</a>
 *      public EnglishAnalyzerWithSmartStopword() {}
 * 
 *      // The path of the stopword file is passed as argument
 *      public EnglishAnalyzerWithSmartStopword(String stopwordPath) {}
 * 
 * @methods:
 *      private void setEnglishAnalyzerWithSmartStopword() {}
 *      public Analyzer getEnglishAnalyzerWithSmartStopword() {}
 *      public Analyzer setAndGetEnglishAnalyzerWithSmartStopword() {}
 */
package neural.common;

import common.EnglishAnalyzerWithSmartStopword;
import common.EnglishAnalyzerWithSmartStopword;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;



public class EnglishAnalyzerSmartStopWords {

    Analyzer    analyzer;
    String      stopFilePath;

    /**
     * Assumed that the smart-stopword file is present in the path:
     *      <a href=build/classes/resources/smart-stopwords>stopword-path</a>.
     */
    public EnglishAnalyzerSmartStopWords() {

        String filePath = new File("").getAbsolutePath();
        //System.out.println(filePath);

        if(!filePath.endsWith("/build/classes")) // This will be true when running from inside IDE (e.g. NetBeans)
            filePath += "/build/classes";

        this.stopFilePath = filePath+"/resources/smart-stopwords";
    }

    /**
     * The path of the stopword file is passed as argument to the constructor
     * @param stopwordPath Path of the stopword file
     */
    public EnglishAnalyzerSmartStopWords(String stopwordPath) {
        this.stopFilePath = stopwordPath;
    }
    /**
     * Set analyzer with EnglishAnalyzer with SMART-stoplist
     */
    public void setEnglishAnalyzerWithSmartStopword() {

        List<String> stopwords = new ArrayList<>();

        String line;
        try {
            System.out.println("Stopword Path: "+stopFilePath);
            FileReader fr = new FileReader(stopFilePath);
            BufferedReader br = new BufferedReader(fr);
            while ((line = br.readLine()) != null)
                stopwords.add(line.trim());
            br.close(); 
            fr.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Error: \n"
                + "StandardAnalyzerWithSmartStopword: setAnalyzer()\n"
                + "Stopword file not found in: "+stopFilePath);
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Error: \n"
                + "StandardAnalyzerWithSmartStopword: setAnalyzer()\n"
                + "IOException occurs");
            System.exit(1);
        }

        // for Lucene 4.10.4
//        analyzer = new EnglishAnalyzer(Version.LUCENE_4_9, StopFilter.makeStopSet(Version.LUCENE_4_9, stopwords));
        
        // for Lucene 5.3
        analyzer = new EnglishAnalyzer(StopFilter.makeStopSet(stopwords));

        //analyzer = new StandardAnalyzer(StopFilter.makeStopSet(stopwords));
    }

    /**
     * Set analyzer with EnglishAnalyzer with SMART-stoplist
     */
    public void setStandardAnalyzerWithSmartStopword() {

        List<String> stopwords = new ArrayList<>();

        String line;
        try {
            System.out.println("Stopword Path: "+stopFilePath);
            FileReader fr = new FileReader(stopFilePath);
            BufferedReader br = new BufferedReader(fr);
            while ( (line = br.readLine()) != null )
                stopwords.add(line.trim());

            br.close(); fr.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Error: \n"
                + "StandardAnalyzerWithSmartStopword: setAnalyzer()\n"
                + "Stopword file not found in: "+stopFilePath);
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Error: \n"
                + "StandardAnalyzerWithSmartStopword: setAnalyzer()\n"
                + "IOException occurs");
            System.exit(1);
        }

        analyzer = new StandardAnalyzer(StopFilter.makeStopSet(stopwords));
    }

    /** 
     * Get the EnglishAnalyzer with Smart stopword list
     * @return analyzer
     */
    public Analyzer getEnglishAnalyzerWithSmartStopword() { return analyzer; }

    /** 
     * Set and get an EnglishAnalyzer with Smart stopword list
     * @return analyzer
     */
    public Analyzer setAndGetEnglishAnalyzerWithSmartStopword() {
        setEnglishAnalyzerWithSmartStopword(); 
        return analyzer; }

    /** 
     * Set and get an StandardAnalyzer with Smart stopword list
     * @return analyzer
     */
    public Analyzer setAndGetStandardAnalyzerWithSmartStopword() {
        setStandardAnalyzerWithSmartStopword(); 
        return analyzer; }

    /**
     * For debugging purpose
     * @param args 
     */
    public static void main(String[] args) {

        EnglishAnalyzerWithSmartStopword obj = new EnglishAnalyzerWithSmartStopword();
        obj.setEnglishAnalyzerWithSmartStopword();
    }
}
