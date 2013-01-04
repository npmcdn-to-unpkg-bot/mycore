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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs.FileObject;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.ifs2.MCRContentInputStream;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * Used to read/write content from any source to any target. Sources and targets
 * can be strings, local files, Apache VFS file objects, XML documents, byte[]
 * arrays and streams. The different sources are implemented by subclasses.
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 */
public abstract class MCRContent {

    /**
     * Holds the systemID of the current content
     */
    protected String systemId;

    /**
     * Holds the docType of the current content
     */
    protected String docType;

    /**
     * Sets the systemID of the current content
     */
    void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Returns the systemID of the current content
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Returns content as input stream. Be sure to close this stream properly!
     * 
     * @return input stream to read content from
     * @throws IOException 
     */
    public abstract InputStream getInputStream() throws IOException;

    /**
     * Returns content as content input stream, which provides MD5
     * functionality. Be sure to close this stream properly!
     * 
     * @return the content input stream
     * @throws IOException 
     */
    public MCRContentInputStream getContentInputStream() throws IOException {
        return new MCRContentInputStream(getInputStream());
    }

    /**
     * Return the content as Source
     * @return content as Source
     */
    public Source getSource() throws IOException {
        return new StreamSource(getInputStream(), systemId);
    }

    /**
     * Sends content to the given OutputStream. 
     * The OutputStream is NOT automatically closed afterwards.
     * 
     * @param out
     *            the OutputStream to write the content to
     */
    public void sendTo(OutputStream out) throws IOException {
        InputStream in = getInputStream();
        try {
            IOUtils.copy(in, out);
        } finally {
            in.close();
        }
    }

    /**
     * Sends content to the given OutputStream. 
     * 
     * @param out
     *            the OutputStream to write the content to
     * @param close 
     *             if true, close OutputStream afterwards
     */
    public void sendTo(OutputStream out, boolean close) throws IOException {
        sendTo(out);
        if (close) {
            out.close();
        }
    }

    /**
     * Returns content as SAX input source.
     * 
     * @return input source to read content from
     * @throws IOException 
     */
    public InputSource getInputSource() throws IOException {
        InputSource source = new InputSource(getInputStream());
        source.setSystemId(systemId);
        return source;
    }

    /**
     * Sends content to the given local file
     * 
     * @param target
     *            the file to write the content to
     */
    public void sendTo(File target) throws IOException {
        sendTo(new FileOutputStream(target), true);
    }

    /**
     * Sends the content to the given Apache VFS file object
     * 
     * @param target
     *            the file to write the content to
     */
    public void sendTo(FileObject target) throws IOException {
        sendTo(target.getContent().getOutputStream(), true);
    }

    /**
     * Returns the raw content
     * 
     * @return the content
     */
    public byte[] asByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        sendTo(baos);
        baos.close();
        return baos.toByteArray();
    }

    /**
     * Returns content as String, assuming UTF-8 encoding
     * 
     * @return content as String
     */
    public String asString() throws IOException, UnsupportedEncodingException {
        return asString("UTF-8");
    }

    /**
     * Returns the content as String, assuming the provided encoding
     * 
     * @param encoding
     *            the encoding to use to build the characters
     * @return content as String
     */
    public String asString(String encoding) throws IOException, UnsupportedEncodingException {
        return new String(asByteArray(), encoding);
    }

    /**
     * Parses content, assuming it is XML, and returns the parsed document.
     * 
     * @return the XML document parsed from content
     * @throws SAXParseException 
     * @throws MCRException 
     */
    public Document asXML() throws JDOMException, IOException, SAXParseException {
        return MCRXMLParserFactory.getNonValidatingParser().parseXML(this);
    }

    /**
     * Ensures that content is XML. The content is parsed as if asXML() is called.
     * When content is XML, an MCRContent instance is returned that guarantees that.
     * When XML can not be parsed, an exception is thrown.
     * 
     * @throws SAXParseException 
     */
    public MCRContent ensureXML() throws IOException, JDOMException, SAXParseException {
        return new MCRJDOMContent(asXML());
    }

    /**
     * Return the document type of the content, assuming content is XML
     *
     * @return document type as String
     */
    public String getDocType() throws IOException {
        if (docType != null) {
            return docType;
        }
        if (!isReusable()) {
            throw new IOException("Cannot determine DOCTYPE as it would destroy underlaying InputStream.");
        }
        MCRContentInputStream cin = getContentInputStream();
        try {
            byte[] header = cin.getHeader();
            return MCRUtils.parseDocumentType(new ByteArrayInputStream(header));
        } finally {
            cin.close();
        }
    }

    /**
     * If true, content can be read more than once by calling getInputStream() and
     * similar methods. If false, content may be consumed when it is read more than once.
     * Most subclasses provide reusable content.  
     */
    public boolean isReusable() {
        return true;
    }

    /**
     * Returns a reusable copy of this content, that is an instance (may be the same instance)
     * thats content can be read more than once without consuming the stream.
     */
    public MCRContent getReusableCopy() throws IOException {
        if (isReusable()) {
            return this;
        } else {
            MCRContent copy = new MCRByteContent(asByteArray());
            copy.setSystemId(systemId);
            return copy;
        }
    }
}
