/**
 * $RCSfile: MCRStartClassEditorServlet.java,v $
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.frontend.servlets;

import java.util.List;
import java.util.Properties;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.jdom.Element;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.classifications.MCRClassificationBrowserData;
import org.mycore.datamodel.classifications.MCRClassificationEditor;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;

/**
 * The servlet start the MyCoRe class editor session with some parameters from a
 * HTML form. The parameters are:<br />
 * <li> name="todo" values like 'create-classification, modify-classification,
 * delete-classification, up and down' </li>
 * <li> name="path" uri to page after editactions </li>
 * <li> name="clid" classification id </li>
 * <li> name="categid" category id </li>
 * 
 * @author Anja Schaar
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */

public class MCRStartClassEditorServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRStartClassEditorServlet.class);

    private String todo = "";

    private String todo2 = "";

    private String clid = "";

    private String categid = "";

    private String path = "";

    private static MCRClassificationEditor clE = new MCRClassificationEditor();

    /**
     * Replace the doGetPost method of MCRServlet. This method will be called
     * two times when using the classification editor. Firtst time it prepare
     * date for the editor and second time it execute the operation.
     */
    public void doGetPost(MCRServletJob job) throws Exception {

        // read the XML data if given from Editorsession
        MCREditorSubmission sub = (MCREditorSubmission) (job.getRequest().getAttribute("MCREditorSubmission"));

        // read the parameter
        MCRRequestParameters parms;
        if (sub == null)
            parms = new MCRRequestParameters(job.getRequest());
        else {
            parms = sub.getParameters();
        }

        // read the parameter
        todo = parms.getParameter("todo"); // getProperty(job.getRequest(),
        // "todo");
        todo2 = parms.getParameter("todo2"); // getProperty(job.getRequest(),
        // "todo2");
        path = parms.getParameter("path"); // getProperty(job.getRequest(),
        // "path");

        // get the Classification
        clid = parms.getParameter("clid"); // getProperty(job.getRequest(),
        // "clid");
        
        categid = parms.getParameter("categid"); // getProperty(job.getRequest(),
        // "categid");

        if (todo == null)
            todo = "";
        if (todo2 == null)
            todo2 = "";

        LOGGER.debug("MCRStartClassEditorServlet TODO: " + todo);
        LOGGER.debug("MCRStartClassEditorServlet TODO2: " + todo2);
        LOGGER.debug("MCRStartClassEditorServlet CLID: " + clid);
        LOGGER.debug("MCRStartClassEditorServlet CATEGID: " + categid);

        String pagedir = CONFIG.getString("MCR.classeditor_page_dir", "");
        String myfile = "editor_form_" + todo + ".xml";

        String usererrorpage = pagedir + CONFIG.getString("MCR.classeditor_page_error_user", "editor_error_user.xml");
        String cancelpage = pagedir + CONFIG.getString("MCR.classeditor_page_cancel", "classeditor_cancel.xml");
        String icerrorpage = pagedir + CONFIG.getString("MCR.classeditor_page_error_id", "classeditor_error_clid.xml");
        String iderrorpage = pagedir + CONFIG.getString("MCR.classeditor_page_error_delete", "editor_error_delete.xml");
        String imerrorpage = pagedir + CONFIG.getString("MCR.classeditor_page_error_move", "classeditor_error_move.xml");
        String imperrorpage = pagedir + CONFIG.getString("MCR.classeditor_page_error_import", "classeditor_error_import.xml");

        String referrer=job.getRequest().getHeader("Referer");
        if(referrer==null||referrer.equals("")) {
            referrer=getBaseURL() + cancelpage;
        }

        if (!(AI.checkPermission("create-classification"))) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }

        // nach Editoraufruf von new/modify auf commit
        if ("commit-classification".equals(todo)) {
            org.jdom.Document indoc = sub.getXML();
            boolean bret = false;

            // for debug
            /*
             * XMLOutputter outputter = new XMLOutputter();
             * LOGGER.debug(outputter.outputString(indoc));
             */

            if ("create-category".equals(todo2) || "modify-category".equals(todo2)) {
                if ("create-category".equals(todo2)) {
                    // create
                    if (!clE.isLocked(clid)) {
                        MCRCategoryID id = new MCRCategoryID(clid, categid);
                        bret = clE.createCategoryInClassification(indoc, id);
                    }
                } else {
                    // modify
                    if (!clE.isLocked(clid)) {
                        MCRCategoryID id = new MCRCategoryID(clid, categid);
                        bret = clE.modifyCategoryInClassification(indoc, id);
                    }
                }
                if (bret)
                    job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(path + "&categid=" + categid + "&clid=" + clid));
            } else {
                if (path.indexOf("&clid") > 0) {
                    // Classification abschneiden um wieder auf der
                    // Classifikationsstartseite zu landen
                    path = path.substring(0, path.indexOf("&clid"));
                }
                if ("create-classification".equals(todo2)) {
                    bret = clE.createNewClassification(indoc);
                } else if ("modify-classification".equals(todo2)) {
                    if (!clE.isLocked(clid)) {
                        bret = clE.modifyClassificationDescription(indoc, clid);
                    }
                } else if ("import-classification".equals(todo2)) {
                    String fname = parms.getParameter("/mycoreclass/pathes/path").trim();
                    fname = clE.setTempFile(fname, (FileItem) sub.getFiles().get(0));
                    String sUpdate = parms.getParameter("/mycoreclass/update");
                    bret = clE.importClassification(("true".equals(sUpdate)), fname);
                    clE.deleteTempFile();
                    if (!bret) {
                        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + imperrorpage));
                        return;
                    }
                }
                if (bret)
                    job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(path));

            }
            if (!bret) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + icerrorpage));
            }
            return;
        }

        if ("up-category".equals(todo) || "down-category".equals(todo) || "left-category".equals(todo) || "right-category".equals(todo)) {
            boolean bret = false;
            if (!clE.isLocked(clid)) {
                bret = clE.moveCategoryInClassification(categid, clid, todo.substring(0, todo.indexOf("-")));
            }
            if (bret) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(path + "&categid=" + categid + "&clid=" + clid));
            } else {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + imerrorpage));
            }
            return;
        }

        // first call, direct without editor
        else if ("delete-category".equals(todo)) {
            // l?schen
            if (!clE.isLocked(clid)) {
                int cnt = clE.deleteCategoryInClassification(clid, categid);

                if (cnt == 0) { // deleted, no more references
                    job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(path + "&clid=" + clid));
                } else { // not delete cause references exist
                    job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + iderrorpage));
                }
            }
            return;
        }

        // first call, direct without editor
        else if ("delete-classification".equals(todo)) {
            if (!clE.isLocked(clid)) {
                boolean cnt = clE.deleteClassification(clid);
                if (cnt) { // deleted, no more references
                    path = getBaseURL() + "browse?mode=edit";
                    job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(path));
                } else { // not delete cause references exist
                    job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + iderrorpage));
                }
            }
            return;
        }

        // first call of editor, build the import dialogue
        else if ("import-classification".equals(todo)) {
            String base = getBaseURL() + myfile;
            Properties params = new Properties();
            params.put("cancelUrl", referrer);
            params.put("clid", clid);
            params.put("path", path);
            params.put("todo2", todo);
            params.put("todo", "commit-classification");
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));
            return;

        }

        else if ("save-all".equals(todo)) {

            if (clE.saveAll()) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(path + "&clid=" + clid));
            } else {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + imerrorpage));
            }
            return;
        } else if ("purge-all".equals(todo)) {
            if (clE.purgeAll()) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(path + "&clid=" + clid));
            } else {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + imerrorpage));
            }
            return;
        }
        // first call of editor, build the editor dialogue
        if ("create-category".equals(todo) || "modify-category".equals(todo) || "create-classification".equals(todo) || "modify-classification".equals(todo)) {

            String base = getBaseURL() + myfile;
            final String sessionObjectID = "classificationEditor";
            Properties params = new Properties();
            StringBuffer sb = new StringBuffer();
            boolean isEdited = MCRClassificationBrowserData.getClassificationPool().isEdited(MCRCategoryID.rootID(clid));
            MCRCategory classif = null;
            if (isEdited) {
                classif = MCRClassificationBrowserData.getClassificationPool().getClassificationAsPojo(MCRCategoryID.rootID(clid), false);
                LOGGER.info("CLASSIF: "+classif.getId());
            }

            if ("modify-classification".equals(todo)) {
                 if (isEdited) {
                 sb.append("session:").append(sessionObjectID);
                 MCRSessionMgr.getCurrentSession().put(sessionObjectID,
                 MCRCategoryTransformer.getMetaDataDocument(classif,true).getRootElement());
                 } else {
                 sb.append("classification:metadata:0:children:").append(clid);
                 }
                params.put("sourceUri", sb.toString());

            }
            if ("create-classification".equals(todo)) {
                MCRObjectID cli = new MCRObjectID();
                String idBase = CONFIG.getString("MCR.SWF.Project.ID", "DocPortal") + "_class";
                cli.setNextFreeId(idBase);

                if (!cli.isValid()) {
                    LOGGER.error("Create an unique CLID failed. " + cli.toString());
                }
                Element classRoot = new Element("mycoreclass").setAttribute("ID", cli.getId());
                params.put("sourceUri", "session:" + sessionObjectID);
                MCRSessionMgr.getCurrentSession().put(sessionObjectID, classRoot);
            }
            if ("modify-category".equals(todo)) {
                if (isEdited) {
                    sb.append("session:").append(sessionObjectID);
                    Element classRoot = new Element("mycoreclass").setAttribute("ID", classif.getId().getRootID());
                    Element categs = new Element("categories");
                    MCRCategoryID id = new MCRCategoryID(classif.getId().getRootID(), categid);
                    MCRCategory cat = clE.findCategory(classif, id);
                    categs.addContent(MCRCategoryTransformer.getMetaDataElement(cat, true));
                    classRoot.addContent(categs);
                    MCRSessionMgr.getCurrentSession().put(sessionObjectID, classRoot);
                } else {
                    sb.append("classification:metadata:0:children:").append(clid).append(':').append(categid);
                }
                params.put("sourceUri", sb.toString());
                params.put("categid", categid);
            }
            if ("create-category".equals(todo)) {
                 if (isEdited) {
                 sb.append("session:").append(sessionObjectID);
                 MCRSessionMgr.getCurrentSession().put(sessionObjectID, MCRCategoryTransformer.getMetaDataDocument(classif,true).getRootElement());
                 } else {
                 sb.append("classification:metadata:0:children:").append(clid);
                 }
                params.put("sourceUri", sb.toString());
                params.put("categid", categid);
            }
            params.put("cancelUrl", referrer);
            params.put("clid", clid);
            params.put("path", path);
            params.put("todo2", todo);
            params.put("todo", "commit-classification");
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));
            return;
        }

        /* Wrong input data, write warning log */
        LOGGER.warn("MCRStartClassEditorServlet default Case - Nothing to do ? " + todo);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(path));

    }

    /**
     * Normalize the ACL to use in the SWF ACL editor. Some single conditions
     * are one step to high in the hierarchie of the conditin tree. This method
     * move it down and normalized the output.
     * 
     * @param ruleelm
     *            The XML access condition from the ACL system
     */
    private final org.jdom.Element normalizeACLforSWF(org.jdom.Element ruleelm) {
        if (LOGGER.isDebugEnabled()) {
            try {
                MCRUtils.writeJDOMToSysout(new org.jdom.Document().addContent(ruleelm));
            } catch (Exception e) {

            }
        }
        org.jdom.Element newcondition = new org.jdom.Element("condition");
        newcondition.setAttribute("format", "xml");
        org.jdom.Element newwrapperand = new org.jdom.Element("boolean");
        newwrapperand.setAttribute("operator", "and");
        newcondition.addContent(newwrapperand);
        if (ruleelm == null) {
            return newcondition;
        }
        try {
            org.jdom.Element newtrue = new org.jdom.Element("boolean");
            newtrue.setAttribute("operator", "true");
            org.jdom.Element oldwrapperand = ruleelm.getChild("boolean");
            if (oldwrapperand == null) {
                return newcondition;
            }

            org.jdom.Element newuser = (org.jdom.Element) newtrue.detach();
            org.jdom.Element newdate = (org.jdom.Element) newtrue.detach();
            org.jdom.Element newip = (org.jdom.Element) newtrue.detach();
            org.jdom.Element newelm = null;

            List<org.jdom.Element> parts = oldwrapperand.getChildren();
            for (int i = 0; i < parts.size(); i++) {
                if (i > 2)
                    break;
                org.jdom.Element oldelm = (org.jdom.Element) parts.get(i).detach();
                if (oldelm.getChildren().size() == 0)
                    continue;
                if (oldelm.getName().equals("condition")) {
                    org.jdom.Element newwrapper = new org.jdom.Element("boolean");
                    newwrapper.setAttribute("operator", "or");
                    newwrapper.addContent(oldelm);
                    newelm = newwrapper;
                } else {
                    newelm = oldelm;
                }
                String testfield = "";
                List<org.jdom.Element> innercond = newelm.getChildren();
                for (int j = 0; j < innercond.size(); j++) {
                    org.jdom.Element cond = (org.jdom.Element) innercond.get(j);
                    if (cond.getName().equals("condition")) {
                        testfield = cond.getAttributeValue("field");
                    }
                }
                if (testfield.equals("user") || testfield.equals("group")) {
                    newuser = newelm;
                }
                if (testfield.equals("date")) {
                    newdate = newelm;
                }
                if (testfield.equals("ip")) {
                    newip = newelm;
                }
            }
            newwrapperand.addContent(newuser.detach());
            newwrapperand.addContent(newdate.detach());
            newwrapperand.addContent(newip.detach());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newcondition;
    }

}
