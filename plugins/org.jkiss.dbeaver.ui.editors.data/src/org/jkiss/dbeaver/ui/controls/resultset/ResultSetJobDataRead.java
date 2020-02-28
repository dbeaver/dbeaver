/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressLoaderVisualizer;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;

import java.lang.reflect.InvocationTargetException;

class ResultSetJobDataRead extends ResultSetJobAbstract implements ILoadService<Object> {

    private DBDDataFilter dataFilter;
    private Composite progressControl;
    private int offset;
    private int maxRows;
    private Throwable error;
    private DBCStatistics statistics;
    private boolean refresh;

    ResultSetJobDataRead(DBSDataContainer dataContainer, DBDDataFilter dataFilter, ResultSetViewer controller, DBCExecutionContext executionContext, Composite progressControl) {
        super(ResultSetMessages.controls_rs_pump_job_name + " [" + dataContainer + "]", dataContainer, controller, executionContext);
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

    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
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
        DBRProgressMonitor progressMonitor = visualizer.overwriteMonitor(monitor);

        new PumpVisualizer(visualizer).schedule(PROGRESS_VISUALIZE_PERIOD * 2);

        long fetchFlags = DBSDataContainer.FLAG_READ_PSEUDO;
        if (offset > 0) {
            fetchFlags |= DBSDataContainer.FLAG_FETCH_SEGMENT;
        }

        if (offset > 0 && dataContainer.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_SET_REREAD_ON_SCROLLING)) {
            if (maxRows > 0) {
                maxRows += offset;
            }
            offset = 0;
        }

        if (refresh) {
            fetchFlags |= DBSDataContainer.FLAG_REFRESH;
        }
        long finalFlags = fetchFlags;

        DBCExecutionPurpose purpose = dataFilter != null && dataFilter.hasFilters() ? DBCExecutionPurpose.USER_FILTERED : DBCExecutionPurpose.USER;

        try (DBCSession session = getExecutionContext().openSession(
            progressMonitor,
            purpose,
            NLS.bind(ResultSetMessages.controls_rs_pump_job_context_name, dataContainer.toString())))
        {
            DBExecUtils.tryExecuteRecover(monitor, session.getDataSource(), monitor1 -> {
                try {
                    statistics = dataContainer.readData(
                        ResultSetJobDataRead.this,
                        session,
                        controller.getDataReceiver(),
                        dataFilter,
                        offset,
                        maxRows,
                        finalFlags,
                        0);
                } catch (Throwable e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (Throwable e) {
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

    private static final int PROGRESS_VISUALIZE_PERIOD = 100;

    private class PumpVisualizer extends UIJob {

        private ProgressLoaderVisualizer<Object> visualizer;

        PumpVisualizer(ProgressLoaderVisualizer<Object> visualizer) {
            super(UIUtils.getDisplay(), "RSV Pump Visualizer");
            setSystem(true);
            this.visualizer = visualizer;
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            if (!controller.getDataReceiver().isDataReceivePaused()) {
                visualizer.visualizeLoading();
            } else {
                visualizer.resetStartTime();
            }
            if (!visualizer.isCompleted()) {
                schedule(PROGRESS_VISUALIZE_PERIOD);
            }
            return Status.OK_STATUS;
        }

    }

}
