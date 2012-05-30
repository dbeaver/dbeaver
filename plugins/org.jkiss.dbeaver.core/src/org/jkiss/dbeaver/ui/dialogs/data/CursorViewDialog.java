/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetProvider;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * TextViewDialog
 */
public class CursorViewDialog extends ValueViewDialog implements ResultSetProvider {

    private DBDCursor value;
    private ResultSetViewer resultSetViewer;
    private CursorDataContainer dataContainer;
    private static boolean keepStatementOpenToggleState = false;

    public CursorViewDialog(DBDValueController valueController) {
        super(valueController);
        dataContainer = new CursorDataContainer();
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        final DBDValueController valueController = getValueController();
        value = (DBDCursor) valueController.getValue();

        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        if (value instanceof DBDCursor) {
            IPreferenceStore globalPreferenceStore = DBeaverCore.getInstance().getGlobalPreferenceStore();
            if (!globalPreferenceStore.getBoolean(PrefConstants.KEEP_STATEMENT_OPEN) && !keepStatementOpenToggleState) {
                MessageDialogWithToggle dialogWithToggle = MessageDialogWithToggle.openOkCancelConfirm(
                        getShell(),
                        CoreMessages.dialog_cursor_view_keep_cursor_title,
                        CoreMessages.dialog_cursor_view_keep_cursor_message,
                        null, keepStatementOpenToggleState, null, null);
                keepStatementOpenToggleState = dialogWithToggle.getToggleState();
                if (dialogWithToggle.getReturnCode() == Window.OK) {
                    globalPreferenceStore.setValue(PrefConstants.KEEP_STATEMENT_OPEN, true);
                    ((SQLEditor)valueController.getValueSite().getPart()).getResultsView().refresh();
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
    protected Object getEditorValue()
    {
        return null;
    }

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
            return 0;
        }

        @Override
        public long readData(DBCExecutionContext context, DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows) throws DBException
        {
            DBRProgressMonitor monitor = context.getProgressMonitor();
            DBCResultSet dbResult = value;
            try {
                dataReceiver.fetchStart(context, dbResult);
                long rowCount;
                try {
                    rowCount = 0;
                    while (dbResult.nextRow()) {
                        if (monitor.isCanceled()) {
                            // Fetch not more than max rows
                            break;
                        }
                        dataReceiver.fetchRow(context, dbResult);
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
                        dataReceiver.fetchEnd(context);
                    } catch (DBCException e) {
                        log.error("Error while finishing result set fetch", e); //$NON-NLS-1$
                    }
                }
                return rowCount;
            }
            finally {
                //dbResult.close();
            }
        }

        @Override
        public long readDataCount(DBCExecutionContext context, DBDDataFilter dataFilter) throws DBException
        {
            return -1;
        }

        @Override
        public long insertData(DBCExecutionContext context, java.util.List<DBDColumnValue> columns, DBDDataReceiver keysReceiver) throws DBException
        {
            return -1;
        }

        @Override
        public long updateData(DBCExecutionContext context, List<DBDColumnValue> keyColumns, List<DBDColumnValue> updateColumns, DBDDataReceiver keysReceiver) throws DBException
        {
            return -1;
        }

        @Override
        public long deleteData(DBCExecutionContext context, List<DBDColumnValue> keyColumns) throws DBException
        {
            return -1;
        }

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

        @Override
        public DBPDataSource getDataSource()
        {
            final DBDValueController valueController = getValueController();
            return valueController == null ? null : valueController.getDataSource();
        }

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