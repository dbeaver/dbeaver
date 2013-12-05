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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
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
            //visualizer.schedule(PROGRESS_VISUALIZE_PERIOD);
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
        private int drawCount = 0;

        public PumpVisualizer() {
            super(resultSetViewer.getSite().getShell().getDisplay(), "RSV Pump Visualizer");
            setSystem(true);
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            final Control control = resultSetViewer.getSpreadsheet();
            if (!control.isDisposed()) {
                control.addPaintListener(new PaintListener() {
                    @Override
                    public void paintControl(PaintEvent e) {
                        if (!finished) {
                            Rectangle bounds = control.getBounds();
                            Image image = PROGRESS_IMAGES[drawCount % 8].getImage();
                            e.gc.drawImage(image, (bounds.width - image.getBounds().x) / 2, bounds.height / 3 + 20);
                        }
                        control.removePaintListener(this);
                    }
                });
                control.redraw();
                drawCount++;
                if (!finished) {
                    schedule(PROGRESS_VISUALIZE_PERIOD);
                } else {
                    control.redraw();
                }
            }
            return Status.OK_STATUS;
        }
    }

}
