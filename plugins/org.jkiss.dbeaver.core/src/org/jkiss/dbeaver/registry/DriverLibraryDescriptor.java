/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;

import java.io.File;

/**
 * DriverLibraryDescriptor
 */
public class DriverLibraryDescriptor
{
    static final Log log = LogFactory.getLog(DriverLibraryDescriptor.class);

    private DriverDescriptor driver;
    private String path;
    private String description;
    private boolean loaded = false;
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

    public boolean isLoaded()
    {
        return loaded;
    }

    public boolean isCustom()
    {
        return custom;
    }

    public void setCustom(boolean custom)
    {
        this.custom = custom;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    public File getLibraryFile()
    {
        String url = driver.getLibraryURL(path).toExternalForm();
        if (url.startsWith("file:/")) {
            url = url.substring(6);
        }
        return new File(url);
    }


}
