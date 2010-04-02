package org.jkiss.dbeaver.runtime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.IProgressConstants;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;

/**
 * ConnectJob
 */
public class ConnectJob extends AbstractJob
{
    static Log log = LogFactory.getLog(ConnectJob.class);

    private DataSourceDescriptor container;
    private DBPDataSource dataSource;

    public ConnectJob(
        DataSourceDescriptor container)
    {
        super("Connect to " + container.getName());
        setUser(true);
        //setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
        setProperty(IProgressConstants.KEEPONE_PROPERTY, Boolean.TRUE);
        this.container = container;
    }

    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        monitor.beginTask("Open Datasource ...", 2);
        try {
            monitor.subTask("Connecting to Remote Database");
            dataSource = container.getDriver().getDataSourceProvider().openDataSource(container);
            monitor.worked(1);
            monitor.beginTask("Initializing Datasource", 10);
            dataSource.initialize(monitor);
            // Change connection properties
            monitor.done();

            return new Status(
                Status.OK,
                DBeaverCore.getInstance().getPluginID(),
                "Connected");
        }
        catch (Throwable ex) {
            return new Status(
                Status.ERROR,
                DBeaverCore.getInstance().getPluginID(),
                "Error connecting to datasource '" + container.getName() + "': " + ex.getMessage());
        }
    }

    public boolean belongsTo(Object family)
    {
        return container == family;
    }

    protected void canceling()
    {
        getThread().interrupt();
    }

}