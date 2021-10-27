package org.correlation;
import java.util.Arrays;
import java.util.Map;

public class MinMaxNormalizer {
    public static double[] normalize(double[] x) {
        double[] z = new double[x.length];
        double min = Arrays.stream(x).min().getAsDouble();
        double max = Arrays.stream(x).max().getAsDouble();
        double diff = max - min;
        if (max - min == 0) {
            System.err.println("Values of max and min identical for maxmin normalization");
            System.exit(1);
        }

        for (int i=0; i < x.length; i++) {
            z[i] = (x[i]-min)/diff;
        }
        return z;
    }
    
    // normalize all values of a hashmap
    public static Map<String, Double> normalize(Map<String, Double> x) {
        int i = 0;
        double[] z = new double[x.size()];
        for (Map.Entry<String, Double> entry : x.entrySet()) {
            z[i++] = entry.getValue();
        }
        double min = Arrays.stream(z).min().getAsDouble();
        double max = Arrays.stream(z).max().getAsDouble();
        double diff = max - min;
        if (max - min == 0) {
            System.err.println("Values of max and min identical for maxmin normalization");
            System.exit(1);
        }
        
        for (Map.Entry<String, Double> entry : x.entrySet()) {
            x.put(entry.getKey(), (entry.getValue()-min)/diff);
        }
        
//        System.out.println("normalized values : " + x);
        return x;
    }
}
