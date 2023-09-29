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
package org.jkiss.dbeaver.model.fs;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.auth.SMSessionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class DBFFileSystemManager {
    private static final Log log = Log.getLog(DBFFileSystemManager.class);
    private final Map<String, FileSystemProvider> nioFileSystems = new LinkedHashMap<>();
    private final Map<String, DBFVirtualFileSystem> dbfFileSystems = new LinkedHashMap<>();
    @NotNull
    private final SMSessionContext sessionContext;

    public DBFFileSystemManager(@NotNull SMSessionContext sessionContext) {
        this.sessionContext = sessionContext;
    }


    public synchronized void reloadFileSystems(@NotNull DBRProgressMonitor monitor) {
        clear();
        var fsRegistry = DBWorkbench.getPlatform().getFileSystemRegistry();
        for (DBFFileSystemDescriptor fileSystemProviderDescriptor : fsRegistry.getFileSystemProviders()) {
            var fsProvider = fileSystemProviderDescriptor.getInstance();
            try {
                FileSystemProvider nioFileSystem = fsProvider.createNioFileSystemProvider(monitor, sessionContext);
                if (nioFileSystem == null) {
                    continue;
                }
                nioFileSystems.put(nioFileSystem.getScheme(), nioFileSystem);
            } catch (DBException e) {
                log.error("Failed to create nio fs: " + e.getMessage(), e);
            }
            for (DBFVirtualFileSystem dbfFileSystem : fsProvider.getAvailableFileSystems(monitor, sessionContext)) {
                dbfFileSystems.put(dbfFileSystem.getId(), dbfFileSystem);
            }
        }
    }

    public synchronized void clear() {
        nioFileSystems.clear();
        dbfFileSystems.clear();
    }

    @NotNull
    public Path of(URI uri) throws DBException {
        String fsType = uri.getScheme();
        String fsId = uri.getHost();

//        "s3://config/{something}"
//        "gcp://config/{something}/{2}/{3}"
//        "rm://project_id/"
//        "rm://project_id/root/Scripts/folder/script.sql"

        dbfFileSystems.get(fsId).of(uri);
        if (CommonUtils.isEmpty(fsId)) {
            throw new DBException("File system id not present in file uri: " + uri);
        }
        FileSystemProvider fs = nioFileSystems.get(fsId);
        if (fs == null) {
            throw new DBException("File system not found" + fsId);
        }

        try {
            return fs.getPath(uri);
        } catch (Throwable e) {
            throw new DBException(String.format("Failed to get path from uri[%s]: %s", uri, e.getMessage()), e);
        }
    }

    @NotNull
    public Collection<DBFVirtualFileSystem> getDbfFileSystems() {
        return dbfFileSystems.values();
    }

    @NotNull
    public Collection<FileSystemProvider> getNioFileSystems() {
        return nioFileSystems.values();
    }
}
