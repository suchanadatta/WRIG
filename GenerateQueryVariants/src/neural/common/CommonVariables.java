/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package neural.common;

/**
 *
 * @author suchana
 */
public class CommonVariables {
    
    /**
     * The unique document id of each of the documents.
     */
    static final public String FIELD_ID = "docid";
    
    /**
     * The analyzed content of each of the documents.
     */
    static final public String FIELD_BOW = "content";
//    static final public String FIELD_BOW = "words";
    /**
     * Analyzed full content (including tag informations): Mainly used for WT10G initial retrieval.
     */
    static final public String FIELD_FULL_BOW = "full-content";

    /**
     * The meta content, that is removed from the the full-content to get the cleaned-content.
     */
    static final public String FIELD_META = "meta-content";
    
    /**
     * The news category of WashingtonPost corpus
     */
    //static final public String FIELD_CAT = "category";    
}
