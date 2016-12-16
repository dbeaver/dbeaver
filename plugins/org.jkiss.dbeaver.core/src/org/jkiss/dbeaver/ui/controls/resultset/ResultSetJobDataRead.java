/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.controls.ProgressLoaderVisualizer;

import java.lang.reflect.InvocationTargetException;

class ResultSetJobDataRead extends ResultSetJobAbstract implements ILoadService<Object> {

    private DBDDataFilter dataFilter;
    private Composite progressControl;
    private int offset;
    private int maxRows;
    private Throwable error;
    private DBCStatistics statistics;
    private DBRProgressMonitor progressMonitor;

    protected ResultSetJobDataRead(DBSDataContainer dataContainer, DBDDataFilter dataFilter, ResultSetViewer controller, DBCExecutionContext executionContext, Composite progressControl) {
        super(CoreMessages.controls_rs_pump_job_name + " [" + dataContainer + "]", dataContainer, controller, executionContext);
        this.dataFilter = dataFilter;
        this.progressControl = progressControl;
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

    DBCStatistics getStatistics()
    {
        return statistics;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
        error = null;
        final ProgressLoaderVisualizer<Object> visualizer = new ProgressLoaderVisualizer<>(this, progressControl);
        progressMonitor = visualizer.overwriteMonitor(monitor);
        DBCExecutionPurpose purpose = DBCExecutionPurpose.USER;
        if (dataFilter != null && dataFilter.hasFilters()) {
            purpose = DBCExecutionPurpose.USER_FILTERED;
        }
        new PumpVisualizer(visualizer).schedule(PROGRESS_VISUALIZE_PERIOD * 2);
        try (DBCSession session = getExecutionContext().openSession(
            progressMonitor,
            purpose,
            NLS.bind(CoreMessages.controls_rs_pump_job_context_name, dataContainer.toString())))
        {
            statistics = dataContainer.readData(
                this,
                session,
                controller.getDataReceiver(),
                dataFilter,
                offset,
                maxRows,
                DBSDataContainer.FLAG_READ_PSEUDO
            );
        } catch (DBException e) {
            error = e;
        } finally {
            visualizer.completeLoading(null);
        }

        return Status.OK_STATUS;
    }

    @Override
    public String getServiceName() {
        return "ResultSet data pump";
    }

    @Override
    public Object evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        // It is not a real service so just return nothing
        return null;
    }

    @Override
    public Object getFamily() {
        return getExecutionController();
    }

    protected static final int PROGRESS_VISUALIZE_PERIOD = 100;

    private class PumpVisualizer extends UIJob {

        private ProgressLoaderVisualizer<Object> visualizer;

        public PumpVisualizer(ProgressLoaderVisualizer<Object> visualizer) {
            super(progressControl.getDisplay(), "RSV Pump Visualizer");
            setSystem(true);
            this.visualizer = visualizer;
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            visualizer.visualizeLoading();
            if (!visualizer.isCompleted()) {
                schedule(PROGRESS_VISUALIZE_PERIOD);
            }
            return Status.OK_STATUS;
        }

    }

}
