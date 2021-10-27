/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package neuralqpp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.search.Query;


/**
 *
 * @author suchana
 */

public class CreateQueryVariantsW2V {
    
    GenerateQueryVariantsTrec    gqv;
    GenerateQueryVariantsClueWeb gqvc;
    String                       fieldToSearch;
    String                       qtermNN;
    Pattern                      pattern;
    String                       regex;
    Matcher                      matcher; 
    List<String>                 variantListPerQuery;
    HashMap<String, String[]>    qterm_NN_map;
    Random                       rand;
        
    
    public CreateQueryVariantsW2V(GenerateQueryVariantsTrec gqv) throws IOException {

        this.gqv = gqv;
        this.fieldToSearch = gqv.fieldToSearch;
        this.qtermNN = gqv.qtermNN;
        regex = "(.)*(\\d)(.)*";      
        pattern = Pattern.compile(regex);
    }
    
    public CreateQueryVariantsW2V(GenerateQueryVariantsClueWeb gqvc) throws IOException {

        this.gqvc = gqvc;
        this.fieldToSearch = gqvc.fieldToSearch;
        this.qtermNN = gqvc.qtermNN;
        regex = "(.)*(\\d)(.)*";      
        pattern = Pattern.compile(regex);
    }    
    
    public void makeQueryTermNN() throws FileNotFoundException, IOException {
        
        qterm_NN_map = new LinkedHashMap<>();
        System.out.println("Read query nearest neighbours from path : " + qtermNN);
        File queryNN = new File(qtermNN.trim());
        BufferedReader br = new BufferedReader(new FileReader(queryNN));
        String line = br.readLine();
        while(line != null){ 
            String[] qterm_NN_list = line.split("\t");
//            System.out.println("qid : " + qterm_NN_list[0]);
            String[] NN_split_list = qterm_NN_list[1].split(",");
            qterm_NN_map.put(qterm_NN_list[0], NN_split_list);
            line = br.readLine();            
        }
        
        // to print the map
//        System.out.println("size of the map : " + qterm_NN_map.size());
//        for (Map.Entry<String, String[]> termSet : qterm_NN_map.entrySet()){
//            String term = termSet.getKey();
//            String[] temp = termSet.getValue();
//            System.out.print("Term : " + term + "\t");
//            for (String wp : temp){
//                System.out.print("link : " + wp);
//            }
//            System.out.print("\n");
//        }
    }
    
       
    public List<String> createVariantsW2VTrec(Query luceneQuery, int varLen) throws Exception {
        
        int choice;
        String qtermChoice, selectTerm;
        String[] NNList;
        
        variantListPerQuery = new ArrayList<>();
        
        makeQueryTermNN();
        System.out.println("size of the Nearest Neighbour matrix : " + qterm_NN_map.size());
        
        /*  Roulette Wheel selection for query term substitution */
        String qTerms [] = luceneQuery.toString(fieldToSearch).split(" ");
        
        /* event-1 : choose which qterm to substitute (equally likely to choose any term) */
        for (int itr=0; itr < 30; itr++) {
            
            int count = 1;
            rand = new Random(); 
            choice = rand.nextInt(qTerms.length);
            qtermChoice = qTerms[choice];
            matcher = pattern.matcher(qtermChoice);
            
            /* random no. generation and select the fittest candidate */
            if (qTerms.length > 1) {
                while (matcher.matches() || qtermChoice.contains(".")) {
                    rand = new Random();
                    choice = rand.nextInt(qTerms.length);
                    qtermChoice = qTerms[choice]; 
                    matcher = pattern.matcher(qtermChoice);
                    System.out.println("Qterm selected for substitution : " + qtermChoice);
                } 
                System.out.println("Qterm selected for substitution : " + qtermChoice);
                        
                selectTerm = qtermChoice;
                NNList = qterm_NN_map.get(selectTerm);
                while (count < qTerms.length) {
                    rand = new Random();
                    choice = rand.nextInt(NNList.length);
                    System.out.println("roulette : " + choice);
                    selectTerm = NNList[choice];
                    if (!qtermChoice.contains(selectTerm)) {
                        qtermChoice = qtermChoice + " " + selectTerm;
                        System.out.println("new query : " + qtermChoice + "\n");
                        count++;
                    }
                }
                variantListPerQuery.add(qtermChoice);
            }
            else {
                selectTerm = qtermChoice;
                NNList = qterm_NN_map.get(selectTerm);
                while (count <= qTerms.length) {
                    rand = new Random();
                    choice = rand.nextInt(NNList.length);
                    System.out.println("roulette_here : " + choice);
                    selectTerm = NNList[choice];
                    if (!qtermChoice.contains(selectTerm)) {
                        qtermChoice = qtermChoice + " " + selectTerm;
                        System.out.println("new query : " + qtermChoice + "\n");
                        count++;
                    }
                }
                variantListPerQuery.add(qtermChoice);
            }
        }
 
        return variantListPerQuery;        
    }
    
    public List<String> createVariantsW2VClueWeb(Query luceneQuery, int varLen) throws Exception {
        
        int choice;
        String qtermChoice, selectTerm;
        String[] NNList;
        
        variantListPerQuery = new ArrayList<>();
        
        makeQueryTermNN();
        System.out.println("size of the Nearest Neighbour matrix : " + qterm_NN_map.size());
        
        /*  Roulette Wheel selection for query term substitution */
        String qTerms [] = luceneQuery.toString(fieldToSearch).split(" ");
        
        /* event-1 : choose which qterm to substitute (equally likely to choose any term) */
        for (int itr=0; itr < 15; itr++) {
            
            int count = 1;
            rand = new Random(); 
            choice = rand.nextInt(qTerms.length);
            qtermChoice = qTerms[choice];
            matcher = pattern.matcher(qtermChoice);
            
            /* random no. generation and select the fittest candidate */
            if (qTerms.length > 1) {
                while (matcher.matches() || qtermChoice.contains(".")) {
                    rand = new Random();
                    choice = rand.nextInt(qTerms.length);
                    qtermChoice = qTerms[choice]; 
                    matcher = pattern.matcher(qtermChoice);
                    System.out.println("Qterm selected for substitution : " + qtermChoice);
                } 
                System.out.println("Qterm selected for substitution : " + qtermChoice);
                        
                selectTerm = qtermChoice;
                NNList = qterm_NN_map.get(selectTerm);
                while (count < qTerms.length) {
                    rand = new Random();
                    choice = rand.nextInt(NNList.length);
                    System.out.println("roulette : " + choice);
                    selectTerm = NNList[choice];
                    if (!qtermChoice.contains(selectTerm)) {
                        qtermChoice = qtermChoice + " " + selectTerm;
                        System.out.println("new query : " + qtermChoice + "\n");
                        count++;
                    }
                }
                variantListPerQuery.add(qtermChoice);
            }
            else {
                selectTerm = qtermChoice;
                NNList = qterm_NN_map.get(selectTerm);
                while (count <= qTerms.length) {
                    rand = new Random();
                    choice = rand.nextInt(NNList.length);
                    System.out.println("roulette_here : " + choice);
                    selectTerm = NNList[choice];
                    if (!qtermChoice.contains(selectTerm)) {
                        qtermChoice = qtermChoice + " " + selectTerm;
                        System.out.println("new query : " + qtermChoice + "\n");
                        count++;
                    }
                }
                variantListPerQuery.add(qtermChoice);
            }
        }
 
        return variantListPerQuery;        
    }
}


