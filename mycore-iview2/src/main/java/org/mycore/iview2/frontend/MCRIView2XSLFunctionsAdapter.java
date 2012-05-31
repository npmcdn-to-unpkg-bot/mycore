/*
 * $Id$
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

package org.mycore.iview2.frontend;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.iview2.services.MCRIView2Tools;

/**
 * Adapter that can be extended to work with different internal files systems.
 * To get the extending class invoked, one need to define a MyCoRe property, which defaults to:
 * <code>MCR.Module-iview2.MCRIView2XSLFunctionsAdapter=org.mycore.iview2.frontend.MCRIView2XSLFunctionsAdapter</code>
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRIView2XSLFunctionsAdapter {
    private static Logger LOGGER = Logger.getLogger(MCRIView2XSLFunctionsAdapter.class);

    public static MCRIView2XSLFunctionsAdapter getInstance() {
        return (MCRIView2XSLFunctionsAdapter) MCRConfiguration.instance().getInstanceOf(
                MCRIView2Tools.CONFIG_PREFIX + "MCRIView2XSLFunctionsAdapter", MCRIView2XSLFunctionsAdapter.class.getName());
    }

    public boolean hasMETSFile(String derivateID) {
        return (MCRIView2Tools.getMCRFile(derivateID, "/mets.xml") != null);
    }

    public String getSupportedMainFile(String derivateID) {
        return MCRIView2Tools.getSupportedMainFile(derivateID);
    }

    public String getOptions(String derivateID, String extensions) {
        MCRConfiguration config = MCRConfiguration.instance();
        StringBuilder options = new StringBuilder();
        options.append('{');
        options.append("\"derivateId\":").append('\"').append(derivateID).append("\",");
        options.append("\"webappBaseUri\":").append('\"').append(MCRServlet.getBaseURL()).append("\",");
        String baseUris = config.getString("MCR.Module-iview2.BaseURL", "");
        if (baseUris.length() < 10) {
            baseUris = MCRServlet.getServletBaseURL() + "MCRTileServlet";
        }
        options.append("\"baseUri\":").append('\"').append(baseUris).append("\".split(\",\")");
        if (MCRAccessManager.checkPermission(derivateID, "create-pdf")) {
            options.append(",\"pdfCreatorURI\":").append('\"').append(config.getString("MCR.Module-iview2.PDFCreatorURI", "")).append("\",");
            options.append("\"pdfCreatorStyle\":").append('\"').append(config.getString("MCR.Module-iview2.PDFCreatorStyle", ""))
                    .append("\"");
        }
        
        if(extensions!=null && !extensions.equals(""))
        {
        	options.append(",");
        	options.append(extensions);
        }
        
        options.append('}');
        LOGGER.debug(options.toString());
        return options.toString();
    }
}
