/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProcessListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.jobs.ConnectJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionAuthDialog;
import org.jkiss.utils.ArrayUtils;

public class DataSourceConnectHandler extends DataSourceHandler
{
    @Nullable
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final DataSourceDescriptor dataSourceContainer = (DataSourceDescriptor) getDataSourceContainer(event, false);
        if (dataSourceContainer != null) {
            execute(null, dataSourceContainer, null);
        }
        return null;
    }

    /**
     * Connects datasource
     * @param monitor progress monitor or null. If nul then new job will be started
     * @param dataSourceContainer    container to connect
     * @param onFinish               finish handler
     */
    public static void execute(
        @Nullable DBRProgressMonitor monitor,
        @NotNull DBSDataSourceContainer dataSourceContainer,
        @Nullable final DBRProcessListener onFinish)
    {
        if (dataSourceContainer instanceof DataSourceDescriptor && !dataSourceContainer.isConnected()) {
            final DataSourceDescriptor dataSourceDescriptor = (DataSourceDescriptor)dataSourceContainer;
            if (!ArrayUtils.isEmpty(Job.getJobManager().find(dataSourceDescriptor))) {
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
                            // TODO: to be correct we need to reset password info.
                            // but we need a password to open isolated contexts (e.g. for data export)
                            // Currently it is not possible to ask for password from isolation context opening
                            // procedure. We need to do something here...
                            //dataSourceDescriptor.getConnectionInfo().setUserName(oldName);
                            //dataSourceDescriptor.getConnectionInfo().setUserPassword(oldPassword);
                        }
                    }
                    if (onFinish != null) {
                        onFinish.onProcessFinish(event.getResult());
                    }
                }
            };
            if (monitor != null) {
                final IStatus result = connectJob.runSync(monitor);
                jobChangeAdapter.done(new IJobChangeEvent() {
                    @Override
                    public long getDelay()
                    {
                        return 0;
                    }

                    @Override
                    public Job getJob()
                    {
                        return connectJob;
                    }

                    @Override
                    public IStatus getResult()
                    {
                        return result;
                    }
                });
                if (!result.isOK()) {
                    if (onFinish != null) {
                        onFinish.onProcessFinish(result);
                    } else {
                        UIUtils.showErrorDialog(
                            null,
                            connectJob.getName(),
                            null,//NLS.bind(CoreMessages.runtime_jobs_connect_status_error, dataSourceContainer.getName()),
                            result);
                    }
                }
            } else {
                connectJob.addJobChangeListener(jobChangeAdapter);
                // Schedule in UI because connect may be initiated during application startup
                // and UI is still not initiated. In this case no progress dialog will appear
                // to be sure run in UI async
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        connectJob.schedule();
                    }
                });
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

    public static boolean askForPassword(@NotNull final DataSourceDescriptor dataSourceContainer, @Nullable final DBWHandlerConfiguration handler)
    {
        final boolean[] authResult = new boolean[] { false };
        UIUtils.runInUI(null, new Runnable() {
            @Override
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
