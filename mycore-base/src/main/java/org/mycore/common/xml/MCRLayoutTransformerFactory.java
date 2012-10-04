/*
 * $Id$
 * $Revision: 5697 $ $Date: 02.10.2012 $
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

package org.mycore.common.xml;

import java.text.MessageFormat;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.content.transformer.MCRFopper;
import org.mycore.common.content.transformer.MCRIdentityTransformer;
import org.mycore.common.content.transformer.MCRTransformerPipe;
import org.mycore.common.content.transformer.MCRXSLTransformer;

/**
 * This class acts as a {@link MCRContentTransformer} factory for {@link MCRLayoutService}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRLayoutTransformerFactory {
    /** Map of transformer instances by ID */
    private static HashMap<String, MCRContentTransformer> transformers = new HashMap<String, MCRContentTransformer>();

    private static Logger LOGGER = Logger.getLogger(MCRLayoutTransformerFactory.class);

    private static MCRFopper fopper = new MCRFopper();

    private static final MCRIdentityTransformer NOOP_TRANSFORMER = new MCRIdentityTransformer("text/xml", "xml");

    /** 
     * Returns the transformer with the given ID. If the transformer is not instantiated yet,
     * it is created and initialized.
     */
    public static MCRContentTransformer getTransformer(String id) {
        MCRContentTransformer transformer = transformers.get(id);
        if (transformer != null) {
            return transformer;
        }
        //try to get configured transformer
        transformer = MCRContentTransformerFactory.getTransformer(id.replaceAll("-default$", ""));
        if (transformer != null) {
            transformers.put(id, transformer);
            return transformer;
        }
        return buildLayoutTransformer(id);
    }

    private static MCRContentTransformer buildLayoutTransformer(String id) {
        String idStripped = id.replaceAll("-default$", "");
        LOGGER.info("Configure property MCR.ContentTransformer." + idStripped + ".Class if you do not want to use default behaviour.");
        String stylesheet = getResourceName(id);
        if (stylesheet == null) {
            LOGGER.info("Using noop transformer for " + idStripped);
            return NOOP_TRANSFORMER;
        }
        MCRContentTransformer transformer = new MCRXSLTransformer(stylesheet);
        if ("application/pdf".equals(transformer.getMimeType())) {
            transformer = new MCRTransformerPipe(transformer, fopper);
            LOGGER.info("Using styleshet '" + stylesheet + "' for " + idStripped + " and MCRFopper for PDF output.");
        } else {
            LOGGER.info("Using styleshet '" + stylesheet + "' for " + idStripped);
        }
        transformers.put(id, transformer);
        return transformer;
    }

    /**
     * Builds the filename of the stylesheet to use, e. g. "playlist-simple.xsl"
     */
    private static String buildStylesheetName(String id) {
        return MessageFormat.format("xsl/{0}.xsl", id.replaceAll("-default$", ""));
    }

    private static String getResourceName(String id) {
        LOGGER.debug("MCRLayoutService using style " + id);

        String styleName = buildStylesheetName(id);
        try {
            if (MCRXMLResource.instance().exists(styleName, MCRLayoutTransformerFactory.class.getClassLoader())) {
                return styleName;
            }
        } catch (Exception e) {
            throw new MCRException("Error while loading stylesheet: " + styleName, e);
        }

        // If no stylesheet exists, forward raw xml instead
        // You can transform raw xml code by providing a stylesheed named
        // [doctype]-xml.xsl now
        if (id.endsWith("-xml") || id.endsWith("-default")) {
            LOGGER.warn("XSL stylesheet not found: " + styleName);
            return null;
        }
        throw new MCRException("XSL stylesheet not found: " + styleName);
    }

}
