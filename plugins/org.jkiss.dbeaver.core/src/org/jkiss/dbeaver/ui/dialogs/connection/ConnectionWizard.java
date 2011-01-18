/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
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
 * This is a sample new wizard.
 */

public abstract class ConnectionWizard extends Wizard implements INewWizard
{

    protected final IProject project;
    protected final DataSourceRegistry dataSourceRegistry;

    protected ConnectionWizard(IProject project) {
        setNeedsProgressMonitor(true);
        this.project = project;
        this.dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(project);
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

    abstract ConnectionPageFinal getPageFinal();

    abstract DriverDescriptor getSelectedDriver();

    public abstract ConnectionPageSettings getPageSettings();

    public void testConnection(final DBPConnectionInfo connectionInfo)
    {
        DBRRunnableWithProgress op = new DBRRunnableWithProgress()
        {
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                monitor.beginTask("Obtain connection", 3);
                Thread.currentThread().setName("Test datasource connection");

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
                        monitor.subTask("Test connection");
                        try {
                            // test connection
                            dataSource.invalidateConnection(monitor);
                            monitor.done();
                        }
                        finally {
                            monitor.subTask("Close connection");
                            dataSource.close(monitor);
                        }
                    }
                    monitor.subTask("Success");
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
            RuntimeUtils.run(getContainer(), true, true, op);

            MessageDialog.openInformation(
                getShell(), "Success", "Successfully connected!");
        }
        catch (InterruptedException ex) {
            UIUtils.showErrorDialog(
                getShell(),
                "Interrupted",
                "Test interrupted");
        }
        catch (InvocationTargetException ex) {
            UIUtils.showErrorDialog(
                getShell(),
                "Connection error",
                "Database connectivity error",
                ex.getTargetException());
        }
    }

}