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

package org.mycore.backend.filesystem;

import org.mycore.common.*;
import org.mycore.datamodel.ifs.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * This class implements the MCRContentStore interface to store the content of
 * MCRFile objects on a local filesystem. The filesystem parameters are 
 * configured in mycore.properties:
 *
 * <code>
 *   MCR.IFS.ContentStore.<StoreID>.BaseDirectory  Directory on filesystem where content will be stored
 * </code>
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRCStoreLocalFilesystem extends MCRContentStoreBase implements MCRContentStore
{ 
  /** Base directory on local filesystem where content is stored */
  protected File baseDir;

  public void init( String storeID )
  {
    super.init( storeID );
    
    MCRConfiguration config = MCRConfiguration.instance();  
    baseDir = new File( config.getString( prefix + "BaseDirectory" ) );
    
    if( ! baseDir.exists() )
    {
      boolean success = baseDir.mkdirs();
      if( ! success )
      {
        String msg = "Could not create content store base directory: " + baseDir.getPath();
        throw new MCRConfigurationException( msg );
      }
    }
    else if( ! baseDir.isDirectory() )
    {
      String msg = "Content store base must be a directory, but is not: " + baseDir.getPath();
      throw new MCRConfigurationException( msg );
    }
    else if( ! baseDir.canWrite() )
    {
      String msg = "Content store base directory must be writeable: " + baseDir.getPath();
      throw new MCRConfigurationException( msg );
    }
    else if( ! baseDir.canRead() )
    {
      String msg = "Content store base directory must be readable: " + baseDir.getPath();
      throw new MCRConfigurationException( msg );
    }
  }

  public String storeContent( MCRFile file, MCRContentInputStream source )
    throws MCRPersistenceException
  {
    try
    {
      StringBuffer storageID = new StringBuffer();  
      String[] slots = buildSlotPath();
      
      for( int i = 0; i < slots.length; i++ )
        storageID.append( slots[ i ] ).append( File.separator );
      
      File dir = new File( baseDir, storageID.toString() );
      if( ! dir.exists() )
      {
        boolean success = dir.mkdirs();
        if( ! success )
        {
          String msg = "Could not create content store slot directory: " + dir.getPath();
          throw new MCRPersistenceException( msg );
        }
      }
      
      String fileID = buildNextID();
      if( file.getExtension().length() > 0 ) fileID += "." + file.getExtension();
      
      storageID.append( fileID );
      
      File local = new File( dir, fileID );
      OutputStream out = new BufferedOutputStream( new FileOutputStream( local ) );
      copy( source, out );
      out.close();
      
      return storageID.toString();
    }
    catch( Exception exc )
    {
      String msg = "Could not store content of file: " + file.getPath();
      throw new MCRPersistenceException( msg, exc );
    }
  }
  
  public void deleteContent( String storageID )
    throws MCRPersistenceException
  {
    try
    { 
      File local = new File( baseDir, storageID );
      local.delete();
      
      // Recursively remove all directories that have been created, if empty:
      StringTokenizer st = new StringTokenizer( storageID, File.separator );
      int numDirs = st.countTokens() - 1;
      String[] dirs = new String[ numDirs ];
      
      for( int i = 0; i < numDirs; i++ )
      {
        dirs[ i ] = st.nextToken();  
        if( i > 0 ) dirs[ i ] = dirs[ i - 1 ] + File.separator + dirs[ i ];  
      }
      
      for( int i = numDirs; i > 0; i-- )
      { 
        File dir = new File( baseDir, dirs[ i - 1 ] ); 
        if( dir.listFiles().length > 0 ) break;
        dir.delete();
      }
    }
    catch( Exception exc )
    {
      String msg = "Could not delete content of stored file: " + storageID;
      throw new MCRPersistenceException( msg, exc );
    }
  }

  public void retrieveContent( MCRFile file, OutputStream target )
    throws MCRPersistenceException
  {
    try
    { 
      File local = new File( baseDir, file.getStorageID() );
      InputStream in = new BufferedInputStream( new FileInputStream( local ) );
      copy( in, target );
    }
    catch( Exception exc )
    {
      String msg = "Could not get content of stored file to output stream: " + file.getStorageID();
      throw new MCRPersistenceException( msg, exc );
    }
  }
  
  protected void copy( InputStream in, OutputStream out )
    throws IOException
  {
    byte[] buffer = new byte[ 65536 ] ;
    int num;
      
    while( ( num = in.read( buffer ) ) != -1 )
      out.write( buffer, 0, num );
  }
}

