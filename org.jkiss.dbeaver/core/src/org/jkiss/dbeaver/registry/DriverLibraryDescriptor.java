/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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
    private boolean loaded = false;
    private boolean custom;
    private boolean disabled;

    DriverLibraryDescriptor(DriverDescriptor driver, String path)
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
            try {
                return new File(url.toURI());
            } catch (URISyntaxException ex) {
                log.warn(ex);
            }
        }
        return new File(path);
    }

    public URL getLibraryURL()
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
        if (url == null) {
            File libraryFile = new File(path);
            if (!libraryFile.exists()) {
                return null;
            }
            try {
                url = libraryFile.toURI().toURL();
            }
            catch (MalformedURLException ex) {
                log.warn(ex);
            }
        }
        return url;
    }

}
