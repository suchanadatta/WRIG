/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drmm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 *
 * @author Debasis
 */
public class QueryObject {
    public String       id;
    public String       title;
    public String       desc;
    public String       narr;
    public Query        luceneQuery;
    
    @Override
    public String toString() {
        return luceneQuery.toString();
    }

    public QueryObject() {}
    
    public QueryObject(String id, String title, String desc, String narr) { // copy constructor
        this.id = id;
        this.title = title;
        this.desc = desc;
        this.narr = narr;
    }

    public QueryObject(QueryObject that) { // copy constructor
        this.id = that.id;
        this.title = that.title;
        this.desc = that.desc;
        this.narr = that.narr;
    }

    public void setLuceneQueryObj(Analyzer analyzer, String fieldName) { 
        BooleanQuery query = new BooleanQuery();
        for (String s : title.split(" ")) {
            Term term1 = new Term(fieldName, s);
            //create the term query object
            Query query1 = new TermQuery(term1);
            //query1.setBoost(1.2f);
            query.add(query1, BooleanClause.Occur.SHOULD);
        }
        this.luceneQuery = query;
    }

    public Query getLuceneQueryObj() { return luceneQuery; }

    public Set<Term> getQueryTerms() {
        Set<Term> terms = new HashSet<>();
      //  luceneQuery.extractTerms(terms);
        return terms;
    }

    /**
     * Returns a list containing all the analyzed query terms.
     * @param fieldName
     * @return 
     */
    public List<String> getQueryTerms(String fieldName) {

        String[] split = luceneQuery.toString(fieldName).split(" ");
        List<String> al;
	al = Arrays.asList(split);
        return al;
    }
}
