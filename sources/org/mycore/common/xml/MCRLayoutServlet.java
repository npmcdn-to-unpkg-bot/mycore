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

package org.mycore.common.xml;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.sax.*;
import org.mycore.common.*;
// JDOM imports
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.jdom.transform.JDOMResult;
// XSLT imports
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Does the layout for other MyCoRe servlets by transforming XML 
 * input to various output formats, using XSL stylesheets.
 *
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler
 * @version $Revision$ $Date$
 */
public class MCRLayoutServlet extends HttpServlet 
{
  // The configuration
  private MCRConfiguration conf = null;

  protected SAXTransformerFactory factory;  
  protected MCRCache stylesheetCache;
  protected MCRCache staticFileCache;
  private static Logger logger=Logger.getLogger(MCRLayoutServlet.class);
  public static final String JDOM_ATTR="MCRLayoutServlet.Input.JDOM";
  public static final String DOM_ATTR="MCRLayoutServlet.Input.DOM";

  public void init()
  {
    // Get SAX transformer factory
    TransformerFactory tf = TransformerFactory.newInstance();
    conf = MCRConfiguration.instance();
   	PropertyConfigurator.configure(conf.getLoggingProperties());
  
    if( ! tf.getFeature( SAXTransformerFactory.FEATURE ) )
      throw new MCRConfigurationException
      ( "Could not load a SAXTransformerFactory for use with XSLT" );
      
    factory = (SAXTransformerFactory)( tf );
    
    // Create caches
    stylesheetCache = new MCRCache( 50 );
    staticFileCache = new MCRCache( 50 );
  }

 /**
  * Reads the input XML from a file or from the invoking servlet,
  * chooses a stylesheet and does the layout by applying the XSL
  * stylesheet. This method handles HTTP POST requests.
  */
  public void doPost( HttpServletRequest  request,
                      HttpServletResponse response )
    throws IOException, ServletException
  { doGet( request, response ) ; }
    
 /**
  * Reads the input XML from a file or from the invoking servlet,
  * chooses a stylesheet and does the layout by applying the XSL
  * stylesheet. This method handles HTTP GET requests.
  */
  public void doGet( HttpServletRequest  request, 
                     HttpServletResponse response ) 
    throws IOException, ServletException
  {
  	logger.info("MCRLayoutServlet started...");
    Properties parameters = buildXSLParameters( request );
    
    org.jdom.Document xml;  
      
    if( invokedByServlet( request ) )  
      xml = getXMLInputFromServlet( request );
    else
      xml = getXMLInputFromFile( request, parameters );
    
    String style = parameters.getProperty( "Style", "default" );

    if( "xml".equals( style ) )
    {
      renderAsXML( xml, response );
    }
    else
    {
      String documentType = getDocumentType( xml );
      String styleName    = buildStylesheetName( style, documentType );
      String styleDir     = "/WEB-INF/stylesheets/";
      
      File styleFile = getStylesheetFile( styleDir, styleName );
      logger.info("MCRLayoutServlet: Stylesheet read!");

      if( styleFile == null ) 
        renderAsXML( xml, response );
      else
      {
        Templates stylesheet = getCompiledStylesheet( factory, styleFile );
        TransformerHandler handler = getHandler( stylesheet );
        setXSLParameters( handler, parameters );
        try {
			transform( xml, stylesheet, handler, response );
		} catch (IOException e) {
			logger.error("IO Error while transforming Document",e);
		}
      }
    }
  	logger.info("MCRLayoutServlet finished!");
  }

 /**
  * Returns true, if LayoutServlet was invoked by another servlet, or false
  * otherwise, meaning it was invoked by a direct static "*.xml" mapping.
  */
  protected boolean invokedByServlet( HttpServletRequest request )
  { 
    return ( request.getAttribute( JDOM_ATTR ) != null ) ||
           ( request.getAttribute( DOM_ATTR  ) != null );
  }
  
 /**
  * Assuming that LayoutServlet was invoked by another servlet by 
  * request dispatching, gets XML input as JDOM or DOM document from
  * the request attributesc that the invoking servlet has set.
  */
  protected org.jdom.Document getXMLInputFromServlet( HttpServletRequest request )
  {
    org.jdom.Document jdom = (org.jdom.Document)
      request.getAttribute( JDOM_ATTR );
    if( jdom != null) return jdom;
    
    org.w3c.dom.Document dom = (org.w3c.dom.Document)
      request.getAttribute( DOM_ATTR );

    try{ return new org.jdom.input.DOMBuilder().build( dom ); }
    catch( Exception exc )
    {
      String msg = "LayoutServlet could not transform DOM input to JDOM";
      throw new MCRException( msg, exc );
    }
  }
  
  /**
   * Assuming that LayoutServlet was invoked by a static "*.xml" url mapping,
   * reads a static xml file from disk and parses it to a JDOM document as
   * input.
   */
  protected org.jdom.Document getXMLInputFromFile( HttpServletRequest request,
                                                   Properties parameters )
  {
    String requestedPath = request.getServletPath();
    URL url = null;
    
    try{ url = getServletContext().getResource( requestedPath ); }
    catch( MalformedURLException willNeverBeThrown ){}
    
    if( url == null )
    {
      String msg = "LayoutServlet could not find file " + requestedPath;
      throw new MCRException( msg );
    }

    String path = getServletContext().getRealPath( requestedPath );
    File   file = new File( path );
    long   time = file.lastModified();

    String documentBaseURL = file.getParent() + File.separator;
    parameters.put( "DocumentBaseURL", documentBaseURL );

    org.jdom.Document input = (org.jdom.Document)
      ( staticFileCache.getIfUpToDate( path, time ) );
   
    if( input == null )
    {
      try{ input = new org.jdom.input.SAXBuilder().build( url ); }
      catch( Exception exc )
      {
        String msg = "LayoutServlet could not parse XML input from " + path;
        throw new MCRException( msg, exc );
      }

      staticFileCache.put( path, input );
    }

    return input;
  }
  
  public static final Properties buildXSLParameters(HttpServletRequest request)
  {
  	long time=System.currentTimeMillis();
	Properties parameters = new Properties();
	String name=null;
	String user=null;
    
	// Read all *.xsl attributes that are stored in the browser session
	HttpSession session = request.getSession(false);
	if (session != null){
		for( Enumeration e = session.getAttributeNames(); e.hasMoreElements(); )		{
	  		name = (String)( e.nextElement() );
	  		if( name.startsWith( "XSL." ) ){
	  			parameters.put( name.substring( 4 ), session.getAttribute( name ) );
	  		}
		}
		user = (String)( session.getAttribute( "XSL.CurrentUser" ) );
	}
    
	// Read all *.xsl attributes provided by the invoking servlet
	for( Enumeration e = request.getAttributeNames(); e.hasMoreElements(); )
	{
	  name = (String)( e.nextElement() );
	  if( name.startsWith( "XSL." ) )
	  {
		parameters.put( name.substring( 4 ), request.getAttribute( name ) );
	  }
	}
      
	// Read all *.xsl attributes from the client HTTP request parameters
	for( Enumeration e = request.getParameterNames(); e.hasMoreElements(); )
	{
	  name = (String)( e.nextElement() );
	  if( name.startsWith( "XSL." ) )
	  {
		parameters.put( name.substring( 4 ), request.getParameter( name ) );
	  }
	}

	// Set some predefined XSL parameters:

	if( user == null ) user = "gast";
    
	String contextPath = request.getContextPath() + "/";
	String requestURL  = getCompleteURL( request );
    
	int pos = requestURL.indexOf( contextPath, 9 );
	String applicationBaseURL = requestURL.substring( 0, pos ) + contextPath;

	String servletsBaseURL = applicationBaseURL + "servlets/";

	String defaultLang = MCRConfiguration.instance()
	  .getString( "MCR.metadata_default_lang", "en" );

	parameters.put( "CurrentUser",           user               );
	parameters.put( "RequestURL",            requestURL         );
	parameters.put( "WebApplicationBaseURL", applicationBaseURL );
	parameters.put( "ServletsBaseURL",       servletsBaseURL    );
	parameters.put( "DefaultLang",           defaultLang        );
    
    logger.info("buildXSLTParameters runned: "+(System.currentTimeMillis()-time)+" msec!");
    return parameters;
  }

  public static final String getCompleteURL( HttpServletRequest request )
  {
    StringBuffer buffer = HttpUtils.getRequestURL( request );
    String queryString = request.getQueryString();
    if( queryString != null ) buffer.append( "?" ).append( queryString );
    return buffer.toString();
  }
  
  protected void renderAsXML( org.jdom.Document xml,
                              HttpServletResponse response )
    throws IOException
  {
    response.setContentType( "text/xml" );
    OutputStream out = response.getOutputStream();
    new org.jdom.output.XMLOutputter( "  ", true ).output( xml, out );
    out.close();
  }
  
  /**
   * Returns the XML document type name as declared in the XML input document,
   * or otherwise returns the name of the root element instead.
   */
  protected String getDocumentType( org.jdom.Document xml )
  {
    if( xml.getDocType() != null ) 
      return xml.getDocType().getElementName();
    else
      return xml.getRootElement().getName();
  }

 /**
  * Builds the filename of the stylesheet to use, e. g. "playlist-simple.xsl"
  */
  protected String buildStylesheetName( String style, String docType )
  {
    StringBuffer filename = new StringBuffer( docType );
    if( ! "default".equals( style ) )
    {
      filename.append( "-"   );
      filename.append( style );
    }
    filename.append( ".xsl" );
    
    return filename.toString();
  }
  
  protected File getStylesheetFile( String dir, String name )
  {
    String path = getServletContext().getRealPath( dir + name );
    File file = new File( path );
    logger.info("Stylesheet: "+file.getPath());
    
    if( ! file.exists() ) return null;

    if( ! file.canRead() )
    {
      String msg = "XSL stylesheet " + path + " not readable";
      throw new MCRConfigurationException( msg );
    }  
    
    return file;
  }
  
  protected Templates getCompiledStylesheet( TransformerFactory factory,
                                             File file )
  {
    String path = file.getPath();
    long   time = file.lastModified();
    
    Templates stylesheet = (Templates)( stylesheetCache.getIfUpToDate( path, time ) );
    
    if( stylesheet == null )
    {
      try
      { stylesheet = factory.newTemplates( new StreamSource( file ) ); }
      catch( TransformerConfigurationException exc )
      {
        String msg = "Error while compiling XSL stylesheet " + file.getName();
        throw new MCRConfigurationException( msg, exc );
      }
      
      stylesheetCache.put( path, stylesheet );
    }
    
    return stylesheet;
  }

  protected TransformerHandler getHandler( Templates stylesheet )
  {
    try
    { return factory.newTransformerHandler( stylesheet ); }
    catch( TransformerConfigurationException exc )
    {
      String msg = "Error while compiling XSL stylesheet";
      throw new MCRConfigurationException( msg, exc );
    }
  }  
    
  protected void setXSLParameters( TransformerHandler handler,
                                   Properties         parameters )
  {
    Transformer transformer = handler.getTransformer();
    Enumeration names       = parameters.propertyNames();
    
    while( names.hasMoreElements() )
    {
      String name  = (String)( names.nextElement() );
      String value = parameters.getProperty( name );

      transformer.setParameter( name, value );
    }
  }
  
  protected void transform( org.jdom.Document   xml,
                            Templates           xsl,
                            TransformerHandler  handler,
                            HttpServletResponse response )
    throws IOException
  {
    // Set content type  from "<xsl:output media-type = "...">
    // Set char encoding from "<xsl:output encoding   = "...">

    String ct  = xsl.getOutputProperties().getProperty( "media-type" );
    String enc = xsl.getOutputProperties().getProperty( "encoding"   );
    response.setContentType( ct + "; charset=" + enc );

    OutputStream out = response.getOutputStream();
    handler.setResult( new StreamResult( out ) );
    
    try
    {  
    	new org.jdom.output.SAXOutputter( handler ).output( xml );
    }
    catch( org.jdom.JDOMException ex )
    {
      String msg = "Error while transforming XML using XSL stylesheet";
      throw new MCRException( msg, ex );
    }
    finally
    { out.close(); }
  }
}
