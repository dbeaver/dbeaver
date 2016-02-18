/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.exec;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.AbstractUIJob;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * ExecutionQueueErrorJob
 */
public class ExecutionQueueErrorJob extends AbstractUIJob {

    private String errorName;
    private Throwable error;
    private boolean queue;
    private ExecutionQueueErrorResponse response = ExecutionQueueErrorResponse.STOP;

    public ExecutionQueueErrorJob(String errorName, Throwable error, boolean queue)
    {
        super("Execution Error Job");
        this.errorName = errorName;
        this.error = error;
        this.queue = queue;
    }

    @Override
    public IStatus runInUIThread(DBRProgressMonitor monitor)
    {
        ExecutionQueueErrorDialog dialog = new ExecutionQueueErrorDialog(
            DBeaverUI.getActiveWorkbenchShell(),
            "Execution Error",
            "Error occurred during " + errorName,
            GeneralUtils.makeExceptionStatus(error),
            IStatus.INFO | IStatus.WARNING | IStatus.ERROR,
            queue);
        int result = dialog.open();
        switch (result) {
            case IDialogConstants.STOP_ID: response = ExecutionQueueErrorResponse.STOP; break;
            case IDialogConstants.SKIP_ID: response = ExecutionQueueErrorResponse.IGNORE; break;
            case IDialogConstants.RETRY_ID: response = ExecutionQueueErrorResponse.RETRY; break;
            default: response = ExecutionQueueErrorResponse.IGNORE_ALL; break;
        }

        return Status.OK_STATUS;
    }

    public ExecutionQueueErrorResponse getResponse()
    {
        return response;
    }

    public static ExecutionQueueErrorResponse showError(String task, Throwable error, boolean queue) {
        ExecutionQueueErrorJob errorJob = new ExecutionQueueErrorJob(task, error, queue);
        errorJob.schedule();
        try {
            errorJob.join();
        } catch (InterruptedException e1) {
            log.error(e1);
        }
        return errorJob.getResponse();
    }

}
