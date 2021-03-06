/**
 * 
 */
package org.mycore.solr.index;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory;
import org.mycore.solr.index.handlers.MCRSolrOptimizeIndexHandler;
import org.mycore.solr.index.handlers.stream.MCRSolrFilesIndexHandler;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;
import org.mycore.solr.search.MCRSolrSearchUtils;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author shermann
 * @author Matthias Eichner
 */
public class MCRSolrIndexer {
    private static final Logger LOGGER = Logger.getLogger(MCRSolrIndexer.class);

    /**
     * Specify how many documents will be submitted to solr at a time when rebuilding the metadata index. Default is
     * 100.
     */
    final static int BULK_SIZE = MCRConfiguration.instance().getInt(CONFIG_PREFIX + "Indexer.BulkSize", 100);

    final static ExecutorService PARALLEL_SERVICE;

    final static ExecutorService SERIAL_SERVICE = MoreExecutors.newDirectExecutorService();

    private static final int BATCH_AUTO_COMMIT_WITHIN_MS = 60000;

    static {
        int poolSize = MCRConfiguration.instance().getInt(CONFIG_PREFIX + "Indexer.ThreadCount", 4);
        PARALLEL_SERVICE = Executors.newFixedThreadPool(poolSize,
            new ThreadFactoryBuilder().setNameFormat("SOLR-Indexer-#%d").build());
        MCRShutdownHandler.getInstance().addCloseable(new Closeable() {

            @Override
            public void prepareClose() {
                PARALLEL_SERVICE.shutdown();
                SERIAL_SERVICE.shutdown();
            }

            @Override
            public int getPriority() {
                return Integer.MIN_VALUE + 6;
            }

            @Override
            public void close() {
                waitForShutdown(SERIAL_SERVICE);
                waitForShutdown(PARALLEL_SERVICE);
            }

            private void waitForShutdown(ExecutorService service) {
                if (!service.isTerminated()) {
                    try {
                        service.awaitTermination(10, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        LOGGER.warn("Error while waiting for shutdown.", e);
                    }
                }
            }
        });
        MCRShutdownHandler.getInstance().addCloseable(new Closeable() {

            @Override
            public void prepareClose() {
            }

            @Override
            public int getPriority() {
                return Integer.MIN_VALUE + 4;
            }

            @Override
            public void close() {
                String documentStats = MessageFormat.format("Solr documents: {0}, each: {1} ms.",
                    MCRSolrIndexStatisticCollector.documents.getDocuments(),
                    MCRSolrIndexStatisticCollector.documents.reset());
                String metadataStats = MessageFormat.format("XML documents: {0}, each: {1} ms.",
                    MCRSolrIndexStatisticCollector.xml.getDocuments(), MCRSolrIndexStatisticCollector.xml.reset());
                String fileStats = MessageFormat.format("File transfers: {0}, each: {1} ms.",
                    MCRSolrIndexStatisticCollector.fileTransfer.getDocuments(),
                    MCRSolrIndexStatisticCollector.fileTransfer.reset());
                String operationsStats = MessageFormat.format("Other index operations: {0}, each: {1} ms.",
                    MCRSolrIndexStatisticCollector.operations.getDocuments(),
                    MCRSolrIndexStatisticCollector.operations.reset());
                String msg = MessageFormat.format("\nFinal statistics:\n{0}\n{1}\n{2}\n{3}", documentStats,
                    metadataStats, fileStats, operationsStats);
                LOGGER.info(msg);
            }
        });
    }

    public static UpdateResponse deleteOrphanedNestedDocuments() throws SolrServerException, IOException {
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        return solrClient.deleteByQuery("-({!join from=id to=_root_}_root_:*) +_root_:*", 0);
    }

    /**
     * Deletes a list of documents by unique ID. Also removes any nested document of that ID.
     * 
     * @param solrIDs
     *            the list of solr document IDs to delete
     */
    public static UpdateResponse deleteById(String... solrIDs) {
        if (solrIDs == null || solrIDs.length == 0) {
            return null;
        }
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        UpdateResponse updateResponse = null;
        long start = System.currentTimeMillis();
        try {
            LOGGER.debug("Deleting \"" + Arrays.asList(solrIDs) + "\" from solr");
            UpdateRequest req = new UpdateRequest();
            //delete all documents rooted at this id
            if(useNestedDocuments()) {
                StringBuilder deleteQuery = new StringBuilder("_root_:(");
                for (String solrID : solrIDs) {
                    deleteQuery.append('"');
                    deleteQuery.append(MCRSolrUtils.escapeSearchValue(solrID));
                    deleteQuery.append("\" ");
                }
                deleteQuery.setCharAt(deleteQuery.length() - 1, ')');
                req.deleteByQuery(deleteQuery.toString());
            }
            //for document without nested
            req.deleteById(Arrays.asList(solrIDs));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Delete request: " + req.getXML());
            }
            updateResponse = req.process(solrClient);
            solrClient.commit();
        } catch (Exception e) {
            LOGGER.error("Error deleting document from solr", e);
        }
        long end = System.currentTimeMillis();
        MCRSolrIndexStatistic operations = MCRSolrIndexStatisticCollector.operations;
        operations.addDocument(1);
        operations.addTime(end - start);
        return updateResponse;

    }

    /**
     * Convenient method to delete a derivate and all its files at once.
     * 
     * @param id the derivate id
     * @return the solr response
     */
    public static UpdateResponse deleteDerivate(String id) {
        if (id == null) {
            return null;
        }
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        UpdateResponse updateResponse = null;
        long start = System.currentTimeMillis();
        try {
            LOGGER.debug("Deleting derivate \"" + id + "\" from solr");
            UpdateRequest req = new UpdateRequest();
            StringBuilder deleteQuery = new StringBuilder();
            deleteQuery.append("id:").append(id).append(" ");
            deleteQuery.append("derivateID:").append(id);
            if (useNestedDocuments()) {
                deleteQuery.append(" ").append("_root_:").append(id);
            }
            req.deleteByQuery(deleteQuery.toString());
            updateResponse = req.process(solrClient);
            solrClient.commit();
        } catch (Exception e) {
            LOGGER.error("Error deleting document from solr", e);
        }
        long end = System.currentTimeMillis();
        MCRSolrIndexStatistic operations = MCRSolrIndexStatisticCollector.operations;
        operations.addDocument(1);
        operations.addTime(end - start);
        return updateResponse;
    }

    /**
     * Checks if the application uses nested documents. Using nested documents requires
     * additional queries and slows performance.
     * 
     * @return true if nested documents are used, otherwise false
     */
    protected static boolean useNestedDocuments() {
        return MCRConfiguration.instance().getBoolean("MCR.Module-solr.NestedDocuments", true);
    }

    /**
     * Rebuilds solr's metadata index.
     */
    public static void rebuildMetadataIndex(boolean parallel) {
        rebuildMetadataIndex(MCRXMLMetadataManager.instance().listIDs(), parallel);
    }

    /**
     * Rebuilds solr's metadata index.
     */
    public static void rebuildMetadataIndex(SolrClient cuss, boolean parallel) {
        rebuildMetadataIndex(MCRXMLMetadataManager.instance().listIDs(), cuss, parallel);
    }

    /**
     * Rebuilds solr's metadata index only for objects of the given type.
     * 
     * @param type
     *            of the objects to index
     */
    public static void rebuildMetadataIndex(String type, boolean parallel) {
        List<String> identfiersOfType = MCRXMLMetadataManager.instance().listIDsOfType(type);
        rebuildMetadataIndex(identfiersOfType, parallel);
    }

    public static void rebuildMetadataIndex(List<String> list, boolean parallel) {
        rebuildMetadataIndex(list, MCRSolrClientFactory.getConcurrentSolrClient(), parallel);
    }

    /**
     * Rebuilds solr's metadata index.
     * 
     * @param list
     *            list of identifiers of the objects to index
     * @param solrClient
     *            solr server to index
     * @param parallel
     *            TODO
     */
    public static void rebuildMetadataIndex(List<String> list, SolrClient solrClient, boolean parallel) {
        LOGGER.info("Re-building Metadata Index");
        if (list.isEmpty()) {
            LOGGER.info("Sorry, no documents to index");
            return;
        }

        StopWatch swatch = new StopWatch();
        swatch.start();
        int totalCount = list.size();
        LOGGER.info("Sending " + totalCount + " objects to solr for reindexing");

        MCRXMLMetadataManager metadataMgr = MCRXMLMetadataManager.instance();
        HashMap<MCRObjectID, MCRContent> contentMap = new HashMap<>((int) (BULK_SIZE * 1.4));
        MCRSolrIndexStatistic statistic = null;
        int i = 0;
        for (String id : list) {
            i++;
            try {
                LOGGER.debug("Preparing \"" + id + "\" for indexing");
                MCRObjectID objId = MCRObjectID.getInstance(id);
                MCRContent content = metadataMgr.retrieveContent(objId);
                contentMap.put(objId, content);
                if (i % BULK_SIZE == 0 || totalCount == i) {
                    MCRSolrIndexHandler indexHandler = MCRSolrIndexHandlerFactory.getInstance().getIndexHandler(
                        contentMap);
                    indexHandler.setCommitWithin(BATCH_AUTO_COMMIT_WITHIN_MS);
                    indexHandler.setSolrServer(solrClient);
                    statistic = indexHandler.getStatistic();
                    submitIndexHandler(indexHandler, parallel);
                    contentMap.clear();
                }
            } catch (Exception ex) {
                LOGGER.error("Error creating index thread for object " + id, ex);
            }
        }
        long durationInMilliSeconds = swatch.getTime();
        if (statistic != null) {
            statistic.addTime(durationInMilliSeconds);
        }
    }

    /**
     * Rebuilds solr's content index.
     */
    public static void rebuildContentIndex(boolean parallel) {
        rebuildContentIndex(MCRSolrClientFactory.getSolrClient(),
            MCRXMLMetadataManager.instance().listIDsOfType("derivate"), parallel);
    }

    public static void rebuildContentIndex(SolrClient hss, boolean parallel) {
        rebuildContentIndex(hss, MCRXMLMetadataManager.instance().listIDsOfType("derivate"), parallel);
    }

    /**
     * Rebuilds the content index for the given mycore objects. You can mix derivates and mcrobjects here. For each
     * mcrobject all its derivates are indexed.
     * 
     * @param list
     *            containing mycore object id's
     */
    public static void rebuildContentIndex(List<String> list, boolean parallel) {
        rebuildContentIndex(MCRSolrClientFactory.getSolrClient(), list, parallel);
    }

    /**
     * Rebuilds solr's content index.
     */
    public static void rebuildContentIndex(SolrClient solrClient, List<String> list, boolean parallel) {
        LOGGER.info("Re-building Content Index");

        if (list.isEmpty()) {
            LOGGER.info("No objects to index");
            return;
        }
        long tStart = System.currentTimeMillis();

        int totalCount = list.size();
        LOGGER.info("Sending content of " + totalCount + " derivates to solr for reindexing");

        for (String id : list) {
            MCRSolrFilesIndexHandler indexHandler = new MCRSolrFilesIndexHandler(id, solrClient);
            indexHandler.setCommitWithin(BATCH_AUTO_COMMIT_WITHIN_MS);
            submitIndexHandler(indexHandler, parallel);
        }

        long tStop = System.currentTimeMillis();
        MCRSolrIndexStatisticCollector.fileTransfer.addTime(tStop - tStart);
    }

    /**
     * Submits a index handler to the executor service (execute as a thread) with the given priority.
     * 
     * @param indexHandler
     *            index handler to submit
     * @param parallel
     *            if current transaction is read-only, save to say 'true' here
     */
    public static void submitIndexHandler(MCRSolrIndexHandler indexHandler, boolean parallel) {
        MCRSolrIndexTask indexTask = new MCRSolrIndexTask(indexHandler);
        ExecutorService es = parallel ? PARALLEL_SERVICE : SERIAL_SERVICE;
        Future<List<MCRSolrIndexHandler>> future = es.submit(indexTask);
        try {
            List<MCRSolrIndexHandler> handlerList = future.get();
            if (handlerList != null) {
                //TODO: Java 8: fire an wait to finish
                indexAndWait(handlerList, es);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while submitting index handler.", e);
        }
    }

    private static void indexAndWait(List<MCRSolrIndexHandler> handlerList, ExecutorService es)
        throws InterruptedException,
        ExecutionException {
        if (handlerList == null || handlerList.isEmpty()) {
            return;
        }
        CompletionService<List<MCRSolrIndexHandler>> cs = new ExecutorCompletionService<>(es);
        for (MCRSolrIndexHandler handler : handlerList) {
            cs.submit(new MCRSolrIndexTask(handler));
        }
        int count = handlerList.size();
        LOGGER.info("Waiting for " + count + " index handler to complete");
        for (int i = 0; i < count; i++) {
            List<MCRSolrIndexHandler> handlers = cs.take().get();
            indexAndWait(handlers, es);
        }
    }

    /**
     * Rebuilds and optimizes solr's metadata and content index.
     */
    public static void rebuildMetadataAndContentIndex(boolean parallel) throws Exception {
        MCRSolrIndexer.rebuildMetadataIndex(parallel);
        MCRSolrIndexer.rebuildContentIndex(parallel);
        MCRSolrIndexer.optimize();
    }

    /**
     * Drops the current solr index.
     */
    public static void dropIndex() throws Exception {
        LOGGER.info("Dropping solr index...");
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        solrClient.deleteByQuery("*:*", BATCH_AUTO_COMMIT_WITHIN_MS);
        LOGGER.info("Dropping solr index...done");
    }

    public static void dropIndexByType(String type) throws Exception {
        if (!MCRObjectID.isValidType(type) || "data_file".equals(type)) {
            LOGGER.warn("The type " + type + " is not a valid type in the actual environment");
            return;
        }

        LOGGER.info("Dropping solr index for type " + type + "...");
        String deleteQuery = MessageFormat.format("objectType:{0} _root_:*_{1}_*", type, type);
        MCRSolrClientFactory.getSolrClient().deleteByQuery(deleteQuery, BATCH_AUTO_COMMIT_WITHIN_MS);
        LOGGER.info("Dropping solr index for type " + type + "...done");
    }

    /**
     * Sends a signal to the remote solr server to optimize its index.
     */
    public static void optimize() {
        try {
            MCRSolrOptimizeIndexHandler indexHandler = new MCRSolrOptimizeIndexHandler();
            indexHandler.setCommitWithin(BATCH_AUTO_COMMIT_WITHIN_MS);
            submitIndexHandler(indexHandler, false);
        } catch (Exception ex) {
            LOGGER.error("Could not optimize solr index", ex);
        }
    }

    /**
     * Synchronizes the solr server with the database. As a result the solr server contains the same documents as the
     * database. All solr zombie documents will be removed, and all not indexed mycore objects will be indexed.
     */
    public static void synchronizeMetadataIndex(boolean parallel) throws IOException, SolrServerException {
        Collection<String> objectTypes = MCRXMLMetadataManager.instance().getObjectTypes();
        for (String objectType : objectTypes) {
            synchronizeMetadataIndex(objectType, parallel);
        }
    }

    /**
     * Synchronizes the solr server with the mycore store for a given object type. As a result the solr server contains
     * the same documents as the store. All solr zombie documents will be removed, and all not indexed mycore objects
     * will be indexed.
     */
    public static void synchronizeMetadataIndex(String objectType, boolean parallel) throws IOException,
        SolrServerException {
        LOGGER.info("synchronize " + objectType);
        // get ids from store
        LOGGER.info("fetching mycore store...");
        List<String> storeList = MCRXMLMetadataManager.instance().listIDsOfType(objectType);
        LOGGER.info("there are " + storeList.size() + " mycore objects");
        // get ids from solr
        LOGGER.info("fetching solr...");
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        List<String> solrList = MCRSolrSearchUtils.listIDs(solrClient, "objectType:" + objectType);
        LOGGER.info("there are " + solrList.size() + " solr objects");

        // documents to remove
        List<String> toRemove = new ArrayList<>(1000);
        for (String id : solrList) {
            if (!storeList.contains(id)) {
                toRemove.add(id);
            }
        }
        if (!toRemove.isEmpty()) {
            LOGGER.info("remove " + toRemove.size() + " zombie objects from solr");
            deleteById(toRemove.toArray(new String[toRemove.size()]));
        }
        deleteOrphanedNestedDocuments();
        // documents to add
        storeList.removeAll(solrList);
        if (!storeList.isEmpty()) {
            LOGGER.info("index " + storeList.size() + " mycore objects");
            rebuildMetadataIndex(storeList, parallel);
        }
    }

}
