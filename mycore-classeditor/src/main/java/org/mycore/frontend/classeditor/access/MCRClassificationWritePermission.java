/*
 * $Id$
 * $Revision: 5697 $ $Date: Feb 20, 2013 $
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

package org.mycore.frontend.classeditor.access;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.util.Set;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.frontend.classeditor.utils.MCRCategUtils;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;

import com.sun.jersey.spi.container.ContainerRequest;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRClassificationWritePermission implements MCRResourceAccessChecker {

    private static Logger LOGGER = Logger.getLogger(MCRClassificationWritePermission.class);

    /* (non-Javadoc)
     * @see org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker#isPermitted(com.sun.jersey.spi.container.ContainerRequest)
     */
    @Override
    public boolean isPermitted(ContainerRequest request) {
        String value = request.getEntity(String.class);
        Set<MCRCategoryID> categories = MCRCategUtils.getRootCategoryIDs(value);
        if (categories == null) {
            LOGGER.error("Could not parse: " + value);
            return false;
        }
        for (MCRCategoryID category : categories) {
            if (!MCRAccessManager.checkPermission(category.getRootID(), PERMISSION_WRITE)) {
                LOGGER.info("Permission denied on classification: " + category);
                return false;
            }
        }
        return true;
    }
}