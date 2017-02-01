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
