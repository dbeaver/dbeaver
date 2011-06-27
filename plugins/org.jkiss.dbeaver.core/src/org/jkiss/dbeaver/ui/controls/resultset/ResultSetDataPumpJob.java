/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

class ResultSetDataPumpJob extends DataSourceJob {

    private ResultSetViewer resultSetViewer;
    private int offset;
    private int maxRows;
    private Throwable error;

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

    public Throwable getError()
    {
        return error;
    }

    protected IStatus run(DBRProgressMonitor monitor) {
        error = null;
        DBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.USER, "Read data from '" + resultSetViewer.getDataContainer().getName() + "'");
        try {
            resultSetViewer.getDataContainer().readData(
                context,
                resultSetViewer.getDataReceiver(),
                resultSetViewer.getDataFilter(),
                offset,
                maxRows);
        }
        catch (DBException e) {
            error = e;
        }
        finally {
            context.close();
        }

        return Status.OK_STATUS;
    }

}
