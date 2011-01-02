/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;

/**
 * InvalidateJob
 */
public class InvalidateJob extends DataSourceJob
{
    public InvalidateJob(
        DBPDataSource dataSource)
    {
        super("Invalidate " + dataSource.getContainer().getName(), null, dataSource);
    }

    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            // Close datasource
            monitor.subTask("Invalidate datasource");
            getDataSource().invalidateConnection(monitor);

            return Status.OK_STATUS;
        }
        catch (Exception ex) {
            return RuntimeUtils.makeExceptionStatus(
                "Error invalidating datasource '" + getDataSource().getContainer().getName() + "'",
                ex);
        }
    }

    protected void canceling()
    {
        getThread().interrupt();
    }

}