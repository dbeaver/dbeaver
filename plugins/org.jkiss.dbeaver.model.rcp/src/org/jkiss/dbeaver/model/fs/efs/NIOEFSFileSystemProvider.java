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
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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

    private final Map<URI, NIOEFSFileSystem> fileSystemMap = new HashMap<>();

    @Override
    public String getScheme() {
        return "efs";
    }

    @Override
    @NotNull
    public NIOEFSFileSystem newFileSystem(@NotNull URI uri, @Nullable Map<String, ?> ignored) throws IOException {
        try {
            URI rootStoreUri = getRootSoreUri(uri);
            fileSystemMap.put(rootStoreUri, new NIOEFSFileSystem(this, EFS.getStore(rootStoreUri)));
            return getFileSystem(uri);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    @NotNull
    public NIOEFSFileSystem getFileSystem(@NotNull URI uri) {
        NIOEFSFileSystem fileSystem = fileSystemMap.get(getRootSoreUri(uri));
        if (fileSystem != null) {
            return fileSystem;
        }
        throw new FileSystemNotFoundException("Filesystem for: " + uri + "not yet created. Use newFileSystem() instead");
    }

    @NotNull
    private URI getRootSoreUri(@NotNull URI uri) {
        return NIOEFSUtils.createCopyWithPath(uri, "/");
    }

    @NotNull
    public NIOEFSFileSystem getOrCreateFileSystem(@NotNull URI uri) {
        try {
            return getFileSystem(uri);
        } catch (Exception e) {
            try {
                return newFileSystem(uri, Map.of());
            } catch (IOException ex) {
                throw new FileSystemNotFoundException("Failed to create new file system for: " + uri + " reason:" + e.getMessage());
            }
        }
    }

    @Override
    @NotNull
    public NIOEFSPath getPath(@NotNull URI uri) {
        return getOrCreateFileSystem(uri).getPath(uri.getPath());
    }

    @Override
    @NotNull
    public SeekableByteChannel newByteChannel(
        @NotNull Path path, Set<? extends OpenOption> options,
        @NotNull FileAttribute<?>... attrs
    ) throws IOException {
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException("Cannot open channel for a folder");
        }
        NIOEFSPath nioefsPath = toNIOEFSPath(path);
        IFileStore store = nioefsPath.createStore();
        if (exists(nioefsPath)) {
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
    @NotNull
    public DirectoryStream<Path> newDirectoryStream(@NotNull Path dir, @Nullable DirectoryStream.Filter<? super Path> filter)
    throws IOException {
        IFileStore store = toNIOEFSPath(dir).createStore();
        IFileStore[] children;
        try {
            children = store.childStores(EFS.NONE, null);
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
        IFileStore store = toNIOEFSPath(dir).createStore();
        performStoreOperation(() -> store.mkdir(EFS.NONE, null));
    }

    @Override
    public void delete(@NotNull Path path) throws IOException {
        IFileStore store = toNIOEFSPath(path).createStore();
        performStoreOperation(() -> store.delete(EFS.NONE, null));
    }

    @Override
    public void copy(@NotNull Path source, @NotNull Path target, @NotNull CopyOption... options) throws IOException {
        copyOrMoveOperation(IFileStore::copy, source, target, options);
    }

    @Override
    public void move(@NotNull Path source, @NotNull Path target, @NotNull CopyOption... options) throws IOException {
        copyOrMoveOperation(IFileStore::move, source, target, options);
    }

    private void copyOrMoveOperation(
        @NotNull ToStoreOperation operation,
        @NotNull Path source,
        @NotNull Path target,
        @NotNull CopyOption... options
    )
    throws IOException {
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
            operation.execute(src, dst, efsOptions, null);
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
    @NotNull
    public FileStore getFileStore(@NotNull Path path) throws IOException {
        return new NIOEFSFileStore(toNIOEFSPath(path).getFileSystem());
    }

    @Override
    public void checkAccess(@NotNull Path path, @NotNull AccessMode... modes) throws IOException {
        NIOEFSPath nioefsPath = toNIOEFSPath(path);
        if (!exists(nioefsPath)) {
            throw new NoSuchFileException(nioefsPath.toString());
        }
        Set<AccessMode> supportedModes = new HashSet<>();
        supportedModes.add(AccessMode.READ);
        if (nioefsPath.getFileSystem().canWrite()) {
            supportedModes.add(AccessMode.WRITE);
        }
        for (AccessMode mode : modes) {
            if (!supportedModes.contains(mode)) {
                throw new AccessDeniedException(nioefsPath.toString(), null, "Unsupported access mode: " + mode);
            }
        }
    }


    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null;
    }

    @Override
    @NotNull
    public <A extends BasicFileAttributes> A readAttributes(@NotNull Path path, @NotNull Class<A> type, @NotNull LinkOption... options)
    throws IOException {
        if (!type.isAssignableFrom(BasicFileAttributes.class)) {
            throw new UnsupportedOperationException("Only BasicFileAttributes supported");
        }
        NIOEFSPath nioefsPath = toNIOEFSPath(path);
        if (!exists(nioefsPath)) {
            throw new NoSuchFileException(nioefsPath.toString());
        }
        return type.cast(new NIOEFSBasicFileAttribute(nioefsPath.getFileInfo()));
    }

    public boolean exists(@NotNull NIOEFSPath nioefsPath) {
        return nioefsPath.getFileInfo().exists();
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

    private NIOEFSPath toNIOEFSPath(@NotNull Path path) throws IOException {
        if (path instanceof NIOEFSPath nioefsPath) {
            return nioefsPath;
        } else {
            throw new IOException("Path must be an instance of " + NIOEFSPath.class.getName());
        }
    }

    private void performStoreOperation(@NotNull StoreOperation storeOperation) throws IOException {
        try {
            storeOperation.run();
        } catch (CoreException e) {
            throw new IOException(e);
        }
    }

    @FunctionalInterface
    private interface StoreOperation {
        void run() throws CoreException;
    }

    @FunctionalInterface
    private interface ToStoreOperation {
        void execute(
            @NotNull IFileStore source,
            @NotNull IFileStore destination,
            int options,
            @Nullable IProgressMonitor monitor
        ) throws CoreException;
    }
}
