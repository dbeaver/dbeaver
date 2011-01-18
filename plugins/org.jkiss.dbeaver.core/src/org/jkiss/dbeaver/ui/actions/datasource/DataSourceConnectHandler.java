/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.datasource;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.jobs.ConnectJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionAuthDialog;

public class DataSourceConnectHandler extends DataSourceHandler
{
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final DataSourceDescriptor dataSourceContainer = (DataSourceDescriptor) getDataSourceContainer(event, false, false);
        if (dataSourceContainer != null) {
            execute(dataSourceContainer, null);
        }
        return null;
    }

    public static void execute(DBSDataSourceContainer dataSourceContainer, final Runnable onFinish) {
        if (dataSourceContainer instanceof DataSourceDescriptor && !dataSourceContainer.isConnected()) {
            final DataSourceDescriptor dataSourceDescriptor = (DataSourceDescriptor)dataSourceContainer;
            if (!CommonUtils.isEmpty(Job.getJobManager().find(dataSourceDescriptor))) {
                // Already connecting/disconnecting - just return
                return;
            }
            final String oldName = dataSourceDescriptor.getConnectionInfo().getUserName();
            final String oldPassword = dataSourceDescriptor.getConnectionInfo().getUserPassword();
            if (!dataSourceDescriptor.isSavePassword()) {
                // Ask for password
                if (!askForPassword(dataSourceDescriptor)) {
                    dataSourceDescriptor.getRegistry().fireDataSourceEvent(
                        DBPEvent.Action.OBJECT_UPDATE,
                        dataSourceContainer,
                        false);
                    return;
                }
            }

            ConnectJob connectJob = new ConnectJob(dataSourceDescriptor);
            connectJob.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    if (event.getResult().isOK()) {
                        if (!dataSourceDescriptor.isSavePassword()) {
                            // Rest password back to null
                            dataSourceDescriptor.getConnectionInfo().setUserName(oldName);
                            dataSourceDescriptor.getConnectionInfo().setUserPassword(oldPassword);
                        }
                    }
                    if (onFinish != null) {
                        onFinish.run();
                    }
                }
            });
            connectJob.schedule();
        }
    }

    public static boolean askForPassword(final DataSourceDescriptor dataSourceContainer)
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
                        dataSourceContainer.getRegistry().updateDataSource(dataSourceContainer);
                    }
                    authResult[0] = true;
                } else {
                    authResult[0] = false;
                }
            }
        });
        return authResult[0];
    }

}