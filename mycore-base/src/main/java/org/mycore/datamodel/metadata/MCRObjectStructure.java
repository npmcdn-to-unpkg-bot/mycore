/*
 * 
 * $Revision$ $Date$
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

package org.mycore.datamodel.metadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * This class implements code for the inheritance of metadata of linked objects
 * and the linking of derivates onto an MCRObject. These links are described by
 * the <em>MCRMetaLink</em> class. For links to another object, there are
 * "locators" in use only, and the href variable gives the ID of the linked
 * object, while the label and title attributes can be used freely. Subtag name = "
 * &lt;child&gt;" means a child link from a "parent" object (collected in the
 * "children" and "parents" section of the "structure" part, respectively). The
 * child inherits all heritable metadata of the parent. If the parent itself is
 * a child of another parent, the heritable metadata of this "grand parent" is
 * inherited by the child as well. This mechanism recursively traces the full
 * inheritance hierarchy. So if the grand parent itself has a parent, this grand
 * parent parent's heritable metadata will be inherited and so on. Note, that it
 * is impossible to inherit metadata from multiple parents. In cases of multiple
 * inheritance request, an exception is thrown. A child link cannot occur twice
 * from the same object to the same href (preventing from doubled links). Not
 * supported by this class are links from or to a defined place of a document
 * (inner structure and combination of inner and outer structures of the
 * objects). This will possibly be done in a later extension of
 * <em>MCRMetaLink</em> and <em>MCRObjectStructure</em>.
 * 
 * @author Mathias Hegner
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date: 2008-02-06 18:27:24 +0100 (Mi, 06 Feb
 *          2008) $
 */
public class MCRObjectStructure {

    private MCRMetaLinkID parent;

    private final ArrayList<MCRMetaLinkID> children;

    private final ArrayList<MCRMetaLinkID> derivates;

    private static final Logger LOGGER = Logger.getLogger(MCRObjectStructure.class);

    /**
     * The constructor initializes NL (non-static, in order to enable different
     * NL's for different objects) and the link vectors the elements of which
     * are MCRMetaLink's.
     */
    public MCRObjectStructure() {
        children = new ArrayList<MCRMetaLinkID>();
        derivates = new ArrayList<MCRMetaLinkID>();
    }

    /**
     * This method clean the data lists parent, children and derivates of this
     * class.
     */
    public final void clear() {
        parent = null;
        children.clear();
        derivates.clear();
    }

    /**
     * This method clean the data lists children of this class.
     */
    public final void clearChildren() {
        children.clear();
    }

    /**
     * This method clean the data lists derivate of this class.
     */
    public final void clearDerivates() {
        derivates.clear();
    }

    /**
     * The method returns the parent link.
     * 
     * @return MCRMetaLinkID the corresponding link
     */
    public final MCRMetaLinkID getParent() {
        return parent;
    }

    /**
     * The method return the parent reference as a MCRObjectID.
     * 
     * @return the parent MCRObjectID or null if there is no parent present
     */
    public final MCRObjectID getParentID() {
        if (parent == null) {
            return null;
        }
        return parent.getXLinkHrefID();
    }

    /**
     * This method set the parent value from a given MCRMetaLinkID.
     * 
     * @param in_parent
     *            the MCRMetaLinkID to set
     *  
     */
    public final void setParent(MCRMetaLinkID in_parent) {
        parent = in_parent;
    }

    public final void setParent(MCRObjectID parentID) {
        setParent(parentID.toString());
    }

    public final void setParent(String parentID) {
        parent = new MCRMetaLinkID();
        parent.setSubTag("parent");
        parent.setReference(parentID, null, null);
    }

    /**
     * The method appends a child ID to the child link list if and only if it is
     * not already contained in the list, preventing from doubly-linked objects.
     * If the link could be added a "true" will be returned, otherwise "false".
     * 
     * @param child
     *            the MCRMetaLinkID of the child
     * @return boolean true, if successfully done
     */
    public final boolean addChild(MCRMetaLinkID child) {
        for (MCRMetaLinkID c : children) {
            if (c.getXLinkHrefID().equals(child.getXLinkHrefID())) {
                return false;
            }
        }
        children.add(child);

        return true;
    }

    /**
     * removes a child link to another object.
     *  If the link was found a "true" will be returned, otherwise
     * "false".
     * 
     * @param href
     *            the MCRObjectID of the child
     * @return boolean true, if successfully completed
     */
    public final boolean removeChild(MCRObjectID href) {
        LOGGER.debug("Remove child ID " + href);
        return removeMetaLink(getChildren().iterator(), href);
    }

    /**
     * removes a derivate link.
     * If the link was found a "true" will be returned, otherwise
     * "false".
     * 
     * @param href
     *            the MCRObjectID of the child
     * @return boolean true, if successfully completed
     */
    public final boolean removeDerivate(MCRObjectID href) {
        LOGGER.debug("Remove derivate ID " + href);
        return removeMetaLink(getDerivates().iterator(), href);
    }

    /**
     * Removes a MCRMetaLinkID instance by it MCRObjectID.
     * @param it
     * @param href
     * @return
     */
    private boolean removeMetaLink(Iterator<MCRMetaLinkID> it, MCRObjectID href) {
        while (it.hasNext()) {
            MCRMetaLinkID derLink = it.next();
            if (derLink.getXLinkHrefID().equals(href)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all children in this structure
     * */
    public final List<MCRMetaLinkID> getChildren() {
        return children;
    }

    /**
     * <em>addDerivate</em> methode append the given derivate link data to the
     * derivate vector. If the link could be added a "true" will be returned,
     * otherwise "false".
     * 
     * @param add_derivate
     *            the link to be added as MCRMetaLinkID
     */
    public final boolean addDerivate(MCRMetaLinkID add_derivate) {
        MCRObjectID href = add_derivate.getXLinkHrefID();
        if (containsDerivate(href)) {
            return false;
        }
        if (!MCRMetadataManager.exists(href)) {
            LOGGER.warn("Cannot find derivate " + href.toString() + ", will add it anyway.");
        }
        derivates.add(add_derivate);
        return true;
    }

    /**
     * Adds or updates the derivate link. Returns true if the derivate is added
     * or updated. Returns false when nothing is done.
     * 
     * @param derivateLink the link to add or update
     * @return true when the structure is changed
     */
    public final boolean addOrUpdateDerivate(MCRMetaLinkID derivateLink) {
        if (derivateLink == null) {
            return false;
        }
        MCRObjectID derivateId = derivateLink.getXLinkHrefID();
        MCRMetaLinkID oldLink = getDerivateLink(derivateId);
        if(derivateLink.equals(oldLink)) {
            return false;
        }
        if (oldLink != null) {
            removeDerivate(oldLink.getXLinkHrefID());
        }
        return addDerivate(derivateLink);
    }

    /**
     * Checks if the derivate is in the derivate vector.
     * 
     * @param derivateId derivate to check
     */
    public final boolean containsDerivate(MCRObjectID derivateId) {
        return getDerivateLink(derivateId) != null;
    }

    /**
     * Returns the derivate link by id or null.
     */
    public final MCRMetaLinkID getDerivateLink(MCRObjectID derivateId) {
        for (MCRMetaLinkID derivate : getDerivates()) {
            if (derivate.getXLinkHrefID().equals(derivateId)) {
                return derivate;
            }
        }
        return null;
    }

    /** 
     * @return a list with all related derivate ids encapsulated within a {@link MCRMetaLinkID}
     * */
    public List<MCRMetaLinkID> getDerivates() {
        return this.derivates;
    }

    /**
     * While the preceding methods dealt with the structure's copy in memory
     * only, the following three will affect the operations to or from datastore
     * too. Thereby <em>setFromDOM</em> will read the structure data from an
     * XML input stream (the "structure" entry).
     * 
     * @param element
     *            the structure node list
     */
    public final void setFromDOM(Element element) {
        clear();
        Element subElement = element.getChild("children");

        if (subElement != null) {
            List<Element> childList = subElement.getChildren();

            for (Element linkElement : childList) {
                MCRMetaLinkID link = new MCRMetaLinkID();
                link.setFromDOM(linkElement);
                children.add(link);
            }
        }

        // Stricture parent part
        subElement = element.getChild("parents");

        if (subElement != null) {
            parent = new MCRMetaLinkID();
            parent.setFromDOM(subElement.getChild("parent"));
        }

        // Structure derivate part
        subElement = element.getChild("derobjects");

        if (subElement != null) {
            List<Element> derobjectList = subElement.getChildren();

            for (Element derElement : derobjectList) {
                MCRMetaLinkID der = new MCRMetaLinkID();
                der.setFromDOM(derElement);
                addDerivate(der);
            }
        }
    }

    /**
     * <em>createXML</em> is the inverse of setFromDOM and converts the
     * structure's memory copy into an XML string.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return org.jdom2.Element the structure XML string
     */
    public final Element createXML() throws MCRException {
        if (!isValid()) {
            throw new MCRException("The content is not valid.");
        }

        Element elm = new Element("structure");
        
        if (parent != null) {
            Element elmm = new Element("parents");
            elmm.setAttribute("class", "MCRMetaLinkID");
            elmm.addContent(parent.createXML());
            elm.addContent(elmm);
        }

        if (children.size() > 0) {
            Element elmm = new Element("children");
            elmm.setAttribute("class", "MCRMetaLinkID");
            for (MCRMetaLinkID child : getChildren()) {
                elmm.addContent(child.createXML());
            }
            elm.addContent(elmm);
        }

        if (derivates.size() > 0) {
            Element elmm = new Element("derobjects");
            elmm.setAttribute("class", "MCRMetaLinkID");
            for (MCRMetaLinkID derivate : getDerivates()) {
                elmm.addContent(derivate.createXML());
            }
            elm.addContent(elmm);
        }

        return elm;
    }

    /**
     * Creates the JSON representation of this structure.
     * 
     * <pre>
     *   {
     *     parent: {@link MCRMetaLinkID#createJSON()},
     *     children: [
     *      {@link MCRMetaLinkID#createJSON()}
     *      ...
     *     ],
     *     derivates: [
     *       {@link MCRMetaLinkID#createJSON()}
     *        ...
     *     ]
     *   }
     * </pre>
     * 
     * @return a json gson representation of this structure
     */
    public JsonObject createJSON() {
        JsonObject structure = new JsonObject();
        // parent
        Optional.ofNullable(getParent()).ifPresent(link -> {
            structure.add("parent", link.createJSON());
        });
        // children
        JsonArray children = new JsonArray();
        getChildren().forEach(child -> {
            children.add(child.createJSON());
        });
        structure.add("children", children);
        // derivates
        JsonArray derivates = new JsonArray();
        getDerivates().forEach(derivate -> {
            derivates.add(derivate.createJSON());
        });
        structure.add("derivates", derivates);
        return structure;
    }

    /**
     * The method print all informations about this MCRObjectStructure.
     */
    public final void debug() {
        for (MCRMetaLinkID linkID : derivates) {
            linkID.debug();
        }
        if (parent != null) {
            parent.debug();
        }
        for (MCRMetaLinkID linkID : children) {
            linkID.debug();
        }
    }
    
    /**
     * <em>isValid</em> checks whether all of the MCRMetaLink's in the link
     * vectors are valid or not.
     * 
     * @return boolean true, if structure is valid
     */
    public final boolean isValid() {
        for (MCRMetaLinkID child : getChildren()) {
            if (!child.isValid()) {
                return false;
            }
        }

        if (parent != null) {
            if (!parent.isValid()) {
                return false;
            }
        }

        for (MCRMetaLinkID derivate : getDerivates()) {
            if (!derivate.isValid()) {
                return false;
            }

            if (!derivate.getXLinkType().equals("locator")) {
                return false;
            }

            if (!derivate.getXLinkHrefID().getTypeId().equals("derivate")) {
                return false;
            }
        }

        return true;
    }

}
