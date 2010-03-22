package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.DBeaverUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * This is a sample new wizard.
 */

public abstract class ConnectionWizard<CONTAINER extends IWizardContainer>  extends Wizard implements INewWizard
{

    public CONTAINER getContainer()
    {
        return (CONTAINER) super.getContainer();
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
        IRunnableWithProgress op = new IRunnableWithProgress()
        {
            public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                monitor.beginTask("Obtain connection", 3);

                DriverDescriptor driver = getSelectedDriver();
                DBPDataSourceProvider provider;
                try {
                    provider = driver.getProviderDescriptor().getInstance();
                }
                catch (DBException ex) {
                    throw new InvocationTargetException(ex);
                }
                DataSourceDescriptor container = new DataSourceDescriptor(driver, connectionInfo);
                try {
                    monitor.worked(1);
                    DBPDataSource dataSource = provider.openDataSource(container);
                    monitor.worked(1);
                    if (dataSource == null) {
                        throw new InvocationTargetException(
                            new DBException("Internal error: null datasource returned from provider " + provider));
                    } else {
                        monitor.setTaskName("Test connection");
                        try {
                            // test connection
                            dataSource.checkConnection();
                            monitor.done();
                        }
                        finally {
                            monitor.setTaskName("Close connection");
                            dataSource.close();
                        }
                    }
                    monitor.setTaskName("Success");
                }
                catch (DBException ex) {
                    throw new InvocationTargetException(ex);
                }
            }
        };

        try {
            getContainer().run(true, true, op);

            MessageDialog.openInformation(
                getShell(), "Success", "Successfully connected!");
        }
        catch (InterruptedException ex) {
            DBeaverUtils.showErrorDialog(
                getShell(),
                "Interrupted",
                "Test interrupted");
        }
        catch (InvocationTargetException ex) {
            DBeaverUtils.showErrorDialog(
                getShell(),
                "Connection error",
                "Database connectivity error",
                ex.getTargetException());
        }
    }

    public void changePage(Object currentPage, Object targetPage)
    {
        if (currentPage instanceof ConnectionPageSettings) {
            ((ConnectionPageSettings) currentPage).deactivate();
        }

        if (targetPage instanceof ConnectionPageFinal) {
            ((ConnectionPageFinal) targetPage).activate();
        }
        if (targetPage instanceof ConnectionPageSettings) {
            ((ConnectionPageSettings) targetPage).activate();
        }
    }

/*
    private class CancelTask extends Thread
    {
        private Thread targetThread;
        private IProgressMonitor monitor;
        private boolean isStopped = false;

        private CancelTask(Thread targetThread, IProgressMonitor monitor)
        {
            this.targetThread = targetThread;
            this.monitor = monitor;
        }

        public void setStopped(boolean stopped)
        {
            isStopped = stopped;
        }

        public void run()
        {
            while (!isStopped) {
                if (monitor.isCanceled()) {
                    targetThread.interrupt();
                    break;
                }
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException ex) {
                    break;
                }
            }
        }
    }
*/
}