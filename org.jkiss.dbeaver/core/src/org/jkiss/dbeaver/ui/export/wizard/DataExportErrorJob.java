/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.wizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.runtime.sql.SQLQueryErrorDialog;
import org.jkiss.dbeaver.runtime.sql.SQLQueryErrorResponse;
import org.jkiss.dbeaver.utils.DBeaverUtils;

/**
 * DataExportErrorJob
 */
public class DataExportErrorJob extends AbstractUIJob {

    private Throwable error;

    public DataExportErrorJob(Throwable error)
    {
        super("Data Export Error");
        this.error = error;
    }

    public IStatus runInUIThread(DBRProgressMonitor monitor)
    {
        DBeaverUtils.showErrorDialog(
            getDisplay().getActiveShell(),
            "Data export error",
            error.getMessage(), error);
        return Status.OK_STATUS;
    }

}