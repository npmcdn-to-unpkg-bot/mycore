/*
 * $Id$
 * $Revision: 5697 $ $Date: 07.09.2011 $
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

package org.mycore.mods;

import java.util.HashSet;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Eventhandler for linking MODS_OBJECTTYPE document to MyCoRe classifications.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRMODSLinksEventHandler extends MCREventHandlerBase {

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectCreated(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectCreated(final MCREvent evt, final MCRObject obj) {
        if (!getSupportedObjectType().equals(obj.getId().getTypeId())) {
            return;
        }
        final Element metadata = obj.getMetadata().createXML();
        final HashSet<MCRCategoryID> categories = new HashSet<MCRCategoryID>();
        try {
            final XPath categoryPath = XPath.newInstance(".//*[@authority or @authorityURI]");
            @SuppressWarnings("unchecked")
            final List<Element> nodes = categoryPath.selectNodes(metadata);
            for (Element node : nodes) {
                final MCRCategoryID categoryID = MCRMODSClassificationSupport.getCategoryID(node);
                if (categoryID != null) {
                    categories.add(categoryID);
                }
            }
        } catch (final JDOMException e) {
            throw new MCRException(e);
        }
        if (!categories.isEmpty()) {
            final MCRCategLinkReference objectReference = new MCRCategLinkReference(obj.getId());
            MCRCategLinkServiceFactory.getInstance().setLinks(objectReference, categories);
        }
    }

    protected String getSupportedObjectType() {
        return "mods";
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectUpdated(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectUpdated(final MCREvent evt, final MCRObject obj) {
        handleObjectCreated(evt, obj);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectRepaired(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectRepaired(final MCREvent evt, final MCRObject obj) {
        handleObjectCreated(evt, obj);
    }

}
