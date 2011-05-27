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
                if (localFile.exists()) {
                    final String libName = localFile.getName();
                    if (libName.equalsIgnoreCase(libname)) {
                        return localFile.getAbsolutePath();
                    }
                    int dotPos = libName.lastIndexOf('.');
                    if (dotPos != -1 && libName.substring(0, dotPos).equalsIgnoreCase(libname)) {
                        return localFile.getAbsolutePath();
                    }
                }
            }
        }
        return super.findLibrary(libname);
    }
}
