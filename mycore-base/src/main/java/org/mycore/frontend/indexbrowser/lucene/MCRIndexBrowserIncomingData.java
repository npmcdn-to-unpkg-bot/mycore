package org.mycore.frontend.indexbrowser.lucene;

/**
 * Contains all incoming data from the web browser.
 *
 * @author Matthias Eichner
 */
public class MCRIndexBrowserIncomingData {

    private String searchclass;

    private int from = 0;

    private int to = Integer.MAX_VALUE - 10;

    private StringBuffer path;

    private String search;

    private String mode;

    private boolean init;

    public MCRIndexBrowserIncomingData(String search, String mode, String searchclass, String fromTo, String init) {
        set(search, mode, searchclass, fromTo, init);
    }

    public void set(String search, String mode, String searchclass, String fromTo, String init) {
        this.search = search;
        this.mode = mode;
        this.searchclass = searchclass;
        path = new StringBuffer(this.searchclass);
        path.append("/");
        if (fromTo != null && fromTo.length() > 0) {
            String from = fromTo.substring(0, fromTo.indexOf("-"));
            String to = fromTo.substring(fromTo.indexOf("-") + 1);
            this.from = Integer.parseInt(from);
            this.to = Integer.parseInt(to);
            updatePath();
        }
        this.init = Boolean.parseBoolean(init);
    }

    private void updatePath() {
        path.append(from);
        path.append("-");
        path.append(to);
        path.append("/");
    }

    public String getSearchclass() {
        return searchclass;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public String getPath() {
        return path.toString();
    }

    public String getSearch() {
        return search;
    }

    public String getMode() {
        return mode;
    }

    public boolean isInit() {
        return init;
    }
}