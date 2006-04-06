/*
 * $RCSfile$
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

package org.mycore.datamodel.classifications.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassification;
import org.mycore.datamodel.metadata.MCRLinkTableManager;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRClassificationQuery {

    /**
     * returns a classification as POJO.
     * 
     * @param ID
     *            MCR classification ID.
     * @param levels
     *            of category depth.
     * @return
     */
    public static Classification getClassification(String ID, int levels) {
        Document cl = MCRClassification.receiveClassificationAsJDOM(ID);
        return getClassification(cl, levels);
    }

    /**
     * returns a classification as POJO. Only the given Category (and its
     * children to <code>levels</code> depth) is returned.
     * 
     * @param ID
     *            MCR classification ID.
     * @param categID
     *            MCR category ID.
     * @param levels
     *            of category depth.
     * @return
     */
    public static Classification getClassification(String classID, String categID, int levels) {
        Classification returns = getClassification(classID, categID);
        MCRCategoryItem catItem = MCRCategoryItem.getCategoryItem(classID, categID);
        // map of every categID with numberofObjects
        Map map = MCRLinkTableManager.instance().countReferenceCategory(classID);
        // FIXME: remove debugMethod after tests
        printMap(map);
        Category cat = (Category) returns.getCatgegories().get(0);
        fillCategory(cat, catItem, map, levels);
        return returns;
    }

    private static void printMap(Map map) {
        Iterator it = map.entrySet().iterator();
        System.out.println("Print Map:");
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
    }

    private static int getNumberOfObjects(String classID, String categID, Map map) {
        String mapKey = classID + "##" + categID;
        int count = (map.get(mapKey) != null) ? ((Integer) map.get(mapKey)).intValue() : 0;
        return count;
    }

    /**
     * returns a classification as POJO. Only the given Category is returned.
     * 
     * @param ID
     *            MCR classification ID.
     * @param categID
     *            MCR category ID.
     * @return
     */
    public static Classification getClassification(String classID, String categID) {
        Document doc = MCRClassification.receiveCategoryAsJDOM(classID, categID);
        Classification returns = getClassification(doc, -1);
        return returns;
    }

    public static void main(String[] arg) throws IOException {
        Classification c = MCRClassificationQuery.getClassification(arg[0], 1);
        print(c, 0);
        c = MCRClassificationQuery.getClassification(arg[0], arg[1], 0);
        print(c, 0);
        Document doc = ClassificationTransformer.getMetaDataDocument(c);
        print(doc);
        doc = ClassificationTransformer.getEditorDocument(c);
        print(doc);
//        Document cl = MCRClassification.receiveCategoryAsJDOM(arg[0], arg[1]);
        doc = MCRClassification.receiveClassificationAsJDOM(arg[0]);
        print(doc);
        c = MCRClassificationQuery.getClassification(arg[0], -1);
        doc = ClassificationTransformer.getMetaDataDocument(c);
        print(doc);
    }

    /**
     * @param cl
     * @param levels
     * @return
     */
    private static Classification getClassification(Document cl, int levels) {
        Classification returns = new Classification();
        returns.setId(cl.getRootElement().getAttributeValue("ID"));
        returns.getLabels().addAll(LabelFactory.getLabels(cl.getRootElement().getChildren("label")));
        fillCategory(returns.getId(), returns, cl.getRootElement().getChild("categories"), levels);
        return returns;
    }

    private static void fillCategory(ClassificationObject c, MCRCategoryItem item, Map map, int levels) {
        if (levels != 0) {
            MCRCategoryItem[] children = item.getChildren();
            for (int i = 0; i < children.length; i++) {
                MCRCategoryItem child = children[i];
                Category childC = CategoryFactory.getCategory(child);
                int count = getNumberOfObjects(item.getClassificationID(), item.getID(), map);
                childC.setNumberOfObjects(count);
                childC.setClassID(item.getClassificationID());
                c.getCatgegories().add(childC);
                fillCategory(childC, child, map, levels - 1);
            }
        }
    }

    private static void fillCategory(String classID, ClassificationObject c, Element e, int levels) {
        if (levels != 0) {
            List children = e.getChildren("category");
            Iterator it = children.iterator();
            while (it.hasNext()) {
                Element child = (Element) it.next();
                Category childC = CategoryFactory.getCategory(child);
                childC.setNumberOfObjects(getNumberOfObjects(child));
                childC.setClassID(classID);
                c.getCatgegories().add(childC);
                fillCategory(classID, childC, child, levels - 1);
            }
        }
    }

    private static int getNumberOfObjects(Element e) {
        String counter = e.getAttributeValue("counter");
        int returns = 0;
        if (counter != null) {
            returns = Integer.parseInt(counter);
        }
        return returns;
    }

    private static void print(ClassificationObject c, int depth) {
        intend(depth);
        System.out.println("ID: " + c.getId());
        Iterator it = c.getCatgegories().iterator();
        while (it.hasNext()) {
            print((ClassificationObject) it.next(), depth + 1);
        }
    }

    private static void print(Document doc) {
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        try {
            xout.output(doc, System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void intend(int a) {
        for (int i = 0; i < a; i++) {
            System.out.print(' ');
        }
    }

    private static class CategoryFactory {
        static Category getCategory(Element e) {
            Category c = new Category();
            c.setId(e.getAttributeValue("ID"));
            c.getLabels().addAll(LabelFactory.getLabels(e.getChildren("label")));
            return c;
        }

        static Category getCategory(MCRCategoryItem i) {
            Category c = new Category();
            c.setId(i.getID());
            c.getLabels().addAll(LabelFactory.getLabels(i.getLangArray(), i.getTextArray(), i.getDescriptionArray()));
            return c;
        }
    }

    private static class LabelFactory {
        static Label getLabel(Element e) {
            return getLabel(e.getAttributeValue("lang", Namespace.XML_NAMESPACE), e.getAttributeValue("text"), e.getAttributeValue("description"));
        }

        static Label getLabel(String lang, String text, String description) {
            Label label = new Label();
            label.setText(text);
            label.setDescription(description);
            label.setLang(lang);
            return label;
        }

        static List getLabels(List labels) {
            List returns = new ArrayList(labels.size());
            Iterator it = labels.iterator();
            while (it.hasNext()) {
                returns.add(getLabel((Element) it.next()));
            }
            return returns;
        }

        static List getLabels(List lang, List text, List description) {
            List returns = new ArrayList(lang.size());
            for (int i = 0; i < lang.size(); i++) {
                returns.add(getLabel(lang.get(i).toString(), text.get(i).toString(), description.get(i).toString()));
            }
            return returns;
        }
    }
}
