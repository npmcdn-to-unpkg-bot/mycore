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

package mycore.sql;

import java.sql.*;
import mycore.common.*;

/**
 * Instances of this class are used to read the rows of a result set
 * when an SQL query is done using MCRSQLConnection. This is a wrapper
 * around java.sql.ResultSet that provides some convenience methods.
 *
 * @author Frank Lützenkirchen
 *
 * @version $Revision$ $Date$
 *
 * @see java.sql.ResultSet
 * @see MCRSQLConnection#doQuery()
 * @see MCRSQLConnection#justDoQuery()
 */
public class MCRSQLRowReader
{
  /** The wrapped JDBC result set */
  protected ResultSet rs;
  
  /** 
   * Creates a new MCRSQLRowReader. This constructor is called by MCRSQLConnection
   * methods that execute an SQL query.
   *
   * @see MCRSQLConnection#doQuery()
   * @see MCRSQLConnection#justDoQuery()
   **/
  MCRSQLRowReader( ResultSet rs )
  { this.rs   = rs; }
  
  /** 
   * Points the cursor to the next result row, returning true if there is such a next row.
   *
   * @see java.sql.ResultSet#next()
   *
   * @return true, if there was a next row; false, if the end is reached
   **/
  public boolean next()
    throws MCRPersistenceException
  {
    try{ return rs.next(); }
    catch( SQLException ex )
    { throw new MCRPersistenceException( "Could not close JDBC statement", ex ); }
  }
  
  /** 
   * Returns the value of a column in the current result row as a String, or null.
   *
   * @param index the number of the column in the result row
   * @return the String value of a column in the current result row, or null
   **/
  public String getString( int index )
    throws MCRPersistenceException
  { 
    try
    {
      String value = rs.getString( index );
      return ( rs.wasNull() ? null : value );
    }
    catch( SQLException ex )
    { throw new MCRPersistenceException( "Could not get value from JDBC result set", ex ); }
  }
  
  /** 
   * Returns the value of a column in the current result row as a String, or null.
   *
   * @param columnName the name of the column in the result row
   * @return the String value of a column in the current result row, or null
   **/
  public String getString( String columnName )
    throws MCRPersistenceException
  { 
    try
    {
      String value = rs.getString( columnName );
      return ( rs.wasNull() ? null : value );
    }
    catch( SQLException ex )
    { throw new MCRPersistenceException( "Could not get value from JDBC result set", ex ); }
  }
  
  /** 
   * Returns the value of a column in the current result rowa as an int.
   *
   * @param index the number of the column in the result row
   * @return the int value of a column in the current result row, or Integer.MIN_VALUE if the column was null
   **/
  public int getInt( int index )
    throws MCRPersistenceException
  { 
    try
    {
      int value = rs.getInt( index );
      return ( rs.wasNull() ? Integer.MIN_VALUE : value );
    }
    catch( SQLException ex )
    { throw new MCRPersistenceException( "Could not get value from JDBC result set", ex ); }
  }
  
  /** 
   * Returns the value of a column in the current result rowa as an int.
   *
   * @param columnName the name of the column in the result row
   * @return the int value of a column in the current result row, or Integer.MIN_VALUE if the column was null
   **/
  public int getInt( String columnName )
    throws MCRPersistenceException
  { 
    try
    {
      int value = rs.getInt( columnName );
      return ( rs.wasNull() ? Integer.MIN_VALUE : value );
    }
    catch( SQLException ex )
    { throw new MCRPersistenceException( "Could not get value from JDBC result set", ex ); }
  }
}
