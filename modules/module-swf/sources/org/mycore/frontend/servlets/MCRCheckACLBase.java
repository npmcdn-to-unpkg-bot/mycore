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
import java.util.List;

import javax.servlet.RequestDispatcher;

import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;

/**
 * This class is the superclass of servlets which checks the MCREditorServlet
 * output XML and store the XML in a file or if an error was occured start the
 * editor again.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
abstract public class MCRCheckACLBase extends MCRCheckBase {
    /**
     * This method overrides doGetPost of MCRServlet. <br />
     */
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
        String lang = mcrSession.getCurrentLanguage();
        LOGGER.info("LANG = " + lang);

        // prepare the MCRObjectID's for the Metadata
        String mmcrid = oldmcrid;
        MCRObjectID ID = new MCRObjectID(mmcrid);

        if (!ID.getTypeId().equals(oldtype)) {
            ID = new MCRObjectID(oldmcrid);
        }

        // create a service object and prepare it
        org.jdom.Element outelm = prepareService((org.jdom.Document) indoc.clone(), ID, job, lang);
        org.jdom.Document d = new org.jdom.Document(outelm);
        System.out.println("##################################################");
        System.out.println(new String(MCRUtils.getByteArray(d)));
        System.out.println("##################################################");

        // Save the prepared metadata object
        storeService(outelm, job, ID);

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
     * The method store the incoming service data from the ACL editor to the
     * workflow.
     * 
     * @param outelm
     *            the service subelement of an MCRObject
     * @param job
     *            the MCRServletJob instance
     * @param ID
     *            the MCRObjectID
     */
    abstract public void storeService(org.jdom.Element outelm, MCRServletJob job, MCRObjectID ID);

    /**
     * The method read the incoming JDOM tree in a MCRObject and prepare this by
     * the following rules. After them it return a JDOM Element as clone of the
     * prepared data.
     * 
     * @param jdom_in
     *            the JDOM tree from the editor
     * @param ID
     *            the MCRObjectID of the MCRObject
     * @param job
     *            the MCRServletJob data
     * @param lang
     *            the current language
     */
    protected org.jdom.Element prepareService(org.jdom.Document jdom_in, MCRObjectID ID, MCRServletJob job, String lang) throws Exception {
        org.jdom.Element elm_out = null;
        ArrayList logtext = new ArrayList();
        org.jdom.Element root = jdom_in.getRootElement();
        if (root != null) {
            org.jdom.Element servacls = root.getChild("servacls");
            if (servacls != null) {
                List servacllist = servacls.getChildren("servacl");
                if (servacllist.size() != 0) {
                    for (int i = 0; i < servacllist.size(); i++) {
                        org.jdom.Element servacl = (org.jdom.Element) servacllist.get(i);
                        org.jdom.Element outcond = servacl.getChild("condition");
                        if (outcond != null) {
                            org.jdom.Element outbool = outcond.getChild("boolean");
                            if (outbool != null) {
                                List inbool = outbool.getChildren("boolean");
                                String outoper = outbool.getAttributeValue("operator");
                                if (inbool.size() != 0 && outoper != null && !outoper.equals("true")) {
                                    for (int j = 0; j < inbool.size(); j++) {
                                        List incondlist = ((org.jdom.Element) inbool.get(j)).getChildren("condition");
                                        int k = 0;
                                        if (incondlist.size() != 0) {
                                            for (int l = 0; l < incondlist.size(); l++) {
                                                org.jdom.Element incond = (org.jdom.Element) incondlist.get(l);
                                                String condvalue = incond.getAttributeValue("value");
                                                if (condvalue == null || (condvalue = condvalue.trim()).length() == 0) {
                                                    ((org.jdom.Element) inbool.get(j)).removeContent(incond);
                                                } else {
                                                    k++;
                                                }                                     
                                            }
                                            if (k == 0) {
                                                ((org.jdom.Element) inbool.get(j)).setAttribute("operator","true");
                                            }
                                        } else {
                                            logtext.add("Can't find an inner condition element.");
                                        }
                                    }
                                } else {
                                    if (outoper == null || !outoper.equals("true")) {
                                        logtext.add("Wrong structure of MyCoRe ACL JDOM in boolean.");
                                    }
                                }
                            } else {
                                outbool = new org.jdom.Element("boolean");
                                outbool.setAttribute("operator", "true");
                                outcond.addContent(outbool);
                            }
                        } else {
                            logtext.add("Can't find a condition element.");
                        }
                    }
                } else {
                    logtext.add("Can't find a servacl element.");
                }
            } else {
                logtext.add("Can't find the servacls element.");
            }
        } else {
            logtext.add("The service part is null.");
        }
        elm_out = (org.jdom.Element) root.clone();
        errorHandlerValid(job, logtext, ID, lang);
        return elm_out;
    }

    /**
     * A method to handle valid errors.
     */
    private final void errorHandlerValid(MCRServletJob job, List logtext, MCRObjectID ID, String lang) throws Exception {
        if (logtext.size() == 0) {
            return;
        }

        // write to the log file
        for (int i = 0; i < logtext.size(); i++) {
            LOGGER.error(logtext.get(i));
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

        // restart editor
        job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
        job.getRequest().setAttribute("XSL.Style", lang);

        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(job.getRequest(), job.getResponse());
    }

}
