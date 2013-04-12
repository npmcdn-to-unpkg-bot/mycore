package org.mycore.solr.index.cs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.logging.MCRSolrLogLevels;

/**
 * Commits <code>MCRFile</code> objects to solr, be aware that the files are
 * not indexed directly, but added to a list of sub index handlers.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrFilesIndexHandler implements MCRSolrIndexHandler {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrFilesIndexHandler.class);

    protected String mcrID;

    protected SolrServer solrServer;

    protected List<MCRSolrIndexHandler> subHandlerList;

    private int commitWithin;

    /**
     * Creates a new solr file index handler.
     * 
     * @param mcrID id of the derivate or mcrobject, if you put a mcrobject id here
     * all files of each derivate are indexed
     * @param solrServer where to index
     */
    public MCRSolrFilesIndexHandler(String mcrID, SolrServer solrServer) {
        this.mcrID = mcrID;
        this.solrServer = solrServer;
        this.subHandlerList = new ArrayList<>();
        this.commitWithin = -1;
    }

    @Override
    public void index() throws IOException, SolrServerException {
        MCRObjectID mcrID = MCRObjectID.getInstance(getID());
        if (mcrID.getTypeId().equals("derivate")) {
            indexDerivate(mcrID.toString());
        } else {
            indexObject(mcrID);
        }
    }

    protected void indexDerivate(String derivateID) {
        List<MCRFile> files = MCRUtils.getFiles(derivateID);
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Sending files (" + files.size() + ") for derivate \"" + getID() + "\"");
        for (MCRFile file : files) {
            try {
                this.subHandlerList.add(MCRSolrIndexer.getIndexHandler(file, this.solrServer));
            } catch (Exception ex) {
                LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Error creating transfer thread", ex);
            }
        }
    }

    protected void indexObject(MCRObjectID objectID) {
        MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(objectID);
        for (MCRMetaLinkID link : mcrObject.getStructure().getDerivates()) {
            String derivateID = link.getXLinkHref();
            indexDerivate(derivateID);
        }
    }

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return this.subHandlerList;
    }

    public String getID() {
        return mcrID;
    }

    @Override
    public void setCommitWithin(int commitWithin) {
        this.commitWithin = commitWithin;
    }

    @Override
    public int getCommitWithin() {
        return commitWithin;
    }

}
