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
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdom.*;

import org.mycore.common.*;
import org.mycore.frontend.servlets.*;
import org.mycore.user.*;

/**
 * This servlet returns a XML Object that contains the access check result.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */

public class MCRAccessServlet extends MCRServlet
  {
  // The configuration
  private static MCRConfiguration config;
  private static Logger logger=Logger.getLogger(MCRAccessServlet.class);

  /** Initialisation of the servlet */
  public void init()
    {
    MCRConfiguration.instance().reload(true);
    config = MCRConfiguration.instance();
    PropertyConfigurator.configure(config.getLoggingProperties());
    }

  /**
   * This method overrides doGetPost of MCRServlet.<br />
   * The method looks for the parameters ip and privilege. As minimum one
   * parameter must be set, else the servlet retuns false. <br />
   * The servlet get the privilages of the user of the current session context
   * and check them against the value of access. Then it looks for the own
   * IP address and compares them with the given host or domain name. If both
   * (or one it it is only one parameter) are true the result of the
   * retuned XML stream is the following. The syntax of the stream is<br /><br />
   * &lt;mycoreaccess&gt;<br />
   * &lt;access return="true"&gt;<br />
   * &lt;/mycoreaccess&gt;<br />
   */
  public void doGetPost(MCRServletJob job) throws Exception
    {
    boolean retpriv = false;
    boolean retip = false;

    // read the parameter
    String ip = getStringParameter(job, "ip");
    if (ip == null) { ip = ""; }
    ip.trim();
    if (ip.length()==0) { retip = true; }
    String privilege = getStringParameter(job, "privilege");
    if (privilege == null) { privilege = ""; }
    privilege.trim();
    if (privilege.length()==0) { retpriv = true; }
    StringBuffer sb = new StringBuffer(1024);
    sb.append("Access check for ip [").append(ip).append("] and privilege [")
      .append(privilege).append(']');
    logger.debug(sb.toString());

    // get the MCRSession object for the current thread from the session manager.
    MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
    String userid = mcrSession.getCurrentUserID();
    logger.debug("Access check for user "+userid);

    // check the data
    boolean result = false;
    if (retip && retpriv) { 
      result = false; }
    else {
      if (!retip) { retip = checkIP(ip,job); }
      if (!retpriv) { retpriv = checkPrivileg(privilege,userid); }
      result = retip && retpriv;
      }
    
    // prepare the document
    org.jdom.Element root = new org.jdom.Element("mycoreaccess");
    org.jdom.Document jdom = new org.jdom.Document(root);
    org.jdom.Element access = new org.jdom.Element("access");
    access.setAttribute("return",String.valueOf(result));
    root.addContent(access);
    job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
    job.getRequest().setAttribute("XSL.Style", "xml");

    RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
    rd.forward(job.getRequest(), job.getResponse());
    }

  /**
   * The method check the given host or domain name  against the current
   * ip of this session.
   *
   * @param ip          the host or domain name
   * @param job         the current job
   * @return true if the current session is in the host or domain name.
   **/
  public static boolean checkIP(String ip, MCRServletJob job)
    {
    return true;
    }

  /**
   * The method check the given privilege against the privilege of the user.
   *
   * @param privilege   the given privilege
   * @param userid      the user with his privileges
   * @return true if the user has the privilege, else false
   **/
  public static boolean checkPrivileg(String privilege, String userid)
    {
    if ((privilege == null) || ((privilege = privilege.trim()).length() ==0)) {
      return false; }
    if ((userid == null) || ((userid = userid.trim()).length() ==0)) {
      return false; }
    ArrayList privs = MCRUserMgr.instance().retrieveAllPrivsOfTheUser(userid);
    return privs.contains(privilege);
    }

  }
