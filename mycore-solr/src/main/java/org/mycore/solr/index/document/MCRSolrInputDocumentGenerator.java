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
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.solr.index.document.jaxb.MCRSolrInputDocument;
import org.mycore.solr.index.document.jaxb.MCRSolrInputField;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrInputDocumentGenerator {
    private static Logger LOGGER = Logger.getLogger(MCRSolrInputDocumentGenerator.class);

    public static final JAXBContext JAXB_CONTEXT = initContext();

    private static JAXBContext initContext() {
        try {
            return JAXBContext.newInstance(MCRSolrInputDocument.class.getPackage().getName(), MCRSolrInputDocument.class.getClassLoader());
        } catch (JAXBException e) {
            throw new MCRException("Could not instantiate JAXBContext.", e);
        }
    }

    public static SolrInputDocument getSolrInputDocument(MCRSolrInputDocument jaxbDoc) {
        SolrInputDocument doc = new SolrInputDocument();
        HashSet<MCRSolrInputField> duplicateFilter = new HashSet<>();
        for (MCRSolrInputField field : jaxbDoc.getField()) {
            if (field.getValue().isEmpty() || duplicateFilter.contains(field)) {
                continue;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("adding " + field.getName() + "=" + field.getValue());
            }
            duplicateFilter.add(field);
            doc.addField(field.getName(), field.getValue());
        }
        return doc;
    }

    public static SolrInputDocument getSolrInputDocument(MCRContent source) throws JAXBException, IOException {
        if (source instanceof MCRJAXBContent) {
            @SuppressWarnings("unchecked")
            MCRJAXBContent<MCRSolrInputDocument> jaxbContent = (MCRJAXBContent<MCRSolrInputDocument>) source;
            return getSolrInputDocument(jaxbContent.getObject());
        }
        Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
        MCRSolrInputDocument solrDocument = (MCRSolrInputDocument) unmarshaller.unmarshal(source.getSource());
        return getSolrInputDocument(solrDocument);
    }

    public static SolrInputDocument getSolrInputDocument(Element input) {
        SolrInputDocument doc = new SolrInputDocument();
        HashSet<MCRSolrInputField> duplicateFilter = new HashSet<>();
        List<Element> fieldElements = input.getChildren("field");
        for (Element fieldElement : fieldElements) {
            MCRSolrInputField field = new MCRSolrInputField();
            field.setName(fieldElement.getAttributeValue("name"));
            field.setValue(fieldElement.getText());
            if (field.getValue().isEmpty() || duplicateFilter.contains(field)) {
                continue;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("adding " + field.getName() + "=" + field.getValue());
            }
            duplicateFilter.add(field);
            doc.addField(field.getName(), field.getValue());
        }
        return doc;
    }
}
