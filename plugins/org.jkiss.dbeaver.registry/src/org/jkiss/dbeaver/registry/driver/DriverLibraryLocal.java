/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

/**
 * DriverLibraryLocal
 */
public class DriverLibraryLocal extends DriverLibraryAbstract
{
    private static final Log log = Log.getLog(DriverLibraryLocal.class);

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
    public boolean isDownloadable()
    {
        return false;
    }

    @Override
    public void resetVersion() {
        // do nothing
    }

    @Override
    public boolean isSecureDownload(DBRProgressMonitor monitor) {
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
    public File getLocalFile()
    {
        // Try to use direct path
        String localFilePath = this.getLocalFilePath();

        File libraryFile = new File(localFilePath);
        if (libraryFile.exists()) {
            return libraryFile;
        }

        // Try to get local file
        File platformFile = detectLocalFile();
        if (platformFile != null && platformFile.exists()) {
            // Relative file do not exists - use plain one
            return platformFile;
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
                return new File(url.getFile());
            }
        } else {
            try {
                url = FileLocator.toFileURL(new URL(localFilePath));
                File urlFile = new File(url.getFile());
                if (urlFile.exists()) {
                    platformFile = urlFile;
                }
            }
            catch (IOException ex) {
                // ignore
            }
        }

        // Nothing fits - just return plain url
        return platformFile;
    }

    @Nullable
    @Override
    public Collection<? extends DBPDriverLibrary> getDependencies(@NotNull DBRProgressMonitor monitor) throws IOException {
        return null;
    }

    protected File detectLocalFile()
    {
        String localPath = getLocalFilePath();

        // Try to use relative path from installation dir
        File file = new File(new File(Platform.getInstallLocation().getURL().getFile()), localPath);
        if (!file.exists()) {
            // Use custom drivers path
            file = new File(DriverDescriptor.getCustomDriversHome(), localPath);
        }
        return file;
    }

    @NotNull
    public String getDisplayName() {
        return path;
    }

    @Override
    public String getId() {
        return path;
    }

    @NotNull
    @Override
    public DBIcon getIcon() {
        File localFile = getLocalFile();
        if (localFile != null && localFile.isDirectory()) {
            return DBIcon.TREE_FOLDER_ADMIN;
        } else {
            switch (type) {
                case lib: return DBIcon.LIBRARY;
                case jar: return DBIcon.JAR;
                case license: return DBIcon.TYPE_TEXT;
                default: return DBIcon.TYPE_UNKNOWN;
            }
        }
    }

}
