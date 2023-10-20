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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.IOUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

/**
 * Virtual file system utils
 */
public class DBFUtils {

    private static final Log log = Log.getLog(DBFUtils.class);
    private static volatile Boolean SUPPORT_MULTI_FS = null;

    public static boolean supportsMultiFileSystems(@NotNull DBPProject project) {
        if (SUPPORT_MULTI_FS == null) {
            for (DBFFileSystemDescriptor fsProvider : DBWorkbench.getPlatform().getFileSystemRegistry().getFileSystemProviders()) {
                DBFVirtualFileSystem[] fsList = fsProvider.getInstance().getAvailableFileSystems(
                    new VoidProgressMonitor(), project);
                if (!ArrayUtils.isEmpty(fsList)) {
                    SUPPORT_MULTI_FS = true;
                    break;
                }
            }
            if (SUPPORT_MULTI_FS == null) {
                SUPPORT_MULTI_FS = false;
            }
        }
        return SUPPORT_MULTI_FS;
    }

    @NotNull
    public static Path resolvePathFromString(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBPProject project,
        @NotNull String pathOrUri
    ) throws DBException {
        if (project != null) {
            return project.getFileSystemManager().getPathFromString(monitor, pathOrUri);
        } else {
            return Path.of(pathOrUri);
        }
    }

    @NotNull
    public static Path resolvePathFromURI(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBPProject project,
        @NotNull URI uri
    ) throws DBException {
        if (project != null) {
            return project.getFileSystemManager().getPathFromURI(monitor, uri);
        } else {
            return Path.of(uri);
        }
    }

    @NotNull
    public static Path resolvePathFromString(
        @NotNull DBRRunnableContext runnableContext,
        @Nullable DBPProject project,
        @NotNull String pathOrUri
    ) throws DBException {
        if (!IOUtils.isLocalFile(pathOrUri) &&
            (project != null && DBFUtils.supportsMultiFileSystems(project))) {
            try {
                Path[] result = new Path[1];
                runnableContext.run(true, true, monitor -> {
                    try {
                        result[0] = project.getFileSystemManager().getPathFromString(monitor, pathOrUri);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
                return result[0];
            } catch (InvocationTargetException e) {
                throw new DBException("Error getting path", e.getTargetException());
            } catch (InterruptedException e) {
                throw new DBException("Canceled");
            }
        } else {
            if (pathOrUri.startsWith("file:")) {
                try {
                    return Path.of(new URI(pathOrUri));
                } catch (URISyntaxException e) {
                    log.debug(e);
                }
            }
            return Path.of(pathOrUri);
        }
    }
}
