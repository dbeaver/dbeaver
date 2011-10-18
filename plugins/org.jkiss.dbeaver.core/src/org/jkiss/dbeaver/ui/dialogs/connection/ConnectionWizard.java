/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

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
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                monitor.beginTask(CoreMessages.dialog_connection_wizard_start_connection_monitor_start, 3);
                Thread.currentThread().setName(CoreMessages.dialog_connection_wizard_start_connection_monitor_thread);

                DriverDescriptor driver = getSelectedDriver();
                DBPDataSourceProvider provider;
                try {
                    provider = driver.getProviderDescriptor().getInstance();
                }
                catch (DBException ex) {
                    throw new InvocationTargetException(ex);
                }
                DataSourceDescriptor container = new DataSourceDescriptor(dataSourceRegistry, "test", driver, connectionInfo);
                try {
                    monitor.worked(1);
                    DBPDataSource dataSource = provider.openDataSource(monitor, container);
                    monitor.worked(1);
                    if (dataSource == null) {
                        throw new InvocationTargetException(
                            new DBException("Internal error: null datasource returned from provider " + provider));
                    } else {
                        monitor.subTask(CoreMessages.dialog_connection_wizard_start_connection_monitor_subtask_test);
                        try {
                            // test connection
                            dataSource.invalidateConnection(monitor);
                            monitor.done();
                        }
                        finally {
                            monitor.subTask(CoreMessages.dialog_connection_wizard_start_connection_monitor_close);
                            dataSource.close(monitor);
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