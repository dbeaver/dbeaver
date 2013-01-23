/*
 * Copyright (C) 2010-2012 Serge Rieder
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * Abstract connection wizard
 */

public abstract class ConnectionWizard extends Wizard implements INewWizard
{
    static final Log log = LogFactory.getLog(ConnectionWizard.class);

    //protected final IProject project;
    protected final DataSourceRegistry dataSourceRegistry;

    protected ConnectionWizard(DataSourceRegistry dataSourceRegistry) {
        setNeedsProgressMonitor(true);
        this.dataSourceRegistry = dataSourceRegistry;
    }

    public DataSourceDescriptor getDataSourceDescriptor()
    {
        return null;
    }
    
    @Override
    public boolean performFinish()
    {
        if (getPageSettings() != null) {
            getPageSettings().saveSettings();
        }
        return true;
    }

    abstract DriverDescriptor getSelectedDriver();

    public abstract ConnectionPageSettings getPageSettings();

    public void testConnection(final DBPConnectionInfo connectionInfo)
    {
        DBRRunnableWithProgress op = new DBRRunnableWithProgress()
        {
            @Override
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                monitor.beginTask(CoreMessages.dialog_connection_wizard_start_connection_monitor_start, 3);
                Thread.currentThread().setName(CoreMessages.dialog_connection_wizard_start_connection_monitor_thread);

                DriverDescriptor driver = getSelectedDriver();
                DBPDataSourceProvider provider;
                try {
                    provider = driver.getDataSourceProvider();
                }
                catch (DBException ex) {
                    throw new InvocationTargetException(ex);
                }
                DataSourceDescriptor container = new DataSourceDescriptor(dataSourceRegistry, "test", driver, connectionInfo);
                try {
                    container.setName(connectionInfo.getUrl());
                    monitor.worked(1);
                    container.connect(monitor, false);
                    monitor.worked(1);
                    DBPDataSource dataSource = container.getDataSource();
                    if (dataSource == null) {
                        throw new InvocationTargetException(
                            new DBException("Internal error: null datasource returned from provider " + provider));
                    } else {
                        monitor.subTask(CoreMessages.dialog_connection_wizard_start_connection_monitor_subtask_test);
                        try {
                            // test connection
                            dataSource.initialize(monitor);
                            monitor.done();
                        }
                        finally {
                            try {
                                monitor.subTask(CoreMessages.dialog_connection_wizard_start_connection_monitor_close);
                                container.disconnect(monitor, false);
                            } catch (DBException e) {
                                // ignore it
                                log.error(e);
                            }
                        }
                    }
                    monitor.subTask(CoreMessages.dialog_connection_wizard_start_connection_monitor_success);
                }
                catch (DBException ex) {
                    throw new InvocationTargetException(ex);
                }
                finally {
                    container.dispose();
                }
            }
        };

        try {
            long startTime = System.currentTimeMillis();
            RuntimeUtils.run(getContainer(), true, true, op);
            long connectTime = (System.currentTimeMillis() - startTime);
            MessageDialog.openInformation(
                getShell(),
                CoreMessages.dialog_connection_wizard_start_connection_monitor_success,
                NLS.bind(CoreMessages.dialog_connection_wizard_start_connection_monitor_connected, connectTime));
        }
        catch (InterruptedException ex) {
            UIUtils.showErrorDialog(
                getShell(),
                CoreMessages.dialog_connection_wizard_start_dialog_interrupted_title,
                CoreMessages.dialog_connection_wizard_start_dialog_interrupted_message);
        }
        catch (InvocationTargetException ex) {
            UIUtils.showErrorDialog(
                getShell(),
                CoreMessages.dialog_connection_wizard_start_dialog_error_title,
                CoreMessages.dialog_connection_wizard_start_dialog_error_message,
                ex.getTargetException());
        }
    }

}