/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.fs.efs;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.nio.NIOFileSystemProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.*;

public class NIOEFSFileSystemProvider extends NIOFileSystemProvider {


    @Nullable
    private NIOEFSFileSystem fileSystem;

    @Override
    public String getScheme() {
        return fileSystem != null ? fileSystem.getUri().getScheme() : "efs";
    }

    @Override
    @NotNull
    public NIOEFSFileSystem newFileSystem(@NotNull URI uri, @Nullable Map<String, ?> ignored) throws IOException {
        return new NIOEFSFileSystem(uri, this);
    }

    @Override
    @NotNull
    public NIOEFSFileSystem getFileSystem(@NotNull URI uri) {
        try {
            if (fileSystem == null || !fileSystem.getUri().equals(uri)) {
                fileSystem = newFileSystem(uri, Map.of());
            }
            return fileSystem;
        } catch (IOException e) {
            throw new FileSystemNotFoundException("File system not found: " + e.getMessage());
        }
    }

    @Override
    @NotNull
    public NIOEFSPath getPath(@NotNull URI uri) {
        return NIOEFSPath.of(uri);
    }

    @Override
    public SeekableByteChannel newByteChannel(
        @NotNull Path path, Set<? extends OpenOption> options,
        FileAttribute<?>... attrs
    ) throws IOException {
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException("Cannot open channel for a folder");
        }
        var store = getStore(path);
        if (store.fetchInfo().exists()) {
            try (InputStream out = store.openInputStream(EFS.NONE, null)) {
                return new NIOEFSByteArrayChannel(out.readAllBytes(), options, store);
            } catch (CoreException e) {
                throw new IOException(e);
            }
        } else {
            return new NIOEFSByteArrayChannel(new byte[0], options, store);
        }
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(@NotNull Path dir, @Nullable DirectoryStream.Filter<? super Path> filter)
    throws IOException {
        IFileStore store = getStore(dir);
        IFileStore[] children;
        try {
            children = store.childStores(EFS.NONE, null); // [web:35]
        } catch (CoreException e) {
            throw new IOException(e);
        }

        List<Path> paths = new ArrayList<>(children.length);
        for (IFileStore child : children) {
            URI childUri = child.toURI();
            Path childPath = getPath(childUri);
            if (filter == null || filter.accept(childPath)) {
                paths.add(childPath);
            }
        }

        return new DirectoryStream<Path>() {
            @Override
            public Iterator<Path> iterator() {
                return paths.iterator();
            }

            @Override
            public void close() {
                // nothing to close
            }
        };
    }

    @Override
    public void createDirectory(@NotNull Path dir, @Nullable FileAttribute<?>... ignored) throws IOException {
        IFileStore store = getStore(dir);
        try {
            store.mkdir(EFS.NONE, null);
        } catch (CoreException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void delete(@NotNull Path path) throws IOException {
        IFileStore store = getStore(path);
        try {
            store.delete(EFS.NONE, null);
        } catch (CoreException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void copy(@NotNull Path source, @NotNull Path target, CopyOption... options) throws IOException {
        int efsOptions = EFS.NONE;
        for (CopyOption opt : options) {
            if (opt == StandardCopyOption.REPLACE_EXISTING) {
                efsOptions |= EFS.OVERWRITE;
            } else {
                throw new UnsupportedOperationException(
                    "Only supported option is StandardCopyOption.REPLACE_EXISTING, but found: " + Arrays.toString(options));
            }
        }
        IFileStore src = getStore(source);
        IFileStore dst = getStore(target);
        try {
            src.copy(dst, efsOptions, null);
        } catch (CoreException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        int efsOptions = EFS.NONE;
        for (CopyOption opt : options) {
            if (opt == StandardCopyOption.REPLACE_EXISTING) {
                efsOptions |= EFS.OVERWRITE;
            } else {
                throw new UnsupportedOperationException(
                    "Only supported option is StandardCopyOption.REPLACE_EXISTING, but found: " + Arrays.toString(options));
            }
        }
        IFileStore src = getStore(source);
        IFileStore dst = getStore(target);
        try {
            src.move(dst, efsOptions, null);
        } catch (CoreException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isSameFile(@NotNull Path path, @NotNull Path path2) throws IOException {
        return path.toUri().equals(path2.toUri());
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return null;
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        // todo implement
    }


    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        // todo implement
        return null;
    }

    @Override
    @NotNull
    public <A extends BasicFileAttributes> A readAttributes(@NotNull Path path, Class<A> type, LinkOption... options) throws IOException {
        if (!type.isAssignableFrom(BasicFileAttributes.class)) {
            throw new UnsupportedOperationException("Only BasicFileAttributes supported");
        }
        IFileStore store = getStore(path);
        IFileInfo info = store.fetchInfo();
        return type.cast(new NIOEFSBasicFileAttribute(info));
    }


    @Override
    public boolean exists(@NotNull Path path, @NotNull LinkOption... options) {
        return path instanceof NIOEFSPath nioefsPath ? nioefsPath.getFileInfo().exists() : super.exists(path, options);
    }

    @NotNull
    private IFileStore getStore(@NotNull Path path) throws IOException {
        URI uri = path.toUri();
        return getStore(uri);
    }

    @NotNull
    private IFileStore getStore(@NotNull URI uri) throws IOException {
        try {
            return EFS.getStore(uri);
        } catch (CoreException e) {
            throw new IOException(e);
        }
    }
}
