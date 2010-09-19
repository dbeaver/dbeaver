/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;

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
        String statusMessage = null;
        boolean hasErrors = false;
        DBCExecutionContext context = getDataSource().openContext(monitor, "Read data from '" + resultSetViewer.getDataContainer().getName() + "'");
        try {
            resultSetViewer.getDataContainer().readData(
                context,
                resultSetViewer.getDataReceiver(),
                offset,
                maxRows);
        }
        catch (DBException e) {
            statusMessage = e.getMessage();
            hasErrors = true;
        }
        finally {
            context.close();
        }

        if (hasErrors) {
            // Set status
            final String message = statusMessage;
            resultSetViewer.getControl().getDisplay().syncExec(new Runnable() {
                public void run() {
                    resultSetViewer.setStatus(message, true);
                }
            });
        }
        return Status.OK_STATUS;
    }

}
