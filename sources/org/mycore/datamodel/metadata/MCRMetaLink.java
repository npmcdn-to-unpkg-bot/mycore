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

package mycore.datamodel;

import mycore.common.MCRException;
import mycore.common.MCRUtils;
import mycore.datamodel.MCRObjectID;

/**
 * This class implements all method for handling with the MCRMetaLink part
 * of a metadata object. The MCRMetaLink class present two types. At once
 * a reference of a other MCRObject or a URL. At second a bidirectional
 * link between two MCRObject's. Optional you can append the reference
 * with the label attribute. See to W3C XLink Standard for more informations.
 * <p>
 * &lt;tag class="MCRMetaLink" heritable="..."&gt;<br>
 * &lt;subtag xlink:type="locator" xlink:href="<em>MCRObjectId</em>" xlink:label="..." xlink:title="..."/&gt;<br>
 * &lt;subtag xlink:type="locator" xlink:href="<em>URL</em>" xlink:label="..." xlink:title="..."/&gt;<br>
 * &lt;subtag xlink:type="arc" xlink:from="<em>MCRObjectId</em>" xlink:to="MCRObjectId"/&gt;<br>
 * &lt;/tag&gt;<br>
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
final public class MCRMetaLink extends MCRMetaDefault 
  implements MCRMetaInterface 
{

/** The length of XLink:type **/
public static final int MAX_XLINK_TYPE_LENGTH = 8;
/** The length of XLink:href **/
public static final int MAX_XLINK_HREF_LENGTH = 128;
/** The length of XLink:label **/
public static final int MAX_XLINK_LABEL_LENGTH = 64;
/** The length of XLink:title **/
public static final int MAX_XLINK_TITLE_LENGTH = 64;
/** The length of XLink:from **/
public static final int MAX_XLINK_FROM_LENGTH = MCRObjectID.MAX_LENGTH;
/** The length of XLink:to **/
public static final int MAX_XLINK_TO_LENGTH = MCRObjectID.MAX_LENGTH;

// MetaLink data
private String href;
private String label;
private String title;
private String linktype;
private MCRObjectID ref, from, to;
private boolean ismcrobjectid;

/**
 * This is the constructor. <br>
 * The language element was set to <b>en</b>.
 * All other elemnts was set to an empty value.
 */
public MCRMetaLink()
  {
  super();
  href = "";
  label = "";
  title = "";
  linktype = null;
  ref = null;
  from = null;
  to = null;
  ismcrobjectid = false;
  }

/**
 * This is the constructor. <br>
 * The language element was set. If the value of <em>default_lang</em>
 * is null, empty or false <b>en</b> was set. The subtag element was set
 * to the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
 * is null or empty an exception was throwed. The linktype element was set to
 * the value of <em>set_linktype<em>, if it is not "locator" or "arc" a
 * MCRException was throwed. Depending of the linktype the <em>set_data1</em>
 * value was set to the href or from element. The <em>set_data2</em> was set 
 * to label or to element. The <em>set_data3</em> was set to title element.
 * If the from or to element are not a MCRObjectId
 * a MCRException was throwed.
 *
 * @param set_datapart     the global part of the elements like 'metadata'
 *                         or 'service'
 * @param set_subtag       the name of the subtag
 * @param default_lang     the default language
 * @param set_linktype     the XLink type string
 * @param set_data1        the first data
 * @param set_data2        the second data
 * @param set_data3        the third data
 * @exception MCRException if the set_subtag value is null or empty, or
 *   the from or to element is not a MCRObjectId or the linktype is not 
 *   "locator" or "arc"
 */
public MCRMetaLink(String set_datapart, String set_subtag, 
  String default_lang, String set_linktype, String set_data1, String set_data2,
    String set_data3) throws MCRException
  {
  super(set_datapart,set_subtag,default_lang,"");
  if (set_linktype == null) {
    throw new MCRException("The xlink:type is null."); }
  if (set_linktype.equals("locator")) {
    if (set_data1==null) { href = ""; } else { href = set_data1.trim(); }
    try {
      MCRObjectID id = new MCRObjectID(href); ismcrobjectid = true; }
    catch (Exception e) { ismcrobjectid = false; }
    if (set_data2==null) { label = ""; } else { label = set_data2.trim(); }
    if (set_data3==null) { title = ""; } else { title = set_data3.trim(); }
    linktype = set_linktype;
    }
  if (set_linktype.equals("arc")) {
    try {
      from = new MCRObjectID(set_data1); to = new MCRObjectID(set_data2); }
    catch (Exception e) { 
      throw new MCRException("The from/to value is not a MCRObjectID."); }
    if (set_data3==null) { title = ""; } else { title = set_data3.trim(); }
    linktype = set_linktype;
    return;
    }
  linktype = null;
  throw new MCRException("The xlink:type is not locator or arc."); 
  }

/**
 * This method set a reference with xlink:href, xlink:label and xlink:title. 
 *
 * @param set_href        the reference 
 * @param set_label       the new label string
 * @param set_title       the new title string
 **/
public final void setReference(String set_href, String set_label, 
  String set_title)
  {
  linktype = "locator";
  if (set_href==null) { href = ""; } else { href = set_href.trim(); }
  try {
    MCRObjectID id = new MCRObjectID(href); ismcrobjectid = true; }
  catch (Exception e) { ismcrobjectid = false; }
  if (set_label==null) { label = ""; } else { label = set_label.trim(); }
  if (set_title==null) { title = ""; } else { title = set_title.trim(); }
  }

/**
 * This method set a bidirectional link with xlink:from, xlink:to and 
 * xlink:title. 
 *
 * @param set_from        the source MCRObjectID
 * @param set_to          the target MCRObjectID
 * @param set_title       the new title string
 * @exception MCRException if the from or to element is not a MCRObjectId
 **/
public final void setBiLink(String set_from, String set_to, String set_title)
  throws MCRException
  {
  linktype = "arc";
  try {
    from = new MCRObjectID(set_from); to = new MCRObjectID(set_to); }
  catch (Exception e) { 
    linktype = null;
    throw new MCRException("The from/to value is not a MCRObjectID."); }
  if (set_title==null) { title = ""; } else { title = set_title.trim(); }
  }

/**
 * This method get the xlink:type element.
 *
 * @return the xlink:type
 **/
public final String getXLinkType()
  { return linktype; }

/**
 * This method get the xlink:href element as string.
 *
 * @return the xlink:href element as string
 **/
public final String getXLinkHrefToString()
  { return href; }

/**
 * This method get the xlink:href element as MCRObjectID. If xlink:href
 * is not a MCRObjectID, null was returned.
 *
 * @return the xlink:href as MCRObjectID
 **/
public final MCRObjectID getXLinkHerfToMCRObjectID()
  { 
  if (ismcrobjectid) { return new MCRObjectID(href); }
  return null;
  }

/**
 * This method get the xlink:label element.
 *
 * @return the xlink:label
 **/
public final String getXLinkLabel()
  { return label; }

/**
 * This method get the xlink:title element.
 *
 * @return the xlink:title
 **/
public final String getXLinkTitle()
  { return title; }

/**
 * This method get the xlink:from element as string.
 *
 * @return the xlink:from element as string
 **/
public final String getXLinkFromToString()
  { return from.getId(); }

/**
 * This method get the xlink:from element as MCRObjectID.
 *
 * @return the xlink:from as MCRObjectID
 **/
public final MCRObjectID getXLinkFromToMCRObjectID()
  { 
  return from;
  }

/**
 * This method get the xlink:to element as string.
 *
 * @return the xlink:to element as string
 **/
public final String getXLinkToToString()
  { return to.getId(); }

/**
 * This method get the xlink:to element as MCRObjectID.
 *
 * @return the xlink:to as MCRObjectID
 **/
public final MCRObjectID getXLinkToToMCRObjectID()
  { 
  return to;
  }

/**
 * This method get the flag for the xlink:href element.
 *
 * @return the flag, true if xlink:href is a MCRObjectID, otherwise return
 * false
 **/
public final boolean getIsMCRObjectID()
  { return ismcrobjectid; }

/**
 * This method read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param element a relevant DOM element for the metadata
 * @exception MCRException if the xlink:type is not locator or arc or if 
 * the from or to element is not a MCRObjectId
 **/
public final void setFromDOM(org.jdom.Element element)
  {
  super.setFromDOM(element);
  super.type = "";
  String temp = element.getAttributeValue("type");
  if ((temp!=null) && ((temp = temp.trim()).length() !=0)) {
    if ((temp.equals("locator"))||(temp.equals("arc"))) {
      linktype = temp; }
    else {
      linktype = null;
      throw new MCRException("The xlink:type is not locator or arc."); }
    }
  else {
    linktype = null;
    throw new MCRException("The xlink:type is not locator or arc."); }
  if (linktype.equals("locator")) {
    temp = element.getAttributeValue("href");
    if ((temp!=null) && ((temp = temp.trim()).length() !=0)) {
      try {
        MCRObjectID id = new MCRObjectID(temp); ismcrobjectid = true; }
      catch (Exception e) { ismcrobjectid = false; }
      href = temp;
      }
    temp = (String)element.getAttributeValue("label");
    if ((temp!=null) && ((temp = temp.trim()).length() !=0)) {
      label = temp; }
    temp = (String)element.getAttributeValue("title");
    if ((temp!=null) && ((temp = temp.trim()).length() !=0)) {
      title = temp; }
    }
  else {
    temp = (String)element.getAttributeValue("from");
    if ((temp!=null) && ((temp = temp.trim()).length() !=0)) {
      try { from = new MCRObjectID(temp); }
      catch (Exception e) { 
        throw new MCRException("The from/to value is not a MCRObjectID."); }
      }
    temp = (String)element.getAttributeValue("to");
    if ((temp!=null) && ((temp = temp.trim()).length() !=0)) {
      try { to = new MCRObjectID(temp); }
      catch (Exception e) { 
        throw new MCRException("The from/to value is not a MCRObjectID."); }
      }
    temp = (String)element.getAttributeValue("title");
    if ((temp!=null) && ((temp = temp.trim()).length() !=0)) {
      title = temp; }
    }
  }

/**
 * This method create a XML stream for all data in this class, defined
 * by the MyCoRe XML MCRMetaLink definition for the given subtag.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a JDOM Element with the XML MCRMetaLink part
 **/
public final org.jdom.Element createXML() throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  org.jdom.Element elm = new org.jdom.Element(subtag);
  elm.setAttribute("type",linktype,org.jdom.Namespace.getNamespace("xlink",
    MCRObject.XLINK_URL));
  if (linktype.equals("locator")) {
    elm.setAttribute("href",href,org.jdom.Namespace.getNamespace("xlink",
      MCRObject.XLINK_URL));
    if ((label != null) && ((label = label.trim()).length() !=0)) {
      elm.setAttribute("label",label,org.jdom.Namespace.getNamespace("xlink",
        MCRObject.XLINK_URL)); }
    if ((title != null) && ((title = title.trim()).length() !=0)) {
      elm.setAttribute("title",title,org.jdom.Namespace.getNamespace("xlink",
        MCRObject.XLINK_URL)); }
    }
  else {
    elm.setAttribute("from",from.getId(),org.jdom.Namespace.getNamespace(
      "xlink",MCRObject.XLINK_URL));
    elm.setAttribute("to",to.getId(),org.jdom.Namespace.getNamespace("xlink",
      MCRObject.XLINK_URL));
    if ((title != null) && ((title = title.trim()).length() !=0)) {
      elm.setAttribute("title",title,org.jdom.Namespace.getNamespace("xlink",
        MCRObject.XLINK_URL)); }
    }
  return elm;
  }

/**
 * This methode create a typed content list for all data in this instance.
 *
 * @param parasearch true if the data should parametric searchable
 * @exception MCRException if the content of this class is not valid
 * @return a MCRTypedContent with the data of the MCRObject data
 **/
public final MCRTypedContent createTypedContent(boolean parasearch)
  throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  if(!parasearch) { return tc; }
  tc.addTagElement(tc.TYPE_SUBTAG,subtag);
  tc.addLinkElement("type",linktype);
  if (linktype.equals("locator")) {
    tc.addLinkElement("href",href);
    tc.addLinkElement("label",label);
    tc.addLinkElement("title",title);
    }
  else {
    tc.addLinkElement("from",from.getId());
    tc.addLinkElement("to",to.getId());
    tc.addLinkElement("title",title);
    }
  return tc;
  }

/**
 * This methode create a String for all text searchable data in this instance.
 *
 * @param textsearch true if the data should text searchable
 * @exception MCRException if the content of this class is not valid
 * @return an empty String, because the content is not text searchable.
 **/
public final String createTextSearch(boolean textsearch)
  throws MCRException
  {
  return "";
  }

/**
 * This method check the validation of the content of this class.
 * The method returns <em>true</em> if
 * <ul>
 * <li> the subtag is not null or empty
 * <li> the xlink:type not "locator" or "arc"
 * <li> the from or to are not valid
 * </ul>
 * otherwise the method return <em>false</em>
 *
 * @return a boolean value
 **/
public final boolean isValid()
  {
  if (!super.isValid()) { return false; }
  if (linktype == null) { return false; }
  if ((!linktype.equals("locator"))&&(!linktype.equals("arc"))) { 
    return false; }
  if (linktype.equals("arc")) {
    if (!from.isValid()) { return false; }
    if (!to.isValid()) { return false; }
    }
  return true;
  }

/**
 * This method print all data content from the MCRMetaLink class.
 **/
public final void debug()
  {
  System.out.println("MCRMetaLink debug start:");
  super.debug();
  System.out.println("<xlink:type>"+type+"</xlink:type>");
  if (linktype.equals("locator")) {
    System.out.println("<xlink:href>"+href+"</xlink:href>");
    System.out.println("<xlink:label>"+label+"</xlink:label>");
    System.out.println("<xlink:title>"+title+"</xlink:title>");
    }
  if (linktype.equals("arc")) {
    System.out.println("<xlink:from>"+from.getId()+"</xlink:from>");
    System.out.println("<xlink:to>"+to.getId()+"</xlink:to>");
    System.out.println("<xlink:title>"+title+"</xlink:title>");
    }
  System.out.println("MCRMetaLink debug end"+NL);
  }

}

