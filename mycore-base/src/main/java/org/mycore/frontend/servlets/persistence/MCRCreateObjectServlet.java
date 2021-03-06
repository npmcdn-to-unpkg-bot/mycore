/**
 * 
 */
package org.mycore.frontend.servlets.persistence;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.xml.sax.SAXParseException;

/**
 * Handles CREATE operation on {@link MCRObject}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRCreateObjectServlet extends MCRPersistenceServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 4143057048219690238L;

    Logger LOGGER = LogManager.getLogger();

    private boolean appendDerivate = false;

    @Override
    public void init() throws ServletException {
        super.init();
        String appendDerivate = getInitParameter("appendDerivate");
        if (appendDerivate != null) {
            this.appendDerivate = true;
        }
    }

    @Override
    void handlePersistenceOperation(HttpServletRequest request, HttpServletResponse response)
            throws MCRAccessException, ServletException, MCRActiveLinkException, SAXParseException, JDOMException, IOException {
        Document editorSubmission = MCRPersistenceHelper.getEditorSubmission(request, false);
        MCRObjectID objectID;
        if (editorSubmission != null) {
            objectID = createObject(editorSubmission);
        } else {
            //editorSubmission is null, when editor input is absent (redirect to editor form in render phase)
            String projectID = getProperty(request, "project");
            String type = getProperty(request, "type");
            String formattedId = MCRObjectID.formatID(projectID + "_" + type, 0);
            objectID = MCRObjectID.getInstance(formattedId);

        }
        checkCreatePrivilege(objectID);
        request.setAttribute(OBJECT_ID_KEY, objectID);
    }

    @Override
    void displayResult(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //return to object itself if created, else call editor form
        MCRObjectID returnID = (MCRObjectID) request.getAttribute(OBJECT_ID_KEY);
        if (returnID.getNumberAsInteger() == 0) {
            redirectToCreateObject(request, response);
        } else {
            Properties params = MCRPersistenceHelper.getXSLProperties(request);
            if (this.appendDerivate) {
                params.put("id", returnID.toString());
                params.put("cancelURL", MCRPersistenceHelper.getCancelUrl(request));
                response.sendRedirect(
                        response.encodeRedirectURL(buildRedirectURL(MCRFrontendUtil.getBaseURL() + "servlets/derivate/create", params)));
            } else {
                response.sendRedirect(response
                        .encodeRedirectURL(buildRedirectURL(MCRFrontendUtil.getBaseURL() + "receive/" + returnID.toString(), params)));
            }
        }
    }

    /**
     * Adds a new mycore object to the persistence backend.
     * @param doc
     *  MyCoRe object as XML
     * @return
     *  MCRObjectID of the newly created object.
     * @throws MCRActiveLinkException
     *  If links from or to other objects will fail.
     * @throws JDOMException
     *  from {@link MCRPersistenceHelper#getMCRObject(Document)}
     * @throws IOException
     *  from {@link MCRPersistenceHelper#getMCRObject(Document)}
     * @throws SAXParseException
     * @throws MCRException
     * @throws MCRAccessException 
     */
    private MCRObjectID createObject(Document doc)
            throws MCRActiveLinkException, JDOMException, IOException, MCRException, SAXParseException, MCRAccessException {
        MCRObject mcrObject = MCRPersistenceHelper.getMCRObject(doc);
        MCRObjectID objectId = mcrObject.getId();
        //noinspection SynchronizeOnNonFinalField
        checkCreatePrivilege(objectId);
        synchronized (this) {
            if (objectId.getNumberAsInteger() == 0) {
                String objId = mcrObject.getId().toString();
                objectId = MCRObjectID.getNextFreeId(objectId.getBase());
                if (mcrObject.getLabel().equals(objId))
                    mcrObject.setLabel(objectId.toString());
                mcrObject.setId(objectId);
            }
        }
        MCRMetadataManager.create(mcrObject);
        return objectId;
    }

    private void checkCreatePrivilege(MCRObjectID objectId) throws MCRAccessException {
        String createBasePrivilege = "create-" + objectId.getBase();
        String createTypePrivilege = "create-" + objectId.getTypeId();
        if (!MCRAccessManager.checkPermission(createBasePrivilege) && !MCRAccessManager.checkPermission(createTypePrivilege)) {
            throw MCRAccessException.missingPrivilege("Create object with id " + objectId, createBasePrivilege, createTypePrivilege);
        }
    }

    /**
     * redirects to new mcrobject form.
     *
     * At least "type" HTTP parameter is required to succeed.
     * <dl>
     *   <dt>type</dt>
     *   <dd>object type of the new object (required)</dd>
     *   <dt>project</dt>
     *   <dd>project ID part of object ID (required)</dd>
     *   <dt>layout</dt>
     *   <dd>special editor form layout</dd>
     * </dl>
     * @param job
     * @throws IOException
     * @throws ServletException 
     */
    private void redirectToCreateObject(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        MCRObjectID objectID = (MCRObjectID) request.getAttribute(OBJECT_ID_KEY);
        StringBuilder sb = new StringBuilder();
        sb.append("editor_form_author").append('-').append(objectID.getTypeId());
        String layout = getProperty(request, "layout");
        if (layout != null && layout.length() != 0) {
            sb.append('-').append(layout);
        }
        String base_name = sb.toString();
        String form = MCRPersistenceHelper.getWebPage(getServletContext(), base_name + ".xed", base_name + ".xml");
        Properties params = new Properties();
        params.put("cancelUrl", MCRPersistenceHelper.getCancelUrl(request));
        params.put("mcrid", objectID.toString());
        Enumeration<String> e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            String value = request.getParameter(name);
            params.put(name, value);
        }
        response.sendRedirect(response.encodeRedirectURL(buildRedirectURL(MCRFrontendUtil.getBaseURL() + form, params)));
    }

}
