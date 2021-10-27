/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package drmm;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 *
 * @author suchana
 */

public class TRECQueryParser {
    StringBuffer buff;      // Accumulation buffer for storing the current topic
    String queryFile;
    QueryObject  query;
    Analyzer analyzer;

    public TRECQueryParser(String fileName, Analyzer analyzer) {
        this.queryFile = fileName;
        this.analyzer = analyzer;
    }

    public List<QueryObject> makeQuery() throws IOException {
        
        String fileContent, qid, qtext = null, analyzedQuery = null; 
        Pattern p_qid, p_qtext;
        Matcher m_qid, m_qtext;
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(queryFile)));
        List<QueryObject> queries = new ArrayList<>();
        QueryObject qo;
        
        String line = br.readLine();
        StringBuilder sb = new StringBuilder(); 
        while(line != null){ 
            sb.append(line).append("\n");
            line = br.readLine();
        }
        fileContent = sb.toString();
        fileContent = fileContent.replaceAll("\"", "").replaceAll("", "").replaceAll("\n", "").replaceAll("\r", "");
        
        p_qid = Pattern.compile("<num>(.+?)</num>");
        m_qid = p_qid.matcher(fileContent);
        p_qtext = Pattern.compile("<title>(.+?)</title>");
        m_qtext = p_qtext.matcher(fileContent);
        
        while (m_qid.find()) {
            
            qid = m_qid.group(1).trim().replaceAll("\\s{2,}", "");
            if (m_qtext.find()){
                qtext = m_qtext.group(1).trim().replaceAll("\\s{2,}", " ");
//                System.out.println("parsed query : " + qtext);
                qo = new QueryObject(qid, qtext, "", "");
                String analyzedTitle = analyze(qo);
                qo.title = analyzedTitle;
                qo.luceneQuery = makeLuceneQuery(qo, "content");
                queries.add(qo);
            }
        }
        return queries;
    }

    public String analyze(QueryObject qo) throws IOException {
        
        StringBuffer tokenizedContentBuff = new StringBuffer();
        TokenStream stream = analyzer.tokenStream("dummy", new StringReader(qo.title));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            tokenizedContentBuff.append(term).append(" ");
        }

        stream.end();
        stream.close();

        return tokenizedContentBuff.toString();
    }

    public Query makeLuceneQuery(QueryObject qo, String fieldName) throws IOException {
        StringBuffer tokenizedContentBuff = new StringBuffer();

        BooleanQuery query = new BooleanQuery();
        for (String s : qo.title.split(" ")) {
            Term term1 = new Term(fieldName, s);
            //create the term query object
            Query query1 = new TermQuery(term1);
            //query1.setBoost(1.2f);
            query.add(query1, BooleanClause.Occur.SHOULD);
        }
        return query;
    }
}
