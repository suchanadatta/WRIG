package neural.common;

//package common;
//
//// class for each wordvec
//
//public class WordVecOptimize implements Comparable<WordVecOptimize> {
//    String word;
//    float[] vec;
//    int occur;
//    
//    public WordVecOptimize(String line) {
//        String[] tokens = line.split("\\s+");
//        word = tokens[0];
//        vec = new float[tokens.length-1];
//        for (int i = 1; i < tokens.length; i++)
//            vec[i-1] = Float.parseFloat(tokens[i]);
//    }
//
//    @Override
//    public int compareTo(WordVecOptimize t) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
//}