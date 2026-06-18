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

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.nio.NIOPath;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.StringJoiner;

public class NIOEFSPath extends NIOPath {


    private final String[] pathParts;

    private final URI directURI;

    protected NIOEFSPath(@NotNull NIOEFSFileSystem fileSystem) {
        this(fileSystem.rootURI(), null, fileSystem);
    }

    protected NIOEFSPath(@NotNull URI directURI, @Nullable String path, @NotNull NIOEFSFileSystem fileSystem) {
        super(path, fileSystem);
        this.directURI = directURI;
        this.pathParts = super.pathParts();
    }

    @Override
    @NotNull
    public NIOEFSFileSystem getFileSystem() {
        return (NIOEFSFileSystem) fileSystem;
    }

    @Override
    @NotNull
    public NIOEFSPath getRoot() {
        return new NIOEFSPath(getFileSystem());
    }

    @Override
    @NotNull
    public NIOEFSPath getFileName() {
        if (ArrayUtils.isEmpty(pathParts)) {
            return this;
        }
        return getName(pathParts.length - 1);
    }

    @Override
    @Nullable
    public NIOEFSPath getParent() {
        return pathParts.length > 1
            ? subpath(0, pathParts.length - 1)
            : isAbsolute() && !isRoot()
                ? getRoot()
                : null;
    }

    @Override
    @NotNull
    public NIOEFSPath getName(int index) {
        if (index < 0 || index >= pathParts.length) {
            throw new IllegalArgumentException("Invalid index value: " + index);
        }
        return subpath(index, index + 1);
    }

    @Override
    @NotNull
    public NIOEFSPath subpath(int beginIndex, int endIndex) {
        int length = pathParts.length;
        if (beginIndex < 0 || endIndex > length || beginIndex >= endIndex) {
            throw new IllegalArgumentException(
                "Invalid subpath range: [" + beginIndex + ", " + endIndex + ") for length " + length
            );
        }
        StringJoiner pathResolver = new StringJoiner(getFileSystem().getSeparator());
        StringJoiner uriPartRemover = new StringJoiner("/");
        for (int i = beginIndex; i < endIndex; i++) {
            pathResolver.add(pathParts[i]);
            uriPartRemover.add(pathParts[i]);
        }
        //Path a/b/c/a/b, remover /c result-> a/b
        String uriShortedPath = directURI.getPath().replaceAll(uriPartRemover + ".*", uriPartRemover.toString());

        return new NIOEFSPath(NIOEFSUtils.createCopyWithPath(directURI, uriShortedPath), pathResolver.toString(), getFileSystem());
    }

    @Override
    @NotNull
    public NIOEFSPath relativize(@NotNull Path other) {
        if (other instanceof NIOEFSPath nioefsPath && nioefsPath.getFileSystem().rootURI().equals(getFileSystem().rootURI())) {
            URI relativeUri = directURI.resolve(nioefsPath.directURI);
            return new NIOEFSPath(relativeUri, directURI.relativize(nioefsPath.directURI).getPath(), getFileSystem());
        } else {
            throw new IllegalArgumentException("Can only relativize paths with the same underlying file systems");
        }
    }

    @Override
    @NotNull
    public NIOEFSPath normalize() {
        return this;
    }

    @Override
    @NotNull
    public Path resolve(@NotNull Path other) {
        if (other.isAbsolute()) {
            return other;
        } else if (other.getNameCount() == 0) {
            return this;
        }
        String replacedSeparators = other.normalize().toString()
            .replace(other.getFileSystem().getSeparator(), getFileSystem().getSeparator());
        return resolve(replacedSeparators);
    }

    @Override
    @NotNull
    public NIOEFSPath resolve(@Nullable String other) {
        if (CommonUtils.isEmpty(other)) {
            return this;
        }
        String resolvedPath = resolveString(other);
        return new NIOEFSPath(NIOEFSUtils.createCopyWithPath(directURI, resolvedPath), resolvedPath, getFileSystem());
    }

    @Override
    @NotNull
    public URI toUri() {
        return directURI;
    }

    @Override
    public int compareTo(@NotNull Path other) {
        return toUri().compareTo(other.toUri());
    }

    @Override
    @NotNull
    public NIOEFSPath toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        } else {
            return new NIOEFSPath(directURI, toPathRepresentation(getParts(directURI.getPath())), getFileSystem());
        }
    }

    @Override
    @NotNull
    public NIOEFSPath toRealPath(@NotNull LinkOption... options) throws IOException {
        return toAbsolutePath();
    }

    @Override
    @NotNull
    protected String[] pathParts() {
        return pathParts;
    }

    @NotNull
    public IFileInfo getFileInfo() {
        return createStore().fetchInfo();
    }

    @NotNull
    public IFileStore createStore() {
        return getFileSystem().createStore(toAbsolutePath().pathParts);
    }

    private boolean isRoot() {
        return isAbsolute() && pathParts.length == 0;
    }

    @NotNull
    @Override
    public String toString() {
        return CommonUtils.notEmpty(path);
    }
}
