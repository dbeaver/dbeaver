package org.jkiss.dbeaver.runtime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.progress.IProgressConstants;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

/**
 * ConnectJob
 */
public class ConnectJob extends Job
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

    protected IStatus run(IProgressMonitor monitor)
    {
        monitor.beginTask("Open Datasource ...", 2);
        try {
            monitor.subTask("Connecting to Remote Database");
            dataSource = container.getDriver().getDataSourceProvider().openDataSource(container);
            monitor.worked(1);
            monitor.subTask("Initializing Datasource");
            dataSource.initialize();
            // Change connection properties
            monitor.done();

            return new Status(
                Status.OK,
                DBeaverCore.getInstance().getPluginID(),
                "Connected");
        }
        catch (Exception ex) {
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