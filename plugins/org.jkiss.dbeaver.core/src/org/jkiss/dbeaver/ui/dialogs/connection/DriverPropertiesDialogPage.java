/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.controls.ConnectionPropertiesControl;
import org.jkiss.dbeaver.ui.properties.PropertySourceCustom;

import java.lang.reflect.InvocationTargetException;

/**
 * DriverPropertiesDialogPage
 */
public class DriverPropertiesDialogPage extends ConnectionPageAbstract
{

    private ConnectionPageAbstract hostPage;
    private ConnectionPropertiesControl propsControl;
    private PropertySourceCustom propertySource;

    private DBPConnectionInfo prevConnectionInfo = null;

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
        DataSourceDescriptor activeDataSource = site.getActiveDataSource();
        if (prevConnectionInfo == activeDataSource.getConnectionInfo()) {
            return;
        }
        DBPConnectionInfo tmpConnectionInfo = new DBPConnectionInfo();
        DataSourceDescriptor tempDataSource = new DataSourceDescriptor(
            site.getDataSourceRegistry(),
            activeDataSource.getId(),
            activeDataSource.getDriver(),
            tmpConnectionInfo);
        try {
            hostPage.saveSettings(tempDataSource);
            tmpConnectionInfo.getProperties().putAll(activeDataSource.getConnectionInfo().getProperties());
            propertySource = propsControl.makeProperties(
                site.getRunnableContext(),
                site.getDriver(),
                tmpConnectionInfo);
            propsControl.loadProperties(propertySource);
            prevConnectionInfo = activeDataSource.getConnectionInfo();
        } finally {
            tempDataSource.dispose();
        }
    }

    @Override
    public void saveSettings(DataSourceDescriptor dataSource)
    {
        if (propertySource != null) {
            dataSource.getConnectionInfo().getProperties().putAll(propertySource.getProperties());
        }
    }

    @Override
    public void createControl(Composite parent)
    {
        propsControl = new ConnectionPropertiesControl(parent, SWT.NONE);
        setControl(propsControl.getControl());
    }

}
