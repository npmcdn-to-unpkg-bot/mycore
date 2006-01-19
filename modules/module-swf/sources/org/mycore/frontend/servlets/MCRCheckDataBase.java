/*
 * $RCSfile$
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

package org.mycore.frontend.servlets;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.RequestDispatcher;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRMetaAddress;
import org.mycore.datamodel.metadata.MCRMetaBoolean;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaDate;
import org.mycore.datamodel.metadata.MCRMetaHistoryDate;
import org.mycore.datamodel.metadata.MCRMetaISO8601Date;
import org.mycore.datamodel.metadata.MCRMetaInstitutionName;
import org.mycore.datamodel.metadata.MCRMetaLangText;
import org.mycore.datamodel.metadata.MCRMetaLink;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetaNumber;
import org.mycore.datamodel.metadata.MCRMetaPersonName;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;
import org.mycore.user.MCRUserMgr;

/**
 * This class is the superclass of servlets which checks the MCREditorServlet
 * output XML and store the XML in a file or if an error was occured start the
 * editor again.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
abstract public class MCRCheckDataBase extends MCRCheckBase {
    /**
     * This method overrides doGetPost of MCRServlet. <br />
     */
    private static Logger LOGGER=Logger.getLogger(MCRCheckDataBase.class);
    public void doGetPost(MCRServletJob job) throws Exception {
        // read the XML data
        MCREditorSubmission sub = (MCREditorSubmission) (job.getRequest().getAttribute("MCREditorSubmission"));
        org.jdom.Document indoc = sub.getXML();

        // read the parameter
        MCRRequestParameters parms;

        if (sub == null) {
            parms = new MCRRequestParameters(job.getRequest());
        } else {
            parms = sub.getParameters();
        }

        String oldmcrid = parms.getParameter("mcrid");
        String oldtype = parms.getParameter("type");
        String oldstep = parms.getParameter("step");
        LOGGER.debug("XSL.target.param.0 = " + oldmcrid);
        LOGGER.debug("XSL.target.param.1 = " + oldtype);
        LOGGER.debug("XSL.target.param.2 = " + oldstep);

        // get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String userid = mcrSession.getCurrentUserID();

        // userid = "administrator";
        LOGGER.debug("Current user for edit check = " + userid);

        String usererrorpage = CONFIG.getString("MCR.editor_page_dir", "") + CONFIG.getString("MCR.editor_page_error_user", "editor_error_user.xml");
        ArrayList privs = MCRUserMgr.instance().retrieveAllPrivsOfTheUser(userid);

        if (!hasPrivileg(privs, oldtype)) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));

            return;
        }

        String lang = mcrSession.getCurrentLanguage();
        LOGGER.info("LANG = " + lang);

        // prepare the MCRObjectID's for the Metadata
        String mmcrid = "";
        boolean hasid = false;

        try {
            mmcrid = indoc.getRootElement().getAttributeValue("ID");

            if (mmcrid == null) {
                mmcrid = oldmcrid;
            } else {
                hasid = true;
            }
        } catch (Exception e) {
            mmcrid = oldmcrid;
        }

        MCRObjectID ID = new MCRObjectID(mmcrid);

        if (!ID.getTypeId().equals(oldtype)) {
            ID = new MCRObjectID(oldmcrid);
            hasid = false;
        }

        if (!hasid) {
            indoc.getRootElement().setAttribute("ID", ID.getId());
        }

        // Save the incoming to a file
        byte[] outxml = MCRUtils.getByteArray(indoc);
        String savedir = CONFIG.getString("MCR.editor_" + ID.getTypeId() + "_directory");
        String NL = System.getProperty("file.separator");
        String fullname = savedir + NL + ID.getId() + ".xml";
        storeMetadata(outxml, job, ID, fullname, lang);

        // create a metadata object and prepare it
        org.jdom.Document outdoc = prepareMetadata((org.jdom.Document) indoc.clone(), ID, job, oldstep, lang);
        outxml = MCRUtils.getByteArray(outdoc);

        // Save the prepared metadata object
        storeMetadata(outxml, job, ID, fullname, lang);

        // call the getNextURL and sendMail methods
        String url;
        try {
            url = getNextURL(ID);
        } catch (MCRActiveLinkException e) {
            generateActiveLinkErrorpage(job.getRequest(), job.getResponse(), "Error in the workflow component.", e);
            return;
        }
        sendMail(ID);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + url));
    }

    /**
     * The method stores the data in a working directory dependenced of the
     * type.
     * 
     * @param outdoc
     *            the prepared JDOM object
     * @param job
     *            the MCRServletJob
     * @param the
     *            MCRObjectID of the MCRObject/MCRDerivate
     * @param fullname
     *            the file name where the JDOM was stored.
     * @param lang
     *            the current langauge
     */
    public final void storeMetadata(byte[] outxml, MCRServletJob job, MCRObjectID ID, String fullname, String lang) throws Exception {
        if (outxml == null) {
            return;
        }

        // Save the prepared MCRObject/MCRDerivate to a file
        try {
            FileOutputStream out = new FileOutputStream(fullname);
            out.write(outxml);
            out.flush();
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("Exception while store to file " + fullname);
            errorHandlerIO(job, lang);

            return;
        }

        LOGGER.info("Object " + ID.getId() + " stored under " + fullname + ".");
    }

    /**
     * The method read the incoming JDOM tree in a MCRObject and prepare this by
     * the following rules. After them it return a JDOM as result of
     * MCRObject.createXML(). <br/>
     * <li>remove all target of MCRMetaClassification they have not a categid
     * attribute.</li>
     * <br/>
     * <li>remove all target of MCRMetaLangText they have an empty text</li>
     * <br/>
     * 
     * @param jdom_in
     *            the JDOM tree from the editor
     * @param ID
     *            the MCRObjectID of the MCRObject
     * @param job
     *            the MCRServletJob data
     * @param step
     *            the current workflow step
     * @param lang
     *            the current language
     */
    protected org.jdom.Document prepareMetadata(org.jdom.Document jdom_in, MCRObjectID ID, MCRServletJob job, String step, String lang) throws Exception {
        EditorValidator ev = new EditorValidator(jdom_in, ID);
        Document jdom_out = ev.generateValidMyCoReObject();
        errorHandlerValid(job, ev.getErrorLog(), ID, step, lang);
        return jdom_out;
    }

    /**
     * A method to handle valid errors.
     */
    private final void errorHandlerValid(MCRServletJob job, List logtext, MCRObjectID ID, String step, String lang) throws Exception {
        if (logtext.size() == 0) {
            return;
        }

        // write to the log file
        for (int i = 0; i < logtext.size(); i++) {
            LOGGER.error((String) logtext.get(i));
        }

        // prepare editor with error messages
        String pagedir = CONFIG.getString("MCR.editor_page_dir", "");
        String myfile = pagedir + CONFIG.getString("MCR.editor_page_error_formular", "editor_error_formular.xml");
        org.jdom.Document jdom = null;

        try {
            InputStream in = (new URL(getBaseURL() + myfile + "?XSL.Style=xml")).openStream();

            if (in == null) {
                throw new MCRConfigurationException("Can't read editor file " + myfile);
            }

            jdom = new org.jdom.input.SAXBuilder().build(in);

            org.jdom.Element root = jdom.getRootElement();
            List sectionlist = root.getChildren("section");

            for (int i = 0; i < sectionlist.size(); i++) {
                org.jdom.Element section = (org.jdom.Element) sectionlist.get(i);

                if (!section.getAttributeValue("lang", org.jdom.Namespace.XML_NAMESPACE).equals(lang.toLowerCase())) {
                    continue;
                }

                org.jdom.Element p = new org.jdom.Element("p");
                section.addContent(0, p);

                org.jdom.Element center = new org.jdom.Element("center");

                // the error message
                org.jdom.Element table = new org.jdom.Element("table");
                table.setAttribute("width", "80%");

                for (int j = 0; j < logtext.size(); j++) {
                    org.jdom.Element tr = new org.jdom.Element("tr");
                    org.jdom.Element td = new org.jdom.Element("td");
                    org.jdom.Element el = new org.jdom.Element("font");
                    el.setAttribute("color", "red");
                    el.addContent((String) logtext.get(j));
                    td.addContent(el);
                    tr.addContent(td);
                    table.addContent(tr);
                }

                center.addContent(table);
                section.addContent(1, center);
                p = new org.jdom.Element("p");
                section.addContent(2, p);

                // the edit button
                org.jdom.Element form = section.getChild("form");
                form.setAttribute("action", job.getResponse().encodeRedirectURL(getBaseURL() + "servlets/MCRStartEditorServlet"));

                org.jdom.Element input1 = new org.jdom.Element("input");
                input1.setAttribute("name", "lang");
                input1.setAttribute("type", "hidden");
                input1.setAttribute("value", lang);
                form.addContent(input1);

                org.jdom.Element input2 = new org.jdom.Element("input");
                input2.setAttribute("name", "se_mcrid");
                input2.setAttribute("type", "hidden");
                input2.setAttribute("value", ID.getId());
                form.addContent(input2);

                org.jdom.Element input3 = new org.jdom.Element("input");
                input3.setAttribute("name", "type");
                input3.setAttribute("type", "hidden");
                input3.setAttribute("value", ID.getTypeId());
                form.addContent(input3);
            }
        } catch (org.jdom.JDOMException e) {
            throw new MCRException("Can't read editor file " + myfile + " or it has a parse error.", e);
        }

        System.out.println(jdom);

        // restart editor
        job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
        job.getRequest().setAttribute("XSL.Style", lang);

        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(job.getRequest(), job.getResponse());
    }

    private static final class EditorValidator {
        private List errorlog;

        private Document input;

        private MCRObjectID id;

        public EditorValidator(Document jdom_in, MCRObjectID id) {
            this.errorlog = new ArrayList();
            this.input = jdom_in;
            this.id = id;
            checkObject();
        }

        /**
         * @param input
         * @param errorlog
         * @return
         */
        public Document generateValidMyCoReObject() {
            MCRObject obj = new MCRObject();
            try {
                // load the JDOM object
                byte[] xml = MCRUtils.getByteArray(input);
                obj.setFromXML(xml, true);
                Date curTime = new Date();
                obj.getService().setDate("createdate", curTime);
                obj.getService().setDate("modifydate", curTime);

                // return the XML tree
                input = obj.createXML();
            } catch (MCRException e) {
                errorlog.add(e.getMessage());

                Exception ex = e.getException();

                if (ex != null) {
                    errorlog.add(ex.getMessage());
                }
            }
            return input;
        }

        public List getErrorLog() {
            return errorlog;
        }

        /**
         * @param datasubtag
         * @param errorlog
         */
        private boolean checkMCRMetaBoolean(Element datasubtag) {
            try {
                MCRMetaBoolean test = new MCRMetaBoolean();
                test.setFromDOM(datasubtag);

                if (!test.isValid()) {
                    throw new MCRException("");
                }
            } catch (Exception e) {
                errorlog.add("Element " + datasubtag.getName() + " is not valid.");
                return false;
            }
            return true;
        }

        /**
         * @param datasubtag
         * @param errorlog
         */
        private boolean checkMCRMetaPersonName(Element datasubtag) {
            if (datasubtag.getAttribute("lang") != null) {
                datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE);
            }

            try {
                MCRMetaPersonName test = new MCRMetaPersonName();
                test.setFromDOM(datasubtag);

                if (!test.isValid()) {
                    throw new MCRException("");
                }
            } catch (Exception e) {
                errorlog.add("Element " + datasubtag.getName() + " is not valid.");
                return false;
            }
            return true;
        }

        /**
         * @param datasubtag
         * @param errorlog
         */
        private boolean checkMCRMetaInstitutionName(Element datasubtag) {
            if (datasubtag.getAttribute("lang") != null) {
                datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE);
            }

            try {
                MCRMetaInstitutionName test = new MCRMetaInstitutionName();
                test.setFromDOM(datasubtag);

                if (!test.isValid()) {
                    throw new MCRException("");
                }
            } catch (Exception e) {
                errorlog.add("Element " + datasubtag.getName() + " is not valid.");
                return false;
            }
            return true;
        }

        /**
         * @param datasubtag
         * @param errorlog
         */
        private boolean checkMCRMetaAddress(Element datasubtag) {
            if (datasubtag.getAttribute("lang") != null) {
                datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE);
            }

            try {
                MCRMetaAddress test = new MCRMetaAddress();
                test.setFromDOM(datasubtag);

                if (!test.isValid()) {
                    throw new MCRException("");
                }
            } catch (Exception e) {
                // errorlog.add("Element "+datasubtag.getName()+" is not
                // valid.");
                return false;
            }
            return true;
        }

        /**
         * @param datasubtag
         * @param errorlog
         */
        private boolean checkMCRMetaNumber(Element datasubtag) {
            String text = datasubtag.getTextNormalize();

            if ((text == null) || ((text = text.trim()).length() == 0)) {
                return false;
            } else {

                if (datasubtag.getAttribute("lang") != null) {
                    datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE);
                }

                try {
                    MCRMetaNumber test = new MCRMetaNumber();
                    test.setFromDOM(datasubtag);

                    if (!test.isValid()) {
                        throw new MCRException("");
                    }
                } catch (Exception e) {
                    errorlog.add("Element " + datasubtag.getName() + " is not valid.");
                    return false;
                }
            }
            return true;
        }

        /**
         * @param datasubtag
         * @param errorlog
         */
        private boolean checkMCRMetaHistoryDate(Element datasubtag) {
            if (datasubtag.getAttribute("lang") != null) {
                datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE);
            }

            try {
                MCRMetaHistoryDate test = new MCRMetaHistoryDate();
                test.setFromDOM(datasubtag);

                if (!test.isValid()) {
                    throw new MCRException("");
                }
            } catch (Exception e) {
                errorlog.add("Element " + datasubtag.getName() + " is not valid.");
                return false;
            }
            return true;
        }

        /**
         * @param datasubtag
         * @param errorlog
         */
        private boolean checkMCRDate(Element datasubtag) {
            String text = datasubtag.getTextNormalize();

            if ((text == null) || ((text = text.trim()).length() == 0)) {
                return false;
            } else {

                if (datasubtag.getAttribute("lang") != null) {
                    datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE);
                }

                try {
                    MCRMetaDate test = new MCRMetaDate();
                    test.setFromDOM(datasubtag);

                    if (!test.isValid()) {
                        throw new MCRException("");
                    }
                } catch (Exception e) {
                    errorlog.add("Element " + datasubtag.getName() + " is not valid.");
                    return false;
                }
            }
            return true;
        }

        /**
         * @param errorlog
         * @param datasubtag
         */
        private boolean checkMCRMetaLinkID(List errorlog, Element datasubtag) {
            String href = datasubtag.getAttributeValue("href");

            if (href == null) {
                return false;
            } else {

                if (datasubtag.getAttribute("type") != null) {
                    datasubtag.getAttribute("type").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
                }

                if (datasubtag.getAttribute("href") != null) {
                    datasubtag.getAttribute("href").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
                }

                if (datasubtag.getAttribute("title") != null) {
                    datasubtag.getAttribute("title").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
                }

                if (datasubtag.getAttribute("label") != null) {
                    datasubtag.getAttribute("label").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
                }

                try {
                    MCRMetaLinkID test = new MCRMetaLinkID();
                    test.setFromDOM(datasubtag);

                    if (!test.isValid()) {
                        throw new MCRException("");
                    }
                } catch (Exception e) {
                    errorlog.add("Element " + datasubtag.getName() + " is not valid.");
                    return false;
                }
            }
            return true;
        }

        /**
         * @param datasubtag
         * @param errorlog
         */
        private boolean checkMCRMetaLink(Element datasubtag) {
            String href = datasubtag.getAttributeValue("href");

            if (href == null) {
                return false;
            } else {

                if (datasubtag.getAttribute("type") != null) {
                    datasubtag.getAttribute("type").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
                }

                if (datasubtag.getAttribute("href") != null) {
                    datasubtag.getAttribute("href").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
                }

                if (datasubtag.getAttribute("title") != null) {
                    datasubtag.getAttribute("title").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
                }

                if (datasubtag.getAttribute("label") != null) {
                    datasubtag.getAttribute("label").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
                }

                try {
                    MCRMetaLink test = new MCRMetaLink();
                    test.setFromDOM(datasubtag);

                    if (!test.isValid()) {
                        throw new MCRException("");
                    }
                } catch (Exception e) {
                    errorlog.add("Element " + datasubtag.getName() + " is not valid.");
                    return false;
                }
            }
            return true;
        }

        /**
         * @param datasubtag
         * @param errorlog
         * @param datatagIt
         */
        private boolean checkMCRMetaClassification(Element datasubtag) {
            String categid = datasubtag.getAttributeValue("categid");

            if (categid == null) {
                return false;
            } else {

                try {
                    MCRMetaClassification test = new MCRMetaClassification();
                    test.setFromDOM(datasubtag);

                    if (!test.isValid()) {
                        throw new MCRException("");
                    }
                } catch (Exception e) {
                    errorlog.add("Element " + datasubtag.getName() + " is not valid.");
                    return false;
                }
            }
            return true;
        }

        /**
         * @param datasubtag
         * @param errorlog
         */
        private boolean checkMCRMetaISO8601Date(Element datasubtag) {
            String text = datasubtag.getTextNormalize();

            if ((text == null) || ((text = text.trim()).length() == 0)) {
                return false;
            } else {

                if (datasubtag.getAttribute("lang") != null) {
                    datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE);
                }

                try {
                    MCRMetaISO8601Date test = new MCRMetaISO8601Date();
                    test.setFromDOM(datasubtag);

                    if (!test.isValid()) {
                        throw new MCRException("");
                    }
                } catch (Exception e) {
                    errorlog.add("Element " + datasubtag.getName() + " is not valid.");
                    return false;
                }
            }
            return true;
        }

        /**
         * @param datasubtag
         * @param errorlog
         * @param datatagIt
         */
        private boolean checkMCRMetaLangText(Element datasubtag) {
            String text = datasubtag.getTextNormalize();

            if ((text == null) || ((text = text.trim()).length() == 0)) {
                return false;
            } else {

                if (datasubtag.getAttribute("lang") != null) {
                    datasubtag.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE);
                }

                try {
                    MCRMetaLangText test = new MCRMetaLangText();
                    test.setFromDOM(datasubtag);

                    if (!test.isValid()) {
                        throw new MCRException("");
                    }
                } catch (Exception e) {
                    errorlog.add("Element " + datasubtag.getName() + " is not valid.");
                    return false;
                }
            }
            return true;
        }

        /**
         * @param input
         * @param ID
         * @param errorlog
         */
        private void checkObject() {
            // add the namespaces (this is a workaround)
            org.jdom.Element root = input.getRootElement();
            root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
            root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));

            // set the schema
            String mcr_schema = "datamodel-" + id.getTypeId() + ".xsd";
            root.setAttribute("noNamespaceSchemaLocation", mcr_schema, org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));

            // check the label
            String label = root.getAttributeValue("label");

            if ((label == null) || ((label = label.trim()).length() == 0)) {
                root.setAttribute("label", id.getId());
            }

            // remove the path elements from the incoming
            org.jdom.Element pathes = root.getChild("pathes");

            if (pathes != null) {
                root.removeChildren("pathes");
            }

            // structure
            org.jdom.Element structure = root.getChild("structure");

            if (structure == null) {
                root.addContent(new Element("structure"));
            } else {
                checkObjectStructure(structure);
            }

            // metadata
            Element metadata = root.getChild("metadata");
            checkObjectMetadata(metadata);

            // service
            org.jdom.Element service = root.getChild("service");
            checkObjectService(service);
        }

        /**
         * @param service
         * @param errorlog
         */
        private void checkObjectService(org.jdom.Element service) {
            List servicelist = service.getChildren();
            Iterator serviceIt = servicelist.iterator();

            while (serviceIt.hasNext()) {
                Element datatag = (Element) serviceIt.next();

                if (datatag.getName().equals("servflags")) {
                    // get current user
                    MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
                    String userid = "User:" + mcrSession.getCurrentUserID();

                    // is the a new editor?
                    List servflaglist = datatag.getChildren();
                    int servflaglistlen = servflaglist.size();
                    boolean test = true;

                    for (int h = 0; h < servflaglistlen; h++) {
                        org.jdom.Element servflag = (org.jdom.Element) servflaglist.get(h);

                        if (servflag.getText().equals(userid)) {
                            test = false;

                            break;
                        }
                    }

                    if (test) {
                        MCRMetaLangText line = new MCRMetaLangText("servflags", "servflag", "de", "", 0, "plain", userid);
                        datatag.addContent(line.createXML());
                    }
                }
                if (!checkMetaTags(datatag)) {
                    // e.g. datatag is empty
                    serviceIt.remove();
                }
            }
        }

        /**
         * @param metadata
         * @param errorlog
         */
        private void checkObjectMetadata(Element metadata) {
            metadata.getAttribute("lang").setNamespace(Namespace.XML_NAMESPACE);

            List metadatalist = metadata.getChildren();
            Iterator metaIt = metadatalist.iterator();

            while (metaIt.hasNext()) {
                Element datatag = (Element) metaIt.next();
                if (!checkMetaTags(datatag)) {
                    // e.g. datatag is empty
                    metaIt.remove();
                }
            }
        }

        /**
         * @param datatag
         * @param errorlog
         */
        private boolean checkMetaTags(Element datatag) {
            String mcrclass = datatag.getAttributeValue("class");
            List datataglist = datatag.getChildren();
            Iterator datatagIt = datataglist.iterator();

            while (datatagIt.hasNext()) {
                Element datasubtag = (Element) datatagIt.next();

                if (mcrclass.equals("MCRMetaLangText")) {
                    if (!checkMCRMetaLangText(datasubtag)) {
                        datatagIt.remove();
                    }
                } else if (mcrclass.equals("MCRMetaISO8601Date")) {
                    if (!checkMCRMetaISO8601Date(datasubtag)) {
                        datatagIt.remove();
                    }
                } else if (mcrclass.equals("MCRMetaClassification")) {
                    if (!checkMCRMetaClassification(datasubtag)) {
                        datatagIt.remove();
                    }
                } else if (mcrclass.equals("MCRMetaLink")) {
                    if (!checkMCRMetaLink(datasubtag)) {
                        datatagIt.remove();
                    }
                } else if (mcrclass.equals("MCRMetaLinkID")) {
                    if (!checkMCRMetaLinkID(errorlog, datasubtag)) {
                        datatagIt.remove();
                    }
                } else if (mcrclass.equals("MCRMetaDate")) {
                    if (!checkMCRDate(datasubtag)) {
                        datatagIt.remove();
                    }
                } else if (mcrclass.equals("MCRMetaHistoryDate")) {
                    if (!checkMCRMetaHistoryDate(datasubtag)) {
                        datatagIt.remove();
                    }
                } else if (mcrclass.equals("MCRMetaNumber")) {
                    if (!checkMCRMetaNumber(datasubtag)) {
                        datatagIt.remove();
                    }
                } else if (mcrclass.equals("MCRMetaAddress")) {
                    if (!checkMCRMetaAddress(datasubtag)) {
                        datatagIt.remove();
                    }
                } else if (mcrclass.equals("MCRMetaInstitutionName")) {
                    if (!checkMCRMetaInstitutionName(datasubtag)) {
                        datatagIt.remove();
                    }
                } else if (mcrclass.equals("MCRMetaPersonName")) {
                    if (!checkMCRMetaPersonName(datasubtag)) {
                        datatagIt.remove();
                    }
                } else if (mcrclass.equals("MCRMetaBoolean")) {
                    if (!checkMCRMetaBoolean(datasubtag)) {
                        datatagIt.remove();
                    }
                } else {
                    LOGGER.error("To do for type " + mcrclass + " not found.");
                }
            }
            if (datatag.getChildren().size() == 0) {
                return false;
            }
            return true;
        }

        /**
         * @param structure
         * @param errorlog
         */
        private void checkObjectStructure(org.jdom.Element structure) {
            List structurelist = structure.getChildren();
            Iterator structIt = structurelist.iterator();

            while (structIt.hasNext()) {
                Element datatag = (Element) structIt.next();
                String mcrclass = datatag.getAttributeValue("class");
                List datataglist = datatag.getChildren();
                Iterator datatagIt = datataglist.iterator();

                while (datatagIt.hasNext()) {
                    Element datasubtag = (Element) datatagIt.next();

                    // MCRMetaLinkID
                    if (mcrclass.equals("MCRMetaLinkID")) {
                        String href = datasubtag.getAttributeValue("href");

                        if (href == null) {
                            datatagIt.remove();
                        } else {

                            if (datasubtag.getAttribute("type") != null) {
                                datasubtag.getAttribute("type").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
                            }

                            if (datasubtag.getAttribute("href") != null) {
                                datasubtag.getAttribute("href").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
                            }

                            if (datasubtag.getAttribute("title") != null) {
                                datasubtag.getAttribute("title").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
                            }

                            if (datasubtag.getAttribute("label") != null) {
                                datasubtag.getAttribute("label").setNamespace(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
                            }

                            try {
                                MCRMetaLinkID test = new MCRMetaLinkID();
                                test.setFromDOM(datasubtag);

                                if (!test.isValid()) {
                                    throw new MCRException("");
                                }
                            } catch (Exception e) {
                                errorlog.add("Element " + datasubtag.getName() + " is not valid.");
                                datatagIt.remove();
                            }
                        }
                    }
                }

                datataglist = datatag.getChildren();

                if (datataglist.size() == 0) {
                    structIt.remove();
                } else {
                    if (datatag.getName().equals("parents")) {
                        LOGGER.debug("A parrent was found.");
                    }
                }
            }
        }

    }
}
