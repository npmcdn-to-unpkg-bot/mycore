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

package org.mycore.common.xml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;

import org.apache.xerces.parsers.SAXParser;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * Implements the MCRParserInterface using the Xerces XML to parse XML streams
 * to a DOM document.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRParserXerces implements MCRParserInterface {

    private final static String PARSER_CLASS_NAME = SAXParser.class.getCanonicalName();

    /** A xerces parser instance that will validate */
    SAXBuilder builderValid;

    /** A xerces parser instance that will not validate */
    SAXBuilder builder;

    /** By default, validate xml or not? */
    private static boolean FLAG_VALIDATION = false;

    private static String FEATURE_NAMESPACES = "http://xml.org/sax/features/namespaces";

    private static String FEATURE_SCHEMA_SUPPORT = "http://apache.org/xml/features/validation/schema";

    private static String FEATURE_FULL_SCHEMA_SUPPORT = "http://apache.org/xml/features/validation/schema-full-checking";

    /**
     * Constructor for the Xerces parser. Sets default validation flag as
     * specified by the property MCR.XMLParser.ValidateSchema in
     * mycore.properties
     */
    public MCRParserXerces() {
        FLAG_VALIDATION = MCRConfiguration.instance().getBoolean("MCR.XMLParser.ValidateSchema", FLAG_VALIDATION);
        builderValid = new SAXBuilder(PARSER_CLASS_NAME, true);
        builderValid.setFeature(FEATURE_NAMESPACES, true);
        builderValid.setFeature(FEATURE_SCHEMA_SUPPORT, true);
        builderValid.setFeature(FEATURE_FULL_SCHEMA_SUPPORT, false);
        builderValid.setReuseParser(false);
        builderValid.setErrorHandler(new MCRXMLParserErrorHandler());
        builderValid.setEntityResolver(MCRURIResolver.instance());
        builder = new SAXBuilder(PARSER_CLASS_NAME, false);
        builder.setFeature(FEATURE_NAMESPACES, true);
        builder.setFeature(FEATURE_SCHEMA_SUPPORT, false);
        builder.setFeature(FEATURE_FULL_SCHEMA_SUPPORT, false);
        builder.setReuseParser(false);
        builder.setErrorHandler(new MCRXMLParserErrorHandler());
        builder.setEntityResolver(MCRURIResolver.instance());
    }

    /**
     * Parses the XML byte stream with xerces parser and returns a DOM document.
     * Uses the validation flag from mycore.properties.
     * 
     * @param uri
     *            the URI of the XML input stream
     * @throws MCRException
     *             if XML could not be parsed
     * @return the parsed XML stream as a DOM document
     * @throws SAXParseException 
     */
    public Document parseURI(URI uri) throws SAXParseException {
        return parseURI(uri, FLAG_VALIDATION);
    }

    /**
     * Parses the XML byte stream with xerces parser and returns a DOM document.
     * Uses the validation flag given.
     * 
     * @param uri
     *            the URI of the XML input stream
     * @param validate
     *            if true, will validate against XML Schema
     * @throws MCRException
     *             if XML could not be parsed
     * @return the parsed XML stream as a DOM document
     * @throws SAXParseException 
     */
    public Document parseURI(URI uri, boolean validate) throws SAXParseException {
        InputSource inputSource = null;
        try {
            //use uri as a SystemID
            inputSource = new InputSource(uri.toString());
        } catch (Exception e) {
            throw new MCRException(msg + uri, e);
        }
        if (inputSource == null) {
            throw new MCRException("Could not get " + uri);
        }
        return parse(inputSource, validate);
    }

    /**
     * Parses the XML byte stream with xerces parser and returns a DOM document.
     * Uses the validation flag from mycore.properties
     * 
     * @param xml
     *            the XML byte stream
     * @throws MCRException
     *             if XML could not be parsed
     * @return the parsed XML stream as a DOM document
     * @throws SAXParseException 
     */
    public Document parseXML(String xml) throws SAXParseException {
        return parseXML(xml, FLAG_VALIDATION);
    }

    /**
     * Parses the XML byte stream with xerces parser and returns a DOM document.
     * Uses the validation flag given.
     * 
     * @param xml
     *            the XML byte stream
     * @param validate
     *            if true, will validate against XML Schema
     * @throws MCRException
     *             if XML could not be parsed
     * @return the parsed XML stream as a DOM document
     * @throws SAXParseException 
     */
    public Document parseXML(String xml, boolean validate) throws SAXParseException {
        InputSource source = new InputSource(new StringReader(xml));

        return parse(source, validate);
    }

    /**
     * Parses the XML byte stream with xerces parser and returns a DOM document.
     * Uses the validation flag from mycore.properties
     * 
     * @param xml
     *            the XML byte stream
     * @throws MCRException
     *             if XML could not be parsed
     * @return the parsed XML stream as a DOM document
     * @throws SAXParseException 
     */
    public Document parseXML(byte[] xml) throws SAXParseException {
        return parseXML(xml, FLAG_VALIDATION);
    }

    /**
     * Parses the XML byte stream with xerces parser and returns a DOM document.
     * Uses the given validation flag.
     * 
     * @param xml
     *            the XML byte stream
     * @param validate
     *            if true, will validate against XML Schema
     * @throws MCRException
     *             if XML could not be parsed
     * @return the parsed XML stream as a DOM document
     * @throws SAXParseException 
     */
    public Document parseXML(byte[] xml, boolean validate) throws SAXParseException {
        InputSource source = new InputSource(new ByteArrayInputStream(xml));
        return parse(source, validate);
    }

    public Document parseXML(InputStream input) throws MCRException, SAXParseException {
        return parseXML(input, FLAG_VALIDATION);
    }

    public Document parseXML(InputStream input, boolean validate) throws MCRException, SAXParseException {
        InputSource source = new InputSource(input);
        return parse(source, validate);
    }

    /**
     * Parses the InputSource with xerces parser and returns a DOM document.
     * Uses the given validation flag.
     * 
     * @param source
     *            the XML InputSource
     * @param validate
     *            if true, will validate against XML Schema
     * @throws MCRException
     *             if XML could not be parsed
     * @return the parsed XML stream as a DOM document
     * @throws SAXParseException 
     */
    private Document parse(InputSource source, boolean validate) throws SAXParseException {
        SAXBuilder builder = validate ? builderValid : this.builder;

        try {
            return builder.build(source);
        } catch (Exception ex) {
            if (ex instanceof SAXParseException) {
                throw (SAXParseException) ex;
            }
            Throwable cause = ex.getCause();
            if (cause instanceof SAXParseException) {
                throw (SAXParseException) cause;
            }
            throw new MCRException(msg, ex);
        }
    }

    private final static String msg = "Error while parsing XML document: ";
}
