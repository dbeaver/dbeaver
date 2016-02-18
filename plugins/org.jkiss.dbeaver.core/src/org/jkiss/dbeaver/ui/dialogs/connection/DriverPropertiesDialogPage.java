/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;

import java.lang.reflect.InvocationTargetException;

/**
 * DriverPropertiesDialogPage
 */
public class DriverPropertiesDialogPage extends ConnectionPageAbstract
{

    private ConnectionPageAbstract hostPage;
    private ConnectionPropertiesControl propsControl;
    private PropertySourceCustom propertySource;

    private DBPConnectionConfiguration prevConnectionInfo = null;

    public DriverPropertiesDialogPage(ConnectionPageAbstract hostPage)
    {
        this.hostPage = hostPage;
        setTitle("Driver properties");
        setDescription("JDBC driver properties");
    }

    @Override
    public boolean isComplete()
    {
        return true;
    }

    @Override
    public void loadSettings()
    {
        // Set props model
        if (propsControl != null) {
            // Load properties in job
            DBeaverUI.runUIJob("Refresh driver properties", 250, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    refreshDriverProperties();
                }
            });
        }
    }

    protected void refreshDriverProperties()
    {
        DBPDataSourceContainer activeDataSource = site.getActiveDataSource();
        if (prevConnectionInfo == activeDataSource.getConnectionConfiguration()) {
            return;
        }
        DBPConnectionConfiguration tmpConnectionInfo = new DBPConnectionConfiguration();
        DataSourceDescriptor tempDataSource = new DataSourceDescriptor(
            site.getDataSourceRegistry(),
            activeDataSource.getId(),
            (DriverDescriptor) activeDataSource.getDriver(),
            tmpConnectionInfo);
        try {
            hostPage.saveSettings(tempDataSource);
            tmpConnectionInfo.getProperties().putAll(activeDataSource.getConnectionConfiguration().getProperties());
            propertySource = propsControl.makeProperties(
                site.getRunnableContext(),
                site.getDriver(),
                tmpConnectionInfo);
            propsControl.loadProperties(propertySource);
            prevConnectionInfo = activeDataSource.getConnectionConfiguration();
        } finally {
            tempDataSource.dispose();
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        if (propertySource != null) {
            dataSource.getConnectionConfiguration().getProperties().putAll(propertySource.getProperties());
        }
    }

    @Override
    public void createControl(Composite parent)
    {
        propsControl = new ConnectionPropertiesControl(parent, SWT.NONE);
        setControl(propsControl.getControl());
    }

}
