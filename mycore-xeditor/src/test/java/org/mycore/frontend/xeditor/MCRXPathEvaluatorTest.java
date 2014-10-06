/*
 * $Revision: 28699 $ 
 * $Date: 2013-12-19 21:45:48 +0100 (Do, 19 Dez 2013) $
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

package org.mycore.frontend.xeditor;

import static org.junit.Assert.*;

import java.util.List;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXPathEvaluatorTest extends MCRTestCase {

    private MCRXPathEvaluator evaluator;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        String builder = "document[name/@id='n1'][note/@href='#n1'][location/@href='#n1'][name[@id='n2']][location[@href='#n2']]";
        Element document = new MCRNodeBuilder().buildElement(builder, null, null);
        MCRBinding rootBinding = new MCRBinding(new Document(document));
        MCRBinding documentBinding = new MCRBinding("/document", false, rootBinding);
        evaluator = new MCRXPathEvaluator(documentBinding);
    }

    @Test
    public void testEvaluator() throws JaxenException, JDOMException {
        assertEquals("n1", evaluator.replaceXPathOrI18n("name[1]/@id"));
        assertEquals("n1", evaluator.replaceXPathOrI18n("/document/name[1]/@id"));
    }

    @Test
    public void testGenerateID() throws JaxenException, JDOMException {
        String id = evaluator.replaceXPathOrI18n("xedf:generate-id(/document)");
        assertEquals(id, evaluator.replaceXPathOrI18n("xedf:generate-id(.)"));
        assertFalse(id.equals(evaluator.replaceXPathOrI18n("xedf:generate-id(/document/name[1])")));

        id = evaluator.replaceXPathOrI18n("xedf:generate-id(/document/name[1])");
        assertEquals(id, evaluator.replaceXPathOrI18n("xedf:generate-id(/document/name[1])"));
        assertEquals(id, evaluator.replaceXPathOrI18n("xedf:generate-id(/document/name)"));
        assertFalse(id.equals(evaluator.replaceXPathOrI18n("xedf:generate-id(/document/name[2])")));

        id = evaluator.replaceXPathOrI18n("xedf:generate-id()");
    }

    @Test
    public void testJavaCall() throws JaxenException, JDOMException {
        String res = evaluator.replaceXPathOrI18n("xedf:call-java('org.mycore.frontend.xeditor.MCRXPathEvaluatorTest','testNoArgs')");
        assertEquals(testNoArgs(), res);

        res = evaluator.replaceXPathOrI18n("xedf:call-java('org.mycore.frontend.xeditor.MCRXPathEvaluatorTest','testOneArg',name[2])");
        assertEquals("n2", res);
    }

    public static String testNoArgs() {
        return "testNoArgs";
    }

    public static String testOneArg(Object nodeList) {
        List list = (List<Object>) nodeList;
        Object first = list.get(0);
        Element element = (Element) first;
        return element.getAttributeValue("id");
    }
}