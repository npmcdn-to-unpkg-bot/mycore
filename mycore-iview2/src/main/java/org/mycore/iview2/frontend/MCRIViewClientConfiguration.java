package org.mycore.iview2.frontend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.common.content.MCRXMLContent;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.iview2.services.MCRIView2Tools;

import com.google.gson.Gson;

@XmlRootElement(name = "iviewClientConfiguration")
abstract class MCRIViewClientConfiguration {

    @XmlElement
    public String webApplicationBaseURL;

    /**
     * The name of the derivate which should be displayed.
     */
    @XmlElement()
    public String derivate;

    /**
     * Needed by the metadata plugin.
     */
    @XmlElement
    public String objId;

    /**
     * Needed by the metadata plugin.
     */
    @XmlElement
    public String metadataUrl;

    /**
    * Should the mobile or the desktop client started.
    */
    @XmlElement()
    public Boolean mobile;

    /**
     *  The type of the structure(mets /pdf).
     */
    @XmlElement()
    public String doctype;

    /**
     * The location of the Structure (mets.xml / document.pdf).
     */
    @XmlElement()
    public String location;

    /**
     * The location where the iview-client can load the i18n.json.
     */
    @XmlElement()
    public String i18nPath;

    /**
     * [default = en]
     * The language 
     */
    @XmlElement()
    public String lang;

    /**
     * [optional] 
     */
    @XmlElement()
    public String pdfCreatorURI;

    /**
     * [optional]
     */
    @XmlElement()
    public String pdfCreatorStyle;

    @XmlElements({ @XmlElement(name = "resource", type = MCRIViewClientResource.class) })
    @XmlElementWrapper()
    public List<MCRIViewClientResource> resources;

    @XmlElements({ @XmlElement(name = "property") })
    @XmlElementWrapper(name = "properties")
    public List<MCRIViewClientProperty> getProperties() {
        List<MCRIViewClientProperty> propertyList = new ArrayList<>();
        for (Entry<String, String> entry : properties.entrySet()) {
            propertyList.add(new MCRIViewClientProperty(entry.getKey(), entry.getValue()));
        }
        return propertyList;
    }

    private Map<String, String> properties;

    /**
     * Setup the configuration object.
     * 
     * @param request 
     */
    public void setup(HttpServletRequest request) {
        this.webApplicationBaseURL = MCRServlet.getBaseURL();
        this.i18nPath = MCRServlet.getServletBaseURL() + "MCRLocaleServlet/{lang}/component.iview2.*";
        this.lang = "de";
        this.pdfCreatorStyle = MCRIView2Tools.getIView2Property("PDFCreatorStyle");
        this.pdfCreatorURI = MCRIView2Tools.getIView2Property("PDFCreatorURI");
        this.metadataUrl = MCRIView2Tools.getIView2Property("MetadataUrl");
        this.derivate = request.getParameter("derivate");
        this.location = MCRServlet.getServletBaseURL() + "MCRMETSServlet/" + this.derivate;
        this.mobile = isMobile(request);
        this.resources = new ArrayList<>();
        this.properties = new HashMap<>();
        this.addResources();
    }

    protected void addResources() {
        List<String> scripts = MCRConfiguration.instance().getStrings(MCRIView2Tools.CONFIG_PREFIX + "resource.script",
            new ArrayList<String>());
        for (String script : scripts) {
            addScript(script);
        }
        List<String> stylesheets = MCRConfiguration.instance().getStrings(
            MCRIView2Tools.CONFIG_PREFIX + "resource.css", new ArrayList<String>());
        for (String css : stylesheets) {
            addCSS(css);
        }
    }

    /**
     * Adds a new javascript file which should be included by the image viewer.
     * 
     * @param url 
     */
    public void addScript(final String url) {
        this.resources.add(new MCRIViewClientResource(MCRIViewClientResource.Type.script, url));
    }

    /**
     * Adds a new css file which should be included by the image viewer.
     * 
     * @param url
     */
    public void addCSS(final String url) {
        this.resources.add(new MCRIViewClientResource(MCRIViewClientResource.Type.css, url));
    }

    /**
     * Sets a new property.
     * 
     * @param name name of the property
     * @param value value of the property
     */
    public void setProperty(String name, String value) {
        this.properties.put(name, value);
    }

    /**
     * @return json
     */
    public String toJSON() {
        final Gson gson = new Gson();
        return gson.toJson(this);
    }

    public MCRXMLContent toXMLContent() throws JAXBException {
        MCRJAXBContent<MCRIViewClientConfiguration> config = new MCRJAXBContent<MCRIViewClientConfiguration>(
            JAXBContext.newInstance(this.getClass()), this);
        return config;
    }

    public static boolean isMobile(HttpServletRequest req) {
        String mobileParameter = req.getParameter("mobile");
        if (mobileParameter != null) {
            return mobileParameter.toLowerCase().equals(Boolean.TRUE.toString());
        } else {
            return req.getHeader("User-Agent").indexOf("Mobile") != -1;
        }
    }

    @XmlRootElement(name = "resource")
    public static class MCRIViewClientResource {

        public static enum Type {
            script, css
        }

        @XmlAttribute(name = "type", required = true)
        public Type type;

        @XmlValue
        public String url;

        public MCRIViewClientResource() {
        }

        public MCRIViewClientResource(Type type, String url) {
            this.type = type;
            this.url = url;
        }

    }

    @XmlRootElement(name = "property")
    public static class MCRIViewClientProperty {

        @XmlAttribute(name = "name", required = true)
        public String name;

        @XmlValue
        public String value;

        public MCRIViewClientProperty() {
        }

        public MCRIViewClientProperty(String name, String value) {
            this.name = name;
            this.value = value;
        }

    }

}
