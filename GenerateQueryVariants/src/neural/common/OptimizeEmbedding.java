/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package neural.common;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 *
 * @author suchana
 */

public class OptimizeEmbedding {
    
    String embedFile;
    String embedOptimize;
    String stopWordPath;
    List<String> stopWordList;
    Map<String, WordVecOptimize> optimizeVec;
    EnglishAnalyzer analyzer;
    
    public OptimizeEmbedding(String embedFile, String embedOptimize) throws IOException {
        
        this.embedFile = embedFile;
        this.embedOptimize = embedOptimize;
        stopWordPath = "/home/suchana/smart-stopwords";
        stopWordList = getStopwordList(stopWordPath); 
        analyzer = new EnglishAnalyzer();
    }
    
    public List<String> getStopwordList(String stopwordPath) throws IOException {
        
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
    
    public void makeOptimizeEmbedding() throws FileNotFoundException {
        
        optimizeVec = new HashMap<>();
        String analyzedText = "";
        TokenStream stream;
        
        PrintWriter writer = new PrintWriter(embedOptimize);

//        sum all terms having same root word
        try (FileReader fr = new FileReader(embedFile);
            BufferedReader br = new BufferedReader(fr)) {
            String line;

            while ((line = br.readLine()) != null) {
                WordVecOptimize wv = new WordVecOptimize(line);
                System.out.println("initial word : " + wv.word);
                if(! (wv.word.equalsIgnoreCase(",") || wv.word.equalsIgnoreCase("."))){
                    stream = analyzer.tokenStream("content", new StringReader(wv.word));
                    CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
                    stream.reset();
                    while (stream.incrementToken()) {
                        analyzedText = termAtt.toString();
                    }
                    stream.end();
                    stream.close();
                    System.out.println("Analyzed word : "  + analyzedText);
                    
                    if(!"".equals(analyzedText)) {
                        System.out.println("$$$$$$");
                        if(optimizeVec.get(analyzedText) != null) {
                            System.out.println("matched in here ::::: " + analyzedText + "\twith :::: " + optimizeVec.get(analyzedText).word);
                            wv.word = analyzedText;
                            wv.occur = optimizeVec.get(analyzedText).occur + 1;
                            for(int i=0; i< wv.vec.length; i++) {
                                wv.vec[i] = wv.vec[i] + optimizeVec.get(analyzedText).vec[i];
                                optimizeVec.put(analyzedText, wv); 
                            }                    
                        }
                        else {
                            wv.word = analyzedText;
                            wv.occur = 1;
                            optimizeVec.put(analyzedText, wv);
                        }
                    }
                }
            }
            System.out.println("Total size of the map : " + optimizeVec.size());
            System.out.println("Map is : " + optimizeVec);
        }
        catch (Exception ex) { ex.printStackTrace(); }
        
//        do sum average over all vectors and store
//        for(Map.Entry<String, WordVecOptimize> oneVec : optimizeVec.entrySet()) {
//            writer.print(oneVec.getKey() + " ");
//            for(int i=0; i < oneVec.getValue().vec.length; i++){
//                writer.print(oneVec.getValue().vec[i] / oneVec.getValue().occur + " ");
//            }
//            writer.print("\n");
//        }
//        writer.close();
    }
    
    public static void main(String[] args) {

        if (args.length < 1) {
            args = new String[2];
//            args[0] = "/store/causalIR/drmm/data/glove.840B.300d.vec";
            args[0] = "/home/suchana/NetBeansProjects/NeuralModelQpp/test_data/glove-test";
            args[1] = "/home/suchana/NetBeansProjects/NeuralModelQpp/test_data/glove-test-output";
//            args[1] = "/store/causalIR/drmm/data/glove.840B.300d.vec.optimize";
        }

        try {
            OptimizeEmbedding calHist = new OptimizeEmbedding(args[0], args[1]);

            calHist.makeOptimizeEmbedding();

        } catch (IOException ex) {
        }
    }    
}


class WordVecOptimize implements Comparable<WordVecOptimize> {
    String word;
    float[] vec;
    int occur;
    
    public WordVecOptimize(String line) {
        String[] tokens = line.split("\\s+");
        word = tokens[0];
        vec = new float[tokens.length-1];
        for (int i = 1; i < tokens.length; i++)
            vec[i-1] = Float.parseFloat(tokens[i]);
    }

    @Override
    public int compareTo(WordVecOptimize t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
