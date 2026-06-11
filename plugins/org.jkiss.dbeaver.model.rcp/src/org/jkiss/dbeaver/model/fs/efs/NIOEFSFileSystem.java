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

import org.eclipse.core.filesystem.IFileStore;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.nio.NIOFileSystem;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

public class NIOEFSFileSystem extends NIOFileSystem {

    private final IFileStore rootFileStore;

    private final NIOEFSFileSystemProvider systemProvider;

    public NIOEFSFileSystem(@NotNull NIOEFSFileSystemProvider provider, @NotNull IFileStore rootFileStore) {
        this.systemProvider = provider;
        this.rootFileStore = rootFileStore;
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
        return !rootFileStore.getFileSystem().canDelete() && !rootFileStore.getFileSystem().canWrite();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return List.of(new NIOEFSPath(this));
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return List.of(new NIOEFSFileStore(this));
    }

    @Override
    public NIOEFSPath getPath(@NotNull String first, @NotNull String... more) {
        StringJoiner joiner = new StringJoiner(getSeparator());
        joiner.add(first);
        Arrays.stream(more).forEach(joiner::add);
        return new NIOEFSPath(joiner.toString(), this);
    }

    @NotNull
    public IFileStore createStore(@NotNull String[] pathParts) {
        IFileStore store = rootFileStore;
        for (String part : pathParts) {
            store = store.getChild(part);
        }
        return store;
    }

}
