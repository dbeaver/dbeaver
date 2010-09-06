/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.event.DataSourceEvent;
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
            DataSourceDescriptor descriptor = (DataSourceDescriptor)getDataSource().getContainer();

            descriptor.disconnect(monitor);

            return Status.OK_STATUS;
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