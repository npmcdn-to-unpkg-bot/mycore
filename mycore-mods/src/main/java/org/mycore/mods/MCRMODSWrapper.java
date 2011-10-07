/*
 * $Revision: 5697 $ $Date: 07.04.2011 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetaXML;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectMetadata;
import org.mycore.datamodel.metadata.MCRObjectService;

/**
 * @author Frank L\u00FCtzenkirchen
 * @author Thomas Scheffler
 */
public class MCRMODSWrapper {

    private static final String MODS_CONTAINER = "modsContainer";

    private static final String DEF_MODS_CONTAINER = "def.modsContainer";

    public static final String MODS_OBJECT_TYPE = "mods";

    private static final String MODS_DATAMODEL = "datamodel-mods.xsd";

    private static List<String> topLevelElementOrder = new ArrayList<String>();

    static {
        topLevelElementOrder.add("titleInfo");
        topLevelElementOrder.add("originInfo");
        topLevelElementOrder.add("language");
        topLevelElementOrder.add("abstract");
        topLevelElementOrder.add("subject");
        topLevelElementOrder.add("relatedItem");
        topLevelElementOrder.add("identifier");
        topLevelElementOrder.add("location");
        topLevelElementOrder.add("accessCondition");
    }

    private static int getRankOf(Element topLevelElement) {
        return topLevelElementOrder.indexOf(topLevelElement.getName());
    }

    public static MCRObject wrapMODSDocument(Element modsDefinition, String projectID) {
        MCRMODSWrapper wrapper = new MCRMODSWrapper();
        wrapper.setID(projectID, 0);
        wrapper.setMODS(modsDefinition);
        return wrapper.getMCRObject();
    }

    private MCRObject object;

    private Element mods;

    public MCRMODSWrapper() {
        object = new MCRObject();
        object.setSchema(MODS_DATAMODEL);
    }

    public MCRObject getMCRObject() {
        return object;
    }

    public MCRObjectID setID(String projectID, int ID) {
        MCRObjectID objID = MCRObjectID.getInstance(MCRObjectID.formatID(projectID, MODS_OBJECT_TYPE, ID));
        object.setId(objID);
        return objID;
    }

    public void setMODS(Element mods) {
        MCRObjectMetadata om = object.getMetadata();
        if (om.getMetadataElement(DEF_MODS_CONTAINER) != null)
            om.removeMetadataElement(DEF_MODS_CONTAINER);

        MCRMetaXML modsContainer = new MCRMetaXML(MODS_CONTAINER, null, 0);
        List<MCRMetaXML> list = Collections.nCopies(1, modsContainer);
        MCRMetaElement defModsContainer = new MCRMetaElement(MCRMetaXML.class, DEF_MODS_CONTAINER, false, true, list);
        om.setMetadataElement(defModsContainer);

        this.mods = mods;
        modsContainer.addContent(mods);
    }

    public Element getMODS() {
        return mods;
    }

    private XPath buildXPath(String xPath) throws JDOMException {
        XPath path = XPath.newInstance(xPath);
        path.addNamespace(MCRConstants.MODS_NAMESPACE);
        path.addNamespace(MCRConstants.XLINK_NAMESPACE);
        return path;
    }

    public Element getElement(String xPath) {
        try {
            return (Element) (buildXPath(xPath).selectSingleNode(mods));
        } catch (JDOMException ex) {
            String msg = "Could not get MODS element from " + xPath;
            throw new MCRException(msg, ex);
        }
    }

    public List<Element> getElements(String xPath) {
        try {
            return (List<Element>) (buildXPath(xPath).selectNodes(mods));
        } catch (JDOMException ex) {
            String msg = "Could not get elements at " + xPath;
            throw new MCRException(msg, ex);
        }
    }

    public String getElementValue(String xPath) {
        Element element = getElement(xPath);
        return (element == null ? null : element.getTextTrim());
    }

    public void setElement(String elementName, String attributeName, String attributeValue, String elementValue) {
        String xPath = "mods:" + elementName + "[@" + attributeName + "='" + attributeValue + "']";
        Element element = getElement(xPath);

        if (element == null) {
            element = addElement(elementName);
            element.setAttribute(attributeName, attributeValue);
        }

        if (elementValue != null)
            element.setText(elementValue.trim());
        else
            element.detach();
    }

    public Element addElement(String elementName) {
        Element element = new Element(elementName, MCRConstants.MODS_NAMESPACE);
        insertTopLevelElement(element);
        return element;
    }

    private void insertTopLevelElement(Element element) {
        int rankOfNewElement = getRankOf(element);
        List<Element> topLevelElements = mods.getChildren();
        for (int pos = 0; pos < topLevelElements.size(); pos++)
            if (getRankOf(topLevelElements.get(pos)) > rankOfNewElement) {
                mods.addContent(pos, element);
                return;
            }

        mods.addContent(element);
    }

    public void removeElements(String xPath) {
        Iterator<Element> selected;
        try {
            selected = buildXPath(xPath).selectNodes(mods).iterator();
        } catch (JDOMException ex) {
            String msg = "Could not remove elements at " + xPath;
            throw new MCRException(msg, ex);
        }

        while (selected.hasNext()) {
            Element element = selected.next();
            selected.remove();
            element.detach();
        }
    }

    public String getServiceFlag(String type) {
        MCRObjectService os = object.getService();
        return (os.isFlagTypeSet(type) ? os.getFlags(type).get(0) : null);
    }

    public void setServiceFlag(String type, String value) {
        MCRObjectService os = object.getService();
        if (os.isFlagTypeSet(type))
            os.removeFlags(type);
        if ((value != null) && !value.trim().isEmpty())
            os.addFlag(type, value.trim());
    }
}
