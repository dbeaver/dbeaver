/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.ui.AbstractUIJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * ExecutionQueueErrorJob
 */
public class ExecutionQueueErrorJob extends AbstractUIJob {

    private String errorName;
    private Throwable error;
    private boolean queue;
    private DBPPlatformUI.UserResponse response = DBPPlatformUI.UserResponse.STOP;

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
            UIUtils.getActiveWorkbenchShell(),
            "Execution Error",
            "Error occurred during " + errorName,
            GeneralUtils.makeExceptionStatus(error),
            IStatus.INFO | IStatus.WARNING | IStatus.ERROR,
            queue);
        int result = dialog.open();
        switch (result) {
            case IDialogConstants.CANCEL_ID:
            case IDialogConstants.STOP_ID:
                response = DBPPlatformUI.UserResponse.STOP;
                break;
            case IDialogConstants.SKIP_ID:
                response = DBPPlatformUI.UserResponse.IGNORE;
                break;
            case IDialogConstants.RETRY_ID:
                response = DBPPlatformUI.UserResponse.RETRY;
                break;
            case IDialogConstants.IGNORE_ID:
                response = DBPPlatformUI.UserResponse.IGNORE_ALL;
                break;
            default:
                response = DBPPlatformUI.UserResponse.STOP;
                break;
        }

        return Status.OK_STATUS;
    }

    public DBPPlatformUI.UserResponse getResponse()
    {
        return response;
    }

    public static DBPPlatformUI.UserResponse showError(String task, Throwable error, boolean queue) {
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
