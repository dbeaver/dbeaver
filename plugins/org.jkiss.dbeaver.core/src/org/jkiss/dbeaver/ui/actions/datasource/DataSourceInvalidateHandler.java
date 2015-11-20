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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.runtime.jobs.DisconnectJob;
import org.jkiss.dbeaver.runtime.jobs.InvalidateJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
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
        }
        return null;
    }

    public static void execute(final Shell shell, final DBCExecutionContext context) {
        if (context != null && context.isConnected()) {
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
                        UIUtils.showErrorDialog(
                            shell,
                            "Invalidate data source [" + context.getDataSource().getContainer().getName() + "]",
                            "Error while connecting to the datasource",// + "\nTime spent: " + RuntimeUtils.formatExecutionTime(invalidateJob.getTimeSpent()),
                            error);
                        // Disconnect - to notify UI and reflect model changes
                        new DisconnectJob(context.getDataSource().getContainer()).schedule();
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
        UIUtils.runInUI(shell, runnable);
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
            createButton(parent, IDialogConstants.RETRY_ID, "Reconnect", true);
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