/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * DriverLibraryDescriptor
 */
public class DriverLibraryRepository extends DriverLibraryLocal
{
    public static final String PATH_PREFIX = "repo:/";

    public DriverLibraryRepository(DriverDescriptor driver, FileType type, String path) {
        super(driver, type, path);
    }

    public DriverLibraryRepository(DriverDescriptor driver, IConfigurationElement config) {
        super(driver, config);
    }

    private DriverLibraryRepository(DriverDescriptor driver, DriverLibraryRepository copyFrom) {
        super(driver, copyFrom);
    }

    @Override
    public DBPDriverLibrary copyLibrary(DriverDescriptor driverDescriptor) {
        return new DriverLibraryRepository(driver, this);
    }

    @Override
    public boolean isDownloadable()
    {
        return true;
    }

    @Override
    protected String getLocalFilePath() {
        return path.substring(PATH_PREFIX.length());
    }

    @Nullable
    @Override
    public String getExternalURL(DBRProgressMonitor monitor) {
        String localPath = getLocalFilePath();
        String primarySource = DriverDescriptor.getDriversPrimarySource();
        if (!primarySource.endsWith("/") && !localPath.startsWith("/")) {
            primarySource += '/';
        }
        return primarySource + localPath;
    }



}
