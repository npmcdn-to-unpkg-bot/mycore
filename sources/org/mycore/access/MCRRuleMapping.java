/*
 * $RCSfile$
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
 */

package org.mycore.access;

import org.mycore.user.MCRUser;

public class MCRRuleMapping
{
    private String ruleid;
    private String objid;
    private String pool;
    private MCRUser creator;

    /**
     * objid = MCRObjectID as string
     * @return
     */
    public String getObjId() {
        return objid;
    }
    public void setObjId(String objid) {
        this.objid = objid;
    }

    /**
     * pool
     * @return
     */
    public String getPool() {
        return pool;
    }
    public void setPool(String pool) {
        this.pool = pool;
    }

    /**
     * ruleid
     * @return
     */
    public String getRuleId() {
        return ruleid;
    }
    public void setRuleId(String ruleid) {
        this.ruleid = ruleid;
    }
    
    /**
     * user
     * @return
     */
    public MCRUser getUser() {
        return creator;
    }
    public void setUser(MCRUser user) {
        this.creator = user;
    }
}
