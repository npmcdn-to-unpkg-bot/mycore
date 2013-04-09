/**
 * 
 */
package org.mycore.solr.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.mycore.common.MCRObjectUtils;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.index.cs.MCRSolrIndexer;

/**
 * Class provides useful solr related commands.
 * 
 * @author shermann
 *
 */
public class MCRSolrCommands extends MCRAbstractCommands {

    public MCRSolrCommands() {
        super("Solr Commands");
        MCRCommand com = null;

        com = new MCRCommand("rebuild solr metadata and content index", "org.mycore.solr.index.cs.MCRSolrIndexer.rebuildMetadataAndContentIndex",
                "rebuilds solr's metadata and content index");
        addCommand(com);

        com = new MCRCommand("rebuild solr metadata index", "org.mycore.solr.index.cs.MCRSolrIndexer.rebuildMetadataIndex", "rebuilds solr's metadata index");
        addCommand(com);

        com = new MCRCommand("rebuild solr content index", "org.mycore.solr.index.cs.MCRSolrIndexer.rebuildContentIndex", "rebuilds solr's content index");
        addCommand(com);

        com = new MCRCommand("restricted rebuild solr metadata index for objecttype {0}",
                "org.mycore.solr.index.cs.MCRSolrIndexer.rebuildMetadataIndex String", "rebuilds solr's metadata index for the given type in {0}");
        addCommand(com);

        com = new MCRCommand("restricted rebuild solr metadata index for selected", "org.mycore.solr.commands.MCRSolrCommands.rebuildMetadataIndexForSelected",
                "rebuilds solr's metadata index for selected objects");
        addCommand(com);

        com = new MCRCommand("restricted rebuild solr content index for selected", "org.mycore.solr.commands.MCRSolrCommands.rebuildContentIndexForSelected",
                "rebuilds solr's content index for selected objects and or derivates");
        addCommand(com);

        com = new MCRCommand("restricted rebuild solr metadata index for object {0}",
                "org.mycore.solr.commands.MCRSolrCommands.rebuildMetadataIndexForObject String",
                "rebuilds solr's metadata index for object and all its children");
        addCommand(com);

        com = new MCRCommand("optimize solr index", "org.mycore.solr.index.cs.MCRSolrIndexer.optimize",
                "An optimize is like a hard commit except that it forces all of the index segments to be merged into a single segment first. "
                        + "Depending on the use cases, this operation should be performed infrequently (like nightly), "
                        + "if at all, since it is very expensive and involves reading and re-writing the entire index");
        addCommand(com);

        com = new MCRCommand("drop solr index", "org.mycore.solr.index.cs.MCRSolrIndexer.dropIndex", "Deletes an existing index from solr");
        addCommand(com);

        com = new MCRCommand("delete index part for type {0}", "org.mycore.solr.index.cs.MCRSolrIndexer.dropIndexByType String",
                "Deletes an existing index from solr but only for the given object type.");
        addCommand(com);

        com = new MCRCommand("delete from solr index by id {0}", "org.mycore.solr.index.cs.MCRSolrIndexer.deleteByIdFromSolr String",
                "Deletes an document from the index by id");
        addCommand(com);

        com = new MCRCommand("create solr metadata and content index at {0}", "org.mycore.solr.commands.MCRSolrCommands.createIndex String",
                "create solr's metadata and content index on specific solr server");
        addCommand(com);

        com = new MCRCommand("set solr server {0}", "org.mycore.solr.MCRSolrServerFactory.setSolrServer String", "Sets the solr new server");
        addCommand(com);
    }

    public static void rebuildMetadataIndexForSelected() {
        List<String> selectedObjects = MCRObjectCommands.getSelectedObjectIDs();
        MCRSolrIndexer.rebuildMetadataIndex(selectedObjects);
    }

    public static void rebuildContentIndexForSelected() {
        List<String> selectedObjects = MCRObjectCommands.getSelectedObjectIDs();
        MCRSolrIndexer.rebuildContentIndex(selectedObjects);
    }

    public static void rebuildMetadataIndexForObject(String id) {
        MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(id));
        List<MCRObject> objectList = MCRObjectUtils.getDescendantsAndSelf(mcrObject);
        List<String> idList = new ArrayList<>();
        for (MCRObject obj : objectList) {
            idList.add(obj.getId().toString());
        }
        MCRSolrIndexer.rebuildMetadataIndex(idList);
    }

    public static void createIndex(String url) throws Exception {
        ConcurrentUpdateSolrServer cuss = MCRSolrServerFactory.createConcurrentUpdateSolrServer(url);
        HttpSolrServer hss = MCRSolrServerFactory.createSolrServer(url);
        MCRSolrIndexer.rebuildMetadataIndex(cuss);
        MCRSolrIndexer.rebuildContentIndex(hss);
        hss.optimize();
    }

}
