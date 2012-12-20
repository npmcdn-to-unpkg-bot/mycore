/*
 * $Revision$ $Date$
 * $LastChangedBy$ Copyright 2010 - Thüringer Universitäts- und
 * Landesbibliothek Jena
 * 
 * Mets-Editor is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Mets-Editor is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Mets-Editor. If not, see http://www.gnu.org/licenses/.
 */
package org.mycore.mets.tools;

//import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.common.MCRConfiguration;
import org.mycore.mets.model.Mets;
import org.mycore.mets.tools.model.MCRMETSTreeBuilder;

/**
 * @author Silvio Hermann (shermann)
 *          Sebastian Hofmann
 */
public class MCRJSONProvider {
    public static final String DEFAULT_METS_FILENAME = MCRConfiguration.instance().getString("MCR.Mets.Filename", "mets.xml");

    //final private static Logger LOGGER = Logger.getLogger(MCRJSONProvider.class);

    private Mets mets;

    /**
     * @param mets
     *            the Mets document, must be non null
     * @param derivate
     *            the derivate id
     * @throws Exception 
     */
    public MCRJSONProvider(Document mets, String derivate) throws Exception  {
        Document metsDocument = mets;
        this.mets = new Mets(metsDocument);
    }

    public String getJson() {
        MCRMETSTreeBuilder builder = new MCRMETSTreeBuilder(this.mets);
        return builder.buildTree().toJSon();
    }
}