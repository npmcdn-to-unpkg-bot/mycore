package org.mycore.iview2.events;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.iview2.frontend.MCRIView2Commands;
import org.mycore.iview2.services.MCRTilingQueue;

/**
 * Handles {@link MCRFile} events to keep image tiles up-to-date.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRImageTileEventHandler extends MCREventHandlerBase {

    MCRTilingQueue tq = MCRTilingQueue.getInstance();
    /**
     * creates image tiles if <code>file</code> is an image file.
     */
    @Override
    public void handleFileCreated(MCREvent evt, MCRFile file) {
        MCRIView2Commands.tileImage(file);
    }

    /**
     * deletes image tiles for <code>file</code>.
     */
    @Override
    public void handleFileDeleted(MCREvent evt, MCRFile file) {
        MCRIView2Commands.deleteImageTiles(file.getOwnerID(), file.getAbsolutePath());
    }

    /**
     * updates image times if <code>file</code> is an image file.
     */
    @Override
    public void handleFileUpdated(MCREvent evt, MCRFile file) {
        handleFileDeleted(evt, file);
        handleFileCreated(evt, file);
    }

}
