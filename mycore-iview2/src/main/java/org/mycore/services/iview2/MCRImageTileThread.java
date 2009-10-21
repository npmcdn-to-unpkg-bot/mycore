package org.mycore.services.iview2;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.datamodel.ifs.MCRFile;

public class MCRImageTileThread implements Runnable {
    private MCRTileJob image = null;

    private static SessionFactory sessionFactory = MCRHIBConnection.instance().getSessionFactory();

    private static Logger LOGGER = Logger.getLogger(MCRImageTileThread.class);

    public void run() {
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();
        try {
            MCRFile file = MCRIView2Tools.getMCRFile(image.getDerivate(), image.getPath());
            MCRImage imgTiler = new MCRImage(file);
            MCRTiledPictureProps picProps = new MCRTiledPictureProps();

            try {
                File storeDir = new File(MCRIview2Props.getProperty("DirectoryForTiles") + file.getOwnerID() + "/"
                        + file.getName().substring(0, file.getName().lastIndexOf(".")) + "/");
                storeDir.mkdir();
                imgTiler.setOutputDirectory(storeDir);
                picProps = imgTiler.tile();
            } catch (IOException e) {
                LOGGER.error("IOException occured while Tiling a queued Picture", e);
            }
            image.setFinished(new Date(System.currentTimeMillis()));
            image.setStatus(MCRJobState.FIN);
            image.setHeight(picProps.height);
            image.setWidth(picProps.width);
            image.setTiles(picProps.countTiles);
            image.setZoomLevel(picProps.zoomlevel);
            MCRTilingQueue.getInstance().updateJob(image);
            transaction.commit();
        } catch (HibernateException e) {
            LOGGER.error("Error while getting next tiling job.", e);
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
        }
    }

    MCRImageTileThread(MCRTileJob image) {
        this.image = image;
    }
}
