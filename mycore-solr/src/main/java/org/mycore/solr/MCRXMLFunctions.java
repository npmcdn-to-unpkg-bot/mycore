/**
 * 
 */
package org.mycore.solr;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

/**
 * @author shermann
 *
 */
public class MCRXMLFunctions {

    /**
     * Convenience method for retrieving the result count for a given solr query.
     * 
     * @param q the query to execute (in solr syntax)
     * 
     * @return the amount of documents matching the given query
     *  
     * @throws SolrServerException
     */
    public static long getNumFound(String q) throws SolrServerException {
        if (q == null || q.length() == 0) {
            throw new IllegalArgumentException("The query string must not be null");
        }
        SolrQuery solrQuery = new SolrQuery(q);
        solrQuery.set("rows", 0);
        QueryResponse queryResponse;
        queryResponse = MCRSolrClientFactory.getSolrClient().query(solrQuery);
        return queryResponse.getResults().getNumFound();
    }
}
