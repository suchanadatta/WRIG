/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.correlation.MinMaxNormalizer;
import org.correlation.QPPCorrelationMetric;
import org.evaluator.Metric;
import org.qpp.QPPMethod;
import org.regressor.LearnRegressor;
import org.regressor.FitRegressor;
import org.trec.TRECQuery;

/**
 *
 * @author suchana
 */

public class QPPRegressor {
    static SettingsLoader loader;
    Properties                  prop;
    int                         partition;
    static List<TRECQuery>      trainQueries;
    static List<TRECQuery>      testQueries;
    static String               qrelsFile;
    static String               queryFile;
    static String               resFileTrain;
    static String               resFileTest;
    static List<LearnRegressor> learnRegressor;
    static QPPCorrelationMetric correlationMetric;
    
    static final int SEED = 314159; // first six digits of pi - a beautiful seed!
    static Random rnd = new Random(SEED);
    
    public QPPRegressor(Properties prop, int partition) {
        this.prop = prop;
        this.partition = partition;
        trainQueries = new ArrayList<>();
        testQueries = new ArrayList<>();
        learnRegressor = new ArrayList<>();
        qrelsFile = prop.getProperty("qrels.file");
        queryFile = prop.getProperty("query.file");
        resFileTrain = prop.getProperty("res.train");
        resFileTest = prop.getProperty("res.test");
    }
    
    public void randomSplit(List<TRECQuery> queries) {
        int splitQuery = (int) Math.floor(queries.size() * partition/100);
        System.out.println("##### : " + splitQuery);
        
        for (int i=0; i<splitQuery; i++) {
            trainQueries.add(queries.get(i));
        }
        for (int i=splitQuery; i<queries.size(); i++) {
            testQueries.add(queries.get(i));
        }
        System.out.println("train : " + trainQueries.size() + "\t" + trainQueries.get(0).id);
        System.out.println("test : " + testQueries.size() + "\t" + testQueries.get(0).id);
    }
    
    public void fitRegressorOnTrainSet(QPPMethod [] qppMethods, QPPEvaluator qppEvaluator,
            Similarity sim, int nwanted) throws Exception {
        for (QPPMethod qppMethod: qppMethods) { 
            System.out.println("QPP method : " + qppMethod.name());
            for (Metric m : Metric.values()){
                System.out.println("METRIC : " + m.name());
                LearnRegressor lr = new LearnRegressor();
                lr.setQppMethod(qppMethod.name());
                lr.setMetric(m.name());

                double [] corrMeasure = qppEvaluator.evaluate(trainQueries, sim, m, nwanted, resFileTrain);
                System.out.println("CORR : " + corrMeasure.length);
                double [] qppEstimates = qppEvaluator.evaluateQPPOnModel(qppMethod, trainQueries, corrMeasure, m, resFileTrain);
                System.out.println("ESTIMATE : " + qppEstimates.length);

                FitRegressor fr = new FitRegressor();
                fr.fitLine(corrMeasure, qppEstimates);
                lr.setSlope(fr.learnSlope());
                lr.setyIntercept(fr.learnIntercept());
                learnRegressor.add(lr);
            }
            System.out.println("LEARN REGRESSOR : " + learnRegressor.size());
        }  
    }
    
    public void predictCorrelationTestSet(QPPMethod [] qppMethods, QPPEvaluator qppEvaluator,
            Similarity sim, int nwanted) throws Exception {
        for (QPPMethod qppMethod: qppMethods) { 
            System.out.println("QPP method : " + qppMethod.name());
            for (Metric m : Metric.values()){
                System.out.println("METRIC : " + m.name());
                
                Map<String, Double> corrMeasure = qppEvaluator.evaluateMap(testQueries, sim, m, nwanted, resFileTest);
                System.out.println("CORR : " + corrMeasure.size());
                corrMeasure = MinMaxNormalizer.normalize(corrMeasure);
                
                Map<String, Double> qppEstimates = qppEvaluator.evaluateQPPOnModel(qppMethod, testQueries, corrMeasure, m, resFileTest);
                System.out.println("ESTIMATE : " + qppEstimates.size());
                qppEstimates = MinMaxNormalizer.normalize(qppEstimates);
                Map<String, Double> qppEstimateWithRegressor = new HashMap<>();
                
                for (LearnRegressor lr : learnRegressor) {
                    if (lr.getQppMethod().equalsIgnoreCase(qppMethod.name()) && 
                            lr.getMetric().equalsIgnoreCase(m.name())) {
                        for (Map.Entry<String, Double> spec : qppEstimates.entrySet()) {
//                            System.out.println("%%%% : " + spec.getKey() + "\t" + spec.getValue());
                            qppEstimates.put(spec.getKey(), lr.getSlope() * spec.getValue() + lr.getyIntercept());
                        } 
                    }
                }
                    
                double correlation = qppEvaluator.measureCorrelation(corrMeasure, qppEstimates);
                System.out.println("CORRELATION : " + correlation);
            }
        }  
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            args = new String[1];
            args[0] = "qpp.properties";
        }

        try {
            loader = new SettingsLoader(args[0]);
            QPPEvaluator qppEvaluator = new QPPEvaluator(loader.getProp(), 
                    loader.getCorrelationMetric(), loader.getSearcher(), 
                    loader.getNumWanted());
            QPPRegressor qppreg = new QPPRegressor(loader.getProp(), loader.getTrainTestSplits());

            List<TRECQuery> queries = qppEvaluator.constructQueries();
            System.out.println("QUERIES : " + queries.size() + "\t" + queries.get(0).id);
            
            // create train:test splits
            Collections.shuffle(queries, rnd);
            System.out.println("SHUFFLED : " + queries.size() + "\t" + queries.get(0).id);
            qppreg.randomSplit(queries);
            
            QPPMethod [] qppMethods = qppEvaluator.qppMethods();
            System.out.println("QPPMETHODS : " + qppMethods.length);
            
            Similarity sim = new LMDirichletSimilarity(1000);
//            Similarity sim = new LMJelinekMercerSimilarity(0.6f);
//            Similarity sim = new BM25Similarity(1.5f, 0.75f);
//            Similarity sim = new BM25Similarity(0.7f, 0.3f);
            
            final int nwanted = loader.getNumWanted();
            final int qppTopK = loader.getQppTopK();
            
            qppreg.fitRegressorOnTrainSet(qppMethods, qppEvaluator, sim, nwanted);
            qppreg.predictCorrelationTestSet(qppMethods, qppEvaluator, sim, nwanted);
            
            for (LearnRegressor foo : learnRegressor) {
                System.out.println("$$$$$$$$$$$$$$$$$$ : " 
                + foo.getQppMethod() +"\t" + foo.getMetric() + "\t" 
                        + foo.getSlope() + "\t" + foo.getyIntercept());
            }            
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }   
}