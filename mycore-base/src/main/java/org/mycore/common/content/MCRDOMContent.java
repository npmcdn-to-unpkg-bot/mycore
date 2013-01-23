/*
 * $Revision$ 
 * $Date$
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

package org.mycore.common.content;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.jdom2.JDOMException;
import org.jdom2.input.DOMBuilder;
import org.jdom2.output.XMLOutputter;
import org.w3c.dom.Document;

/**
 * Reads MCRContent from a W3C DOM XML document.
 * 
 * @author Frank L\u00FCtzenkichen
 */
public class MCRDOMContent extends MCRXMLContent {

    private Document dom;

    /**
     * @param dom the W3C DOM XML document to read from 
     */
    public MCRDOMContent(Document dom) {
        this.dom = dom;
        super.docType = dom.getDoctype() == null ? dom.getDocumentElement().getLocalName() : dom.getDoctype().getName();
    }

    @Override
    public Source getSource() {
        DOMSource source = new DOMSource(dom);
        source.setSystemId(systemId);
        return source;
    }

    @Override
    public void sendTo(OutputStream out) throws IOException {
        org.jdom2.Document jdom;
        try {
            jdom = asXML();
        } catch (JDOMException ex) {
            throw new IOException(ex);
        }
        new XMLOutputter(format).output(jdom, out);
    }

    @Override
    public org.jdom2.Document asXML() throws JDOMException {
        return new DOMBuilder().build(dom);
    }
}
