/*
 * $Id$
 * $Revision: 5697 $ $Date: 27.04.2010 $
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

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.mets.tools.METSGenerator;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRMETSServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRMETSServlet.class);

    private static int CACHE_TIME;

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRServlet#init()
     */
    @Override
    public void init() throws ServletException {
        super.init();
        String cacheParam = getInitParameter("cacheTime");
        CACHE_TIME = cacheParam != null ? Integer.parseInt(cacheParam) : (60 * 60 * 24);//default is one day
    }

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        LOGGER.info(request.getPathInfo());

        String derivate = getOwnerID(request.getPathInfo());
        MCRDirectory dir = MCRDirectory.getRootDirectory(derivate);
        if (dir == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, MessageFormat.format("Derivate {0} does not exist.", derivate));
            return;
        }
        MCRFilesystemNode metsFile = dir.getChildByPath("mets.xml");
        request.setAttribute("XSL.derivateID", derivate);
        request.setAttribute("XSL.objectID", MCRLinkTableManager.instance().getSourceOf(derivate).iterator().next());

        long lastModified = dir.getLastModified().getTimeInMillis();
        writeHeaders(response, lastModified);

        if (metsFile != null && useExistingMets(request)) {
            MCRLayoutService.instance().doLayout(request, response, ((MCRFile) metsFile).getContentAsInputStream());
        } else {
            HashSet<MCRFilesystemNode> ignoreNodes = new HashSet<MCRFilesystemNode>();
            if (metsFile != null)
                ignoreNodes.add(metsFile);
            Document mets = METSGenerator.getMETS(dir, ignoreNodes);
            MCRLayoutService.instance().doLayout(request, response, mets);
        }
    }

    private boolean useExistingMets(HttpServletRequest request) {
        String useExistingMetsParam = request.getParameter("useExistingMets");
        if (useExistingMetsParam == null)
            return true;
        return Boolean.valueOf(useExistingMetsParam);
    }

    private void writeHeaders(HttpServletResponse response, long lastModified) {
        response.setHeader("Cache-Control", "max-age=" + CACHE_TIME);
        response.setContentType("image/jpeg");
        response.setDateHeader("Last-Modified", lastModified);
        Date expires = new Date(System.currentTimeMillis() + CACHE_TIME * 1000);
        LOGGER.info("Last-Modified: " + new Date(lastModified) + ", expire on: " + expires);
        response.setDateHeader("Expires", expires.getTime());
    }

    protected static String getOwnerID(String pathInfo) {
        StringBuilder ownerID = new StringBuilder(pathInfo.length());
        boolean running = true;
        for (int i = (pathInfo.charAt(0) == '/') ? 1 : 0; (i < pathInfo.length() && running); i++) {
            switch (pathInfo.charAt(i)) {
            case '/':
                running = false;
                break;
            default:
                ownerID.append(pathInfo.charAt(i));
                break;
            }
        }
        return ownerID.toString();
    }

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRServlet#getLastModified(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected long getLastModified(HttpServletRequest request) {
        String ownerID = getOwnerID(request.getPathInfo());
        MCRSession session = MCRSessionMgr.getCurrentSession();
        try {
            session.beginTransaction();
            MCRDirectory rootNode = MCRDirectory.getRootDirectory(ownerID);
            if (rootNode != null)
                return rootNode.getLastModified().getTimeInMillis();
            return -1l;
        } finally {
            session.commitTransaction();
            MCRSessionMgr.releaseCurrentSession();
            session.close(); //just created session for db transaction
        }
    }

}
