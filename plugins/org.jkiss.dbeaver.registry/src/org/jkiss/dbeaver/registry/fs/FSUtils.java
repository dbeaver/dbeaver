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
package org.jkiss.dbeaver.registry.fs;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.fs.DBFFileSystemProvider;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

public class FSUtils {
    private static final Log log = Log.getLog(FSUtils.class);

    @Nullable
    public static Path getPathFromURI(@NotNull String fileUriString) throws DBException {
        URI fileUri = URI.create(fileUriString);
        if (!fileUri.isAbsolute() || fileUri.getScheme() == null) {
            return Path.of(fileUriString).toAbsolutePath();
        }
        FileSystem defaultFs = FileSystems.getDefault();
        if (defaultFs.provider().getScheme().equals(fileUri.getScheme())) {
            // default filesystem
            return defaultFs.provider().getPath(fileUri);
        } else {
            var externalFsProvider =
                FileSystemProviderRegistry.getInstance().getFileSystemProviderBySchema(fileUri.getScheme());
            if (externalFsProvider == null) {
                log.error("File system not found for scheme: " + fileUri.getScheme());
                return null;
            }

            DBFFileSystemProvider fileSystemProvider = externalFsProvider.getInstance();
            // Use provider's classloader because filesystem registered there as service
            ClassLoader fsClassloader = fileSystemProvider.getClass().getClassLoader();
            Map<String, ?> env = fileSystemProvider.prepareEnv(System.getenv());
            try (
                FileSystem externalFileSystem = FileSystems.newFileSystem(
                    fileUri,
                    env,
                    fsClassloader
                )
            ) {
                return externalFileSystem.provider().getPath(fileUri);
            } catch (Exception e) {
                log.error("Failed to initialize path: " + fileUri, e);
            }
        }
        return null;
    }
}
