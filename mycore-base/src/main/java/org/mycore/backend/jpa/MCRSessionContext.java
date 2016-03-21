/**
 * 
 * $Revision: 33146 $ $Date: 2015-08-11 16:07:48 +0200 (Di, 11 Aug 2015) $
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
package org.mycore.backend.jpa;

import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;

/**
 * A helper class which scopes the notion of
 * current session by the current {@link MCRSession}. This implementation allows a thread of a
 * {@link MCRSession} to keep the {@link Session} open for a long conversation.
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision: 33146 $ $Date: 2011-04-05 10:52:04 +0200 (Di, 05 Apr
 *          2011) $
 * @since 2016.03
 */
class MCRSessionContext implements MCRSessionListener {

    private static final Logger LOGGER = Logger.getLogger(MCRSessionContext.class);

    private EntityManagerFactory factory;

    private ThreadLocal<EntityManager> context;

    public MCRSessionContext(EntityManagerFactory factory) {
        this.factory = factory;
        this.context = new ThreadLocal<>();
        MCRSessionMgr.addSessionListener(this);
    }

    public void sessionEvent(MCRSessionEvent event) {
        MCRSession mcrSession = event.getSession();
        EntityManager currentEntityManager;
        switch (event.getType()) {
            case activated:
                if (event.getConcurrentAccessors() <= 1) {
                    LOGGER.debug("First Thread to access " + mcrSession);
                }
                break;
            case passivated:
                currentEntityManager = unbind();
                autoCloseSession(currentEntityManager);
                break;
            case destroyed:
                currentEntityManager = unbind();
                autoCloseSession(currentEntityManager);
                break;
            case created:
                break;
            default:
                break;
        }
    }

    private EntityManager unbind() {
        EntityManager entityManager = context.get();
        context.set(null);
        return entityManager;
    }

    EntityManager getCurrentEntityManager() {
        return Optional
            .ofNullable(context.get())
            .filter(EntityManager::isOpen)
            .filter(MCRSessionContext::hibernateSessionIsOpen)
            .orElseGet(this::createEntityManagerInContext);
    }

    private static boolean hibernateSessionIsOpen(EntityManager entityManager) {
        try {
            return entityManager.unwrap(Session.class).isOpen();
        } catch (PersistenceException e) {
            //entityManager is not from hibernate;
            return true;
        }
    }

    /**
     * Closes Session if Session is still open.
     */
    private void autoCloseSession(EntityManager currentEntityManager) {
        if (currentEntityManager != null && currentEntityManager.isOpen() && hibernateSessionIsOpen(currentEntityManager)) {
            LOGGER.debug("Autoclosing current JPA EntityManager");
            currentEntityManager.close();
        }
    }

    private EntityManager createEntityManagerInContext() {
        // creates a new one
        LOGGER.debug("Obtaining new entity manager.");
        EntityManager entityManager = factory.createEntityManager();
        LOGGER.debug("Returning entity manager with transaction: " + entityManager.getTransaction());
        LOGGER.debug("Session is open: " + entityManager.unwrap(Session.class).isOpen());
        context.set(entityManager);
        return entityManager;
    }

}
