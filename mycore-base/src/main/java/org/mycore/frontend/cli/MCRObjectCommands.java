/*
 * $Revision$ $Date$ This file is part of M y C o R e See http://www.mycore.de/ for details. This program
 * is free software; you can use it, redistribute it and / or modify it under the terms of the GNU General Public License (GPL) as published by the Free
 * Software Foundation; either version 2 of the License or (at your option) any later version. This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details. You should have received a copy of the GNU General Public License along with this program, in a file called gpl.txt or license.txt. If not,
 * write to the Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
 */

package org.mycore.frontend.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRQueryParser;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.fieldquery.MCRSearcherFactory;
import org.xml.sax.SAXParseException;

/**
 * Provides static methods that implement commands for the MyCoRe command line interface.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRObjectCommands extends MCRAbstractCommands {
    private static final String EXPORT_OBJECT_TO_DIRECTORY_COMMAND = "export object {0} to directory {1} with {2}";

    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRObjectCommands.class.getName());

    /** Default transformer script */
    public static final String DEFAULT_TRANSFORMER = "save-object.xsl";

    /** static compiled transformer stylesheets */
    private static Hashtable<String, javax.xml.transform.Transformer> translist = new Hashtable<String, javax.xml.transform.Transformer>();

    /**
     * The empty constructor.
     */
    public MCRObjectCommands() {
        super();

        MCRCommand com = null;

        com = new MCRCommand("delete objects matching {0}",
                "org.mycore.frontend.cli.MCRObjectCommands.deleteByQuery String",
                "Deletes all objects matching the query given in parameter {0}");
        addCommand(com);

        com = new MCRCommand("delete all objects of type {0}",
                "org.mycore.frontend.cli.MCRObjectCommands.deleteAllObjects String",
                "Removes MCRObjects in the number range between the MCRObjectID {0} and {1}.");
        addCommand(com);

        com = new MCRCommand("delete object from {0} to {1}",
                "org.mycore.frontend.cli.MCRObjectCommands.deleteFromTo String String",
                "Removes MCRObjects in the number range between the MCRObjectID {0} and {1}.");
        addCommand(com);

        com = new MCRCommand("delete object {0}", "org.mycore.frontend.cli.MCRObjectCommands.delete String",
                "Removes a MCRObject with the MCRObjectID {0}");
        addCommand(com);

        com = new MCRCommand("list objects matching {0}",
                "org.mycore.frontend.cli.MCRObjectCommands.listIdsMatchingQuery String",
                "Lists all objects matching the query given in parameter {0}");
        addCommand(com);

        com = new MCRCommand("load object from file {0}",
                "org.mycore.frontend.cli.MCRObjectCommands.loadFromFile String",
                "Adds a MCRObject form the file {0} to the system.");
        addCommand(com);

        com = new MCRCommand("load all objects from directory {0}",
                "org.mycore.frontend.cli.MCRObjectCommands.loadFromDirectory String",
                "Loads all MCRObjects form the directory {0} to the system.");
        addCommand(com);

        com = new MCRCommand("update object from file {0}",
                "org.mycore.frontend.cli.MCRObjectCommands.updateFromFile String",
                "Updates a MCRObject form the file {0} in the system.");
        addCommand(com);

        com = new MCRCommand("update all objects from directory {0}",
                "org.mycore.frontend.cli.MCRObjectCommands.updateFromDirectory String",
                "Updates all MCRObjects form the directory {0} in the system.");
        addCommand(com);

        com = new MCRCommand(
                "export object from {0} to {1} to directory {2} with {3}",
                "org.mycore.frontend.cli.MCRObjectCommands.export String String String String",
                "Stores all MCRObjects with MCRObjectID's between {0} and {1} to the directory {2} with the stylesheet {3}-object.xsl. For {3} save is the default.");
        addCommand(com);

        com = new MCRCommand(
                EXPORT_OBJECT_TO_DIRECTORY_COMMAND,
                "org.mycore.frontend.cli.MCRObjectCommands.export String String String",
                "Stores the MCRObject with the MCRObjectID {0} to the directory {1} with the stylesheet {2}-object.xsl. For {2} save is the default.");
        addCommand(com);

        com = new MCRCommand(
                "export all objects of type {0} to directory {1} with {2}",
                "org.mycore.frontend.cli.MCRObjectCommands.exportAllObjectsOfType String String String",
                "Stores all MCRObjects of type {0} to directory {1} with the stylesheet mcr_{2}-object.xsl. For {2} save is the default.");
        addCommand(com);

        com = new MCRCommand(
                "export all objects of base {0} to directory {1} with {2}",
                "org.mycore.frontend.cli.MCRObjectCommands.exportAllObjectsOfBase String String String",
                "Stores all MCRObjects of base {0} to directory {1} with the stylesheet mcr_{2}-object.xsl. For {2} save is the default.");
        addCommand(com);

        com = new MCRCommand("get last ID for base {0}", "org.mycore.frontend.cli.MCRObjectCommands.getLastID String",
                "Returns the last used MCRObjectID for the ID base {0}.");
        addCommand(com);

        com = new MCRCommand("get next ID for base {0}", "org.mycore.frontend.cli.MCRObjectCommands.getNextID String",
                "Returns the next free MCRObjectID for the ID base {0}.");
        addCommand(com);

        com = new MCRCommand("check file {0}", "org.mycore.frontend.cli.MCRObjectCommands.checkXMLFile String",
                "Checks the data file {0} against the XML Schema.");
        addCommand(com);

        com = new MCRCommand("repair metadata search of type {0}",
                "org.mycore.frontend.cli.MCRObjectCommands.repairMetadataSearch String",
                "Reads the SQL store table of MCRObject XML files for the type {0} and restore them to the search store.");
        addCommand(com);

        com = new MCRCommand("repair metadata search of ID {0}",
                "org.mycore.frontend.cli.MCRObjectCommands.repairMetadataSearchForID String",
                "Read the SQL store table of MCRObject XML files with MCRObjectID {0} and restore them to the search store.");
        addCommand(com);

        com = new MCRCommand("select objects with query {0}",
                "org.mycore.frontend.cli.MCRObjectCommands.selectObjectsWithQuery String",
                "Select MCRObjects with MCRQueryString {0}.");
        addCommand(com);

        com = new MCRCommand("list selected", "org.mycore.frontend.cli.MCRObjectCommands.listSelected",
                "Prints the id of selected objects");
        addCommand(com);

        com = new MCRCommand("delete selected", "org.mycore.frontend.cli.MCRObjectCommands.deleteSelected",
                "Removes selected MCRObjects.");
        addCommand(com);

        com = new MCRCommand("export selected to directory {0} with {1}",
                "org.mycore.frontend.cli.MCRObjectCommands.exportSelected String String",
                "Stores selected MCRObjects to the directory {0} with the stylesheet {1}-object.xsl. For {1} save is the default.");
        addCommand(com);

        com = new MCRCommand("remove selected from searchindex {0}",
                "org.mycore.frontend.cli.MCRObjectCommands.removeFromSearchindex String",
                "Remove selected MCRObjects from searchindex {0}.");
        addCommand(com);

        com = new MCRCommand("check selected in sql store", "org.mycore.frontend.cli.MCRObjectCommands.checkSelected",
                "Checks existence of selected MCRObjects in SQL store and deletes missing ones from search index.");
        addCommand(com);

        com = new MCRCommand("check metadata search of type {0}",
                "org.mycore.frontend.cli.MCRObjectCommands.checkMetadataSearch String",
                "Checks existence of MCRObjects of type {0} in search index and rapairs missing ones in search index.");
        addCommand(com);

        com = new MCRCommand("set mode {0} of searcher for index {1}",
                "org.mycore.frontend.cli.MCRObjectCommands.notifySearcher String String",
                "Notify Searcher of Index {1} what is going on {0}.");
        addCommand(com);

        com = new MCRCommand("list revisions of {0}", "org.mycore.frontend.cli.MCRObjectCommands.listRevisions String",
                "List revisions of MCRObject.");
        addCommand(com);

        com = new MCRCommand("restore {0} to revision {1}",
                "org.mycore.frontend.cli.MCRObjectCommands.restoreToRevision String int",
                "Restores the selected MCRObject to the selected revision.");
        addCommand(com);

        com = new MCRCommand("xslt {0} with file {1}", "org.mycore.frontend.cli.MCRObjectCommands.xslt String String",
                "transforms a mycore object {0} with the given file {1}");
        addCommand(com);

        com = new MCRCommand("transform selected with file {0}",
                "org.mycore.frontend.cli.MCRObjectCommands.transformSelected String",
                "xsl transforms selected MCRObjects");
        addCommand(com);
        com = new MCRCommand("set parent of {0} to {1}",
                "org.mycore.frontend.cli.MCRObjectCommands.replaceParent String String",
                "replaces a parent of an object (first parameter) to the given new one (second parameter)");
        addCommand(com);
    }

    public static void setSelectedObjectIDs(List<String> selected) {
        MCRSessionMgr.getCurrentSession().put("mcrSelectedObjects", selected);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getSelectedObjectIDs() {
        final List<String> list = (List<String>) MCRSessionMgr.getCurrentSession().get("mcrSelectedObjects");
        if (list == null) {
            return Collections.EMPTY_LIST;
        }
        return list;
    }

    /**
     * Delete all MCRObject from the datastore for a given type.
     * 
     * @param type
     *            the type of the MCRObjects that should be deleted
     */
    public static List<String> deleteAllObjects(String type) throws MCRActiveLinkException {
        final List<String> objectIds = MCRXMLMetadataManager.instance().listIDsOfType(type);
        List<String> cmds = new ArrayList<String>(objectIds.size());
        for (String id : objectIds) {
            cmds.add("delete object " + id);
        }
        return cmds;
    }

    /**
     * Delete a MCRObject from the datastore.
     * 
     * @param ID
     *            the ID of the MCRObject that should be deleted
     */
    public static void delete(String ID) throws MCRActiveLinkException {
        MCRObjectID mcrId = MCRObjectID.getInstance(ID);

        try {
            MCRMetadataManager.deleteMCRObject(mcrId);
            LOGGER.info(mcrId + " deleted.");
        } catch (MCRException ex) {
            LOGGER.error("Can't delete " + mcrId + ".", ex);
        } catch (MCRActiveLinkException ex) {
            LOGGER.error("Can't delete " + mcrId + " cause the object is referenced by other.");
        }
    }

    /**
     * Delete MCRObject's form ID to ID from the datastore.
     * 
     * @param IDfrom
     *            the start ID for deleting the MCRObjects
     * @param IDto
     *            the stop ID for deleting the MCRObjects
     */
    public static void deleteFromTo(String IDfrom, String IDto) throws MCRActiveLinkException {
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
                try {
                    delete(id);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                    LOGGER.debug(e.getStackTrace());
                }
            }
        }
    }

    /**
     * Load MCRObject's from all XML files in a directory.
     * 
     * @param directory
     *            the directory containing the XML files
     * @throws MCRActiveLinkException
     */
    public static List<String> loadFromDirectory(String directory) throws MCRActiveLinkException {
        return processFromDirectory(directory, false);
    }

    /**
     * Update MCRObject's from all XML files in a directory.
     * 
     * @param directory
     *            the directory containing the XML files
     * @throws MCRActiveLinkException
     */
    public static List<String> updateFromDirectory(String directory) throws MCRActiveLinkException {
        return processFromDirectory(directory, true);
    }

    /**
     * Load or update MCRObject's from all XML files in a directory.
     * 
     * @param directory
     *            the directory containing the XML files
     * @param update
     *            if true, object will be updated, else object is created
     * @throws MCRActiveLinkException
     */
    private static List<String> processFromDirectory(String directory, boolean update)
            throws MCRActiveLinkException {
        File dir = new File(directory);

        if (!dir.isDirectory()) {
            LOGGER.warn(directory + " ignored, is not a directory.");
            return null;
        }

        String[] list = dir.list();

        if (list.length == 0) {
            LOGGER.warn("No files found in directory " + directory);
            return null;
        }

        List<String> cmds = new ArrayList<String>();
        for (String file : list) {
            if (file.endsWith(".xml") && !file.contains("derivate")) {
                cmds.add((update ? "update" : "load") + " object from file " + new File(dir, file).getAbsolutePath());
            }
        }

        return cmds;
    }

    /**
     * Load a MCRObjects from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @throws MCRActiveLinkException
     * @throws SAXParseException 
     * @throws MCRException 
     */
    public static boolean loadFromFile(String file) throws MCRActiveLinkException, MCRException,
            SAXParseException, IOException {
        return loadFromFile(file, true);
    }

    /**
     * Load a MCRObjects from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws MCRActiveLinkException
     * @throws SAXParseException 
     * @throws MCRException 
     */
    public static boolean loadFromFile(String file, boolean importMode) throws MCRActiveLinkException,
            MCRException, SAXParseException, IOException {
        return processFromFile(new File(file), false, importMode);
    }

    /**
     * Update a MCRObject's from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @throws MCRActiveLinkException
     * @throws SAXParseException 
     * @throws MCRException 
     */
    public static boolean updateFromFile(String file) throws MCRActiveLinkException, MCRException,
            SAXParseException, IOException {
        return updateFromFile(file, true);
    }

    /**
     * Update a MCRObject's from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws MCRActiveLinkException
     * @throws SAXParseException 
     * @throws MCRException 
     */
    public static boolean updateFromFile(String file, boolean importMode) throws MCRActiveLinkException,
            MCRException, SAXParseException, IOException {
        return processFromFile(new File(file), true, importMode);
    }

    /**
     * Load or update an MCRObject's from an XML file.
     * 
     * @param file
     *            the location of the xml file
     * @param update
     *            if true, object will be updated, else object is created
     * @param importMode
     *            if true, servdates are taken from xml file
     * @throws MCRActiveLinkException
     * @throws SAXParseException 
     * @throws MCRException 
     */
    private static boolean processFromFile(File file, boolean update, boolean importMode)
            throws MCRActiveLinkException, MCRException, SAXParseException, IOException {
        if (!file.getName().endsWith(".xml")) {
            LOGGER.warn(file + " ignored, does not end with *.xml");
            return false;
        }

        if (!file.isFile()) {
            LOGGER.warn(file + " ignored, is not a file.");
            return false;
        }

        LOGGER.info("Reading file " + file + " ...");

        MCRObject mycore_obj = new MCRObject(file.toURI());
        mycore_obj.setImportMode(importMode);
        LOGGER.debug("Label --> " + mycore_obj.getLabel());

        if (update) {
            MCRMetadataManager.update(mycore_obj);
            LOGGER.info(mycore_obj.getId().toString() + " updated.");
        } else {
            MCRMetadataManager.create(mycore_obj);
            LOGGER.info(mycore_obj.getId().toString() + " loaded.");
        }

        return true;
    }

    /**
     * Shows the next free MCRObjectIDs.
     * 
     * @param base
     *            the base String of the MCRObjectID
     */
    public static void showNextID(String base) {

        try {
            LOGGER.info("The next free ID  is " + MCRObjectID.getNextFreeId(base));
        } catch (MCRException ex) {
            LOGGER.error(ex.getMessage());
        }
    }

    /**
     * Shows the last used MCRObjectIDs.
     * 
     * @param base
     *            the base String of the MCRObjectID
     */
    public static void showLastID(String base) {
        try {
            LOGGER.info("The last used ID  is " + MCRObjectID.getLastID(base));
        } catch (MCRException ex) {
            LOGGER.error(ex.getMessage());
        }
    }

    /**
     * Export an MCRObject to a file named <em>MCRObjectID</em> .xml in a directory. The method use the converter stylesheet mcr_<em>style</em>_object.xsl.
     * 
     * @param ID
     *            the ID of the MCRObject to be save.
     * @param dirname
     *            the dirname to store the object
     * @param style
     *            the type of the stylesheet
     */
    public static void export(String ID, String dirname, String style) {
        export(ID, ID, dirname, style);
    }

    /**
     * Save any MCRObject's to files named <em>MCRObjectID</em> .xml in a directory. The saving starts with fromID and runs to toID. ID's they was not found
     * will skiped. The method use the converter stylesheet mcr_<em>style</em>_object.xsl.
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
        MCRObjectID fid, tid;

        // check fromID and toID
        try {
            fid = MCRObjectID.getInstance(fromID);
            tid = MCRObjectID.getInstance(toID);
        } catch (Exception ex) {
            LOGGER.error("FromID : " + ex.getMessage());
            return;
        }
        // check dirname
        File dir = new File(dirname);
        if (!dir.isDirectory()) {
            LOGGER.error(dirname + " is not a dirctory.");
            return;
        }

        int k = 0;
        try {
            Transformer trans = getTransformer(style);
            for (int i = fid.getNumberAsInteger(); i < tid.getNumberAsInteger() + 1; i++) {
                String id = MCRObjectID.formatID(fid.getProjectId(), fid.getTypeId(), i);
                if (!MCRMetadataManager.exists(MCRObjectID.getInstance(id))) {
                    continue;
                }
                if (!exportMCRObject(dir, trans, id)) {
                    continue;
                }
                k++;
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("Exception while store file to " + dir.getAbsolutePath());
            return;
        }
        LOGGER.info(k + " Object's stored under " + dir.getAbsolutePath() + ".");
    }

    /**
     * Save all MCRObject's to files named <em>MCRObjectID</em> .xml in a <em>dirname</em>directory for the data type <em>type</em>. The method use the
     * converter stylesheet mcr_<em>style</em>_object.xsl.
     * 
     * @param type
     *            the MCRObjectID type
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the type of the stylesheet
     */
    public static List<String> exportAllObjectsOfType(String type, String dirname, String style) {
        List<String> objectIds = MCRXMLMetadataManager.instance().listIDsOfType(type);
        return buildExportCommands(new File(dirname), style, objectIds);
    }

    /**
     * Save all MCRObject's to files named <em>MCRObjectID</em> .xml in a <em>dirname</em>directory for the data base <em>project_type</em>. The method use the
     * converter stylesheet mcr_<em>style</em>_object.xsl.
     * 
     * @param base
     *            the MCRObjectID base
     * @param dirname
     *            the filename to store the object
     * @param style
     *            the type of the stylesheet
     */
    public static List<String> exportAllObjectsOfBase(String base, String dirname, String style) {
        List<String> objectIds = MCRXMLMetadataManager.instance().listIDsForBase(base);
        return buildExportCommands(new File(dirname), style, objectIds);
    }

    private static List<String> buildExportCommands(File dir, String style, List<String> objectIds) {
        if (dir.isFile()) {
            LOGGER.error(dir + " is not a dirctory.");
            return Collections.emptyList();
        }
        List<String> cmds = new ArrayList<String>(objectIds.size());
        for (String id : objectIds) {
            String command = MessageFormat.format(EXPORT_OBJECT_TO_DIRECTORY_COMMAND, id, dir.getAbsolutePath(), style);
            cmds.add(command);
        }
        return cmds;
    }

    /**
     * The method search for a stylesheet mcr_<em>style</em>_object.xsl and build the transformer. Default is <em>mcr_save-object.xsl</em>.
     * 
     * @param style
     *            the style attribute for the transformer stylesheet
     * @return the transformer
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerConfigurationException
     */
    private static Transformer getTransformer(String style) throws TransformerFactoryConfigurationError,
            TransformerConfigurationException {
        String xslfile = DEFAULT_TRANSFORMER;
        if (style != null && style.trim().length() != 0) {
            xslfile = style + "-object.xsl";
        }
        Transformer trans = translist.get(xslfile);
        if (trans != null) {
            return trans;
        }
        LOGGER.debug("Will load transformer stylesheet " + xslfile + "for export.");

        InputStream in = MCRObjectCommands.class.getResourceAsStream("/" + xslfile);
        if (in == null) {
            in = MCRObjectCommands.class.getResourceAsStream("/xsl/" + DEFAULT_TRANSFORMER);
        }
        try {
            if (in != null) {
                StreamSource source = new StreamSource(in);
                TransformerFactory transfakt = TransformerFactory.newInstance();
                transfakt.setURIResolver(MCRURIResolver.instance());
                trans = transfakt.newTransformer(source);
                translist.put(xslfile, trans);
                return trans;
            } else {
                LOGGER.warn("Can't load transformer ressource " + xslfile + " or " + DEFAULT_TRANSFORMER + ".");
            }
        } catch (Exception e) {
            LOGGER.warn("Error while load transformer ressource " + xslfile + " or " + DEFAULT_TRANSFORMER + ".");
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * The method read a MCRObject and use the transformer to write the data to a file. They are any steps to handel errors and save the damaged data.
     * <ul>
     * <li>Read data for object ID in the MCRObject, add ACL's and store it as checked and transformed XML. Return true.</li>
     * <li>If it can't find a transformer instance (no script file found) it store the checked data with ACL's native in the file. Warning and return true.</li>
     * <li>If it get an exception while build the MCRObject, it try to read the XML blob and stor it without check and ACL's to the file. Warning and return
     * true.</li>
     * <li>If it get an exception while store the native data without check, ACÖ's and transformation it return a warning and false.</li>
     * </ul>
     * 
     * @param dir
     *            the file instance to store
     * @param trans
     *            the XML transformer
     * @param nid
     *            the MCRObjectID
     * @return true if the store was okay (see description), else return false
     * @throws FileNotFoundException
     * @throws TransformerException
     * @throws IOException
     * @throws SAXParseException 
     * @throws MCRException 
     */
    private static boolean exportMCRObject(File dir, Transformer trans, String nid) throws FileNotFoundException,
            TransformerException, IOException, MCRException, SAXParseException {
        byte[] xml = null;
        try {
            // if object do'snt exist - no exception is catched!
            xml = MCRXMLMetadataManager.instance().retrieveBLOB(MCRObjectID.getInstance(nid));
        } catch (MCRException ex) {
            return false;
        }

        MCRContent content = new MCRByteContent(xml);
        File xmlOutput = new File(dir, nid + ".xml");

        if (trans != null) {
            FileOutputStream out = new FileOutputStream(xmlOutput);
            StreamResult sr = new StreamResult(out);
            Document doc = MCRXMLParserFactory.getNonValidatingParser().parseXML(content);
            trans.transform(new org.jdom.transform.JDOMSource(doc), sr);
        } else {
            content.sendTo(xmlOutput);
        }
        LOGGER.info("Object " + nid + " saved to " + xmlOutput.getCanonicalPath() + ".");
        return true;
    }

    /**
     * Get the next free MCRObjectID for the given MCRObjectID base.
     * 
     * @param base
     *            the MCRObjectID base string
     */
    public static void getNextID(String base) {
        try {
            LOGGER.info(MCRObjectID.getNextFreeId(base));
        } catch (MCRException ex) {
            LOGGER.error(ex.getMessage());
        }
    }

    /**
     * Get the last used MCRObjectID for the given MCRObjectID base.
     * 
     * @param base
     *            the MCRObjectID base string
     */
    public static void getLastID(String base) {
        LOGGER.info(MCRObjectID.getLastID(base));
    }

    /**
     * The method parse and check an XML file.
     * 
     * @param fileName
     *            the location of the xml file
     * @throws SAXParseException 
     * @throws MCRException 
     */
    public static boolean checkXMLFile(String fileName) throws MCRException, SAXParseException, IOException {
        if (!fileName.endsWith(".xml")) {
            LOGGER.warn(fileName + " ignored, does not end with *.xml");

            return false;
        }

        File file = new File(fileName);

        if (!file.isFile()) {
            LOGGER.warn(fileName + " ignored, is not a file.");

            return false;
        }

        LOGGER.info("Reading file " + file + " ...");
        MCRContent content = new MCRFileContent(file);

        MCRXMLParserFactory.getParser().parseXML(content);
        LOGGER.info("The file has no XML errors.");

        return true;
    }

    /**
     * The method start the repair of the metadata search for a given MCRObjectID type.
     * 
     * @param type
     *            the MCRObjectID type
     */
    public static List<String> repairMetadataSearch(String type) {
        LOGGER.info("Start the repair for type " + type);
        String typetest = CONFIG.getString("MCR.Metadata.Type." + type, "");

        if (typetest.length() == 0) {
            LOGGER.error("The type " + type + " was not found.");
            return Collections.emptyList();
        }
        List<String> ar = (List<String>) MCRXMLMetadataManager.instance().listIDsOfType(type);
        if (ar.size() == 0) {
            LOGGER.warn("No ID's was found for type " + type + ".");
            return Collections.emptyList();
        }

        removeFromIndex("objectType", type);
        List<String> cmds = new ArrayList<String>(ar.size());

        for (String stid : ar) {
            cmds.add("repair metadata search of ID " + stid);
        }
        return cmds;

    }

    /**
     * The method start the repair of the metadata search for a given MCRObjectID as String.
     * 
     * @param id
     *            the MCRObjectID as String
     */
    public static void repairMetadataSearchForID(String id) {
        LOGGER.info("Start the repair for the ID " + id);

        MCRObjectID mid = null;

        try {
            mid = MCRObjectID.getInstance(id);
        } catch (Exception e) {
            LOGGER.error("The String " + id + " is not a MCRObjectID.");
            return;
        }

        MCRBase obj = MCRMetadataManager.retrieve(mid);
        MCRMetadataManager.fireRepairEvent(obj);
        LOGGER.info("Repaired " + mid.toString());
    }

    /**
     * The method removes entries from searchindex.
     * 
     * @param fieldname
     *            Name of field used to delete entries
     * @param value
     *            Value of the field
     */
    private static void removeFromIndex(String fieldname, String value) {
        MCRFieldDef fd = MCRFieldDef.getDef(fieldname);
        MCRSearcher searcher = getSearcherForField(fieldname);
        MCRFieldValue fv = new MCRFieldValue(fd, value);
        searcher.clearIndex(fieldname, fv.getValue());
    }

    private static MCRSearcher getSearcherForField(String fieldname) {
        MCRFieldDef fd = MCRFieldDef.getDef(fieldname);
        String index = fd.getIndex();
        return MCRSearcherFactory.getSearcherForIndex(index);
    }

    /**
     * Builds a resulset with a query. Used in later command to do work with.
     * 
     * @param querystring
     *            MCRQuery as String
     */
    public static void selectObjectsWithQuery(String querystring) {
        LOGGER.info("Build Resultset with query " + querystring);

        MCRCondition cond = new MCRQueryParser().parse(querystring);
        MCRQuery query = new MCRQuery(cond);
        final MCRResults results = MCRQueryManager.search(query);
        ArrayList<String> ids = new ArrayList<String>(results.getNumHits());
        for (MCRHit hit : results) {
            ids.add(hit.getID());
        }
        setSelectedObjectIDs(ids);

        LOGGER.info("Resultset built");
    }

    /**
     * List all selected MCRObjects.
     */
    public static void listSelected() {
        LOGGER.info("List selected MCRObjects");
        if (getSelectedObjectIDs().isEmpty()) {
            LOGGER.info("No Resultset to work with, use command \"select objects with query {0}\" to build one");
            return;
        }
        StringBuilder out = new StringBuilder();
        for (String id : getSelectedObjectIDs()) {
            out.append(id).append(" ");
        }
        LOGGER.info(out.toString());
    }

    /**
     * Delete all selected MCRObjects from the datastore.
     */
    public static void deleteSelected() throws MCRActiveLinkException {
        LOGGER.info("Start removing selected MCRObjects");
        if (getSelectedObjectIDs().isEmpty()) {
            LOGGER.info("No Resultset to work with, use command \"select objects with query {0}\" to build one");
            return;
        }
        for (String id : getSelectedObjectIDs()) {
            delete(id);
        }
        LOGGER.info("Selected MCRObjects deleted");
    }

    /**
     * Does a xsl transform with selected MCRObjects.
     * 
     * @param xslFilePath file to transform the objects
     * @return a list of transform commands
     * @throws MCRActiveLinkException
     * @see {@link #xslt(String, String)}
     */
    public static List<String> transformSelected(String xslFilePath) {
        LOGGER.info("Start transforming selected MCRObjects");
        File xslFile = new File(xslFilePath);
        if (!xslFile.exists()) {
            LOGGER.error("XSLT file not found " + xslFilePath);
            return new ArrayList<String>();
        }
        if (getSelectedObjectIDs().isEmpty()) {
            LOGGER.info("No Resultset to work with, use command \"select objects with query {0}\" to build one");
            return new ArrayList<String>();
        }
        List<String> commandList = new ArrayList<String>();
        for (String mcrId : getSelectedObjectIDs()) {
            commandList.add("xslt " + mcrId + " with file " + xslFilePath);
        }
        return commandList;
    }

    /*
     * Export selected MCRObjects to a file named <em>MCRObjectID</em> .xml in a directory. The method use the converter stylesheet
     * mcr_<em>style</em>_object.xsl.
     * @param dirname the dirname to store the object @param style the type of the stylesheet
     */
    public static List<String> exportSelected(String dirname, String style) {
        LOGGER.info("Start exporting selected MCRObjects");

        if (null == getSelectedObjectIDs()) {
            LOGGER.info("No Resultset to work with, use command \"select objects with query {0}\" to build one");
            return Collections.emptyList();
        }
        List<String> cmds = new ArrayList<String>(getSelectedObjectIDs().size());
        for (String id : getSelectedObjectIDs()) {
            cmds.add("export object from " + id + " to " + id + " to directory " + dirname + " with " + style);
        }
        return cmds;
    }

    /**
     * The method removes all selected entries from search index.
     * 
     * @param index
     *            index of searcher
     */
    public static void removeFromSearchindex(String index) {
        LOGGER.info("Start removing selected entries from search index " + index);

        MCRSearcher searcher = MCRSearcherFactory.getSearcherForIndex(index);

        if (null == getSelectedObjectIDs()) {
            LOGGER.info("No Resultset to work with, use command \"select objects with query {0}\" to build one");
            return;
        }
        for (String id : getSelectedObjectIDs()) {
            searcher.removeFromIndex(id);
        }
        LOGGER.info("Selected entries from search index removed");
    }

    /**
     * The method checks the existence of selected MCRObjects in SQL store.
     */
    public static void checkSelected() {
        LOGGER.info("Start checking existence of selected MCRObjects in sql store");

        if (null == getSelectedObjectIDs()) {
            LOGGER.info("No Resultset to work with, use command \"select objects with query {0}\" to build one");
            return;
        }

        int instore = 0;
        int notinstore = 0;

        for (String id : getSelectedObjectIDs()) {
            if (MCRMetadataManager.exists(MCRObjectID.getInstance(id))) {
                instore++;
            } else {
                LOGGER.info("is not in store " + id + " delete from search index ...");
                removeFromIndex("id", id);
                notinstore++;
            }
        }

        LOGGER.info("entries in Resultset    : " + getSelectedObjectIDs().size());
        LOGGER.info("entries in SQL Store    : " + instore);
        LOGGER.info("entries NOT in SQL Store: " + notinstore);
    }

    /**
     * Checks existence of MCRObjectID type {0} in search index and rapairs missing ones in search index.
     * 
     * @param type
     *            the MCRObjectID type
     */
    public static void checkMetadataSearch(String type) {
        LOGGER.info("Start the check for type " + type);
        String typetest = CONFIG.getString("MCR.Metadata.Type." + type, "");

        if (typetest.length() == 0) {
            LOGGER.error("The type " + type + " was not found.");
            return;
        }
        List<String> ar = (List<String>) MCRXMLMetadataManager.instance().listIDsOfType(type);
        if (ar.size() == 0) {
            LOGGER.warn("No ID's was found for type " + type + ".");
            return;
        }

        for (String stid : ar) {
            String querystring = "id = " + stid;
            MCRCondition cond = new MCRQueryParser().parse(querystring);
            MCRQuery query = new MCRQuery(cond);
            MCRResults results = MCRQueryManager.search(query);
            if (1 != results.getNumHits()) {
                repairMetadataSearchForID(stid);
            }
        }
    }

    /**
     * Inform Searcher what is going on.
     * 
     * @param mode
     *            what is going on, for example rebuild insert ... finish
     * @param index
     *            of searcher
     */
    public static void notifySearcher(String mode, String index) {
        MCRSearcher searcher = MCRSearcherFactory.getSearcherForIndex(index);
        searcher.notifySearcher(mode);
    }

    /**
     * List revisions of an MyCoRe Object.
     * 
     * @param id
     *            id of MyCoRe Object
     */
    public static void listRevisions(String id) {
        MCRObjectID mcrId = MCRObjectID.getInstance(id);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            StringBuilder log = new StringBuilder("Revisions:\n");
            List<MCRMetadataVersion> revisions = MCRUtils.listRevisions(mcrId);
            for (MCRMetadataVersion revision : revisions) {
                log.append(revision.getRevision()).append(" ");
                log.append(revision.getType()).append(" ");
                log.append(sdf.format(revision.getDate())).append(" ");
                log.append(revision.getUser());
                log.append("\n");
            }
            LOGGER.info(log.toString());
        } catch (Exception exc) {
            LOGGER.error("While print revisions.", exc);
        }
    }

    /**
     * This method restores a MyCoRe Object to the selected revision. Please note
     * that children and derivates are not deleted or reverted!
     *
     * @param id
     *            id of MyCoRe Object
     * @param revision
     *            revision to restore
     * @throws MCRActiveLinkException
     *             if object is created (no real update)
     */
    public static void restoreToRevision(String id, int revision) throws MCRActiveLinkException {
        LOGGER.info("Try to restore object " + id + " with revision " + revision);
        MCRObjectID mcrId = MCRObjectID.getInstance(id);
        // get xml of revision
        Document xml = null;
        try {
            xml = MCRUtils.requestVersionedObject(mcrId, revision);
            if (xml == null) {
                LOGGER.warn("No such object " + id + " with revision " + revision);
                return;
            }
        } catch (Exception e) {
            LOGGER.error("While retrieving object " + id + " with revision " + revision, e);
            return;
        }
        // store it
        MCRObject mcrObj = new MCRObject(xml);
        MCRMetadataManager.update(mcrObj);
        LOGGER.info("Object " + id + " successfully restored!");
    }

    public static void deleteByQuery(String source) throws Exception {
        LOGGER.info("Given query is \"" + source + "\"");
        if (source == null || source.length() == 0) {
            LOGGER.error("Given query is invalid");
            return;
        }

        MCRCondition condition = null;
        try {
            condition = new MCRQueryParser().parse(source);
        } catch (Exception ex) {
            LOGGER.error("Exception occured while parsing the input string", ex);
            return;
        }

        MCRQuery q = new MCRQuery(condition);
        MCRResults results = MCRQueryManager.search(q);

        if (results == null) {
            return;
        }
        File deletedItems = new File(System.getenv("HOME") + File.separator + System.currentTimeMillis()
                + "_deleted_objects.txt");
        FileWriter fw = new FileWriter(deletedItems);
        try {
            fw.write("query=" + source + "\n\n");
            for (MCRHit hit : results) {
                fw.write(hit.getID() + "\n");
                MCRMetadataManager.deleteMCRObject(MCRObjectID.getInstance(hit.getID()));
            }
        } catch (Exception ex) {
            LOGGER.error(ex);
        } finally {
            fw.flush();
            fw.close();
        }

        LOGGER.info("A list with the identifiers of the deleted items has been saved to "
                + deletedItems.getAbsolutePath());
    }

    /**
     * Lists all identifiers where the associated objects are matching the given query.
     * 
     * @param qSource the query to execute
     * @throws Exception
     */
    public static void listIdsMatchingQuery(String qSource) throws Exception {
        LOGGER.info("Given query is \"" + qSource + "\"");
        if (qSource == null || qSource.length() == 0) {
            LOGGER.error("Given query is invalid");
            return;
        }

        MCRCondition condition = null;
        try {
            condition = new MCRQueryParser().parse(qSource);
        } catch (Exception ex) {
            LOGGER.error("Exception occured while parsing the input string", ex);
            return;
        }

        MCRQuery q = new MCRQuery(condition);
        MCRResults results = MCRQueryManager.search(q);

        if (results == null) {
            return;
        }
        File matchingItems = new File(System.getenv("HOME") + File.separator + System.currentTimeMillis()
                + "_matched.txt");
        FileWriter fw = new FileWriter(matchingItems);
        try {
            fw.write("query=" + qSource + "\n\n");
            for (MCRHit hit : results) {
                LOGGER.info(hit.getID());
                fw.write(hit.getID() + "\n");
            }
        } catch (Exception ex) {
            LOGGER.error(ex);
        } finally {
            fw.flush();
            fw.close();
        }

        LOGGER.info("A list with the identifiers matching query \"" + qSource + "\" has been saved to "
                + matchingItems.getAbsolutePath());
    }

    /**
     * Does a xsl transform with the given mycore object.
     * <p>
     * To use this command create a new xsl file and copy following xslt code into it.
     * </p>
     * <?xml version="1.0" encoding="utf-8"?>
     * <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
     *
     *   <xsl:template match='@*|node()'>
     *     <!-- default template: just copy -->
     *     <xsl:copy>
     *       <xsl:apply-templates select='@*|node()' />
     *     </xsl:copy>
     *   </xsl:template>
     *
     * </xsl:stylesheet>
     * 
     * <p>Insert a new template match, for example:</p>
     * <xsl:template match="metadata/mainTitle/@heritable">
     *   <xsl:attribute name="heritable"><xsl:value-of select="'true'"/></xsl:attribute>
     * </xsl:template>
     * 
     * @param objectId object to transform
     * @param xslFilePath path to xsl file
     * @throws Exception
     */
    public static void xslt(String objectId, String xslFilePath) throws Exception {
        File xslFile = new File(xslFilePath);
        if (!xslFile.exists()) {
            LOGGER.error("XSLT file not found " + xslFilePath);
            return;
        }
        MCRObjectID mcrId = MCRObjectID.getInstance(objectId);
        Document doc = MCRXMLMetadataManager.instance().retrieveXML(mcrId);
        // do XSL transform
        Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(xslFile));
        transformer.setURIResolver(MCRURIResolver.instance());
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        JDOMResult result = new JDOMResult();
        transformer.transform(new JDOMSource(doc), result);
        // write to mycore
        MCRXMLMetadataManager.instance().update(mcrId, result.getDocument(), new Date(System.currentTimeMillis()));
    }

    /**
     * Moves object to new parent.
     * 
     * @param sourceId
     *            object that should be attached to new parent
     * @param newParentId
     *            the ID of the new parent
     * @throws MCRPersistenceException 
     * @throws MCRActiveLinkException
     */
    public static void replaceParent(String sourceId, String newParentId) throws MCRPersistenceException,
            MCRActiveLinkException {
        // child
        MCRObject sourceMCRObject = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(sourceId));
        // old parent
        MCRObjectID oldParentId = sourceMCRObject.getStructure().getParentID();

        if (newParentId.equals(oldParentId)) {
            LOGGER.info("Object " + sourceId + " is already child of " + newParentId);
            return;
        }

        MCRObject oldParentMCRObject = null;

        if (oldParentId != null) {
            oldParentMCRObject = MCRMetadataManager.retrieveMCRObject(oldParentId);
        }

        // change href to new parent
        LOGGER.info("Setting link in \"" + sourceId + "\" to parent \"" + newParentId + "\"");
        MCRMetaLinkID parentLinkId = new MCRMetaLinkID("parent", 0);
        parentLinkId.setReference(newParentId, null, null);
        sourceMCRObject.getStructure().setParent(parentLinkId);

        if (oldParentMCRObject != null) {
            // remove Child in old parent
            LOGGER.info("Remove child \"" + sourceId + "\" in old parent \"" + oldParentId + "\"");
            oldParentMCRObject.getStructure().removeChild(sourceMCRObject.getId());

            LOGGER.info("Update old parent \"" + oldParentId + "\n");
            MCRMetadataManager.update(oldParentMCRObject);
        }

        LOGGER.info("Update \"" + sourceId + "\" in datastore (saving new link)");
        MCRMetadataManager.update(sourceMCRObject);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Structure: " + sourceMCRObject.getStructure().isValid());
            LOGGER.debug("Object: " + sourceMCRObject.isValid());
        }
    }

}
