/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.utils.DBeaverUtils;

class ResultSetDataPumpJob extends DataSourceJob {

    private ResultSetViewer resultSetViewer;
    private int offset;
    private int maxRows;

    protected ResultSetDataPumpJob(ResultSetViewer resultSetViewer) {
        super("Read data", DBIcon.SQL_EXECUTE.getImageDescriptor(), resultSetViewer.getDataContainer().getDataSource());
        this.resultSetViewer = resultSetViewer;
    }

    public void setOffset(int offset)
    {
        this.offset = offset;
    }

    public void setMaxRows(int maxRows)
    {
        this.maxRows = maxRows;
    }

    protected IStatus run(DBRProgressMonitor monitor) {
        boolean hasErrors = false;
        Throwable error = null;
        DBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.USER, "Read data from '" + resultSetViewer.getDataContainer().getName() + "'");
        try {
            resultSetViewer.getDataContainer().readData(
                context,
                resultSetViewer.getDataReceiver(),
                offset,
                maxRows);
        }
        catch (DBException e) {
            error = e;
            hasErrors = true;
        }
        finally {
            context.close();
        }

        if (hasErrors) {
            // Set status
            final Throwable err = error;
            resultSetViewer.getControl().getDisplay().syncExec(new Runnable() {
                public void run() {
                    resultSetViewer.setStatus(err.getMessage(), true);
                    DBeaverUtils.showErrorDialog(
                        resultSetViewer.getControl().getShell(),
                        "Error executing query",
                        err.getMessage());
                }
            });
        }
        return Status.OK_STATUS;
    }

}
