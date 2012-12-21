/*
 * $Revision$ $Date$
 * 
 * This file is part of M y C o R e See http://www.mycore.de/ for details. This
 * program is free software; you can use it, redistribute it and / or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation; either version 2 of the License or (at your option)
 * any later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details. You should have received a copy of the GNU
 * General Public License along with this program, in a file called gpl.txt or
 * license.txt. If not, write to the Free Software Foundation Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307 USA
 */

package org.mycore.frontend.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileImportExport;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.xml.sax.SAXParseException;

/**
 * Provides static methods that implement commands for the MyCoRe command line
 * interface.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date: 2010-10-29 15:17:03 +0200 (Fri, 29 Oct
 *          2010) $
 */
public class MCRDerivateCommands extends MCRAbstractCommands {
    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRDerivateCommands.class.getName());

    /** The ACL interface */
    private static final MCRAccessInterface ACCESS_IMPL = MCRAccessManager.getAccessImpl();

    /** Default transformer script */
    public static final String DEFAULT_TRANSFORMER = "save-derivate.xsl";

    /**
     * The constructor.
     */
    public MCRDerivateCommands() {
        super();

        MCRCommand com = null;

        com = new MCRCommand("delete all derivates", "org.mycore.frontend.cli.MCRDerivateCommands.deleteAllDerivates",
            "Removes all derivates from the repository");
        addCommand(com);

        com = new MCRCommand("delete derivate from {0} to {1}", "org.mycore.frontend.cli.MCRDerivateCommands.delete String String",
            "The command remove derivates in the number range between the MCRObjectID {0} and {1}.");
        addCommand(com);

        com = new MCRCommand("delete derivate {0}", "org.mycore.frontend.cli.MCRDerivateCommands.delete String",
            "The command remove a derivate with the MCRObjectID {0}");
        addCommand(com);

        com = new MCRCommand("load derivate from file {0}", "org.mycore.frontend.cli.MCRDerivateCommands.loadFromFile String",
            "The command add a derivate form the file {0} to the system.");
        addCommand(com);

        com = new MCRCommand("update derivate from file {0}", "org.mycore.frontend.cli.MCRDerivateCommands.updateFromFile String",
            "The command update a derivate form the file {0} in the system.");
        addCommand(com);

        com = new MCRCommand("load all derivates from directory {0}", "org.mycore.frontend.cli.MCRDerivateCommands.loadFromDirectory String",
            "The command load all derivates form the directory {0} to the system.");
        addCommand(com);

        com = new MCRCommand("update all derivates from directory {0}", "org.mycore.frontend.cli.MCRDerivateCommands.updateFromDirectory String",
            "The command update all derivates form the directory {0} in the system.");
        addCommand(com);

        com = new MCRCommand("export derivate from {0} to {1} to directory {2} with {3}",
            "org.mycore.frontend.cli.MCRDerivateCommands.export String String String String",
            "The command store all derivates with MCRObjectID's between {0} and {1} to the directory {2} with the stylesheet {3}-object.xsl. For {3} save is the default.");
        addCommand(com);

        com = new MCRCommand("export derivate {0} to directory {1} with {2}", "org.mycore.frontend.cli.MCRDerivateCommands.export String String String",
            "The command store the derivate with the MCRObjectID {0} to the directory {1} with the stylesheet {2}-object.xsl. For {2} save is the default.");
        addCommand(com);

        com = new MCRCommand("export all derivates to directory {0} with {1}", "org.mycore.frontend.cli.MCRDerivateCommands.exportAllDerivates String String",
            "Stores all derivates to the directory {0} with the stylesheet mcr_{1}-derivate.xsl. For {1} save is the default.");
        addCommand(com);

        com = new MCRCommand("export all derivates of project {0} to directory {1} with {2}",
            "org.mycore.frontend.cli.MCRDerivateCommands.exportAllDerivatesOfProject String String String",
            "Stores all derivates of project {0} to the directory {1} with the stylesheet mcr_{2}-derivate.xsl. For {2} save is the default.");
        addCommand(com);

        com = new MCRCommand("show loadable derivate of {0} to directory {1}", "org.mycore.frontend.cli.MCRDerivateCommands.show String String",
            "The command store the derivate with the MCRObjectID {0} to the directory {1}, without ifs-metadata");
        addCommand(com);

        com = new MCRCommand("show loadable derivate of {0} to directory {1}", "org.mycore.frontend.cli.MCRDerivateCommands.show String String",
            "The command store the derivate with the MCRObjectID {0} to the directory {1}, without ifs-metadata");
        addCommand(com);

        com = new MCRCommand("repair derivate search of type derivate", "org.mycore.frontend.cli.MCRDerivateCommands.repairDerivateSearch",
            "The command read the Content store and reindex the derivate search stores.");
        addCommand(com);

        com = new MCRCommand("repair derivate search of ID {0}", "org.mycore.frontend.cli.MCRDerivateCommands.repairDerivateSearchForID String",
            "The command read the Content store for MCRObjectID {0} and reindex the derivate search store.");
        addCommand(com);

        com = new MCRCommand("synchronize all derivates", "org.mycore.frontend.cli.MCRDerivateCommands.synchronizeAllDerivates",
            "The command read each derivate and synchronize the xlink:label with the derivate entry of the mycoreobject.");
        addCommand(com);

        com = new MCRCommand("synchronize derivate with ID {0}", "org.mycore.frontend.cli.MCRDerivateCommands.synchronizeDerivateForID String",
            "The command read a derivate with the MCRObjectID {0} and synchronize the xlink:label with the derivate entry of the mycoreobject.");
        addCommand(com);

        com = new MCRCommand("link derivate {0} to {1}", "org.mycore.frontend.cli.MCRDerivateCommands.linkDerivateToObject String String",
            "links the given derivate {0} to the given mycore object {1}");
        addCommand(com);
    }

    /**
     * deletes all MCRDerivate from the datastore.
     */
    public static List<String> deleteAllDerivates() {
        List<String> ids = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        List<String> cmds = new ArrayList<String>(ids.size());
        for (String id : ids) {
            cmds.add("delete derivate " + id);
        }
        return cmds;
    }

    /**
     * Delete an MCRDerivate from the datastore.
     * 
     * @param ID
     *            the ID of the MCRDerivate that should be deleted
     * @throws MCRActiveLinkException
     *             com = new
     *             MCRCommand("export all derivates to directory {0} with {1}",
     *             "org.mycore.frontend.cli.MCRDerivateCommands.exportAllDerivates String String"
     *             ,
     *             "Stores all derivates to the directory {0} with the stylesheet mcr_{1}-derivate.xsl. For {1} save is the default."
     *             ); command.add(com);
     * @throws MCRPersistenceException
     */
    public static void delete(String ID) throws MCRPersistenceException, MCRActiveLinkException {
        MCRObjectID objectID = MCRObjectID.getInstance(ID);
        MCRMetadataManager.deleteMCRDerivate(objectID);
        LOGGER.info(objectID + " deleted.");
    }

    /**
     * Delete MCRDerivates form ID to ID from the datastore.
     * 
     * @param IDfrom
     *            the start ID for deleting the MCRDerivate
     * @param IDto
     *            the stop ID for deleting the MCRDerivate
     * @throws MCRActiveLinkException
     * @throws MCRPersistenceException
     */
    public static void delete(String IDfrom, String IDto) throws MCRPersistenceException, MCRActiveLinkException {
        int from_i = 0;
        int to_i = 0;

        MCRObjectID from = MCRObjectID.getInstance(IDfrom);
        MCRObjectID to = MCRObjectID.getInstance(IDto);
        from_i = from.getNumberAsInteger();
        to_i = to.getNumberAsInteger();

        if (from_i > to_i) {
            throw new MCRException("The from-to-interval is false.");
        }

        for (int i = from_i; i < to_i + 1; i++) {

            String id = MCRObjectID.formatID(from.getProjectId(), from.getTypeId(), i);
            if (MCRMetadataManager.exists(MCRObjectID.getInstance(id))) {
                delete(id);
            }
        }
    }

    /**
     * Loads MCRDerivates from all XML files in a directory.
     * 
     * @param directory
     *            the directory containing the XML files
     */
    public static List<String> loadFromDirectory(String directory) {
        return processFromDirectory(directory, false);
    }

    /**
     * Updates MCRDerivates from all XML files in a directory.
     * 
     * @param directory
     *            the directory containing the XML files
     */
    public static List<String> updateFromDirectory(String directory) {
        return processFromDirectory(directory, true);
    }

    /**
     * Loads or updates MCRDerivates from all XML files in a directory.
     * 
     * @param directory
     *            the directory containing the XML files
     * @param update
     *            if true, object will be updated, else object is created
     */
    private static List<String> processFromDirectory(String directory, boolean update) {
        File dir = new File(directory);

        if (!dir.isDirectory()) {
            LOGGER.warn(directory + " ignored, is not a directory.");
            return null;
        }

        File[] list = dir.listFiles();

        if (list.length == 0) {
            LOGGER.warn("No files found in directory " + directory);
            return null;
        }

        List<String> cmds = new ArrayList<String>();
        for (File file : list) {
            String name = file.getName();
            if (!(name.endsWith(".xml") && name.contains("derivate"))) {
                continue;
            }
            name = name.substring(0, name.length() - 4); // remove ".xml"
            File contentDir = new File(dir, name);
            if (!(contentDir.exists() && contentDir.isDirectory())) {
                continue;
            }
            cmds.add((update ? "update" : "load") + " derivate from file " + file.getAbsolutePath());
        }

        return cmds;
    }

    /**
     * Loads an MCRDerivates from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @throws SAXParseException
     */
    public static boolean loadFromFile(String file) throws SAXParseException, IOException {
        return loadFromFile(file, true);
    }

    /**
     * Loads an MCRDerivates from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws SAXParseException
     */
    public static boolean loadFromFile(String file, boolean importMode) throws SAXParseException, IOException {
        return processFromFile(new File(file), false, importMode);
    }

    /**
     * Updates an MCRDerivates from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @throws SAXParseException
     */
    public static boolean updateFromFile(String file) throws SAXParseException, IOException {
        return updateFromFile(file, true);
    }

    /**
     * Updates an MCRDerivates from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws SAXParseException
     */
    public static boolean updateFromFile(String file, boolean importMode) throws SAXParseException, IOException {
        return processFromFile(new File(file), true, importMode);
    }

    /**
     * Loads or updates an MCRDerivates from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @param update
     *            if true, object will be updated, else object is created
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws SAXParseException
     */
    private static boolean processFromFile(File file, boolean update, boolean importMode) throws SAXParseException, IOException {
        if (!file.getName().endsWith(".xml")) {
            LOGGER.warn(file + " ignored, does not end with *.xml");
            return false;
        }

        if (!file.isFile()) {
            LOGGER.warn(file + " ignored, is not a file.");
            return false;
        }

        LOGGER.info("Reading file " + file + " ...");

        MCRDerivate derivate = new MCRDerivate(file.toURI());
        derivate.setImportMode(importMode);

        // Replace relative path with absolute path of files
        if (derivate.getDerivate().getInternals() != null) {
            String path = derivate.getDerivate().getInternals().getSourcePath();
            path = path.replace('/', File.separatorChar).replace('\\', File.separatorChar);
            if (path.trim().length() <= 1) {
                // the path is the path name plus the name of the derivate -
                path = derivate.getId().toString().toLowerCase();
            }
            File sPath = new File(path);

            if (!sPath.isAbsolute()) {
                // only change path to absolute path when relative
                String prefix = file.getParent();

                if (prefix != null) {
                    path = prefix + File.separator + path;
                }
            }

            derivate.getDerivate().getInternals().setSourcePath(path);
            LOGGER.info("Source path --> " + path);
        }

        LOGGER.info("Label --> " + derivate.getLabel());

        if (update) {
            MCRMetadataManager.update(derivate);
            LOGGER.info(derivate.getId().toString() + " updated.");
            LOGGER.info("");
        } else {
            MCRMetadataManager.create(derivate);
            LOGGER.info(derivate.getId().toString() + " loaded.");
            LOGGER.info("");
        }

        return true;
    }

    /**
     * Save an MCRDerivate to a file named <em>MCRObjectID</em> .xml in a
     * directory with <em>dirname</em> and store the derivate objects in a
     * directory under them named <em>MCRObjectID</em>. The IFS-Attribute of the
     * derivate files aren't saved, for reloading purpose after deleting a
     * derivate in the datastore
     * 
     * @param ID
     *            the ID of the MCRDerivate to be save.
     * @param dirname
     *            the dirname to store the derivate
     */
    public static void show(String ID, String dirname) {
        export(ID, ID, dirname, "save");
    }

    /**
     * Save an MCRDerivate to a file named <em>MCRObjectID</em> .xml in a
     * directory with <em>dirname</em> and store the derivate objects in a
     * directory under them named <em>MCRObjectID</em>. The method use the
     * converter stylesheet mcr_<em>style</em>_object.xsl.
     * 
     * @param ID
     *            the ID of the MCRDerivate to be save.
     * @param dirname
     *            the dirname to store the derivate
     * @param style
     *            the type of the stylesheet
     */
    public static void export(String ID, String dirname, String style) {
        export(ID, ID, dirname, style);
    }

    /**
     * Export any MCRDerivate's to files named <em>MCRObjectID</em> .xml in a
     * directory and the objects under them named <em>MCRObjectID</em>. The
     * saving starts with fromID and runs to toID. ID's they was not found will
     * skiped. The method use the converter stylesheet mcr_<em>style</em>
     * _object.xsl.
     * 
     * @param fromID
     *            the ID of the MCRObject from be save.
     * @param toID
     *            the ID of the MCRObject to be save.
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the type of the stylesheet
     */
    public static void export(String fromID, String toID, String dirname, String style) {
        // check fromID and toID
        MCRObjectID fid = null;
        MCRObjectID tid = null;

        try {
            fid = MCRObjectID.getInstance(fromID);
        } catch (Exception ex) {
            LOGGER.error("FromID : " + ex.getMessage());

            return;
        }

        try {
            tid = MCRObjectID.getInstance(toID);
        } catch (Exception ex) {
            LOGGER.error("ToID : " + ex.getMessage());

            return;
        }

        // check dirname
        File dir = new File(dirname);

        if (dir.isFile()) {
            LOGGER.error(dirname + " is not a dirctory.");

            return;
        }

        Transformer trans = getTransformer(style);

        int k = 0;

        try {
            for (int i = fid.getNumberAsInteger(); i < tid.getNumberAsInteger() + 1; i++) {

                exportDerivate(dir, trans, MCRObjectID.formatID(fid.getProjectId(), fid.getTypeId(), i));

                k++;
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("Exception while store file or objects to " + dir.getAbsolutePath(), ex);

            return;
        }

        LOGGER.info(k + " Object's stored under " + dir.getAbsolutePath() + ".");
    }

    /**
     * The command look for all derivates in the application and build export
     * commands.
     * 
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the type of the stylesheet
     * @return a list of export commands for each derivate
     */
    public static List<String> exportAllDerivates(String dirname, String style) {
        // check dirname
        File dir = new File(dirname);

        if (dir.isFile()) {
            throw new MCRException(dirname + " is not a dirctory.");
        }

        List<String> ids = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        List<String> cmds = new ArrayList<String>(ids.size());
        for (String id : ids) {
            cmds.add("export derivate " + id + " to directory " + dirname + " with " + style);
        }
        return cmds;
    }

    /**
     * The command look for all derivates starts with project name in the
     * application and build export commands.
     * 
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the type of the stylesheet
     * @return a list of export commands for derivates with project name
     */
    public static List<String> exportAllDerivatesOfProject(String project, String dirname, String style) {
        // check dirname
        File dir = new File(dirname);

        if (dir.isFile()) {
            throw new MCRException(dirname + " is not a dirctory.");
        }

        List<String> ids = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        List<String> cmds = new ArrayList<String>(ids.size());
        for (String id : ids) {
            if (!id.startsWith(project))
                continue;
            cmds.add("export derivate " + id + " to directory " + dirname + " with " + style);
        }
        return cmds;
    }

    /**
     * @param dir
     * @param trans
     * @param nid
     * @throws FileNotFoundException
     * @throws TransformerException
     * @throws IOException
     */
    private static void exportDerivate(File dir, Transformer trans, String nid) throws FileNotFoundException, TransformerException, IOException {
        // store the XML file
        Document xml = null;
        MCRDerivate obj;

        try {
            obj = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(nid));
            String path = obj.getDerivate().getInternals().getSourcePath();
            // reset from the absolute to relative path, for later reload
            LOGGER.info("Old Internal Path ====>" + path);
            obj.getDerivate().getInternals().setSourcePath(nid);
            LOGGER.info("New Internal Path ====>" + nid);
            // add ACL's
            Collection<String> l = ACCESS_IMPL.getPermissionsForID(nid);
            for (String permission : l) {
                Element rule = ACCESS_IMPL.getRule(nid, permission);
                obj.getService().addRule(permission, rule);
            }
            // build JDOM
            xml = obj.createXML();

        } catch (MCRException ex) {
            LOGGER.warn("Could not read " + nid + ", continue with next ID");
            return;
        }
        File xmlOutput = new File(dir, nid + ".xml");
        FileOutputStream out = new FileOutputStream(xmlOutput);
        dir = new File(dir, nid);

        if (trans != null) {
            trans.setParameter("dirname", dir.getPath());
            StreamResult sr = new StreamResult(out);
            trans.transform(new org.jdom.transform.JDOMSource(xml), sr);
        } else {
            new org.jdom.output.XMLOutputter().output(xml, out);
            out.flush();
            out.close();
        }

        LOGGER.info("Object " + nid + " stored under " + xmlOutput + ".");

        // store the derivate file under dirname
        try {

            if (!dir.isDirectory()) {
                dir.mkdir();
            }

            MCRFileImportExport.exportFiles(obj.receiveDirectoryFromIFS(nid), dir);
        } catch (MCRException ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("Exception while store to object in " + dir.getAbsolutePath());
            return;
        }

        LOGGER.info("Derivate " + nid + " saved under " + dir.toString() + " and " + xmlOutput.toString() + ".");
    }

    /**
     * @param style
     * @return
     * @throws TransformerFactoryConfigurationError
     */
    private static Transformer getTransformer(String style) throws TransformerFactoryConfigurationError {
        String xslfile = DEFAULT_TRANSFORMER;
        if (style != null && style.trim().length() != 0) {
            xslfile = style + "-derivate.xsl";
        }
        Transformer trans = null;

        try {
            InputStream in = MCRDerivateCommands.class.getResourceAsStream("/" + xslfile);

            if (in != null) {
                StreamSource source = new StreamSource(in);
                TransformerFactory transfakt = TransformerFactory.newInstance();
                transfakt.setURIResolver(MCRURIResolver.instance());
                trans = transfakt.newTransformer(source);
            }
        } catch (Exception e) {
            LOGGER.debug("Cannot build Transformer.", e);
        }
        return trans;
    }

    /**
     * The method start the repair the content search index for all derivates.
     */
    public static List<String> repairDerivateSearch() {
        LOGGER.info("Start the repair for type derivate.");
        List<String> ids = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        if (ids.size() == 0) {
            LOGGER.warn("No ID's was found for type derivate.");
            return Collections.emptyList();
        }
        List<String> cmds = new ArrayList<String>(ids.size());
        for (String id : ids) {
            cmds.add("repair derivate search of ID " + id);
        }
        return cmds;
    }

    /**
     * The method start the repair the content search index for one.
     * 
     * @param id
     *            the MCRObjectID as String
     */
    public static void repairDerivateSearchForID(String id) {
        LOGGER.info("Start the repair for the ID " + id);
        MCRDirectory rootifs = MCRDirectory.getRootDirectory(MCRObjectID.getInstance(id).toString());
        doForChildren(rootifs);
        LOGGER.info("Repaired " + id);
    }

    /**
     * This is a recursive method to start an event handler for each file.
     * 
     * @param thisnode
     *            a IFS nod (file or directory)
     */
    private static void doForChildren(MCRFilesystemNode thisnode) {
        if (thisnode instanceof MCRDirectory) {
            MCRFilesystemNode[] childnodes = ((MCRDirectory) thisnode).getChildren();
            for (MCRFilesystemNode childnode : childnodes) {
                doForChildren(childnode);
            }
        } else {
            // handle events
            MCREvent evt = new MCREvent(MCREvent.FILE_TYPE, MCREvent.REPAIR_EVENT);
            evt.put("file", thisnode);
            MCREventManager.instance().handleEvent(evt);
            String fn = ((MCRFile) thisnode).getAbsolutePath();
            LOGGER.debug("repair file " + fn);
        }
    }

    /**
     * The method start the repair the content search index for all derivates.
     */
    public static List<String> synchronizeAllDerivates() {
        LOGGER.info("Start the synchronization for derivates.");
        List<String> ids = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        if (ids.size() == 0) {
            LOGGER.warn("No ID's was found for type derivate.");
            return Collections.emptyList();
        }
        List<String> cmds = new ArrayList<String>(ids.size());
        for (String id : ids) {
            cmds.add("synchronize derivate with ID " + id);
        }
        return cmds;
    }

    /**
     * The method sychronize the xlink:label of the mycorederivate with the
     * xlink:label of the derivate refernce of mycoreobject.
     * 
     * @param id
     *            the MCRObjectID as String
     */
    public static void synchronizeDerivateForID(String id) {
        MCRObjectID mid = null;
        try {
            mid = MCRObjectID.getInstance(id);
        } catch (Exception e) {
            LOGGER.error("The String " + id + " is not a MCRObjectID.");
            return;
        }

        // set mycoreobject
        MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(mid);
        String label = der.getLabel();
        String href = der.getDerivate().getMetaLink().getXLinkHref();
        MCRObject obj = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(href));
        int size = obj.getStructure().getDerivateSize();
        boolean isset = false;
        for (int i = 0; i < size; i++) {
            MCRMetaLinkID link = obj.getStructure().getDerivate(i);
            if (link.getXLinkHref().equals(mid.toString())) {
                String oldlabel = link.getXLinkLabel();
                if (oldlabel != null && !oldlabel.trim().equals(label)) {
                    obj.getStructure().getDerivate(i).setXLinkTitle(label);
                    isset = true;
                }
                break;
            }
        }
        // update mycoreobject
        if (isset) {
            MCRMetadataManager.fireUpdateEvent(obj);
            LOGGER.info("Synchronized " + mid.toString());
        }
    }

    /**
     * Links the given derivate to the given object.
     * 
     * @param derivateId
     * @param objectId
     */
    public static void linkDerivateToObject(String derivateId, String objectId) throws Exception {
        if (derivateId == null || objectId == null) {
            LOGGER.error("Either derivate id or object id is null. Derivate=" + derivateId + ", object=" + objectId);
            return;
        }
        MCRObjectID derID = MCRObjectID.getInstance(derivateId);
        MCRObjectID objID = MCRObjectID.getInstance(objectId);

        if (!MCRMetadataManager.exists(objID)) {
            throw new Exception("The object with id " + objID + " does not exist");
        }

        if (!MCRMetadataManager.exists(derID)) {
            throw new Exception("The derivate with id " + derID + " does not exist");
        }

        MCRDerivate derObj = MCRMetadataManager.retrieveMCRDerivate(derID);
        MCRMetaLinkID oldDerivateToObjectLink = derObj.getDerivate().getMetaLink();
        MCRObjectID oldOwnerId = oldDerivateToObjectLink.getXLinkHrefID();

        /* set link to new parent in the derivate object */
        LOGGER.info("Setting " + objID + " as parent for derivate " + derID);
        derObj.getDerivate().getMetaLink().setReference(objID, oldDerivateToObjectLink.getXLinkLabel(), oldDerivateToObjectLink.getXLinkTitle());
        derObj.setLabel("data object from " + objectId + " (prev. owner was " + oldOwnerId);
        MCRMetadataManager.updateMCRDerivateXML(derObj);

        /* set link to derivate in the new parent */
        MCRObject oldOwner = MCRMetadataManager.retrieveMCRObject(oldOwnerId);
        List<MCRMetaLinkID> derivates = oldOwner.getStructure().getDerivates();
        MCRMetaLinkID oldObjectToDerivateLink = null;
        for (MCRMetaLinkID derivate : derivates) {
            if (derivate.getXLinkHrefID().equals(derID)) {
                oldObjectToDerivateLink = derivate;
            }
        }
        if (oldObjectToDerivateLink == null) {
            oldObjectToDerivateLink = new MCRMetaLinkID();
        }
        LOGGER.info("Linking derivate " + derID + " to " + objID);
        MCRMetaLinkID derivateLink = new MCRMetaLinkID();
        derivateLink.setReference(derID, oldObjectToDerivateLink.getXLinkLabel(), oldObjectToDerivateLink.getXLinkTitle());
        derivateLink.setSubTag("derobject");
        MCRMetadataManager.addDerivateToObject(objID, derivateLink);

        /* removing link from old parent */
        boolean flag = oldOwner.getStructure().removeDerivate(derID);
        LOGGER.info("Unlinking derivate " + derID + " from object " + oldOwnerId + ". Success=" + flag);
        MCRMetadataManager.fireUpdateEvent(oldOwner);
    }
}
