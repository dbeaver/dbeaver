/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
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
        super(CoreMessages.controls_rs_pump_job_name, DBIcon.SQL_EXECUTE.getImageDescriptor(), resultSetViewer.getDataContainer().getDataSource());
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

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
        error = null;
        DBCExecutionContext context = getDataSource().openContext(
                monitor,
                DBCExecutionPurpose.USER,
                NLS.bind(CoreMessages.controls_rs_pump_job_context_name, resultSetViewer.getDataContainer().getName()));
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
