/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * DriverClassLoader
 */
public class DriverClassLoader extends URLClassLoader
{
    private final DriverDescriptor driver;

    public DriverClassLoader(DriverDescriptor driver, URL[] urls, ClassLoader parent)
    {
        super(urls, parent);
        this.driver = driver;
    }

    @Override
    protected String findLibrary(String libname)
    {
        for (DriverFileDescriptor driverFile : driver.getFiles()) {
            if (driverFile.getType() == DriverFileType.lib && driverFile.matchesCurrentPlatform()) {
                final File localFile = driverFile.getLocalFile();
                if (localFile.exists() && localFile.getName().equalsIgnoreCase(libname)) {
                    return localFile.getAbsolutePath();
                }
            }
        }
        return super.findLibrary(libname);
    }
}
