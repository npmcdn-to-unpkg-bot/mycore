package org.mycore.solr.index.cs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.log4j.Logger;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;

/**
 * Wraps objects to be sent to solr in a content stream.
 * 
 * @see {@link ContentStream}
 * 
 * @author shermann
 * @author Matthias Eichner
 * */
public abstract class MCRSolrAbstractContentStream<T> extends ContentStreamBase {

    final static Logger LOGGER = Logger.getLogger(MCRSolrAbstractContentStream.class);

    protected boolean setup;

    protected InputStream inputStream;

    protected InputStreamReader streamReader;

    private T source;

    public MCRSolrAbstractContentStream() {
        this(null);
    }

    public MCRSolrAbstractContentStream(T source) {
        this.inputStream = null;
        this.streamReader = null;
        this.setup = false;
        this.source = source;
    }

    @Override
    public InputStream getStream() throws IOException {
        doSetup();
        return this.inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Sets certain properties on a contentStream object. Subclasses must override this method.
     * <p>Its important to call the following setter methods:
     * <li>setName</li>
     * <li>setSize</li>
     * <li>setSourceInfo</li>
     * <li>setContentType</li>
     * <li>setInputStream</li></p>
     */
    abstract protected void setup() throws IOException;

    /**
     * Checks if the content stream is already set up and ready to use.
     * 
     * @return true if set up.
     */
    public boolean isSetup() {
        return this.setup;
    }

    private void doSetup() throws IOException {
        if (!isSetup()) {
            setup();
            this.setup = true;
        }
    }

    @Override
    public Reader getReader() throws IOException {
        doSetup();
        if (this.streamReader == null) {
            this.streamReader = new InputStreamReader(getStream());
        }
        return this.streamReader;
    }

    @Override
    public Long getSize() {
        try {
            doSetup();
        } catch (IOException e) {
            LOGGER.error("Could not setup content stream.", e);
        }
        return super.getSize();
    }

    public T getSource() {
        return source;
    }

}
