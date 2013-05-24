package org.mycore.solr.proxy;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;
import static org.mycore.solr.MCRSolrConstants.QUERY_PATH;
import static org.mycore.solr.MCRSolrConstants.QUERY_XML_PROTOCOL_VERSION;
import static org.mycore.solr.MCRSolrConstants.SERVER_URL;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRSolrProxyServlet extends MCRServlet {

    static final Logger LOGGER = Logger.getLogger(MCRSolrProxyServlet.class);

    private static final long serialVersionUID = 1L;

    /**
     * Attribute key to store Query parameters as <code>Map&lt;String, String[]&gt;</code> for SOLR.
     * 
     * This takes precedence over any {@link HttpServletRequest} parameter.
     */
    public static final String MAP_KEY = MCRSolrProxyServlet.class.getName() + ".map";

    /**
     * Attribute key to store a {@link SolrQuery}.
     * 
     * This takes precedence over {@link #MAP_KEY} or any {@link HttpServletRequest} parameter.
     */
    public static final String QUERY_KEY = MCRSolrProxyServlet.class.getName() + ".query";

    private static int MAX_CONNECTIONS = MCRConfiguration.instance().getInt(
        CONFIG_PREFIX + "SelectProxy.MaxConnections");

    private HttpClient httpClient;

    private MCRIdleConnectionMonitorThread idleConnectionMonitorThread;

    protected HttpHost solrHost;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        ModifiableSolrParams solrParameter = getSolrQueryParameter(request);

        HttpGet solrHttpMethod = MCRSolrProxyServlet.getSolrHttpMethod(solrParameter);
        try {
            LOGGER.info("Sending Request: " + solrHttpMethod.getURI());
            HttpResponse response = httpClient.execute(solrHost, solrHttpMethod);
            int statusCode = response.getStatusLine().getStatusCode();

            HttpServletResponse resp = job.getResponse();

            // set status code
            resp.setStatus(statusCode);

            boolean isXML = response.getFirstHeader(HTTP.CONTENT_TYPE).getValue().contains("/xml");
            boolean justCopyInput = (statusCode != HttpStatus.SC_OK) || !isXML;

            // set all headers
            for (Header header : response.getAllHeaders()) {
                if (justCopyInput || !HTTP.TRANSFER_ENCODING.equals(header.getName())) {
                    resp.setHeader(header.getName(), header.getValue());
                }
            }

            HttpEntity solrResponseEntity = response.getEntity();
            if (solrResponseEntity != null) {
                try (InputStream solrResponseStream = solrResponseEntity.getContent()) {
                    if (justCopyInput) {
                        // copy solr response to servlet outputstream
                        OutputStream servletOutput = resp.getOutputStream();
                        IOUtils.copy(solrResponseStream, servletOutput);
                    } else {
                        String docType = solrParameter.get("xslt", "response");
                        MCRStreamContent solrResponse = new MCRStreamContent(solrResponseStream, solrHttpMethod
                            .getURI().toString(), docType);
                        MCRLayoutService.instance().doLayout(request, resp, solrResponse);
                    }
                }
            }
        } catch (IOException ex) {
            solrHttpMethod.abort();
            throw ex;
        }
        solrHttpMethod.releaseConnection();
    }

    /**
     * Gets a HttpGet to make a request to the Solr-Server.
     * 
     * @param parameterMap
     *            Parameters to use with the Request
     * @return a method to make the request
     */
    private static HttpGet getSolrHttpMethod(ModifiableSolrParams params) {
        HttpGet httpGet = new HttpGet(MessageFormat.format("{0}{1}?{2}", SERVER_URL, QUERY_PATH, params.toString()));
        return httpGet;
    }

    @SuppressWarnings("unchecked")
    private static ModifiableSolrParams getSolrQueryParameter(HttpServletRequest request) {
        SolrQuery query = (SolrQuery) request.getAttribute(QUERY_KEY);
        if (query != null) {
            return query;
        }
        Map<String, String[]> solrParameter;
        solrParameter = (Map<String, String[]>) request.getAttribute(MAP_KEY);
        if (solrParameter == null) {
            //good old way
            solrParameter = request.getParameterMap();
        }
        return getQueryString(solrParameter);
    }

    @Override
    public void init() throws ServletException {
        super.init();

        LOGGER.info("Initializing SOLR connection to \"" + SERVER_URL + "\"");

        solrHost = MCRSolrProxyUtils.getHttpHost(SERVER_URL);
        if (solrHost == null) {
            throw new ServletException("URI does not specify a valid host name: " + SERVER_URL);
        }
        httpClient = MCRSolrProxyUtils.getHttpClient(MAX_CONNECTIONS);

        //start thread to monitor stalled connections
        idleConnectionMonitorThread = new MCRIdleConnectionMonitorThread(httpClient.getConnectionManager());
        idleConnectionMonitorThread.start();
    }

    @Override
    public void destroy() {
        idleConnectionMonitorThread.shutdown();
        ClientConnectionManager clientConnectionManager = httpClient.getConnectionManager();
        clientConnectionManager.shutdown();
        super.destroy();
    }

    private static ModifiableSolrParams getQueryString(Map<String, String[]> parameters) {
        //to maintain order
        LinkedHashMap<String, String[]> copy = new LinkedHashMap<String, String[]>(parameters);
        ModifiableSolrParams solrParams = new ModifiableSolrParams(copy);
        if (!parameters.containsKey("version") && !parameters.containsKey("wt")) {
            solrParams.set("version", QUERY_XML_PROTOCOL_VERSION);
        }
        return solrParams;
    }
}
