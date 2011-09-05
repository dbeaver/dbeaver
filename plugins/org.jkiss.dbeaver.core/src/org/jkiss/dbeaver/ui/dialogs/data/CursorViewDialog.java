/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetProvider;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

import java.util.List;

/**
 * TextViewDialog
 */
public class CursorViewDialog extends ValueViewDialog implements ResultSetProvider {

    private DBDCursor value;
    private ResultSetViewer resultSetViewer;
    private CursorDataContainer dataContainer;

    public CursorViewDialog(DBDValueController valueController) {
        super(valueController);
        dataContainer = new CursorDataContainer();
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        DBDValueController valueController = getValueController();
        value = (DBDCursor) valueController.getValue();

        Composite dialogGroup = (Composite)super.createDialogArea(parent);

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

    public DBSDataContainer getDataContainer()
    {
        return dataContainer;
    }

    public boolean isReadyToRun()
    {
        return true;
    }

    private class CursorDataContainer implements DBSDataContainer {

        public int getSupportedFeatures()
        {
            // Nothing but plain read
            return 0;
        }

        public long readData(DBCExecutionContext context, DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows) throws DBException
        {
            return 0;
        }

        public long readDataCount(DBCExecutionContext context, DBDDataFilter dataFilter) throws DBException
        {
            return -1;
        }

        public long insertData(DBCExecutionContext context, java.util.List<DBDColumnValue> columns, DBDDataReceiver keysReceiver) throws DBException
        {
            return -1;
        }

        public long updateData(DBCExecutionContext context, List<DBDColumnValue> keyColumns, List<DBDColumnValue> updateColumns, DBDDataReceiver keysReceiver) throws DBException
        {
            return -1;
        }

        public long deleteData(DBCExecutionContext context, List<DBDColumnValue> keyColumns) throws DBException
        {
            return -1;
        }

        public String getDescription()
        {
            return value.toString();
        }

        public DBSObject getParentObject()
        {
            return null;
        }

        public DBPDataSource getDataSource()
        {
            return getValueController().getDataSource();
        }

        public String getName()
        {
            return value.toString();
        }

        public boolean isPersisted()
        {
            return false;
        }
    }

}