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
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.fs.event.DBFEventListener;
import org.jkiss.dbeaver.model.fs.event.DBFEventManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class DBFFileSystemManager implements DBFEventListener {
    private volatile Map<String, DBFVirtualFileSystem> dbfFileSystems;
    @NotNull
    private final DBPProject project;

    public DBFFileSystemManager(@NotNull DBPProject project) {
        this.project = project;
        DBFEventManager.getInstance().addListener(this);
    }

    public synchronized void reloadFileSystems(@NotNull DBRProgressMonitor monitor) {
        this.dbfFileSystems = new LinkedHashMap<>();
        var fsRegistry = DBWorkbench.getPlatform().getFileSystemRegistry();
        for (DBFFileSystemDescriptor fileSystemProviderDescriptor : fsRegistry.getFileSystemProviders()) {
            var fsProvider = fileSystemProviderDescriptor.getInstance();
            for (DBFVirtualFileSystem dbfFileSystem : fsProvider.getAvailableFileSystems(monitor, project)) {
                dbfFileSystems.put(dbfFileSystem.getId(), dbfFileSystem);
            }
        }
    }

    @NotNull
    public Path getPathFromString(DBRProgressMonitor monitor, String pathOrUri) throws DBException {
        if (pathOrUri.contains("://")) {
            return getPathFromURI(monitor, URI.create(pathOrUri));
        } else {
            return Path.of(pathOrUri);
        }
    }

    @NotNull
    public Path getPathFromURI(DBRProgressMonitor monitor, URI uri) throws DBException {
        if (IOUtils.isLocalURI(uri)) {
            return Path.of(uri);
        }
        String fsType = uri.getScheme();
        if (CommonUtils.isEmpty(fsType)) {
            throw new DBException("File system type not present in the file uri: " + uri);
        }
        String fsId = uri.getAuthority();
        if (CommonUtils.isEmpty(fsId)) {
            throw new DBException("File system id not present in the file uri: " + uri);
        }
        DBFVirtualFileSystem fileSystem = getVirtualFileSystems()
            .stream()
            .filter(fs -> fs.getType().equals(fsType) && fs.getId().equals(fsId))
            .findFirst()
            .orElseThrow(() -> new DBException("Cannot find file system provider for the uri '" + uri + "'"));

        try {
            return fileSystem.getPathByURI(monitor, uri);
        } catch (Throwable e) {
            throw new DBException("Failed to get path from uri '" + uri + "': " + e.getMessage(), e);
        }
    }

    @NotNull
    public synchronized Collection<DBFVirtualFileSystem> getVirtualFileSystems() {
        if (dbfFileSystems == null) {
            reloadFileSystems(new LoggingProgressMonitor());
        }
        return dbfFileSystems.values();
    }

    @Override
    public void handleFSEvent() {
        reloadFileSystems(new LoggingProgressMonitor());
    }

    public void close() {
        DBFEventManager.getInstance().removeListener(this);
    }

}
