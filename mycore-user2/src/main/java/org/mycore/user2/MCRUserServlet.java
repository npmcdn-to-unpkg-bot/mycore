/**
 * $Revision: 23345 $ 
 * $Date: 2012-01-30 12:08:41 +0100 (Mo, 30 Jan 2012) $
 *
 * This file is part of the MILESS repository software.
 * Copyright (C) 2011 MILESS/MyCoRe developer team
 * See http://duepublico.uni-duisburg-essen.de/ and http://www.mycore.de/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/

package org.mycore.user2;

import static org.mycore.user2.utils.MCRUserTransformer.JAXB_CONTEXT;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.services.i18n.MCRTranslation;
import org.mycore.user2.utils.MCRUserTransformer;

/**
 * Provides functionality to search for users, list users, 
 * retrieve, delete or update user data. 
 * 
 * @author Frank L\u00fctzenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRUserServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRUserServlet.class);

    /**
     * Handles requests. The parameter 'action' selects what to do, possible
     * values are show, save, delete, password (with id as second parameter). 
     * The default is to search and list users. 
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();
        if (forbidIfGuest(res)) {
            return;
        }
        String action = req.getParameter("action");
        String uid = req.getParameter("id");
        MCRUser user;

        if ((uid == null) || (uid.trim().length() == 0)) {
            user = MCRUserManager.getCurrentUser();
            uid = user != null ? String.valueOf(user.getUserID()) : null;
        } else
            user = MCRUserManager.getUser(uid);

        if ("show".equals(action))
            showUser(req, res, user, uid);
        else if ("save".equals(action))
            saveUser(req, res);
        else if ("saveCurrentUser".equals(action))
            saveCurrentUser(req, res);
        else if ("changeMyPassword".equals(action))
            redirectToPasswordChangePage(req, res);
        else if ("password".equals(action))
            changePassword(req, res, user, uid);
        else if ("delete".equals(action))
            deleteUser(req, res, user);
        else
            listUsers(req, res);
    }

    private void redirectToPasswordChangePage(HttpServletRequest req, HttpServletResponse res) throws Exception {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        if (!checkUserIsNotNull(res, currentUser, null)) {
            return;
        }
        if (checkUserIsLocked(res, currentUser)) {
            return;
        }
        String url = currentUser.getRealm().getPasswordChangeURL();
        if (url == null) {
            String msg = MCRTranslation.translate("component.user2.UserServlet.missingRealPasswortChangeURL", currentUser.getRealmID());
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        } else {
            res.sendRedirect(url);
        }
    }

    private static boolean checkUserIsNotNull(HttpServletResponse res, MCRUser currentUser, String userID) throws IOException {
        if (currentUser == null) {
            String uid = userID == null ? MCRSessionMgr.getCurrentSession().getUserInformation().getUserID() : userID;
            String msg = MCRTranslation.translate("component.user2.UserServlet.currentUserUnknown", uid);
            res.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return false;
        }
        return true;
    }

    private static boolean checkUserIsLocked(HttpServletResponse res, MCRUser currentUser) throws IOException {
        if (currentUser.isLocked()) {
            String userName = currentUser.getUserID();
            String msg = MCRTranslation.translate("component.user2.UserServlet.isLocked", userName);
            res.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return true;
        }
        return false;
    }

    private static boolean forbidIfGuest(HttpServletResponse res) throws IOException {
        if (MCRSessionMgr.getCurrentSession().getUserInformation().equals(MCRSystemUserInformation.getGuestInstance())) {
            String msg = MCRTranslation.translate("component.user2.UserServlet.noGuestAction");
            res.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return true;
        }
        return false;
    }

    /**
     * Handles MCRUserServlet?action=show&id={userID}.
     * Outputs user data for the given id using user.xsl.
     */
    private void showUser(HttpServletRequest req, HttpServletResponse res, MCRUser user, String uid) throws Exception {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        if (!checkUserIsNotNull(res, currentUser, null) || !checkUserIsNotNull(res, user, uid)) {
            return;
        }
        boolean allowed = MCRAccessManager.checkPermission(MCRUser2Constants.USER_ADMIN_PERMISSION) || currentUser.equals(user)
            || currentUser.equals(user.getOwner());
        if (!allowed) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        LOGGER.info("show user " + user.getUserID() + " " + user.getUserName() + " " + user.getRealmID());
        getLayoutService().doLayout(req, res, getContent(user));
    }

    /**
     * Invoked by editor form user-editor.xml to check for a valid
     * login user name and realm combination.
     */
    public static boolean checkUserName(Element u) {
        String userName = u.getAttributeValue("name");

        String realmID = MCRRealmFactory.getLocalRealm().getID();
        if (u.getChild("realm") != null) {
            realmID = u.getChild("realm").getAttributeValue("id");
        }

        // Check for required fields is done in the editor form itself, not here
        if ((userName == null) || (realmID == null)) {
            return true;
        }

        // In all other cases, combination of userName and realm must not exist
        return !MCRUserManager.exists(userName, realmID);
    }

    private void saveCurrentUser(HttpServletRequest req, HttpServletResponse res) throws IOException {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        if (!checkUserIsNotNull(res, currentUser, null)) {
            return;
        }
        if (checkUserIsLocked(res, currentUser)) {
            return;
        }
        if (!currentUser.hasNoOwner()) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        MCREditorSubmission sub = (MCREditorSubmission) (req.getAttribute("MCREditorSubmission"));
        Element u = sub.getXML().getRootElement();
        updateBasicUserInfo(u, currentUser);
        MCRUserManager.updateUser(currentUser);

        res.sendRedirect(res.encodeRedirectURL("MCRUserServlet?action=show"));
    }

    /**
     * Handles MCRUserServlet?action=save&id={userID}.
     * This is called by user-editor.xml editor form to save the
     * changed user data from editor submission. Redirects to
     * show user data afterwards. 
     */
    private void saveUser(HttpServletRequest req, HttpServletResponse res) throws Exception {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        if (!checkUserIsNotNull(res, currentUser, null)) {
            return;
        }
        boolean hasAdminPermission = MCRAccessManager.checkPermission(MCRUser2Constants.USER_ADMIN_PERMISSION);
        boolean allowed = hasAdminPermission || MCRAccessManager.checkPermission(MCRUser2Constants.USER_CREATE_PERMISSION);
        if (!allowed) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        MCREditorSubmission sub = (MCREditorSubmission) (req.getAttribute("MCREditorSubmission"));
        Element u = sub.getXML().getRootElement();
        String userName = u.getAttributeValue("name");

        String realmID = MCRRealmFactory.getLocalRealm().getID();
        if (hasAdminPermission) {
            realmID = u.getChild("realm").getAttributeValue("id");
        }

        MCRUser user;
        boolean userExists = MCRUserManager.exists(userName, realmID);
        if (!userExists) {
            user = new MCRUser(userName, realmID);
            LOGGER.info("create new user " + userName + " " + realmID);

            // For new local users, set password
            String pwd = u.getChildText("password");
            if ((pwd != null) && (pwd.trim().length() > 0) && user.getRealm().equals(MCRRealmFactory.getLocalRealm())) {
                MCRUserManager.updatePasswordHashToSHA1(user, pwd);
            }
        } else {
            user = MCRUserManager.getUser(userName, realmID);
            if (!(hasAdminPermission || currentUser.equals(user) || currentUser.equals(user.getOwner()))) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        String hint = u.getChildText("hint");
        if ((hint != null) && (hint.trim().length() == 0)) {
            hint = null;
        }
        user.setHint(hint);

        updateBasicUserInfo(u, user);

        if (hasAdminPermission) {
            boolean locked = "true".equals(u.getAttributeValue("locked"));
            user.setLocked(locked);
            Element o = u.getChild("owner");
            if (o != null && !o.getAttributes().isEmpty()) {
                String ownerName = o.getAttributeValue("name");
                String ownerRealm = o.getAttributeValue("realm");
                MCRUser owner = MCRUserManager.getUser(ownerName, ownerRealm);
                if (!checkUserIsNotNull(res, owner, ownerName + "@" + ownerRealm)) {
                    return;
                }
                user.setOwner(owner);
            } else {
                user.setOwner(null);
            }
            String validUntilText = u.getChildTextTrim("validUntil");
            if (validUntilText == null || validUntilText.length() == 0) {
                user.setValidUntil(null);
            } else {
                MCRISO8601Date date = new MCRISO8601Date(validUntilText);
                user.setValidUntil(date.getDate());
            }
        } else { // save read user of creator
            user.setRealm(MCRRealmFactory.getLocalRealm());
            user.setOwner(currentUser);
        }
        Element gs = u.getChild("roles");
        if (gs != null) {
            user.getSystemRoleIDs().clear();
            user.getExternalRoleIDs().clear();
            List<Element> groupList = (List<Element>) gs.getChildren("role");
            for (Element group : groupList) {
                String groupName = group.getAttributeValue("name");
                if (hasAdminPermission || currentUser.isUserInRole(groupName)) {
                    user.assignRole(groupName);
                } else {
                    LOGGER.warn("Current user " + currentUser.getUserID() + " has not the permission to add user to group " + groupName);
                }
            }
        }

        if (userExists) {
            MCRUserManager.updateUser(user);
        } else {
            MCRUserManager.createUser(user);
        }

        res.sendRedirect(res.encodeRedirectURL("MCRUserServlet?action=show&id=" + user.getUserID()));
    }

    private void updateBasicUserInfo(Element u, MCRUser user) {
        String name = u.getChildText("realName");
        if ((name != null) && (name.trim().length() == 0)) {
            name = null;
        }
        user.setRealName(name);

        String eMail = u.getChildText("eMail");
        if ((eMail != null) && (eMail.trim().length() == 0)) {
            eMail = null;
        }
        user.setEMail(eMail);

        Element attributes = u.getChild("attributes");
        if (attributes != null) {
            List<Element> attributeList = attributes.getChildren("attribute");
            user.getAttributes().clear();
            for (Element attribute : attributeList) {
                String key = attribute.getAttributeValue("name");
                String value = attribute.getAttributeValue("value");
                user.getAttributes().put(key, value);
            }
        }
    }

    /**
     * Handles MCRUserServlet?action=save&id={userID}.
     * This is called by user-editor.xml editor form to save the
     * changed user data from editor submission. Redirects to
     * show user data afterwards. 
     */
    private void changePassword(HttpServletRequest req, HttpServletResponse res, MCRUser user, String uid) throws Exception {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        if (!checkUserIsNotNull(res, currentUser, null) || !checkUserIsNotNull(res, user, uid)) {
            return;
        }
        boolean allowed = MCRAccessManager.checkPermission(MCRUser2Constants.USER_ADMIN_PERMISSION) || currentUser.equals(user.getOwner())
            || (currentUser.equals(user) && currentUser.hasNoOwner());
        if (!allowed) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        LOGGER.info("change password of user " + user.getUserID() + " " + user.getUserName() + " " + user.getRealmID());

        MCREditorSubmission sub = (MCREditorSubmission) (req.getAttribute("MCREditorSubmission"));
        String password = sub.getXML().getRootElement().getChildText("password");
        MCRUserManager.updatePasswordHashToSHA1(user, password);
        MCRUserManager.updateUser(user);

        res.sendRedirect(res.encodeRedirectURL("MCRUserServlet?action=show&XSL.step=changedPassword&id=" + user.getUserID()));
    }

    /**
     * Handles MCRUserServlet?action=delete&id={userID}.
     * Deletes the user. 
     * Outputs user data of the deleted user using user.xsl afterwards.
     */
    private void deleteUser(HttpServletRequest req, HttpServletResponse res, MCRUser user) throws Exception {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        boolean allowed = MCRAccessManager.checkPermission(MCRUser2Constants.USER_ADMIN_PERMISSION) || currentUser.equals(user.getOwner());
        if (!allowed) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        LOGGER.info("delete user " + user.getUserID() + " " + user.getUserName() + " " + user.getRealmID());
        MCRUserManager.deleteUser(user);
        getLayoutService().doLayout(req, res, getContent(user));
    }

    private MCRJAXBContent<MCRUser> getContent(MCRUser user) {
        return new MCRJAXBContent<MCRUser>(JAXB_CONTEXT, user.getSafeCopy());
    }

    /**
     * Handles MCRUserServlet?search={pattern}, which is an optional parameter.
     * Searches for users matching the pattern in user name or real name and outputs
     * the list of results using users.xsl. The search pattern may contain * and ?
     * wildcard characters. The property MCR.user2.Users.MaxResults (default 100) specifies
     * the maximum number of users to return. When there are more hits, just the
     * number of results is returned.
     * 
     * When current user is not admin, the search pattern will be ignored and only all 
     * the users the current user is owner of will be listed.
     */
    private void listUsers(HttpServletRequest req, HttpServletResponse res) throws Exception {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        boolean hasAdminPermission = MCRAccessManager.checkPermission(MCRUser2Constants.USER_ADMIN_PERMISSION);
        boolean allowed = hasAdminPermission || MCRAccessManager.checkPermission(MCRUser2Constants.USER_CREATE_PERMISSION);
        if (!allowed) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Element users = new Element("users");

        List<MCRUser> results = null;
        if (hasAdminPermission) {
            String search = req.getParameter("search");
            if ((search == null) || search.trim().length() == 0)
                search = null;

            if (search != null) {
                users.setAttribute("search", search);
                search = "*" + search + "*";
            }

            LOGGER.info("search users like " + search);

            int max = MCRConfiguration.instance().getInt(MCRUser2Constants.CONFIG_PREFIX + "Users.MaxResults", 100);
            int num = MCRUserManager.countUsers(search, null, search);

            if ((num < max) && (num > 0))
                results = MCRUserManager.listUsers(search, null, search);
            users.setAttribute("num", String.valueOf(num));
            users.setAttribute("max", String.valueOf(max));
        } else {
            LOGGER.info("list owned users of " + currentUser.getUserName() + " " + currentUser.getRealmID());
            results = MCRUserManager.listUsers(currentUser);
        }

        if (results != null)
            for (MCRUser user : results) {
                Element u = MCRUserTransformer.buildBasicXML(user).detachRootElement();
                addString(u, "realName", user.getRealName());
                addString(u, "eMail", user.getEMailAddress());
                users.addContent(u);
            }

        getLayoutService().doLayout(req, res, new MCRJDOMContent(users));
    }

    private void addString(Element parent, String name, String value) {
        if ((value != null) && (value.trim().length() > 0))
            parent.addContent(new Element(name).setText(value.trim()));
    }
}
