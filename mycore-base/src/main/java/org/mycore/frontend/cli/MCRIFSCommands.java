package org.mycore.frontend.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.comparator.NameFileComparator;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.mycore.backend.filesystem.MCRCStoreVFS;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRFSNODES;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.ifs.MCRContentStoreFactory;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Attributes2Impl;

@MCRCommandGroup(name = "IFS Maintenance")
public class MCRIFSCommands {
    private static final String ELEMENT_FILE = "file";

    private static final String CDATA = "CDATA";

    private static final String ATT_FILE_NAME = "name";

    private static final String NS_URI = "";

    private static Logger LOGGER = Logger.getLogger(MCRIFSCommands.class);

    private static abstract class FSNodeChecker {
        public abstract String getName();

        public abstract boolean checkNode(MCRFSNODES node, File localFile, Attributes2Impl atts);

        void addBaseAttributes(MCRFSNODES node, Attributes2Impl atts) {
            atts.clear();
            atts.addAttribute(NS_URI, ATT_SIZE, ATT_SIZE, CDATA, Long.toString(node.getSize()));
            atts.addAttribute(NS_URI, ATT_MD5, ATT_MD5, CDATA, node.getMd5());
            atts.addAttribute(NS_URI, ATT_STORAGEID, ATT_STORAGEID, CDATA, node.getStorageid());
            atts.addAttribute(NS_URI, ATT_OWNER, ATT_OWNER, CDATA, node.getOwner());
            atts.addAttribute(NS_URI, ATT_NAME, ATT_NAME, CDATA, node.getName());
        }

        final static String ATT_STORAGEID = "storageid";

        final static String ATT_OWNER = "owner";

        final static String ATT_NAME = "fileName";

        final static String ATT_MD5 = "md5";

        final static String ATT_SIZE = "size";

    }

    private static class LocalFileExistChecker extends FSNodeChecker {
        @Override
        public String getName() {
            return "missing";
        }

        @Override
        public boolean checkNode(MCRFSNODES node, File localFile, Attributes2Impl atts) {
            if (localFile.exists()) {
                return true;
            }
            LOGGER.warn("File is missing: " + localFile);
            addBaseAttributes(node, atts);
            return false;
        }

    }

    private static final class MD5Checker extends LocalFileExistChecker {
        @Override
        public String getName() {
            return "md5";
        }

        @Override
        public boolean checkNode(MCRFSNODES node, File localFile, Attributes2Impl atts) {
            if (!super.checkNode(node, localFile, atts)) {
                atts.addAttribute(MCRIFSCommands.NS_URI, super.getName(), super.getName(), MCRIFSCommands.CDATA, "true");
                return false;
            }
            addBaseAttributes(node, atts);
            if (localFile.length() != node.getSize()) {
                LOGGER.warn("File size does not match for file: " + localFile);
                atts.addAttribute(MCRIFSCommands.NS_URI, "actualSize", "actualSize", MCRIFSCommands.CDATA, Long.toString(localFile.length()));
                return false;
            }
            //we can check MD5Sum
            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(localFile);
            } catch (FileNotFoundException e1) {
                //should not happen as we check it before
                LOGGER.warn(e1);
                return false;
            }
            String md5Sum;
            try {
                md5Sum = MCRUtils.getMD5Sum(fileInputStream);
            } catch (NoSuchAlgorithmException e) {
                LOGGER.error(e);
                return false;
            } catch (IOException e) {
                LOGGER.error(e);
                return false;
            }
            if (md5Sum.equals(node.getMd5())) {
                return true;
            }
            LOGGER.warn("MD5 sum does not match for file: " + localFile);
            atts.addAttribute(MCRIFSCommands.NS_URI, "actualMD5", "actualMD5", MCRIFSCommands.CDATA, md5Sum);
            return false;
        }
    }

    public static class FileStoreIterator implements Iterable<File> {

        private File baseDir;

        public FileStoreIterator(File basedir) {
            this.baseDir = basedir;
        }

        @Override
        public Iterator<File> iterator() {
            return new Iterator<File>() {
                File currentDir = baseDir;

                LinkedList<File> files = getInitialList(currentDir);

                LinkedList<Iterator<File>> iterators = initIterator();

                @Override
                public boolean hasNext() {
                    if (iterators.isEmpty()) {
                        return false;
                    }
                    if (!iterators.getFirst().hasNext()) {
                        iterators.removeFirst();
                        return hasNext();
                    }
                    return true;
                }

                private LinkedList<Iterator<File>> initIterator() {
                    LinkedList<Iterator<File>> iterators = new LinkedList<Iterator<File>>();
                    iterators.add(getIterator(files));
                    return iterators;
                }

                private Iterator<File> getIterator(LinkedList<File> files) {
                    return files.iterator();
                }

                private LinkedList<File> getInitialList(File currentDir) {
                    File[] children = currentDir.listFiles();
                    Arrays.sort(children, NameFileComparator.NAME_COMPARATOR);
                    LinkedList<File> list = new LinkedList<File>();
                    list.addAll(Arrays.asList(children));
                    return list;
                }

                @Override
                public File next() {
                    if (iterators.isEmpty()) {
                        throw new NoSuchElementException("No more files");
                    }
                    File next = iterators.getFirst().next();
                    if (next.isDirectory()) {
                        LinkedList<File> list = getInitialList(next);
                        if (!list.isEmpty()) {
                            Iterator<File> iterator = getIterator(list);
                            iterators.addFirst(iterator);
                        }
                    }
                    return next;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("remove() is not supported");
                }
            };
        }
    }

    @MCRCommand(syntax = "generate md5sum files in directory {0}", help = "writes md5sum files for every content store in directory {0}")
    public static void writeMD5SumFile(String targetDirectory) throws IOException {
        File targetDir = getDirectory(targetDirectory);
        Session session = MCRHIBConnection.instance().getSession();
        Criteria criteria = session.createCriteria(MCRFSNODES.class);
        criteria.addOrder(Order.asc("storeid"));
        criteria.addOrder(Order.asc("storageid"));
        criteria.add(Restrictions.eq("type", "F"));
        ScrollableResults fsnodes = criteria.scroll(ScrollMode.FORWARD_ONLY);
        Map<String, MCRContentStore> availableStores = MCRContentStoreFactory.getAvailableStores();
        String currentStoreId = null;
        MCRContentStore currentStore = null;
        File currentStoreBaseDir = null;
        FileWriter fw = null;
        String nameOfProject = MCRConfiguration.instance().getString("MCR.NameOfProject", "MyCoRe");
        try {
            while (fsnodes.next()) {
                MCRFSNODES fsNode = (MCRFSNODES) fsnodes.get(0);
                String storeID = fsNode.getStoreid();
                String storageID = fsNode.getStorageid();
                String md5 = fsNode.getMd5();
                session.evict(fsNode);
                if (!storeID.equals(currentStoreId)) {
                    //initialize current store
                    currentStoreId = storeID;
                    currentStore = availableStores.get(storeID);
                    if (fw != null) {
                        fw.close();
                    }
                    File outputFile = new File(targetDir, MessageFormat.format("{0}-{1}.md5", nameOfProject, storeID));
                    LOGGER.info("Writing to file: " + outputFile.getAbsolutePath());
                    fw = new FileWriter(outputFile);
                    if (currentStore instanceof MCRCStoreVFS) {
                        try {
                            currentStoreBaseDir = ((MCRCStoreVFS) currentStore).getBaseDir();
                        } catch (Exception e) {
                            LOGGER.warn("Could not get baseDir of store: " + storeID, e);
                            currentStoreBaseDir = null;
                        }
                    } else {
                        currentStoreBaseDir = null;
                    }
                }
                String path = currentStoreBaseDir != null ? new File(currentStoreBaseDir, storageID).getAbsolutePath() : storageID;
                //current store initialized
                String line = MessageFormat.format("{0}  {1}\n", md5, path);
                fw.write(line);
            }
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e1) {
                    LOGGER.warn("Error while closing file.", e1);
                }
            }
            session.clear();
        }
    }

    @MCRCommand(syntax = "generate missing file report in directory {0}", help = "Writes XML report about missing files in directory {0}")
    public static void writeMissingFileReport(String targetDirectory) throws IOException, SAXException, TransformerConfigurationException {
        File targetDir = getDirectory(targetDirectory);
        FSNodeChecker checker = new LocalFileExistChecker();
        writeReport(targetDir, checker);
    }

    @MCRCommand(syntax = "generate md5 file report in directory {0}", help = "Writes XML report about failed md5 checks in directory {0}")
    public static void writeFileMD5Report(String targetDirectory) throws IOException, SAXException, TransformerConfigurationException {
        File targetDir = getDirectory(targetDirectory);
        FSNodeChecker checker = new MD5Checker();
        writeReport(targetDir, checker);
    }

    private static void writeReport(File targetDir, FSNodeChecker checker) throws TransformerFactoryConfigurationError, SAXException, IOException,
        FileNotFoundException, TransformerConfigurationException {
        Session session = MCRHIBConnection.instance().getSession();
        Criteria criteria = session.createCriteria(MCRFSNODES.class);
        criteria.addOrder(Order.asc("storeid"));
        criteria.addOrder(Order.asc("owner"));
        criteria.addOrder(Order.asc(ATT_FILE_NAME));
        criteria.add(Restrictions.eq("type", "F"));
        ScrollableResults fsnodes = criteria.scroll(ScrollMode.FORWARD_ONLY);
        Map<String, MCRContentStore> availableStores = MCRContentStoreFactory.getAvailableStores();
        String currentStoreId = null;
        MCRContentStore currentStore = null;
        File currentStoreBaseDir = null;
        StreamResult streamResult = null;
        String nameOfProject = MCRConfiguration.instance().getString("MCR.NameOfProject", "MyCoRe");
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler th = null;
        Attributes2Impl atts = new Attributes2Impl();
        final String rootName = checker.getName();
        final String elementName = ELEMENT_FILE;
        final String ATT_BASEDIR = "basedir";
        final String nsURI = NS_URI;
        final String ATT_TYPE = CDATA;
        String owner = null;

        try {
            while (fsnodes.next()) {
                MCRFSNODES fsNode = (MCRFSNODES) fsnodes.get(0);
                String storeID = fsNode.getStoreid();
                String storageID = fsNode.getStorageid();
                session.evict(fsNode);
                if (!storeID.equals(currentStoreId)) {
                    //initialize current store
                    currentStoreId = storeID;
                    currentStore = availableStores.get(storeID);
                    if (th != null) {
                        th.endElement(nsURI, rootName, rootName);
                        th.endDocument();
                        OutputStream outputStream = streamResult.getOutputStream();
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    }
                    File outputFile = new File(targetDir, MessageFormat.format("{0}-{1}-{2}.xml", nameOfProject, storeID, rootName));
                    streamResult = new StreamResult(new FileOutputStream(outputFile));
                    th = tf.newTransformerHandler();
                    Transformer serializer = th.getTransformer();
                    serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
                    th.setResult(streamResult);
                    LOGGER.info("Writing to file: " + outputFile.getAbsolutePath());
                    th.startDocument();
                    atts.clear();
                    atts.addAttribute(nsURI, "project", "project", ATT_TYPE, nameOfProject);
                    if (currentStore instanceof MCRCStoreVFS) {
                        try {
                            currentStoreBaseDir = ((MCRCStoreVFS) currentStore).getBaseDir();
                            atts.addAttribute(nsURI, ATT_BASEDIR, ATT_BASEDIR, ATT_TYPE, currentStoreBaseDir.getAbsolutePath());
                        } catch (Exception e) {
                            LOGGER.warn("Could not get baseDir of store: " + storeID, e);
                            currentStoreBaseDir = null;
                        }
                    } else {
                        currentStoreBaseDir = null;
                    }
                    th.startElement(nsURI, rootName, rootName, atts);
                }
                if (currentStoreBaseDir == null) {
                    continue;
                }
                if (!fsNode.getOwner().equals(owner)) {
                    owner = fsNode.getOwner();
                    LOGGER.info("Checking owner/derivate: " + owner);
                }
                File f = new File(currentStoreBaseDir, storageID);
                if (!checker.checkNode(fsNode, f, atts)) {
                    th.startElement(nsURI, elementName, elementName, atts);
                    th.endElement(nsURI, elementName, elementName);
                }
            }
        } finally {
            session.clear();
            if (th != null) {
                try {
                    th.endElement(nsURI, rootName, rootName);
                    th.endDocument();
                    OutputStream outputStream = streamResult.getOutputStream();
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e1) {
                    LOGGER.warn("Error while closing file.", e1);
                }
            }
        }
    }

    /**
     * @param targetDirectory
     * @return
     */
    static File getDirectory(String targetDirectory) {
        File targetDir = new File(targetDirectory);
        if (!targetDir.isDirectory()) {
            throw new IllegalArgumentException("Target directory " + targetDir.getAbsolutePath() + " is not a directory.");
        }
        return targetDir;
    }

    @MCRCommand(syntax = "generate missing nodes report in directory {0}", help = "Writes XML report about missing ifs nodes in directory {0}")
    public static void writeMissingNodesReport(String targetDirectory) throws SAXException, TransformerConfigurationException, IOException {
        File targetDir = getDirectory(targetDirectory);
        Map<String, MCRContentStore> availableStores = MCRContentStoreFactory.getAvailableStores();
        final String nsURI = NS_URI;
        final String ATT_TYPE = CDATA;
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        Attributes2Impl atts = new Attributes2Impl();
        final String rootName = "missingnodes";
        StatelessSession session = MCRHIBConnection.instance().getSessionFactory().openStatelessSession();
        try {
            for (MCRContentStore currentStore : availableStores.values()) {
                if (currentStore instanceof MCRCStoreVFS) {
                    MCRCStoreVFS storeVFS;
                    File baseDir;
                    try {
                        storeVFS = (MCRCStoreVFS) currentStore;
                        baseDir = storeVFS.getBaseDir();
                    } catch (Exception e) {
                        LOGGER.warn("Could not get baseDir of store: " + currentStore.getID(), e);
                        continue;
                    }
                    Criteria criteria = session.createCriteria(MCRFSNODES.class);
                    criteria.add(Restrictions.eq("type", "F"));
                    criteria.add(Restrictions.eq("storeid", storeVFS.getID()));
                    criteria.addOrder(Order.asc("storageid"));
                    criteria.setProjection(Projections.property("storageid"));
                    ScrollableResults storageIds = criteria.scroll(ScrollMode.FORWARD_ONLY);
                    boolean endOfList = false;
                    String nameOfProject = MCRConfiguration.instance().getString("MCR.NameOfProject", "MyCoRe");
                    String storeID = storeVFS.getID();
                    File outputFile = new File(targetDir, MessageFormat.format("{0}-{1}-{2}.xml", nameOfProject, storeID, rootName));
                    StreamResult streamResult;
                    try {
                        streamResult = new StreamResult(new FileOutputStream(outputFile));
                    } catch (FileNotFoundException e) {
                        //should not happen as we checked it before
                        LOGGER.error(e);
                        return;
                    }
                    try {
                        TransformerHandler th = tf.newTransformerHandler();
                        Transformer serializer = th.getTransformer();
                        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
                        th.setResult(streamResult);
                        LOGGER.info("Writing to file: " + outputFile.getAbsolutePath());
                        th.startDocument();
                        atts.clear();
                        atts.addAttribute(nsURI, "project", "project", ATT_TYPE, nameOfProject);
                        atts.addAttribute(nsURI, "store", "store", ATT_TYPE, storeID);
                        atts.addAttribute(nsURI, "baseDir", "baseDir", ATT_TYPE, baseDir.getAbsolutePath());
                        th.startElement(nsURI, rootName, rootName, atts);
                        URI baseURI = baseDir.toURI();
                        for (File currentFile : new FileStoreIterator(baseDir)) {
                            if (currentFile.isDirectory()) {
                                String relative = baseURI.relativize(currentFile.toURI()).getPath();
                                LOGGER.info("Checking segment: " + relative);
                            } else {
                                int checkFile = endOfList ? -1 : checkFile(baseURI, currentFile, storageIds);
                                endOfList = checkFile == -1;
                                if (endOfList || checkFile == 1) {
                                    LOGGER.warn("Found orphaned file: " + currentFile);
                                    atts.clear();
                                    atts.addAttribute(NS_URI, ATT_FILE_NAME, ATT_FILE_NAME, CDATA, baseURI.relativize(currentFile.toURI()).getPath());
                                    th.startElement(NS_URI, ELEMENT_FILE, ELEMENT_FILE, atts);
                                    th.endElement(NS_URI, ELEMENT_FILE, ELEMENT_FILE);
                                }
                            }
                        }
                        storageIds.close();
                        th.endElement(nsURI, rootName, rootName);
                        th.endDocument();
                    } finally {
                        OutputStream stream = streamResult.getOutputStream();
                        if (stream != null) {
                            stream.close();
                        }
                    }
                }
            }
        } finally {
            session.close();
        }
    }

    @MCRCommand(syntax = "delete ifs node {0}", help = "deletes ifs node {0} recursivly")
    public void deleteIFSNode(String nodeID) {
        MCRFilesystemNode node = MCRFilesystemNode.getNode(nodeID);
        if (node == null) {
            LOGGER.warn("IFS Node " + nodeID + " does not exist.");
            return;
        }
        LOGGER.info(MessageFormat.format("Deleting IFS Node {0}: {1}{2}", nodeID, node.getOwnerID(), node.getAbsolutePath()));
        node.delete();
    }

    /**
     * 
     * @param baseURI
     * @param currentFile
     * @param storageIds
     * @return 0 (node present), 1 (node not present), -1 (end of storageIds)
     */
    private static int checkFile(URI baseURI, File currentFile, ScrollableResults storageIds) {
        if (storageIds.getRowNumber() == -1) {
            //go to first Result;
            if (!storageIds.next()) {
                return 1;
            }
        }
        String storageId = storageIds.getString(0);
        String relativePath = baseURI.relativize(currentFile.toURI()).getPath();
        int comp = relativePath.compareTo(storageId);
        while (comp > 0) {
            if (storageIds.next()) {
                storageId = storageIds.getString(0);
                comp = relativePath.compareTo(storageId);
            } else {
                return -1;
            }
        }
        return comp == 0 ? 0 : 1;
    }
}
