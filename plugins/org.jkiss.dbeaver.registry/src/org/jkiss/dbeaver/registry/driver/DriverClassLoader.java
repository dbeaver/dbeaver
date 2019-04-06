/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.registry.driver;

import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;

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
        String nativeName = System.mapLibraryName(libname);
        for (DBPDriverLibrary driverFile : driver.getDriverLibraries()) {
            if (driverFile.getType() == DBPDriverLibrary.FileType.lib && driverFile.matchesCurrentPlatform()) {
                final File localFile = driverFile.getLocalFile();
                if (localFile != null && localFile.exists()) {
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
        return super.findLibrary(libname);
    }
}
