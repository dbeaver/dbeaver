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

package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Driver library
 */
public interface DBPDriverLibrary {

    /**
     * Driver file type
     */
    enum FileType {
        jar,
        lib,
        executable,
        license;

        public static FileType getFileTypeByFileName(String fileName) {
            return fileName.endsWith(".jar") || fileName.endsWith(".zip") ? DBPDriverLibrary.FileType.jar : DBPDriverLibrary.FileType.lib;
        }
    }

    @NotNull
    String getDisplayName();

    /**
     * Library native id.
     * Id doesn't include version information so the same libraries with different versions have the same id.
     */
    @NotNull
    String getId();

    /**
     * Library version. If library doesn't support versions returns null.
     */
    @Nullable
    String getVersion();

    @NotNull
    DBIcon getIcon();

    @NotNull
    FileType getType();

    /**
     * Native library URI.
     * Could be a file path or maven artifact references or anything else.
     */
    @NotNull
    String getPath();

    @Nullable
    String getDescription();

    boolean isOptional();

    boolean isCustom();

    boolean isDisabled();

    void setDisabled(boolean disabled);

    boolean isDownloadable();

    @Nullable
    String getExternalURL(DBRProgressMonitor monitor);

    @Nullable
    Path getLocalFile();

    @Nullable
    Path getLocalFile(@NotNull DBRProgressMonitor monitor);

    boolean matchesCurrentPlatform();

    @Nullable
    Collection<? extends DBPDriverLibrary> getDependencies(@NotNull DBRProgressMonitor monitor) throws IOException;

    void downloadLibraryFile(@NotNull DBRProgressMonitor monitor, boolean forceUpdate, String taskName)
        throws IOException, InterruptedException;

    @NotNull
    Collection<String> getAvailableVersions(@NotNull DBRProgressMonitor monitor) throws IOException;

    String getPreferredVersion();

    void setPreferredVersion(@NotNull String version);

    void resetVersion();

    boolean isSecureDownload(@NotNull DBRProgressMonitor monitor);

    boolean isInvalidLibrary();
}
