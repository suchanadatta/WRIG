/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package common.clueweb;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author suchana
 */

public class ClueWebQueriesTrecFormat {
    
    String queryFile;
    String outputFile;
    
    public ClueWebQueriesTrecFormat(String queryFile, String outputFile) throws IOException {
        
        this.queryFile = queryFile;
        this.outputFile = outputFile;
    }
    
    public void makeClueWebQueries() throws FileNotFoundException, IOException {
        
        String fileContent, qnum, qtitle, qdesc;
        Pattern p_num, p_title, p_desc;
        Matcher m_num, m_title, m_desc;
        
        PrintWriter writer = new PrintWriter(outputFile);
        
        BufferedReader br = new BufferedReader(new FileReader(queryFile));
        String line = br.readLine();
        StringBuilder sb = new StringBuilder(); 
        while(line != null){ 
            sb.append(line).append("\n");
            line = br.readLine();
        } 
        fileContent = sb.toString();
//        System.out.println("file content : " + fileContent);
        fileContent = fileContent.replaceAll("\"", "").replaceAll("", "").replaceAll("\n", "").replaceAll("\r", "");
//        System.out.println("file content : " + fileContent);
        
        p_num = Pattern.compile("<num>(.+?)</num>");
        m_num = p_num.matcher(fileContent);
        p_title = Pattern.compile("<title>(.+?)</title>");
        m_title = p_title.matcher(fileContent);
        p_desc = Pattern.compile("<desc>(.+?)</desc>");
        m_desc = p_desc.matcher(fileContent);
        writer.print("<topics>\n\n");
        
        while (m_num.find()) {
            qnum = m_num.group(1).trim().replaceAll("\\s{2,}", "");
            writer.print("<top>\n<num>" + qnum + "</num>\n");
            System.out.println("query no : " + qnum);
            if (m_title.find()){
                qtitle = m_title.group(1).replaceAll("\\s{2,}", " ").replace(".", " ").replace("'", " ").trim();
                writer.print("<title>" + qtitle + "</title>\n");
                System.out.println("parsed title : " + qtitle);
            }
            if (m_desc.find()) {
                qdesc = m_desc.group(1).replaceAll("\\s{2,}", " ").replace(".", " ").replace("'", " ").trim();
                writer.print("<desc>" + qdesc + "</desc>\n");
                writer.print("<narr>" + qdesc + "</narr>\n");
                System.out.println("parsed desc : " + qdesc);
            }
            writer.print("</top>\n\n");
        }
        writer.print("</topics>");
        writer.close();
    }
    
    public static void main(String[] args) {

        if (args.length < 1) {
            args = new String[2];
            args[0] = "/store/causalIR/drmm/clueweb_exp/query_train.xml";
            args[1] = "/store/causalIR/drmm/clueweb_exp/query_train_trec.xml";
        }

        try {
            ClueWebQueriesTrecFormat cwqueries = new ClueWebQueriesTrecFormat(args[0], args[1]);

            cwqueries.makeClueWebQueries();

        } catch (IOException ex) {
        }
    }  
}
