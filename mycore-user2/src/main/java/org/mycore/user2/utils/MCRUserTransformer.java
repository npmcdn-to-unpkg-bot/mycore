/*
 * 
 * $Revision: 13085 $ $Date: 02.02.2012 22:25:14 $
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
package org.mycore.user2.utils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRException;
import org.mycore.user2.MCRUser;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public abstract class MCRUserTransformer {

    private static final String USER_ELEMENT_NAME = "user";

    public static final JAXBContext JAXB_CONTEXT = initContext();

    private MCRUserTransformer() {
    }

    private static JAXBContext initContext() {
        try {
            return JAXBContext.newInstance(MCRUser.class.getPackage().getName());
        } catch (JAXBException e) {
            throw new MCRException("Could not instantiate JAXBContext.", e);
        }
    }

    private static Document getDocument(MCRUser user) {
        try {
            Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
            JDOMResult result = new JDOMResult();
            marshaller.marshal(user, result);
            return result.getDocument();
        } catch (JAXBException e) {
            throw new MCRException("Exception while transforming MCRUser " + user.getUserID() + " to JDOM document.", e);
        }
    }

    /**
     * Builds an xml element containing basic information on user. 
     * This includes user ID, login name and realm.
     */
    public static Document buildBasicXML(MCRUser mcrUser) {
        return getDocument(mcrUser.getBasicCopy());
    }

    /**
     * Builds an xml element containing all information on the given user except password info.
     * same as {@link #buildXML(MCRUser)} without owned users resolved
     */
    public static Document buildExportableSafeXML(MCRUser mcrUser) {
        return getDocument(mcrUser.getSafeCopy());
    }

    /**
     * Builds an xml element containing all information on the given user.
     * same as {@link #buildExportableSafeXML(MCRUser)} but with password info if available
     */
    public static Document buildExportableXML(MCRUser mcrUser) {
        return getDocument(mcrUser);
    }

    /**
     * Builds an MCRUser instance from the given element.
     * @param element as generated by {@link #buildExportableXML(MCRUser)}. 
     */
    public static MCRUser buildMCRUser(Element element) {
        if (!element.getName().equals(USER_ELEMENT_NAME)) {
            throw new IllegalArgumentException("Element is not a mycore user element.");
        }
        try {
            Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
            return (MCRUser) unmarshaller.unmarshal(new JDOMSource(element));
        } catch (JAXBException e) {
            throw new MCRException("Exception while transforming Element to MCRUser.", e);
        }
    }

}
