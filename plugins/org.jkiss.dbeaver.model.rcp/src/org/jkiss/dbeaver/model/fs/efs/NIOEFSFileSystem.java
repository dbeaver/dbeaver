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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.nio.NIOFileSystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class NIOEFSFileSystem extends NIOFileSystem {

    private final IFileStore rootFileStore;

    private final NIOEFSFileSystemProvider systemProvider;

    private final Set<String> rootChildren;

    public NIOEFSFileSystem(@NotNull NIOEFSFileSystemProvider provider, @NotNull IFileStore rootFileStore) throws CoreException {
        this.systemProvider = provider;
        this.rootFileStore = rootFileStore;
        this.rootChildren = Arrays.stream(rootFileStore.childNames(EFS.NONE, null)).collect(Collectors.toSet());
    }

    @Override
    public FileSystemProvider provider() {
        return systemProvider;
    }

    @Override
    public void close() throws IOException {
        //does nothing
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return !canDelete() && !canWrite();
    }

    public boolean canDelete() {
        return rootFileStore.getFileSystem().canDelete();
    }

    public boolean canWrite() {
        return rootFileStore.getFileSystem().canWrite();
    }

    @Override
    @NotNull
    public Iterable<Path> getRootDirectories() {
        return List.of(new NIOEFSPath(this));
    }

    @Override
    @NotNull
    public Iterable<FileStore> getFileStores() {
        return List.of(new NIOEFSFileStore(this));
    }

    @Override
    @NotNull
    public NIOEFSPath getPath(@NotNull String first, @NotNull String... more) {
        StringBuilder pathJoiner = new StringBuilder();
        StringJoiner uriCreator = new StringJoiner("/");
        if (rootChildren.contains(first)) {
            pathJoiner.append(getSeparator());
        }
        pathJoiner.append(first);
        uriCreator.add(first);
        Arrays.stream(more).forEach(p -> {
            pathJoiner.append(getSeparator()).append(p);
            uriCreator.add(p);
        });
        return new NIOEFSPath(
            NIOEFSUtils.createCopyWithPath(
                rootURI(),
                uriCreator.toString()
            ),
            pathJoiner.toString(),
            this
        );
    }

    @NotNull
    public IFileStore createStore(@NotNull String[] pathParts) {
        IFileStore store = rootFileStore;
        for (String part : pathParts) {
            store = store.getChild(part);
        }
        return store;
    }

    @NotNull
    public URI rootURI() {
        return rootFileStore.toURI();
    }

}
