/*
 * 
 * $Revision: 23424 $ $Date: 2012-02-02 22:53:29 +0100 (Do, 02 Feb 2012) $
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

import static org.mycore.common.MCRConstants.DEFAULT_ENCODING;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.user2.utils.MCRUserTransformer;
import org.xml.sax.SAXParseException;

/**
 * This class provides a set of commands for the org.mycore.user2 management
 * which can be used by the command line interface.
 *
 * @author Thomas Scheffler (yagee)
 */
public class MCRUserCommands extends MCRAbstractCommands {
    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRUserCommands.class.getName());

    private static final String SYSTEM = MCRConfiguration.instance().getString("MCR.CommandLineInterface.SystemName", "MyCoRe") + ":";

    /**
     * The constructor.
     */
    public MCRUserCommands() {
        super();

        MCRCommand com = null;

        addCommand(new MCRCommand("change to user {0} with {1}", "org.mycore.user2.MCRUserCommands.changeToUser String String",
            "Change the user {0} with the given password in {1}."));
        addCommand(new MCRCommand("login {0}", "org.mycore.user2.MCRUserCommands.login String", "Start the login dialog for the user {0}."));

        com = new MCRCommand("init superuser", "org.mycore.user2.MCRUserCommands.initSuperuser",
            "Initialized the user system. This command runs only if the user database does not exist.");
        addCommand(com);

        com = new MCRCommand("encrypt passwords in user xml file {0} to file {1}", "org.mycore.user2.MCRUserCommands.encryptPasswordsInXMLFile String String",
            "This is a migration tool to change old plain text password entries to encrpted entries.");
        addCommand(com);

        com = new MCRCommand("set password for user {0} to {1}", "org.mycore.user2.MCRUserCommands.setPassword String String",
            "This command sets a new password for the user. You must be this user or you must have administrator access.");
        addCommand(com);

        com = new MCRCommand("enable user {0}", "org.mycore.user2.MCRUserCommands.enableUser String", "The command enables the user for the access.");
        addCommand(com);

        com = new MCRCommand("disable user {0}", "org.mycore.user2.MCRUserCommands.disableUser String", "The command disables the user from the access.");
        addCommand(com);

        com = new MCRCommand("delete role {0}", "org.mycore.user2.MCRUserCommands.deleteRole String",
            "The command delete the role {0} from the user system, but only if it has no user assigned.");
        addCommand(com);

        com = new MCRCommand("add roles from user file {0}", "org.mycore.user2.MCRUserCommands.addRoles String",
            "The command adds roles found in user file {0} that do not exist");
        addCommand(com);

        com = new MCRCommand("delete user {0}", "org.mycore.user2.MCRUserCommands.deleteUser String", "The command delete the user {0}.");
        addCommand(com);

        com = new MCRCommand("assign user {0} to role {1}", "org.mycore.user2.MCRUserCommands.assignUserToRole String String",
            "The command add a user {0} as secondary member in the role {1}.");
        addCommand(com);

        com = new MCRCommand("unassign user {0} from role {1}", "org.mycore.user2.MCRUserCommands.unassignUserFromRole String String",
            "The command remove the user {0} as secondary member from the role {1}.");
        addCommand(com);

        com = new MCRCommand("list all roles", "org.mycore.user2.MCRUserCommands.listAllRoles", "The command list all roles.");
        addCommand(com);

        com = new MCRCommand("list role {0}", "org.mycore.user2.MCRUserCommands.listRole String", "The command list the role {0}.");
        addCommand(com);

        com = new MCRCommand("list all users", "org.mycore.user2.MCRUserCommands.listAllUsers", "The command list all users.");
        addCommand(com);

        com = new MCRCommand("list user {0}", "org.mycore.user2.MCRUserCommands.listUser String", "The command list the user {0}.");
        addCommand(com);

        com = new MCRCommand("export user {0} to file {1}", "org.mycore.user2.MCRUserCommands.exportUserToFile String String",
            "The command exports the data of user {0} to the file {1}.");
        addCommand(com);

        com = new MCRCommand("import user from file {0}", "org.mycore.user2.MCRUserCommands.importUserFromFile String",
            "The command imports a user from file {0}.");
        addCommand(com);
        
        com = new MCRCommand("update user from file {0}", "org.mycore.user2.MCRUserCommands.updateUserFromFile String",
                "The command updates a user from file {0}.");
        addCommand(com);
    }
    

    /**
     * This command changes the user of the session context to a new user.
     * 
     * @param user
     *            the new user ID
     * @param password
     *            the password of the new user
     */
    public static void changeToUser(String user, String password) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        System.out.println(SYSTEM + " The old user ID is " + session.getUserInformation().getUserID());
        if (MCRUserManager.login(user, password) != null) {
            System.out.println(SYSTEM + " The new user ID is " + session.getUserInformation().getUserID());
        } else {
            LOGGER.warn("Wrong password, no changes of user ID in session context!");
        }
    }

    /**
     * This command changes the user of the session context to a new user.
     * 
     * @param user
     *            the new user ID
     */
    public static void login(String user) {
        char[] password = {};
        do {
            password = System.console().readPassword("{0} Enter password for user {1} :> ", SYSTEM, user);
        } while (password.length == 0);

        changeToUser(user, String.valueOf(password));
    }

    /**
     * This method initializes the user and role system an creates a superuser
     * with values set in mycore.properties.private As 'super' default, if no
     * properties were set, mcradmin with password mycore will be used.
     */
    public static List<String> initSuperuser() {
        final String suser = CONFIG.getString("MCR.Users.Superuser.UserName", "administrator");
        final String spasswd = CONFIG.getString("MCR.Users.Superuser.UserPasswd", "alleswirdgut");
        final String srole = CONFIG.getString("MCR.Users.Superuser.GroupName", "admin");

        //set to super user
        MCRSessionMgr.getCurrentSession().setUserInformation(new MCRUserInformation() {

            @Override
            public boolean isUserInRole(String role) {
                return true;
            }

            @Override
            public String getUserID() {
                return suser;
            }

            @Override
            public String getUserAttribute(String attribute) {
                return null;
            }
        });

        if (MCRUserManager.exists(suser)) {
            LOGGER.error("The superuser already exists!");
            return null;
        }

        // the superuser role
        try {
            Set<MCRLabel> labels = new HashSet<MCRLabel>();
            labels.add(new MCRLabel("en", "The superuser role", null));

            MCRRole mcrRole = new MCRRole(srole, labels);
            MCRRoleManager.addRole(mcrRole);
        } catch (Exception e) {
            throw new MCRException("Can't create the superuser role.", e);
        }

        LOGGER.info("The role " + srole + " is installed.");

        // the superuser
        try {
            MCRUser mcrUser = new MCRUser(suser);
            mcrUser.setRealName("Superuser");
            mcrUser.assignRole(srole);
            MCRUserManager.updatePasswordHashToSHA1(mcrUser, spasswd);
            MCRUserManager.createUser(mcrUser);
        } catch (Exception e) {
            throw new MCRException("Can't create the superuser.", e);
        }

        LOGGER.info(MessageFormat.format("The user {0} with password {1} is installed.", suser, spasswd));
        return Arrays.asList("change to user " + suser + " with " + spasswd);
    }

    /**
     * This method invokes {@link MCRRoleManager#deleteRole(String)} and permanently removes a
     * role from the system.
     * 
     * @param roleID
     *            the ID of the role which will be deleted
     */
    public static void deleteRole(String roleID) {
        MCRRoleManager.deleteRole(roleID);
    }

    /**
     * Loads XML from a user and looks for roles currently not present in the system and creates them.
     * 
     * @param fileName
     *            a valid user XML file
     * @throws IOException 
     * @throws SAXParseException 
     */
    public static void addRoles(String fileName) throws SAXParseException, IOException {
        LOGGER.info("Reading file " + fileName + " ...");
        Document doc = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRFileContent(fileName));
        Element user = doc.getRootElement();
        Element roles = user.getChild("roles");
        if (roles == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<Element> roleList = roles.getChildren("role");
        for (Element role : roleList) {
            String name = role.getAttributeValue("name");
            MCRRole mcrRole = MCRRoleManager.getRole(name);
            if (mcrRole == null) {
                @SuppressWarnings("unchecked")
                List<Element> labelList = role.getChildren("label");
                mcrRole = new MCRRole(name, MCRXMLTransformer.getLabels(labelList));
                MCRRoleManager.addRole(mcrRole);
            }
        }
    }

    /**
     * This method invokes MCRUserMgr.deleteUser() and permanently removes a
     * user from the system.
     * 
     * @param userID
     *            the ID of the user which will be deleted
     */
    public static void deleteUser(String userID) throws Exception {
        MCRUserManager.deleteUser(userID);
    }

    /**
     * This method invokes MCRUserMgr.enableUser() that enables a user
     * 
     * @param userID
     *            the ID of the user which will be enabled
     */
    public static void enableUser(String userID) throws Exception {
        MCRUser mcrUser = MCRUserManager.getUser(userID);
        mcrUser.enableLogin();
        MCRUserManager.updateUser(mcrUser);
    }

    /**
     * A given XML file containing user data with cleartext passwords must be
     * converted prior to loading the user data into the system. This method
     * reads all user objects in the given XML file, encrypts the passwords and
     * writes them back to a file with name original-file-name_encrypted.xml.
     * 
     * @param oldFile
     *            the filename of the user data input
     * @param newFile
     *            the filename of the user data output (encrypted passwords)
     */
    public static final void encryptPasswordsInXMLFile(String oldFile, String newFile) throws MCRException {
        if (!checkFilename(oldFile)) {
            return;
        }
        LOGGER.info("Reading file " + oldFile + " ...");

        try {
            Document doc = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRFileContent(oldFile));
            Element rootelm = doc.getRootElement();
            MCRUser mcrUser = MCRUserTransformer.buildMCRUser(rootelm);

            if (mcrUser == null) {
                throw new MCRException("These data do not correspond to a user.");
            }

            MCRUserManager.updatePasswordHashToSHA1(mcrUser, mcrUser.getPassword());

            FileOutputStream outFile = new FileOutputStream(newFile);
            saveToXMLFile(mcrUser, outFile);
        } catch (Exception e) {
            throw new MCRException("Error while encrypting cleartext passwords in user xml file: "+e.getMessage());
        }
    }

    /**
     * This method invokes MCRUserMgr.disableUser() that disables a user
     * 
     * @param userID
     *            the ID of the user which will be enabled
     */
    public static void disableUser(String userID) throws Exception {
        MCRUser mcrUser = MCRUserManager.getUser(userID);
        mcrUser.disableLogin();
        MCRUserManager.updateUser(mcrUser);
    }

    /**
     * This method invokes MCRUserMgr.getAllUserIDs() and retrieves a ArrayList
     * of all users stored in the persistent datastore.
     */
    public static void listAllUsers() throws Exception {
        List<MCRUser> users = MCRUserManager.listUsers(null, null, null);

        for (MCRUser uid : users) {
            listUser(uid);
        }
    }

    /**
     * This method invokes {@link MCRRoleManager#listSystemRoles()} and retrieves a list
     * of all roles stored in the persistent datastore.
     */
    public static void listAllRoles() throws Exception {
        List<MCRRole> roles = MCRRoleManager.listSystemRoles();

        for (MCRRole role : roles) {
            listRole(role);
        }
    }

    /**
     * This command takes a userID and file name as a parameter, retrieves the
     * user from MCRUserMgr as JDOM document and export this to the given file.
     * 
     * @param userID
     *            ID of the user to be saved
     * @param filename
     *            Name of the file to store the exported user
     */
    public static void exportUserToFile(String userID, String filename) throws MCRException {
        try {
            MCRUser user = MCRUserManager.getUser(userID);
            FileOutputStream outFile = new FileOutputStream(filename);
            LOGGER.info("Writing to file " + filename + " ...");
            saveToXMLFile(user, outFile);
        } catch (Exception e) {
            throw new MCRException("Error while command saveUserToFile()", e);
        }
    }

    /**
     * This command takes a file name as a parameter, creates the
     * MCRUser instances stores it in the database if it does not exists.
     * 
     * @param filename
     *            Name of the file to import user from
     * @throws IOException 
     * @throws SAXParseException 
     */
    public static void importUserFromFile(String filename) throws SAXParseException, IOException {
        MCRUser user = getMCRUserFromFile(filename);
        if (MCRUserManager.exists(user.getUserName(), user.getRealmID())) {
            throw new MCRException("User already exists: " + user.getUserID());
        }
        MCRUserManager.createUser(user);
    }

    /**
     * This method invokes MCRUserMgr.retrieveUser() and then works with the
     * retrieved user object to change the password.
     * 
     * @param userID
     *            the ID of the user for which the password will be set
     */
    public static void setPassword(String userID, String password) throws MCRException {
        MCRUser user = MCRUserManager.getUser(userID);
        MCRUserManager.updatePasswordHashToSHA1(user, password);
        MCRUserManager.updateUser(user);
    }

    /**
     * This method invokes {@link MCRRoleManager#getRole(String)} and then works with the
     * retrieved role object to get an XML-Representation.
     * 
     * @param roleID
     *            the ID of the role for which the XML-representation is needed
     */
    public static final void listRole(String roleID) throws MCRException {
        MCRRole role = MCRRoleManager.getRole(roleID);
        listRole(role);
    }

    public static final void listRole(MCRRole role) {
        StringBuilder sb = new StringBuilder();
        sb.append("       role=").append(role.getName());
        for (MCRLabel label : role.getLabels()) {
            sb.append("\n         ").append(label.toString());
        }
        Collection<String> userIds = MCRRoleManager.listUserIDs(role);
        for (String userId : userIds) {
            sb.append("\n          user assigned to role=").append(userId);
        }
        LOGGER.info(sb.toString());
    }

    /**
     * This method invokes MCRUserMgr.retrieveUser() and then works with the
     * retrieved user object to get an XML-Representation.
     * 
     * @param userID
     *            the ID of the user for which the XML-representation is needed
     */
    public static final void listUser(String userID) throws MCRException {
        MCRUser user = MCRUserManager.getUser(userID);
        listUser(user);
    }

    public static void listUser(MCRUser user) {
        StringBuilder sb = new StringBuilder("\n");
        sb.append("       user=")
            .append(user.getUserName())
            .append("   real name=")
            .append(user.getRealName())
            .append('\n')
            .append("   loginAllowed=")
            .append(user.loginAllowed())
            .append('\n');
        List<String> roles = new ArrayList<String>(user.getSystemRoleIDs());
        roles.addAll(user.getExternalRoleIDs());
        for (String rid : roles) {
            sb.append("          assigned to role=").append(rid).append('\n');
        }
        LOGGER.info(sb.toString());
    }

    /**
     * Check the file name
     * 
     * @param filename
     *            the filename of the user data input
     * @return true if the file name is okay
     */
    private static boolean checkFilename(String filename) {
        if (!filename.endsWith(".xml")) {
            LOGGER.warn(filename + " ignored, does not end with *.xml");

            return false;
        }

        if (!new File(filename).isFile()) {
            LOGGER.warn(filename + " ignored, is not a file.");

            return false;
        }

        return true;
    }

    /**
     * This method invokes MCRUserMgr.createUser() with data from a file.
     * 
     * @param filename
     *            the filename of the user data input
     * @throws SAXParseException 
     */
    public static final void createUserFromFile(String filename) throws SAXParseException, IOException {
        MCRUser user = getMCRUserFromFile(filename);
        MCRUserManager.createUser(user);
    }

    /**
     * This method invokes MCRUserMgr.updateUser() with data from a file.
     * 
     * @param filename
     *            the filename of the user data input
     * @throws SAXParseException if file could not be parsed
     */
    public static final void updateUserFromFile(String filename) throws SAXParseException, IOException {
        MCRUser user = getMCRUserFromFile(filename);
        MCRUserManager.updateUser(user);
    }

    private static MCRUser getMCRUserFromFile(String filename) throws SAXParseException, IOException {
        if (!checkFilename(filename)) {
            return null;
        }
        LOGGER.info("Reading file " + filename + " ...");
        Document doc = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRFileContent(filename));
        return MCRUserTransformer.buildMCRUser(doc.getRootElement());
    }

    /**
     * This method adds a user as a member to a role
     * 
     * @param userID
     *            the ID of the user which will be a member of the role
     *            represented by roleID
     * @param roleID
     *            the ID of the role to which the user with ID mbrUserID will
     *            be added
     * @throws MCRException
     */
    public static final void assignUserToRole(String userID, String roleID) throws MCRException {
        try {
            MCRUser user = MCRUserManager.getUser(userID);
            user.assignRole(roleID);
            MCRUserManager.updateUser(user);
        } catch (Exception e) {
            throw new MCRException("Error while assigning " + userID + " to role " + roleID + ".", e);
        }
    }

    /**
     * This method removes a member user from a role
     * 
     * @param userID
     *            the ID of the user which will be removed from the role
     *            represented by roleID
     * @param roleID
     *            the ID of the role from which the user with ID mbrUserID will
     *            be removed
     * @throws MCRException
     */
    public static final void unassignUserFromRole(String userID, String roleID) throws MCRException {
        try {
            MCRUser user = MCRUserManager.getUser(userID);
            user.unassignRole(roleID);
            MCRUserManager.updateUser(user);
        } catch (Exception e) {
            throw new MCRException("Error while unassigning " + userID + " from role " + roleID + ".", e);
        }
    }

    /**
     * This method just saves a JDOM document to a file
     * 
     * automatically closes {@link OutputStream}.
     * 
     * @param mcrUser
     *            the JDOM XML document to be printed
     * @param outFile
     *            a FileOutputStream object for the output
     * @throws IOException if output file can not be closed
     */
    private static void saveToXMLFile(MCRUser mcrUser, FileOutputStream outFile) throws MCRException, IOException {
        String mcr_encoding = CONFIG.getString("MCR.Metadata.DefaultEncoding", DEFAULT_ENCODING);

        // Create the output
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat().setEncoding(mcr_encoding));

        try {
            outputter.output(MCRUserTransformer.buildExportableXML(mcrUser), outFile);
        } catch (Exception e) {
            throw new MCRException("Error while save XML to file: "+e.getMessage());
        } finally {
            outFile.close();
        }
    }
}
