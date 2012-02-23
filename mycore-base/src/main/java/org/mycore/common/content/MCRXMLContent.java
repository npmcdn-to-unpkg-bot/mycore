/*
 * $Revision$ 
 * $Date$
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

package org.mycore.common.content;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jdom.output.Format;
import org.mycore.common.MCRConfiguration;

/**
 * Reads MCRContent from an XML document.
 * Provides functionality to output XML using different formatters. 
 * 
 * @author Frank L\u00FCtzenkichen
 */
public abstract class MCRXMLContent extends MCRContent {

    /** 
     * The default format used when outputting XML as a byte stream.
     * By default, content is outputted using UTF-8 encoding.
     * If MCR.IFS2.PrettyXML=true, a pretty format with indentation is used. 
     */
    protected static Format defaultFormat;

    static {
        boolean prettyXML = MCRConfiguration.instance().getBoolean("MCR.IFS2.PrettyXML", true);
        defaultFormat = prettyXML ? Format.getPrettyFormat().setIndent("  ") : Format.getRawFormat();
        defaultFormat.setEncoding("UTF-8");
    }

    /** The default format used when outputting this XML as a byte stream */
    protected Format format;

    protected MCRXMLContent(Format format) {
        this.format = format;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(asByteArray());
    }

    @Override
    public MCRContent ensureXML() {
        return this;
    }
}
