/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.fs;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Virtual file system
 */
public interface DBFVirtualFileSystem extends Closeable {

    @NotNull
    String getFileSystemDisplayName();

    @NotNull
    String getType();

    String getDescription();

    DBPImage getIcon();

    @NotNull
    String getId();

    @NotNull
    String getProviderId();

    @NotNull
    List<? extends DBFVirtualFileSystemRoot> getRootFolders(DBRProgressMonitor monitor) throws DBException;

    @NotNull
    Path getPathByURI(@NotNull DBRProgressMonitor monitor, @NotNull URI uri) throws DBException;

    /**
     * Splits URI to path segments corresponding to file hierarchy.
     * Typical case: /root/folder/folder/file
     */
    String[] getURISegments(URI uri);

    default void refreshRoots(DBRProgressMonitor monitor) throws DBException {}

    default boolean supportsEmptyFolders() {
        return true;
    }

    /**
     * USed for performance. Some cloud FS (like Azure) perform remote call for each check for directory
     */
    default boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }

    @Override
    default void close() throws IOException {}

}
