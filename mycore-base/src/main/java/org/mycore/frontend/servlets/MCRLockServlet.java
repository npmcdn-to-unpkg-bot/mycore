/*
 * $Id$
 * $Revision: 5697 $ $Date: 13.07.2012 $
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

import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.support.MCRObjectIDLockTable;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRLockServlet extends MCRServlet {
    private static final String OBJECT_ID_KEY = MCRLockServlet.class.getCanonicalName() + ".MCRObjectID";

    private static final String ACTION_KEY = MCRLockServlet.class.getCanonicalName() + ".Action";

    enum Action {
        lock, unlock
    }

    private static final Logger LOGGER = Logger.getLogger(MCRLockServlet.class);

    private static final String PARAM_ACTION = "action";

    private static final String PARAM_OBJECTID = "id";

    private static final String PARAM_REDIRECT = "url";

    @Override
    protected void think(MCRServletJob job) throws Exception {
        if (MCRSessionMgr.getCurrentSession().getUserInformation().equals(MCRSystemUserInformation.getGuestInstance())) {
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String urlValue = getProperty(job.getRequest(), PARAM_REDIRECT);
        if (urlValue == null) {
            LOGGER.debug("Redirect URL is undefined, trying referrer.");
            URL referer = getReferer(job.getRequest());
            urlValue = referer == null ? null : referer.toString();
        }
        if (urlValue == null) {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST,
                "You must provide parameter: " + PARAM_REDIRECT);
            return;
        }
        String actionValue = getProperty(job.getRequest(), PARAM_ACTION);
        String idValue = getProperty(job.getRequest(), PARAM_OBJECTID);
        if (idValue == null) {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST,
                "You must provide parameter: " + PARAM_OBJECTID);
            return;
        }
        Action action = null;
        try {
            action = actionValue != null ? Action.valueOf(actionValue) : Action.lock;
        } catch (IllegalArgumentException e) {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Unsupported value for parameter " + PARAM_ACTION + ": " + actionValue);
            return;
        }
        MCRObjectID objectID = MCRObjectID.getInstance(idValue);
        switch (action) {
            case lock:
                MCRObjectIDLockTable.lock(objectID);
                break;

            case unlock:
                MCRObjectIDLockTable.unlock(objectID);
                break;
        }
        job.getRequest().setAttribute(OBJECT_ID_KEY, objectID);
        job.getRequest().setAttribute(ACTION_KEY, action);
    }

    @Override
    protected void render(MCRServletJob job, Exception ex) throws Exception {
        if (job.getResponse().isCommitted()) {
            LOGGER.info("Response allready committed");
            return;
        }
        if (ex != null) {
            throw ex;
        }
        HttpServletRequest req = job.getRequest();
        MCRObjectID objectId = (MCRObjectID) job.getRequest().getAttribute(OBJECT_ID_KEY);
        Action action = (Action) job.getRequest().getAttribute(ACTION_KEY);
        MCRSession lockingSession = MCRObjectIDLockTable.getLocker(objectId);
        if (MCRObjectIDLockTable.isLockedByCurrentSession(objectId) || action == Action.unlock) {
            String url = getProperty(job.getRequest(), PARAM_REDIRECT);
            if (url.startsWith("/")) {
                url = req.getContextPath() + url;
            }
            url = addQueryParameter(url, req);
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(url));
        } else {
            String errorI18N = getErrorI18N("error", "lockedBy", objectId.toString(), lockingSession
                .getUserInformation().getUserID());
            job.getResponse().sendError(HttpServletResponse.SC_CONFLICT, errorI18N);
        }
    }

    private String addQueryParameter(String url, HttpServletRequest req) {
        boolean hasQueryParameter = url.indexOf('?') != -1;
        StringBuilder sb = new StringBuilder(url);
        Set<Map.Entry<String, String[]>> entrySet = req.getParameterMap().entrySet();
        for (Map.Entry<String, String[]> parameter : entrySet) {
            if (!(parameter.getKey().equals(PARAM_REDIRECT) || parameter.getKey().equals(PARAM_ACTION) || url
                .contains(parameter.getKey() + "="))) {
                for (String value : parameter.getValue()) {
                    if (hasQueryParameter) {
                        sb.append('&');
                    } else {
                        sb.append('?');
                        hasQueryParameter = true;
                    }
                    sb.append(parameter.getKey());
                    sb.append("=");
                    sb.append(value);
                }
            }
        }
        return sb.toString();
    }
}
