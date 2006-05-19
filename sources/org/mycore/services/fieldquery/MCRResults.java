/*
 * $RCSfile$
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRException;

/**
 * This class represents the results of a query performed by MCRSearcher.
 * Searchers add the hits using the addHit() method. Clients can get the hits,
 * sort the entries and do merge/and/or operations on two different result sets.
 * 
 * Searches may add the same hit (hit with the same ID) more than once. If the
 * hit already is contained in the result set, the data of both objects is
 * merged.
 * 
 * @see MCRHit
 * 
 * @author Arne Seifert
 * @author Frank Lützenkirchen
 */
public class MCRResults {
    /** The list of MCRHit objects */
    private List hits = new ArrayList();

    /**
     * A map containing MCRHit IDs used for and/or operations on two different
     * MCRResult objects
     */
    private HashMap map = new HashMap();

    /** If true, this results are already sorted */
    private boolean isSorted = false;

    /** The unique ID of this result set */
    private String id;

    private static Random random = new Random(System.currentTimeMillis());

    /**
     * Creates a new, empty MCRResults.
     */
    public MCRResults() {
        id = Long.toString(random.nextLong(), 36) + Long.toString(System.currentTimeMillis(), 36);
    }

    /**
     * Returns the unique ID of this result set
     * 
     * @return the unique ID of this result set
     */
    public String getID() {
        return id;
    }

    /**
     * Adds a hit. If there is already a hit with the same ID, the sort data and
     * meta data of both hits are merged and the merged hit replaces the
     * existing hit.
     * 
     * @param hit
     *            the MCRHit to add
     */
    public void addHit(MCRHit hit) {
        String key = hit.getKey();
        if (!map.containsKey(key)) {
            // This is a new entry with new ID
            hits.add(hit);
            map.put(key, hit);
        } else {
            // Merge data of existing hit with new one with the same ID
            MCRHit existing = getHit(key);
            MCRHit merged = MCRHit.merge(hit, existing);
            hits.set(hits.indexOf(existing), merged);
            map.put(key, merged);
        }
    }

    /**
     * Gets a single MCRHit. As long as isSorted() returns false, the order of
     * the hits is natural order.
     * 
     * @param i
     *            the position of the hit.
     * @return the hit at this position, or null if position is out of bounds
     */
    public MCRHit getHit(int i) {
        if ((i >= 0) && (i < hits.size())) {
            return (MCRHit) hits.get(i);
        }
        return null;
    }

    /**
     * Returns the MCRHit with the given key, if it is in this results.
     * 
     * @param key
     *            the key of the hit
     * @return the MCRHit, if it exists
     */
    private MCRHit getHit(String key) {
        if (map.containsKey(key)) {
            return (MCRHit) (map.get(key));
        }
        return null;
    }

    /**
     * Returns the number of hits currently in this results
     * 
     * @return the number of hits
     */
    public int getNumHits() {
        return hits.size();
    }

    /**
     * Cuts the result list to the given maximum size, if more hits are present.
     * 
     * @param maxResults
     *            the number of results to be left
     */
    public void cutResults(int maxResults) {
        while ((hits.size() > maxResults) && (maxResults >= 0)) {
            MCRHit hit = (MCRHit) (hits.remove(hits.size() - 1));
            map.remove(hit.getKey());
        }
    }

    /**
     * The searcher must set this to true, if the hits already have been added
     * in sorted order.
     * 
     * @param value
     *            true, if sorted, false otherwise
     */
    public void setSorted(boolean value) {
        isSorted = value;
    }

    /**
     * Returns true if this result list is currently sorted
     * 
     * @return true if this result list is currently sorted
     */
    public boolean isSorted() {
        return isSorted;
    }

    /**
     * Sorts this results by the given sort criteria.
     * 
     * @param sortByList
     *            a List of MCRSortBy objects
     */
    public void sortBy(final List sortByList) {
        Collections.sort(this.hits, new Comparator() {
            public int compare(Object oa, Object ob) {
                MCRHit a = (MCRHit) oa;
                MCRHit b = (MCRHit) ob;

                int result = 0;

                for (int i = 0; (result == 0) && (i < sortByList.size()); i++) {
                    MCRSortBy sortBy = (MCRSortBy) (sortByList.get(i));
                    result = a.compareTo(sortBy.getField(), b);
                    if (sortBy.getSortOrder() == MCRSortBy.DESCENDING) {
                        result *= -1;
                    }
                }

                return result;
            }
        });

        setSorted(true);
    }

    /**
     * Returns a XML element containing hits and their data
     * 
     * @param min
     *            the position of the first hit to include in output
     * @param max
     *            the position of the last hit to include in output
     * @return a 'results' element with attributes 'sorted' and 'numHits' and
     *         hit child elements
     */
    public Element buildXML(int min, int max) {
        Element results = new Element("results", MCRFieldDef.mcrns);
        results.setAttribute("id", getID());
        results.setAttribute("sorted", Boolean.toString(isSorted()));
        results.setAttribute("numHits", String.valueOf(getNumHits()));

        for (int i = min; i <= max; i++)
            results.addContent(((MCRHit) hits.get(i)).buildXML());

        return results;
    }

    /**
     * Returns a XML element containing all hits and their data
     * 
     * @return a 'results' element with attributes 'sorted' and 'numHits' and
     *         hit child elements
     */
    public Element buildXML() {
        return buildXML(0, getNumHits() - 1);
    }

    /**
     * Parses a XML document of a results object containing all hits and their
     * data
     * 
     * @param xml
     *            the XML results document
     * @param hostAlias
     *            the remote host alias (may be null)
     * @return the parsed MCRResults object
     */
    public static MCRResults parseXML(Document doc, String hostAlias) {
        Element xml = doc.getRootElement();
        MCRResults results = new MCRResults();
        results.id = xml.getAttributeValue("id", "");
        if (results.id.length() == 0)
            throw new MCRException("MCRResults id attribute is empty");

        results.isSorted = "true".equals(xml.getAttributeValue("sorted"));

        List hitList = xml.getChildren();
        for (int i = 0; i < hitList.size(); i++) {
            Element hitElement = (Element) (hitList.get(i));
            results.addHit(MCRHit.parseXML(hitElement, hostAlias));
        }
        return results;
    }

    public String toString() {
        return new XMLOutputter(Format.getPrettyFormat()).outputString(buildXML());
    }

    /**
     * Returns a new MCRResults that only contains those hits that are members
     * of both source MCRResults objects. The compare is based on the ID of the
     * hits. The data of each single hit is merged from both results.
     * 
     * @param a
     *            the first result list
     * @param b
     *            the other result list
     * @return the new result list
     */
    public static MCRResults and(MCRResults a, MCRResults b) {
        MCRResults res = new MCRResults();

        for (int i = 0; i < a.getNumHits(); i++) {
            MCRHit hitA = a.getHit(i);
            MCRHit hitB = b.getHit(hitA.getKey());

            if (hitB != null) {
                res.addHit(MCRHit.merge(hitA, hitB));
            }
        }

        return res;
    }

    /**
     * Returns a new MCRResults that contains those hits that are members of at
     * least one of the source MCRResults objects. The compare is based on the
     * ID of the hits. The data of each single hit is merged from both results.
     * 
     * @param a
     *            the first result list
     * @param b
     *            the other result list
     * @return the new result list
     */
    public static MCRResults or(MCRResults a, MCRResults b) {
        MCRResults res = new MCRResults();

        for (int i = 0; i < a.getNumHits(); i++) {
            MCRHit hitA = a.getHit(i);
            MCRHit hitB = b.getHit(hitA.getKey());

            MCRHit hitC = MCRHit.merge(hitA, hitB);
            res.addHit(hitC);
        }

        for (int i = 0; i < b.getNumHits(); i++)
            if (!res.map.containsKey(b.getHit(i).getID())) {
                res.addHit(b.getHit(i));
            }

        return res;
    }

    /**
     * Returns a new MCRResults that contains all hits of both source MCRResults
     * objects. No compare is done, it is assumed that the two lists do not have
     * common members.
     * 
     * @param a
     *            the first result list
     * @param b
     *            the other result list
     * @return the new result list
     */
    public static MCRResults merge(MCRResults a, MCRResults b) {
        MCRResults merged = new MCRResults();

        for (int i = 0; i < a.getNumHits(); i++)
            merged.addHit(a.getHit(i));

        for (int i = 0; i < b.getNumHits(); i++)
            merged.addHit(b.getHit(i));

        return merged;
    }
}
