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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.StringTokenizer;
import javax.servlet.ServletContext;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRConfiguration;
import org.mycore.frontend.servlets.MCRServlet;

public class MCRURIResolver implements javax.xml.transform.URIResolver
{
  private static final Logger LOGGER = Logger.getLogger( MCRURIResolver.class);

  public static synchronized void init( ServletContext ctx, String webAppBase )
  { singleton = new MCRURIResolver( ctx, webAppBase ); }
  
  private static MCRURIResolver singleton = null;
  
  public static MCRURIResolver instance()
  { return singleton; }
  
  private ServletContext context;
  private String base;
  private MCRCache fileCache;

  private MCRURIResolver( ServletContext context, String base )
  {
    this.context = context;
    this.base = base;
    
    MCRConfiguration config = MCRConfiguration.instance();
    String prefix = "MCR.URIResolver.";
    int cacheSize = config.getInt( prefix + "StaticFiles.CacheSize", 100 );
    fileCache = new MCRCache( cacheSize );
  }

  /**
   * URI Resolver that resolves XSL document() calls 
   **/  
  public Source resolve( String href, String base ) 
    throws TransformerException
  {
    if( base != null )
    {
      int posA = base.lastIndexOf( "/"  );
      int posB = base.lastIndexOf( "\\" );
      int pos = ( posA == -1 ? posB : posA );
      String file = ( pos == -1 ? base : base.substring( pos + 1 ) );
      LOGGER.debug( "Including " + href + " from " + file );
    }
    else LOGGER.debug( "Including " + href );

    if( href.indexOf( ":" ) == -1 ) return null;

    String scheme = getScheme( href );
    if( "resource webapp file session".indexOf( scheme ) != -1 )
      return new JDOMSource( resolve( href ) );
    else 
      return null; 
  }
  
  private String getScheme( String uri )
  { return new StringTokenizer( uri, ":" ).nextToken(); }
  
  public Element resolve( String uri )
  {
    LOGGER.info( "Reading xml from uri " + uri );

    String scheme = getScheme( uri );
    
    if( "resource".equals( scheme ) )
      return readFromResource( uri );
    else if( "webapp".equals( scheme ) )
      return readFromWebapp( uri );
    else if( "file".equals( scheme ) )
      return readFromFile( uri );
    else if( "http".equals( scheme ) || "https".equals( scheme ) )
      return readFromHTTP( uri );
    else if( "request".equals( scheme ) )
      return readFromRequest( uri );
    else if( "session".equals( scheme ) )
      return readFromSession( uri );
    else
    {
      String msg = "Unsupported URI type: " + uri;
      throw new MCRUsageException( msg );
    }
  }
  
  // resource:path
  private Element readFromResource( String uri )
  {
    String path = uri.substring( uri.indexOf( ":" ) + 1 );
    LOGGER.debug( "Reading xml from classpath resource " + path );
    return parseStream( this.getClass().getResourceAsStream( path ) );
  }

  // webapp:path
  private Element readFromWebapp( String uri )
  {
    String path = uri.substring( uri.indexOf( ":" ) + 1 );
    LOGGER.debug( "Reading xml from webapp " + path );
    uri = "file://" + context.getRealPath( path );
    return readFromFile( uri );
  }
  
  // file:///path
  private Element readFromFile( String uri )
  {
    String path = uri.substring( "file://".length() );
    LOGGER.debug( "Reading xml from file " + path );
    File file = new File( path );
    Element fromCache = (Element) fileCache.getIfUpToDate( path, file.lastModified() );
    if( fromCache != null ) return (Element)( fromCache.clone() );
    
    try
    {
      Element parsed = parseStream( new FileInputStream( file ) );
      fileCache.put( path, parsed );
      return (Element)( parsed.clone() );
    }
    catch( FileNotFoundException ex )
    {
      String msg = "Could not find file for URI " + uri;
      throw new MCRUsageException( msg, ex ); 
    }
  }
  
  // http:// oder https://
  private Element readFromHTTP( String url )
  {
    LOGGER.debug( "Reading xml from url " + url );

    try
    { return parseStream( new URL( url ).openStream() ); }
    catch( java.net.MalformedURLException ex )
    {
      String msg = "Malformed http url: " + url;
      throw new MCRUsageException( msg, ex ); 
    }
    catch( IOException ex )
    {
      String msg = "Unable to open input stream at " + url;
      throw new MCRUsageException( msg, ex ); 
    }
  }

  // request:pfad
  private Element readFromRequest( String uri )
  {
    String path = uri.substring( uri.indexOf( ":" ) + 1 );
    LOGGER.debug( "Reading xml from request " + path );

    StringBuffer url = new StringBuffer( MCRServlet.getBaseURL() );
    url.append( path );
    
    if( path.indexOf( "?" ) != -1 )
      url.append( "&" );
    else
      url.append( "?" );
    
    url.append( "MCRSessionID=" );
    url.append( MCRSessionMgr.getCurrentSession().getID() );
    
    return readFromHTTP( url.toString() );
  }

  // session:key
  private Element readFromSession( String uri )
  {
    String key = uri.substring( uri.indexOf( ":" ) + 1 );
    
    LOGGER.debug( "Reading xml from session using key " + key );
    Object value = MCRSessionMgr.getCurrentSession().get( key );
    return (Element)( ((Element)value).clone() );
  }
  
  private Element parseStream( InputStream in )
  { 
    try
    { return new org.jdom.input.SAXBuilder().build( in ).getRootElement(); }
    catch( Exception ex )
    {
      String msg = "Exception while reading and parsing XML InputStream";
      throw new MCRUsageException( msg, ex );
    }
  }
}
