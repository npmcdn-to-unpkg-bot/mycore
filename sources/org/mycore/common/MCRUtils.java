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

package mycore.common;

import java.util.*;
import java.text.*;
import mycore.common.MCRArgumentChecker;

/**
 * This class represent a general set of external methods to support
 * the programming API.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRUtils
{
// common data
private static DateFormat DE_DF =
  DateFormat.getDateInstance(DateFormat.MEDIUM,Locale.GERMANY);
private static DateFormat UK_DF =
  DateFormat.getDateInstance(DateFormat.MEDIUM,Locale.UK);
private static DateFormat US_DF =
  DateFormat.getDateInstance(DateFormat.MEDIUM,Locale.US);

/**
 * This method check the language string base on RFC 1766 to the supported
 * languages in mycore.
 *
 * @param lang          the language string
 * @return trye if the language was supported, otherwise false
 **/
public static final boolean isSupportedLang(String lang)
  { 
  if ((lang == null) || ((lang = lang.trim()).length() ==0)) {
    return false; }
  lang = lang.trim().toLowerCase();
  if (lang.equals("de")) return true;
  if (lang.equals("en")) return true;
  if (lang.equals("en_uk")) return true;
  if (lang.equals("en_us")) return true;
  return false;
  }

/**
 * The method return the instance of DateFormat for the given language.
 *
 * @param lang          the language string
 * @return the instance of DateFormat or null
 **/
public static final DateFormat getDateFormat(String lang)
  {
  lang = lang.trim().toLowerCase();
  if (!isSupportedLang(lang)) { return null; }
  if (lang.equals("de")) return DE_DF;
  if (lang.equals("en")) return US_DF;
  if (lang.equals("en_uk")) return UK_DF;
  if (lang.equals("en_us")) return US_DF;
  return null;
  }
         
/**
 * The method check a date string for the pattern <em>tt.mm.jjjj</em>.
 *
 * @param date          the date string
 * @return true if the pattern is correct, otherwise false
 **/
public static final boolean isDateInDe(String date)
  {
  if ((date == null) || ((date = date.trim()).length() ==0)) {
    return false; }
  date = date.trim().toUpperCase();
  if (date.length()!=10) { return false; }
  try {
    GregorianCalendar newdate = new GregorianCalendar();
    newdate.setTime(DE_DF.parse(date)); }
  catch (ParseException e) {
    return false; }
  return true;
  }

/**
 * The method check a date string for the pattern <em>yyyy-dd-mm</em>.
 *
 * @param date          the date string
 * @return true if the pattern is correct, otherwise false
 **/
public static final boolean isDateInEn_UK(String date)
  {
  if ((date == null) || ((date = date.trim()).length() ==0)) {
    return false; }
  date = date.trim().toUpperCase();
  if (date.length()!=10) { return false; }
  try {
    GregorianCalendar newdate = new GregorianCalendar();
    newdate.setTime(UK_DF.parse(date)); }
  catch (ParseException e) {
    return false; }
  return true;
  }

/**
 * The method check a date string for the pattern <em>yyyy/dd/mm</em>.
 *
 * @param date          the date string
 * @return true if the pattern is correct, otherwise false
 **/
public static final boolean isDateInEn_US(String date)
  {
  if ((date == null) || ((date = date.trim()).length() ==0)) {
    return false; }
  date = date.trim().toUpperCase();
  if (date.length()!=10) { return false; }
  try {
    GregorianCalendar newdate = new GregorianCalendar();
    newdate.setTime(US_DF.parse(date)); }
  catch (ParseException e) {
    return false; }
  return true;
  }

/**
 * This methode replace any characters to XML entity references.<p>
 * <ul>
 * <li> &lt; to &amp;lt;
 * <li> &gt; to &amp;gt;
 * <li> &amp; to &amp;amp;
 * <li> &quot; to &amp;quot;
 * <li> &apos; to &amp;apos;
 * </ul>
 *
 * @param in  a string
 * @return the converted string.
 **/
public static final String stringToXML(String in)
  {
  if (in == null) { return ""; }
  StringBuffer sb = new StringBuffer(2048);
  for (int i=0;i<in.length();i++) {
    if (in.charAt(i)=='<') { sb.append("&lt;"); continue; }
    if (in.charAt(i)=='>') { sb.append("&gt;"); continue; }
    if (in.charAt(i)=='&') { sb.append("&amp;"); continue; }
    if (in.charAt(i)=='\"') { sb.append("&quot;"); continue; }
    if (in.charAt(i)=='\'') { sb.append("&apos;"); continue; }
    sb.append(in.charAt(i));
    }
  return sb.toString();
  }
} 

