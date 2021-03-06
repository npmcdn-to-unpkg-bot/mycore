/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.services.fieldquery;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.parsers.bool.MCRCondition;

/** Represents a query with its condition and optional parameters */
public class MCRQuery {

    /** The query condition */
    private MCRCondition cond;

    /** The maximum number of results, default is 0 = unlimited */
    private int maxResults = 0;

    /** A list of MCRSortBy criteria, may be empty */
    private List<MCRSortBy> sortBy = new ArrayList<MCRSortBy>();

    /** A cached xml representation of the query */
    private Document doc = null;

    /**
     * Builds a new MCRQuery object without sort criteria and unlimited results.
     * 
     * @param cond
     *            the query conditions
     */
    public MCRQuery(MCRCondition cond) {
        this.cond = MCRQueryParser.normalizeCondition(cond);
    }

    /**
     * Builds a new MCRQuery object with sort criteria and limited number of
     * results.
     * 
     * @param cond
     *            the query conditions
     * @param sortBy
     *            a list of MCRSortBy criteria for sorting the results
     * @param maxResults
     *            the maximum number of results to return
     */
    public MCRQuery(MCRCondition cond, List<MCRSortBy> sortBy, int maxResults) {
        this.cond = MCRQueryParser.normalizeCondition(cond);
        this.setSortBy(sortBy);
        setMaxResults(maxResults);
    }

    /**
     * Returns the query condition
     * 
     * @return the query condition
     */
    public MCRCondition getCondition() {
        return cond;
    }

    /**
     * Returns the maximum number of results the query should return
     * 
     * @return the maximum number of results, or 0
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * Sets the maximum number of results the query should return. Default is 0
     * which means "return all results".
     * 
     * @param maxResults
     *            the maximum number of results
     */
    public void setMaxResults(int maxResults) {
        if (maxResults < 0) {
            maxResults = 0;
        }
        this.maxResults = maxResults;
        doc = null;
    }

    /**
     * Returns the list of MCRSortBy criteria for sorting query results
     * 
     * @return a list of MCRSortBy objects, may be empty
     */
    public List<MCRSortBy> getSortBy() {
        return sortBy;
    }

    /**
     * Sets the sort criteria for the query results
     * 
     * @param sortBy
     *            a list of MCRSortBy objects, may be empty
     */
    public void setSortBy(List<MCRSortBy> sortBy) {
        if (sortBy == null) {
            sortBy = new ArrayList<MCRSortBy>();
        }
        this.sortBy = sortBy;
        doc = null;
    }

    /**
     * Sets the sort criteria for the query results
     * 
     * @param sortBy
     *            a MCRSortBy object
     */
    public void setSortBy(MCRSortBy sortBy) {
        this.sortBy = new ArrayList<MCRSortBy>();
        if (sortBy != null) {
            this.sortBy.add(sortBy);
        }
        doc = null;
    }

    /**
     * Builds a XML representation of the query
     * 
     * @return a XML document containing all query parameters
     */
    public synchronized Document buildXML() {
        if (doc == null) {
            Element query = new Element("query");
            query.setAttribute("maxResults", String.valueOf(maxResults));

            if (sortBy != null && sortBy.size() > 0) {
                Element sortByElem = new Element("sortBy");
                query.addContent(sortByElem);
                for (MCRSortBy sb : sortBy) {
                    Element ref = new Element("field");
                    ref.setAttribute("name", sb.getFieldName());
                    ref.setAttribute("order", sb.getSortOrder() ? "ascending" : "descending");
                    sortByElem.addContent(ref);
                }
            }

            Element conditions = new Element("conditions");
            query.addContent(conditions);
            conditions.setAttribute("format", "xml");
            conditions.addContent(cond.toXML());
            doc = new Document(query);
        }
        return doc;
    }

    /**
     * Parses a XML representation of a query.
     * 
     * @param doc
     *            the XML document
     * @return the parsed MCRQuery
     */
    public static MCRQuery parseXML(Document doc) {
        Element xml = doc.getRootElement();
        Element conditions = xml.getChild("conditions");
        MCRQuery query = null;
        if (conditions.getAttributeValue("format", "xml").equals("xml")) {
            Element condElem = (Element) conditions.getChildren().get(0);
            query = new MCRQuery(new MCRQueryParser().parse(condElem));
        } else {
            String queryString = conditions.getTextTrim();
            query = new MCRQuery(new MCRQueryParser().parse(queryString));
        }

        String max = xml.getAttributeValue("maxResults", "");
        if (max.length() > 0) {
            query.setMaxResults(Integer.parseInt(max));
        }

        List<MCRSortBy> sortBy = null;
        Element sortByElem = xml.getChild("sortBy");

        if (sortByElem != null) {
            List children = sortByElem.getChildren();
            sortBy = new ArrayList<MCRSortBy>(children.size());

            for (Object aChildren : children) {
                Element sortByChild = (Element) aChildren;
                String name = sortByChild.getAttributeValue("name");
                String ad = sortByChild.getAttributeValue("order");

                boolean direction = "ascending".equals(ad) ? MCRSortBy.ASCENDING : MCRSortBy.DESCENDING;
                sortBy.add(new MCRSortBy(name, direction));
            }
        }
        if (sortBy != null) {
            query.setSortBy(sortBy);
        }

        return query;
    }
}
