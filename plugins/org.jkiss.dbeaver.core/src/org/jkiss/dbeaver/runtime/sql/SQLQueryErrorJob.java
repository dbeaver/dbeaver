/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.runtime.RuntimeUtils;

/**
 * SQLQueryErrorJob
 */
public class SQLQueryErrorJob extends AbstractUIJob {

    private Throwable error;
    private boolean script;
    private SQLQueryErrorResponse response = SQLQueryErrorResponse.STOP;

    public SQLQueryErrorJob(Throwable error, boolean script)
    {
        super("SQL Error Job");
        this.error = error;
        this.script = script;
    }

    @Override
    public IStatus runInUIThread(DBRProgressMonitor monitor)
    {
        SQLQueryErrorDialog dialog = new SQLQueryErrorDialog(
            null,
            "SQL Error",
            script ?
                "Error occurred during SQL script execution" :
                "Error occurred during SQL query execution",
            RuntimeUtils.makeExceptionStatus(error),
            IStatus.INFO | IStatus.WARNING | IStatus.ERROR,
            script);
        int result = dialog.open();
        switch (result) {
            case IDialogConstants.STOP_ID: response = SQLQueryErrorResponse.STOP; break;
            case IDialogConstants.SKIP_ID: response = SQLQueryErrorResponse.IGNORE; break;
            case IDialogConstants.RETRY_ID: response = SQLQueryErrorResponse.RETRY; break;
            default: response = SQLQueryErrorResponse.IGNORE_ALL; break;
        }

        return Status.OK_STATUS;
    }

    public SQLQueryErrorResponse getResponse()
    {
        return response;
    }
}
