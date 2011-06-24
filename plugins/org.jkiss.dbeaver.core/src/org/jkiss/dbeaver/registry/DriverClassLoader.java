/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

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
        String nativeName = System.mapLibraryName(libname);
        for (DriverFileDescriptor driverFile : driver.getFiles()) {
            if (driverFile.getType() == DriverFileType.lib && driverFile.matchesCurrentPlatform()) {
                final File localFile = driverFile.getLocalFile();
                if (localFile.exists()) {
                    final String fileName = localFile.getName();
                    if (fileName.equalsIgnoreCase(nativeName)) {
                        return localFile.getAbsolutePath();
                    }
//                    int dotPos = fileName.lastIndexOf('.');
//                    if (dotPos != -1 && fileName.substring(0, dotPos).equalsIgnoreCase(libname)) {
//                        return localFile.getAbsolutePath();
//                    }
                }
            }
        }
        final Collection<String> pathList = driver.getOrderedPathList();
        if (!CommonUtils.isEmpty(pathList)) {
            for (String path : pathList) {
                File localFile = new File(path, nativeName);
                if (localFile.exists()) {
                    return localFile.getAbsolutePath();
                }
            }
        }
        return super.findLibrary(libname);
    }
}
