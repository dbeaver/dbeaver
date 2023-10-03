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
package org.jkiss.dbeaver.model.nio.base;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractFileSystemProvider<
    P extends AbstractPath<P, FS, FP>,
    FS extends AbstractFileSystem<P, FS, FP>,
    FP extends AbstractFileSystemProvider<P, FS, FP>> extends FileSystemProvider {

    protected final Map<Path, FileSystem> filesystems = new ConcurrentHashMap<>();

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        throw new UnsupportedOperationException();
    }

    public boolean isOpen(@NotNull AbstractFileSystem<P, FS, FP> fileSystem) {
        return filesystems.containsKey(fileSystem.getRoot());
    }

    public void removeFileSystem(@NotNull AbstractFileSystem<P, FS, FP> fileSystem) {
        filesystems.remove(fileSystem.getRoot());
    }

    @NotNull
    public P getPath(@NotNull Path path) {
        if (isValidPath(path)) {
            return getPathType().cast(path);
        } else {
            throw new ProviderMismatchException();
        }
    }

    public boolean isValidPath(@NotNull Path path) {
        if (!(path instanceof AbstractPath)) {
            return false;
        }

        final AbstractPath<?, ?, ?> p = (AbstractPath<?, ?, ?>) path;

        return p.filesystem.provider.getFileSystemType() == getFileSystemType()
            && p.filesystem.provider.getPathType() == getPathType();
    }

    @NotNull
    protected abstract P toPath(@Nullable URI uri);

    @NotNull
    protected abstract P newPath(@NotNull AbstractFileSystem<P, FS, FP> filesystem, @NotNull String path);

    @NotNull
    protected abstract P newPath(@NotNull AbstractFileSystem<P, FS, FP> filesystem, @NotNull String path, boolean normalized);

    @NotNull
    protected abstract FS newFileSystemImpl(@NotNull Path path, @NotNull Map<String, ?> env);

    @NotNull
    protected abstract Class<P> getPathType();

    @NotNull
    protected abstract Class<FS> getFileSystemType();
}
