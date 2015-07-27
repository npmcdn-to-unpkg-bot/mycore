/*
 * $Id$
 * $Revision: 5697 $ $Date: Jul 10, 2014 $
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

package org.mycore.datamodel.niofs.ifs1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;

import org.apache.commons.codec.digest.DigestUtils;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRFile;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRFileChannel extends FileChannel {

    private FileChannel baseChannel;

    private MCRFile file;

    private boolean read, write;

    public MCRFileChannel(MCRFile file, FileChannel baseChannel, boolean read, boolean write) {
        this.file = file;
        this.baseChannel = baseChannel;
        this.read = read;
        this.write = write;
    }

    public void implCloseChannel() throws IOException {
        baseChannel.close(); //MCR-1003 close before updating metadata, as we read attributes from this file later
        updateMetadata();
    }

    private void updateMetadata() throws IOException {
        if (!write) {
            return;
        }
        MessageDigest md5Digest = DigestUtils.getMd5Digest();
        FileChannel md5Channel = (FileChannel) ((read && baseChannel instanceof FileChannel) ? this : Files
            .newByteChannel(file.getLocalFile().toPath(), StandardOpenOption.READ));
        try {
            ByteBuffer byteBuffer = md5Channel.map(FileChannel.MapMode.READ_ONLY, 0, md5Channel.size());
            while (byteBuffer.hasRemaining()) {
                md5Digest.update(byteBuffer);
            }
        } finally {
            if (md5Channel != baseChannel) {
                md5Channel.close();
            }
        }
        String md5 = MCRContentInputStream.getMD5String(md5Digest.digest());
        BasicFileAttributes attrs = Files.readAttributes(file.getLocalFile().toPath(), BasicFileAttributes.class);
        file.adjustMetadata(attrs.lastModifiedTime(), md5, attrs.size());
    }

    //Delegate to baseChannel

    public int read(ByteBuffer dst) throws IOException {
        return baseChannel.read(dst);
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return baseChannel.read(dsts, offset, length);
    }

    public int write(ByteBuffer src) throws IOException {
        return baseChannel.write(src);
    }

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        return baseChannel.write(srcs, offset, length);
    }

    public long position() throws IOException {
        return baseChannel.position();
    }

    public FileChannel position(long newPosition) throws IOException {
        return baseChannel.position(newPosition);
    }

    public long size() throws IOException {
        return baseChannel.size();
    }

    public FileChannel truncate(long size) throws IOException {
        return baseChannel.truncate(size);
    }

    public void force(boolean metaData) throws IOException {
        baseChannel.force(metaData);
    }

    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        return baseChannel.transferTo(position, count, target);
    }

    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        return baseChannel.transferFrom(src, position, count);
    }

    public int read(ByteBuffer dst, long position) throws IOException {
        return baseChannel.read(dst, position);
    }

    public int write(ByteBuffer src, long position) throws IOException {
        return baseChannel.write(src, position);
    }

    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        return baseChannel.map(mode, position, size);
    }

    public FileLock lock(long position, long size, boolean shared) throws IOException {
        return baseChannel.lock(position, size, shared);
    }

    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return baseChannel.tryLock(position, size, shared);
    }

}
