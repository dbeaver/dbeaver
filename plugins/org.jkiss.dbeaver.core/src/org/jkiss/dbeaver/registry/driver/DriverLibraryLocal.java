/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
import org.jkiss.dbeaver.ui.UIIcon;

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

    @Override
    public boolean isDownloadable()
    {
        return false;
    }

    @Override
    public void resetVersion() {
        // do nothing
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
            url = driver.getProviderDescriptor().getRegistry().findResourceURL(localFilePath);
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
            return DBIcon.TREE_FOLDER;
        } else {
            switch (type) {
                case lib: return UIIcon.LIBRARY;
                case jar: return UIIcon.JAR;
                default: return DBIcon.TYPE_UNKNOWN;
            }
        }
    }

}
