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
package org.jkiss.dbeaver.runtime.exec;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.runtime.RuntimeUtils;

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
            RuntimeUtils.makeExceptionStatus(error),
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
}
