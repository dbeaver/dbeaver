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
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.nio.NIOFileSystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;

public class NIOEFSFileSystem extends NIOFileSystem {

    private final NIOEFSFileSystemProvider systemProvider;

    private final IFileStore efsFileStore;


    public NIOEFSFileSystem(@NotNull URI uri, @NotNull NIOEFSFileSystemProvider systemProvider) throws IOException {
        try {
            this.efsFileStore = EFS.getStore(uri);
            this.systemProvider = systemProvider;
        } catch (CoreException e) {
            throw new IOException(e);
        }
    }

    public NIOEFSFileSystem(@NotNull IFileStore efsFileStore) {
        this.efsFileStore = efsFileStore;
        this.systemProvider = new NIOEFSFileSystemProvider();
    }

    @Override
    public FileSystemProvider provider() {
        return systemProvider;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        IFileSystem fileSystem = efsFileStore.getFileSystem();
        return !fileSystem.canDelete() && !fileSystem.canWrite();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        // todo implement
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return List.of(new NIOEFSFileStore(efsFileStore));
    }

    @Override
    public NIOEFSPath getPath(@NotNull String first, @Nullable String... more) {

        return null;
    }

    @NotNull
    public URI getUri() {
        return efsFileStore.toURI();
    }

    @NotNull
    public IFileStore getEfsFileStore() {
        return efsFileStore;
    }
}
