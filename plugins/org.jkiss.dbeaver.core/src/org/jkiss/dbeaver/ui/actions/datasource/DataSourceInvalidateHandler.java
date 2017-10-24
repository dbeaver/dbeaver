/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.IDataSourceContainerProviderEx;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.runtime.jobs.DisconnectJob;
import org.jkiss.dbeaver.runtime.jobs.InvalidateJob;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.ConnectionLostDialog;
import org.jkiss.dbeaver.ui.dialogs.StandardErrorDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;

// TODO: invalidate ALL contexts
public class DataSourceInvalidateHandler extends AbstractDataSourceHandler
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBCExecutionContext context = getExecutionContext(event, true);
        if (context != null) {
            execute(HandlerUtil.getActiveShell(event), context);
        } else {
            IEditorPart editor = HandlerUtil.getActiveEditor(event);
            if (editor instanceof IDataSourceContainerProviderEx) {
                // Try to set the same container.
                // It should trigger connection instantiation if for some reason it was lost (SQLEditor specific?)
                DBPDataSourceContainer dsContainer = ((IDataSourceContainerProviderEx) editor).getDataSourceContainer();
                if (dsContainer != null) {
                    ((IDataSourceContainerProviderEx) editor).setDataSourceContainer(dsContainer);
                }
            }

        }
        return null;
    }

    public static void execute(final Shell shell, final DBCExecutionContext context) {
        if (context != null) {
            //final DataSourceDescriptor dataSourceDescriptor = (DataSourceDescriptor) context;
            if (!ArrayUtils.isEmpty(Job.getJobManager().find(context.getDataSource().getContainer()))) {
                // Already connecting/disconnecting - just return
                return;
            }
            final InvalidateJob invalidateJob = new InvalidateJob(context);
            invalidateJob.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    StringBuilder message = new StringBuilder();
                    Throwable error = null;
                    int totalNum = 0, connectedNum = 0, aliveNum = 0;
                    for (InvalidateJob.ContextInvalidateResult result : invalidateJob.getInvalidateResults()) {
                        totalNum++;
                        if (result.error != null) {
                            error = result.error;
                        }
                        switch (result.result) {
                            case CONNECTED:
                            case RECONNECTED:
                                connectedNum++;
                                break;
                            case ALIVE:
                                aliveNum++;
                                break;
                            default:
                                break;
                        }
                    }
                    if (connectedNum > 0) {
                        message.insert(0, "Connections reopened: " + connectedNum + " (of " + totalNum + ")");
                    } else if (message.length() == 0) {
                        message.insert(0, "All connections (" + totalNum + ") are alive!");
                    }
                    if (error != null) {
//                        UIUtils.showErrorDialog(
//                            shell,
//                            "Invalidate data source [" + context.getDataSource().getContainer().getName() + "]",
//                            "Error while connecting to the datasource",// + "\nTime spent: " + RuntimeUtils.formatExecutionTime(invalidateJob.getTimeSpent()),
//                            error);
                        DBeaverUI.syncExec(new Runnable() {
                            @Override
                            public void run() {

                            }
                        });
                        final DBPDataSourceContainer container = context.getDataSource().getContainer();
                        final Throwable dialogError = error;
                        final Integer result = new UITask<Integer>() {
                            @Override
                            protected Integer runTask() {
                                ConnectionLostDialog clDialog = new ConnectionLostDialog(shell, container, dialogError, "Disconnect");
                                return clDialog.open();
                            }
                        }.execute();
                        if (result == IDialogConstants.STOP_ID) {
                            // Disconnect - to notify UI and reflect model changes
                            new DisconnectJob(container).schedule();
                        } else if (result == IDialogConstants.RETRY_ID) {
                            execute(shell, context);
                        }
                    } else {
                        log.info(message);
                    }
                }
            });
            invalidateJob.schedule();
        }
    }

    public static void showConnectionLostDialog(final Shell shell, final String message, final DBException error)
    {
        //log.debug(message);
        Runnable runnable = new Runnable() {
            @Override
            public void run()
            {
                // Display the dialog
                DBPDataSource dataSource = error.getDataSource();
                if (dataSource == null) {
                    throw new IllegalStateException("No data source in error");
                }
                String title = "Connection with [" + dataSource.getContainer().getName() + "] lost";
                ConnectionRecoverDialog dialog = new ConnectionRecoverDialog(shell, title, message == null ? title : message, error);
                dialog.open();
            }
        };
        DBeaverUI.syncExec(runnable);
    }

    private static class ConnectionRecoverDialog extends StandardErrorDialog {

        private final DBPDataSource dataSource;

        public ConnectionRecoverDialog(Shell shell, String title, String message, DBException error)
        {
            super(
                shell == null ? DBeaverUI.getActiveWorkbenchShell() : shell,
                title,
                message,
                GeneralUtils.makeExceptionStatus(error),
                IStatus.ERROR);
            dataSource = error.getDataSource();
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent)
        {
            createButton(parent, IDialogConstants.RETRY_ID, "&Reconnect", true);
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);
            createDetailsButton(parent);
        }

        @Override
        protected void buttonPressed(int id)
        {
            if (id == IDialogConstants.RETRY_ID) {
                execute(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                    dataSource.getDefaultContext(false));
                super.buttonPressed(IDialogConstants.OK_ID);
            }
            super.buttonPressed(id);
        }
    }

}