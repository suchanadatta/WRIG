package drmm;

// Class to store a hashmap of wordvecs

import knrm.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Properties;

public class WordVecs {

    HashMap<String, WordVec> wordvecmap;
    CreateMatrixFromQrel cmq;
    KNRMDataGenerator knrmdg;
    Properties prop;

    public WordVecs(KNRMDataGenerator knrmdg, CreateMatrixFromQrel cmq) {
        this.knrmdg = knrmdg;
        this.cmq = cmq;
    }
    
    public WordVecs(Properties prop) {
        this.prop = prop;
    }
    
    public void loadFromTextFile() {
        String wordvecFile = prop.getProperty("wordvecs.vecfile");
        wordvecmap = new HashMap();
        try (FileReader fr = new FileReader(wordvecFile);
            BufferedReader br = new BufferedReader(fr)) {
            String line;

            while ((line = br.readLine()) != null) {
                WordVec wv = new WordVec(line);
                wordvecmap.put(wv.word.trim(), wv);
                System.out.println("WORD : " + wv.word);
            }
        }
        catch (Exception ex) { ex.printStackTrace(); }        
    }
    
    public WordVec getVec(String word) {
        return wordvecmap.get(word);
    }
}

