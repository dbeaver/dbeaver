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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.fs.event.DBFEvent;
import org.jkiss.dbeaver.model.fs.event.DBFEventListener;
import org.jkiss.dbeaver.model.fs.event.DBFEventManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class DBFFileSystemManager implements DBFEventListener {
    public static final String QUERY_PARAM_FS_ID = "fs";
    public static final Pattern URI_SCHEME_PREFIX = Pattern.compile("[a-z\\d]+:/.+", Pattern.CASE_INSENSITIVE);

    private static final Log log = Log.getLog(DBFFileSystemManager.class);
    private volatile Map<String, DBFVirtualFileSystem> dbfFileSystems;
    @NotNull
    private final DBPProject project;

    public DBFFileSystemManager(@NotNull DBPProject project) {
        this.project = project;
        DBFEventManager.getInstance().addListener(this);
    }

    public synchronized boolean reloadFileSystems(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (dbfFileSystems != null) {
            for (DBFVirtualFileSystem fs : dbfFileSystems.values()) {
                try {
                    fs.close();
                } catch (IOException e) {
                    log.debug("Error closing virtual FS", e);
                }
            }
        }
        Map<String, DBFVirtualFileSystem> fsList = new LinkedHashMap<>();
        var fsRegistry = DBWorkbench.getPlatform().getFileSystemRegistry();
        for (DBFFileSystemDescriptor fileSystemProviderDescriptor : fsRegistry.getFileSystemProviders()) {
            var fsProvider = fileSystemProviderDescriptor.getInstance();
            for (DBFVirtualFileSystem dbfFileSystem : fsProvider.getAvailableFileSystems(monitor, project)) {
                fsList.put(dbfFileSystem.getId(), dbfFileSystem);
            }
        }
        dbfFileSystems = fsList;
        return true;
    }

    @NotNull
    public Path getPathFromString(DBRProgressMonitor monitor, String pathOrUri) throws DBException {
        if (URI_SCHEME_PREFIX.matcher(pathOrUri).matches()) {
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

        DBFFileSystemDescriptor fsProvider = DBWorkbench.getPlatform().getFileSystemRegistry()
            .getFileSystemProviderBySchema(fsType);
        if (fsProvider == null) {
            throw new DBException("File system schema '" + fsType + "' not recognized");
        }

        if (dbfFileSystems == null) {
            reloadFileSystems(monitor);
        }
        DBFVirtualFileSystem[] fsCandidates = dbfFileSystems.values().stream()
            .filter(fs -> fs.getProviderId().equals(fsProvider.getId())).toArray(DBFVirtualFileSystem[]::new);

        return fsProvider.getInstance().getPathByURI(monitor, uri, fsCandidates);
    }

    @NotNull
    public synchronized Collection<DBFVirtualFileSystem> getVirtualFileSystems(DBRProgressMonitor monitor) throws DBException {
        if (dbfFileSystems == null) {
            reloadFileSystems(monitor);
        }
        return dbfFileSystems.values();
    }

    @Override
    public void handleFileSystemEvent(@NotNull DBFEvent event) {
        try {
            DBWorkbench.getPlatformUI().runWithMonitor(this::reloadFileSystems);
        } catch (DBException e) {
            log.error(e);
        }
    }

    public void close() {
        DBFEventManager.getInstance().removeListener(this);
    }

}
