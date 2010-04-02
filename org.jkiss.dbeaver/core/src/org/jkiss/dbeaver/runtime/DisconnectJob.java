package org.jkiss.dbeaver.runtime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * DisconnectJob
 */
public class DisconnectJob extends DataSourceJob
{
    static Log log = LogFactory.getLog(DisconnectJob.class);

    public DisconnectJob(
        DBPDataSource dataSource)
    {
        super("Disconnect from " + dataSource.getContainer().getName(), null, dataSource);
    }

    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            getDataSource().close();

            return new Status(
                Status.OK,
                DBeaverCore.getInstance().getPluginID(),
                "Disconnected");
        }
        catch (Exception ex) {
            return new Status(
                Status.ERROR,
                DBeaverCore.getInstance().getPluginID(),
                "Error disconnecting from datasource '" + getDataSource().getContainer().getName() + "': " + ex.getMessage());
        }
    }

    protected void canceling()
    {
        getThread().interrupt();
    }

}