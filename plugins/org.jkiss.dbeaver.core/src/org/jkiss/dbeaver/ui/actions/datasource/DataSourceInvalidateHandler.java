/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.datasource;

import org.jkiss.utils.CommonUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.jobs.InvalidateJob;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;

public class DataSourceInvalidateHandler extends DataSourceHandler
{
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final DataSourceDescriptor dataSourceContainer = (DataSourceDescriptor) getDataSourceContainer(event, false, false);
        if (dataSourceContainer != null) {
            execute(dataSourceContainer);
        }
        return null;
    }

    public static void execute(DBSDataSourceContainer dataSourceContainer) {
        if (dataSourceContainer instanceof DataSourceDescriptor && dataSourceContainer.isConnected()) {
            final DataSourceDescriptor dataSourceDescriptor = (DataSourceDescriptor)dataSourceContainer;
            if (!CommonUtils.isEmpty(Job.getJobManager().find(dataSourceDescriptor))) {
                // Already connecting/disconnecting - just return
                return;
            }
            InvalidateJob invalidateJob = new InvalidateJob(dataSourceContainer.getDataSource());
            invalidateJob.schedule();
        }
    }

}