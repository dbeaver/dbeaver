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
package org.jkiss.dbeaver.ext.wmi;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.wmi.model.WMIDataSource;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.MonitorRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.wmi.service.WMIService;

public class WMIDataSourceProvider implements DBPDataSourceProvider {


    private boolean libLoaded = false;

    @Override
    public void init(@NotNull DBPApplication application)
    {
    }

    @Override
    public long getFeatures()
    {
        return FEATURE_SCHEMAS;
    }

    @Override
    public DBPPropertyDescriptor[] getConnectionProperties(
        DBRRunnableContext runnableContext,
        DBPDriver driver,
        DBPConnectionConfiguration connectionInfo) throws DBException
    {
        driver.validateFilesPresence(runnableContext);
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
    public DBPDataSource openDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBSDataSourceContainer container) throws DBException
    {
        if (!libLoaded) {
            DBPDriver driver = container.getDriver();
            driver.loadDriver(new MonitorRunnableContext(monitor));
            loadNativeLib(driver);
            libLoaded = true;
        }
        return new WMIDataSource(container);
    }

    private void loadNativeLib(DBPDriver driver) throws DBException
    {
        for (DBPDriverFile libFile : driver.getFiles()) {
            if (libFile.matchesCurrentPlatform() && libFile.getType() == DBPDriverFileType.lib) {
                try {
                    WMIService.linkNative(libFile.getFile().getAbsolutePath());
                } catch (UnsatisfiedLinkError e) {
                    throw new DBException("Can't load native library '" + libFile.getFile().getAbsolutePath() + "'", e);
                }
            }
        }
    }

}
