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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.ProxyProgressMonitor;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.controls.spreadsheet.Spreadsheet;
import org.jkiss.utils.CommonUtils;

class ResultSetDataPumpJob extends DataSourceJob {

    private static final int PROGRESS_VISUALIZE_PERIOD = 100;

    private static final DBIcon[] PROGRESS_IMAGES = {
            DBIcon.PROGRESS0, DBIcon.PROGRESS1, DBIcon.PROGRESS2, DBIcon.PROGRESS3,
            DBIcon.PROGRESS4, DBIcon.PROGRESS5, DBIcon.PROGRESS6, DBIcon.PROGRESS7,
            DBIcon.PROGRESS8, DBIcon.PROGRESS9
    };
    static final Log log = LogFactory.getLog(ResultSetDataPumpJob.class);

    private ResultSetViewer resultSetViewer;
    private int offset;
    private int maxRows;
    private Throwable error;
    private DBCStatistics statistics;
    private long pumpStartTime;
    private String progressMessage;

    protected ResultSetDataPumpJob(ResultSetViewer resultSetViewer) {
        super(CoreMessages.controls_rs_pump_job_name, DBIcon.SQL_EXECUTE.getImageDescriptor(), resultSetViewer.getDataContainer().getDataSource());
        this.resultSetViewer = resultSetViewer;
        progressMessage = CoreMessages.controls_rs_pump_job_name;
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
        pumpStartTime = System.currentTimeMillis();
        DBRProgressMonitor proxyMonitor = new ProxyProgressMonitor(monitor) {
            @Override
            public void beginTask(String name, int totalWork) {
                //progressMessage = name;
                super.beginTask(name, totalWork);
            }

            @Override
            public void subTask(String name) {
                progressMessage = name;
                super.subTask(name);
            }
        };
        DBCSession session = getDataSource().openSession(
            proxyMonitor,
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

    private class PumpVisualizer extends UIJob {

        private volatile boolean finished = false;
        private ControlEditor progressOverlay;
        private volatile int drawCount = 0;
        private Button cancelButton;
        private PaintListener painListener;

        public PumpVisualizer() {
            super(resultSetViewer.getSite().getShell().getDisplay(), "RSV Pump Visualizer");
            setSystem(true);
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            final Spreadsheet spreadsheet = resultSetViewer.getSpreadsheet();
            if (!spreadsheet.isDisposed()) {
                if (!finished) {
                    try {
                        showProgress(spreadsheet);
                    } catch (Exception e) {
                        log.error("Internal error during progress visualization", e);
                        // Something went terribly wrong
                        // We shouldn't be here ever. In any case we must finish the job
                        finishProgress(spreadsheet);
                    }
                } else {
                    finishProgress(spreadsheet);

                }
            }
            return Status.OK_STATUS;
        }

        private void showProgress(final Spreadsheet spreadsheet) {
            if (progressOverlay == null) {
                // Start progress visualization
                resultSetViewer.getModel().setUpdateInProgress(true);
                resultSetViewer.updateFiltersText();

                cancelButton = new Button(spreadsheet, SWT.PUSH);
                cancelButton.setText("Cancel");
                GridData gd = new GridData(GridData.FILL_BOTH);
                gd.verticalIndent = DBIcon.PROGRESS0.getImage().getBounds().height * 2;
                cancelButton.setLayoutData(gd);
                cancelButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        cancelButton.setText("Canceled");
                        cancelButton.setEnabled(false);
                        Point buttonSize = cancelButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                        progressOverlay.minimumWidth = buttonSize.x;
                        progressOverlay.minimumHeight = buttonSize.y;
                        progressOverlay.layout();
                        ResultSetDataPumpJob.this.cancel();
                    }
                });

                painListener = new PaintListener() {
                    @Override
                    public void paintControl(PaintEvent e) {
                        Image image = PROGRESS_IMAGES[drawCount % PROGRESS_IMAGES.length].getImage();
                        Rectangle buttonBounds = cancelButton.getBounds();
                        Rectangle imageBounds = image.getBounds();
                        e.gc.drawImage(
                                image,
                                (buttonBounds.x + buttonBounds.width / 2) - imageBounds.width / 2,
                                buttonBounds.y - imageBounds.height - 5);

                        long elapsedTime = System.currentTimeMillis() - pumpStartTime;
                        String elapsedString = elapsedTime > 10000 ?
                                String.valueOf(elapsedTime / 1000) :
                                String.valueOf(((double) (elapsedTime / 100)) / 10);
                        String statusMessage = CommonUtils.truncateString(
                                progressMessage.replaceAll("\\s", " "), 64);
                        String status = statusMessage + " - " + elapsedString + "s";
                        Point statusSize = e.gc.textExtent(status);

                        int statusX = (buttonBounds.x + buttonBounds.width / 2) - statusSize.x / 2;
                        int statusY = buttonBounds.y - imageBounds.height - 10 - statusSize.y;
                        e.gc.setForeground(spreadsheet.getForegroundNormal());
                        e.gc.setBackground(spreadsheet.getBackground());
                        e.gc.fillRectangle(statusX - 2, statusY - 2, statusSize.x + 4, statusSize.y + 4);
                        e.gc.drawText(status, statusX, statusY, true);
                    }
                };
                spreadsheet.addPaintListener(painListener);

                progressOverlay = new ControlEditor(spreadsheet) {
                    @Override
                    public void layout() {
                        spreadsheet.redraw();
                        super.layout();
                    }
                };
                Point buttonSize = cancelButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                progressOverlay.minimumWidth = buttonSize.x;
                progressOverlay.minimumHeight = buttonSize.y;
                progressOverlay.setEditor(cancelButton);
            }
            drawCount++;
            progressOverlay.layout();
            schedule(PROGRESS_VISUALIZE_PERIOD);
        }

        private void finishProgress(Spreadsheet spreadsheet) {
            // Last update - remove progress visualization
            if (progressOverlay != null) {
                progressOverlay.dispose();
                progressOverlay = null;
                cancelButton.dispose();
                spreadsheet.removePaintListener(painListener);
                spreadsheet.redraw();
            }
            resultSetViewer.getModel().setUpdateInProgress(false);
            resultSetViewer.updateFiltersText();
        }

    }

}
