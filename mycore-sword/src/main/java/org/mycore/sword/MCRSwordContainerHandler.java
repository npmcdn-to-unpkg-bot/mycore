package org.mycore.sword;

import java.util.Map;
import java.util.Optional;

import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.sword.application.MCRSwordCollectionProvider;
import org.mycore.sword.application.MCRSwordLifecycle;
import org.mycore.sword.application.MCRSwordLifecycleConfiguration;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

/**
 * Handles request made to the Edit-IRI.
 *
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSwordContainerHandler implements MCRSwordLifecycle {

    private MCRSwordLifecycleConfiguration lifecycleConfiguration;

    public DepositReceipt addObject(Deposit deposit) throws SwordError, SwordServerException {
        final MCRSwordCollectionProvider collection = MCRSword.getCollection(this.lifecycleConfiguration.getCollection());
        final MCRObjectID idOfIngested = collection.getIngester().ingestMetadata(deposit);
        final MCRObject createdObject = (MCRObject) MCRMetadataManager.retrieve(idOfIngested);
        return collection.getMetadataProvider().provideMetadata(createdObject);
    }

    public DepositReceipt addObjectWithDerivate(String objectIdString, Deposit deposit) throws SwordError, SwordServerException {
        final MCRSwordCollectionProvider collection = MCRSword.getCollection(this.lifecycleConfiguration.getCollection());
        final MCRObjectID idOfIngested = collection.getIngester().ingestMetadataResources(deposit);
        final MCRObject createdObject = (MCRObject) MCRMetadataManager.retrieve(idOfIngested);
        return collection.getMetadataProvider().provideMetadata(createdObject);
    }

    public DepositReceipt addMetadata(MCRObject object, Deposit deposit) throws SwordError, SwordServerException {
        final MCRSwordCollectionProvider collection = MCRSword.getCollection(this.lifecycleConfiguration.getCollection());
        collection.getIngester().updateMetadata(object, deposit, false);
        return collection.getMetadataProvider().provideMetadata(object);
    }

    /**
     * Replaces the metadata of an existing object.
     * @param object  The object with the metadata to replace
     * @param deposit the deposit with the new metadata
     * @return a new {@link DepositReceipt} with the new metadata
     */
    public DepositReceipt replaceMetadata(MCRObject object, Deposit deposit) throws SwordError, SwordServerException {
        final MCRSwordCollectionProvider collection = MCRSword.getCollection(this.lifecycleConfiguration.getCollection());
        collection.getIngester().updateMetadata(object, deposit, true);
        return collection.getMetadataProvider().provideMetadata(object);
    }

    public DepositReceipt replaceMetadataAndResources(MCRObject object, Deposit deposit) throws SwordError, SwordServerException {
        final MCRSwordCollectionProvider collection = MCRSword.getCollection(this.lifecycleConfiguration.getCollection());
        collection.getIngester().updateMetadataResources(object, deposit);
        return collection.getMetadataProvider().provideMetadata(object);
    }

    public DepositReceipt addResources(MCRObject object, Deposit deposit) throws SwordError, SwordServerException {
        final MCRSwordCollectionProvider collection = MCRSword.getCollection(this.lifecycleConfiguration.getCollection());
        collection.getIngester().ingestResource(object, deposit);
        return collection.getMetadataProvider().provideMetadata(object);
    }


    /**
     * This method should add metadata to the receipt. The are the more important Metadata.
     *
     * @param object the MyCoReObject
     * @param accept the accept header of the HTTP-Request
     */
    public DepositReceipt getMetadata(String collectionString, MCRObject object, Optional<Map<String, String>> accept) throws SwordError {
        return MCRSword.getCollection(collectionString).getMetadataProvider().provideMetadata(object);
    }

    public void deleteObject(MCRObject object) throws SwordServerException {
        object.getStructure().getDerivates().forEach(derLink->{
            final MCRObjectID xLinkToID = derLink.getXLinkHrefID();
            MCRMetadataManager.deleteMCRDerivate(xLinkToID);
        });
        try {
            MCRMetadataManager.delete(object);
        } catch (MCRActiveLinkException e) {
            throw new SwordServerException("Error while deleting Object.",e);
        }
    }

    @Override
    public void init(MCRSwordLifecycleConfiguration lifecycleConfiguration) {
        this.lifecycleConfiguration = lifecycleConfiguration;
    }

    @Override
    public void destroy() {

    }
}
