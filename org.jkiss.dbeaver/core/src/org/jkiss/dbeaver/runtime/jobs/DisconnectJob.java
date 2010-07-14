/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.DBeaverUtils;

/**
 * DisconnectJob
 */
public class DisconnectJob extends DataSourceJob
{
    static final Log log = LogFactory.getLog(DisconnectJob.class);

    public DisconnectJob(
        DBPDataSource dataSource)
    {
        super("Disconnect from " + dataSource.getContainer().getName(), null, dataSource);
    }

    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            // First rollback active transaction
            DBCExecutionContext context = getDataSource().openContext(monitor);
            try {
                monitor.subTask("Rollback active transaction");
                context.getTransactionManager().rollback(null);
            }
            catch (Throwable e) {
                log.warn("Could not rallback active transaction before disconnect", e);
            }
            finally {
                context.close();
            }

            // Close datasource
            monitor.subTask("Disconnect datasource");
            getDataSource().close(monitor);

            return new Status(
                Status.OK,
                DBeaverCore.getInstance().getPluginID(),
                "Disconnected");
        }
        catch (Exception ex) {
            return DBeaverUtils.makeExceptionStatus(
                "Error disconnecting from datasource '" + getDataSource().getContainer().getName() + "'",
                ex);
        }
    }

    protected void canceling()
    {
        getThread().interrupt();
    }

}