/*
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.hibernate.tables;

import java.sql.Blob;

public class MCRXMLTABLE
{
    private String id;
    private int version;
    private String type;
    private Blob xml;

    public MCRXMLTABLE()
    {
    }

    public MCRXMLTABLE(String id, int version, String type, Blob xml) 
    {
	this.id = id;
	this.version = version;
	this.type = type;
	this.xml = xml;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public int getVersion() {
        return version;
    }
    public void setVersion(int version) {
        this.version = version;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public Blob getXml() {
        return xml;
    }
    public byte[] getXmlByteArray() {
	try {
	    java.io.InputStream in = xml.getBinaryStream();
	    byte[] b = new byte[in.available()];
	    int t;
	    for(t=0;t<b.length;t++)
		b[t] = (byte)in.read();
	    return b;
	} catch(java.sql.SQLException e) {
	    e.printStackTrace();
	    return null;
	} catch(java.io.IOException e) {
	    e.printStackTrace();
	    return null;
	}
    }
    public void setXml(Blob xml) {
        this.xml = xml;
    }
    public void setXml(byte[] xml) {
	/* FIXME */
        this.xml = null;
    }
}
