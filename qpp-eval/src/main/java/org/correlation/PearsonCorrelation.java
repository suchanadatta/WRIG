package org.correlation;

import java.util.Map;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

public class PearsonCorrelation implements QPPCorrelationMetric {
    @Override
    public double correlation(double[] a, double[] b) {
        return new PearsonsCorrelation().correlation(a, b);
    }
    
    @Override
    public double correlation(Map<String, Double> a, Map<String, Double> b) {
        return 0;
    }

    @Override
    public String name() {
        return "r";
    }
}
