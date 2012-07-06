package org.mycore.migration21_22.cli;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.jdom.xpath.XPath;
import org.mycore.common.xml.MCRXSLTransformation;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;

public class MCRMigrationCommands22 extends MCRAbstractCommands {
    private static Logger LOGGER = Logger.getLogger(MCRMigrationCommands22.class);

    public MCRMigrationCommands22() {
    }

    @MCRCommand(help = "Move xlink label which is not NCName to xlink:title.", syntax = "migrate xlink label")
    public static void xlinkLabelMigration() throws TransformerException, JDOMException, IOException {
        MCRXMLMetadataManager xmlMetaManager = MCRXMLMetadataManager.instance();
        List<String> listIDs = xmlMetaManager.listIDs();
        
        InputStream resourceAsStream = MCRMigrationCommands22.class.getResourceAsStream("/xsl/xlinkLabelMigration.xsl");
        Source stylesheet = new StreamSource(resourceAsStream);
        Transformer xsltTransformer = MCRXSLTransformation.getInstance().getStylesheet(stylesheet).newTransformer();

        XPath xlinkLabel = XPath.newInstance("/mycoreobject/*/*[starts-with(@class,'MCRMetaLink')]/*/@xlink:label");
        for (String ID : listIDs) {
            MCRObjectID mcrid = MCRObjectID.getInstance(ID);
            Document mcrObjXML = xmlMetaManager.retrieveXML(mcrid);
            if (!xlinkLabel.selectNodes(mcrObjXML).isEmpty()) {
                Source xmlSource = new JDOMSource(mcrObjXML);
                JDOMResult jdomResult = new JDOMResult();
                xsltTransformer.transform(xmlSource, jdomResult);
                Document migratedMcrObjXML = jdomResult.getDocument();

                xmlMetaManager.update(mcrid, migratedMcrObjXML, new Date());
                LOGGER.info("Migrated xlink for " + mcrid);
            } else {
                LOGGER.info("No xlink migration for " + mcrid);
            }
        }
    }
}
