package org.mycore.backend.xmldb;

import org.xmldb.api.*;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.*;

/**
 * This is the implementation of Tools for the XML:DB API
 *
 * @author marc schluepmann
 * @author harald richter
 * @version $Revision$ $Date$
 **/
public final class MCRXMLDBTools {
     static Logger logger;
     static String conf_prefix = "MCR.persistence_xmldb_";
     static String driver      = "";
     static String connString  = "";
     static String database    = "";
     private static MCRConfiguration conf = null;
     
static
  {
   logger     = Logger.getLogger( MCRXMLDBTools.class.getName() );   
   conf       = MCRConfiguration.instance();
   driver     = conf.getString( conf_prefix + "driver" );
   logger.info("MCRXMLDBTools MCR.persistence_xmldb_driver      : " + driver); 
   connString = conf.getString( conf_prefix + "database_url" , "");
   logger.info("MCRXMLDBTools MCR.persistence_xmldb_database_url: " + connString); 
   database   = conf.getString( conf_prefix + "database" , "");
   logger.info("MCRXMLDBTools MCR.persistence_xmldb_database    : " + database); 
  }
     
    /**
     * This method returns the root collection of a given connection.
     *
     * @param driver name of the XML:DB driver
     * @param connection the connection string
     *
     * @throws ClassNotFoundException if driver class could not be found
     * @throws IllegalAccessException if you have no access to database
     * @throws InstantiationException if driver class could not be instantiated
     * @throws XMLDBException if an XML:DB problem occurs
     **/
    public static Collection getRootCollection( String driver,
						String connection )
	throws ClassNotFoundException,
	       IllegalAccessException,
	       InstantiationException,
	       XMLDBException {
	Class driverclass = Class.forName( driver );
	Database database = (Database)driverclass.newInstance();
	DatabaseManager.registerDatabase( database );
	return DatabaseManager.getCollection( connection );
    }

    /**
     * Closes all child collections of the given collection
     * recursively and the collection itself.
     *
     * @param collection the collection whose child collections shall
     * be closed
     *
     * @throws XMLDBException a XMLDBException
     **/
    public static void safelyClose( Collection collection )
    throws XMLDBException {
  	    if( collection != null && collection.isOpen() )
		collection.close();
    }

    /**
     * Looks under given collection if a collection with the given
     * name exists. If not the collection will be generated if create
     * parameter is true.
     * @param parent the collection to look under
     * @param name collection name to look for
     * @param create creates collection if it is not there
     * @return the collection found or the newly created one, null if
     * collection was not found and should not be created
     **/
    public static Collection collectionExistsUnder( Collection parent, String name, boolean create ) throws XMLDBException {
	Collection new_coll;
	if( (new_coll = parent.getChildCollection( name ) ) == null && create ) {
	    CollectionManagementService cms = 
		(CollectionManagementService) parent.getService(
								"CollectionManagementService",
								"1.0" );
	    new_coll = cms.createCollection( name );
	}
	return new_coll;
    }
    
    /**
     * Get driver name for XML:DB database
     **/
    public static String getDriverName( ) {
	return driver;
    }
    
    /**
     * Get connection string for XML:DB database
     **/
    public static String getConnectString( ) {
	return connString;
    }
    
    /**
     * Handle query string for XML:DB database
     **/
    public static String handleQueryString( String query, String type ) {
        logger.debug("MCRXMLDBTools handlequerstring   (old)  : " + query + " type : " + type); 
        
        if ( database.equals( "xindice" ) )
          query = handleQueryStringXindice( query, type );
        else if ( database.equals( "exist" ) )
          query = handleQueryStringExist( query, type );
        
        logger.debug("MCRXMLDBTools handlequerstring   (new)  : " + query); 
	return query;
    }
    
    /**
     * Handle query string for Xindice
     **/
    static String handleQueryStringXindice( String query, String type ) {
	return query;
    }
    
    /**
     * Handle query string for exist
     **/
    static String handleQueryStringExist( String query, String type ) {
// with exist dev version from 03/07/03 no longer needed        
//        if ( query.equals( "/*" ))
//          query = "xcollection('/db/mycore/" + type + "')/mycoreobject";
//        query = MCRUtils.replaceString(query, "like", "=");
// a lot of queries of mycore sample (document and legal entity) work!!
        query = MCRUtils.replaceString(query, "like", "&=");
//        query = MCRUtils.replaceString(query, "contains(", "contains(.,");
        query = MCRUtils.replaceString(query, ")", "");
        query = MCRUtils.replaceString(query, "contains(", ".&=");
        query = MCRUtils.replaceString(query, "metadata/*/*/@href=", "metadata//*/@xlink:href=");
        if ( -1 != query.indexOf("] and") )
        {
          query = MCRUtils.replaceString(query, "[", "/");
          query = MCRUtils.replaceString(query, "] and", " and");
          query = "//*[" + query; 
        }
        query = MCRUtils.replaceString(query, "/mycoreobject/", "/");
	return query;
    }
    
}
