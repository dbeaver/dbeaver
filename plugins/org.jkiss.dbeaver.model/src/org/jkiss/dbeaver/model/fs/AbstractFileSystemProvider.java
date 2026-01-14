/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.net.URI;
import java.nio.file.Path;

/**
 * Virtual file system provider
 */
public abstract class AbstractFileSystemProvider implements DBFFileSystemProvider {

    @NotNull
    @Override
    public Path getPathByURI(@NotNull DBRProgressMonitor monitor, @NotNull URI uri, @NotNull DBFVirtualFileSystem[] fileSystems) throws DBException {
        String fsId = DBFUtils.getQueryParameters(uri.getRawQuery()).get(DBFFileSystemManager.QUERY_PARAM_FS_ID);
        return getFileSystemByID(monitor, uri, fileSystems, fsId);
    }

    @NotNull
    protected Path getFileSystemByID(@NotNull DBRProgressMonitor monitor, @NotNull URI uri, @NotNull DBFVirtualFileSystem[] fileSystems, @Nullable String fsId) throws DBException {
        DBFVirtualFileSystem fileSystem = null;
        if (CommonUtils.isEmpty(fsId)) {
            fileSystem = fileSystems.length == 0 ? null : fileSystems[0];
        } else {
            for (DBFVirtualFileSystem fs : fileSystems) {
                if (fs.getId().equals(fsId)) {
                    fileSystem = fs;
                    break;
                }
            }
        }
        if (fileSystem == null) {
            throw new DBException("Cannot find file system provider for the uri '" + uri + "'");
        }

        try {
            return fileSystem.getPathByURI(monitor, uri);
        } catch (Throwable e) {
            throw new DBException("Failed to get path from uri '" + uri + "': " + e.getMessage(), e);
        }
    }

}
