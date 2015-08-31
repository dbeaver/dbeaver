/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.registry;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDriverFile;
import org.jkiss.dbeaver.model.DBPDriverFileType;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.registry.maven.MavenArtifact;
import org.jkiss.dbeaver.registry.maven.MavenRegistry;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * DriverFileDescriptor
 */
public class DriverFileDescriptor implements DBPDriverFile
{
    static final Log log = Log.getLog(DriverFileDescriptor.class);

    public static final String FILE_SOURCE_MAVEN = "maven:/";
    public static final String FILE_SOURCE_PLATFORM = "platform:/";

    private final DriverDescriptor driver;
    private final DBPDriverFileType type;
    private final OSDescriptor system;
    private String path;
    private String fileExtension;
    private String description;
    private boolean custom;
    private boolean disabled;

    public DriverFileDescriptor(DriverDescriptor driver, DBPDriverFileType type, String path)
    {
        this.driver = driver;
        this.type = type;
        this.system = DBeaverCore.getInstance().getLocalSystem();
        this.path = path;
        this.custom = true;
    }

    DriverFileDescriptor(DriverDescriptor driver, IConfigurationElement config)
    {
        this.driver = driver;
        this.type = DBPDriverFileType.valueOf(config.getAttribute(RegistryConstants.ATTR_TYPE));

        String osName = config.getAttribute(RegistryConstants.ATTR_OS);
        this.system = osName == null ? null : new OSDescriptor(
            osName,
            config.getAttribute(RegistryConstants.ATTR_ARCH));
        this.path = config.getAttribute(RegistryConstants.ATTR_PATH);
        this.fileExtension = config.getAttribute(RegistryConstants.ATTR_EXTENSION);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.custom = false;
    }

    public DriverDescriptor getDriver()
    {
        return driver;
    }

    @Override
    public DBPDriverFileType getType()
    {
        return type;
    }

    @Override
    public OSDescriptor getSystem()
    {
        return system;
    }

    @Override
    public String getPath()
    {
        return path;
    }

    @Override
    public String getFileType() {
        return null;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public boolean isCustom()
    {
        return custom;
    }

    public void setCustom(boolean custom)
    {
        this.custom = custom;
    }

    @Override
    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    @Override
    public boolean isLocal()
    {
        return path.startsWith(DriverDescriptor.DRIVERS_FOLDER) || path.startsWith(FILE_SOURCE_MAVEN);
    }

    private File getLocalFile()
    {
        if (path.startsWith(FILE_SOURCE_MAVEN)) {
            MavenArtifact artifact = MavenRegistry.getInstance().findArtifact(path);
            return new File(DriverDescriptor.getCustomDriversHome(), getMavenArtifactFileName());
        }
        // Try to use relative path from installation dir
        File file = new File(new File(Platform.getInstallLocation().getURL().getFile()), path);
        if (!file.exists()) {
            // Use custom drivers path
            file = new File(DriverDescriptor.getCustomDriversHome(), path);
        }
        return file;
    }

    public String getExternalURL() {
        if (path.startsWith(FILE_SOURCE_PLATFORM)) {
            return path;
        } else if (path.startsWith(FILE_SOURCE_MAVEN)) {
            String artifactName = path.substring(FILE_SOURCE_MAVEN.length());
            return "http://unknown/" + artifactName;
        } else {
            String primarySource = DriverDescriptor.getDriversPrimarySource();
            if (!primarySource.endsWith("/") && !path.startsWith("/")) {
                primarySource += '/';
            }
            return primarySource + path;
        }
    }


    @Override
    public File getFile()
    {
        if (path.startsWith(FILE_SOURCE_PLATFORM)) {
            try {
                return RuntimeUtils.getPlatformFile(path);
            } catch (IOException e) {
                log.warn("Bad file URL: " + path, e);
            }
        }
        // Try to use direct path
        File libraryFile = new File(path);
        if (libraryFile.exists()) {
            return libraryFile;
        }
        // Try to get local file
        File platformFile = getLocalFile();
        if (platformFile.exists()) {
            // Relative file do not exists - use plain one
            return platformFile;
        }

        // Try to get from plugin's bundle/from external resources
        {
            URL url = driver.getProviderDescriptor().getContributorBundle().getEntry(path);
            if (url == null) {
                // Find in external resources
                url = driver.getProviderDescriptor().getRegistry().findResourceURL(path);
            }
            if (url != null) {
                try {
                    url = FileLocator.toFileURL(url);
                }
                catch (IOException ex) {
                    log.warn(ex);
                }
            }
            if (url != null) {
                return new File(url.getFile());
            }
        }

        // Nothing fits - just return plain url
        return platformFile;
    }

    @Override
    public boolean matchesCurrentPlatform()
    {
        return system == null || system.matches(DBeaverCore.getInstance().getLocalSystem());
    }

    private String getMavenArtifactFileName() {
        String artifactName = path.substring(FILE_SOURCE_MAVEN.length());
        String ext = fileExtension;
        String[] artifactNameParts = artifactName.split(":");
        if (artifactNameParts.length != 3) {
            log.warn("Bad Maven artifact reference: " + artifactName);
            return artifactName.replace(':', '.');
        }
        String artifactFileName = DriverDescriptor.DRIVERS_FOLDER + "/" + artifactNameParts[1] + "/" +
            artifactNameParts[0] + "." + artifactNameParts[1] + "." + artifactNameParts[2];
        if (CommonUtils.isEmpty(ext)) {
            switch (type) {
                case lib:
                    return System.mapLibraryName(artifactFileName);
                case executable:
                    if (RuntimeUtils.isPlatformWindows()) {
                        ext = "exe";
                    } else {
                        return artifactFileName;
                    }
                    break;
                case jar:
                    ext = "jar";
                    break;
                case license:
                    ext = "txt";
                    break;
            }
        }
        return artifactFileName + '.' + ext;
    }

    @Override
    public String toString() {
        return path;
    }

}
