/**
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
 **/

package mycore.user;

import java.io.*;
import java.util.Date;
import java.util.Vector;
import java.util.ListIterator;
import java.text.SimpleDateFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import mycore.common.*;
import mycore.xml.MCRXMLHelper;

/**
 * Instances of this class represent MyCoRe users.
 *
 * @see mycore.user.MCRUserMgr
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRUser extends MCRUserUnit
{
  /** The password of the MyCoRe user */
  private String passwd = "";

  /** Specify whether the user ID is enabled or disabled */
  private String idEnabled = "";

  /** Specify whether the user is allowed to update the user object */
  private String updateAllowed = "";

  /** The list of groups this MyCoRe user is a member of */
  private Vector groups = null;

  /** The primary group of the user */
  private String primaryGroup = "";

  /** Object representing user address information */
  private MCRUserAddress userAddress;

  /**
   * Default constructor. It is used to create a user object with empty fields. This is
   * useful for constructing an XML representation of a user without specialized data.
   * This empty user object will not be created in the persistent data store.
   */
  public MCRUser() throws Exception
  {
    this("", "", "", "", "", null, "", "", "", "", "", "", "",
         "", "", "", "", "", "", "", "", "", "", null, false);
  }

  /**
   * Constructors. Create a user object from an XML string or from a DOM element which must
   * be passed as a parameter.
   *
   * @param userXML      XML string containing all neccessary information to create a user object.
   * @param bCreateInMgr boolean value which specifies whether the MCRUserMgr must be notified
   *                     about the creation of this object
   */
  public MCRUser(String userXML) throws Exception
  { this(userXML, true); }

  public MCRUser(Element domUser) throws Exception
  { create(domUser, true); }

  public MCRUser(String userXML, boolean bCreateInMgr) throws Exception
  {
    MCRArgumentChecker.ensureNotNull(userXML, "userXML");
    super.bCreateInMgr = bCreateInMgr;

    // Parse the XML-string of the user and get a DOM representation
    Document mcrDocument = MCRXMLHelper.parseXML(userXML);
    NodeList domUserList = mcrDocument.getElementsByTagName("user");
    Element domUser = (Element)domUserList.item(0);

    create(domUser, bCreateInMgr);
  }

  /**
   * This constructor takes all attributes of this class as single variables. The only
   * attribute not in the lsit is the date of the last changes, which will be set
   * inside of the constructor.
   *
   * @param ID             the user ID
   * @param passwd         password of the user
   * @param idEnabled      specifies whether the account is disabled or enabled, must be "true" or "false"
   * @param updateAllowed  specifies whether the user may update his data, must be "true" or "false"
   * @param creator        the user ID who creates this user
   * @param creationDate   date of the creation of this user, if null the current date will be used
   * @param description    description of the user
   * @param salutation     how to address the user, e.g. Mr. or Prof. Dr. ...
   * @param firstname      the first name (or given name) of the user
   * @param lastname       the last name (or surname) of the user
   * @param street         address information
   * @param city           address information
   * @param postalcode     address information
   * @param country        address information
   * @param institution    address information
   * @param faculty        address information
   * @param department     address information
   * @param institute      address information
   * @param telephone      telephone number
   * @param fax            fax number
   * @param email          email address
   * @param cellphone      number of cellular phone, if available
   * @param primaryGroup   the primary group of the user
   * @param groups         a Vector of groups the user belongs to
   * @param bCreateInMgr   boolean value which specifies whether the MCRUserMgr must be notified
   *                       about the creation of this object
   */
  public MCRUser(String ID, String passwd, String idEnabled, String updateAllowed,
                 String creator, Date creationDate, String description,
                 String salutation, String firstname, String lastname, String street,
                 String city, String postalcode, String country, String institution,
                 String faculty, String department, String institute, String telephone,
                 String fax, String email, String cellphone, String primaryGroup,
                 Vector groups, boolean bCreateInMgr) throws MCRException, Exception
  {
    super.ID = trim(ID);
    super.creator = trim(creator);
    super.bCreateInMgr = bCreateInMgr;

    // check if the creation date is provided. If not, use current date
    if (creationDate == null)
      super.creationDate = new Date();
    else
      super.creationDate = creationDate;

    // use the update method to populate the remaining attributes
    update(passwd, idEnabled, updateAllowed, description, salutation, firstname,
           lastname, street, city, postalcode, country, institution, faculty,
           department, institute, telephone, fax, email, cellphone, primaryGroup,
           groups, false);

    // Notify the User Manager and create a new user object in the datastore, but
    // only if this object is valid, i.e. all required fields are provided
    if (bCreateInMgr) {
      if (isValid())
        MCRUserMgr.instance().createUser(this);
      else
        throw new MCRException("User object "+ID+" is not valid! Attributes are missing.");
    }
  }

  /**
   * This method creates a user object from a DOM element which must be passed as a parameter.
   *
   * @param domUser      DOM element containing all neccessary information to create a user object.
   * @param bCreateInMgr boolean value which specifies whether the MCRUserMgr must be notified
   *                     about the creation of this object
   */
  private final void create(Element domUser, boolean bCreateInMgr) throws MCRException, Exception
  {
    // In a later stage of the software development we expect that users may have
    // more than one account. Therefore we use the following NodeList even though
    // we only allow one <account> element in the XML file at this time. The same
    // holds true for the user address part <address>.

    NodeList accountList = domUser.getElementsByTagName("account");
    NodeList accountElements = accountList.item(0).getChildNodes();

    ID            = trim(MCRXMLHelper.getElementText("userID", accountElements));
    passwd        = trim(MCRXMLHelper.getElementText("password", accountElements));
    idEnabled     = trim(MCRXMLHelper.getElementText("id_enabled", accountElements));
    updateAllowed = trim(MCRXMLHelper.getElementText("update_allowed", accountElements));
    creator       = trim(MCRXMLHelper.getElementText("creator", accountElements));
    description   = trim(MCRXMLHelper.getElementText("description", accountElements));

    // extract date information
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
    String date = trim(MCRXMLHelper.getElementText("creationdate", accountElements));
    if (date.equals(""))
      creationDate = new Date();
    else
      creationDate = sdf.parse(date);

    date = trim(MCRXMLHelper.getElementText("last_changes", accountElements));
    if (date.equals(""))
      lastChanges = creationDate;
    else
      lastChanges = sdf.parse(date);

    // extract address information
    NodeList addressList = domUser.getElementsByTagName("address");
    NodeList addressElements = addressList.item(0).getChildNodes();
    userAddress = new MCRUserAddress(addressElements);

    // Now we extract the group-information from the DOM Element and fill the
    // Vector "groups". So the user knows which groups he or she is a member of.

    NodeList groupList = domUser.getElementsByTagName("groups");
    NodeList groupElements = groupList.item(0).getChildNodes();
    primaryGroup = trim(MCRXMLHelper.getElementText("primary_group", groupElements));
    groups = new Vector(MCRXMLHelper.getAllElementTexts("group", groupElements));
    if (!groups.contains(primaryGroup))
      groups.add(primaryGroup);

    // Notify the User Manager and create a new user object in the datastore, but
    // only if this object is valis, i.e. all required fields are provided
    if (bCreateInMgr) {
      if (isValid())
        MCRUserMgr.instance().createUser(this);
      else
        throw new MCRException("User object "+ID+" is not valid! Attributes are missing.");
    }
  }

  /**
   * returns the address information of the user. Only useful for a nice listing
   * of the address information.
   *
   * @return returns the address information of the user in a table formatted
   *         output, all in one string
   */
  public String getAddress()
  { return userAddress.toString(); }

  /**
   * returns the address information of the user. All attributes of the address
   * information are separated by a separator string, which must be provided as
   * a parameter. This is useful if you want e.g. a comma-separated list of the
   * attributes.
   *
   * @param separator separator sequence for the address attributes, e.g. a comma
   * @return address information of the user, all attributes separated by the
   *         separator sequence
   */
  public String getAddress(String separator)
  { return userAddress.toString(separator); }

  /**
   * This method returns the user information as a formatted string.
   * The password will not be returned.
   *
   * @return user information, all in one string
   */
  public String getFormattedInfo() throws Exception
  {
    StringBuffer sb = new StringBuffer();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");

    sb.append("user ID        : ").append(ID).append("\n");
    sb.append("user enabled   : ").append(idEnabled).append("\n");
    sb.append("update allowed : ").append(updateAllowed).append("\n");
    sb.append("creator        : ").append(creator).append("\n");
    sb.append("creation date  : ").append(sdf.format(creationDate)).append("\n");
    sb.append("last changes   : ").append(sdf.format(lastChanges)).append("\n");
    sb.append("description    : ").append(description).append("\n");
    sb.append(getAddress()).append("\n");
    sb.append("groups         : ");

    for (int i=0; i<groups.size(); i++) {
      sb.append(groups.elementAt(i)).append(",");
    }
    sb.append("\n");
    sb.append("primary group  : ").append(primaryGroup).append("\n");
    return sb.toString();
  }
  /**
   * returns the group list of the user as a Vector of strings
   * @return returns the group list of the user as a Vector of strings
   */
  public Vector getGroups()
  { return groups; }

  /**
   * returns the password of the user
   * @return returns the password of the user
   */
  public String getPassword()
  { return passwd; }

  /**
   * returns the primary group of the user
   * @return returns the primary group of the user
   */
  public String getPrimaryGroup()
  { return primaryGroup; }

  /**
   * checks if the user has a specific privilege. To do this all groups of the user
   * are checked if the given privilege is in their list of privileges.
   *
   * @return returns true if the given privilege is in one of the list of privileges
   *         of one of the groups
   */
  public boolean hasPrivilege(String privilege) throws Exception
  {
    ListIterator groupIter = groups.listIterator();
    while (groupIter.hasNext()) {
      MCRGroup currentGroup = MCRUserMgr.instance().retrieveGroup((String)groupIter.next());
      if (currentGroup.hasPrivilege(privilege))
        return true;
    }
    return false;
  }

  /**
   * checks if the user is member of the administrators group
   * @return returns true if the user is member of the administrators group
   */
  public boolean isAdmin()
  {
    if (groups.contains("administrators"))
      return true;
    else return false;
  }

  /**
   * checks if the user is enabled and may login
   * @return returns true if the user is enabled and may login
   */
  public boolean isEnabled()
  {
    if (idEnabled.equals("true"))
      return true;
    else return false;
  }

  /**
   * checks if the user may update his or her own data
   * @return returns true if the user may update his or her data
   */
  public boolean isUpdateAllowed()
  {
    if (updateAllowed.equals("true"))
      return true;
    else return false;
  }

  /**
   * checks if all required fields have been provided. In a later stage of the
   * software development a User Policy object will be asked, which fields exactly
   * are the required fields. This will be configurable.
   *
   * @return returns true if all required fields have been provided
   */
  public boolean isValid() throws Exception
  {
    Vector requiredUserAttributes = MCRUserPolicy.instance().getRequiredUserAttributes();
    boolean test = true;

    if (requiredUserAttributes.contains("userID"))
      test = test && super.ID.length() > 0;
    if (requiredUserAttributes.contains("password"))
      test = test && this.passwd.length() > 0;
    if (requiredUserAttributes.contains("id_enabled"))
      test = test && this.idEnabled.length() > 0;
    if (requiredUserAttributes.contains("update_allowed"))
      test = test && this.updateAllowed.length() > 0;
    if (requiredUserAttributes.contains("creator"))
      test = test && super.creator.length() > 0;
    if (requiredUserAttributes.contains("description"))
      test = test && super.description.length() > 0;
    if (requiredUserAttributes.contains("salutation"))
      test = test && userAddress.getSalutation().length() > 0;
    if (requiredUserAttributes.contains("firstname"))
      test = test && userAddress.getFirstName().length() > 0;
    if (requiredUserAttributes.contains("lastname"))
      test = test && userAddress.getLastName().length() > 0;
    if (requiredUserAttributes.contains("street"))
      test = test && userAddress.getStreet().length() > 0;
    if (requiredUserAttributes.contains("city"))
      test = test && userAddress.getCity().length() > 0;
    if (requiredUserAttributes.contains("postalcode"))
      test = test && userAddress.getPostalCode().length() > 0;
    if (requiredUserAttributes.contains("country"))
      test = test && userAddress.getCountry().length() > 0;
    if (requiredUserAttributes.contains("institution"))
      test = test && userAddress.getInstitution().length() > 0;
    if (requiredUserAttributes.contains("faculty"))
      test = test && userAddress.getFaculty().length() > 0;
    if (requiredUserAttributes.contains("department"))
      test = test && userAddress.getDepartment().length() > 0;
    if (requiredUserAttributes.contains("institute"))
      test = test && userAddress.getInstitute().length() > 0;
    if (requiredUserAttributes.contains("telephone"))
      test = test && userAddress.getTelephone().length() > 0;
    if (requiredUserAttributes.contains("fax"))
      test = test && userAddress.getFax().length() > 0;
    if (requiredUserAttributes.contains("email"))
      test = test && userAddress.getEmail().length() > 0;
    if (requiredUserAttributes.contains("cellphone"))
      test = test && userAddress.getCellphone().length() > 0;
    if (requiredUserAttributes.contains("primary_group"))
      test = test && this.primaryGroup.length() >0;

    return test;
  }

  /**
   * sets the password of the user
   * @param newPassword the new password of the user
   */
  public boolean setPassword(String newPassword) throws Exception
  {
    // We do not allow empty passwords. Later we might check if the password is
    // conform with a password policy.
    if (newPassword.length() != 0) {
      passwd = newPassword;
      super.lastChanges = new Date();
      MCRUserMgr.instance().updateUser(this);
      return true;
    }
    else
      return false;
  }

  /**
   * returns the user object as an xml representation
   *
   * @param NL separation sequence. Typically this will be an empty string (if the XML
   *        representation is needed as one line) or a newline ("\n") sequence.
   * @return returns the user object as an xml representation
   */
  public String toXML(String NL) throws Exception
  {
    Vector requiredUserAttributes = MCRUserPolicy.instance().getRequiredUserAttributes();

    // At first we create an XML representation of the group list. This will be
    // used further down.

    StringBuffer groupBuf = new StringBuffer();
    groupBuf.append("<groups>").append(NL);

    if (requiredUserAttributes.contains("primary_group"))
      groupBuf.append("<primary_group required=\"true\">");
    else
      groupBuf.append("<primary_group>");
    groupBuf.append(primaryGroup).append("</primary_group>").append(NL);

    for (int i=0; i<groups.size(); i++) {
      // special treatment for the primary group which is already appended
      if (!groups.elementAt(i).equals(primaryGroup))
        groupBuf.append("<group>").append(groups.elementAt(i)).append("</group>").append(NL);
    }
    groupBuf.append("</groups>");

    // Now we put together all information of the user
    StringBuffer ub = new StringBuffer();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");

    ub.append("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>").append(NL)
      .append("<mycore_user_and_group_info type=\"user\">").append(NL)
      .append("<user>").append(NL)
      .append("<account>").append(NL);

    if (requiredUserAttributes.contains("userID"))
      ub.append("<userID required=\"true\">");
    else
      ub.append("<userID>");
    ub.append(ID).append("</userID>" ).append(NL);

    if (requiredUserAttributes.contains("password"))
      ub.append("<password required=\"true\">");
    else
      ub.append("<password>");
    ub.append(passwd).append("</password>").append(NL);

    if (requiredUserAttributes.contains("id_enabled"))
      ub.append("<id_enabled required=\"true\">");
    else
      ub.append("<id_enabled>");
    ub.append(idEnabled).append("</id_enabled>").append(NL);

    if (requiredUserAttributes.contains("update_allowed"))
      ub.append("<update_allowed required=\"true\">");
    else
      ub.append("<update_allowed>");
    ub.append(updateAllowed).append("</update_allowed>").append(NL);

    if (requiredUserAttributes.contains("creator"))
      ub.append("<creator required=\"true\">");
    else
      ub.append("<creator>");
    ub.append(creator).append("</creator>").append(NL);

    // These attributes will be filled automatically if not provided, so we must not check
    // whether they are required
    ub.append("<creationdate>").append(sdf.format(creationDate)).append("</creationdate>").append(NL);
    ub.append("<last_changes>").append(sdf.format(lastChanges)).append("</last_changes>").append(NL);

    if (requiredUserAttributes.contains("description"))
      ub.append("<description required=\"true\">");
    else
      ub.append("<description>");
    ub.append(description).append("</description>").append(NL);

    ub.append("</account>").append(NL)
      .append(userAddress.getAddressAsXmlElement(NL)).append(NL)
      .append(groupBuf).append(NL)
      .append("</user>").append(NL)
      .append("</mycore_user_and_group_info>").append(NL);
    return ub.toString();
  }

  /**
   * Updates some of the attributes of the user. Some attributes (ID, creator, creationDate,
   * lastChanges) cannot be updated. The date of the last changes will be updated inside
   * of this method.
   *
   * @param passwd         password of the user
   * @param idEnabled      specifies whether the account is disabled or enabled, must be "true" or "false"
   * @param updateAllowed  specifies whether the user may update his data, must be "true" or "false"
   * @param description    description of the user
   * @param salutation     how to address the user, e.g. Mr. or Prof. Dr. ...
   * @param firstname      the first name (or given name) of the user
   * @param lastname       the last name (or surname) of the user
   * @param street         address information
   * @param city           address information
   * @param postalcode     address information
   * @param country        address information
   * @param institution    address information
   * @param faculty        address information
   * @param department     address information
   * @param institute      address information
   * @param telephone      telephone number
   * @param fax            fax number
   * @param email          email address
   * @param cellphone      number of cellular phone, if available
   * @param primaryGroup   the primary group of the user
   * @param groups         a Vector of groups the user belongs to
   * @param bUpdateInMgr   boolean value which specifies whether the MCRUserMgr must be notified
   *                       about the update of this object
   */
  public void update(String passwd, String idEnabled, String updateAllowed, String description,
                String salutation, String firstname, String lastname, String street,
                String city, String postalcode, String country, String institution,
                String faculty, String department, String institute, String telephone,
                String fax, String email, String cellphone, String primaryGroup,
                Vector groups, boolean bUpdateInMgr) throws MCRException, Exception
  {
    super.description = trim(description);

    this.passwd = trim(passwd);
    this.idEnabled = trim(idEnabled);
    this.updateAllowed = trim(updateAllowed);
    this.primaryGroup = trim(primaryGroup);

    // the group might be null due to the default constructor
    if (groups != null)
      this.groups = groups;
    else
      this.groups = new Vector();

    // check if the primary group is in the groups list. If not, put it into the list.
    if ((primaryGroup.length() > 0) && (!groups.contains(primaryGroup)))
      groups.add(primaryGroup);

    // update the date of the last changes
    super.lastChanges = new Date();

    // create the MCRUserAddress object
    userAddress = new MCRUserAddress(salutation, firstname, lastname, street, city,
                      postalcode, country, institution, faculty, department,
                      institute, telephone, fax, email, cellphone);

    // Notify the User Manager and update the user object in the datastore, but
    // only if this object is valid.
    if (bUpdateInMgr) {
      if (isValid())
        MCRUserMgr.instance().updateUser(this);
      else
        throw new MCRException("User object "+ID+" is not valid! Attributes are missing.");
    }
  }
}
