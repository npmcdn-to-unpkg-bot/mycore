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

package org.mycore.datamodel.classifications;

import java.util.ArrayList;
import java.util.List;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class is the manangement class for the SQL store of the classification
 * system of MyCoRe. They would only used by the MCRClassification.
 * 
 * @author Frank Luetzenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRClassificationManager {
    protected static MCRClassificationManager manager;

    /**
     * Make an instance of MCRClassificationManager.
     */
    public static MCRClassificationManager instance() {
        if (manager == null) {
            manager = new MCRClassificationManager();
        }

        return manager;
    }

    protected MCRCache categoryCache;

    protected MCRCache classificationCache;

    protected MCRCache jDomCache;

    protected MCRClassificationInterface store;

    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    /**
     * Constructor for a new MCRClassificationManager.
     */
    protected MCRClassificationManager() {
        Object object = CONFIG.getInstanceOf("MCR.classifications_store_class");
        store = (MCRClassificationInterface) object;

        int classifSize = CONFIG.getInt("MCR.classifications_classification_cache_size", 30);
        int categSize = CONFIG.getInt("MCR.classifications_category_cache_size", 500);
        classificationCache = new MCRCache(classifSize);
        categoryCache = new MCRCache(categSize);
        jDomCache = new MCRCache(categSize);

    }

    void createClassificationItem(MCRClassificationItem classification) {
        if (store.classificationItemExists(classification.getId())) {
            throw new MCRPersistenceException("Classification already exists");
        }

        store.createClassificationItem(classification);
        classificationCache.put(classification.getId(), classification);
        CONFIG.systemModified();
    }

    protected void createCategoryItem(MCRCategoryItem category) {
        if (store.categoryItemExists(category.getClassID(), category.getId())) {
            throw new MCRPersistenceException("Category " + category.getId() + " already exists");
        }

        store.createCategoryItem(category);
        categoryCache.put(getCachingID(category), category);
        CONFIG.systemModified();
    }
    
    protected void createCategoryItems(List catlist) {
        if ((catlist == null) || (catlist.size() == 0)) return;
        for (int i=0; i<catlist.size();i++) {
        MCRCategoryItem cat = (MCRCategoryItem) catlist.get(i);
        createCategoryItem(cat);
        createCategoryItems(cat.getCategories());
        }
    }

    public final MCRClassificationItem retrieveClassificationItem(String ID) {
        MCRClassificationItem c = (MCRClassificationItem) (classificationCache.get(ID));

        if (c == null) {
            c = store.retrieveClassificationItem(ID);

            if (c != null) {
                classificationCache.put(ID, c);
            }
        }

        return c;
    }

    MCRCategoryItem retrieveCategoryItem(String classifID, String categID) {
        String cachingID = classifID + "@@" + categID;
        MCRCategoryItem c = (MCRCategoryItem) (categoryCache.get(cachingID));

        if (c == null) {
            c = store.retrieveCategoryItem(classifID, categID);

            if (c != null) {
                categoryCache.put(cachingID, c);
            }
        }

        return c;
    }

    MCRCategoryItem retrieveCategoryItemForLabelText(String classifID, String labeltext) {
        MCRCategoryItem c = store.retrieveCategoryItemForLabelText(classifID, labeltext);

        return c;
    }

    MCRCategoryItem[] retrieveChildren(String classifID, String parentID) {
        ArrayList retrieved = store.retrieveChildren(classifID, parentID);
        MCRCategoryItem[] children = new MCRCategoryItem[retrieved.size()];

        for (int i = 0; i < children.length; i++) {
            MCRCategoryItem cRetrieved = (MCRCategoryItem) (retrieved.get(i));
            String cachingID = getCachingID(cRetrieved);
            MCRCategoryItem cFromCache = (MCRCategoryItem) (categoryCache.get(cachingID));

            if (cFromCache != null) {
                children[i] = cFromCache;
            } else {
                children[i] = cRetrieved;
                categoryCache.put(cachingID, cRetrieved);
            }
        }

        return children;
    }

    int retrieveNumberOfChildren(String classifID, String parentID) {
        return store.retrieveNumberOfChildren(classifID, parentID);
    }

    protected String getCachingID(MCRCategoryItem category) {
        return category.getClassID() + "@@" + category.getId();
    }

    protected String[] getAllClassificationID() {
        return store.getAllClassificationID();
    }

    protected MCRClassificationItem[] getAllClassification() {
        return store.getAllClassification();
    }

    protected void deleteClassificationItem(MCRObjectID classifID) {
        deleteClassificationItem(classifID.getId());
    }
    
    protected void deleteClassificationItem(String classifID) {
        classificationCache.remove(classifID);
        jDomCache.remove(classifID);
        store.deleteClassificationItem(classifID);
        CONFIG.systemModified();
    }

    protected void deleteCategoryItem(MCRObjectID classifID, String categID) {
        deleteCategoryItem(classifID.getId(),categID);
    }
    
    protected void deleteCategoryItem(String classifID, String categID) {
        categoryCache.remove(classifID + "@@" + categID);
        jDomCache.remove(classifID + "@@" + categID);
        store.deleteCategoryItem(classifID, categID);
        CONFIG.systemModified();
    }

}
