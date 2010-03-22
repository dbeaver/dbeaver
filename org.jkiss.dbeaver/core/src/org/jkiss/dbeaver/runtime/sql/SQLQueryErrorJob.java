package org.jkiss.dbeaver.runtime.sql;

import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.PlatformUI;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.ui.DBeaverUtils;

/**
 * SQLQueryErrorJob
 */
public class SQLQueryErrorJob extends UIJob {

    private Throwable error;
    private boolean script;
    private SQLQueryErrorResponse response = SQLQueryErrorResponse.STOP;

    public SQLQueryErrorJob(Throwable error, boolean script)
    {
        super("SQL Error Job");
        this.error = error;
        this.script = script;
    }

    public IStatus runInUIThread(IProgressMonitor monitor)
    {
        SQLQueryErrorDialog dialog = new SQLQueryErrorDialog(
            null,
            "SQL Error",
            script ?
                "Error occured during SQL script execution" :
                "Error occured during SQL query execution",
            DBeaverUtils.makeExceptionStatus(error),
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
