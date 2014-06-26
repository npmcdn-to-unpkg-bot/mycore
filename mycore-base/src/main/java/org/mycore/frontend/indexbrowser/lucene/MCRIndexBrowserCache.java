package org.mycore.frontend.indexbrowser.lucene;

import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRException;

/**
 * Caches index browser entries in a hash table. Each object type 
 * gets an entry in the table. The value of an entry is a MCRCache.
 * This cache again creates new entries for different search and modes.
 * So the cache key is: object index # search # mode
 *
 * @author Matthias Eichner
 */
public class MCRIndexBrowserCache {
    protected static Logger LOGGER = Logger.getLogger(MCRIndexBrowserCache.class);

    private static Hashtable<String, MCRCache> TYPE_CACHE_TABLE = new Hashtable<String, MCRCache>();

    private static ReentrantReadWriteLock TYPE_CACHE_TABLE_LOCK = new ReentrantReadWriteLock();

    /**
     * Add a new list of index browser entries to the cache.
     * @param listToCache a list to cache
     */
    public static void addToCache(String cacheKey, String index, List<MCRIndexBrowserEntry> listToCache) {
        if (cacheKey == null || index == null || listToCache == null) {
            return;
        }

        // adds a new mcr cache to the hash table
        MCRCache mcrCache = null;
        if (!isInHashtable(index)) {
            mcrCache = addIndexCacheToHashtable(index);
        } else {
            mcrCache = getIndexCache(index);
        }
        // add the index browser entry list to the mcr cache
        mcrCache.put(cacheKey, listToCache);
    }

    /**
     * Adds a new MCRCache to the hash table.
     * @param index the key of the entry.
     */
    protected static MCRCache addIndexCacheToHashtable(String index) {
        if (index == null) {
            throw new MCRException("Could not determine index: " + index);
        }
        MCRCache cache = null;
        try {
            TYPE_CACHE_TABLE_LOCK.writeLock().lock();
            if (!TYPE_CACHE_TABLE.containsKey(index)) {
                TYPE_CACHE_TABLE.put(index, cache = new MCRCache(1000, "IndexBrowser,objectType=" + index.replace(",", "_")));
            }
        } finally {
            TYPE_CACHE_TABLE_LOCK.writeLock().unlock();
        }
        return cache;
    }

    /**
     * Deletes a whole MCRCache from the hash table.
     * @param objectType the object type which has to be removed
     */
    public static void deleteIndexCacheFromHashtable(String objectType) {
        if (objectType == null) {
            return;
        }
        TYPE_CACHE_TABLE_LOCK.writeLock().lock();
        TYPE_CACHE_TABLE.remove(objectType);
        TYPE_CACHE_TABLE_LOCK.writeLock().unlock();
    }

    /**
     * Returns the cached index browser list.
     * @return the cached list.
     */
    public static List<MCRIndexBrowserEntry> getFromCache(String cacheKey, String index) {
        // if the list is not cached, return null
        if (!isCached(cacheKey, index)) {
            return null;
        }

        // get the mcr cache from the hash table
        MCRCache mcrCache = getIndexCache(index);
        // get the cached list from the mcr cache
        @SuppressWarnings("unchecked")
        List<MCRIndexBrowserEntry> cachedList = (List<MCRIndexBrowserEntry>) mcrCache.get(cacheKey);
        // return the list
        return cachedList;
    }

    /**
     * Returns the MCRCache from the hash table by the given index name.
     * @param index the index name of the specific index browser
     * @return a MCRCache from the hash table
     */
    protected static MCRCache getIndexCache(String index) {
        try {
            TYPE_CACHE_TABLE_LOCK.readLock().lock();
            return TYPE_CACHE_TABLE.get(index);
        } finally {
            TYPE_CACHE_TABLE_LOCK.readLock().unlock();
        }
    }

    /**
     * Checks if a hash table entry with the specified index
     * exists. If true, the method checks if the specified key
     * exists in the MCRCache.
     * @return true if an entry in the hash table and in the MCRCache exists,
     * otherwise false
     */
    public static boolean isCached(String cacheKey, String index) {
        try {
            TYPE_CACHE_TABLE_LOCK.readLock().lock();
            MCRCache mcrCache = TYPE_CACHE_TABLE.get(index);
            return mcrCache != null && mcrCache.get(cacheKey) != null;
        } finally {
            TYPE_CACHE_TABLE_LOCK.readLock().unlock();
        }
    }

    /**
     * Checks if the entry exists in the hash table.
     * @param key the key which will be checked
     * @return true if the entry was found, otherwise false
     */
    public static boolean isInHashtable(String key) {
        try {
            TYPE_CACHE_TABLE_LOCK.readLock().lock();
            return TYPE_CACHE_TABLE.containsKey(key);
        } finally {
            TYPE_CACHE_TABLE_LOCK.readLock().unlock();
        }
    }
}