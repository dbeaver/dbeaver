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
import org.jkiss.dbeaver.model.nio.NIOPath;
import org.jkiss.utils.ArrayUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public class NIOEFSPath extends NIOPath {

    private final IFileStore efsFileStore;

    private NIOEFSPath(@NotNull String path, @NotNull NIOEFSFileSystem fileSystem) {
        super(path, fileSystem);
        efsFileStore = fileSystem.getEfsFileStore();
    }

    @Override
    @NotNull
    public NIOEFSFileSystem getFileSystem() {
        return (NIOEFSFileSystem) fileSystem;
    }

    @Override
    @NotNull
    public NIOEFSPath getRoot() {
        return of(NIOEFSUtils.getRootStore(efsFileStore));
    }

    @Override
    @NotNull
    public NIOEFSPath getFileName() {
        var parts = pathParts();
        if (ArrayUtils.isEmpty(parts)) {
            return this;
        }
        return getFileSystem().getPath(efsFileStore.getName());
    }

    @Override
    @Nullable
    public NIOEFSPath getParent() {
        IFileStore parent = efsFileStore.getParent();
        return parent == null ? null : of(efsFileStore.getParent());
    }

    @Override
    @NotNull
    public NIOEFSPath getName(int index) {
        String[] parts = pathParts();
        if (index < 0 || index > parts.length) {
            throw new IllegalArgumentException("Invalid index value: " + index);
        }
        // todo think about null pointer here in case if root reached
        IFileStore foundParent = efsFileStore;
        for (int i = parts.length - 1; i != index; i--) {
            foundParent = efsFileStore.getParent();
        }
        return of(foundParent);
    }

    @Override
    @NotNull
    public NIOEFSPath relativize(@NotNull Path other) {
        URI relativeUri = toUri().resolve(other.toUri());
        return NIOEFSPath.of(relativeUri);
    }

    @Override
    @NotNull
    public NIOEFSPath normalize() {
        return this;
    }

    @Override
    @NotNull
    public NIOEFSPath resolve(@NotNull Path other) {
        return relativize(other);
    }

    @Override
    @NotNull
    public URI toUri() {
        return efsFileStore.toURI();
    }

    @Override
    public int compareTo(@NotNull Path other) {
        return toUri().compareTo(other.toUri());
    }

    @Override
    // url based so always absolute
    public boolean isAbsolute() {
        return true;
    }

    @Override
    @NotNull
    public NIOEFSPath toAbsolutePath() {
        return this;
    }

    @Override
    public NIOEFSPath toRealPath(@NotNull LinkOption... options) throws IOException {
        return toAbsolutePath();
    }

    @NotNull
    public IFileStore getEfsFileStore() {
        return efsFileStore;
    }

    @NotNull
    public IFileInfo getFileInfo() {
        return efsFileStore.fetchInfo();
    }

    @NotNull
    public static NIOEFSPath of(@NotNull URI uri) {
        try {
            IFileStore store = EFS.getStore(uri);
            return new NIOEFSPath(uri.getPath(), new NIOEFSFileSystem(store));
        } catch (CoreException e) {
            // todo remove
            throw new RuntimeException(e + "Moked for now");
        }
    }

    @NotNull
    public static NIOEFSPath of(@NotNull IFileStore efsFileStore) {
        URI uri = efsFileStore.toURI();
        return new NIOEFSPath(uri.getPath(), new NIOEFSFileSystem(efsFileStore));
    }
}
