/*
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.controls.spreadsheet.Spreadsheet;

class ResultSetDataPumpJob extends DataSourceJob {

    private static final int PROGRESS_VISUALIZE_PERIOD = 50;

    private static final DBIcon[] PROGRESS_IMAGES = {
            DBIcon.PROGRESS0, DBIcon.PROGRESS1, DBIcon.PROGRESS2, DBIcon.PROGRESS3,
            DBIcon.PROGRESS4, DBIcon.PROGRESS5, DBIcon.PROGRESS6, DBIcon.PROGRESS7};

    private ResultSetViewer resultSetViewer;
    private int offset;
    private int maxRows;
    private Throwable error;
    private DBCStatistics statistics;

    protected ResultSetDataPumpJob(ResultSetViewer resultSetViewer) {
        super(CoreMessages.controls_rs_pump_job_name, DBIcon.SQL_EXECUTE.getImageDescriptor(), resultSetViewer.getDataContainer().getDataSource());
        this.resultSetViewer = resultSetViewer;
        setUser(false);
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
        DBCSession session = getDataSource().openSession(
            monitor,
            DBCExecutionPurpose.USER,
            NLS.bind(CoreMessages.controls_rs_pump_job_context_name, resultSetViewer.getDataContainer().getName()));
        PumpVisualizer visualizer = new PumpVisualizer();
        try {
            visualizer.schedule(PROGRESS_VISUALIZE_PERIOD * 2);
            statistics = resultSetViewer.getDataContainer().readData(
                session,
                resultSetViewer.getDataReceiver(),
                resultSetViewer.getModel().getDataFilter(),
                offset,
                maxRows,
                DBSDataContainer.FLAG_READ_PSEUDO);
        }
        catch (DBException e) {
            error = e;
        }
        finally {
            session.close();
            visualizer.finished = true;
        }

        return Status.OK_STATUS;
    }

    private class ProgressPanel extends Composite {

        private final Label progressTextLabel;
        private final Label progressImageLabel;

        public ProgressPanel(Composite parent) {
            super(parent, SWT.TRANSPARENT);
            setLayoutData(new GridData(GridData.FILL_BOTH));
            setLayout(new GridLayout(1, false));
            setBackgroundMode(SWT.INHERIT_DEFAULT);

            // Placeholder to center all controls
            Composite emptyLabel = new Composite(this, SWT.NO_BACKGROUND);
            emptyLabel.setLayout(new GridLayout(1, false));
            emptyLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            progressTextLabel = new Label(this, SWT.TRANSPARENT);
            progressTextLabel.setText("Executing");
            progressTextLabel.setBackground(parent.getBackground());
            progressTextLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));

            progressImageLabel = new Label(this, SWT.NONE);
            progressImageLabel.setImage(DBIcon.PROGRESS0.getImage());
            progressImageLabel.setBackground(parent.getBackground());
            progressImageLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));

            Button cancelButton = new Button(this, SWT.PUSH);
            cancelButton.setImage(DBIcon.REJECT.getImage());
            cancelButton.setText("Cancel");
            cancelButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        }
    }

    private class PumpVisualizer extends UIJob {

        private volatile boolean finished = false;
        private ProgressPanel progressPanel;
        private ControlEditor progressOverlay;
        private int drawCount = 0;

        public PumpVisualizer() {
            super(resultSetViewer.getSite().getShell().getDisplay(), "RSV Pump Visualizer");
            setSystem(true);
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            final Spreadsheet spreadsheet = resultSetViewer.getSpreadsheet();
            if (!spreadsheet.isDisposed()) {
                drawCount++;
                if (!finished) {
                    if (progressPanel == null) {
                        progressPanel = new ProgressPanel(spreadsheet);
                        progressOverlay = new ControlEditor(spreadsheet) {
                            @Override
                            public void layout() {
                                spreadsheet.redraw();
                                super.layout();
                            }
                        };
                        Point progressBounds = progressPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                        progressOverlay.grabHorizontal = true;
                        progressOverlay.minimumHeight = progressBounds.y;
                        progressOverlay.setEditor(progressPanel);
                    } else {
                        progressPanel.progressImageLabel.setImage(
                                PROGRESS_IMAGES[drawCount % PROGRESS_IMAGES.length].getImage());
                    }
                    schedule(PROGRESS_VISUALIZE_PERIOD);
                } else {
                    // Last update - remove progress panel
                    if (progressOverlay != null) {
                        progressOverlay.dispose();
                        progressOverlay = null;
                        progressPanel.dispose();
                        progressPanel = null;
                    }
                }
            }
            return Status.OK_STATUS;
        }
    }

}
