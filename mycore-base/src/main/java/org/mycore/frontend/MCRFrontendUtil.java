package org.mycore.frontend;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;

/**
 * Servlet/Jersey Resource utility class.
 */
public class MCRFrontendUtil {

    private static final String PROXY_HEADER = "X-Forwarded-Host";

    public static final String BASE_URL_ATTRIBUTE = "org.mycore.base.url";

    public static String BASE_URL;

    public static String BASE_HOST_IP;

    private static Logger LOGGER = Logger.getLogger(MCRFrontendUtil.class);

    /** The IP addresses of trusted web proxies */
    protected static final Set<String> TRUSTED_PROXIES = getTrustedProxies();

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

    /**
     * returns the base URL of the mycore system.
     * 
     * This method uses the request to 'calculate' the right baseURL.
     * Generally it is sufficent to use {@link #getBaseURL()} instead.
     */
    public static String getBaseURL(ServletRequest req) {
        HttpServletRequest request = (HttpServletRequest) req;
        StringBuilder webappBase = new StringBuilder(request.getScheme());
        webappBase.append("://");
        String proxyHeader = request.getHeader(PROXY_HEADER);
        if (proxyHeader != null) {
            StringTokenizer sttoken = new StringTokenizer(proxyHeader, ",");
            String proxyHost = sttoken.nextToken().trim();
            webappBase.append(proxyHost);
        } else {
            webappBase.append(request.getServerName());
            int port = request.getServerPort();
            if (!(port == 80 || request.isSecure() && port == 443)) {
                webappBase.append(':').append(port);
            }
        }
        webappBase.append(request.getContextPath()).append('/');
        return webappBase.toString();
    }

    public static synchronized void prepareBaseURLs(String baseURL) {
        BASE_URL = MCRConfiguration.instance().getString("MCR.baseurl", baseURL);
        if (!BASE_URL.endsWith("/")) {
            BASE_URL = BASE_URL + "/";
        }
        try {
            URL url = new URL(BASE_URL);
            InetAddress BASE_HOST = InetAddress.getByName(url.getHost());
            BASE_HOST_IP = BASE_HOST.getHostAddress();
        } catch (MalformedURLException e) {
            LOGGER.error("Can't create URL from String " + BASE_URL);
        } catch (UnknownHostException e) {
            LOGGER.error("Can't find host IP for URL " + BASE_URL);
        }
    }

    public static void configureSession(MCRSession session, HttpServletRequest request) {
        // language
        String lang = getProperty(request, "lang");
        if (lang != null && lang.trim().length() != 0) {
            session.setCurrentLanguage(lang.trim());
        }

        // Set the IP of the current session
        if (session.getCurrentIP().length() == 0) {
            session.setCurrentIP(getRemoteAddr(request));
        }

        // set BASE_URL_ATTRIBUTE to MCRSession
        if (request.getAttribute(BASE_URL_ATTRIBUTE) != null) {
            session.put(BASE_URL_ATTRIBUTE, request.getAttribute(BASE_URL_ATTRIBUTE));
        }

        // Store XSL.*.SESSION parameters to MCRSession
        putParamsToSession(request);
    }

    public static String getProperty(HttpServletRequest request, String name) {
        String value = (String) request.getAttribute(name);

        // if Attribute not given try Parameter
        if (value == null || value.length() == 0) {
            value = request.getParameter(name);
        }

        return value;
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
        String remoteAddress = req.getRemoteAddr();
        if (TRUSTED_PROXIES.contains(remoteAddress)) {
            String xff = getXForwardedFor(req);
            if (xff != null)
                remoteAddress = xff;
        }
        return remoteAddress;
    }

    /**
     * Get header to check if request comes in via a proxy.
     * There are two possible header names
     */
    private static String getXForwardedFor(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if ((xff == null) || xff.trim().isEmpty()) {
            xff = req.getHeader("X_Forwarded_For");
        }
        if ((xff == null) || xff.trim().isEmpty())
            return null;

        // X_FORWARDED_FOR can be comma separated list of hosts,
        // if so, take last entry, all others are not reliable because
        // any client may have set the header to any value.

        LOGGER.debug("X-Forwarded-For complete: " + xff);
        StringTokenizer st = new StringTokenizer(xff, " ,;");
        while (st.hasMoreTokens()) {
            xff = st.nextToken().trim();
        }
        LOGGER.debug("X-Forwarded-For last: " + xff);
        return xff;
    }

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
     * Builds a list of trusted proxy IPs from MCR.Request.TrustedProxies. The
     * IP address of the local host is automatically added to this list.
     * @return 
     */
    private static TreeSet<String> getTrustedProxies() {
        boolean closeSession = !MCRSessionMgr.hasCurrentSession();
        HashSet<InetAddress> trustedProxies = new HashSet<>();

        String sTrustedProxies = MCRConfiguration.instance().getString("MCR.Request.TrustedProxies", "");
        StringTokenizer st = new StringTokenizer(sTrustedProxies, " ,;");
        while (st.hasMoreTokens()) {
            String host = st.nextToken().trim();
            try {
                Collections.addAll(trustedProxies, InetAddress.getAllByName(host));
            } catch (UnknownHostException e) {
                LOGGER.warn("Unknown host: " + host);
            }
        }

        // Always trust the local host
        try {
            InetAddress[] localAddresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
            Collections.addAll(trustedProxies, localAddresses);
        } catch (UnknownHostException e) {
            LOGGER.warn("Could not get local host name.", e);
        }
        trustedProxies.add(InetAddress.getLoopbackAddress());
        try {
            Collections.addAll(trustedProxies, InetAddress.getAllByName("localhost"));
        } catch (UnknownHostException e) {
            LOGGER.warn("Could not get IP adresses of 'localhost'.", e);
        }

        //junit test cannot configure baseurl properly
        if (getBaseURL() != null) {
            try {
                String host = new java.net.URL(getBaseURL()).getHost();
                Collections.addAll(trustedProxies, InetAddress.getAllByName(host));
            } catch (Exception ex) {
                LOGGER.warn("Could not determine IP of local host serving:" + getBaseURL(), ex);
            }
        }

        TreeSet<String> sortedAdresses = new TreeSet<>();
        if (LOGGER.isDebugEnabled()) {
            for (InetAddress address : trustedProxies) {
                sortedAdresses.add(address.toString());
            }
            LOGGER.debug("Trusted proxies: " + sortedAdresses);
            sortedAdresses.clear();
        }
        for (InetAddress address : trustedProxies) {
            sortedAdresses.add(address.getHostAddress());
        }
        if (closeSession) {
            //getBaseURL() creates MCRSession
            MCRSessionMgr.getCurrentSession().close();
        }
        return sortedAdresses;
    }

}
