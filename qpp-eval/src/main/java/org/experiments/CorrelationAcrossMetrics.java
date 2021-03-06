package org.experiments;

import org.trec.TRECQuery;

import java.util.List;

public class CorrelationAcrossMetrics {

    public static void main(String[] args) {
        if (args.length < 1) {
            args = new String[1];
            args[0] = "qpp.properties";
        }

        try {
            SettingsLoader loader = new SettingsLoader(args[0]);

            QPPEvaluator qppEvaluator = new QPPEvaluator(
                    loader.getProp(), loader.getCorrelationMetric(), 
                    loader.getSearcher(), loader.getNumWanted());

            List<TRECQuery> queries = qppEvaluator.constructQueries();
            // for a single metric if want to use different cutoff (e.g. ap@10/ap@100...)
            int[] cutOffList = {10, 100, 1000};
//            qppEvaluator.relativeSystemRanksAcrossMetrics(queries);
            qppEvaluator.relativeSystemRanksAcrossMetrics(queries, cutOffList);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
