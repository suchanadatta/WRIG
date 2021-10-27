/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package neuralqpp;

import static neural.common.CommonVariables.FIELD_BOW;
import neural.common.DocumentVector;
import neural.common.PerTermStat;
import java.io.IOException;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

/**
 *
 * @author suchana
 */

public class CreateQueryVariantsRLM {
    
    GenerateQueryVariantsTrec gqv;
    GenerateQueryVariantsClueWeb gqvc;
    IndexReader     indexReader;
    IndexSearcher   indexSearcher;
    String          fieldForFeedback;
    String          fieldToSearch;
    int             numFeedbackDocs;
    float           mixingLambda;
    long            vocSize;
    Pattern         pattern;
    String          regex;
    Matcher         matcher;
    
    /**
     * Hashmap of Vectors of all feedback documents, keyed by luceneDocId.
     */
    HashMap<Integer, DocumentVector> feedbackDocumentVectors;
    /**
     * HashMap of PerTermStat of all feedback terms, keyed by the term.
     */
    HashMap<String, PerTermStat> feedbackTermStats;
    /**
     * HashMap of P(Q|D) for all feedback documents, keyed by luceneDocId.
     */
    HashMap<Integer, Float> hash_P_Q_Given_D;
    /**
     * List, for sorting the words in non-increasing order of probability.
     */
    List<WordProbability> list_PwGivenR, cooccurList;
    /**
     * HashMap of P(w|R) for all 'numFeedbackTerms' terms for all feedback documents
     * keyed by the term with P(w|R) as the value.
     */
    HashMap<String, List<Float>> uniqTermSet;
    
    HashMap<String, List<WordProbability>> cooccurMatrix;
    
    List<String> variantListPerQuery;
    
    
    public CreateQueryVariantsRLM(GenerateQueryVariantsTrec gqv) throws IOException {

        this.gqv = gqv;
        this.indexReader = gqv.indexReader;
        this.indexSearcher = gqv.indexSearcher;
        this.fieldForFeedback = gqv.fieldForFeedback;
        this.numFeedbackDocs = gqv.numFeedbackDocs;
        this.fieldToSearch = gqv.fieldToSearch;
        mixingLambda = 0.6f;
        vocSize = getVocabularySize();
        System.out.println("voc size : " + vocSize);
        regex = "(.)*(\\d)(.)*";      
        pattern = Pattern.compile(regex);
    }
    
    public CreateQueryVariantsRLM(GenerateQueryVariantsClueWeb gqvc) throws IOException {

        this.gqvc = gqvc;
        this.indexReader = gqvc.indexReader;
        this.indexSearcher = gqvc.indexSearcher;
        this.fieldForFeedback = gqvc.fieldForFeedback;
        this.numFeedbackDocs = gqvc.numFeedbackDocs;
        this.fieldToSearch = gqvc.fieldToSearch;
        mixingLambda = 0.6f;
        vocSize = getVocabularySize();
        System.out.println("voc size : " + vocSize);
        regex = "(.)*(\\d)(.)*";      
        pattern = Pattern.compile(regex);
    }    
    
    /**
     * Returns the vocabulary size of the collection for the field 'fieldForFeedback'.
     * @return vocSize : Total number of terms in the vocabulary
     * @throws IOException IOException
     */
    
    private long getVocabularySize() throws IOException {

        Fields fields = MultiFields.getFields(indexReader);
        Terms terms = fields.terms(fieldForFeedback);
        if(null == terms) {
            System.err.println("Field: "+fieldForFeedback);
            System.err.println("Error buildCollectionStat(): terms Null found");
        }
        vocSize = terms.getSumTotalTermFreq();  // total number of terms in the index in that field

        return vocSize;                         // total number of terms in the index in that field
    }
    
    
//    /**
//     * Sets the following variables with feedback statistics: to be used consequently.<p>
//     * {@link #feedbackDocumentVectors},<p> 
//     * {@link #feedbackTermStats}, <p>
//     * {@link #hash_P_Q_Given_D}
//     * @param topDocs
//     * @param analyzedQuery
//     * @param gqv
//     * @throws IOException 
//     */
    
    public void setFeedbackStats(TopDocs topDocs, String[] analyzedQuery) throws IOException {

        feedbackDocumentVectors = new LinkedHashMap<>();
        feedbackTermStats = new LinkedHashMap<>();
        hash_P_Q_Given_D = new LinkedHashMap<>();

        ScoreDoc[] hits;
        int hits_length;
        hits = topDocs.scoreDocs;
        hits_length = hits.length;  // number of documents retrieved in the first retrieval
        System.out.println("########## : " + hits_length);
        
        for (int i = 0; i < Math.min(numFeedbackDocs, hits_length); i++) {
            
            // for each feedback document
            int luceneDocId = hits[i].doc;
            System.out.println("lucene doc id : " + luceneDocId);
            Document d = indexSearcher.doc(luceneDocId);
            DocumentVector docV = new DocumentVector(fieldForFeedback);
            docV = docV.getDocumentVector(luceneDocId, indexReader);
            if(docV == null)
                continue;
            feedbackDocumentVectors.put(luceneDocId, docV);  // the feedback document vector is added in the list
            System.out.println("@@@@@@ : " + feedbackDocumentVectors.size());

            for (Map.Entry<String, PerTermStat> entrySet : docV.docPerTermStat.entrySet()) {
                
            // for each term of that feedback document
                String key = entrySet.getKey();
//                System.out.println("KEY : " + key);
                PerTermStat value = entrySet.getValue();
                if(null == feedbackTermStats.get(key)) {
                    
                // this feedback term is not already put in the hashmap, hence to be added;
                    Term termInstance = new Term(fieldForFeedback, key);
                    long cf = indexReader.totalTermFreq(termInstance);  // CF: Returns the total number of occurrences of term across all documents (the sum of the freq() for each doc that has this term).
//                    System.out.println("CF : " + cf);
                    long df = indexReader.docFreq(termInstance);        // DF: Returns the number of documents containing the term
//                    System.out.println("DF : " + df);
                    
                    feedbackTermStats.put(key, new PerTermStat(key, cf, df));
                }
            } // ends for each term of that feedback document
        } // ends for each feedback document

        /* Calculating P(Q|d) for each feedback documents */
        for (Map.Entry<Integer, DocumentVector> entrySet : feedbackDocumentVectors.entrySet()) {
            
            // for each feedback document
            int luceneDocId = entrySet.getKey();
            DocumentVector docV = entrySet.getValue();
            float p_Q_GivenD = 0;
            float smoothMLE = 0;
            
            for (String qTerm : analyzedQuery){
                smoothMLE = return_Smoothed_MLE_Log(qTerm, docV);
                p_Q_GivenD += smoothMLE;
            }                
            if(null == hash_P_Q_Given_D.get(luceneDocId)){
                hash_P_Q_Given_D.put(luceneDocId, p_Q_GivenD);
            }
            else {
                System.err.println("Error while pre-calculating P(Q|d). "
                + "For luceneDocId: " + luceneDocId + ", P(Q|d) already existed.");
            }
        }
//        for (Map.Entry<Integer, Float> entryset : hash_P_Q_Given_D.entrySet()){
//            System.out.println("lucene id : " + entryset.getKey() + "\t p(q|d) : " + entryset.getValue());
//        }
    }
    
    
    public float return_Smoothed_MLE_Log(String t, DocumentVector dv) throws IOException {
        
        float smoothedMLEofTerm = 1;
        PerTermStat docPTS;
        docPTS = dv.docPerTermStat.get(t);
        PerTermStat colPTS = feedbackTermStats.get(t);

        if (colPTS != null) 
            smoothedMLEofTerm = 
                ((docPTS!=null)?(mixingLambda * (float)docPTS.getCF() / (float)dv.getDocSize()):(0)) /
                ((feedbackTermStats.get(t)!=null)?((1.0f-mixingLambda)*(float)feedbackTermStats.get(t).getCF()/(float)vocSize):0);
     
        return (float)Math.log(1+smoothedMLEofTerm);
    } 
    
    
    public void makeUniqTermSet(TopDocs topDocs) throws Exception{
        
        ScoreDoc[] hits;
        String topDoc = null;
        uniqTermSet = new LinkedHashMap<>();
        
        // remove all terms that contain any digit
        hits = topDocs.scoreDocs;
        if(hits == null)
            System.out.println("Nothing found");
        int hits_length = hits.length;
        
        for (int i = 0; i < Math.min(numFeedbackDocs, hits_length); ++i) {
            int docId = hits[i].doc;
            Document d = indexSearcher.doc(docId);
            topDoc = topDoc + d.get(FIELD_BOW);
        }
        String totalTerms[] = topDoc.split(" ");
        System.out.println("Total no. of terms : " + totalTerms.length);
        for(int i=1; i < totalTerms.length; i++){
            matcher = pattern.matcher(totalTerms[i]);
            if (matcher.matches()) {
            } else uniqTermSet.put(totalTerms[i], new ArrayList<>());
        }
    }
    
    public void makeUniqTermSet(List<String> topRetTerms) throws Exception{
        
//        ScoreDoc[] hits;
//        String topDoc = null;
        uniqTermSet = new LinkedHashMap<>();
        
        // remove all terms that contain any digit
//        hits = topDocs.scoreDocs;
//        if(hits == null)
//            System.out.println("Nothing found");
//        int hits_length = hits.length;
//        
//        for (int i = 0; i < Math.min(numFeedbackDocs, hits_length); ++i) {
//            int docId = hits[i].doc;
//            Document d = indexSearcher.doc(docId);
//            topDoc = topDoc + d.get(FIELD_BOW);
//        }
//        String totalTerms[] = topDoc.split(" ");
        System.out.println("Total no. of terms : " + topRetTerms.size());
        for(int i=1; i < topRetTerms.size(); i++){
            matcher = pattern.matcher(topRetTerms.get(i));
            if (matcher.matches()) {
            } else uniqTermSet.put(topRetTerms.get(i), new ArrayList<>());
        }
//        System.out.println("total unique terms : " + uniqTermSet);
    }    
    
    public void createCooccurMatrix() throws Exception {
        
        int count = 1;
        cooccurMatrix = new LinkedHashMap<>();
        
        /* Calculating for each w_i in PRD: P(w_i|R)~P(wi, q1 ... qk)
           P(wi, q1 ... qk) = {P(w|D)*\prod_{i=1... k} {P(qi|D}} */
        
        for (Map.Entry<Integer, DocumentVector> docEntrySet : feedbackDocumentVectors.entrySet()) {
            
            // for each doc in RF-set
            int luceneDocId = docEntrySet.getKey();
            DocumentVector docV = docEntrySet.getValue(); 
            float normFactor = 0;
            list_PwGivenR = new ArrayList<>();
            
            for (Map.Entry<String, PerTermStat> termEntrySet : docV.docPerTermStat.entrySet()) {
                
                String feedbackTerm = termEntrySet.getKey();
                float p_W_GivenR_one_doc = return_Smoothed_MLE_Log(feedbackTerm, feedbackDocumentVectors.get(luceneDocId)) *
                    hash_P_Q_Given_D.get(luceneDocId);
                list_PwGivenR.add(new WordProbability(feedbackTerm, p_W_GivenR_one_doc));
                normFactor += p_W_GivenR_one_doc;
            }

            for (WordProbability singleTerm : list_PwGivenR) {
                singleTerm.p_w_given_R = singleTerm.p_w_given_R / normFactor;
            } 
            
            /* put RM1 scores for each PRD term in the corresponding term list of the doc-term matrix */
            for (WordProbability singleTerm : list_PwGivenR) {
                for (Map.Entry<String, List<Float>> termSet : uniqTermSet.entrySet()){
                    List<Float> temp = termSet.getValue();
                    if (singleTerm.w.equalsIgnoreCase(termSet.getKey())){
                        temp.add(singleTerm.p_w_given_R);
                        uniqTermSet.put(termSet.getKey(), temp);
                        break;
                    }                        
                }
            }
            
            /* fill with 0 for non-matching terms in the doc-term matrix */
            for (Map.Entry<String, List<Float>> termSet : uniqTermSet.entrySet()){
                List<Float> temp = termSet.getValue();
                if (temp.size() < count){
                   temp.add(0.0f);
                }                
            }
            count++;
        }
        
        // to print the map
//        System.out.println("size of the map : " + uniqTermSet.size());
//        for (Map.Entry<String, List<Float>> termSet : uniqTermSet.entrySet()){
//            String term = termSet.getKey();
//            List<Float> temp = termSet.getValue();
//            System.out.print("Term : " + term + "\t");
//            for (Float val : temp){
//                System.out.print(val + ",");
//            }
//            System.out.print("\n");
//        }
        
        /* create term-term co-occurance matrix */
        for (Map.Entry<String, List<Float>> termSet_1 : uniqTermSet.entrySet()){
            String term_1 = termSet_1.getKey();
            List<Float> scoreRM_1 = termSet_1.getValue(); 
            cooccurList = new ArrayList<>();
            for (Map.Entry<String, List<Float>> termSet_2 : uniqTermSet.entrySet()){
                String term_2 = termSet_2.getKey();
                List<Float> scoreRM_2 = termSet_2.getValue();
                float finalScore = 0;
                for (int i = 0; i < scoreRM_1.size(); i++)
                    finalScore += scoreRM_1.get(i) * scoreRM_2.get(i);
                cooccurList.add(new WordProbability(term_2, finalScore));
            }
            
            /* sorting probability list in descending order */
            Collections.sort(cooccurList, (WordProbability t, WordProbability t1) -> t.p_w_given_R<t1.p_w_given_R?1:t.p_w_given_R==t1.p_w_given_R?0:-1);
            
            /* normalize "cooccurList" scores for each term in "uniqTermSet" */
            float norm = 0;
            for (WordProbability wp : cooccurList) {
                norm += wp.p_w_given_R;
            }
            
            float sum = 0;
            for (WordProbability wp : cooccurList) {
                wp.p_w_given_R /= norm;
                sum += wp.p_w_given_R;                        
            }
//            System.out.println("Sum in the cooccur list : " + sum);
            cooccurMatrix.put(term_1, cooccurList);
        }
        
        // to print the map
//        System.out.println("size of the map : " + cooccurMatrix.size());
//        for (Map.Entry<String, List<WordProbability>> termSet : cooccurMatrix.entrySet()){
//            String term = termSet.getKey();
//            List<WordProbability> temp = termSet.getValue();
//            System.out.print("Term : " + term + "\t");
//            for (WordProbability wp : temp){
//                System.out.print("link : " + wp.w + " :: " + wp.p_w_given_R + ",");
//                System.out.print("size : " + temp.size());
//            }
//            System.out.print("\n");
//        }
    }
    
    
    public String rouletteSelection(String qtermChoice){
        
        String selectTerm = "";
        int index;
        float sumProb = 0;
        float[] cumProbList;
        
        cooccurList = cooccurMatrix.get(qtermChoice);
        
        /* create Roulette wheel items */
        index = 0;
        cumProbList = new float[cooccurList.size()];
        System.out.println("prob list size : " + cumProbList.length);
        for (WordProbability cumProb : cooccurList) {
            if (index < cumProbList.length) {
                System.out.print("\tterm " + cumProb.p_w_given_R);
                sumProb += cumProb.p_w_given_R;
                cumProbList[index] = sumProb;
                System.out.print("\tindex : " + index + "\tval : " + sumProb);                
                index++;
            }
        }
        System.out.println("sum in the wheel : " + sumProb);
        
        /* random no. generation and select the fittest candidate */
        float roulette = (float)Math.random();
        System.out.println("roulette : " + roulette);
        index = 0;
        while (index < cumProbList.length) {
            if (cumProbList[index] > roulette){
                if (index == 0 || index == cumProbList.length) {
                    selectTerm = cooccurList.get(index).w;
                    System.out.println("roulete wheeel : " + selectTerm);
                    break;
                }
                else {
                    selectTerm = cooccurList.get(index-1).w;
                    System.out.println("roulete wheeel : " + selectTerm);
                    break;
                }
            }
            index++;
        }
        
    return selectTerm;
    }
    
    /* keep only one original query term and replace all others */
    
    public List<String> createVariantsRLMTrec(TopDocs topDocs, Query luceneQuery, int varLen) throws Exception {
        
        int choice;
        String qtermChoice, selectTerm;
        Random rand;
        
        variantListPerQuery = new ArrayList<>();
        
        makeUniqTermSet(topDocs);
        System.out.println("Total no. of unique terms : " + uniqTermSet.size());
        
        setFeedbackStats(topDocs, luceneQuery.toString(fieldToSearch).split(" "));
        System.out.println("Feedback docs and terms stats are set.");
        
        createCooccurMatrix();
        System.out.println("size of the co-occurance matrix : " + cooccurMatrix.size());
        
        /*  Roulette Wheel selection for query term substitution */
        String qTerms [] = luceneQuery.toString(fieldToSearch).split(" ");
        
        /* event-1 : choose which qterm to substitute (equally likely to choose any term) */
        for (int itr=0; itr < 10; itr++) {
            
            int count = 1;
            rand = new Random(); 
            choice = rand.nextInt(qTerms.length);
            qtermChoice = qTerms[choice];
            matcher = pattern.matcher(qtermChoice);
            while (matcher.matches() || qtermChoice.contains(".")) {
                rand = new Random();
                choice = rand.nextInt(qTerms.length);
                qtermChoice = qTerms[choice]; 
                matcher = pattern.matcher(qtermChoice);
//                System.out.println("Qterm selected for substitution : " + qtermChoice);
            } 
            System.out.println("Qterm selected for substitution : " + qtermChoice);

            /* event-2 : sample a word from the top-K vocab to replace this word (Roulette Wheel) */
            if (qTerms.length > 1) {
                selectTerm = qtermChoice;
                while (count < qTerms.length) {
                    selectTerm = rouletteSelection(selectTerm);
                    qtermChoice = qtermChoice + " ";
                    qtermChoice = qtermChoice + selectTerm;
                    System.out.println("new query : " + qtermChoice + "\n");
                    count++;
                }
                variantListPerQuery.add(qtermChoice);
            }
            else {
                selectTerm = qtermChoice;
                while (count <= qTerms.length) {
                    selectTerm = rouletteSelection(selectTerm);
                    qtermChoice = qtermChoice + " ";
                    qtermChoice = qtermChoice + selectTerm;
                    System.out.println("new query : " + qtermChoice + "\n");
                    count++;
                }
                variantListPerQuery.add(qtermChoice);
            }
        }
        
        return variantListPerQuery;
    }
    
    /* keep only one original query term and replace all others */
    
    /* keep more than one original query term and replace |Q|-m terms (where, m = 1,2,3...|Q|-2) */
    
//    public List<String> createVariantsRLMTrec(TopDocs topDocs, Query luceneQuery, int varLen) throws Exception {
//        
//        int choice;
//        String qtermChoice, selectTerm = "";
//        Random rand;
//        int keepOriginal = 2;
//        
//        variantListPerQuery = new ArrayList<>();
//        
//        makeUniqTermSet(topDocs);
//        System.out.println("Total no. of unique terms : " + uniqTermSet.size());
//        
//        setFeedbackStats(topDocs, luceneQuery.toString(fieldToSearch).split(" "));
//        System.out.println("Feedback docs and terms stats are set.");
//        
//        createCooccurMatrix();
//        System.out.println("size of the co-occurance matrix : " + cooccurMatrix.size());
//        
//        /*  Roulette Wheel selection for query term substitution */
//        String qTerms [] = luceneQuery.toString(fieldToSearch).split(" ");
//        
//        /* event-1 : choose which qterm to substitute (equally likely to choose any term) */
//        for (int itr = 0; itr < 10; itr++) {
//            
//            int count = 1;
//            
//            if (qTerms.length > keepOriginal) {                
//                for (int x = 0; x < keepOriginal; x++) {
//                rand = new Random(); 
//                choice = rand.nextInt(qTerms.length);
//                qtermChoice = qTerms[choice];
//                matcher = pattern.matcher(qtermChoice);
//                while (matcher.matches() || qtermChoice.contains(".") && qtermChoice != selectTerm) {
//                    rand = new Random();
//                    choice = rand.nextInt(qTerms.length);
//                    qtermChoice = qTerms[choice]; 
//                    matcher = pattern.matcher(qtermChoice);
//    //                System.out.println("Qterm selected for substitution : " + qtermChoice);
//                } 
//                selectTerm = qtermChoice;
//                System.out.println("Qterm selected for substitution : " + qtermChoice);
//            }
//                
//        }
//            
//
//            /* event-2 : sample a word from the top-K vocab to replace this word (Roulette Wheel) */
//            if (qTerms.length > 1) {
//                selectTerm = qtermChoice;
//                while (count < qTerms.length) {
//                    selectTerm = rouletteSelection(selectTerm);
//                    qtermChoice = qtermChoice + " ";
//                    qtermChoice = qtermChoice + selectTerm;
//                    System.out.println("new query : " + qtermChoice + "\n");
//                    count++;
//                }
//                variantListPerQuery.add(qtermChoice);
//            }
//            else {
//                selectTerm = qtermChoice;
//                while (count <= qTerms.length) {
//                    selectTerm = rouletteSelection(selectTerm);
//                    qtermChoice = qtermChoice + " ";
//                    qtermChoice = qtermChoice + selectTerm;
//                    System.out.println("new query : " + qtermChoice + "\n");
//                    count++;
//                }
//                variantListPerQuery.add(qtermChoice);
//            }
//        }
//        
//        return variantListPerQuery;
//    }

    /* keep more than one original query term and replace |Q|-m terms (where, m = 1,2,3...|Q|-2) */

    
    public List<String> createVariantsRLMClueWeb(List<String> topRetTerms, Query luceneQuery, int varLen, TopDocs topDocs) throws Exception {
        
        int choice;
        String qtermChoice, selectTerm;
        Random rand;
        
        variantListPerQuery = new ArrayList<>();
        
        makeUniqTermSet(topRetTerms);
        System.out.println("Total no. of unique terms : " + uniqTermSet.size());
        
        setFeedbackStats(topDocs, luceneQuery.toString(fieldToSearch).split(" "));
        System.out.println("Feedback docs and terms stats are set.");
        
        createCooccurMatrix();
        System.out.println("size of the co-occurance matrix : " + cooccurMatrix.size());
        
        /*  Roulette Wheel selection for query term substitution */
        String qTerms [] = luceneQuery.toString(fieldToSearch).split(" ");
        System.out.println("QTERMS : " + qTerms.length);
        
        /* event-1 : choose which qterm to substitute (equally likely to choose any term) */
        for (int itr=0; itr < 10; itr++) {
            
            int count = 1;
            rand = new Random(); 
            choice = rand.nextInt(qTerms.length);
            System.out.println("CHOICE : " + choice);
            qtermChoice = qTerms[choice];
            System.out.println("Qterm Choice : " + qtermChoice);
            matcher = pattern.matcher(qtermChoice);
//            while (matcher.matches() || qtermChoice.contains(".")) {
//                rand = new Random();
//                choice = rand.nextInt(qTerms.length);
//                System.out.println("CHOICE### : " + choice);
//                qtermChoice = qTerms[choice];
//                System.out.println("Qterm Choice### : " + qtermChoice);
//                matcher = pattern.matcher(qtermChoice);
////                System.out.println("Qterm selected for substitution : " + qtermChoice);
//            } 
            System.out.println("Qterm selected for substitution : " + qtermChoice);

            /* event-2 : sample a word from the top-K vocab to replace this word (Roulette Wheel) */
            if (qTerms.length > 1) {
                selectTerm = qtermChoice;
                while (count < qTerms.length) {
                    selectTerm = rouletteSelection(selectTerm);
                    qtermChoice = qtermChoice + " ";
                    qtermChoice = qtermChoice + selectTerm;
                    System.out.println("new query : " + qtermChoice + "\n");
                    count++;
                }
                variantListPerQuery.add(qtermChoice);
            }
            else {
                selectTerm = qtermChoice;
                while (count <= qTerms.length) {
                    selectTerm = rouletteSelection(selectTerm);
                    qtermChoice = qtermChoice + " ";
                    qtermChoice = qtermChoice + selectTerm;
                    System.out.println("new query : " + qtermChoice + "\n");
                    count++;
                }
                variantListPerQuery.add(qtermChoice);
            }
        }
        
        return variantListPerQuery;
    }
}