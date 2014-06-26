package org.mycore.frontend.indexbrowser.lucene;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.mycore.common.config.MCRConfiguration;

/**
 * Contains all data of a specified index. The configuration data will be
 * read from the mycore properties file. Each index has to define the
 * parameters in the form: MCR.IndexBrowser.{index-id}.{property}
 *
 * @author Matthias Eichner
 */
public class MCRIndexBrowserConfig {

    private String index;

    private String browseField;

    private List<String> outputFields;

    private List<String> sortFields;

    private String order;

    private int maxPerPage;

    public MCRIndexBrowserConfig(String searchclass) {
        set(searchclass);
    }

    public void set(String searchclass) {
        MCRConfiguration config = MCRConfiguration.instance();
        String prefix = "MCR.IndexBrowser." + searchclass + ".";
        index = config.getString(prefix + "Table");
        browseField = config.getString(prefix + "Searchfield");
        maxPerPage = config.getInt(prefix + "MaxPerPage");
        String fields = config.getString(prefix + "ExtraOutputFields", null);
        String fieldToSort = config.getString(prefix + "FieldsToSort", null);
        order = config.getString(prefix + "Order", "ascending");
        outputFields = buildFieldList(fields);
        sortFields = buildFieldList(fieldToSort);
    }

    private List<String> buildFieldList(String myfields) {
        ArrayList<String> list = new ArrayList<String>();
        if (myfields != null) {
            StringTokenizer st = new StringTokenizer(myfields, ",");
            while (st.hasMoreTokens()) {
                list.add(st.nextToken());
            }
        }
        return list;
    }

    public String getIndex() {
        return index;
    }

    public String getBrowseField() {
        return browseField;
    }

    public List<String> getOutputList() {
        return outputFields;
    }

    public List<String> getSortFields() {
        return sortFields;
    }

    public String getOrder() {
        return order;
    }

    public int getMaxPerPage() {
        return maxPerPage;
    }

}