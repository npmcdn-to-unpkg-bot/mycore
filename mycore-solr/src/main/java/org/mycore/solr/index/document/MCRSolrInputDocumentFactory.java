/*
 * $Id$
 * $Revision: 5697 $ $Date: Apr 16, 2013 $
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

package org.mycore.solr.index.document;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public abstract class MCRSolrInputDocumentFactory {

    private static MCRSolrInputDocumentFactory instance = (MCRSolrInputDocumentFactory) MCRConfiguration.instance().getInstanceOf(
        "MCR.Module-solr.SolrInputDocumentFactory", MCRSolrTransformerInputDocumentFactory.class.getCanonicalName());

    public static MCRSolrInputDocumentFactory getInstance() {
        return instance;
    }

    public abstract SolrInputDocument getDocument(MCRObjectID id, MCRContent content) throws SAXException, IOException;

    public abstract Iterator<SolrInputDocument> getDocuments(Map<MCRObjectID, MCRContent> contentMap) throws IOException, SAXException;

    public SolrInputDocument getDocument(MCRObjectID id) throws SAXException, IOException {
        MCRContent content = MCRXMLMetadataManager.instance().retrieveContent(id);
        return getDocument(id, content);
    }

    public SolrInputDocument getDocument(MCRBase derOrObj) throws SAXException, IOException {
        MCRBaseContent content = new MCRBaseContent(derOrObj);
        return getDocument(derOrObj.getId(), content);
    }

}
