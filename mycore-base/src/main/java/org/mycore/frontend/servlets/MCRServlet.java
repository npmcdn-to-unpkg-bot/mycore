/*
 * 
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.frontend.servlets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.services.i18n.MCRTranslation;

/**
 * This is the superclass of all MyCoRe servlets. It provides helper methods for
 * logging and managing the current session data. Part of the code has been
 * taken from MilessServlet.java written by Frank Lützenkirchen.
 * 
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date: 2008-02-06 17:27:24 +0000 (Mi, 06 Feb
 *          2008) $
 */
public class MCRServlet extends HttpServlet {
    private static final String CURRENT_THREAD_NAME_KEY = "currentThreadName";

    public static final String MCR_SERVLET_JOB_KEY = "MCRServletJob";

    private static final String INITIAL_SERVLET_NAME_KEY = "currentServletName";

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRServlet.class);

    private static String BASE_URL;

    private static String SERVLET_URL;

    private static final boolean ENABLE_BROWSER_CACHE = MCRConfiguration.instance().getBoolean("MCR.Servlet.BrowserCache.enable", false);

    private static MCRLayoutService LAYOUT_SERVICE;

    public static final String BASE_URL_ATTRIBUTE = "org.mycore.base.url";

    public static MCRLayoutService getLayoutService() {
        return LAYOUT_SERVICE;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        if (LAYOUT_SERVICE == null) {
            LAYOUT_SERVICE = MCRLayoutService.instance();
        }
    }

    /** returns the base URL of the mycore system */
    public static String getBaseURL() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        Object value = session.get(BASE_URL_ATTRIBUTE);
        if (value != null) {
            LOGGER.debug("Returning BaseURL " + value.toString() + " from user session.");
            return value.toString();
        }
        return BASE_URL;
    }

    /** returns the servlet base URL of the mycore system */
    public static String getServletBaseURL() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        Object value = session.get(BASE_URL_ATTRIBUTE);
        if (value != null) {
            LOGGER.debug("Returning BaseURL " + value.toString() + "servlets/ from user session.");
            return value.toString() + "servlets/";
        }
        return SERVLET_URL;
    }

    /**
     * Initialisation of the static values for the base URL and servlet URL of
     * the mycore system.
     */
    private static synchronized void prepareBaseURLs(ServletContext context, HttpServletRequest req) {
        String contextPath = req.getContextPath() + "/";

        String requestURL = req.getRequestURL().toString();
        int pos = requestURL.indexOf(contextPath, 9);
        String baseURLofRequest = requestURL.substring(0, pos) + contextPath;

        prepareBaseURLs(baseURLofRequest);
        MCRURIResolver.init(context, getBaseURL());
    }

    private static synchronized void prepareBaseURLs(String baseURL) {
        BASE_URL = MCRConfiguration.instance().getString("MCR.baseurl", baseURL);
        if (!BASE_URL.endsWith("/")) {
            BASE_URL = BASE_URL + "/";
        }
        SERVLET_URL = BASE_URL + "servlets/";
    }

    static {
        prepareBaseURLs(""); // getBaseURL() etc. may be called before any HTTP Request    
    }

    // The methods doGet() and doPost() simply call the private method
    // doGetPost(),
    // i.e. GET- and POST requests are handled by one method only.
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGetPost(req, res);
    }

    protected void doGet(MCRServletJob job) throws Exception {
        doGetPost(job);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGetPost(req, res);
    }

    protected void doPost(MCRServletJob job) throws Exception {
        doGetPost(job);
    }

    public static MCRSession getSession(HttpServletRequest req) {
        HttpSession theSession = req.getSession(true);
        MCRSession session = null;

        MCRSession fromHttpSession = (MCRSession) theSession.getAttribute("mycore.session");

        if (fromHttpSession != null && fromHttpSession.getID() != null) {
            // Take session from HttpSession with servlets
            session = fromHttpSession;
        } else {
            // Create a new session
            session = MCRSessionMgr.getCurrentSession();
        }

        // Store current session in HttpSession
        theSession.setAttribute("mycore.session", session);
        // store the HttpSession ID in MCRSession
        session.put("http.session", theSession.getId());
        // Forward MCRSessionID to XSL Stylesheets
        req.setAttribute("XSL.MCRSessionID", session.getID());

        return session;
    }

    private static void bindSessionToRequest(HttpServletRequest req, String servletName, MCRSession session) {
        if (!isSessionBoundToRequest(req)) {
            // Bind current session to this thread:
            MCRSessionMgr.setCurrentSession(session);
            req.setAttribute(CURRENT_THREAD_NAME_KEY, Thread.currentThread().getName());
            req.setAttribute(INITIAL_SERVLET_NAME_KEY, servletName);
        }
    }

    private static boolean isSessionBoundToRequest(HttpServletRequest req) {
        String currentThread = getProperty(req, CURRENT_THREAD_NAME_KEY);
        // check if this is request passed the same thread before
        // (RequestDispatcher)
        return currentThread != null && currentThread.equals(Thread.currentThread().getName());
    }

    /**
     * This private method handles both GET and POST requests and is invoked by
     * doGet() and doPost().
     * 
     * @param req
     *            the HTTP request instance
     * @param res
     *            the HTTP response instance
     * @exception IOException
     *                for java I/O errors.
     * @exception ServletException
     *                for errors from the servlet engine.
     */
    private void doGetPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (MCRConfiguration.instance() == null) {
            // removes NullPointerException below, if somehow Servlet is not yet
            // intialized
            init();
        }

        // Try to set encoding of form values
        String ReqCharEncoding = req.getCharacterEncoding();

        if (ReqCharEncoding == null) {
            // Set default to UTF-8
            ReqCharEncoding = MCRConfiguration.instance().getString("MCR.Request.CharEncoding", "UTF-8");
            req.setCharacterEncoding(ReqCharEncoding);
            LOGGER.debug("Setting ReqCharEncoding to: " + ReqCharEncoding);
        }

        if ("true".equals(req.getParameter("reload.properties"))) {
            MCRConfiguration.instance().reload(true);
        }

        if (BASE_URL == null) {
            prepareBaseURLs(getServletContext(), req);
        }

        MCRServletJob job = new MCRServletJob(req, res);

        MCRSession session = getSession(job.getRequest());
        bindSessionToRequest(req, getServletName(), session);
        try {
            // transaction around 1st phase of request
            Exception thinkException = processThinkPhase(job);
            // first phase completed, start rendering phase
            processRenderingPhase(job, thinkException);
        } catch (Exception ex) {
            if (getProperty(req, INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                session.rollbackTransaction();
            }
            if (ex instanceof ServletException) {
                throw (ServletException) ex;
            } else if (ex instanceof IOException) {
                throw (IOException) ex;
            } else if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        } finally {
            MCRSessionMgr.getCurrentSession().deleteObject(MCR_SERVLET_JOB_KEY);
            // Release current MCRSession from current Thread,
            // in case that Thread pooling will be used by servlet engine
            if (getProperty(req, INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                MCRSessionMgr.releaseCurrentSession();
            }
        }
    }

    private void configureSession(MCRServletJob job) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        session.put(MCR_SERVLET_JOB_KEY, job);

        String c = getClass().getName();
        c = c.substring(c.lastIndexOf(".") + 1);

        StringBuilder msg = new StringBuilder();
        msg.append(c);
        msg.append(" ip=");
        msg.append(getRemoteAddr(job.getRequest()));
        msg.append(" mcr=").append(session.getID());
        msg.append(" user=").append(session.getUserInformation().getCurrentUserID());
        LOGGER.info(msg.toString());

        String lang = getProperty(job.getRequest(), "lang");

        if (lang != null && lang.trim().length() != 0) {
            session.setCurrentLanguage(lang.trim());
        }

        // Set the IP of the current session
        if (session.getCurrentIP().length() == 0) {
            session.setCurrentIP(getRemoteAddr(job.getRequest()));
        }

        // set BASE_URL_ATTRIBUTE to MCRSession
        if (job.getRequest().getAttribute(BASE_URL_ATTRIBUTE) != null) {
            session.put(BASE_URL_ATTRIBUTE, job.getRequest().getAttribute(BASE_URL_ATTRIBUTE));
        }

        // Store XSL.*.SESSION parameters to MCRSession
        putParamsToSession(job.getRequest());
    }

    private Exception processThinkPhase(MCRServletJob job) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        try {
            if (getProperty(job.getRequest(), INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                session.beginTransaction();
            }
            configureSession(job);
            think(job);
            if (getProperty(job.getRequest(), INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                session.commitTransaction();
            }
        } catch (Exception ex) {
            if (getProperty(job.getRequest(), INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
                // current Servlet not called via RequestDispatcher
                LOGGER.warn("Exception occurred, performing database rollback.");
                session.rollbackTransaction();
            } else {
                LOGGER.warn("Exception occurred, cannot rollback database transaction right now.");
            }
            return ex;
        }
        return null;
    }

    /**
     * 1st phase of doGetPost. This method has a seperate transaction. Per
     * default id does nothing as a fallback to the old behaviour.
     * 
     * @param job
     * @throws Exception
     * @see #render(MCRServletJob, Exception)
     */
    protected void think(MCRServletJob job) throws Exception {
        // not implemented by default
    }

    private void processRenderingPhase(MCRServletJob job, Exception thinkException) throws Exception {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        if (getProperty(job.getRequest(), INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
            // current Servlet not called via RequestDispatcher
            session.beginTransaction();
        }
        render(job, thinkException);
        if (getProperty(job.getRequest(), INITIAL_SERVLET_NAME_KEY).equals(getServletName())) {
            // current Servlet not called via RequestDispatcher
            session.commitTransaction();
        }
    }

    /**
     * 2nd phase of doGetPost This method has a seperate transaction and gets
     * the same MCRServletJob from the first phase (think) and any exception
     * that occurs at the first phase. By default this method calls
     * doGetPost(MCRServletJob) as a fallback to the old behaviour.
     * 
     * @param job
     *            same instance as of think(MCRServlet job)
     * @param ex
     *            any exception thrown by think(MCRServletJob) or transaction
     *            commit
     * @throws Exception
     *             if render could not handle <code>ex</code> to produce a nice
     *             user page
     */
    protected void render(MCRServletJob job, Exception ex) throws Exception {
        // no info here how to handle
        if (ex != null)
            throw ex;
        if (job.getRequest().getMethod().equals("POST")) {
            doPost(job);
        } else {
            doGet(job);
        }
    }

    /**
     * This method should be overwritten by other servlets. As a default
     * response we indicate the HTTP 1.1 status code 501 (Not Implemented).
     */
    protected void doGetPost(MCRServletJob job) throws Exception {
        job.getResponse().sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }

    /** Handles an exception by reporting it and its embedded exception */
    protected void handleException(Exception ex) {
        try {
            reportException(ex);
        } catch (Exception ignored) {
        }
    }

    /** Reports an exception to the log */
    protected void reportException(Exception ex) throws Exception {
        String cname = this.getClass().getName();
        String servlet = cname.substring(cname.lastIndexOf(".") + 1);

        LOGGER.warn("Exception caught in : " + servlet, ex);
    }

    /**
     * @deprecated use {@link HttpServletResponse#sendError(int, String)}
     *             instead or throw Exception
     */
    @Deprecated()
    protected void generateErrorPage(HttpServletRequest request, HttpServletResponse response, int error, String msg, Exception ex,
            boolean xmlstyle) throws IOException {
        LOGGER.error(getClass().getName() + ": Error " + error + " occured. The following message was given: " + msg, ex);

        String rootname = "mcr_error";
        String style = getProperty(request, "XSL.Style");
        if (style == null || !style.equals("xml")) {
            style = "default";
        }
        Element root = new Element(rootname);
        root.setAttribute("HttpError", Integer.toString(error)).setText(msg);

        Document errorDoc = new Document(root, new DocType(rootname));

        while (ex != null) {
            Element exception = new Element("exception");
            exception.setAttribute("type", ex.getClass().getName());
            Element trace = new Element("trace");
            Element message = new Element("message");
            trace.setText(MCRException.getStackTraceAsString(ex));
            message.setText(ex.getMessage());
            exception.addContent(message).addContent(trace);
            root.addContent(exception);

            if (ex instanceof MCRException) {
                ex = ((MCRException) ex).getException();
            } else {
                ex = null;
            }
        }

        request.setAttribute("XSL.Style", style);

        final String requestAttr = "MCRServlet.generateErrorPage";
        if (!response.isCommitted() && request.getAttribute(requestAttr) == null) {
            response.setStatus(error);
            request.setAttribute(requestAttr, msg);
            LAYOUT_SERVICE.doLayout(request, response, errorDoc);
            return;
        } else {
            if (request.getAttribute(requestAttr) != null) {
                LOGGER.warn("Could not send error page. Generating error page failed. The original message:\n"
                        + request.getAttribute(requestAttr));
            } else {
                LOGGER.warn("Could not send error page. Response allready commited. The following message was given:\n" + msg);
            }
        }
    }

    /**
     * This method builds a URL that can be used to redirect the client browser
     * to another page, thereby including http request parameters. The request
     * parameters will be encoded as http get request.
     * 
     * @param baseURL
     *            the base url of the target webpage
     * @param parameters
     *            the http request parameters
     */
    protected String buildRedirectURL(String baseURL, Properties parameters) {
        StringBuilder redirectURL = new StringBuilder(baseURL);
        boolean first = true;
        for (Enumeration<?> e = parameters.keys(); e.hasMoreElements();) {
            if (first) {
                redirectURL.append("?");
                first = false;
            } else {
                redirectURL.append("&");
            }

            String name = (String) e.nextElement();
            String value = null;
            try {
                value = URLEncoder.encode(parameters.getProperty(name), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                value = parameters.getProperty(name);
            } catch (NullPointerException npe) {
                throw new MCRException("NullPointerException while encoding " + name, npe);
            }
            redirectURL.append(name).append("=").append(value);
        }
        LOGGER.debug("Sending redirect to " + redirectURL.toString());
        return redirectURL.toString();
    }

    protected void generateActiveLinkErrorpage(HttpServletRequest request, HttpServletResponse response, String msg,
            MCRActiveLinkException activeLinks) throws IOException {
        StringBuilder msgBuf = new StringBuilder(msg);
        msgBuf.append("\nThere are links active preventing the commit of work, see error message for details. The following links where affected:");
        Map<String, Collection<String>> links = activeLinks.getActiveLinks();
        Iterator<Map.Entry<String, Collection<String>>> entryIt = links.entrySet().iterator();
        while (entryIt.hasNext()) {
            Map.Entry<String, Collection<String>> entry = entryIt.next();
            for (String source : entry.getValue()) {
                msgBuf.append('\n').append(source).append("==>").append(entry.getKey());
            }
        }
        generateErrorPage(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msgBuf.toString(), activeLinks, false);
    }

    /**
     * allows browser to cache requests. This method is usefull as it allows
     * browsers to cache content that is not changed. Please overwrite this
     * method in every Servlet that depends on "remote" data.
     */
    @Override
    protected long getLastModified(HttpServletRequest request) {
        if (ENABLE_BROWSER_CACHE) {
            // we can cache every (local) request
            long lastModified = MCRSessionMgr.getCurrentSession().getLoginTime() > MCRConfiguration.instance().getSystemLastModified() ? MCRSessionMgr
                    .getCurrentSession().getLoginTime() : MCRConfiguration.instance().getSystemLastModified();
            LOGGER.info("LastModified: " + lastModified);
            return lastModified;
        }
        return -1; // time is not known
    }

    public static String getProperty(HttpServletRequest request, String name) {
        String value = (String) request.getAttribute(name);

        // if Attribute not given try Parameter
        if (value == null || value.length() == 0) {
            value = request.getParameter(name);
        }

        return value;
    }

    /** The IP addresses of trusted web proxies */
    protected static Set<String> trustedProxies = new HashSet<String>();

    /**
     * Builds a list of trusted proxy IPs from MCR.Request.TrustedProxies. The
     * IP address of the local host is automatically added to this list.
     */
    protected static synchronized void initTrustedProxies() {
        String sTrustedProxies = MCRConfiguration.instance().getString("MCR.Request.TrustedProxies", "");
        StringTokenizer st = new StringTokenizer(sTrustedProxies, " ,;");
        while (st.hasMoreTokens()) {
            trustedProxies.add(st.nextToken());
        }

        // Always trust the local host
        trustedProxies.add("127.0.0.1");

        try {
            String host = new java.net.URL(getBaseURL()).getHost();
            trustedProxies.add(InetAddress.getByName(host).getHostAddress());
        } catch (Exception ex) {
            LOGGER.warn("Could not determine IP of local host", ex);
        }

        for (String proxy : trustedProxies) {
            LOGGER.debug("Trusted proxy: " + proxy);
        }
    }

    /**
     * Returns the IP address of the client that made the request. When a
     * trusted proxy server was used, e. g. a local Apache mod_proxy in front of
     * Tomcat, the value of the last entry in the HTTP header X_FORWARDED_FOR is
     * returned, otherwise the REMOTE_ADDR is returned. The list of trusted
     * proxy IPs can be configured using the property
     * MCR.Request.TrustedProxies, which is a List of IP addresses separated by
     * blanks and/or comma.
     */
    public static String getRemoteAddr(HttpServletRequest req) {
        if (trustedProxies.isEmpty()) {
            initTrustedProxies();
        }

        // Check if request comes in via a proxy
        // There are two possible header names
        String xForwardedFor = req.getHeader("X_FORWARDED_FOR");
        if (xForwardedFor == null || xForwardedFor.trim().length() == 0) {
            xForwardedFor = req.getHeader("x-forwarded-for");
        }

        // If no proxy is used, use client IP from HTTP request
        if (xForwardedFor == null || xForwardedFor.trim().length() == 0) {
            return req.getRemoteAddr();
        }

        // X_FORWARDED_FOR can be comma separated list of hosts,
        // if so, take last entry, all others are not reliable because
        // any client may have set the header to any value.
        StringTokenizer st = new StringTokenizer(xForwardedFor, " ,;");
        while (st.hasMoreTokens()) {
            xForwardedFor = st.nextToken();
        }

        // If request comes from a trusted proxy,
        // the best IP is the last entry in xForwardedFor
        if (trustedProxies.contains(req.getRemoteAddr())) {
            return xForwardedFor;
        }

        // Otherwise, use client IP from HTTP request
        return req.getRemoteAddr();
    }

    @SuppressWarnings("unchecked")
    private static void putParamsToSession(HttpServletRequest request) {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();

        for (Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            if (name.startsWith("XSL.") && name.endsWith(".SESSION")) {
                String key = name.substring(0, name.length() - 8);
                // parameter is not empty -> store
                if (!request.getParameter(name).trim().equals("")) {
                    mcrSession.put(key, request.getParameter(name));
                    LOGGER.debug("Found HTTP-Req.-Parameter " + name + "=" + request.getParameter(name)
                            + " that should be saved in session, safed " + key + "=" + request.getParameter(name));
                }
                // paramter is empty -> do not store and if contained in
                // session, remove from it
                else {
                    if (mcrSession.get(key) != null) {
                        mcrSession.deleteObject(key);
                    }
                }
            }
        }
        for (Enumeration<String> e = request.getAttributeNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            if (name.startsWith("XSL.") && name.endsWith(".SESSION")) {
                String key = name.substring(0, name.length() - 8);
                // attribute is not empty -> store
                if (!request.getAttribute(name).toString().trim().equals("")) {
                    mcrSession.put(key, request.getAttribute(name));
                    LOGGER.debug("Found HTTP-Req.-Attribute " + name + "=" + request.getParameter(name)
                            + " that should be saved in session, safed " + key + "=" + request.getParameter(name));
                }
                // attribute is empty -> do not store and if contained in
                // session, remove from it
                else {
                    if (mcrSession.get(key) != null) {
                        mcrSession.deleteObject(key);
                    }
                }
            }
        }
    }

    /**
     * returns a translated error message for the current Servlet. I18N keys are
     * of form 'error.'{SimpleServletClassName}'.'{subIdentifier}
     * 
     * @param subIdentifier
     *            last part of I18n key
     * @param args
     *            any arguments that should be passed to
     *            {@link MCRTranslation#translate(String, Object...)}
     */
    protected String getErrorI18N(String subIdentifier, Object... args) {
        String key = MessageFormat.format("error.{0}.{1}", getClass().getSimpleName(), subIdentifier);
        return MCRTranslation.translate(key, args);
    }

    protected void writeCacheHeaders(HttpServletResponse response, int CACHE_TIME, long lastModified, boolean useExpire) {
        response.setHeader("Cache-Control", "max-age=" + CACHE_TIME);
        response.setDateHeader("Last-Modified", lastModified);
        if (useExpire) {
            Date expires = new Date(System.currentTimeMillis() + CACHE_TIME * 1000);
            LOGGER.info("Last-Modified: " + new Date(lastModified) + ", expire on: " + expires);
            response.setDateHeader("Expires", expires.getTime());
        }
    }

    /**
     * This method encodes the url. After the encoding the url is redirectable.
     * 
     * @param url
     *            the source URL
     */
    public static String encodeURL(String url) throws URISyntaxException {
        url = url.replace("%", "%25");
        url = url.replace(" ", "%20");

        URI uri = new URI(url);

        String scheme = uri.getScheme();
        String userInfo = uri.getUserInfo();
        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();
        String query = uri.getQuery();
        String fragment = uri.getFragment();

        uri = new URI(scheme, userInfo, host, port, path, query, fragment);
        return uri.toASCIIString();
    }
}
