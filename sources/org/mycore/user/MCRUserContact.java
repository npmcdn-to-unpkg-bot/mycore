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

package org.mycore.user;

import java.io.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.MCRConfiguration;

/**
 * Instances of this class store contact information of MyCoRe users. MCRUserContact
 * is part of the MyCoRe user component and must not be used directly by other components.
 * MCRUserContact is aggregated by MCRUser and all user objects are managed by the
 * user manager (the instance of the singleton MCRUserMgr).
 *
 * @see org.mycore.user.MCRUserMgr
 * @see org.mycore.user.MCRUser
 *
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRUserContact
{
  private static Logger logger = Logger.getLogger(MCRUserContact.class.getName());
  private static MCRConfiguration config = null;

  /** The maximum length of the salutation string */
  public final static int salutation_len = 24;

  /** The maximum length of the firstname string */
  public final static int firstname_len = 64;

  /** The maximum length of the lastname string */
  public final static int lastname_len = 32;

  /** The maximum length of the street string */
  public final static int street_len = 64;

  /** The maximum length of the city string */
  public final static int city_len = 32;

  /** The maximum length of the postalcode string */
  public final static int postalcode_len = 32;

  /** The maximum length of the country string */
  public final static int country_len = 32;

  /** The maximum length of the state string */
  public final static int state_len = 32;

  /** The maximum length of the institution string */
  public final static int institution_len = 64;

  /** The maximum length of the faculty string */
  public final static int faculty_len = 64;

  /** The maximum length of the department string */
  public final static int department_len = 64;

  /** The maximum length of the institute string */
  public final static int institute_len = 64;

  /** The maximum length of the telephone string */
  public final static int telephone_len = 32;

  /** The maximum length of the fax string */
  public final static int fax_len = 32;

  /** The maximum length of the email string */
  public final static int email_len = 64;

  /** The maximum length of the cellphone string */
  public final static int cellphone_len = 32;

  private String salutation = "";
  private String firstname = "";
  private String lastname = "";
  private String street = "";
  private String city = "";
  private String postalcode = "";
  private String country = "";
  private String state = "";
  private String institution = "";
  private String faculty = "";
  private String department = "";
  private String institute = "";
  private String telephone = "";
  private String fax = "";
  private String email = "";
  private String cellphone = "";

  /**
   * Constructor for an empty object.
   */
  public MCRUserContact()
  { init(); }

  /**
   * Constructor. All address and contact attributes are passed as Strings
   */
  public MCRUserContact(String salutation, String firstname, String lastname,
                        String street, String city, String postalcode,
                        String country, String state, String institution,
                        String faculty, String department, String institute,
                        String telephone, String fax, String email,
                        String cellphone)
  {
    init();
    this.salutation  = MCRUserObject.trim(salutation,salutation_len);
    this.firstname   = MCRUserObject.trim(firstname,firstname_len);
    this.lastname    = MCRUserObject.trim(lastname,lastname_len);
    this.street      = MCRUserObject.trim(street,street_len);
    this.city        = MCRUserObject.trim(city,city_len);
    this.postalcode  = MCRUserObject.trim(postalcode,postalcode_len);
    this.country     = MCRUserObject.trim(country,country_len);
    this.state       = MCRUserObject.trim(state,state_len);
    this.institution = MCRUserObject.trim(institution,institution_len);
    this.faculty     = MCRUserObject.trim(faculty,faculty_len);
    this.department  = MCRUserObject.trim(department,department_len);
    this.institute   = MCRUserObject.trim(institute,institute_len);
    this.telephone   = MCRUserObject.trim(telephone,telephone_len);
    this.fax         = MCRUserObject.trim(fax,fax_len);
    this.email       = MCRUserObject.trim(email,email_len);
    this.cellphone   = MCRUserObject.trim(cellphone,cellphone_len);
  }

  /**
   * Constructor. All address and contact attributes are provided as a JDOM Element.
   * @param elm the JDOM Element
   */
  public MCRUserContact(org.jdom.Element elm)
  {
    init();
    setFromJDOMElement(elm);
  }

  /**
   * All address and contact attributes are passed as a JDOM Element.
   * @param elm the JDOM Element
   */
  public final void setFromJDOMElement(org.jdom.Element elm)
  {
    if (elm == null) { return; }
    this.salutation  = MCRUserObject.trim(elm.getChildTextTrim("contact.salutation"), salutation_len);
    this.firstname   = MCRUserObject.trim(elm.getChildTextTrim("contact.firstname"), firstname_len);
    this.lastname    = MCRUserObject.trim(elm.getChildTextTrim("contact.lastname"), lastname_len);
    this.street      = MCRUserObject.trim(elm.getChildTextTrim("contact.street"), street_len);
    this.city        = MCRUserObject.trim(elm.getChildTextTrim("contact.city"), city_len);
    this.postalcode  = MCRUserObject.trim(elm.getChildTextTrim("contact.postalcode"), postalcode_len);
    this.country     = MCRUserObject.trim(elm.getChildTextTrim("contact.country"), country_len);
    this.state       = MCRUserObject.trim(elm.getChildTextTrim("contact.state"), state_len);
    this.institution = MCRUserObject.trim(elm.getChildTextTrim("contact.institution"), institution_len);
    this.faculty     = MCRUserObject.trim(elm.getChildTextTrim("contact.faculty"), faculty_len);
    this.department  = MCRUserObject.trim(elm.getChildTextTrim("contact.department"), department_len);
    this.institute   = MCRUserObject.trim(elm.getChildTextTrim("contact.institute"), institute_len);
    this.telephone   = MCRUserObject.trim(elm.getChildTextTrim("contact.telephone"), telephone_len);
    this.fax         = MCRUserObject.trim(elm.getChildTextTrim("contact.fax"), fax_len);
    this.email       = MCRUserObject.trim(elm.getChildTextTrim("contact.email"), email_len);
    this.cellphone   = MCRUserObject.trim(elm.getChildTextTrim("contact.cellphone"), cellphone_len);
  }

  /**
   * This method initializes this object with empty attributes.
   */
  private final void init()
  {
    config = MCRConfiguration.instance();
    PropertyConfigurator.configure(config.getLoggingProperties());
    salutation = "";
    firstname = "";
    lastname = "";
    street = "";
    city = "";
    postalcode = "";
    country = "";
    state = "";
    institution = "";
    faculty = "";
    department = "";
    institute = "";
    telephone = "";
    fax = "";
    email = "";
    cellphone = "";
  }

  /**
   * The following methods simply return the attributes...
   */
  public final String getSalutation()
  { return salutation; }

  public final String getFirstName()
  { return firstname; }

  public final String getLastName()
  { return lastname; }

  public final String getStreet()
  { return street; }

  public final String getCity()
  { return city; }

  public final String getPostalCode()
  { return postalcode; }

  public final String getCountry()
  { return country; }

  public final String getState()
  { return state; }

  public final String getInstitution()
  { return institution; }

  public final String getFaculty()
  { return faculty; }

  public final String getDepartment()
  { return department; }

  public final String getInstitute()
  { return institute; }

  public final String getTelephone()
  { return telephone; }

  public final String getFax()
  { return fax; }

  public final String getEmail()
  { return email; }

  public final String getCellphone()
  { return cellphone; }

  /**
   * This method returns the user contact information as a JDOM Element. This output
   * is used by the corresponding user object to create a full XML representation
   * of the user.
   *
   * @returns
   *   JDOM Element including data fields of this class
   */
  public final org.jdom.Element toJDOMElement()
  {
    org.jdom.Element address     = new org.jdom.Element("user.contact");
    org.jdom.Element Salutation  = new org.jdom.Element("contact.salutation").setText(salutation);
    org.jdom.Element Firstname   = new org.jdom.Element("contact.firstname").setText(firstname);
    org.jdom.Element Lastname    = new org.jdom.Element("contact.lastname").setText(lastname);
    org.jdom.Element Street      = new org.jdom.Element("contact.street").setText(street);
    org.jdom.Element City        = new org.jdom.Element("contact.city").setText(city);
    org.jdom.Element Postalcode  = new org.jdom.Element("contact.postalcode").setText(postalcode);
    org.jdom.Element Country     = new org.jdom.Element("contact.country").setText(country);
    org.jdom.Element State       = new org.jdom.Element("contact.state").setText(state);
    org.jdom.Element Institution = new org.jdom.Element("contact.institution").setText(institution);
    org.jdom.Element Faculty     = new org.jdom.Element("contact.faculty").setText(faculty);
    org.jdom.Element Department  = new org.jdom.Element("contact.department").setText(department);
    org.jdom.Element Institute   = new org.jdom.Element("contact.institute").setText(institute);
    org.jdom.Element Telephone   = new org.jdom.Element("contact.telephone").setText(telephone);
    org.jdom.Element Fax         = new org.jdom.Element("contact.fax").setText(fax);
    org.jdom.Element Email       = new org.jdom.Element("contact.email").setText(email);
    org.jdom.Element Cellphone   = new org.jdom.Element("contact.cellphone").setText(cellphone);

    // Aggregate address element
    address.addContent(Salutation)
           .addContent(Firstname)
           .addContent(Lastname)
           .addContent(Street)
           .addContent(City)
           .addContent(Postalcode)
           .addContent(Country)
           .addContent(State)
           .addContent(Institution)
           .addContent(Faculty)
           .addContent(Department)
           .addContent(Institute)
           .addContent(Telephone)
           .addContent(Fax)
           .addContent(Email)
           .addContent(Cellphone);
    return address;
  }

  /**
   * This method prints the attributes of this object when in debug mode.
   */
  public final void debug()
  {
    logger.debug("Salutation      : "+salutation);
    logger.debug("Firstname       : "+firstname);
    logger.debug("Lastname        : "+lastname);
    logger.debug("Street          : "+street);
    logger.debug("City            : "+city);
    logger.debug("Postalcode      : "+postalcode);
    logger.debug("Country         : "+country);
    logger.debug("State           : "+state);
    logger.debug("Institution     : "+institution);
    logger.debug("Faculty         : "+faculty);
    logger.debug("Department      : "+department);
    logger.debug("Institute       : "+institute);
    logger.debug("Telephone       : "+telephone);
    logger.debug("Fax             : "+fax);
    logger.debug("Email           : "+email);
    logger.debug("Cellphone       : "+cellphone);
  }
}
