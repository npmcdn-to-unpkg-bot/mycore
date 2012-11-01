/*
 * $Revision$ $Date$ This
 * file is part of M y C o R e See http://www.mycore.de/ for details. This
 * program is free software; you can use it, redistribute it and / or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation; either version 2 of the License or (at your option)
 * any later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details. You should have received a copy of the GNU
 * General Public License along with this program, in a file called gpl.txt or
 * license.txt. If not, write to the Free Software Foundation Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307 USA
 */

package org.mycore.services.fieldquery;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandler;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.data2fields.MCRData2FieldsContent;
import org.mycore.services.fieldquery.data2fields.MCRData2FieldsDerivate;
import org.mycore.services.fieldquery.data2fields.MCRData2FieldsFile;
import org.mycore.services.fieldquery.data2fields.MCRData2FieldsObject;
import org.mycore.services.fieldquery.data2fields.MCRIndexEntry;

/**
 * Abstract base class for searchers and indexers. Searcher implementations for
 * a specific backend must be implemented as a subclass. This class implements
 * MCREventHandler. Indexers can easily be implemented by overwriting the two
 * methods addToIndex and removeFromIndex. Searchers are implemented by
 * overwriting the method search. Searchers that do not need indexing or do this
 * on their own can simply ignore the add/remove methods.
 * 
 * @author Frank Lützenkirchen
 */
public abstract class MCRSearcher extends MCREventHandlerBase implements MCREventHandler {
    /** The logger */
    public static Logger LOGGER = Logger.getLogger(MCRSearcher.class);

    /** The unique searcher ID for this MCRSearcher implementation */
    protected String ID;

    /** The prefix of all properties in mycore.properties for this searcher */
    protected String prefix;

    /** The ID of the index this searcher handles * */
    protected String index;

    /**
     * Returns false if this MCRSearcher implements only search and is therefore read-only.
     */
    public abstract boolean isIndexer();

    /**
     * Initializes the searcher and sets its unique ID.
     * 
     * @param ID
     *            the non-null unique ID of this searcher instance
     */
    public void init(String ID) {
        this.ID = ID;
        prefix = "MCR.Searcher." + ID + ".";
        index = MCRConfiguration.instance().getString(prefix + "Index");
    }

    /**
     * Returns the unique store ID that was set for this store instance
     * 
     * @return the unique store ID that was set for this store instance
     */
    public String getID() {
        return ID;
    }

    /**
     * Returns the ID of the index this searcher is configured for.
     */
    public String getIndex() {
        return index;
    }

    @Override
    protected void handleFileCreated(MCREvent evt, MCRFile file) {
        try {
            MCRIndexEntry entry = new MCRData2FieldsFile(index, file).buildIndexEntry();
            addToIndex(entry);
        } catch (Exception e) {
            throw new MCRException(e);
        }
    }

    @Override
    protected void handleFileUpdated(MCREvent evt, MCRFile file) {
        try {
            MCRIndexEntry entry = new MCRData2FieldsFile(index, file).buildIndexEntry();
            removeFromIndex(entry);
            addToIndex(entry);
        } catch (Exception e) {
            throw new MCRException(e);
        }
    }

    @Override
    protected void handleFileDeleted(MCREvent evt, MCRFile file) {
        String entryID = file.getID();
        removeFromIndex(entryID);
    }

    @Override
    protected void handleFileRepaired(MCREvent evt, MCRFile file) {
        handleFileUpdated(evt, file);
    }

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        MCRIndexEntry entry = getEntry(obj, getContentFromEvent(evt));
        addToIndex(entry);
    }

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        MCRIndexEntry entry = getEntry(der, getContentFromEvent(evt));
        addToIndex(entry);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        MCRIndexEntry entry = getEntry(obj, getContentFromEvent(evt));
        removeFromIndex(entry);
        addToIndex(entry);
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        MCRIndexEntry entry = getEntry(der, getContentFromEvent(evt));
        removeFromIndex(entry);
        addToIndex(entry);
    }

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        removeFromIndex(obj.getId());
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        String entryID = der.getId().toString();
        removeFromIndex(entryID);
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        handleObjectUpdated(evt, obj);
    }

    @Override
    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate der) {
        handleDerivateUpdated(evt, der);
    }

    @Override
    protected void undoObjectCreated(MCREvent evt, MCRObject obj) {
        removeFromIndex(obj.getId());
    }

    @Override
    protected void undoDerivateCreated(MCREvent evt, MCRDerivate der) {
        handleDerivateDeleted(evt, der);
    }

    @Override
    protected void undoObjectDeleted(MCREvent evt, MCRObject obj) {
        handleObjectCreated(evt, obj);
    }

    @Override
    protected void undoDerivateDeleted(MCREvent evt, MCRDerivate der) {
        handleDerivateCreated(evt, der);
    }

    private MCRContent getContentFromEvent(MCREvent evt) {
        return (MCRContent) evt.get("content");
    }

    protected MCRIndexEntry getEntry(MCRBase base, MCRContent content) {
        MCRIndexEntry entry = null;
        if (content != null) {
            LOGGER.info("Using event stored content to index: " + base.getId());
            entry = new MCRData2FieldsContent(index, content, base.getId()).buildIndexEntry();
        } else if (base instanceof MCRObject) {
            entry = new MCRData2FieldsObject(index, (MCRObject) base).buildIndexEntry();
        } else if (base instanceof MCRDerivate) {
            entry = new MCRData2FieldsDerivate(index, (MCRDerivate) base).buildIndexEntry();
        }
        return entry;
    }

    protected void removeFromIndex(MCRObjectID id) {
        String entryID = id.toString();
        removeFromIndex(entryID);
    }

    /**
     * Adds field values to the search index. Searchers that need an indexer
     * must overwrite this method to store the values in their backend index. If
     * this class is configured as event handler, this method is automatically
     * called when objects are created or updated. The field values have been
     * extracted from the object's data as defined by searchfields.xml
     * 
     * @param entryID
     *            the unique ID of this entry in the index
     * @param returnID
     *            the ID to return as result of a search (MCRHit ID)
     * @param fields
     *            a List of MCRFieldValue objects
     */
    public void addToIndex(String entryID, String returnID, List<MCRFieldValue> fields) {
    }

    public void addToIndex(MCRIndexEntry entry) {
        addToIndex(entry.getEntryID(), entry.getReturnID(), entry.getFieldValues());
    }

    /**
     * Removes the values of the given entry from the backend index. Searchers
     * that need an indexer must overwrite this method to delete the values in
     * their backend index. If this class is configured as event handler, this
     * method is automatically called when objects are deleted or updated.
     * 
     * @param entryID
     *            the unique ID of this entry in the index
     */
    public void removeFromIndex(String entryID) {
    }

    public void removeFromIndex(MCRIndexEntry entry) {
        removeFromIndex(entry.getEntryID());
    }

    /**
     * Executes a query on this searcher. The query MUST only refer to fields
     * that are managed by this searcher.
     * 
     * @param condition
     *            the query condition
     * @param maxResults
     *            the maximum number of results to return, 0 means all results
     * @param sortBy
     *            a not-null list of MCRSortBy sort criteria. The list is empty
     *            if the results should not be sorted
     * @param addSortData
     *            if false, backend should sort results itself while executing
     *            the query. If this is not possible or the parameter is true,
     *            backend should not sort the results itself, but only store the
     *            data of the fields in the sortBy list which are needed to sort
     *            later
     * @return the query results
     */
    public abstract MCRResults search(MCRCondition condition, int maxResults, List<MCRSortBy> sortBy, boolean addSortData);

    /**
     * Adds field values needed for sorting for those hits that do not have sort
     * data set already. Subclasses must overwrite this method, otherwise
     * sorting results will not always work correctly. The default
     * implementation in this class does nothing.
     * 
     * @param hits
     *            the MCRHit objects that do not have sort data set
     * @param sortBy
     *            the MCRFieldDef fields that are sort criteria
     */
    public void addSortData(Iterator<MCRHit> hits, List<MCRSortBy> sortBy) {
    }

    /**
     * Removes all entries from index.
     */
    public void clearIndex() {
    }

    /**
     * Removes all entries of a field with a given value from index.
     */
    public void clearIndex(String fieldname, String value) {
    }

    /**
     * Inform Searcher what is going on. Searcher can use this to speed up
     * indexing. MCRLuceneSearcher for example uses a Ramdirectory rebuild
     * insert ... finish
     */
    public void notifySearcher(String mode) {
    }

}
