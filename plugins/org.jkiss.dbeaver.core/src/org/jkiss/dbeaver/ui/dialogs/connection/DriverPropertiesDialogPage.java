/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.CoreMessages;
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
        setTitle(CoreMessages.dialog_setting_connection_driver_properties_title);
        setDescription(CoreMessages.dialog_setting_connection_driver_properties_description);
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
                        monitor.beginTask("Loading driver properties", 1);
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
            properties.clear();
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
