/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * DriverLibraryDescriptor
 */
public class DriverLibraryDescriptor
{
    static final Log log = LogFactory.getLog(DriverLibraryDescriptor.class);

    private DriverDescriptor driver;
    private String path;
    private String description;
    private String externalURL;
    private boolean custom;
    private boolean disabled;

    public DriverLibraryDescriptor(DriverDescriptor driver, String path)
    {
        this.driver = driver;
        this.path = path;
        this.custom = true;
    }

    DriverLibraryDescriptor(DriverDescriptor driver, IConfigurationElement config)
    {
        this.driver = driver;
        this.path = config.getAttribute("path");
        this.description = config.getAttribute("description");
        this.externalURL = config.getAttribute("url");
        this.custom = false;
    }

    public DriverDescriptor getDriver()
    {
        return driver;
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

    File getLocalFile()
    {
        return new File(new File(Platform.getInstallLocation().getURL().getFile()), path);
    }

    public File getLibraryFile()
    {
        // Try to use direct path
        File libraryFile = new File(path);
        if (libraryFile.exists()) {
            return libraryFile;
        }
        // Try to use relative path
        File installLocation = new File(Platform.getInstallLocation().getURL().getFile());
        File platformFile = new File(installLocation, path);
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

    public boolean isLocal()
    {
        return path.startsWith("drivers");
    }
}
