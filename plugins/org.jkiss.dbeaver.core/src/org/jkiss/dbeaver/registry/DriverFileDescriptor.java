/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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

    public DBPDriverFileType getType()
    {
        return type;
    }

    public OSDescriptor getSystem()
    {
        return system;
    }

    public String getPath()
    {
        return path;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isCustom()
    {
        return custom;
    }

    public void setCustom(boolean custom)
    {
        this.custom = custom;
    }

    public String getExternalURL()
    {
        return externalURL;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

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
            file = new File(DBeaverCore.getInstance().getWorkspace().getRoot().getLocation().toFile(), path);
        }
        return file;
    }

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

    public boolean matchesCurrentPlatform()
    {
        return system == null || system.matches(DBeaverCore.getInstance().getLocalSystem());
    }
}
