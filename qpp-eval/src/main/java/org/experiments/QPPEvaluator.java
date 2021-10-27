package org.experiments;

import java.io.*;
import java.util.*;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.*;
import org.correlation.KendalCorrelation;
import org.correlation.PearsonCorrelation;
import org.correlation.QPPCorrelationMetric;
import org.evaluator.Evaluator;
import org.evaluator.Metric;
import org.evaluator.MetricCutOff;
import org.evaluator.RetrievedResults;
import org.qpp.*;
import org.trec.TRECQuery;
import org.trec.TRECQueryParser;



public class QPPEvaluator {

    IndexReader               reader;
    IndexSearcher             searcher;
    int                       numWanted;
    Properties                prop;
    Map<String, TopDocs>      topDocsMap;
    Map<String, List<String>> rerankDocsMap;
    QPPCorrelationMetric      correlationMetric;
    TopScoreDocCollector      collector;
    TRECQueryParser           trecQueryParser;
    RerankedDocInfo           rerankDocInfo;
    

    public QPPEvaluator(Properties prop, QPPCorrelationMetric correlationMetric, IndexSearcher searcher, int numWanted) {
        this.prop = prop;
        this.searcher = searcher;
        this.reader = searcher.getIndexReader();
        this.numWanted = numWanted;
        this.correlationMetric = correlationMetric;
    }

    public String getContentFieldName() {
        return prop.getProperty("content.field", "content");
    }

    public String getIdFieldName() {
        return prop.getProperty("id.field", "docid");
    }

    private static List<String> buildStopwordList() {
        List<String> stopwords = new ArrayList<>();
        String line;

        try (FileReader fr = new FileReader("stop.txt");
             BufferedReader br = new BufferedReader(fr)) {
            while ( (line = br.readLine()) != null ) {
                stopwords.add(line.trim());
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return stopwords;
    }

    public static Analyzer englishAnalyzerWithSmartStopwords() {
        return new EnglishAnalyzer(
                StopFilter.makeStopSet(buildStopwordList())); // default analyzer
    }

    public Properties getProperties() { return prop; }
    public IndexReader getReader() { return reader; }

    public List<TRECQuery> constructQueries() throws Exception {
        return constructQueries(prop.getProperty("query.file"));
    }

    public List<TRECQuery> constructQueries(String queryFile) throws Exception {
        trecQueryParser = new TRECQueryParser(this, queryFile, englishAnalyzerWithSmartStopwords());
        trecQueryParser.parse();
        return trecQueryParser.getQueries();
    }

    public TopDocs retrieve(TRECQuery query, Similarity sim, int numWanted) throws IOException {
        searcher.setSimilarity(sim);
        return searcher.search(query.getLuceneQueryObj(), numWanted);
    }

    public static Similarity[] modelsToTest() {
        return new Similarity[]{
            new LMJelinekMercerSimilarity(0.6f),
            new LMDirichletSimilarity(1000),
            new BM25Similarity(0.7f, 0.3f),
            //new BM25Similarity(0.5f, 1.0f)
        };
    }
    
    public static Similarity[] corrAcrossModels() {
        return new Similarity[]{
            new LMJelinekMercerSimilarity(0.3f),
            new LMJelinekMercerSimilarity(0.6f),
            new BM25Similarity(0.7f, 0.3f),
            new BM25Similarity(1.0f, 1.0f),
            new BM25Similarity(0.3f, 0.7f),
            new LMDirichletSimilarity(100),
            new LMDirichletSimilarity(500),
            new LMDirichletSimilarity(1000),            
        };
    }

    double averageIDF(Query q) throws IOException {
        long N = reader.numDocs();
        Set<Term> qterms = new HashSet<>();
        q.createWeight(searcher, false).extractTerms(qterms);

        float aggregated_idf = 0;
        for (Term t: qterms) {
            int n = reader.docFreq(t);
            double idf = Math.log(N/(double)n);
            aggregated_idf += idf;
        }
        return aggregated_idf/(double)qterms.size();
    }

    double[] evaluate(List<TRECQuery> queries, Similarity sim, Metric m) throws Exception {
        return evaluate(queries, sim, m, numWanted);
    }

    public Evaluator executeQueries(List<TRECQuery> queries, Similarity sim,
                                    int cutoff, String qrelsFile, String resFile, 
                                    Map<String, TopDocs> topDocsMap) throws Exception {

        int numQueries = queries.size();
        double[] evaluatedMetricValues = new double[numQueries];

        FileWriter fw = new FileWriter(resFile);
        BufferedWriter bw = new BufferedWriter(fw);

        for (TRECQuery query : queries) {
//            System.out.println("current query : " + query.luceneQuery);
            TopDocs topDocs = retrieve(query, sim, cutoff);
//            System.out.println("no. of docs retrieved : " + topDocs.totalHits);
            if (topDocsMap != null)
                topDocsMap.put(query.title, topDocs);
            saveRetrievedTuples(bw, query, topDocs, sim.toString());
        }
        bw.flush();
        bw.close();
        fw.close();

        Evaluator evaluator = new Evaluator(qrelsFile, resFile); // load ret and rel
        return evaluator;
    }
    
    // Evaluate a given metric (e.g. AP/P@5) for all queries. Return an array of these computed values
    double[] evaluate(List<TRECQuery> queries, Similarity sim, Metric m, int cutoff) throws Exception {
        topDocsMap = new HashMap<>();

        int numQueries = queries.size();
        System.out.println("Num queries : " + numQueries);
        double[] evaluatedMetricValues = new double[numQueries];

        final String resFile = "/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/data/sample.res";
        FileWriter fw = new FileWriter(resFile);
        BufferedWriter bw = new BufferedWriter(fw);

        for (TRECQuery query : queries) {
//            System.out.println(query.id + " : " + query.title);
            TopDocs topDocs = retrieve(query, sim, cutoff);
//            System.out.println("Retrieved docs : " + topDocs.totalHits);
            topDocsMap.put(query.title, topDocs);
            saveRetrievedTuples(bw, query, topDocs, sim.toString());
        }
        bw.flush();
        bw.close();
        fw.close();

        String qrelsFile = prop.getProperty("qrels.file");
        Evaluator evaluator = new Evaluator(qrelsFile, resFile); // load ret and rel

        int i=0;
        for (TRECQuery query : queries) {
            evaluatedMetricValues[i++] = evaluator.compute(query.id, m);
        }
        return evaluatedMetricValues;
    }
    
    double[] evaluate(List<TRECQuery> queries, Similarity sim, Metric m, int cutoff, String resFile) throws Exception {
        topDocsMap = new HashMap<>();

        int numQueries = queries.size();
        System.out.println("Num queries : " + numQueries);
        double[] evaluatedMetricValues = new double[numQueries];

//        final String resFile = "/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/data/sample.res";
        FileWriter fw = new FileWriter(resFile);
        BufferedWriter bw = new BufferedWriter(fw);

        for (TRECQuery query : queries) {
//            System.out.println(query.id + " : " + query.title);
            TopDocs topDocs = retrieve(query, sim, cutoff);
//            System.out.println("Retrieved docs : " + topDocs.totalHits);
            topDocsMap.put(query.title, topDocs);
            saveRetrievedTuples(bw, query, topDocs, sim.toString());
        }
        bw.flush();
        bw.close();
        fw.close();

        String qrelsFile = prop.getProperty("qrels.file");
        Evaluator evaluator = new Evaluator(qrelsFile, resFile); // load ret and rel

        int i=0;
        for (TRECQuery query : queries) {
            evaluatedMetricValues[i++] = evaluator.compute(query.id, m);
        }
        return evaluatedMetricValues;
    }
    
    // Evaluate a given metric (e.g. AP/P@5) for all queries. Return a hashmap
    // of these computed values <qid, metric>
    Map<String, Double> evaluateMap(List<TRECQuery> queries, Similarity sim, 
            Metric m, int cutoff) throws Exception {
        
        topDocsMap = new HashMap<>();

        int numQueries = queries.size();
        System.out.println("Num queries : " + numQueries);
        Map<String, Double> evaluatedMetricValues = new HashMap<>();

        final String resFile = "/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/data/sample.res";
        FileWriter fw = new FileWriter(resFile);
        BufferedWriter bw = new BufferedWriter(fw);

        for (TRECQuery query : queries) {
//            System.out.println(query.id + " : " + query.title);
            TopDocs topDocs = retrieve(query, sim, cutoff);
//            System.out.println("Retrieved docs : " + topDocs.totalHits);
            topDocsMap.put(query.title, topDocs);
            saveRetrievedTuples(bw, query, topDocs, sim.toString());
        }
        bw.flush();
        bw.close();
        fw.close();

        String qrelsFile = prop.getProperty("qrels.file");
        Evaluator evaluator = new Evaluator(qrelsFile, resFile); // load ret and rel

        for (TRECQuery query : queries) {
            evaluatedMetricValues.put(query.id, evaluator.compute(query.id, m));
        }
        return evaluatedMetricValues;
    }
    
    Map<String, Double> evaluateMap(List<TRECQuery> queries, Similarity sim, 
            Metric m, int cutoff, String resFile) throws Exception {
        
        topDocsMap = new HashMap<>();

        int numQueries = queries.size();
        System.out.println("Num queries : " + numQueries);
        Map<String, Double> evaluatedMetricValues = new HashMap<>();

//        final String resFile = "/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/data/sample.res";
        FileWriter fw = new FileWriter(resFile);
        BufferedWriter bw = new BufferedWriter(fw);

        for (TRECQuery query : queries) {
//            System.out.println(query.id + " : " + query.title);
            TopDocs topDocs = retrieve(query, sim, cutoff);
//            System.out.println("Retrieved docs : " + topDocs.totalHits);
            topDocsMap.put(query.title, topDocs);
            saveRetrievedTuples(bw, query, topDocs, sim.toString());
        }
        bw.flush();
        bw.close();
        fw.close();

        String qrelsFile = prop.getProperty("qrels.file");
        Evaluator evaluator = new Evaluator(qrelsFile, resFile); // load ret and rel

        for (TRECQuery query : queries) {
            evaluatedMetricValues.put(query.id, evaluator.compute(query.id, m));
        }
        return evaluatedMetricValues;
    }

    public double evaluateQPPOnModel(
            QPPMethod qppMethod, List<TRECQuery> queries, 
            double[] evaluatedMetricValues, Metric m) throws Exception {
        
        final String resFile = "/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/data/sample.res";
        double[] qppEstimates = new double[queries.size()];
        int i = 0;

        int qppTopK = Integer.parseInt(prop.getProperty("qpp.numtopdocs"));
        String qrelsFile = prop.getProperty("qrels.file");
        Evaluator evaluator = new Evaluator(qrelsFile, resFile); // load ret and rel

        for (TRECQuery query : queries) {
            RetrievedResults rr = evaluator.getRetrievedResultsForQueryId(query.id);
            TopDocs topDocs = topDocsMap.get(query.title);
            if (topDocs==null) {
                System.err.println("No Topdocs found for query <" + query.title + ">");
                System.exit(1);
            }
            qppEstimates[i] = qppMethod.computeSpecificity(query.getLuceneQueryObj(), rr, topDocs, qppTopK);
            i++;
        }
        
        System.out.print("Correlation Coefficient : " + correlationMetric.name() + " --- ");
        
        return correlationMetric.correlation(evaluatedMetricValues, qppEstimates);
    }
    
    public double[] evaluateQPPOnModel(
            QPPMethod qppMethod, List<TRECQuery> queries, 
            double[] evaluatedMetricValues, Metric m, String resFile) throws Exception {
        
//        final String resFile = "/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/data/sample.res";
        double[] qppEstimates = new double[queries.size()];
        int i = 0;

        int qppTopK = Integer.parseInt(prop.getProperty("qpp.numtopdocs"));
        String qrelsFile = prop.getProperty("qrels.file");
        Evaluator evaluator = new Evaluator(qrelsFile, resFile); // load ret and rel

        for (TRECQuery query : queries) {
            RetrievedResults rr = evaluator.getRetrievedResultsForQueryId(query.id);
            TopDocs topDocs = topDocsMap.get(query.title);
            if (topDocs==null) {
                System.err.println("No Topdocs found for query <" + query.title + ">");
                System.exit(1);
            }
            qppEstimates[i] = qppMethod.computeSpecificity(query.getLuceneQueryObj(), rr, topDocs, qppTopK);
            i++;
        }
        
        return qppEstimates;
    }
    
    public double evaluateQPPOnModel(
            QPPMethod qppMethod, List<TRECQuery> queries, 
            Map<String, Double> evaluatedMetricValues, Metric m) throws Exception {
        
        final String resFile = "/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/data/sample.res";
        Map<String, Double> qppEstimates = new HashMap<>();
        int qppTopK = Integer.parseInt(prop.getProperty("qpp.numtopdocs"));
        String qrelsFile = prop.getProperty("qrels.file");
        Evaluator evaluator = new Evaluator(qrelsFile, resFile); // load ret and rel

        for (TRECQuery query : queries) {
            RetrievedResults rr = evaluator.getRetrievedResultsForQueryId(query.id);
            TopDocs topDocs = topDocsMap.get(query.title);
            if (topDocs==null) {
                System.err.println("No Topdocs found for query <" + query.title + ">");
                System.exit(1);
            }
            qppEstimates.put(query.id, qppMethod.computeSpecificity(query.getLuceneQueryObj(), rr, topDocs, qppTopK));
        }
        
//        double corr = correlationMetric.correlation(evaluatedMetricValues, qppEstimates);
        System.out.print("Correlation Coefficient : " + correlationMetric.name() + " --- ");
        
        return correlationMetric.correlation(evaluatedMetricValues, qppEstimates);
    }
    
    public Map<String, Double> evaluateQPPOnModel(
            QPPMethod qppMethod, List<TRECQuery> queries, 
            Map<String, Double> evaluatedMetricValues, Metric m, String resFile) throws Exception {
        
//        final String resFile = "/home/suchana/NetBeansProjects/qpp-eval/qpp-eval-lucene5.3.1/data/sample.res";
        Map<String, Double> qppEstimates = new HashMap<>();
        int qppTopK = Integer.parseInt(prop.getProperty("qpp.numtopdocs"));
        String qrelsFile = prop.getProperty("qrels.file");
        Evaluator evaluator = new Evaluator(qrelsFile, resFile); // load ret and rel

        for (TRECQuery query : queries) {
            RetrievedResults rr = evaluator.getRetrievedResultsForQueryId(query.id);
            TopDocs topDocs = topDocsMap.get(query.title);
            if (topDocs==null) {
                System.err.println("No Topdocs found for query <" + query.title + ">");
                System.exit(1);
            }
            qppEstimates.put(query.id, qppMethod.computeSpecificity(query.getLuceneQueryObj(), rr, topDocs, qppTopK));
        }
        
        return qppEstimates;
    }
    
    public double measureCorrelation (Map<String, Double> evaluatedMetricValues, Map<String, Double> qppEstimates) {
        System.out.print("Correlation Coefficient : " + correlationMetric.name() + " --- ");
        return correlationMetric.correlation(evaluatedMetricValues, qppEstimates);
    }
    
    public QPPMethod[] qppMethods() {
        QPPMethod[] qppMethods = {
//                new BaseIDFSpecificity(searcher),
//                new AvgIDFSpecificity(searcher),
                new NQCSpecificity(searcher),
                new ClaritySpecificity(searcher),
                new WIGSpecificity(searcher),
                new UEFSpecificity(new NQCSpecificity(searcher)),
                new UEFSpecificity(new ClaritySpecificity(searcher)),
                new UEFSpecificity(new WIGSpecificity(searcher)),
        };
        return qppMethods;
    }

//    public QPPMethod[] quickTestQppMethods() {
//        QPPMethod[] qppMethods = {
//                new WIGSpecificity(searcher),
//                new UEFSpecificity(new WIGSpecificity(searcher)),
//        };
//        return qppMethods;
//    }

    public void relativeSystemRanksAcrossSims(List<TRECQuery> queries) throws Exception {
        relativeSystemRanksAcrossSims(queries, numWanted);
    }

    public void relativeSystemRanksAcrossSims(List<TRECQuery> queries, int cutoff) throws Exception {
        Metric[] metricForEval = Metric.values();
        for (Metric m: metricForEval) {
            relativeSystemRanksAcrossSims(m, queries, cutoff);
        }
    }

    public void relativeSystemRanksAcrossMetrics(List<TRECQuery> queries) throws Exception {
        Similarity[] sims = modelsToTest();
        for (Similarity sim: sims) {
            relativeSystemRanksAcrossMetrics(sim, queries, numWanted);
        }
    }

    public void relativeSystemRanksAcrossMetrics(List<TRECQuery> queries, int cutoff) throws Exception {
        Similarity[] sims = modelsToTest();
        for (Similarity sim: sims) {
            relativeSystemRanksAcrossMetrics(sim, queries, cutoff);
        }
    }
    
    public void relativeSystemRanksAcrossMetrics(List<TRECQuery> queries, int[] cutoff) throws Exception {
        Similarity[] sims = modelsToTest();
        for (Similarity sim: sims) {
            relativeSystemRanksAcrossMetrics(sim, queries, cutoff);
        }
    }

    /*
    Compute how much system rankings change with different settings for different IR models (BM25, LM etc.).
    For each model compute the average rank shift over a range of
    different metrics. For stability, these numbers should be high.
     */
    public void relativeSystemRanksAcrossMetrics(Similarity sim, List<TRECQuery> queries, int cutoff) throws Exception {
        int i, j, k;

        Metric[] metricForEval = Metric.values();
        QPPMethod[] qppMethods = qppMethods();
        System.out.println("QPP METHODS : " + qppMethods.length);

        // Rho and tau scores across the QPP methods.
        double[][] corr_scores = new double[metricForEval.length][qppMethods.length];
        double rankcorr;

        int numQueries = queries.size();
        Map<Integer, double[]> preEvaluated = new HashMap<>();

        for (i=0; i< metricForEval.length; i++) { // pre-evaluate for each metric
            Metric m = metricForEval[i];
            double[] evaluatedMetricValues = evaluate(queries, sim, m, cutoff);
//            System.out.println("$$$$$$$$$$$ : " + evaluatedMetricValues.length);
            preEvaluated.put(i, evaluatedMetricValues);
//            System.out.println("%%%%% : " + preEvaluated);
            System.out.println(String.format("Average %s (IR-model: %s, Metric: %s): %.4f",
                    m.toString(), sim.toString(), m.toString(),
                    StatUtils.mean(evaluatedMetricValues)));
        }

        k = 0;
        for (QPPMethod qppMethod: qppMethods) {
            System.out.println("#################### : " + qppMethod.name());
            for (i = 0; i < metricForEval.length-1; i++) {
                rankcorr = evaluateQPPOnModel(qppMethod, queries, preEvaluated.get(i), metricForEval[i]);
                System.out.println(rankcorr);
                corr_scores[i][k] = rankcorr;

                for (j = i+1; j < metricForEval.length; j++) {
                    rankcorr = evaluateQPPOnModel(qppMethod, queries, preEvaluated.get(j), metricForEval[j]);
                    System.out.println(rankcorr);
                    corr_scores[j][k] = rankcorr;
                }
            }
            k++;
        }

        System.out.println("Contingency for IR model: " + sim.toString());
        for (i = 0; i < metricForEval.length-1; i++) {
            System.out.println("Rho values using " + sim.toString() + ", " + metricForEval[i].name() + " as GT");
            for (j = i + 1; j < metricForEval.length; j++) {
                double inter_corr = rankCorrAcrossCutOffs(corr_scores, i, j, new PearsonCorrelation());
                System.out.println("Rho values using " + sim.toString() + ", " + metricForEval[j].name() + " as GT");
                System.out.printf("%s %s/%s: %s = %.4f \n",
                        sim.toString(), metricForEval[i].name(), metricForEval[j].name(), correlationMetric.name(), inter_corr);
            }
        }
    }
    
    public void relativeSystemRanksAcrossMetrics(Similarity sim, List<TRECQuery> queries, int[] cutoff) throws Exception {
        int i, j, k, l, key = 0;

        Metric[] metricForEval = Metric.values();
        System.out.println("Metric for eval : " + metricForEval.length);
        QPPMethod[] qppMethods = qppMethods();
        System.out.println("QPP METHODS : " + qppMethods.length);
        // Rho and tau scores across the QPP methods.
        double[][] corr_scores = new double[metricForEval.length * cutoff.length][qppMethods.length];
        double rankcorr;

        int numQueries = queries.size();
        Map<Integer, double[]> preEvaluated = new HashMap<>();

        for (i = 0; i < metricForEval.length; i++) { // pre-evaluate for each metric
            Metric m = metricForEval[i];
            System.out.println("metric name : " + m.name());
            for (l = 0; l< cutoff.length; l++) {
                double[] evaluatedMetricValues = evaluate(queries, sim, m, cutoff[l]);
//                System.out.println("$$$$$$$$$$$ : " + evaluatedMetricValues.length);
                preEvaluated.put(key, evaluatedMetricValues);
//                System.out.println("%%%%% : " + preEvaluated);
                System.out.println(String.format("Average %s (IR-model: %s, Metric: %s): %.4f",
                        m.toString(), sim.toString(), m.toString(),
                        StatUtils.mean(evaluatedMetricValues)));
                key++;
            }
        }

        k = 0;
        int metric = 0;
        for (QPPMethod qppMethod: qppMethods) {
            System.out.println("#################### : " + qppMethod.name());
            for (i = 0; i < preEvaluated.size(); i++) {
                if (i > 0 && i % 3 == 0)
                    metric++;
                System.out.println("Metric name : " + metricForEval[metric]);
                rankcorr = evaluateQPPOnModel(qppMethod, queries, preEvaluated.get(i), metricForEval[metric]);
                System.out.println(rankcorr);
                corr_scores[i][k] = rankcorr;
//                System.out.println("i = " + i + "\t" + "k = " + k);
            }
            metric = 0;
            k++;
        }
        
        MetricCutOff[] metricContingency = MetricCutOff.values();
        System.out.println("Contingency for IR model: " + sim.toString());
        for (i = 0; i < preEvaluated.size()-1; i++) {
            if (i > 0 && i % 3 == 0)
                metric++;
//            System.out.println("Rho values using " + sim.toString() + ", " + metricForEval[i].name() + " as GT");
            for (j = i + 1; j < preEvaluated.size(); j++) {
                double inter_corr = rankCorrAcrossCutOffs(corr_scores, i, j, new KendalCorrelation());
//                System.out.println("Rho values using " + sim.toString() + ", " + metricForEval[j].name() + " as GT");
                System.out.printf("%s %s/%s: %s = %.4f \n", sim.toString(), metricContingency[i].name(), 
                        metricContingency[j].name(), correlationMetric.name(), inter_corr);
            }
        }
    }

    /*
    Compute how much system rankings change with different settings for metric (AP, P@5)
    or retrieval model used. For each metric compute the average rank correlation over a range of
    different retrieval models. For stability, these numbers should be high.
     */
    public void relativeSystemRanksAcrossSims(Metric m, List<TRECQuery> queries, int cutoff) throws Exception {
        int i, j, k;

        Similarity[] sims = corrAcrossModels();
        System.out.println("Total models : " + sims.length);
        QPPMethod[] qppMethods = qppMethods();
        System.out.println("QPP METHODS : " + qppMethods.length);

        // Rho and tau scores across the QPP methods.
        double[][] corr_scores = new double[sims.length][qppMethods.length];
        double rankcorr;

        int numQueries = queries.size();
        Map<Integer, double[]> evaluatedMetricValuesSims = new HashMap<>();

        for (i=0; i<sims.length; i++) {
            double[] evaluatedMetricValues = evaluate(queries, sims[i], m, cutoff);
            evaluatedMetricValuesSims.put(i, evaluatedMetricValues);
            System.out.println(String.format("Average %s (IR-model: %s, Metric: %s): %.4f",
                    m.toString(), sims[i].toString(), m.toString(),
                    StatUtils.mean(evaluatedMetricValues)));
        }

        k = 0;
        for (QPPMethod qppMethod: qppMethods) {
            System.out.println("#################### : " + qppMethod.name());
            for (i = 0; i < sims.length-1; i++) {
                rankcorr = evaluateQPPOnModel(qppMethod, queries, evaluatedMetricValuesSims.get(i), m);
                System.out.println(rankcorr);
                corr_scores[i][k] = rankcorr;

                for (j = i+1; j < sims.length; j++) {
                    rankcorr = evaluateQPPOnModel(qppMethod, queries, evaluatedMetricValuesSims.get(j), m);
                    System.out.println(rankcorr);
                    corr_scores[j][k] = rankcorr;
                }
            }
            k++;
        }

        System.out.println("Contingency for metric: " + m.toString());
        for (i = 0; i < sims.length-1; i++) {
            //System.out.println("Rho values using " + sims[i].toString() + ", " + m.name() + " as GT: " + getRowVector_Str(rho_scores, i));
            for (j = i + 1; j < sims.length; j++) {
                //System.out.println("Rho values using " + sims[i].toString() + ", " + m.name() + " as GT: " + getRowVector_Str(rho_scores, i));
                double inter_corr = rankCorrAcrossCutOffs(corr_scores, i, j, new KendalCorrelation());
                System.out.printf("%s %s/%s: %s = %.4f \n",
                        m.name(), sims[i].toString(), sims[j].toString(), correlationMetric.name(), inter_corr);
            }
        }
    }

    public void evaluateQPPAllWithCutoffs(List<TRECQuery> queries) throws Exception {
        QPPMethod[] qppMethods = qppMethods();
        for (QPPMethod qppMethod: qppMethods) {
            System.out.println("Results with " + qppMethod.name());
            evaluateQPPAllWithCutoffs(qppMethod, queries);
        }
    }

    public void evaluateQPPAllWithCutoffs(QPPMethod qppMethod, List<TRECQuery> queries) throws Exception {
        // Measure the relative stability of the rank of different systems
        // with varying number of top-docs and metrics
        Metric[] metricForEval = Metric.values();
        Similarity[] sims = modelsToTest();
        int i, j;
        final int numCutOffs = 10;
        final int cutOffStep = 10;

        for (Metric m : metricForEval) {
            for (i=0; i<numCutOffs; i++) {
                int cutoff = (i+1)*cutOffStep;
                for (Similarity sim : sims) {
                    double[] evaluatedMetricValues = evaluate(queries, sim, m, cutoff);
                    double rankcorr = evaluateQPPOnModel(qppMethod, queries, evaluatedMetricValues, m);
                    System.out.printf("Model: %s, Metric %s: QPP-corr (%s) = %.4f%n",
                            sim.toString(), m.toString(), rankcorr);
                }
            }
        }
    }

    public void evaluateQPPAtCutoff(QPPMethod qppMethod, List<TRECQuery> queries, int cutoff) throws Exception {
        Metric[] metricForEval = Metric.values();
        Similarity[] sims = modelsToTest();

        for (Metric m : metricForEval) {
            for (Similarity sim : sims) {
                System.out.println("Similarity : " + sim);
                double[] evaluatedMetricValues = evaluate(queries, sim, m, cutoff);
                System.out.println(String.format("Average %s (IR-model: %s, Metric: %s): %.4f",
                        m.toString(), sim.toString(), m.toString(),
                        StatUtils.mean(evaluatedMetricValues)));

                double rankcorr = evaluateQPPOnModel(qppMethod, queries, evaluatedMetricValues, m);
                System.out.printf("QPP-method: %s Model: %s, Metric %s: %s = %.4f%n", qppMethod.name(),
                        sim.toString(), m.toString(), correlationMetric.name(), rankcorr);
            }
        }
    }

    double rankCorrAcrossCutOffs(double[][] rankCorrMatrix, int row_a, int row_b,
                                 QPPCorrelationMetric correlationMetric) {
        double[] rc_a = getRowVector(rankCorrMatrix, row_a);
        double[] rc_b = getRowVector(rankCorrMatrix, row_b);
        return correlationMetric.correlation(rc_a, rc_b);
    }

    double[] getRowVector(double[][] rankCorrMatrix, int row) {
        double[] values = new double[rankCorrMatrix[row].length];
        for (int j=0; j < values.length; j++) {
            values[j] = rankCorrMatrix[row][j];
        }
        return values;
    }

    String getRowVector_Str(double[][] rankCorrMatrix, int row) {
        double[] rowvec = getRowVector(rankCorrMatrix, row);
        StringBuilder buff = new StringBuilder("{");
        for (double x: rowvec)
            buff.append(x).append(" ");

        return buff.append("}").toString();
    }

    public void saveRetrievedTuples(BufferedWriter bw, TRECQuery query,
                                    TopDocs topDocs, String runName) throws Exception {
        StringBuilder buff = new StringBuilder();
        ScoreDoc[] hits = topDocs.scoreDocs;
        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            buff.append(query.id.trim()).append("\tQ0\t").
                    append(d.get(getIdFieldName())).append("\t").
                    append((i+1)).append("\t").
                    append(hits[i].score).append("\t").
                    append(runName).append("\n");
        }
        bw.write(buff.toString());
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            args = new String[1];
            args[0] = "init.properties";
        }

        try {
            SettingsLoader loader = new SettingsLoader(args[0]);

            QPPEvaluator qppEvaluator = new QPPEvaluator(
                    loader.getProp(), loader.getCorrelationMetric(), 
                    loader.getSearcher(), loader.getNumWanted());
            List<TRECQuery> queries = qppEvaluator.constructQueries();
            System.out.println("Queries : " + queries.size());
            System.out.println("QPP method loaded : " + loader.getQPPMethod() + "\tMeasure specificity on docs :" + loader.getNumWanted());
//            qppEvaluator.evaluateQPPAtCutoff(loader.getQPPMethod(), queries, loader.getNumWanted());
//            qppEvaluator.evaluateQPPAtCutoff(queries);
            
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
