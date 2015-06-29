/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProcessListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.jobs.ConnectJob;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.utils.ArrayUtils;

public class DataSourceConnectHandler extends DataSourceHandler
{
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
            final String oldName = dataSourceDescriptor.getConnectionConfiguration().getUserName();
            final String oldPassword = dataSourceDescriptor.getConnectionConfiguration().getUserPassword();
            if (!dataSourceDescriptor.isSavePassword()) {
                // Ask for password
                if (!askForPassword(dataSourceDescriptor, null)) {
                    updateDataSourceObject(dataSourceDescriptor);
                    return;
                }
            }
            for (DBWHandlerConfiguration handler : dataSourceDescriptor.getConnectionConfiguration().getDeclaredHandlers()) {
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
                    IStatus result = connectJob.getConnectStatus();
                    if (result.isOK()) {
                        if (!dataSourceDescriptor.isSavePassword()) {
                            // Rest password back to null
                            // TODO: to be correct we need to reset password info.
                            // but we need a password to open isolated contexts (e.g. for data export)
                            // Currently it is not possible to ask for password from isolation context opening
                            // procedure. We need to do something here...
                            //dataSourceDescriptor.getConnectionConfiguration().setUserName(oldName);
                            //dataSourceDescriptor.getConnectionConfiguration().setUserPassword(oldPassword);
                        }
                    }
                    if (onFinish != null) {
                        onFinish.onProcessFinish(result);
                    } else if (!result.isOK()) {
                        DBUserInterface.getInstance().showError(
                            connectJob.getName(),
                            null,//NLS.bind(CoreMessages.runtime_jobs_connect_status_error, dataSourceContainer.getName()),
                            result);
                    }
                }
            };
            if (monitor != null) {
                connectJob.runSync(monitor);
                jobChangeAdapter.done(new IJobChangeEvent() {
                    @Override
                    public long getDelay() {
                        return 0;
                    }

                    @Override
                    public Job getJob() {
                        return connectJob;
                    }

                    @Override
                    public IStatus getResult() {
                        return connectJob.getConnectStatus();
                    }

                    public IStatus getJobGroupResult() {
                        return null;
                    }
                });
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

    private static void updateDataSourceObject(DataSourceDescriptor dataSourceDescriptor)
    {
        dataSourceDescriptor.getRegistry().fireDataSourceEvent(
            DBPEvent.Action.OBJECT_UPDATE,
            dataSourceDescriptor,
            false);
    }

    public static boolean askForPassword(@NotNull final DataSourceDescriptor dataSourceContainer, @Nullable final DBWHandlerConfiguration networkHandler)
    {
        String prompt = networkHandler != null ?
            NLS.bind(CoreMessages.dialog_connection_auth_title_for_handler, networkHandler.getTitle()) :
            "'" + dataSourceContainer.getName() + CoreMessages.dialog_connection_auth_title; //$NON-NLS-1$
        String user = networkHandler != null ? networkHandler.getUserName() : dataSourceContainer.getConnectionConfiguration().getUserName();
        String password = networkHandler != null ? networkHandler.getPassword() : dataSourceContainer.getConnectionConfiguration().getUserPassword();

        DBAAuthInfo authInfo = DBUserInterface.getInstance().promptUserCredentials(prompt, user, password);
        if (authInfo == null) {
            return false;
        }

        if (networkHandler != null) {
            networkHandler.setUserName(authInfo.getUserName());
            networkHandler.setPassword(authInfo.getUserPassword());
            networkHandler.setSavePassword(authInfo.isSavePassword());
        } else {
            dataSourceContainer.getConnectionConfiguration().setUserName(authInfo.getUserName());
            dataSourceContainer.getConnectionConfiguration().setUserPassword(authInfo.getUserPassword());
            dataSourceContainer.setSavePassword(authInfo.isSavePassword());
        }
        if (dataSourceContainer.isSavePassword()) {
            // Update connection properties
            dataSourceContainer.getRegistry().updateDataSource(dataSourceContainer);
        }

        return true;
    }

}
