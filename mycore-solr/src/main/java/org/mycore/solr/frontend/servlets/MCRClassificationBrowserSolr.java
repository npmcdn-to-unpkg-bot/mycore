/*
* $Revision: 23924 $ $Date: 2012-03-20 10:29:36 +0100 (Tue, 20 Mar 2012) $
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

package org.mycore.solr.frontend.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.jdom.Element;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.solr.MCRSolrServerFactory;

/**
 * This servlet provides a way to visually navigate through the tree of
 * categories of a classification. The XML output is transformed to HTML
 * using classificationBrowserData.xsl on the server side, then sent to
 * the client browser, where AJAX does the rest.
 *
 * 
 * @author shermann 
 */
public class MCRClassificationBrowserSolr extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRClassificationBrowserSolr.class);

    private static HttpSolrServer SOLR_SERVER;

    static {
        try {
            SOLR_SERVER = MCRSolrServerFactory.getSolrServer();
        } catch (Exception e) {
            LOGGER.error("Error creating solr server object", e);
        } finally {
            LOGGER.info("Solr: using server at address \"" + SOLR_SERVER.getBaseURL() + "\"");
        }
    }

    public void doGetPost(MCRServletJob job) throws Exception {
        long time = System.currentTimeMillis();

        HttpServletRequest req = job.getRequest();

        String classifID = req.getParameter("classification");
        String categID = req.getParameter("category");
        String objectType = req.getParameter("objecttype");
        String field = req.getParameter("field");
        String restriction = req.getParameter("restriction");
        String parameters = req.getParameter("parameters");

        boolean countResults = Boolean.valueOf(req.getParameter("countresults"));
        boolean addClassId = Boolean.valueOf(req.getParameter("addclassid"));
        boolean uri = Boolean.valueOf(req.getParameter("adduri"));

        String el = req.getParameter("emptyleaves");
        boolean emptyLeaves = true;
        if ((el != null) && (el.trim().length() > 0))
            emptyLeaves = Boolean.valueOf(el);

        LOGGER.info(getClass().getSimpleName() + " " + classifID + " " + (categID == null ? "" : categID));

        MCRCategoryID id = new MCRCategoryID(classifID, categID);
        Element xml = new Element("classificationBrowserData");
        xml.setAttribute("classification", classifID);
        xml.setAttribute("webpage", req.getParameter("webpage"));

        String preCondition = new String();

        if (countResults || field.length() > 0) {
            //maybe we drop this later
            //query += field + ":" + "DUMMY";
            if ((objectType != null) && (objectType.trim().length() > 0)) {
                xml.setAttribute("objectType", objectType);
                preCondition += " object_type:" + objectType;
            }
            // restriction must be in solr query format "field:value"
            if ((restriction != null) && (restriction.trim().length() > 0)) {
                preCondition += " AND " + restriction;
            }
        }

        if (parameters != null)
            xml.setAttribute("parameters", parameters);

        List<Element> data = new ArrayList<Element>();
        MCRCategory category = MCRCategoryDAOFactory.getInstance().getCategory(id, 1);
        if (category == null) {
            job.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find category: " + id);
            return;
        }
        SolrQuery solrQuery = null;
        for (MCRCategory child : category.getChildren()) {
            String childID = child.getId().getID();
            String query = new String(preCondition);
            long numResults = 0;
            if (countResults) {
                if (query.length() > 0) {
                    query += " AND ";
                }
                query += (addClassId ? field + ":" + child.getId().toString() : field + ":" + childID);
                solrQuery = new SolrQuery(query);
                solrQuery.setRows(0);

                numResults = SOLR_SERVER.query(solrQuery).getResults().getNumFound();
                if ((!emptyLeaves) && (numResults < 1))
                    continue;
            }

            Element categoryE = new Element("category");
            data.add(categoryE);
            if (countResults)
                categoryE.setAttribute("numResults", String.valueOf(numResults));

            categoryE.setAttribute("id", childID);
            categoryE.setAttribute("children", Boolean.toString(child.hasChildren()));
            if (solrQuery != null)
                categoryE.setAttribute("query", solrQuery.getQuery());

            if (uri && (child.getURI() != null))
                categoryE.addContent(new Element("uri").setText(child.getURI().toString()));

            addLabel(req, child, categoryE);
        }

        countLinks(req, emptyLeaves, objectType, category, data);
        sortCategories(req, data);
        xml.addContent(data);
        renderToHTML(job, req, xml);

        LOGGER.trace("ClassificationBrowser finished in " + (System.currentTimeMillis() - time) + " ms");
    }

    /**
     * Add label in current lang, otherwise default lang, optional with
     * description
     */
    private void addLabel(HttpServletRequest req, MCRCategory child, Element category) {
        MCRLabel label = child.getCurrentLabel();

        category.addContent(new Element("label").setText(label.getText()));

        // if true, add description
        boolean descr = Boolean.valueOf(req.getParameter("adddescription"));
        if (descr && (label.getDescription() != null))
            category.addContent(new Element("description").setText(label.getDescription()));
    }

    /** Add link count to each category */
    private void countLinks(HttpServletRequest req, boolean emptyLeaves, String objectType, MCRCategory category, List<Element> data) {
        if (!Boolean.valueOf(req.getParameter("countlinks")))
            return;
        if (objectType.trim().length() == 0)
            objectType = null;
        String classifID = category.getId().getRootID();
        Map<MCRCategoryID, Number> count = MCRCategLinkServiceFactory.getInstance().countLinksForType(category, objectType, true);
        for (Iterator<Element> it = data.iterator(); it.hasNext();) {
            Element child = it.next();
            MCRCategoryID childID = new MCRCategoryID(classifID, child.getAttributeValue("id"));
            int num = (count.containsKey(childID) ? count.get(childID).intValue() : 0);
            child.setAttribute("numLinks", String.valueOf(num));
            if ((!emptyLeaves) && (num < 1))
                it.remove();
        }
    }

    /** Sorts by id, by label in current language, or keeps natural order */
    private void sortCategories(HttpServletRequest req, List<Element> data) {
        final String sortBy = req.getParameter("sortby");
        if (sortBy != null)
            Collections.sort(data, new Comparator<Element>() {
                public int compare(Element a, Element b) {
                    if ("id".equals(sortBy))
                        return (a.getAttributeValue("id").compareTo(b.getAttributeValue("id")));
                    else if ("label".equals(sortBy))
                        return (a.getChildText("label").compareToIgnoreCase(b.getChildText("label")));
                    else
                        return 0;
                }
            });
    }

    /** Sends output to client browser */
    private void renderToHTML(MCRServletJob job, HttpServletRequest req, Element xml) throws IOException {
        String style = req.getParameter("style"); // XSL.Style, optional
        if ((style != null) && (style.length() > 0))
            req.setAttribute("XSL.Style", style);

        MCRServlet.getLayoutService().doLayout(req, job.getResponse(), new MCRJDOMContent(xml));
    }

    @Override
    protected boolean allowCrossDomainRequests() {
        return true;
    }
}
