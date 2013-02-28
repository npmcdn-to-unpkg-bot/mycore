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

package org.mycore.common.content.transformer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TooManyListenersException;

import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.apache.xalan.trace.TraceManager;
import org.apache.xalan.transformer.TransformerImpl;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.streams.MCRByteArrayOutputStream;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xsl.MCRErrorListener;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.common.xsl.MCRTemplatesSource;
import org.mycore.common.xsl.MCRTraceListener;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Transforms XML content using a static XSL stylesheet.
 * The stylesheet is configured via
 * 
 * MCR.ContentTransformer.{ID}.Stylesheet

 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXSLTransformer extends MCRParameterizedTransformer {

    private static final int INITIAL_BUFFER_SIZE = 32 * 1024;

    private static final MCRURIResolver URI_RESOLVER = MCRURIResolver.instance();

    private static Logger LOGGER = Logger.getLogger(MCRXSLTransformer.class);

    private static MCRTraceListener TRACE_LISTENER = new MCRTraceListener();

    private static boolean TRACE_LISTENER_ENABLED = Logger.getLogger(MCRTraceListener.class).isDebugEnabled();

    private static MCRCache<String, MCRXSLTransformer> INSTANCE_CACHE = new MCRCache<String, MCRXSLTransformer>(100,
        "MCRXSLTransformer instance cache");

    private static long CHECK_PERIOD = MCRConfiguration.instance().getLong("MCR.LayoutService.LastModifiedCheckPeriod", 10000);

    /** The compiled XSL stylesheet */
    protected MCRTemplatesSource[] templateSources;

    protected Templates[] templates;

    protected long[] modified;

    protected long[] modifiedChecked;

    protected SAXTransformerFactory tFactory;

    public MCRXSLTransformer(String... stylesheets) {
        this();
        setStylesheets(stylesheets);
    }

    public MCRXSLTransformer() {
        super();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        LOGGER.info("Transformerfactory: " + transformerFactory.getClass().getName());
        transformerFactory.setURIResolver(URI_RESOLVER);
        transformerFactory.setErrorListener(MCRErrorListener.getInstance());
        if (transformerFactory.getFeature(SAXSource.FEATURE) && transformerFactory.getFeature(SAXResult.FEATURE)) {
            this.tFactory = (SAXTransformerFactory) transformerFactory;
        } else {
            throw new MCRConfigurationException("Transformer Factory " + transformerFactory.getClass().getName()
                + " does not implement SAXTransformerFactory");
        }
    }

    public static MCRXSLTransformer getInstance(String... stylesheets) {
        String key = stylesheets.length == 1 ? stylesheets[0] : Arrays.toString(stylesheets);
        MCRXSLTransformer instance = INSTANCE_CACHE.get(key);
        if (instance == null) {
            instance = new MCRXSLTransformer(stylesheets);
            INSTANCE_CACHE.put(key, instance);
        }
        return instance;
    }

    @Override
    public void init(String id) {
        super.init(id);
        String property = "MCR.ContentTransformer." + id + ".Stylesheet";
        String[] stylesheets = MCRConfiguration.instance().getString(property).split(",");
        setStylesheets(stylesheets);
    }

    public void setStylesheets(String... stylesheets) {
        this.templateSources = new MCRTemplatesSource[stylesheets.length];
        for (int i = 0; i < stylesheets.length; i++) {
            this.templateSources[i] = new MCRTemplatesSource(stylesheets[i].trim());
        }
        this.modified = new long[templateSources.length];
        this.modifiedChecked = new long[templateSources.length];
        this.templates = new Templates[templateSources.length];
    }

    private void checkTemplateUptodate() throws TransformerConfigurationException, SAXException {
        for (int i = 0; i < templateSources.length; i++) {
            long lastModifiedChecked = modifiedChecked[i];
            boolean check = System.currentTimeMillis() - lastModifiedChecked > CHECK_PERIOD;
            long lastModified = modified[i];
            if (check) {
                lastModified = templateSources[i].getLastModified();
                modifiedChecked[i] = System.currentTimeMillis();
            }
            if (templates[i] == null || modified[i] < lastModified) {
                SAXSource source = templateSources[i].getSource();
                templates[i] = tFactory.newTemplates(source);
                if (templates[i] == null) {
                    throw new TransformerConfigurationException("XSLT Stylesheet could not be compiled: " + templateSources[i].getURL());
                }
                modified[i] = lastModified;
            }
        }
    }

    @Override
    public String getEncoding() {
        return getOutputProperty("encoding", "UTF-8");
    }

    @Override
    public String getMimeType() {
        return getOutputProperty("media-type", "text/xml");
    }

    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        return transform(source, new MCRParameterCollector());
    }

    @Override
    public MCRContent transform(MCRContent source, MCRParameterCollector parameter) throws IOException {
        try {
            LinkedList<TransformerHandler> transformHandlerList = getTransformHandlerList(parameter);
            XMLReader reader = getXMLReader(transformHandlerList);
            TransformerHandler lastTransformerHandler = transformHandlerList.getLast();
            return transform(source, reader, lastTransformerHandler);
        } catch (TransformerConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void transform(MCRContent source, OutputStream out) throws IOException {
        transform(source, out, new MCRParameterCollector());
    }

    @Override
    public void transform(MCRContent source, OutputStream out, MCRParameterCollector parameter) throws IOException {
        try {
            LinkedList<TransformerHandler> transformHandlerList = getTransformHandlerList(parameter);
            XMLReader reader = getXMLReader(transformHandlerList);
            TransformerHandler lastTransformerHandler = transformHandlerList.getLast();
            StreamResult result = new StreamResult(out);
            lastTransformerHandler.setResult(result);
            reader.parse(source.getInputSource());
        } catch (TransformerConfigurationException e) {
            throw new IOException(e);
        } catch (IllegalArgumentException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    protected MCRContent transform(MCRContent source, XMLReader reader, TransformerHandler transformerHandler) throws IOException,
        SAXException {
        MCRByteArrayOutputStream baos = new MCRByteArrayOutputStream(INITIAL_BUFFER_SIZE);
        StreamResult serializer = new StreamResult(baos);
        transformerHandler.setResult(serializer);
        // Parse the source XML, and send the parse events to the
        // TransformerHandler.
        reader.parse(source.getInputSource());
        return new MCRByteContent(baos.getBuffer(), 0, baos.size());
    }

    private LinkedList<TransformerHandler> getTransformHandlerList(MCRParameterCollector parameterCollector)
        throws TransformerConfigurationException, SAXException {
        checkTemplateUptodate();
        LinkedList<TransformerHandler> xslSteps = new LinkedList<TransformerHandler>();
        for (Templates template : templates) {
            TransformerHandler handler = tFactory.newTransformerHandler(template);
            parameterCollector.setParametersTo(handler.getTransformer());
            handler.getTransformer().setErrorListener(MCRErrorListener.getInstance());
            if (TRACE_LISTENER_ENABLED) {
                TransformerImpl transformer = (TransformerImpl) handler.getTransformer();
                TraceManager traceManager = transformer.getTraceManager();
                try {
                    traceManager.addTraceListener(TRACE_LISTENER);
                } catch (TooManyListenersException e) {
                    LOGGER.warn("Could not add MCRTraceListener.", e);
                }
            }
            if (!xslSteps.isEmpty()) {
                Result result = new SAXResult(handler);
                xslSteps.getLast().setResult(result);
            }
            xslSteps.add(handler);
        }
        return xslSteps;
    }

    /**
     * @param transformHandlerList
     * @return
     * @throws SAXException
     */
    private XMLReader getXMLReader(LinkedList<TransformerHandler> transformHandlerList) throws SAXException {
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setEntityResolver(URI_RESOLVER);
        reader.setContentHandler(transformHandlerList.getFirst());
        return reader;
    }

    private String getOutputProperty(String propertyName, String defaultValue) {
        try {
            checkTemplateUptodate();
            Templates lastTemplate = templates[templates.length - 1];
            Properties outputProperties = lastTemplate.getOutputProperties();
            if (outputProperties == null) {
                return defaultValue;
            }
            String value = outputProperties.getProperty(propertyName);
            if (value == null) {
                return defaultValue;
            }
            return value;
        } catch (Exception e) {
            throw new MCRException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.common.content.transformer.MCRContentTransformer#getFileExtension()
     */
    @Override
    public String getFileExtension() {
        String fileExtension = super.getFileExtension();
        if (fileExtension != null && !getDefaultExtension().equals(fileExtension)) {
            return fileExtension;
        }
        //until we have a better solution
        if ("text/html".equals(getMimeType())) {
            return "html";
        }
        if ("text/xml".equals(getMimeType())) {
            return "xml";
        }
        return getDefaultExtension();
    }

}
