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

package org.mycore.frontend.xeditor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xalan.extensions.ExpressionContext;
import org.apache.xpath.NodeSet;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XNodeSetForDOM;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRXSL2XMLTransformer;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.frontend.xeditor.MCRXPathParser.MCRLocationStep;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXEditorTransformer {

    private final static Logger LOGGER = Logger.getLogger(MCRXEditorTransformer.class);

    private MCREditorSession editorSession;

    private MCRParameterCollector transformationParameters;

    private MCRBinding currentBinding;

    private Stack<MCRRepeat> repeats = new Stack<MCRRepeat>();

    public MCRXEditorTransformer(MCREditorSession editorSession, MCRParameterCollector transformationParameters) {
        this.editorSession = editorSession;
        this.transformationParameters = transformationParameters;
    }

    public MCRContent transform(MCRContent editorSource) throws IOException, JDOMException, SAXException {
        MCRUsedNamespaces.addNamespacesFrom(editorSource.asXML().getRootElement());
        MCRXSL2XMLTransformer transformer = MCRXSL2XMLTransformer.getInstance("xsl/xeditor.xsl");
        String key = MCRXEditorTransformerStore.storeTransformer(this);
        transformationParameters.setParameter("XEditorTransformerKey", key);
        return transformer.transform(editorSource, transformationParameters);
    }

    public static MCRXEditorTransformer getTransformer(String key) {
        return MCRXEditorTransformerStore.getAndRemoveTransformer(key);
    }

    public String getEditorSessionID() {
        return editorSession.getID();
    }

    public void readSourceXML(String uri) throws JDOMException, IOException, SAXException, TransformerException {
        uri = replaceParameters(uri);
        if (!uri.contains("{"))
            editorSession.setEditedXML(uri);
    }

    public void setCancelURL(String url) throws JDOMException, IOException, SAXException, TransformerException {
        url = replaceParameters(url);
        if (!url.contains("{"))
            editorSession.setCancelURL(url);
    }

    public void bind(String xPath, String name) throws JDOMException, ParseException {
        if (editorSession.getEditedXML() == null) {
            String rPath = xPath.startsWith("/") ? xPath.substring(1) : xPath;

            MCRLocationStep root = MCRXPathParser.parse(rPath).getLocationSteps().get(0);
            editorSession.setEditedXML(new Document(new Element(root.getLocalName(), root.getNamespace())));
        }
        if (currentBinding == null) {
            currentBinding = new MCRBinding(editorSession.getEditedXML());
        }
        currentBinding = new MCRBinding(xPath, name, currentBinding);
    }

    public void unbind() {
        currentBinding = currentBinding.getParent();
    }

    public String getAbsoluteXPath() {
        return currentBinding.getAbsoluteXPath();
    }

    public String getValue() {
        markAsUsed();
        return currentBinding.getValue();
    }

    public boolean hasValue(String value) {
        markAsUsed();
        return currentBinding.hasValue(value);
    }

    private void markAsUsed() {
        for (Object node : currentBinding.getBoundNodes()) {
            editorSession.markAsTransformedToInputField(node);
        }
    }

    public String repeat(String xPath, int minRepeats, int maxRepeats) throws JDOMException, ParseException {
        MCRRepeat repeat = new MCRRepeat(currentBinding, xPath, minRepeats, maxRepeats);
        repeats.push(repeat);
        return StringUtils.repeat("a ", repeat.getNumRepeats());
    }

    public int getNumRepeats() {
        return repeats.peek().getNumRepeats();
    }

    public int getMaxRepeats() {
        return repeats.peek().getMaxRepeats();
    }

    public int getRepeatPosition() {
        return repeats.peek().getRepeatPosition();
    }

    public void bindRepeatPosition() throws JDOMException, ParseException {
        currentBinding = repeats.peek().bindRepeatPosition();
    }

    public void endRepeat() {
        currentBinding = repeats.pop().getParentBinding();
    }

    public String getControlsParameter() throws UnsupportedEncodingException {
        return repeats.peek().getControlsParameter();
    }

    private final static Pattern PATTERN_URI = Pattern.compile("\\{\\{\\$(.+)\\}\\}");

    public String replaceParameters(String uri) {
        Matcher m = PATTERN_URI.matcher(uri);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String token = m.group(1);
            String value = transformationParameters.getParameter(token, null);
            m.appendReplacement(sb, value == null ? m.group().replace("$", "\\$") : value);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private final static Pattern PATTERN_XPATH = Pattern.compile("\\{\\{([^\\}]+)\\}\\}");

    public String replaceXPaths(String text) {
        Matcher m = PATTERN_XPATH.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String xPath = m.group(1);
            m.appendReplacement(sb, evaluateXPath(xPath));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public String evaluateXPath(String xPathExpression) {
        try {
            Map<String, Object> xPathVariables = currentBinding.buildXPathVariables();
            xPathVariables.putAll(transformationParameters.getParameterMap());
            XPathFactory factory = XPathFactory.instance();
            List<Namespace> namespaces = MCRUsedNamespaces.getNamespaces();
            XPathExpression<Object> xPath = factory.compile(xPathExpression, Filters.fpassthrough(), xPathVariables, namespaces);
            return xPath.evaluateFirst(currentBinding.getBoundNodes()).toString();
        } catch (Exception ex) {
            LOGGER.warn("unable to evaluate XPath: " + xPathExpression);
            LOGGER.debug(ex);
            return "";
        }
    }

    public XNodeSet getRequestParameters(ExpressionContext context) throws ParserConfigurationException, TransformerException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document doc = builder.newDocument();
        NodeSet ns = new NodeSet();
        Map<String, String[]> parameters = editorSession.getRequestParameters();
        for (String name : parameters.keySet()) {
            for (String value : parameters.get(name)) {
                if ((value != null) && !value.isEmpty()) {
                    org.w3c.dom.Element element = doc.createElement("param");
                    element.setAttribute("name", name);
                    element.setTextContent(value);
                    ns.addNode(element);
                }
            }
        }
        return new XNodeSetForDOM((NodeList) ns, context.getXPathContext());
    }
}
