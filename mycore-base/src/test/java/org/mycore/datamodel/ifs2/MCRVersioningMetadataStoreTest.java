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

package org.mycore.datamodel.ifs2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.VFS;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRUsageException;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

/**
 * JUnit test for MCRVersioningMetadataStore
 * 
 * @author Frank Lützenkirchen
 */
public class MCRVersioningMetadataStoreTest extends MCRIFS2VersioningTestCase {

    private static Logger LOGGER = Logger.getLogger(MCRVersioningMetadataStoreTest.class);

    @Test
    public void createDocument() throws Exception {
        Document testXmlDoc = new Document(new Element("root"));
        MCRContent testContent = MCRContent.readFrom(testXmlDoc);
        
        MCRVersionedMetadata versionedMetadata = getVersStore().create(testContent);
        MCRContent contentFromStore = getVersStore().retrieve(versionedMetadata.getID()).getMetadata();
        String contentStrFromStore = contentFromStore.asString();
        
        MCRContent mcrContent = MCRContent.readFrom(testXmlDoc);
        String expectedContentStr = mcrContent.asString();
        
        
        assertNotNull(versionedMetadata);
        assertEquals(expectedContentStr, contentStrFromStore);
        
        assertTrue(versionedMetadata.getID() > 0);
        assertTrue(versionedMetadata.getRevision() > 0);

        MCRVersionedMetadata vm3 = getVersStore().create(MCRContent.readFrom(testXmlDoc));
        assertTrue(vm3.getID() > versionedMetadata.getID());
        assertTrue(vm3.getRevision() > versionedMetadata.getRevision());
    }

    @Test
    public void createDocumentInt() throws Exception {
        int id = getVersStore().getNextFreeID();
        assertTrue(id > 0);
        Document xml1 = new Document(new Element("root"));
        MCRVersionedMetadata vm1 = getVersStore().create(MCRContent.readFrom(xml1), id);
        MCRContent xml2 = getVersStore().retrieve(id).getMetadata();
        
        assertNotNull(vm1);
        assertEquals(MCRContent.readFrom(xml1).asString(), xml2.asString());
        getVersStore().create(MCRContent.readFrom(xml1), id + 1);
        MCRContent xml3 = getVersStore().retrieve(id + 1).getMetadata();
        assertEquals(MCRContent.readFrom(xml1).asString(), xml3.asString());
    }

    @Test
    public void delete() throws Exception {
        Document xml1 = new Document(new Element("root"));
        int id = getVersStore().create(MCRContent.readFrom(xml1)).getID();
        assertTrue(getVersStore().exists(id));
        getVersStore().delete(id);
        assertFalse(getVersStore().exists(id));
    }

    @Test
    public void update() throws Exception {
        Document xml1 = new Document(new Element("root"));
        MCRVersionedMetadata vm = getVersStore().create(MCRContent.readFrom(xml1));
        Document xml3 = new Document(new Element("update"));
        long rev = vm.getRevision();
        vm.update(MCRContent.readFrom(xml3));
        assertTrue(vm.getRevision() > rev);
        MCRContent xml4 = getVersStore().retrieve(vm.getID()).getMetadata();
        assertEquals(MCRContent.readFrom(xml3).asString(), xml4.asString());
    }

    @Test
    public void retrieve() throws Exception {
        Document xml1 = new Document(new Element("root"));
        int id = getVersStore().create(MCRContent.readFrom(xml1)).getID();
        MCRVersionedMetadata sm1 = getVersStore().retrieve(id);
        MCRContent xml2 = sm1.getMetadata();
        assertEquals(MCRContent.readFrom(xml1).asString(), xml2.asString());
    }

    @Test
    public void versioning() throws Exception {
        Document xml1 = new Document(new Element("bingo"));
        MCRVersionedMetadata vm = getVersStore().create(MCRContent.readFrom(xml1));
        long baseRev = vm.getRevision();
        assertTrue(vm.isUpToDate());

        List<MCRMetadataVersion> versions = vm.listVersions();
        assertNotNull(versions);
        assertEquals(1, versions.size());
        MCRMetadataVersion mv = versions.get(0);
        assertSame(mv.getMetadataObject(), vm);
        assertEquals(baseRev, mv.getRevision());
        assertEquals(MCRSessionMgr.getCurrentSession().getCurrentUserID(), mv.getUser());
        assertEquals(MCRMetadataVersion.CREATED, mv.getType());

        bzzz();
        Document xml2 = new Document(new Element("bango"));
        vm.update(MCRContent.readFrom(xml2));
        assertTrue(vm.getRevision() > baseRev);
        assertTrue(vm.isUpToDate());

        versions = vm.listVersions();
        assertEquals(2, versions.size());
        mv = versions.get(0);
        assertEquals(baseRev, mv.getRevision());
        mv = versions.get(1);
        assertEquals(vm.getRevision(), mv.getRevision());
        assertEquals(MCRMetadataVersion.UPDATED, mv.getType());

        bzzz();
        Document xml3 = new Document(new Element("bongo"));
        vm.update(MCRContent.readFrom(xml3));

        versions = vm.listVersions();
        assertEquals(3, versions.size());
        mv = versions.get(0);
        assertEquals(baseRev, mv.getRevision());
        mv = versions.get(2);
        assertEquals(vm.getRevision(), mv.getRevision());
        assertTrue(versions.get(0).getRevision() < versions.get(1).getRevision());
        assertTrue(versions.get(1).getRevision() < versions.get(2).getRevision());
        assertTrue(versions.get(0).getDate().before(versions.get(1).getDate()));
        assertTrue(versions.get(1).getDate().before(versions.get(2).getDate()));

        xml1 = versions.get(0).retrieve().asXML();
        assertNotNull(xml1);
        assertEquals("bingo", xml1.getRootElement().getName());
        xml2 = versions.get(1).retrieve().asXML();
        assertNotNull(xml2);
        assertEquals("bango", xml2.getRootElement().getName());
        xml3 = versions.get(2).retrieve().asXML();
        assertNotNull(xml1);
        assertEquals("bongo", xml3.getRootElement().getName());

        bzzz();
        versions.get(1).restore();
        assertTrue(vm.getRevision() > versions.get(2).getRevision());
        assertTrue(vm.getLastModified().after(versions.get(2).getDate()));
        assertEquals("bango", vm.getMetadata().asXML().getRootElement().getName());
        assertEquals(4, vm.listVersions().size());
    }

    @Test
    public void createUpdateDeleteCreate() throws Exception {
        Element root = new Element("bingo");
        Document xml1 = new Document(root);
        MCRVersionedMetadata vm = getVersStore().create(MCRContent.readFrom(xml1));
        root.setName("bango");
        vm.update(MCRContent.readFrom(xml1));
        vm.delete();
        root.setName("bongo");
        vm = getVersStore().create(MCRContent.readFrom(xml1), vm.getID());
        List<MCRMetadataVersion> versions = vm.listVersions();
        assertEquals(4, versions.size());
        assertEquals(MCRMetadataVersion.CREATED, versions.get(0).getType());
        assertEquals(MCRMetadataVersion.UPDATED, versions.get(1).getType());
        assertEquals(MCRMetadataVersion.DELETED, versions.get(2).getType());
        assertEquals(MCRMetadataVersion.CREATED, versions.get(3).getType());
        versions.get(1).restore();
        assertEquals("bango", vm.getMetadata().asXML().getRootElement().getName());
    }

    @Test
    public void deletedVersions() throws Exception {
        Element root = new Element("bingo");
        Document xml1 = new Document(root);
        MCRVersionedMetadata vm = getVersStore().create(MCRContent.readFrom(xml1));
        assertFalse(vm.isDeleted());

        vm.delete();
        assertTrue(vm.isDeleted());
        assertFalse(getVersStore().exists(vm.getID()));

        vm = getVersStore().retrieve(vm.getID());
        assertTrue(vm.isDeleted());
        List<MCRMetadataVersion> versions = vm.listVersions();
        MCRMetadataVersion v1 = versions.get(0);
        MCRMetadataVersion v2 = versions.get(1);

        boolean cannotRestoreDeleted = false;
        try {
            v2.restore();
        } catch (MCRUsageException ex) {
            cannotRestoreDeleted = true;
        }
        assertTrue(cannotRestoreDeleted);

        v1.restore();
        assertFalse(vm.isDeleted());
        assertEquals(root.getName(), vm.getMetadata().asXML().getRootElement().getName());
    }

    @Test
    public void performance() throws Exception {
        Document xml = new Document(new Element("root"));
        LOGGER.info("Storing 10 XML documents in store:");
        long time = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            getVersStore().create(MCRContent.readFrom(xml));
        }
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        time = System.currentTimeMillis();
        xml = new Document(new Element("update"));
        LOGGER.info("Updating 10 XML documents in store:");
        for (Iterator<Integer> ids = getVersStore().listIDs(MCRStore.ASCENDING); ids.hasNext();) {
            getVersStore().retrieve(ids.next()).update(MCRContent.readFrom(xml));
        }
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");

        time = System.currentTimeMillis();
        LOGGER.info("Deleting 10 XML documents from store:");
        for (Iterator<Integer> ids = getVersStore().listIDs(MCRStore.ASCENDING); ids.hasNext();) {
            ids.next();
            ids.remove();
        }
        LOGGER.info("Time: " + (System.currentTimeMillis() - time) + " ms");
    }
}
