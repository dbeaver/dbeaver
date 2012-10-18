/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDriverFile;
import org.jkiss.dbeaver.model.DBPDriverFileType;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * DriverFileDescriptor
 */
public class DriverFileDescriptor implements DBPDriverFile
{
    static final Log log = LogFactory.getLog(DriverFileDescriptor.class);

    private final DriverDescriptor driver;
    private final DBPDriverFileType type;
    private final OSDescriptor system;
    private String path;
    private String description;
    private String externalURL;
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
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.externalURL = config.getAttribute(RegistryConstants.ATTR_URL);
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
    public String getExternalURL()
    {
        return externalURL;
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
        return path.startsWith(DriverDescriptor.DRIVERS_FOLDER);
    }

    File getLocalFile()
    {
        // Try to use relative path from installation dir
        File file = new File(new File(Platform.getInstallLocation().getURL().getFile()), path);
        if (!file.exists()) {
            // Try to use relative path from workspace dir
            // [compatibility with 1.x]
            file = new File(DBeaverCore.getInstance().getWorkspace().getRoot().getLocation().toFile(), path);
            if (!file.exists()) {
                // USe custom drivers path
                file = new File(driver.getCustomDriversHome(), path);
            }
        }
        return file;
    }

    @Override
    public File getFile()
    {
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

        // Try to get from plugin's bundle
        {
            URL url = driver.getProviderDescriptor().getContributorBundle().getEntry(path);
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
        return libraryFile;
    }

    @Override
    public boolean matchesCurrentPlatform()
    {
        return system == null || system.matches(DBeaverCore.getInstance().getLocalSystem());
    }
}
