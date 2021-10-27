package org.experiments;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.evaluator.Evaluator;
import org.evaluator.RetrievedResults;
import org.qpp.*;
import org.trec.TRECQuery;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.evaluator.Metric;

public class QPPScoresFileWriter {
    
    static public QPPMethod[] qppMethods(IndexSearcher searcher) {
        QPPMethod[] qppMethods = {
                new WIGSpecificity(searcher),
                new UEFSpecificity(new WIGSpecificity(searcher)),
        };
        return qppMethods;
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            args = new String[1];
            args[0] = "qpp.properties";
        }

        final String queryFile = "/store/query/trec-robust.xml";
        final String resFile = "/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/outputs/lm-dir.res";
        final String qrelsFile = "/store/qrels/trec-robust.qrel";

        try {
            SettingsLoader loader = new SettingsLoader(args[0]);

            QPPEvaluator qppEvaluator = new QPPEvaluator(loader.getProp(),
                    loader.getCorrelationMetric(), loader.getSearcher(), loader.getNumWanted());
            List<TRECQuery> queries = qppEvaluator.constructQueries(queryFile);

            QPPMethod[] qppMethods = qppEvaluator.qppMethods();
            
//            QPPMethod[] qppMethods = qppMethods(loader.getSearcher());
            
            Similarity sim = new LMDirichletSimilarity(1000);
//            Similarity sim = new LMJelinekMercerSimilarity(0.6f);
//            Similarity sim = new BM25Similarity(1.5f, 0.75f);
//            Similarity sim = new BM25Similarity(0.7f, 0.3f);

            final int nwanted = loader.getNumWanted();
            final int qppTopK = loader.getQppTopK();

            Map<String, TopDocs> topDocsMap = new HashMap<>();
           
            Evaluator evaluator = qppEvaluator.executeQueries(queries, sim, nwanted, qrelsFile, resFile, topDocsMap);

            FileWriter fw = new FileWriter("/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/outputs/lm-dir.qpp");
            BufferedWriter bw = new BufferedWriter(fw);
            StringBuilder buff = new StringBuilder();
            buff.append("QID\t");
            for (QPPMethod qppMethod: qppMethods) {
                buff.append(qppMethod.name()).append("\t");
            }
            buff.deleteCharAt(buff.length()-1);
            bw.write(buff.toString());
            bw.newLine();

            for (TRECQuery query : queries) {
                buff.setLength(0);
                buff.append(query.id).append("\t");

                for (QPPMethod qppMethod: qppMethods) {
                    System.out.println(String.format("computing %s scores for qid %s", qppMethod.name(), query.id));
                    RetrievedResults rr = evaluator.getRetrievedResultsForQueryId(query.id);
//                    System.out.println("RR : " + rr.toString());
                    TopDocs topDocs = topDocsMap.get(query.title);
                    if (topDocs==null) {
                        System.err.println("No Topdocs found for query <" + query.title + ">");
                        System.exit(1);
                    }

                    float qppEstimate = (float)qppMethod.computeSpecificity(query.getLuceneQueryObj(), rr, topDocs, qppTopK);
                    System.out.println("==== " + qppEstimate);
                    buff.append(qppEstimate).append("\t");

                }
                buff.deleteCharAt(buff.length()-1);
                bw.write(buff.toString());
                bw.newLine();
            }
            
//            for (QPPMethod qppMethod: qppMethods) { 
//                System.out.println("QPP method : " + qppMethod.name());
//                for (Metric m : Metric.values()){
//                    if (loader.getCorrelationMetric().name().equalsIgnoreCase("classacc")) {
//                        Map<String, Double> corrMeasure = qppEvaluator.evaluateMap(queries, sim, m, nwanted);
//                        double corr = qppEvaluator.evaluateQPPOnModel(qppMethod, queries, corrMeasure, m);
//                        System.out.print("Metric ::: " + m.name() + " : " + corr + "\n");
//                    }
//                    else {
//                        double [] corrMeasure = qppEvaluator.evaluate(queries, sim, m, nwanted);
//                        double corr = qppEvaluator.evaluateQPPOnModel(qppMethod, queries, corrMeasure, m);
//                        System.out.print("Metric ::: " + m.name() + " : " + corr + "\n");
//                    }
//                }
//            }
            
            bw.close();
            fw.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
