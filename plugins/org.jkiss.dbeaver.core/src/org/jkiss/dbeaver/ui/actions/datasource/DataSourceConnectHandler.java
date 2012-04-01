/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.jobs.ConnectJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionAuthDialog;
import org.jkiss.utils.CommonUtils;

public class DataSourceConnectHandler extends DataSourceHandler
{
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final DataSourceDescriptor dataSourceContainer = (DataSourceDescriptor) getDataSourceContainer(event, false, false);
        if (dataSourceContainer != null) {
            execute(null, dataSourceContainer, null);
        }
        return null;
    }

    /**
     * Connects datasource
     * @param monitor progress monitor or null. If nul then new job will be started
     * @param dataSourceContainer
     * @param onFinish
     */
    public static void execute(DBRProgressMonitor monitor, DBSDataSourceContainer dataSourceContainer, final Runnable onFinish) {
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
                if (!askForPassword(dataSourceDescriptor, null)) {
                    updateDataSourceObject(dataSourceDescriptor);
                    return;
                }
            }
            for (DBWHandlerConfiguration handler : dataSourceDescriptor.getConnectionInfo().getDeclaredHandlers()) {
                if (handler.isEnabled() && handler.isSecured() && !handler.isSavePassword()) {
                    if (!askForPassword(dataSourceDescriptor, handler)) {
                        updateDataSourceObject(dataSourceDescriptor);
                        return;
                    }
                }
            }

            final ConnectJob connectJob = new ConnectJob(dataSourceDescriptor);
            final JobChangeAdapter jobChangeAdapter = new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event)
                {
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
            };
            if (monitor != null) {
                final IStatus result = connectJob.runSync(monitor);
                jobChangeAdapter.done(new IJobChangeEvent() {
                    public long getDelay()
                    {
                        return 0;
                    }

                    public Job getJob()
                    {
                        return connectJob;
                    }

                    public IStatus getResult()
                    {
                        return result;
                    }
                });
                if (!result.isOK()) {
                    UIUtils.showErrorDialog(
                        null,
                        connectJob.getName(),
                        null,//NLS.bind(CoreMessages.runtime_jobs_connect_status_error, dataSourceContainer.getName()),
                        result);
                }
            } else {
                connectJob.addJobChangeListener(jobChangeAdapter);
                connectJob.schedule();
            }
        }
    }

    private static void updateDataSourceObject(DataSourceDescriptor dataSourceDescriptor)
    {
        dataSourceDescriptor.getRegistry().fireDataSourceEvent(
            DBPEvent.Action.OBJECT_UPDATE,
            dataSourceDescriptor,
            false);
    }

    public static boolean askForPassword(final DataSourceDescriptor dataSourceContainer, final DBWHandlerConfiguration handler)
    {
        final boolean[] authResult = new boolean[] { false };
        Display.getCurrent().syncExec(new Runnable() {
            public void run()
            {
                ConnectionAuthDialog auth = new ConnectionAuthDialog(UIUtils.getActiveShell(), dataSourceContainer, handler);
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
