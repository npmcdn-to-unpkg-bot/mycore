package org.mycore.iview2.services;

import java.util.AbstractQueue;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;

public class MCRTilingQueue extends AbstractQueue<MCRTileJob> implements Closeable {
    private static MCRTilingQueue instance = new MCRTilingQueue();

    private static ScheduledExecutorService StalledJobScheduler;

    private static Logger LOGGER = Logger.getLogger(MCRTilingQueue.class);

    private final ReentrantLock pollLock;

    private boolean running;

    private MCRTilingQueue() {
        // periodische Ausführung von runProcess
        int waitTime = Integer.parseInt(MCRIView2Tools.getIView2Property("TimeTillReset")) * 60;
        StalledJobScheduler = Executors.newSingleThreadScheduledExecutor();
        StalledJobScheduler.scheduleAtFixedRate(MCRStalledJobResetter.getInstance(), waitTime, waitTime, TimeUnit.SECONDS);
        running = true;
        pollLock = new ReentrantLock();
        MCRShutdownHandler.getInstance().addCloseable(this);
    }

    /**
     * @return singleton instance of this class
     */
    public static MCRTilingQueue getInstance() {
        if (!instance.running)
            return null;
        return instance;
    }

    /**
     * @return next available tile job instance
     */
    public MCRTileJob poll() {
        if (!running)
            return null;
        try {
            pollLock.lock();
            MCRTileJob job = getElement();
            if (job != null) {
                job.setStart(new Date(System.currentTimeMillis()));
                job.setStatus(MCRJobState.PROCESSING);
                if (!updateJob(job)) {
                    job = null;
                }
            }
            return job;
        } finally {
            pollLock.unlock();
        }
    }

    /**
     * removes next job.
     * same as {@link #poll()} but never returns null
     * @throws NoSuchElementException if {@link #poll()} would return null
     */
    @Override
    public MCRTileJob remove() throws NoSuchElementException {
        if (!running)
            return null;
        MCRTileJob job = poll();
        if (job == null) {
            throw new NoSuchElementException();
        }
        return job;
    }

    /**
     * get next job without modifying it state to {@link MCRJobState#PROCESSING} 
     */
    public MCRTileJob peek() {
        if (!running)
            return null;
        MCRTileJob job = getElement();
        return job;
    }

    /**
     * removes next job.
     * same as {@link #peek()} but never returns null
     * @throws NoSuchElementException if {@link #peek()} would return null
     */
    @Override
    public MCRTileJob element() throws NoSuchElementException {
        if (!running)
            return null;
        MCRTileJob job = peek();
        if (job == null) {
            throw new NoSuchElementException();
        }
        return job;
    }

    /**
     * adds <code>job</code> to queue.
     * alters date added to current time and status of job to {@link MCRJobState#NEW}
     */
    public boolean offer(MCRTileJob job) {
        if (!running)
            return false;
        MCRTileJob oldJob = getJob(job.getDerivate(), job.getPath());
        if (oldJob != null) {
            job = oldJob;
        } else {
            job.setAdded(new Date());
        }
        job.setStatus(MCRJobState.NEW);
        job.setStart(null);
        if (addJob(job)) {
            notifyListener();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Deletes all tile jobs no matter what the current state is.
     */
    @Override
    public void clear() {
        if (!running)
            return;
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("DELETE FROM MCRTileJob");
        query.executeUpdate();
    }

    /**
     * iterates of jobs of status {@link MCRJobState#NEW}
     * 
     * does not change the status.
     */
    @Override
    public Iterator<MCRTileJob> iterator() {
        if (!running) {
            List<MCRTileJob> empty = Collections.emptyList();
            return empty.iterator();
        }
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("FROM MCRTileJob WHERE status='" + MCRJobState.NEW.toChar() + "' ORDER BY added ASC");
        @SuppressWarnings("unchecked")
        List<MCRTileJob> result = query.list();
        return result.iterator();
    }

    /**
     * returns the current size of this queue
     */
    @Override
    public int size() {
        if (!running)
            return 0;
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("SELECT count(*) FROM MCRTileJob WHERE status='" + MCRJobState.NEW.toChar() + "'");
        return ((Number) query.iterate().next()).intValue();
    }

    /**
     * get the specific job and alters it status to {@link MCRJobState#PROCESSING}
     * @param derivate
     * @param path
     * @return
     * @throws NoSuchElementException
     */
    public MCRTileJob getElementOutOfOrder(String derivate, String path) throws NoSuchElementException {
        if (!running)
            return null;
        MCRTileJob job = getJob(derivate, path);
        if (job == null)
            return null;
        job.setStart(new Date(System.currentTimeMillis()));
        job.setStatus(MCRJobState.PROCESSING);
        if (!updateJob(job)) {
            throw new NoSuchElementException();
        }
        return job;
    }

    private MCRTileJob getJob(String derivate, String path) {
        if (!running)
            return null;
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("FROM MCRTileJob WHERE  derivate= :derivate AND path = :path");
        query.setParameter("derivate", derivate);
        query.setParameter("path", path);
        @SuppressWarnings("unchecked")
        Iterator<MCRTileJob> results = query.iterate();
        if (!results.hasNext())
            return null;
        MCRTileJob job = results.next();
        return job;
    }

    @SuppressWarnings("unchecked")
    private MCRTileJob getElement() {
        if (!running)
            return null;
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("FROM MCRTileJob WHERE status='" + MCRJobState.NEW.toChar() + "' ORDER BY added ASC")
                .setMaxResults(1);
        Iterator<MCRTileJob> queryResult;
        queryResult = query.iterate();
        if (queryResult.hasNext()) {
            return (MCRTileJob) queryResult.next();
        }
        return null;
    }

    private boolean updateJob(MCRTileJob job) {
        if (!running)
            return false;
        Session session = MCRHIBConnection.instance().getSession();
        session.update(job);
        return true;
    }

    private boolean addJob(MCRTileJob job) {
        if (!running)
            return false;
        Session session = MCRHIBConnection.instance().getSession();
        session.save(job);
        return true;
    }

    /**
     * every attached listener is informed that something happened to the state of the queue.
     */
    public synchronized void notifyListener() {
        this.notifyAll();
    }

    /**
     * removes specific job from queue no matter what its current status is.
     * @param derivate ID of derivate
     * @param path absolute image path
     * @return the number of jobs deleted
     */
    public int remove(String derivate, String path) {
        if (!running)
            return 0;
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("DELETE FROM " + MCRTileJob.class.getName() + " WHERE derivate = :derivate AND path = :path");
        query.setParameter("derivate", derivate);
        query.setParameter("path", path);
        return query.executeUpdate();
    }

    /**
     * removes all jobs from queue for that <code>derivate</code> its current status is.
     * @param derivate ID of derivate
     * @return the number of jobs deleted
     */
    public int remove(String derivate) {
        if (!running)
            return 0;
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("DELETE FROM " + MCRTileJob.class.getName() + " WHERE derivate = :derivate");
        query.setParameter("derivate", derivate);
        return query.executeUpdate();
    }

    /**
     * Shuts down {@link MCRStalledJobResetter} and does not alter any job anymore.
     */
    public void prepareClose() {
        StalledJobScheduler.shutdownNow();
        running = false;
        try {
            StalledJobScheduler.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.info("Could not wait for 60 seconds...");
            StalledJobScheduler.shutdownNow();
        }
    }

    /**
     * does nothing
     */
    public void close() {
        //nothing to be done in this phase
    }

    /**
     * @return "MCRTilingQueue"
     */
    @Override
    public String toString() {
        return "MCRTilingQueue";
    }
}
