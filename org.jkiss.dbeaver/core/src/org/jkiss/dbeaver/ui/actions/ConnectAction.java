/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.jobs.ConnectJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionAuthDialog;

public class ConnectAction extends DataSourceAction
{
    @Override
    protected void updateAction(IAction action) {
        if (action != null) {
            DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(false);
            action.setEnabled(dataSourceContainer != null && !dataSourceContainer.isConnected());
        }
    }

    public void run(IAction action)
    {
        final DataSourceDescriptor dataSourceContainer = (DataSourceDescriptor) getDataSourceContainer(false);
        if (dataSourceContainer != null && !dataSourceContainer.isConnected()) {

            if (!CommonUtils.isEmpty(Job.getJobManager().find(this))) {
                // Already connecting/disconnecting - jsut return
                return;
            }
            final String oldName = dataSourceContainer.getConnectionInfo().getUserName();
            final String oldPassword = dataSourceContainer.getConnectionInfo().getUserPassword();
            if (!dataSourceContainer.isSavePassword()) {
                // Ask for password
                if (!askForPassword(dataSourceContainer)) {
                    dataSourceContainer.getViewCallback().getDataSourceRegistry().fireDataSourceEvent(
                        DBPEvent.Action.OBJECT_UPDATE,
                        dataSourceContainer,
                        false);
                    return;
                }
            }

            ConnectJob connectJob = new ConnectJob(dataSourceContainer);
            connectJob.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    if (event.getResult().isOK()) {
                        if (!dataSourceContainer.isSavePassword()) {
                            // Rest password back to null
                            dataSourceContainer.getConnectionInfo().setUserName(oldName);
                            dataSourceContainer.getConnectionInfo().setUserPassword(oldPassword);
                        }
                    }
                }
            });
            connectJob.schedule();
        }
    }

    public boolean askForPassword(final DataSourceDescriptor dataSourceContainer)
    {
        final boolean[] authResult = new boolean[] { false };
        Display.getCurrent().syncExec(new Runnable() {
            public void run()
            {
                ConnectionAuthDialog auth = new ConnectionAuthDialog(UIUtils.getActiveShell(), dataSourceContainer);
                int result = auth.open();
                if (result == IDialogConstants.OK_ID) {
                    if (dataSourceContainer.isSavePassword()) {
                        // Update connection properties
                        dataSourceContainer.getDriver().getProviderDescriptor().getRegistry().updateDataSource(dataSourceContainer);
                    }
                    authResult[0] = true;
                } else {
                    authResult[0] = false;
                }
            }
        });
        return authResult[0];
    }

    public static void execute(DBSDataSourceContainer dataSourceContainer) {
        ConnectAction action = new ConnectAction();
        action.selectionChanged(null, new StructuredSelection(dataSourceContainer));
        action.run(null);
    }
}