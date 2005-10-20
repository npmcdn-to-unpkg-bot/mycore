/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.access;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRXMLTableManager;

/**
 * The purpose of this interface is to make the choice of the persistence layer
 * configurable. Any concrete database-class which stores MyCoRe Access control
 * must implement this interface. Which database actually will be used can then
 * be configured by reading the value <code>MCR.accessstore_class_name</code>
 * from mycore.properties.access
 * 
 * @author Arne Seifert
 * @version $Revision$ $Date$
 */
public abstract class MCRAccessStore {
    public abstract void createTables();

    public abstract String getRuleID(String objID, String ACPool);

    public abstract void createAccessDefinition(MCRRuleMapping accessdata);

    public abstract void deleteAccessDefinition(MCRRuleMapping accessdata);

    public abstract void updateAccessDefinition(MCRRuleMapping accessdata);

    public abstract MCRRuleMapping getAccessDefinition(String ruleid, String pool, String objid);
    
    public abstract ArrayList getMappedObjectId(String pool); // ArrayList with ObjID's as String
    
    public abstract ArrayList getPoolsForObject(String objid); // ArrayList with pools as String
   
    public abstract ArrayList getDatabasePools();
    
    public static Logger logger = Logger.getLogger(MCRAccessStore.class.getName());
    
    public static Hashtable types = getTypes();

    final protected static String sqlDateformat = "yyyy-MM-dd HH:mm:ss";

    final protected static String SQLAccessCtrlRule = MCRConfiguration.instance().getString("MCR.access_store_sql_table_rule", "MCRACCESSRULE");

    final protected static String SQLAccessCtrlMapping = MCRConfiguration.instance().getString("MCR.access_store_sql_table_map", "MCRACCESS");

    final protected static String AccessPools = MCRConfiguration.instance().getString("MCR.AccessPools", "");

    static private MCRAccessStore implementation;

    public static MCRAccessStore getInstance() {
        try {
            if (implementation == null) {
                implementation = (MCRAccessStore) MCRConfiguration.instance().getSingleInstanceOf("MCR.accessstore_class_name", "org.mycore.backend.sql.MCRSQLAccessStore");
            }
        } catch (Exception e) {
            logger.error(e);
        }

        return implementation;
    }
    
    public static List getPools(){
        try{
            List ret = new LinkedList();
            String[] pool = AccessPools.split(",");
            
            for (int i=0; i<pool.length; i++){
                ret.add(pool[i]);
            }
            return ret;
        }catch(Exception e){
            logger.error(e);
            return null;
        }
    }
    
    public static Hashtable getTypes(){
        try{
            Hashtable map = new Hashtable();
            final String prefix = "MCR.type_";
            Properties prop = MCRConfiguration.instance().getProperties(prefix);
            Enumeration names = prop.propertyNames();
            
            while (names.hasMoreElements()) {
                String name = (String) (names.nextElement());
                String name_in = name + "_in";
                
                if (MCRConfiguration.instance().getBoolean(name) && MCRConfiguration.instance().getProperties().containsKey(name_in)) {
                    map.put(name.substring(prefix.length()),MCRConfiguration.instance().getString(prefix+MCRConfiguration.instance().getString(name_in)));
                }
            }
            return map;
        }catch(Exception e){
            logger.error("Typeloading failed: "+e);
            return null;
        }
    }
    
    /**
     * alle Elemente eines Datentypes aufbereiten
     * @param type
     * @return
     */
    public List getDefinition(String type) {
        try{
            Hashtable sqlDefinition = new Hashtable();
            List pools = MCRAccessStore.getInstance().getDatabasePools();
            //merge pools
            pools.removeAll(getPools());
            pools.addAll(getPools());

            for(int i=0; i<pools.size(); i++){
                sqlDefinition.put((String)pools.get(i),MCRAccessStore.getInstance().getMappedObjectId((String)pools.get(i)));
            }
               
            List ret = new LinkedList();
            List elements = new LinkedList();
            MCRAccessDefinition def = null;
            
            if (types.get(type).equals("true")){
                elements = MCRXMLTableManager.instance().retrieveAllIDs(type);
            }else{
                elements = MCRXMLTableManager.instance().retrieveAllIDs((String)types.get(type));
            }
            
            for (int i=0; i<elements.size(); i++){
                def =  new MCRAccessDefinition();
                def.setObjID((String)elements.get(i));
                for(int j=0; j<pools.size(); j++){
                    List l = (List) sqlDefinition.get((String)pools.get(j));
                    if (l.contains((String)elements.get(i))){
                        def.addPool((String)pools.get(j),"X");
                    }else{
                        def.addPool((String)pools.get(j)," ");
                    }
                }
                ret.add(def);
            }
            return ret;
        }catch(Exception e){
            logger.error("definition loading failed: ");
            return null;
        }
    }
    
    public List getRules(String objid){
        try{
            List pools = MCRAccessStore.getInstance().getDatabasePools();
            //merge pools
            pools.removeAll(getPools());
            pools.addAll(getPools());

            List ret = new LinkedList();
            //List elements = new LinkedList();
            MCRAccessDefinition def = new MCRAccessDefinition();
            def.setObjID(objid);
            for(int j=0; j<pools.size(); j++){
                String rule = getRuleID(objid, (String)pools.get(j));
                if (rule!=null){
                    def.addPool((String)pools.get(j),rule);
                }else{
                    def.addPool((String)pools.get(j)," ");
                }
            }
            ret.add(def);
            return ret;
        }catch(Exception e){
            logger.error("definition loading failed: ");
            return null;
        }
    }

}
