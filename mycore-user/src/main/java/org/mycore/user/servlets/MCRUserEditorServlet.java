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

package org.mycore.user.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.user.MCRGroup;
import org.mycore.user.MCRGroupResolver;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRUserEditorHandler;
import org.mycore.user.MCRUserMgr;
import org.mycore.user.MCRUserResolver;

/**
 * This servlet provides a web interface for the editors of the user management
 * of the mycore system.
 * 
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRUserEditorServlet extends MCRUserAdminGUICommons {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRUserEditorServlet.class);

    /** Initialisation of the servlet */
    public void init() throws ServletException {
        super.init();
    }

    /**
     * This method overrides doGetPost of MCRServlet and handles HTTP requests.
     * Depending on the mode parameter in the request, the method dispatches
     * actions to be done by subsequent private methods. In contrast to the
     * MCRUserAdminServlet - which serves as an entry point and dispatcher for
     * all use cases of the user management GUI - this servlet is called by the
     * MyCoRe editor framework, triggered by the editor definition files of the
     * user management GUI. In addition, this servlet is the target of the
     * MyCoRe editor framework for the user management GUI, i.e. it reads the
     * editor submission and acts accordingly.
     * 
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    public void doGetPost(MCRServletJob job) throws IOException {

        if (MCRWebsiteWriteProtection.printInfoPageIfNoAccess(job.getRequest(), job.getResponse(), getBaseURL()))
            return;

        String mode = getProperty(job.getRequest(), "mode");

        if (mode == null) {
            mode = "xml";
        }

        if (mode.equals("getAssignableGroupsForUser")) {
            getAssignableGroupsForUser(job);
        } else if (mode.equals("getAllUsers")) {
            getAllUsers(job);
        } else if (mode.equals("getAllGroups")) {
            getAllGroups(job);
        } else if (mode.equals("retrieveuserxml")) {
            retrieveUserXML(job);
        } else if (mode.equals("retrievegroupxml")) {
            retrieveGroupXML(job);
        } else if (mode.equals("retrievealluserxml")) {
            retrieveAllUserXML(job);
        } else if (mode.equals("xml")) {
            getEditorSubmission(job);
        } else { // no valid mode

            String msg = "The request did not contain a valid mode for this servlet!";
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        }
    }

    /**
     * This method retrieves a list of groups the current user may assign to a
     * new or existing user account. Typically this servlet mode is implicitly
     * called by an MyCoRe editor definition file, e.g. to fill drop down boxes
     * or lists in the user administration GUI.
     * @deprecated use the {@link MCRGroupResolver} instead
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    @Deprecated
    private void getAssignableGroupsForUser(MCRServletJob job) throws IOException {
        org.jdom2.Element root;
        try {
            root = MCRUserEditorHandler.getAssignableGroupsForUser();
        } catch (MCRAccessException ex) {
            LOGGER.warn("", ex);
            showNoPrivsPage(job);
            return;
        }
        org.jdom2.Document jdomDoc = new org.jdom2.Document(root);
        getLayoutService().sendXML(job.getRequest(), job.getResponse(), new MCRJDOMContent(jdomDoc));
    }

    /**
     * This method retrieves a list of all groups. Typically this servlet mode
     * is implicitly called by an MyCoRe editor definition file, e.g. to fill
     * drop down boxes or lists in the user administration GUI.
     * 
     * @deprecated use the {@link MCRGroupResolver} instead
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    @Deprecated
    private void getAllGroups(MCRServletJob job) throws IOException {
        try {
            Document jdomDoc = MCRUserEditorHandler.getAllGroups();
            getLayoutService().sendXML(job.getRequest(), job.getResponse(), new MCRJDOMContent(jdomDoc));
        } catch(MCRAccessException exc) {
            showNoPrivsPage(job);
        }
    }

    /**
     * This method retrieves a list of all users. Typically this servlet mode is
     * implicitly called by an MyCoRe editor definition file, e.g. to fill drop
     * down boxes or lists in the user administration GUI.
     * 
     * @deprecated use the {@link MCRUserResolver)} instead
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    @Deprecated
    private void getAllUsers(MCRServletJob job) throws IOException {
        try {
            Document jdomDoc = MCRUserEditorHandler.getAllUsers();
            getLayoutService().sendXML(job.getRequest(), job.getResponse(), new MCRJDOMContent(jdomDoc));
        } catch(MCRAccessException exc) {
            showNoPrivsPage(job);
        }
    }

    /**
     * This method retrieves the all users list
     * 
     * @param job
     *            The MCRServletJob instance
     * @param currentPrivs
     *            The current privlegs in ArrayList
     * 
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    private void retrieveAllUserXML(MCRServletJob job) throws IOException {
        // We first check the privileges for this use case
        if (!MCRAccessManager.checkPermission("modify-user") && !MCRAccessManager.checkPermission("modify-contact")) {
            showNoPrivsPage(job);
            return;
        }
        try {
            org.jdom2.Document userlist = MCRUserMgr.instance().getAllUsers();
            doLayout(job, "ListAllUser", userlist, false);
        } catch (MCRException ex) {
            // TODO: Es gibt Probleme mit den Fehlermeldungen, siehe oben.
            String msg = "An error occured while retrieving a user object from the store!";
            job.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        }
    }

    /**
     * @deprecated use the {@link MCRUserResolver} instead
     * 
     * This method is still experimental! It is needed in the use case "modify
     * user".
     */
    @Deprecated
    private void retrieveUserXML(MCRServletJob job) throws IOException {
        try {
            String userID = getProperty(job.getRequest(), "uid");
            Document jdomDoc = MCRUserEditorHandler.retrieveUserXml(userID);
            getLayoutService().sendXML(job.getRequest(), job.getResponse(), new MCRJDOMContent(jdomDoc));
        } catch(MCRAccessException ex) {
            showNoPrivsPage(job);
        } catch (MCRException ex) {
            // TODO: Es gibt Probleme mit den Fehlermeldungen, siehe oben.
            String msg = "An error occured while retrieving a user object from the store!";
            job.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        }
    }

    /**
     * @deprecated use the {@link MCRGroupResolver} instead
     * This method is still experimental! It is needed in the use case "modify
     * group".
     */
    @Deprecated
    private void retrieveGroupXML(MCRServletJob job) throws IOException {
        try {
            String groupID = getProperty(job.getRequest(), "gid");
            Document jdomDoc = MCRUserEditorHandler.retrieveGroupXml(groupID);
            getLayoutService().sendXML(job.getRequest(), job.getResponse(), new MCRJDOMContent(jdomDoc));
        } catch(MCRAccessException ex) {
            showNoPrivsPage(job);
        } catch (MCRException ex) {
            // TODO: Es gibt Probleme mit den Fehlermeldungen, siehe oben.
            String msg = "An error occured while retrieving a group object from the store!";
            job.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        }
        return;
    }

    /**
     * This method reads the XML data sent by the MyCoRe editor framework.
     * 
     * @param job
     *            The MCRServletJob instance
     * @throws IOException
     *             for java I/O errors.
     * @throws ServletException
     *             for errors from the servlet engine.
     */
    private void getEditorSubmission(MCRServletJob job) throws IOException {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String currentUserID = mcrSession.getUserInformation().getUserID();

        // Read the XML data sent by the editor
        MCREditorSubmission sub = (MCREditorSubmission) (job.getRequest().getAttribute("MCREditorSubmission"));
        org.jdom2.Document jdomDoc = sub.getXML();

        // Read the request parameters
        MCRRequestParameters parms;

        parms = sub.getParameters();

        String useCase = parms.getParameter("usecase");

        // Determine the use case
        if ((useCase.equals("create-user") || useCase.equals("modify-user") || useCase.equals("modify-contact"))
            && jdomDoc.getRootElement().getName().equals("mycoreuser")) {
            String numID = Integer.toString(MCRUserMgr.instance().getMaxUserNumID() + 1);
            jdomDoc.getRootElement().getChild("user").setAttribute("numID", numID);

            if (jdomDoc.getRootElement().getChild("user").getAttributeValue("id_enabled") == null) {
                jdomDoc.getRootElement().getChild("user").setAttribute("id_enabled", "false");
            }

            if (jdomDoc.getRootElement().getChild("user").getAttributeValue("update_allowed") == null) {
                jdomDoc.getRootElement().getChild("user").setAttribute("update_allowed", "false");
            }

            org.jdom2.Element userElement = jdomDoc.getRootElement().getChild("user");

            if (useCase.equals("create-user")) {
                MCRUser newUser = new MCRUser(userElement, true);
                MCRUserMgr.instance().createUser(newUser);

                LOGGER.info("User " + currentUserID + " has successfully created the new user: " + newUser.getID());
            } else {
                String uid = jdomDoc.getRootElement().getChild("user").getAttributeValue("ID");
                String newpwd = jdomDoc.getRootElement().getChild("user").getChild("user.password").getText();
                MCRUserMgr umgr = MCRUserMgr.instance();
                MCRUser olduser = umgr.retrieveUser(uid);
                MCRUser thisUser = null;
                if (olduser.getPassword().compareTo(newpwd) == 0) {
                    thisUser = new MCRUser(userElement, false);
                } else {
                    thisUser = new MCRUser(userElement, true);
                }
                MCRUserMgr.instance().updateUser(thisUser);
            }

            // doLayout(job, "xml", jdomDoc, true);
            showOkPage(job);
        } else if ((useCase.equals("create-group") || useCase.equals("modify-group")) && jdomDoc.getRootElement().getName().equals("mycoregroup")) {
            String groupID = jdomDoc.getRootElement().getChild("group").getAttributeValue("ID");
            if (groupID == null)
                throw new MCRException("groupid is not valid");
            MCRGroup group = new MCRGroup(jdomDoc.getRootElement().getChild("group"));
            if (useCase.equals("create-group")) {
                MCRUserMgr.instance().createGroup(group);
            } else {
                MCRUserMgr.instance().updateGroup(group);
            }
            // doLayout(job, "xml", jdomDoc, true);
            showOkPage(job);
        } else {
            // TODO: error message
        }
    }
}
