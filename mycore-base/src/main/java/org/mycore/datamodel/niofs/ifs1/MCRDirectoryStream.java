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
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.apache.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRMD5AttributeView;
import org.mycore.datamodel.niofs.MCRPath;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * A {@link SecureDirectoryStream} on internal file system.
 * This implementation uses IFS directly. Do use this class but stick to the interface.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRDirectoryStream implements SecureDirectoryStream<Path> {
    static Logger LOGGER = Logger.getLogger(MCRDirectoryStream.class);

    private MCRDirectory dir;

    private Iterator<Path> iterator;

    /**
     * @throws IOException 
     *         if 'path' is not from {@link MCRIFSFileSystem}
     * 
     */
    public MCRDirectoryStream(MCRDirectory dir) throws IOException {
        this.dir = Objects.requireNonNull(dir, "'dir' may not be null");
    }

    @Override
    public Iterator<Path> iterator() {
        checkClosed();
        synchronized (this) {
            if (iterator != null) {
                throw new IllegalStateException("Iterator already obtained");
            }
            iterator = new MCRDirectoryIterator(this);
            return iterator;
        }
    }

    @Override
    public void close() throws IOException {
        dir = null;
    }

    void checkClosed() {
        if (dir == null) {
            throw new ClosedDirectoryStreamException();
        }
    }

    MCRPath checkRelativePath(Path path) {
        if (path.isAbsolute()) {
            throw new IllegalArgumentException(path + " is absolute.");
        }
        return checkFileSystem(path);
    }

    private MCRPath checkFileSystem(Path path) {
        if (!(path.getFileSystem() instanceof MCRIFSFileSystem)) {
            throw new IllegalArgumentException(path + " is not from " + MCRIFSFileSystem.class.getSimpleName());
        }
        return MCRPath.toMCRPath(path);
    }

    @Override
    public SecureDirectoryStream<Path> newDirectoryStream(Path path, LinkOption... options) throws IOException {
        checkClosed();
        MCRPath mcrPath = checkFileSystem(path);
        if (mcrPath.isAbsolute()) {
            return (SecureDirectoryStream<Path>) Files.newDirectoryStream(mcrPath);
        }
        MCRFilesystemNode childByPath = dir.getChildByPath(mcrPath.toString());
        if (childByPath == null || childByPath instanceof MCRFile) {
            throw new NoSuchFileException(dir.toString(), path.toString(), "Does not exist or is a file.");
        }
        return new MCRDirectoryStream((MCRDirectory) childByPath);
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
        throws IOException {
        checkClosed();
        MCRPath mcrPath = checkRelativePath(path);
        if (mcrPath.isAbsolute()) {
            return mcrPath.getFileSystem().provider().newByteChannel(mcrPath, options, attrs);
        }
        Set<? extends OpenOption> fileOpenOptions = Sets.filter(options, new Predicate<OpenOption>() {
            @Override
            public boolean apply(OpenOption option) {
                return !(option == StandardOpenOption.CREATE || option == StandardOpenOption.CREATE_NEW);
            }
        });
        boolean create = options.contains(StandardOpenOption.CREATE);
        boolean createNew = options.contains(StandardOpenOption.CREATE_NEW);
        if (create || createNew) {
            for (OpenOption option : fileOpenOptions) {
                //check before we create any file instance
                MCRFile.checkOpenOption(option);
            }
        }
        MCRFileSystemProvider provider = (MCRFileSystemProvider) mcrPath.getFileSystem().provider();
        MCRFile mcrFile = MCRFileSystemUtils.getMCRFile(dir, mcrPath, create, createNew);
        return provider.newByteChannel(mcrFile.toPath(), fileOpenOptions, attrs);
    }

    @Override
    public void deleteFile(Path path) throws IOException {
        deleteFileSystemNode(checkFileSystem(path));
    }

    @Override
    public void deleteDirectory(Path path) throws IOException {
        deleteFileSystemNode(checkFileSystem(path));
    }

    /**
     * Deletes {@link MCRFilesystemNode} if it exists.
     * @param path relative or absolute
     * @throws IOException 
     */
    private void deleteFileSystemNode(MCRPath path) throws IOException {
        checkClosed();
        if (path.isAbsolute()) {
            Files.delete(path);
        }
        MCRFilesystemNode childByPath = dir.getChildByPath(path.toString());
        if (childByPath == null) {
            throw new NoSuchFileException(dir.toPath().toString(), path.toString(), null);
        }
        try {
            childByPath.delete();
        } catch (MCRPersistenceException e) {
            throw new IOException("Error whil deleting file system node.", e);
        }
    }

    @Override
    public void move(Path srcpath, SecureDirectoryStream<Path> targetdir, Path targetpath) throws IOException {
        checkClosed();
        checkFileSystem(srcpath);
        checkFileSystem(targetpath);
        throw new AtomicMoveNotSupportedException(srcpath.toString(), targetpath.toString(),
            "Currently not implemented");
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Class<V> type) {
        return getFileAttributeView(null, type, (LinkOption[]) null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        if (path != null) {
            MCRPath file = checkRelativePath(path);
            if (file.getNameCount() != 1) {
                throw new InvalidPathException(path.toString(), "'path' must have one name component.");
            }
        }
        checkClosed();
        if (type == null) {
            throw new NullPointerException();
        }
        Class<?> c = type;
        //must support BasicFileAttributeView
        if (c == BasicFileAttributeView.class) {
            return (V) new BasicFileAttributeViewImpl(this, path);
        }
        if (c == MCRMD5AttributeView.class) {
            return (V) new MD5FileAttributeViewImpl(this, path);
        }
        return (V) null;
    }

    private static class MCRDirectoryIterator implements Iterator<Path> {

        Path nextPath;

        boolean hasNextCalled;

        private MCRDirectoryStream mcrDirectoryStream;
        MCRFilesystemNode[] children;

        private int pos;

        public MCRDirectoryIterator(MCRDirectoryStream mcrDirectoryStream) {
            this.mcrDirectoryStream = mcrDirectoryStream;
            children = mcrDirectoryStream.dir.getChildren();
            this.nextPath = null;
            hasNextCalled = false;
            pos = -1;
        }

        @Override
        public boolean hasNext() {
            LOGGER.debug("hasNext() called: " + pos);
            MCRDirectory dir = mcrDirectoryStream.dir;
            if (dir == null) {
                return false; //stream closed
            }
            int nextPos = pos + 1;
            if (nextPos >= children.length) {
                return false;
            }
            //we are OK
            nextPath = getPath(children, nextPos);
            hasNextCalled = true;
            return true;
        }

        private MCRPath getPath(MCRFilesystemNode[] children, int index) {
            try {
                MCRPath path = children[index].toPath();
                LOGGER.debug("getting path at index " + index + ": " + path);
                return path;
            } catch (RuntimeException e) {
                throw new DirectoryIteratorException(new IOException(e));
            }
        }

        @Override
        public Path next() {
            LOGGER.debug("next() called: " + pos);
            pos++;
            if (hasNextCalled) {
                hasNextCalled = false;
                return nextPath;
            }
            MCRDirectory dir = mcrDirectoryStream.dir;
            mcrDirectoryStream.checkClosed();
            MCRFilesystemNode[] children = dir.getChildren();
            if (pos >= children.length) {
                throw new NoSuchElementException();
            }
            return getPath(children, pos);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    static class BasicFileAttributeViewImpl extends MCRBasicFileAttributeViewImpl {

        private MCRDirectoryStream mcrDirectoryStream;

        private Path fileName;

        public BasicFileAttributeViewImpl(MCRDirectoryStream mcrDirectoryStream, Path path) {
            this.mcrDirectoryStream = mcrDirectoryStream;
            if (path.toString().length() <= 2 && (path.toString().equals(".") || path.toString().equals(".."))) {
                throw new InvalidPathException(path.toString(), "'path' must be a valid file name.");
            }
            this.fileName = path;
        }

        protected MCRFilesystemNode resolveNode() throws IOException {
            MCRDirectory parent = mcrDirectoryStream.dir;
            mcrDirectoryStream.checkClosed();
            MCRFilesystemNode child;
            try {
                child = parent.getChild(fileName.toString());
            } catch (RuntimeException e) {
                throw new IOException(e);
            }
            if (child == null) {
                throw new NoSuchFileException(parent.toPath().toString(), fileName.toString(), null);
            }
            return child;
        }
    }

    private static class MD5FileAttributeViewImpl extends BasicFileAttributeViewImpl implements
        MCRMD5AttributeView<String> {

        public MD5FileAttributeViewImpl(MCRDirectoryStream mcrDirectoryStream, Path path) {
            super(mcrDirectoryStream, path);
            // TODO Auto-generated constructor stub
        }

        @Override
        public MCRFileAttributes<String> readAllAttributes() throws IOException {
            return readAttributes();
        }

        @Override
        public String name() {
            return "md5";
        }

    }
}
