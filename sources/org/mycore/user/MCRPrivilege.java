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

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.MCRException;
import org.mycore.common.MCRConfiguration;

/**
 * This class defines a privilege of the MyCoRe user management system.
 *
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRPrivilege
{
  /** The logger */
  private static Logger logger = Logger.getLogger(MCRPrivilege.class.getName());

  /** The name of the privilege */
  private String privName;

  /** The description of the privilege */
  private String privDescription;

  /** constructor */
  public MCRPrivilege (String name, String description)
  {
    privName = name;
    privDescription = description;
  }

  /** constructor */
  public MCRPrivilege (org.jdom.Element priv)
  {
  privName = "";
  privDescription = "";
  if (!priv.getName().equals("privilege")) return;
  privName = ((String)priv.getAttributeValue("name")).trim();
  List listelm = priv.getChildren();
  if (listelm.size()>0) {
    org.jdom.Element elm = (org.jdom.Element)listelm.get(0);
    if (!priv.getName().equals("privilege.descrition")) return; 
    privDescription = ((String)elm.getText()).trim();
    }
  }

  /** @return returns the name of the privilege */
  public String getName()
  { return privName; }

  /** @return returns the description of the privilege */
  public String getDescription()
  { return privDescription; }

  /**
   * @return
   *   This method returns the privilege object as a JDOM element. This is needed if
   *   one wants to get a representation of several privileges in one xml document.
   */
  public org.jdom.Element toJDOMElement() throws MCRException
  {
    org.jdom.Element priv = new org.jdom.Element("privilege").setAttribute("name", privName);
    org.jdom.Element Description = new org.jdom.Element("privilege.descrition").setText(privDescription);

    // Aggregate privilege element
    priv.addContent(Description);
    return priv;
  }

/**
 * The method check the validation of this class.
 * @return true if name is not null or empty, else return false
 **/
public boolean isValid()
  { if (privName.length()==0) return false; else return true; }

/**
 * This helper method replaces null with an empty string and trims whitespace from
 * non-null strings.
 */
protected final static String trim(String s)
  { return (s != null) ? s.trim() : ""; }

/**
 * This method put debug data to the logger (for the debug mode).
 **/
public final void debug()
  {
  MCRConfiguration config = MCRConfiguration.instance();
  PropertyConfigurator.configure(config.getLoggingProperties());
  logger.debug("privName           = "+privName);
  logger.debug("privDescription    = "+privDescription);
  }
}
