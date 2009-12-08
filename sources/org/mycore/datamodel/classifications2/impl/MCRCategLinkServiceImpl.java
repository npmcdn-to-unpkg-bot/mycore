/**
 * 
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
package org.mycore.datamodel.classifications2.impl;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRObjectReference;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
public class MCRCategLinkServiceImpl implements MCRCategLinkService {

    private static MCRHIBConnection HIB_CONNECTION_INSTANCE;

    private static Logger LOGGER = Logger.getLogger(MCRCategLinkServiceImpl.class);

    private static Class<MCRCategoryLink> LINK_CLASS = MCRCategoryLink.class;

    private static MCRCache categCache = new MCRCache(MCRConfiguration.instance().getInt(
            "MCR.Classifications.LinkServiceImpl.CategCache.Size", 1000), "MCRCategLinkService category cache");

    private static MCRCategoryDAOImpl DAO = new MCRCategoryDAOImpl();

    public MCRCategLinkServiceImpl() {
        HIB_CONNECTION_INSTANCE = MCRHIBConnection.instance();
    }

    public Map<MCRCategoryID, Number> countLinks(MCRCategory parent, boolean childrenOnly) {
        return countLinksForType(parent, null, childrenOnly);
    }

    public Map<MCRCategoryID, Number> countLinksForType(MCRCategory parent, String type, boolean childrenOnly) {
        boolean restrictedByType = (type != null);
        String queryName;
        if (childrenOnly) {
            queryName = restrictedByType ? ".NumberByTypePerChildOfParentID" : ".NumberPerChildOfParentID";
        } else {
            queryName = restrictedByType ? ".NumberByTypePerClassID" : ".NumberPerClassID";
        }
        Map<MCRCategoryID, Number> countLinks = new HashMap<MCRCategoryID, Number>();
        Collection<MCRCategoryID> ids = childrenOnly ? getAllChildIDs(parent) : getAllCategIDs(parent);
        for (MCRCategoryID id : ids) {
            // initialize all categIDs with link count of zero
            countLinks.put(id, 0);
        }
        //have to use rootID here if childrenOnly=false
        //old classification browser/editor could not determine links correctly otherwise 
        if (!childrenOnly) {
            parent = parent.getRoot();
        } else if (!(parent instanceof MCRCategoryImpl) || ((MCRCategoryImpl) parent).getInternalID() == 0) {
            final Session session = MCRHIBConnection.instance().getSession();
            parent = MCRCategoryDAOImpl.getByNaturalID(session, parent.getId());
        }
        LOGGER.info("parentID:" + parent.getId());
        String classID = parent.getId().getRootID();
        Query q = HIB_CONNECTION_INSTANCE.getNamedQuery(LINK_CLASS.getName() + queryName);
        // query can take long time, please cache result
        q.setCacheable(true);
        q.setParameter("classID", classID);
        if (childrenOnly) {
            q.setParameter("parentID", ((MCRCategoryImpl) parent).getInternalID());
        }
        if (restrictedByType) {
            q.setParameter("type", type);
        }
        // get object count for every category (not accumulated)
        @SuppressWarnings("unchecked")
        List<Object[]> result = q.list();
        for (Object[] sr : result) {
            MCRCategoryID key = new MCRCategoryID(classID, sr[0].toString());
            Number value = (Number) sr[1];
            countLinks.put(key, value);
        }
        return countLinks;
    }

    public void deleteLink(String id) {
        Query q = HIB_CONNECTION_INSTANCE.getNamedQuery(LINK_CLASS.getName() + ".deleteByObjectID");
        q.setParameter("id", id);
        int deleted = q.executeUpdate();
        LOGGER.debug("Number of Links deleted: " + deleted);
    }

    public void deleteLinks(Collection<String> ids) {
        Query q = HIB_CONNECTION_INSTANCE.getNamedQuery(LINK_CLASS.getName() + ".deleteByObjectCollection");
        q.setParameterList("ids", ids);
        int deleted = q.executeUpdate();
        LOGGER.debug("Number of Links deleted: " + deleted);
    }

    @SuppressWarnings("unchecked")
    public Collection<String> getLinksFromCategory(MCRCategoryID id) {
        Query q = HIB_CONNECTION_INSTANCE.getNamedQuery(LINK_CLASS.getName() + ".ObjectIDByCategory");
        q.setCacheable(true);
        q.setParameter("rootID", id.getRootID());
        q.setParameter("categID", id.getID());
        return q.list();
    }

    @SuppressWarnings("unchecked")
    public Collection<String> getLinksFromCategoryForType(MCRCategoryID id, String type) {
        Query q = HIB_CONNECTION_INSTANCE.getNamedQuery(LINK_CLASS.getName() + ".ObjectIDByCategoryAndType");
        q.setCacheable(true);
        q.setParameter("rootID", id.getRootID());
        q.setParameter("categID", id.getID());
        q.setParameter("type", type);
        return q.list();
    }

    @SuppressWarnings("unchecked")
    public Collection<MCRCategoryID> getLinksFromObject(String id) {
        Query q = HIB_CONNECTION_INSTANCE.getNamedQuery(LINK_CLASS.getName() + ".categoriesByObjectID");
        q.setCacheable(true);
        q.setParameter("id", id);
        List<Object[]> result = q.list();
        ArrayList<MCRCategoryID> returns = new ArrayList<MCRCategoryID>(result.size());
        for (Object[] idValues : result) {
            returns.add(new MCRCategoryID(idValues[0].toString(), idValues[1].toString()));
        }
        return returns;
    }

    public void setLinks(MCRObjectReference objectReference, Collection<MCRCategoryID> categories) {
        Session session = HIB_CONNECTION_INSTANCE.getSession();
        for (MCRCategoryID categID : categories) {
            final MCRCategoryImpl category = getMCRCategory(session, categID);
            if (category == null)
                throw new MCRPersistenceException("Could not link to unknown category " + categID);
            MCRCategoryLink link = new MCRCategoryLink(category, objectReference);
            LOGGER.debug("Adding Link from " + link.getCategory().getId() + "(" + link.getCategory().getInternalID() + ") to "
                    + objectReference.getObjectID());
            session.save(link);
            LOGGER.debug("===DONE: " + link.id);
        }
    }

    private static MCRCategoryImpl getMCRCategory(Session session, MCRCategoryID categID) {
        MCRCategoryImpl categ = (MCRCategoryImpl) categCache.getIfUpToDate(categID, DAO.getLastModified());
        if (categ != null)
            return categ;
        categ = MCRCategoryDAOImpl.getByNaturalID(session, categID);
        if (categ == null) {
            return null;
        }
        categCache.put(categID, categ);
        return categ;
    }

    public Map<MCRCategoryID, Boolean> hasLinks(MCRCategory category) {
        if(category == null) {
            return hasLinksForClassification();
        }
        
        MCRCategoryImpl rootImpl = (MCRCategoryImpl) MCRCategoryDAOFactory.getInstance().getCategory(category.getRoot().getId(), -1);
        if (rootImpl == null) {
            //Category does not exist, so it has no links
            return getNoLinksMap(category);
        }
        HashMap<MCRCategoryID, Boolean> boolMap = new HashMap<MCRCategoryID, Boolean>();
        final BitSet linkedInternalIds = getLinkedInternalIds();
        storeHasLinkValues(boolMap, linkedInternalIds, rootImpl);
        return boolMap;
    }

    private Map<MCRCategoryID, Boolean> hasLinksForClassification() {
        HashMap<MCRCategoryID, Boolean> boolMap = new HashMap<MCRCategoryID, Boolean>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Boolean get(Object key) {
                Boolean haslink = super.get(key);
                return haslink == null ? false : haslink;
            }
        };

        Session session = MCRHIBConnection.instance().getSession();
        String queryString = "select distinct node.classid from MCRCATEGORY as node, MCRCATEGORYLINK as link where node.internalid=link.category";
        SQLQuery sqlQueryHasLink = session.createSQLQuery(queryString);
        @SuppressWarnings("unchecked")
        List<String> categList = sqlQueryHasLink.list();

        for (String rootID : categList) {
            MCRCategoryID categoryID = MCRCategoryID.rootID(rootID);
            boolMap.put(categoryID, true);
        }

        return boolMap;
    }

    private Map<MCRCategoryID, Boolean> getNoLinksMap(MCRCategory category) {
        HashMap<MCRCategoryID, Boolean> boolMap = new HashMap<MCRCategoryID, Boolean>();
        for (MCRCategoryID categID : getAllCategIDs(category)) {
            boolMap.put(categID, false);
        }
        return boolMap;
    }

    private void storeHasLinkValues(HashMap<MCRCategoryID, Boolean> boolMap, BitSet internalIds, MCRCategoryImpl parent) {
        final int internalID = parent.getInternalID();
        if (internalID < internalIds.size() && internalIds.get(internalID)) {
            addParentHasValues(boolMap, parent);
        } else {
            boolMap.put(parent.getId(), false);
        }
        for (MCRCategory child : parent.getChildren()) {
            storeHasLinkValues(boolMap, internalIds, (MCRCategoryImpl) child);
        }
    }

    private void addParentHasValues(HashMap<MCRCategoryID, Boolean> boolMap, MCRCategory parent) {
        boolMap.put(parent.getId(), true);
        if (parent.isCategory() && !boolMap.get(parent.getParent().getId()).booleanValue()) {
            addParentHasValues(boolMap, parent.getParent());
        }
    }

    private BitSet getLinkedInternalIds() {
        Session session = HIB_CONNECTION_INSTANCE.getSession();
        Criteria criteria = session.createCriteria(LINK_CLASS);
        criteria.setProjection(Projections.distinct(Projections.property("category.internalID")));
        criteria.addOrder(Order.desc("category.internalID"));
        @SuppressWarnings("unchecked")
        List<Number> result = criteria.list();
        int maxSize = result.size() == 0 ? 1 : result.get(0).intValue() + 1;
        BitSet linkSet = new BitSet(maxSize);
        for (Number internalID : result) {
            linkSet.set(internalID.intValue(), true);
        }
        return linkSet;
    }

    private static Collection<MCRCategoryID> getAllCategIDs(MCRCategory category) {
        HashSet<MCRCategoryID> ids = new HashSet<MCRCategoryID>();
        ids.add(category.getId());
        for (MCRCategory cat : category.getChildren()) {
            ids.addAll(getAllCategIDs(cat));
        }
        return ids;
    }

    private static Collection<MCRCategoryID> getAllChildIDs(MCRCategory category) {
        HashSet<MCRCategoryID> ids = new HashSet<MCRCategoryID>();
        for (MCRCategory cat : category.getChildren()) {
            ids.add(cat.getId());
        }
        return ids;
    }

    public boolean hasLink(MCRCategory mcrCategory) {
        return !hasLinks(mcrCategory).isEmpty();
    }
}
