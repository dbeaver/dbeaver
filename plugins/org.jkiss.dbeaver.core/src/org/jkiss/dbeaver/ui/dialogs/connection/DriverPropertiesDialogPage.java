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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;

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

            // Load properties in job
            AbstractJob propsLoadJob = new AbstractJob("Refresh driver properties") {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    propertySource = propsControl.makeProperties(
                        site.getRunnableContext(),
                        site.getDriver(),
                        tmpConnectionInfo);
                    return Status.OK_STATUS;
                }
            };
            propsLoadJob.schedule();
            propsLoadJob.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    DBeaverUI.syncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (propsControl.getControl() == null || propsControl.getControl().isDisposed()) {
                                return;
                            }
                            propsControl.loadProperties(propertySource);
                            prevConnectionInfo = activeDataSource.getConnectionConfiguration();

                            tempDataSource.dispose();
                        }
                    });
                }
            });
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
