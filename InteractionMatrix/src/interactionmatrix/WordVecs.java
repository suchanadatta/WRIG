package interactionmatrix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Properties;

// Class to store a hashmap of wordvecs

public class WordVecs {

    Properties prop;
    HashMap<String, WordVec> wordvecmap;

    public WordVecs(Properties prop) {
        this.prop = prop;
    }
    
    public void loadFromTextFile() {
        String wordvecFile = prop.getProperty("wordVecPath");
        wordvecmap = new HashMap();
        try (FileReader fr = new FileReader(wordvecFile);
            BufferedReader br = new BufferedReader(fr)) {
            String line;

            while ((line = br.readLine()) != null) {
                WordVec wv = new WordVec(line);
                wordvecmap.put(wv.word.trim(), wv);
//                System.out.println("Word : " + wv.word);
            }
        }
        catch (Exception ex) { ex.printStackTrace(); }        
    }
    
    public WordVec getVec(String word) {
        return wordvecmap.get(word);
    }
}

