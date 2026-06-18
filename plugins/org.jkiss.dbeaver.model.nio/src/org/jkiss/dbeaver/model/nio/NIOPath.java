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
package org.jkiss.dbeaver.model.nio;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;

public abstract class NIOPath implements Path {
    @Nullable
    protected final String path;
    protected final FileSystem fileSystem;

    protected NIOPath(@Nullable String path, FileSystem fileSystem) {
        this.fileSystem = fileSystem;
        String separator = fileSystem.getSeparator();
        String preparedPath = path;
        if (CommonUtils.isNotEmpty(path) && path.endsWith(separator)) {
            if (path.length() == separator.length()) {
                // its root
                preparedPath = null;
            } else {
                preparedPath = path.substring(0, path.length() - separator.length());
            }
        }
        this.path = preparedPath;
    }


    @Override
    public boolean isAbsolute() {
        return path == null // empty is just empty path
            || (CommonUtils.isNotEmpty(path) && path.startsWith(getFileSystem().getSeparator()));
    }

    protected String resolveString(String otherPath) {
        return NIOUtils.resolve(getFileSystem().getSeparator(), path, otherPath);
    }

    @Override
    public int getNameCount() {
        return pathParts().length;
    }

    @NotNull
    protected String[] pathParts() {
        return getParts(path);
    }

    @NotNull
    protected String[] getParts(@Nullable String pathRepresentation) {
        return CommonUtils.isEmpty(pathRepresentation) ? new String[0]
            : Arrays.stream(pathRepresentation.split(getFileSystem().getSeparator()))
            .filter(CommonUtils::isNotEmpty)
            .toArray(String[]::new);
    }

    @NotNull
    protected String toPathRepresentation(@NotNull String... parts) {
        return String.join(getFileSystem().getSeparator(), parts);
    }

    @Override
    public int compareTo(@NotNull Path other) {
        return toString().compareTo(other.toString());
    }

    @NotNull
    @Override
    public String toString() {
        return toUri().toString();
    }


    @Override
    public boolean startsWith(@NotNull Path other) {
        if (getClass() != other.getClass()) {
            return false;
        }
        return toString().startsWith(other.toString());
    }
    @Override
    public boolean endsWith(@NotNull Path other) {
        if (getClass() != other.getClass()) {
            return false;
        }
        return toString().endsWith(other.toUri().getPath());
    }

    @Override
    public File toFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register(@NotNull WatchService watcher, @NotNull WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws
        IOException {
        throw new UnsupportedOperationException();
    }

}
