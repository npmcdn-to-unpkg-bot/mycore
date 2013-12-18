/*
 * $Id$
 * $Revision: 5697 $ $Date: 30.11.2010 $
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

package org.mycore.common;

import org.mycore.common.config.MCRConfiguration;

/**
 * A {@link MCRUserInformation} implementation with no roles attached.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSystemUserInformation implements MCRUserInformation {

    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    private static MCRSystemUserInformation systemInstance = new MCRSystemUserInformation("SYSTEM", false);

    private static MCRSystemUserInformation guestInstance = new MCRSystemUserInformation(CONFIG.getString("MCR.Users.Guestuser.UserName",
        "guest"), false);

    private static MCRSystemUserInformation superUserInstance = new MCRSystemUserInformation(CONFIG.getString(
        "MCR.Users.Superuser.UserName", "administrator"), true);

    private boolean roleReturn;

    private String userID;

    private MCRSystemUserInformation(String userID, boolean roleReturn) {
        this.userID = userID;
        this.roleReturn = roleReturn;
    }

    /**
     * Always returns "SYSTEM" 
     */
    @Override
    public String getUserID() {
        return userID;
    }

    /**
     * Always returns <em>false</em>
     */
    @Override
    public boolean isUserInRole(String role) {
        return roleReturn;
    }

    /**
     * @return the systemInstance
     */
    public static MCRSystemUserInformation getSystemUserInstance() {
        return systemInstance;
    }

    @Override
    public String getUserAttribute(String attribute) {
        return null;
    }

    /**
     * @return the guestInstance
     */
    public static MCRSystemUserInformation getGuestInstance() {
        return guestInstance;
    }

    /**
     * @return the superUserInstance
     */
    public static MCRSystemUserInformation getSuperUserInstance() {
        return superUserInstance;
    }

}
