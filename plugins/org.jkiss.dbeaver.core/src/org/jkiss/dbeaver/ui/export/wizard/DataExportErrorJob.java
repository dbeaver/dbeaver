/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.wizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.ui.UIUtils;

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
        UIUtils.showErrorDialog(
            getDisplay().getActiveShell(),
            "Data export error",
            error.getMessage(), error);
        return Status.OK_STATUS;
    }

}