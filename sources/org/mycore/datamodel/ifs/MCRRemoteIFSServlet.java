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

package org.mycore.datamodel.ifs;

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import javax.servlet.*;
import javax.servlet.http.*;
import org.jdom.*;
import org.mycore.common.*;
import org.mycore.common.xml.*;

/**
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt 
 * @version $Revision$ $Date$
 **/
public class MCRRemoteIFSServlet extends HttpServlet
{
// The configuration
private MCRConfiguration conf = null;

// Default Language (as UpperCase)
private String defaultLang = "";

// The list of hosts from the configuration
private ArrayList remoteAliasList = null;

 /**
  * The initialization method for this servlet. This read the default
  * language from the configuration.
  **/
  public void init() throws MCRConfigurationException
  {
    conf = MCRConfiguration.instance();
    // read the default language
    String defaultLang = conf
      .getString( "MCR.metadata_default_lang", "en" ).toUpperCase();
    // read host list from configuration
    String hostconf = conf.getString("MCR.communication_hostaliases","local");
    remoteAliasList = new ArrayList();
    int i = 0;
    int j = hostconf.length();
    int k = 0;
    while (k!=-1) 
    {
      k = hostconf.indexOf(",",i);
      if (k==-1) { remoteAliasList.add(hostconf.substring(i,j)); }
      else { remoteAliasList.add(hostconf.substring(i,k)); i = k+1; }
    }
  }

  public void doPost( HttpServletRequest req, HttpServletResponse res )
    throws IOException, ServletException
  { doGet(req,res); }

  public void doGet( HttpServletRequest req, HttpServletResponse res )
    throws IOException, ServletException
  {
    String requestPath = req.getPathInfo();
System.out.println("requestPath = "+requestPath);
    if( requestPath == null ) 
    {
      String msg = "Error: HTTP request path is null";
      System.out.println( msg );
      res.sendError( res.SC_NOT_FOUND, msg );
      return;
    }
    
    StringTokenizer st = new StringTokenizer( requestPath, "/" );
    if( ! st.hasMoreTokens() )
    {
      String msg = "Error: HTTP request path is empty";
      System.out.println( msg );
      res.sendError( res.SC_NOT_FOUND, msg );
      return;
    }
    
    // get the language
    String lang  = req.getParameter( "lang" );
    String att_lang  = (String) req.getAttribute( "lang" );
    if (att_lang!=null) { lang = att_lang; }
    if( lang  == null ) { lang  = defaultLang; }
    if (lang.equals("")) { lang = defaultLang; }
    lang = lang.toUpperCase();
System.out.println("MCRRemoteIFSServlet : lang = "+lang);

    // get the host alias
    String host  = req.getParameter( "hosts" );
    String att_host  = (String) req.getAttribute( "hosts" );
    if (att_host!=null) { host = att_host; }
    if( host  == null ) host  = "local";
    if( host.equals("") ) host  = "local";
System.out.println("MCRRemoteIFSServlet : hosts = "+host);

    String ownerID = st.nextToken();
    
    if (host.equals("local"))
    {
      MCRFilesystemNode[] roots = MCRFilesystemNode.getRootNodes( ownerID );
      if( roots.length == 0 )
      {
        String msg = "Error: No root node found for owner ID " + ownerID;
        System.out.println( msg );
        res.sendError( res.SC_NOT_FOUND, msg );
        return;
      }
    
      MCRFilesystemNode root = roots[ 0 ];
    
      if( root instanceof MCRFile )
      {
        if( st.hasMoreTokens() )
        {
          String msg = "Error: No such file or directory " + st.nextToken();
          System.out.println( msg );
          res.sendError( res.SC_NOT_FOUND, msg );
          return;
        }
        else
        {
          sendFile( req, res, (MCRFile)root );
          return;
        }
      }
      else
      {
        int pos = ownerID.length() + 1;
        String path = requestPath.substring( pos );
      
        MCRDirectory dir = (MCRDirectory)root;
        MCRFilesystemNode node = dir.getChildByPath( path );
      
        if( node == null )
        {
          String msg = "Error: No such file or directory " + path;
          System.out.println( msg );
          res.sendError( res.SC_NOT_FOUND );
          return;
        }
        else if( node instanceof MCRFile )
        {
          sendFile( req, res, (MCRFile)node );
          return;
        }
        else
        {
          sendDirectory( req, res, (MCRDirectory)node );
          return;
        }
      }
    }
    else
    {
      // read the configuration
      String protocol = conf.getString("MCR.communication_"+host+"_protocol");
      String realhost = conf.getString("MCR.communication_"+host+"_host");
      int port = conf.getInt("MCR.communication_"+host+"_port");
      String location = conf.getString("MCR.communication_"+host+
        "_ifs_servlet")+requestPath;
System.out.println("Protokoll = "+protocol);
System.out.println("Realhost = "+realhost);
System.out.println("Port = "+port);
System.out.println("Location = "+location);
      // get data from the URL
      BufferedInputStream in = null;
      String headercontext = "";
      try {
        URL currentURL = new URL(protocol,realhost,port,location);
        HttpURLConnection urlCon = 
          (HttpURLConnection) currentURL.openConnection();
        urlCon.setDoOutput(true);
        urlCon.setRequestMethod("POST");
        PrintWriter pout = new PrintWriter(urlCon.getOutputStream());
        pout.print("hosts=local");
        pout.close();
        in = new BufferedInputStream(urlCon.getInputStream());
        headercontext = urlCon.getContentType();
        }
      catch(MCRException mcre) {
        System.err.println("Can't use the response from host:"+realhost+".");
        return; }
      catch(UnknownHostException uhe) {
        System.err.println("Don't know about host: "+realhost+"."); 
        return; }
      catch(IOException ioe) {
        System.err.println("Couldn't get I/O for the connection to: "
          +realhost+"."); 
        return; }
      catch(Exception e) {
        e.printStackTrace(System.err); 
        return; }

      if(!headercontext.equals("text/xml")) {
        res.setContentType(headercontext);
        OutputStream out = new BufferedOutputStream( res.getOutputStream() );
        int inread;
        while ((inread = in.read()) != -1) { out.write(inread); }
        out.close();
        return;
        }

      org.jdom.Document jdom = null;
      String style = "";
      Properties parameters = MCRLayoutServlet.buildXSLParameters( req );
      StringBuffer buf = new StringBuffer(1024);
      int inread;
      while (( inread = in.read()) != -1) { buf.append((char) inread); }
      boolean ismcrxml = true;
      MCRXMLContainer resarray = new MCRXMLContainer();
      try {
        resarray.importElements(buf.toString().getBytes()); }
      catch (org.jdom.JDOMException e) {
        res.setContentType(headercontext);
        OutputStream out = new BufferedOutputStream( res.getOutputStream() );
        out.write(buf.toString().getBytes()); 
        out.close();
        return;
        }
      catch (MCRException e) {
        ismcrxml = false; }
      if (!ismcrxml) {
        ByteArrayInputStream bin = new ByteArrayInputStream(buf.toString()
          .getBytes());
        org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder();
        try {
          jdom = builder.build(bin); }
        catch (org.jdom.JDOMException f) { }
        style = parameters.getProperty("Style");
        }
      else {
        resarray.setHost(0,host);
        jdom = resarray.exportAllToDocument();
        style = parameters.getProperty("Style","IFSMetadata-"+lang);
        }
      System.out.println("Style = "+style);

    if (style.equals("xml")) {
      res.setContentType( "text/xml" );
      OutputStream out = res.getOutputStream();
      new org.jdom.output.XMLOutputter( "  ", true ).output( jdom, out );
      out.close();
      }
    else {
      req.setAttribute( "MCRLayoutServlet.Input.JDOM", jdom );
      req.setAttribute( "XSL.Style", style );
      RequestDispatcher rd = getServletContext()
        .getNamedDispatcher( "MCRLayoutServlet" );
      rd.forward( req, res );
      }

    }
  }
  
  private void sendFile( HttpServletRequest req, HttpServletResponse res, MCRFile file )
    throws IOException, ServletException
  {
    System.out.println( "Sending file " + file.getName() );
    
    res.setContentType( file.getContentType().getMimeType() );
    res.setContentLength( (int)( file.getSize() ) );
    
    OutputStream out = new BufferedOutputStream( res.getOutputStream() );
    file.getContentTo( out );
    out.close();
  }
  
  private void sendDirectory( HttpServletRequest req, HttpServletResponse res, 
    MCRDirectory dir ) throws IOException, ServletException
  {
    String lang  = req.getParameter( "lang" );
    String att_lang  = (String) req.getAttribute( "lang" );
    if (att_lang!=null) { lang = att_lang; }
    if( lang  == null ) { lang  = defaultLang; }
    if (lang.equals("")) { lang = defaultLang; }
    lang = lang.toUpperCase();
    System.out.println("MCRFileNodeServlet : lang = "+lang);

    System.out.println( "Sending list of files in directory " + dir.getName() );
    
    Element root = new Element( "mcr_directory" );
    Document doc = new org.jdom.Document( root );
    
    root.setAttribute( "ID", dir.getID() );
    
    addString( root, "path",         dir.getPath() );
    addString( root, "ownerID",      dir.getOwnerID() );
    addDate  ( root, "lastModified", dir.getLastModified() );
    addString( root, "numChildren",  String.valueOf( dir.getNumChildren() ) );
    addString( root, "size",         String.valueOf( dir.getSize() ) );

    Element nodes = new Element( "children" );
    root.addContent( nodes );
    
    MCRFilesystemNode[] children = dir.getChildren();
    for( int i = 0; i < children.length; i++ )
    {
      Element node = new Element( "child" );
      node.setAttribute( "ID", children[ i ].getID() );
      nodes.addContent( node );

      addString( node, "name",         children[ i ].getName() );
      addString( node, "size",         String.valueOf( children[ i ].getSize() ) );
      addDate  ( node, "lastModified", children[ i ].getLastModified() );
      
      if( children[ i ] instanceof MCRFile )
      {
        node.setAttribute( "type", "file" );
        
        MCRFile file = (MCRFile)( children[ i ] );
        addString( node, "contentType", file.getContentTypeID() );
        addString( node, "md5",         file.getMD5() );
                
        if( file.hasAudioVideoExtender() )
        {  
          MCRAudioVideoExtender ext = file.getAudioVideoExtender();

          Element xExtender = new Element( "extender" );
          node.addContent( xExtender );
          addExtenderData( xExtender, ext );
        }
      }
      else node.setAttribute( "type", "directory" );
    }
    
    // put it in an MCRXMLContainer
    MCRXMLContainer resarray = new MCRXMLContainer();
    resarray.add("local",dir.getOwnerID(),1,doc.getRootElement());
    org.jdom.Document jdom = resarray.exportAllToDocument();
    
    // prepare the stylesheet name
    Properties parameters = MCRLayoutServlet.buildXSLParameters( req );
    String style = parameters.getProperty("Style","IFSMetadata-"+lang);
    System.out.println("Style = "+style);

    if (style.equals("xml")) {
      res.setContentType( "text/xml" );
      OutputStream out = res.getOutputStream();
      new org.jdom.output.XMLOutputter( "  ", true ).output( jdom, out );
      out.close();
      }
    else {
      req.setAttribute( "MCRLayoutServlet.Input.JDOM", jdom );
      req.setAttribute( "XSL.Style", style );
      RequestDispatcher rd = getServletContext()
        .getNamedDispatcher( "MCRLayoutServlet" );
      rd.forward( req, res );
      }
  }
  
  private String     dateFormat    = "dd.MM.yyyy HH:mm:ss";
  private DateFormat dateFormatter = new SimpleDateFormat( dateFormat );

  private void addDate( Element parent, String type, GregorianCalendar date )
  {
    Element xDate = new Element( "date" );
    parent.addContent( xDate );
    
    xDate.setAttribute( "type", type );
    
    String time = dateFormatter.format( date.getTime() );
    
    xDate.setAttribute( "format", dateFormat );
    xDate.addContent( time );
  }
  
  private String     timeFormat    = "HH:mm:ss";
  private DateFormat timeFormatter = new SimpleDateFormat( timeFormat );

  private void addTime( Element parent, String type, int hh, int mm, int ss )
  {
    Element xTime = new Element( type );
    parent.addContent( xTime );

    GregorianCalendar date = new GregorianCalendar( 2002, 01, 01, hh, mm, ss );
    String time = timeFormatter.format( date.getTime() );
    
    xTime.setAttribute( "format", timeFormat );
    xTime.addContent( time );
  }
  
  private void addString( Element parent, String itemName, String content )
  {
    if( ( content == null ) || ( content.trim().length() == 0 ) ) return;
    parent.addContent( new Element( itemName ).addContent( content.trim() ) );
  }
  
  private void addExtenderData( Element parent, MCRAudioVideoExtender ext )
  {
    parent.setAttribute( "type", ext.isVideo() ? "video" : "audio" );

    int hh = ext.getDurationHours();
    int mm = ext.getDurationMinutes();
    int ss = ext.getDurationSeconds();
    addTime( parent, "duration", hh, mm, ss );
    
    addString( parent, "bitRate", String.valueOf( ext.getBitRate() ) );
    
    if( ext.isVideo() )
      addString( parent, "frameRate", String.valueOf( ext.getFrameRate() ) );
    
    addString( parent, "playerURL", ext.getPlayerDownloadURL() );
  }
}

