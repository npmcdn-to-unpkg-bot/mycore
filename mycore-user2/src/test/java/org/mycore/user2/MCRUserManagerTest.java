/*
 * $Id$
 * $Revision: 5697 $ $Date: 03.02.2012 $
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

package org.mycore.user2;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.hibernate.cfg.Configuration;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRHibTestCase;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImplTest;
import org.mycore.user2.utils.MCRUserTransformer;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRUserManagerTest extends MCRHibTestCase {
    MCRUser user;

    @BeforeClass
    public static void addMapping() {
        Configuration configuration = MCRHIBConnection.instance().getConfiguration();
        configuration.addResource("org/mycore/user2/MCRUser.hbm.xml");
        configuration.setProperty("show_sql", "true");
        MCRHIBConnection.instance().buildSessionFactory(configuration);
        SESSION_FACTORY = MCRHIBConnection.instance().getSessionFactory();
    }

    /* (non-Javadoc)
     * @see org.mycore.common.MCRHibTestCase#setUp()
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        MCRCategory groupsCategory = MCRCategoryDAOImplTest.loadClassificationResource("/mcr-groups.xml");
        MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();
        DAO.addCategory(null, groupsCategory);
        user = new MCRUser("junit");
        user.setRealName("Test Case");
        user.setPassword("test");
        MCRUserManager.createUser(user);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.MCRHibTestCase#tearDown()
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#getUser(java.lang.String, org.mycore.user2.MCRRealm)}.
     */
    @Test
    public final void testGetUserStringMCRRealm() {
        assertNull("Should not load user.", MCRUserManager.getUser(this.user.getUserName(), ""));
        MCRUser user = MCRUserManager.getUser(this.user.getUserName(), MCRRealm.getLocalRealm());
        assertNotNull("Could not load user.", user);
        assertEquals("Password hash is not as expected", "test", user.getPassword());
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#exists(java.lang.String, org.mycore.user2.MCRRealm)}.
     */
    @Test
    public final void testExistsStringMCRRealm() {
        assertFalse("Should not find user", MCRUserManager.exists(this.user.getUserName(), ""));
        assertTrue("Could not find user", MCRUserManager.exists(this.user.getUserName(), this.user.getRealm()));
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#updateUser(org.mycore.user2.MCRUser)}.
     */
    @Test
    public final void testUpdateUser() {
        String eMail = "info@mycore.de";
        this.user.setEMail(eMail);
        MCRUserManager.updateUser(this.user);
        MCRUser user = MCRUserManager.getUser(this.user.getUserName(), this.user.getRealm());
        assertEquals("User information was not updated", eMail, user.getEMailAddress());
        assertEquals("User was created not updated", 1, MCRUserManager.countUsers(null, null, null));
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#deleteUser(java.lang.String, org.mycore.user2.MCRRealm)}.
     */
    @Test
    public final void testDeleteUserStringMCRRealm() {
        MCRUserManager.deleteUser(this.user.getUserName(), this.user.getRealm());
        assertNull("Should not find user", MCRUserManager.getUser(this.user.getUserName(), this.user.getRealm()));
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#deleteUser(org.mycore.user2.MCRUser)}.
     */
    @Test
    public final void testDeleteUserMCRUser() {
        MCRUserManager.deleteUser(user);
        assertNull("Should not find user", MCRUserManager.getUser(this.user.getUserName(), this.user.getRealm()));
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#listUsers(org.mycore.user2.MCRUser)}.
     */
    @Test
    public final void testListUsersMCRUser() {
        List<MCRUser> listUsers = MCRUserManager.listUsers(null);
        assertEquals("Should not find a user", 0, listUsers.size());
        user.setOwner(user);
        MCRUserManager.updateUser(user);
        listUsers = MCRUserManager.listUsers(user);
        assertEquals("Could not find a user", 1, listUsers.size());
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#listUsers(java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public final void testListUsersStringStringString() {
        List<MCRUser> listUsers = MCRUserManager.listUsers(null, null, "Test*");
        assertEquals("Could not find a user", 1, listUsers.size());
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#countUsers(java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public final void testCountUsers() {
        assertEquals("Could not find a user", 1, MCRUserManager.countUsers(null, null, "*Case*"));
    }

    /**
     * Test method for {@link org.mycore.user2.MCRUserManager#login(java.lang.String, java.lang.String)}.
     */
    @Test
    public final void testLogin() {
        String clearPasswd = user.getPassword();
        Date curTime = new Date();
        MCRUser user = MCRUserManager.login(this.user.getUserName(), clearPasswd);
        assertNull("Should not login user", user);
        MCRUserManager.updatePasswordHashToSHA1(this.user, clearPasswd);
        MCRUserManager.updateUser(this.user);
        user = MCRUserManager.login(this.user.getUserName(), clearPasswd);
        assertNotNull("Could not login user", user);
        assertNotNull("Hash value was not updated", user.getHashType());
        user = MCRUserManager.login(this.user.getUserName(), clearPasswd);
        assertNotNull("No date set for last login.", user.getLastLogin());
        assertTrue("Date was not updated", curTime.before(user.getLastLogin()));
    }

    @Test
    public final void toXML() throws IOException {
        MCRUserManager.updatePasswordHashToSHA1(this.user, this.user.getPassword());
        this.user.setEMail("info@mycore.de");
        this.user.setHint("JUnit Test");
        this.user.getSystemGroupIDs().add("admin");
        this.user.getSystemGroupIDs().add("editor");
        this.user.setLastLogin(new Date());
        this.user.setRealName("Test Case");
        this.user.getAttributes().put("tel", "555 4812");
        this.user.getAttributes().put("street", "Heidestraße 12");
        this.user.setOwner(this.user);
        MCRUserManager.updateUser(this.user);
        assertEquals("Too many users", 1, MCRUserManager.countUsers(null, null, null));
        assertEquals("Too many users", 1, this.user.getOwnedUsers().size());
        Element exportableXML = MCRUserTransformer.buildExportableXML(this.user);
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        xout.output(exportableXML, System.out);
    }

}
