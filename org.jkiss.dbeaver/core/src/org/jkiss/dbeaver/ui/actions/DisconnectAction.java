/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.StructuredSelection;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.jobs.DisconnectJob;

public class DisconnectAction extends DataSourceAction
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

            DisconnectJob disconnectJob = new DisconnectJob((DataSourceDescriptor) dataSourceContainer);
            disconnectJob.schedule();
        }
/*
                    try {
                        dataSourceContainer.disconnect(this);
                    }
                    catch (DBException ex) {
                        DBeaverUtils.showErrorDialog(
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                            "Disconnect", "Can't disconnect from '" + dataSourceContainer.getName() + "'", ex);
                    }
*/
    }

    public static void execute(DataSourceDescriptor descriptor)
    {
        DisconnectAction action = new DisconnectAction();
        action.selectionChanged(null, new StructuredSelection(descriptor));
        action.run(null);
    }
}