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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Virtual file system utils
 */
public class DBFUtils {

    public static final String PRODUCT_FEATURE_MULTI_FS = "multi-fs";
    private static final Log log = Log.getLog(DBFUtils.class);

    private static volatile Boolean SUPPORT_MULTI_FS = null;

    private static final Map<FileSystem, String> fileSystemIdCache = new IdentityHashMap<>();

    public static boolean supportsMultiFileSystems(@NotNull DBPProject project) {
        if (SUPPORT_MULTI_FS == null) {
            SUPPORT_MULTI_FS = DBWorkbench.getPlatform().getApplication().hasProductFeature(PRODUCT_FEATURE_MULTI_FS);
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
        if (!IOUtils.isLocalFile(pathOrUri) && project != null && DBFUtils.supportsMultiFileSystems(project)) {
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

    public static URI getUriFromPath(Path path) {
        URI uri = path.toUri();
        String fileSystemId = getFileSystemId(path.getFileSystem());
        if (!CommonUtils.isEmpty(fileSystemId)) {
            try {
                if (!CommonUtils.isEmpty(uri.getAuthority())) {
                    uri = new URI(
                        uri.getScheme(),
                        uri.getAuthority(),
                        uri.getPath(),
                        DBFFileSystemManager.QUERY_PARAM_FS_ID + "=" + fileSystemId,
                        null
                    );
                } else {
                    uri = new URI(
                        uri.getScheme(),
                        uri.getHost(),
                        uri.getPath(),
                        DBFFileSystemManager.QUERY_PARAM_FS_ID + "=" + fileSystemId,
                        null
                    );
                }
            } catch (URISyntaxException e) {
                log.debug("Error generating FS URI", e);
            }
        }
        return uri;
    }

    public static Map<String, String> getQueryParameters(String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, String> result = new LinkedHashMap<>();
        final String[] pairs = query.split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8) : pair;
            final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8) : null;
            result.put(key, value);
        }
        return result;
    }

    public static String getFileSystemId(FileSystem fs) {
        return fileSystemIdCache.get(fs);
    }

    public static void mapFileSystem(FileSystem fs, String id) {
        if (id == null) {
            fileSystemIdCache.remove(fs);
        } else {
            fileSystemIdCache.put(fs, id);
        }
    }

}
