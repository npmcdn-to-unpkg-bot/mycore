/*
 * 
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

package org.mycore.backend.jpa.access;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.mycore.access.mcrimpl.MCRAccessRule;
import org.mycore.access.mcrimpl.MCRRuleStore;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * JPA implementation for RuleStore, storing access rules
 * 
 * @author Arne Seifert
 * @author Thomas Scheffler (yagee)
 */
public class MCRJPARuleStore extends MCRRuleStore {
    private static final Logger LOGGER = LogManager.getLogger();

    private static int CACHE_SIZE = MCRConfiguration.instance().getInt("MCR.AccessPool.CacheSize", 2048);

    private static LoadingCache<String, MCRAccessRule> ruleCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE)
        .build(new CacheLoader<String, MCRAccessRule>() {
            @Override
            public MCRAccessRule load(String ruleid) {
                Session session = MCRHIBConnection.instance().getSession();
                MCRAccessRule rule = null;
                MCRACCESSRULE hibrule = (MCRACCESSRULE) session.createCriteria(MCRACCESSRULE.class)
                    .add(Restrictions.eq("rid", ruleid)).uniqueResult();
                LOGGER.debug("Getting MCRACCESSRULE done");

                if (hibrule != null) {
                    LOGGER.debug("new MCRAccessRule");
                    rule = new MCRAccessRule(ruleid, hibrule.getCreator(), hibrule.getCreationdate(),
                        hibrule.getRule(),
                        hibrule.getDescription());
                    LOGGER.debug("new MCRAccessRule done");
                }
                return rule;
            }
        });

    /**
     * Method creates new rule in database by given rule-object
     * 
     * @param rule
     *            as MCRAccessRule
     */
    @Override
    public void createRule(MCRAccessRule rule) {

        if (!existsRule(rule.getId())) {
            Session session = MCRHIBConnection.instance().getSession();
            MCRACCESSRULE hibrule = new MCRACCESSRULE();

            DateFormat df = new SimpleDateFormat(sqlDateformat, Locale.ROOT);
            hibrule.setCreationdate(Timestamp.valueOf(df.format(rule.getCreationTime())));
            hibrule.setCreator(rule.getCreator());
            hibrule.setRid(rule.getId());
            hibrule.setRule(rule.getRuleString());
            hibrule.setDescription(rule.getDescription());
            session.saveOrUpdate(hibrule);
        } else {
            LOGGER.error("rule with id '" + rule.getId() + "' can't be created, rule still exists.");
        }
    }

    /**
     * Method retrieves the ruleIDs of rules, whose string-representation starts with given data
     */
    @Override
    @SuppressWarnings("unchecked")
    public Collection<String> retrieveRuleIDs(String ruleExpression, String description) {
        ArrayList<String> ret = new ArrayList<String>();
        Session session = MCRHIBConnection.instance().getSession();
        List<MCRACCESSRULE> l = session.createCriteria(MCRACCESSRULE.class)
            .add(Restrictions.like("rule", ruleExpression)).add(Restrictions.like("description", description)).list();
        for (MCRACCESSRULE aL : l) {
            ret.add(aL.getRid());
        }
        return ret;
    }

    /**
     * Method updates accessrule by given rule. internal: get rule object from session set values, update via session
     */
    @Override
    public void updateRule(MCRAccessRule rule) {
        Session session = MCRHIBConnection.instance().getSession();
        MCRACCESSRULE hibrule = (MCRACCESSRULE) session.get(MCRACCESSRULE.class, rule.getId());

        DateFormat df = new SimpleDateFormat(sqlDateformat, Locale.ROOT);
        hibrule.setCreationdate(Timestamp.valueOf(df.format(rule.getCreationTime())));
        hibrule.setCreator(rule.getCreator());
        hibrule.setRule(rule.getRuleString());
        hibrule.setDescription(rule.getDescription());
        ruleCache.put(rule.getId(), rule);
    }

    /**
     * Method deletes accessrule for given ruleid
     */
    @Override
    public void deleteRule(String ruleid) {
        Session session = MCRHIBConnection.instance().getSession();
        session.createQuery("delete MCRACCESSRULE where RID = '" + ruleid + "'").executeUpdate();
        ruleCache.invalidate(ruleid);
    }

    /**
     * Method returns MCRAccessRule by given id
     * 
     * @param ruleid
     *            as string
     * @return MCRAccessRule
     */
    @Override
    public MCRAccessRule getRule(String ruleid) {
        return ruleCache.getUnchecked(ruleid);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<String> retrieveAllIDs() {
        Session session = MCRHIBConnection.instance().getSession();
        return session.createCriteria(MCRACCESSRULE.class).setProjection(Projections.id()).list();
    }

    /**
     * Method checks existance of rule in db
     * 
     * @param ruleid
     *            id as string
     * @return boolean value
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean existsRule(String ruleid) throws MCRException {
        if (ruleCache.getIfPresent(ruleid) != null) {
            return true;
        }

        Session session = MCRHIBConnection.instance().getSession();
        List<MCRACCESSRULE> l = session.createCriteria(MCRACCESSRULE.class).add(Restrictions.eq("rid", ruleid)).list();
        return l.size() == 1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int getNextFreeRuleID(String prefix) {
        int ret = 1;
        Session session = MCRHIBConnection.instance().getSession();
        List<String> l = session.createQuery("select max(rid) from MCRACCESSRULE where rid like '" + prefix + "%'")
            .list();
        if (l.size() > 0) {
            String max = l.get(0);
            if (max == null) {
                ret = 1;
            } else {
                int lastNumber = Integer.parseInt(max.substring(prefix.length()));
                ret = lastNumber + 1;
            }
        } else {
            return 1;
        }
        return ret;
    }
}
