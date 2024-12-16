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
package org.jkiss.dbeaver.registry.driver;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * DriverLibraryLocal
 */
public class DriverLibraryLocal extends DriverLibraryAbstract {
    private static final Log log = Log.getLog(DriverLibraryLocal.class);
    private boolean useOriginalJar;

    public DriverLibraryLocal(DriverDescriptor driver, FileType type, String path) {
        super(driver, type, path);
    }

    public DriverLibraryLocal(DriverDescriptor driver, IConfigurationElement config) {
        super(driver, config);
    }

    public DriverLibraryLocal(DriverDescriptor driverDescriptor, DriverLibraryLocal copyFrom) {
        super(driverDescriptor, copyFrom);
    }

    @Override
    public DBPDriverLibrary copyLibrary(DriverDescriptor driverDescriptor) {
        return new DriverLibraryLocal(driverDescriptor, this);
    }

    @Override
    public boolean isDownloadable() {
        return false;
    }

    @Override
    public void resetVersion() {
        // do nothing
    }

    @Override
    public boolean isSecureDownload(@NotNull DBRProgressMonitor monitor) {
        return true;
    }

    protected String getLocalFilePath() {
        return path;
    }

    @Nullable
    @Override
    public String getExternalURL(DBRProgressMonitor monitor) {
        return null;
    }


    @Nullable
    @Override
    public Path getLocalFile() {
        // Try to use direct path
        String localFilePath = this.getLocalFilePath();
        if (DBWorkbench.isDistributed() || DBWorkbench.getPlatform().getApplication().isMultiuser()) {
            Path resolvedCache;
            List<DriverDescriptor.DriverFileInfo> driverFileInfos = driver.getResolvedFiles().get(this);
            if (!CommonUtils.isEmpty(driverFileInfos) && driverFileInfos.size() == 1) {
                DriverDescriptor.DriverFileInfo driverFileInfo = driverFileInfos.get(0);
                resolvedCache = resolveCacheDir().resolve(driverFileInfo.getFile().toString());
            } else {
                // need to correct driver initialization, otherwise, if at least one file was copied,
                // the driver configuration will be incorrect and other driver files will not be copied
                resolvedCache = resolveCacheDir().resolve(localFilePath);
            }
            if (Files.exists(resolvedCache)) {
                localFilePath = resolvedCache.toAbsolutePath().toString();
            }
        }

        Path platformFile = null;
        try {
            Path libraryFile = Path.of(localFilePath);
            if (Files.exists(libraryFile)) {
                return libraryFile;
            }

            // Try to get local file
            platformFile = detectLocalFile();
            if (platformFile != null && Files.exists(platformFile)) {
                // Relative file do not exists - use plain one
                return platformFile;
            }
        } catch (InvalidPathException e) {
            // ignore - bad local path
        }

        URL url = driver.getProviderDescriptor().getContributorBundle().getEntry(localFilePath);
        if (url == null) {
            // Find in external resources
            url = DataSourceProviderRegistry.getInstance().findResourceURL(localFilePath);
        }
        if (url != null) {
            try {
                url = FileLocator.toFileURL(url);
            } catch (IOException ex) {
                log.warn(ex);
            }
            if (url != null) {
                return Path.of(url.getFile());
            }
        } else {
            try {
                url = FileLocator.toFileURL(new URL(localFilePath));
                Path urlFile = RuntimeUtils.getLocalPathFromURL(url);
                if (Files.exists(urlFile)) {
                    return urlFile;
                }
            } catch (IOException ex) {
                // ignore
            }
        }

        return platformFile;
    }

    @Nullable
    @Override
    public Path getLocalFile(@NotNull DBRProgressMonitor monitor) {
        return getLocalFile();
    }

    private Path resolveCacheDir() {
        if (isUseOriginalJar()) {
            return DriverDescriptor.getProvidedDriversStorageFolder();
        }
        if (DBWorkbench.isDistributed() || isCustom()) {
            // we do not have any provided drivers in distributed mode
            // and custom drivers stored in the workspace
            return DriverDescriptor.getWorkspaceDriversStorageFolder();
        }

        return DriverDescriptor.getProvidedDriversStorageFolder();
    }

    @Nullable
    @Override
    public Collection<? extends DBPDriverLibrary> getDependencies(@NotNull DBRProgressMonitor monitor) throws IOException {
        return null;
    }

    protected Path detectLocalFile() {
        // Try to use relative path from installation dir
        String localPath = getLocalFilePath();
        Path file = null;
        try {
            file = RuntimeUtils.getLocalPathFromURL(Platform.getInstallLocation().getURL()).resolve(localPath);
        } catch (IOException e) {
            log.warn("Error getting platform location", e);
        }
        if (file == null || !Files.exists(file)) {
            // Use custom drivers path
            file = DriverDescriptor.getCustomDriversHome().resolve(localPath);
        }
        return file;
    }

    @NotNull
    public String getDisplayName() {
        return path;
    }

    @NotNull
    @Override
    public String getId() {
        return path;
    }

    @NotNull
    @Override
    public DBIcon getIcon() {
        Path localFile = getLocalFile();
        if (localFile != null && Files.isDirectory(localFile)) {
            return DBIcon.TREE_FOLDER_ADMIN;
        } else {
            switch (type) {
                case lib:
                    return DBIcon.LIBRARY;
                case jar:
                    return DBIcon.JAR;
                case license:
                    return DBIcon.TYPE_TEXT;
                default:
                    return DBIcon.TYPE_UNKNOWN;
            }
        }
    }

    /**
     * Use original jar files and ignore all user changes
     */
    public boolean isUseOriginalJar() {
        return useOriginalJar;
    }

    public void setUseOriginalJar(boolean useOriginalJar) {
        this.useOriginalJar = useOriginalJar;
    }
}
