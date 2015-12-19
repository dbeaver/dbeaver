/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.data.DBDCursor;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetContainer;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;

/**
 * TextViewDialog
 */
public class CursorViewDialog extends ValueViewDialog implements IResultSetContainer {

    private DBDCursor value;
    private ResultSetViewer resultSetViewer;
    private CursorDataContainer dataContainer;
    private static boolean keepStatementOpenToggleState = false;

    public CursorViewDialog(IValueController valueController) {
        super(valueController);
        dataContainer = new CursorDataContainer();
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        final IValueController valueController = getValueController();
        value = (DBDCursor) valueController.getValue();

        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        if (value != null) {
            DBPPreferenceStore globalPreferenceStore = DBeaverCore.getGlobalPreferenceStore();
            if (!globalPreferenceStore.getBoolean(DBeaverPreferences.KEEP_STATEMENT_OPEN)) {
                if (ConfirmationDialog.showConfirmDialog(
                        getShell(),
                        DBeaverPreferences.CONFIRM_KEEP_STATEMENT_OPEN,
                        ConfirmationDialog.QUESTION) == IDialogConstants.YES_ID) {
                    globalPreferenceStore.setValue(DBeaverPreferences.KEEP_STATEMENT_OPEN, true);
                    if (valueController.getValueSite().getPart() instanceof IResultSetContainer) {
                        ResultSetViewer rsv = ((IResultSetContainer) valueController.getValueSite().getPart()).getResultSetViewer();
                        if (rsv != null) {
                            rsv.refresh();
                        }
                    }
                }
                dialogGroup.getDisplay().asyncExec(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        close();
                    }
                });
            }
        }

        resultSetViewer = new ResultSetViewer(dialogGroup, valueController.getValueSite(), this);

        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        gd.grabExcessVerticalSpace = true;
        resultSetViewer.getControl().setLayoutData(gd);

        resultSetViewer.refresh();
        return dialogGroup;
    }

    @Override
    public Object extractEditorValue()
    {
        return null;
    }

    @Override
    public Control getControl()
    {
        return resultSetViewer.getControl();
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        resultSetViewer.refresh();
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return resultSetViewer.getContainer().getExecutionContext();
    }

    @Nullable
    @Override
    public ResultSetViewer getResultSetViewer()
    {
        return resultSetViewer;
    }

    @Nullable
    @Override
    public DBSDataContainer getDataContainer()
    {
        return dataContainer;
    }

    @Override
    public boolean isReadyToRun()
    {
        return true;
    }

    private class CursorDataContainer implements DBSDataContainer {

        @Override
        public int getSupportedFeatures()
        {
            // Nothing but plain read
            return DATA_SELECT;
        }

        @NotNull
        @Override
        public DBCStatistics readData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, @NotNull DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows, long flags) throws DBCException
        {
            DBCStatistics statistics = new DBCStatistics();
            DBRProgressMonitor monitor = session.getProgressMonitor();
            DBCResultSet dbResult = value;
            try {
                long startTime = System.currentTimeMillis();
                dataReceiver.fetchStart(session, dbResult, firstRow, maxRows);
                long rowCount;
                try {
                    rowCount = 0;
                    while (dbResult.nextRow()) {
                        if (monitor.isCanceled()) {
                            // Fetch not more than max rows
                            break;
                        }
                        dataReceiver.fetchRow(session, dbResult);
                        rowCount++;
                        if (rowCount >= maxRows) {
                            break;
                        }
                        if (rowCount % 100 == 0) {
                            monitor.subTask(rowCount + CoreMessages.dialog_cursor_view_monitor_rows_fetched);
                            monitor.worked(100);
                        }

                    }
                } finally {
                    try {
                        dataReceiver.fetchEnd(session, dbResult);
                    } catch (DBCException e) {
                        log.error("Error while finishing result set fetch", e); //$NON-NLS-1$
                    }
                }
                statistics.setFetchTime(System.currentTimeMillis() - startTime);
                statistics.setRowsFetched(rowCount);
                return statistics;
            }
            finally {
                dataReceiver.close();
            }
        }

        @Override
        public long countData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, DBDDataFilter dataFilter)
        {
            return -1;
        }

        @Nullable
        @Override
        public String getDescription()
        {
            return value.toString();
        }

        @Override
        public DBSObject getParentObject()
        {
            return null;
        }

        @NotNull
        @Override
        public DBPDataSource getDataSource()
        {
            final IValueController valueController = getValueController();
            return valueController == null ? null : valueController.getExecutionContext().getDataSource();
        }

        @NotNull
        @Override
        public String getName()
        {
            return value.toString();
        }

        @Override
        public boolean isPersisted()
        {
            return false;
        }
    }

}