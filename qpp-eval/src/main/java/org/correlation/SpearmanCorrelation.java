package org.correlation;

import java.util.Map;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

public class SpearmanCorrelation implements QPPCorrelationMetric {
    @Override
    // a = GT (AP/P@10/nDCG), b = computed specificity 
    public double correlation(double[] a, double[] b) { 
        return new SpearmansCorrelation().correlation(a, b);
    }
    
    @Override
    public double correlation(Map<String, Double> a, Map<String, Double> b) {
        return 0;
    }

    @Override
    public String name() {
        return "rho";
    }
}
