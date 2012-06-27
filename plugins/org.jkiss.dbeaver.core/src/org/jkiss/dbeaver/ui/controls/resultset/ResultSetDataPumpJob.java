/*
 * Copyright (C) 2010-2012 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
