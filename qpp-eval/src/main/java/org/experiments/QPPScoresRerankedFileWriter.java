///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package org.experiments;
//
//import java.io.BufferedWriter;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//import org.apache.lucene.search.TopDocs;
//import org.apache.lucene.search.similarities.LMDirichletSimilarity;
//import org.apache.lucene.search.similarities.Similarity;
//import org.evaluator.Evaluator;
//import org.evaluator.RetrievedResults;
//import org.qpp.QPPMethod;
//import org.trec.TRECQuery;
//import org.qpp.rerank.QPPMethodRerank;
//
///**
// *
// * @author suchana
// */
//
//public class QPPScoresRerankedFileWriter {
//    
//    public static void main(String[] args) throws IOException {
//
//        if (args.length < 1) {
//            args = new String[1];
//            args[0] = "qppeval.properties";
//        }
//        
//        /* read properties file */
//        Properties prop = new Properties();
//        prop.load(new FileReader(args[0]));
//        
//        /* read from properties file */
//        final String queryFile = prop.getProperty("query.file");
//        System.out.println("query : " + queryFile);
//        final String qrelsFile = prop.getProperty("qrels.file");
//        System.out.println("qrels : " + qrelsFile);
//        final String rerankedFile = prop.getProperty("rerank.file");
//        System.out.println("reranked file : " + rerankedFile);
//        final String resFile = prop.getProperty("res.file");
//        System.out.println("res file : " + resFile);
//        
//        try {
//            /* initialize qpp settings */
//            SettingsLoader loader = new SettingsLoader(args[0]);
//            
//            QPPEvaluator qppEvaluator = new QPPEvaluator(loader.getProp(), loader.getCorrelationMetric(), 
//                    loader.getSearcher(), loader.getNumWanted());
//            
//            List<TRECQuery> queries = qppEvaluator.constructQueries(queryFile);
//            System.out.println("List of queries : " + queries.size());
//            
//            /* where specificity computation is only dependent on set of top docs */
//            QPPMethod[] qppMethods = qppEvaluator.qppMethods();
//            System.out.println("QPP methods : " + qppMethods.length);
//            
//            /* where RSV or rank is used in computing specificity */
//            QPPMethodRerank[] qppMethodsRerank = qppEvaluator.qppMethodsRerank();
//            System.out.println("QPP methods rerank : " + qppMethodsRerank.length);
//            
//            Similarity sim = new LMDirichletSimilarity(1000);
//            
//            final int nwanted = loader.getNumWanted();
//            final int qppTopK = loader.getQppTopK();
//            
//            /* where RSV or rank is used in computing specificity */
//            Map<String, TopDocs> topDocsMap = new HashMap<>();
//            
//            /* where specificity computation is only dependent on set of top docs */
//            Map<String, List<RerankedDocInfo>> rerankedDocsMap = new HashMap<>();
//            
//            Evaluator evaluator = qppEvaluator.executeQueries(queries, sim, nwanted, qrelsFile, resFile, topDocsMap);
//            Evaluator evaluatorRerank = qppEvaluator.executeQueriesRerank(queries, qrelsFile, rerankedFile, rerankedDocsMap);
//            System.out.println("######### : " + rerankedDocsMap.size());
//            
//            FileWriter fw = new FileWriter(prop.getProperty("qpp.res"));
//            BufferedWriter bw = new BufferedWriter(fw);
//            StringBuilder buff = new StringBuilder();
//            buff.append("QID\t");
//            for (QPPMethod qppMethod: qppMethods) {
//                buff.append(qppMethod.name()).append("\t");
//            }
//            for (QPPMethodRerank qppMethodRerank: qppMethodsRerank) {
//                buff.append(qppMethodRerank.name()).append("\t");
//            }
//            buff.deleteCharAt(buff.length()-1);
//            bw.write(buff.toString());
//            bw.newLine();
//            
//            for (TRECQuery query : queries) {
//                buff.setLength(0);
//                buff.append(query.id).append("\t");
//                
//                for (QPPMethod qppMethod: qppMethods) {
//                    System.out.println(String.format("computing %s scores for qid %s", qppMethod.name(), query.id));
//                    RetrievedResults rr = evaluator.getRetrievedResultsForQueryId(query.id);
////                    System.out.println("%%%%%%%%% : " + rr.toString());
//                    TopDocs topDocs = topDocsMap.get(query.title);
//                    if (topDocs==null) {
//                        System.err.println("No Topdocs found for query <" + query.title + ">");
//                        System.exit(1);
//                    }
//
//                    float qppEstimate = (float)qppMethod.computeSpecificity(query.getLuceneQueryObj(), rr, topDocs, qppTopK);
//                    System.out.println("==== " + qppEstimate);
//                    buff.append(qppEstimate).append("\t");
//                }
//                
//                for (QPPMethodRerank qppMethodRerank: qppMethodsRerank) {
//                    System.out.println(String.format("computing %s scores for qid %s", qppMethodRerank.name(), query.id));
//                    RetrievedResults rr = evaluatorRerank.getRetrievedResultsForQueryId(query.id);
////                    System.out.println("%%%%%%%%% : " + rr.toString());
//                    List<RerankedDocInfo> topDocsRerank = rerankedDocsMap.get(query.title);
//                    if (topDocsRerank==null) {
//                        System.err.println("No Topdocs found for query <" + query.title + ">");
//                        System.exit(1);
//                    }
//
//                    float qppEstimateRerank = (float)qppMethodRerank.computeSpecificityRerank(query.getLuceneQueryObj(), rr, topDocsRerank, qppTopK);
//                    System.out.println("##### " + qppEstimateRerank);
//                    buff.append(qppEstimateRerank).append("\t");
//                }
//                
//                buff.deleteCharAt(buff.length()-1);
//                bw.write(buff.toString());
//                bw.newLine();
//            }
//
//            bw.close();
//            fw.close();
//            
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }    
//}
