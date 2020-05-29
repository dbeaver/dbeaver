/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

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
                File localFile = driverFile.getLocalFile();
                if (localFile == null) {
                    // Check library files cache
                    List<DriverDescriptor.DriverFileInfo> cachedFiles = driver.getCachedFiles(driverFile);
                    if (!CommonUtils.isEmpty(cachedFiles)) {
                        for (DriverDescriptor.DriverFileInfo fileInfo : cachedFiles) {
                            if (fileInfo.getFile() != null && fileInfo.getFile().getName().equalsIgnoreCase(nativeName)) {
                                return fileInfo.getFile().getAbsolutePath();
                            }
                        }
                    }
                }
                if (localFile != null && localFile.exists()) {
                    if (localFile.isDirectory()) {
                        localFile = new File(localFile, nativeName);
                    }
                    if (localFile.getName().equalsIgnoreCase(nativeName)) {
                        return localFile.getAbsolutePath();
                    }
                }
            }
        }
        return super.findLibrary(libname);
    }
}
