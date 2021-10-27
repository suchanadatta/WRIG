package drmm;

// class for each wordvec

import knrm.*;
// class for each wordvec

public class WordVec implements Comparable<WordVec> {
    String word;
    float[] vec;
    
    public WordVec(String line) {
        String[] tokens = line.split("\\s+");
        word = tokens[0];
        vec = new float[tokens.length-1];
        for (int i = 1; i < tokens.length; i++)
            vec[i-1] = Float.parseFloat(tokens[i]);
    }

    @Override
    public int compareTo(WordVec t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public float cosineSim(WordVec that) {
        
        float dotProduct = 0;
        float magnitude1 = 0;
        float magnitude2 = 0;
        float cosineSimilarity = 0;        
        float[] queryVec = this.vec;
        float[] docVec = that.vec;
        
        for (int i = 0; i < queryVec.length; i++) {
            dotProduct += queryVec[i] * docVec[i];  //a.b
//                        System.out.println("dot product : " + dotProduct);
            magnitude1 += Math.pow(queryVec[i], 2);  //(a^2)
//                        System.out.println("mag 1 : " + magnitude1);
            magnitude2 += Math.pow(docVec[i], 2); //(b^2)
//                        System.out.println("mag 2 : " + magnitude2);
        }

        magnitude1 = (float)Math.sqrt(magnitude1);//sqrt(a^2)
        magnitude2 = (float)Math.sqrt(magnitude2);//sqrt(b^2)

        if (magnitude1 != 0 | magnitude2 != 0) {
            cosineSimilarity = dotProduct / (magnitude1 * magnitude2);
        } 
        else {
            cosineSimilarity = 0;
        }
        return cosineSimilarity;
    }
}