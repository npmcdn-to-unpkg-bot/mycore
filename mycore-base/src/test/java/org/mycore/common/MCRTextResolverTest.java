package org.mycore.common;

import java.util.Hashtable;

public class MCRTextResolverTest extends MCRTestCase {

    public void testVariables() throws Exception {
        Hashtable<String, String> variablesTable = new Hashtable<String, String>();
        variablesTable.put("f1", "v1");
        variablesTable.put("f2", "v2");
        variablesTable.put("f3", "v3");
        variablesTable.put("f4", "v4 - {f1}");
        variablesTable.put("f5", "[{f1}[_{e1}]]");
        variablesTable.put("f6", "[[[[{f3}]]]]");

        variablesTable.put("num", "10");
        variablesTable.put("add", "5");
        variablesTable.put("x_10_5", "value1");
        variablesTable.put("x_10", "value2");

        MCRTextResolver resolver = new MCRTextResolver(variablesTable);

        // some simple variables tests
        assertEquals("v1", resolver.resolve("{f1}"));
        assertEquals("v2 & v3", resolver.resolve("{f2} & {f3}"));
        // internal variables
        assertEquals("v4 - v1", resolver.resolve("{f4}"));
        // conditions
        assertEquals("v1_v2", resolver.resolve("{f1}[_{f2}][_{e1}]"));
        // internal conditions
        assertEquals("", resolver.resolve("{f5}"));
        // advanced condition tests
        assertEquals("v3", resolver.resolve("{f6}"));
        // escaping char test
        assertEquals("[{v1}] \\", resolver.resolve("\\[\\{{f1}\\}\\] \\\\"));
        // resolving variables in an other variable
        assertEquals("value1", resolver.resolve("{x_{num}[_{add}]}"));
        assertEquals("value2", resolver.resolve("{x_{num}[_{add2}]}"));

        // hash table and list size tests
        resolver.resolve("{f1}, {f3}, {notInTable}, {add}");
        assertEquals(3, resolver.getResolvedVariables().size());
        assertEquals(1, resolver.getUnresolvedVariables().size());
        assertEquals(4, resolver.getUsedVariables().size());
        assertEquals(7, resolver.getNotUsedVariables().size());
        assertEquals(false, resolver.isCompletelyResolved());

        resolver.resolveNext("{f3}, {f6}");
        assertEquals(4, resolver.getResolvedVariables().size());
        assertEquals(1, resolver.getUnresolvedVariables().size());
        assertEquals(5, resolver.getUsedVariables().size());
        assertEquals(6, resolver.getNotUsedVariables().size());
        assertEquals(false, resolver.isCompletelyResolved());
    }

    public void testProperties() throws Exception {
        MCRConfiguration.instance().set("prop1", "propValue1");
        MCRConfiguration.instance().set("prop2", "propValue2");

        Hashtable<String, String> variablesTable = new Hashtable<String, String>();
        variablesTable.put("f1", "v1");
        MCRTextResolver resolver = new MCRTextResolver(variablesTable);

        assertEquals("propValue1", resolver.resolve("{prop1}"));
        assertEquals("propValue1 - v1 - propValue2", resolver.resolve("{prop1} - {f1} - {prop2}"));
        assertEquals("test: propValue1", resolver.resolve("[test: {prop1}]"));
        assertEquals("", resolver.resolve("[test: {prop3}]"));
        resolver.setUseMCRProperties(false);
        assertEquals("", resolver.resolve("[test: {prop1}]"));
    }
    
    public void testAddRemove() throws Exception {
        Hashtable<String, String> variablesTable = new Hashtable<String, String>();
        variablesTable.put("f1", "v1");
        variablesTable.put("f2", "v2");
        variablesTable.put("f3", "v3");
        MCRTextResolver resolver = new MCRTextResolver(variablesTable);
        resolver.addVariable("f4", "v4");
        assertEquals("v1, v2, v3, v4", resolver.resolve("{f1}, {f2}, {f3}, {f4}"));
        resolver.removeVariable("f2");
        resolver.removeVariable("f3");
        assertEquals("v1, v4", resolver.resolve("[{f1}][, {f2}][, {f3}][, {f4}]"));
        assertEquals(true, resolver.containsVariable("f1"));
        assertEquals(false, resolver.containsVariable("f2"));
    }
}