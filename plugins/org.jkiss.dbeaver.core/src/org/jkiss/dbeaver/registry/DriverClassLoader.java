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

import org.jkiss.dbeaver.model.DBPDriverFileType;
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
            if (driverFile.getType() == DBPDriverFileType.lib && driverFile.matchesCurrentPlatform()) {
                final File localFile = driverFile.getFile();
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
