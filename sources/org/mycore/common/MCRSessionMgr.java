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

package org.mycore.common;

import static org.mycore.common.events.MCRSessionEvent.Type.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mycore.common.events.MCRSessionListener;

/**
 * Manages sessions for a MyCoRe system. This class is backed by a ThreadLocal
 * variable, so every Thread is guaranteed to get a unique instance of
 * MCRSession. Care must be taken when using an environment utilizing a Thread
 * pool, such as many Servlet engines. In this case it is possible for the
 * session object to stay attached to a thread where it should not be. Use the
 * {@link #releaseCurrentSession()}method to reset the session object for a
 * Thread to its default values.
 * 
 * The basic idea for the implementation of this class is taken from an apache
 * project, namely the class org.apache.common.latka.LatkaProperties.java
 * written by Morgan Delagrange. Please see <http://www.apache.org/>.
 * 
 * @author Detlev Degenhardt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRSessionMgr {
    
    private static Map<String, MCRSession> sessions=new HashMap<String, MCRSession>();
    
    private static List<MCRSessionListener> listeners = new ArrayList<MCRSessionListener>();
    
    private static ReentrantReadWriteLock listenersLock = new ReentrantReadWriteLock();

    /**
     * This ThreadLocal is automatically instantiated per thread with a MyCoRe
     * session object containing the default session parameters which are set in
     * the constructor of MCRSession.
     * 
     * @see ThreadLocal
     */
    private static ThreadLocal<MCRSession> theThreadLocalSession = new ThreadLocal<MCRSession>() {
        public MCRSession initialValue() {
            return new MCRSession();
        }
    };

    /**
     * This method returns the unique MyCoRe session object for the current
     * Thread. The session object is initialized with the default MyCoRe session
     * data.
     * 
     * @return MyCoRe MCRSession object
     */
    public static synchronized MCRSession getCurrentSession() {
        return theThreadLocalSession.get();
    }

    /**
     * This method sets a MyCoRe session object for the current Thread.
     */
    public static synchronized void setCurrentSession(MCRSession theSession) {
        theSession.activate();
        theThreadLocalSession.set(theSession);
    }

    /**
     * Releases the MyCoRe session from its current thread. Subsequent calls of
     * getCurrentSession() will return a different MCRSession object than before
     * for the current Thread. One use for this method is to reset the session
     * inside a Thread-pooling environment like Servlet engines.
     */
    public static synchronized void releaseCurrentSession() {
        MCRSession session = theThreadLocalSession.get();
        session.passivate();
        MCRSession.LOGGER.debug("MCRSession released " + session.getID());
        theThreadLocalSession.remove();
    }

    /**
     * Returns the MCRSession for the given sessionID.
     */
    public static MCRSession getSession(String sessionID) {
        MCRSession s = sessions.get(sessionID);
        if (s == null) {
            MCRSession.LOGGER.warn("MCRSession with ID " + sessionID + " not cached any more");
        }
        return s;
    }
    
    static void addSession(MCRSession session) {
        sessions.put(session.getID(), session);
        session.fireSessionEvent(created);
    }
    
    static void removeSession(MCRSession session) {
        sessions.remove(session.getID());
        session.fireSessionEvent(destroyed);
    }
    
    public static void addSessionListener(MCRSessionListener listener){
        listenersLock.writeLock().lock();
        listeners.add(listener);
        listenersLock.writeLock().unlock();
    }
    
    public static void removeSessionListener(MCRSessionListener listener){
        listenersLock.writeLock().lock();
        listeners.remove(listener);
        listenersLock.writeLock().unlock();
    }
    
    static List<MCRSessionListener> getListeners(){
        return listeners;
    }
    
    static ReentrantReadWriteLock getListenersLock(){
        return listenersLock;
    }
    
}
