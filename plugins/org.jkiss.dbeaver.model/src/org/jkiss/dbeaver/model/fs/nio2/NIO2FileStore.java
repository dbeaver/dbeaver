/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.fs.nio2;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.fs.nio.NIOFileSystemRoot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class NIO2FileStore extends FileStore {
    private final NIOFileSystemRoot root;
    private final Path path;
    private final URI uri;
    private volatile String[] childNames;
    private volatile NIO2FileInfo info;
    private boolean allowedToLoadChildNames;

    public NIO2FileStore(@NotNull NIOFileSystemRoot root, @NotNull Path path) {
        this.root = root;
        this.path = path;
        this.uri = NIO2FileSystem.toURI(root.getProject(), root.getRoot(), path);
    }

    public void setAllowedToLoadChildNames(boolean allowedToLoadChildNames) {
        this.allowedToLoadChildNames = allowedToLoadChildNames;
    }

    @Override
    public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
        if (!allowedToLoadChildNames) {
            return new String[0];
        }

        if (childNames == null) {
            synchronized (this) {
                if (childNames == null) {
                    try (Stream<Path> stream = Files.list(path)) {
                        childNames = stream
                            .filter(NIO2FileStore::isValidPath)
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .toArray(String[]::new);
                    } catch (IOException e) {
                        throw new CoreException(Status.error("Unable to get child names", e));
                    }
                }
            }
        }

        return childNames;
    }

    public boolean childrenCached() {
        return childNames != null;
    }

    @Override
    public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
        if (options != EFS.NONE) {
            throw new CoreException(Status.error("Unsupported options: " + options));
        }

        if (info == null) {
            synchronized (this) {
                if (info == null) {
                    info = new NIO2FileInfo(path);
                }
            }
        }

        return info;
    }

    @Override
    public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
        if (options != EFS.NONE) {
            throw new CoreException(Status.error("Unsupported options: " + options));
        }

        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new CoreException(Status.error("Unable to open input stream", e));
        }
    }

    @Override
    public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
        if (options != EFS.NONE) {
            throw new CoreException(Status.error("Unsupported options: " + options));
        }

        try {
            return Files.newOutputStream(path);
        } catch (IOException e) {
            throw new CoreException(Status.error("Unable to open output stream", e));
        }
    }

    @Override
    public void delete(int options, IProgressMonitor monitor) throws CoreException {
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new CoreException(Status.error("Unable to delete file", e));
        }
    }

    @Override
    public void move(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
        if (options != EFS.NONE) {
            throw new CoreException(Status.error("Unsupported options: " + options));
        }

        try {
            Files.move(path, ((NIO2FileStore) destination).path);
        } catch (IOException e) {
            throw new CoreException(Status.error("Unable to move file", e));
        }
    }

    @Override
    public IFileStore getChild(String name) {
        return getFileSystem().getStore(NIO2FileSystem.toURI(root.getProject(), root.getRoot(), path.resolve(name)));
    }

    @Override
    public String getName() {
        final int names = path.getNameCount();

        if (names == 0) {
            return "/";
        } else {
            return path.getName(names - 1).toString();
        }
    }

    @Override
    public IFileStore getParent() {
        final Path parent = path.getParent();

        if (parent != null) {
            return getFileSystem().getStore(NIO2FileSystem.toURI(root.getProject(), root.getRoot(), parent));
        } else {
            return null;
        }
    }

    @Override
    public URI toURI() {
        return uri;
    }

    private static boolean isValidPath(@NotNull Path path) {
        try {
            path.toUri();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
