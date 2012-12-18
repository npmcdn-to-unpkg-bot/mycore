/*
 * 
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.datamodel.metadata;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.xml.sax.SAXParseException;

/**
 * This class holds all information of a derivate. For persistence operations
 * see methods of {@link MCRMetadataManager}.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler
 * @version $Revision$ $Date: 2010-09-30 17:49:21 +0200 (Thu, 30 Sep
 *          2010) $
 */
final public class MCRDerivate extends MCRBase {
    private static final Logger LOGGER = Logger.getLogger(MCRDerivate.class);

    // the object content
    private MCRObjectDerivate mcr_derivate;

    /**
     * This is the constructor of the MCRDerivate class. It make an instance of
     * the parser class and the metadata class.
     * 
     * @exception MCRException
     *                general Exception of MyCoRe
     * @exception MCRConfigurationException
     *                a special exception for configuartion data
     */
    public MCRDerivate() throws MCRException, MCRConfigurationException {
        super();
        mcr_derivate = new MCRObjectDerivate(getId());
    }

    /**
     * @param bytes
     * @param valid
     * @throws SAXParseException
     */
    public MCRDerivate(byte[] bytes, boolean valid) throws SAXParseException, IOException {
        this();
        setFromXML(bytes, valid);
    }

    /**
     * @param doc
     * @throws SAXParseException
     */
    public MCRDerivate(Document doc) {
        this();
        setFromJDOM(doc);
    }

    /**
     * @param uri
     * @throws SAXParseException
     */
    public MCRDerivate(URI uri) throws SAXParseException, IOException {
        this();
        setFromURI(uri);
    }

    /**
     * This methode return the instance of the MCRObjectDerivate class. If this
     * was not found, null was returned.
     * 
     * @return the instance of the MCRObjectDerivate class
     */
    public final MCRObjectDerivate getDerivate() {
        return mcr_derivate;
    }

    /**
     * The given DOM was convert into an internal view of metadata. This are the
     * object ID and the object label, also the blocks structure, flags and
     * metadata.
     * 
     * @exception MCRException
     *                general Exception of MyCoRe
     */
    protected final void setUp() throws MCRException {
        super.setUp();

        // get the derivate data of the object
        Element derivateElement = jdom_document.getRootElement().getChild("derivate");
        mcr_derivate = new MCRObjectDerivate(mcr_id, derivateElement);
    }

    /**
     * This methode create a XML stream for all object data.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Document with the XML data of the object as byte array
     */
    @Override
    public final org.jdom.Document createXML() throws MCRException {
        Document doc = super.createXML();
        Element elm = doc.getRootElement();
        elm.addContent(mcr_derivate.createXML());
        elm.addContent(mcr_service.createXML());
        return doc;
    }

    @Override
    protected String getRootTagName() {
        return "mycorederivate";
    }

    /**
     * Reads all files and urns from the derivate.
     * 
     * @return A {@link Map} which contains the files as key and the urns as value. If no URN assigned the map will be empty.
     */
    public Map<String, String> getUrnMap() {
        Map<String, String> fileUrnMap = new HashMap<String, String>();

        try {
            XPath filesetPath = XPath.newInstance("./mycorederivate/derivate/fileset");
            Object obj = filesetPath.selectSingleNode(this.createXML());
            if (obj == null) {
                return fileUrnMap;
            }

            Element result = (Element) obj;
            String urn = result.getAttributeValue("urn");

            if (urn != null) {
                XPath filePath = XPath.newInstance("./mycorederivate/derivate/fileset[@urn='" + urn + "']/file");
                @SuppressWarnings("unchecked")
                List<Element> files = filePath.selectNodes(this.createXML());

                for (Element currentFileElement : files) {
                    String currentUrn = currentFileElement.getChildText("urn");
                    String currentFile = currentFileElement.getAttributeValue("name");
                    fileUrnMap.put(currentFile, currentUrn);
                }
            }
        } catch (JDOMException ex) {
            LOGGER.error("Read derivate XML cause error", ex);
        }
        return fileUrnMap;
    }

    /**
     * The methode receive the multimedia object(s) for the given MCRObjectID
     * and returned it as MCRDirectory.
     * 
     * @param id
     *            the object ID
     * @return the MCRDirectory of the multimedia object
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     * @deprecated use {@link MCRDerivate#receiveDirectoryFromIFS()} instead
     */
    public final MCRDirectory receiveDirectoryFromIFS(String id) throws MCRPersistenceException {
        // check the ID
        mcr_id = MCRObjectID.getInstance(id);

        // receive the IFS informations
        MCRDirectory difs = MCRDirectory.getRootDirectory(mcr_id.toString());

        if (difs == null) {
            throw new MCRPersistenceException("Error while receiving derivate with " + "ID " + mcr_id + " from IFS.");
        }

        return difs;
    }

    /**
     * The methode receive the multimedia object(s) for the given MCRObjectID
     * and returned it as MCRDirectory.
     * 
     * @return the MCRDirectory of the multimedia object
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final MCRDirectory receiveDirectoryFromIFS() throws MCRPersistenceException {
        // receive the IFS informations
        MCRDirectory difs = MCRDirectory.getRootDirectory(getId().toString());

        if (difs == null) {
            throw new MCRPersistenceException("Error while receiving derivate with " + "ID " + getId() + " from IFS.");
        }
        return difs;
    }

    /**
     * The method print all informations about this MCRObject.
     */
    public final void debug() {
        LOGGER.debug("MCRDerivate ID : " + mcr_id);
        LOGGER.debug("MCRDerivate Label : " + mcr_label);
        LOGGER.debug("MCRDerivate Schema : " + mcr_schema);
        LOGGER.debug("");
    }

    @Override
    public boolean isValid() {
        if (!super.isValid()) {
            LOGGER.warn("MCRBase is invalid");
            return false;
        }
        if (!getDerivate().isValid()) {
            LOGGER.warn("MCRObjectDerivate is invalid");
            return false;
        }
        return true;
    }

    /**
     * @return the {@link MCRObjectID} of the owner of the derivate
     */
    public MCRObjectID getOwnerID() {
        return this.getDerivate().getMetaLink().getXLinkHrefID();
    }

    @Override
    public void setId(MCRObjectID id) {
        super.setId(id);
        this.mcr_derivate.setDerivateID(id);
    }
}
