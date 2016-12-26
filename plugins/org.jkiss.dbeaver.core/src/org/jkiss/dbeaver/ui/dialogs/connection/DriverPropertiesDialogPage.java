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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

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
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        // Set props model
        if (visible && propsControl != null) {
            final DBPDataSourceContainer activeDataSource = site.getActiveDataSource();
            if (prevConnectionInfo == activeDataSource.getConnectionConfiguration()) {
                return;
            }

            final DBPConnectionConfiguration tmpConnectionInfo = new DBPConnectionConfiguration();
            final DataSourceDescriptor tempDataSource = new DataSourceDescriptor(
                site.getDataSourceRegistry(),
                activeDataSource.getId(),
                (DriverDescriptor) activeDataSource.getDriver(),
                tmpConnectionInfo);

            hostPage.saveSettings(tempDataSource);
            tmpConnectionInfo.getProperties().putAll(activeDataSource.getConnectionConfiguration().getProperties());

            try {
                getSite().getRunnableContext().run(true, true, new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        monitor.beginTask("Load driver properties", 1);
                        try {
                            propertySource = propsControl.makeProperties(
                                monitor,
                                site.getDriver(),
                                tmpConnectionInfo);
                        } finally {
                            monitor.done();
                        }
                    }
                });
            } catch (InvocationTargetException e) {
                setErrorMessage(e.getTargetException().getMessage());
            } catch (InterruptedException e) {
                // ignore
            }
            propsControl.loadProperties(propertySource);
            prevConnectionInfo = activeDataSource.getConnectionConfiguration();

            tempDataSource.dispose();
        }
    }

    @Override
    public void loadSettings()
    {
        // Do nothing
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        if (propertySource != null) {
            final Map<String, String> properties = dataSource.getConnectionConfiguration().getProperties();
            for (Map.Entry<Object, Object> entry : propertySource.getProperties().entrySet()) {
                properties.put(CommonUtils.toString(entry.getKey()), CommonUtils.toString(entry.getValue()));
            }
        }
    }

    @Override
    public void createControl(Composite parent)
    {
        propsControl = new ConnectionPropertiesControl(parent, SWT.NONE);
        setControl(propsControl.getControl());
    }

}
