/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.StructuredSelection;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.runtime.jobs.ReconnectJob;

public class ReconnectAction extends DataSourceAction
{
    @Override
    protected void updateAction(IAction action) {
        if (action != null) {
            DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(false);
            action.setEnabled(dataSourceContainer != null && dataSourceContainer.isConnected());
        }
    }

    public void run(IAction action)
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(false);
        if (dataSourceContainer != null && dataSourceContainer.isConnected()) {

            if (!CommonUtils.isEmpty(Job.getJobManager().find(dataSourceContainer))) {
                // Already connecting/disconnecting - just return
                return;
            }

            ReconnectJob reconnectJob = new ReconnectJob(dataSourceContainer.getDataSource());
            reconnectJob.schedule();
        }
    }

    public static void execute(DBSDataSourceContainer dataSourceContainer) {
        ReconnectAction action = new ReconnectAction();
        action.selectionChanged(null, new StructuredSelection(dataSourceContainer));
        action.run(null);
    }

}