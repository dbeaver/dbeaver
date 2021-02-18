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
package org.jkiss.dbeaver.ext.wmi;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.wmi.model.WMIDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.wmi.service.WMIService;

import java.io.File;

public class WMIDataSourceProvider implements DBPDataSourceProvider {


    private boolean libLoaded = false;

    @Override
    public void init(@NotNull DBPPlatform platform)
    {
    }

    @Override
    public long getFeatures()
    {
        return FEATURE_SCHEMAS;
    }

    @Override
    public DBPPropertyDescriptor[] getConnectionProperties(
        DBRProgressMonitor monitor,
        DBPDriver driver,
        DBPConnectionConfiguration connectionInfo) throws DBException
    {
        return null;
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo)
    {
        return
            "wmi://" + connectionInfo.getServerName() +
                "/" + connectionInfo.getHostName() +
                "/" + connectionInfo.getDatabaseName();
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container) throws DBException
    {
        if (!libLoaded) {
            DBPDriver driver = container.getDriver();
            driver.loadDriver(monitor);
            loadNativeLib(driver);
            libLoaded = true;
        }
        return new WMIDataSource(container);
    }

    private void loadNativeLib(DBPDriver driver) throws DBException
    {
        for (DBPDriverLibrary libFile : driver.getDriverLibraries()) {
            if (libFile.matchesCurrentPlatform() && libFile.getType() == DBPDriverLibrary.FileType.lib) {
                File localFile = libFile.getLocalFile();
                if (localFile != null) {
                    try {
                        WMIService.linkNative(localFile.getAbsolutePath());
                    } catch (UnsatisfiedLinkError e) {
                        throw new DBException("Can't load native library '" + localFile.getAbsolutePath() + "'", e);
                    }
                }
            }
        }
    }

}
