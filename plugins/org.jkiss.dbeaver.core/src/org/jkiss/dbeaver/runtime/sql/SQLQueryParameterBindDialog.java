/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataTypeProviderDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Parameter binding
 */
public class SQLQueryParameterBindDialog extends StatusDialog {

    private DBPDataSource dataSource;
    private List<SQLStatementParameter> parameters;
    private List<DBSDataType> validDataTypes = new ArrayList<DBSDataType>();

    protected SQLQueryParameterBindDialog(Shell shell, DBPDataSource dataSource, List<SQLStatementParameter> parameters)
    {
        super(shell);
        this.dataSource = dataSource;
        this.parameters = parameters;

        if (dataSource instanceof DBPDataTypeProvider) {
            for (DBSDataType dataType : ((DBPDataTypeProvider)dataSource).getDataTypes()) {
                switch (dataType.getDataKind()) {
                    case UNKNOWN:
                    case LOB:
                    case BINARY:
                        continue;
                }
                final DataTypeProviderDescriptor dataTypeProvider = DataSourceProviderRegistry.getDefault().getDataTypeProvider(dataSource, dataType.getName(), dataType.getValueType());
                if (dataTypeProvider != null) {
                    validDataTypes.add(dataType);
                }
                //dataTypeProvider.getInstance().getHandler(dataType.getName(), dataType.getValueType());
            }
        }

        for (SQLStatementParameter param : this.parameters) {
            final DBSDataType dataType = DBUtils.findBestDataType(((DBPDataTypeProvider) dataSource).getDataTypes(), DBConstants.DEFAULT_DATATYPE_NAMES);
            if (dataType != null) {
                param.setParamType(dataType);
                param.resolve();
            }
        }
    }

    protected boolean isResizable()
    {
        return true;
    }


    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Bind parameter(s)");
        final Composite composite = (Composite)super.createDialogArea(parent);

        Table paramTable = new Table(composite, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 200;
        paramTable.setLayoutData(gd);
        paramTable.setHeaderVisible(true);
        paramTable.setLinesVisible(true);

        TableEditor tableEditor = new TableEditor(paramTable);
        tableEditor.horizontalAlignment = SWT.LEFT;
        tableEditor.verticalAlignment = SWT.TOP;
        tableEditor.grabHorizontal = true;

        final TableColumn indexColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, "#");
        indexColumn.setWidth(30);
        final TableColumn nameColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, "Name");
        nameColumn.setWidth(100);
        final TableColumn typeColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, "Type");
        typeColumn.setWidth(70);
        final TableColumn valueColumn = UIUtils.createTableColumn(paramTable, SWT.RIGHT, "Value");
        valueColumn.setWidth(200);

        for (SQLStatementParameter param : parameters) {
            TableItem item = new TableItem(paramTable, SWT.NONE);
            item.setData(param);
            item.setText(0, String.valueOf(param.getIndex() + 1));
            item.setText(1, param.getTitle());
            item.setText(2, CommonUtils.toString(param.getTypeName()));
            item.setText(3, CommonUtils.toString(param.getValue()));


        }

        return composite;
    }

    private class ParametersMouseListener implements MouseListener {
        private final TableEditor tableEditor;
        private final Table columnsTable;

        public ParametersMouseListener(TableEditor tableEditor, Table columnsTable)
        {
            this.tableEditor = tableEditor;
            this.columnsTable = columnsTable;
        }

        private void disposeOldEditor()
        {
            Control oldEditor = tableEditor.getEditor();
            if (oldEditor != null) oldEditor.dispose();
        }

        public void mouseDoubleClick(MouseEvent e)
        {
            //handleColumnClick(e, true);
        }

        public void mouseDown(MouseEvent e)
        {
        }

        public void mouseUp(MouseEvent e)
        {
            handleColumnClick(e);
        }

        private void handleColumnClick(MouseEvent e) {
            // Clean up any previous editor control
            disposeOldEditor();

            TableItem item = columnsTable.getItem(new Point(e.x, e.y));
            if (item == null) {
                return;
            }
            int columnIndex = UIUtils.getColumnAtPos(item, e.x, e.y);
            if (columnIndex <= 0) {
                return;
            }
            if (columnIndex == 1) {
                showTypeSelector(item);
            } else if (columnIndex == 2) {
                showEditor(item);
            }
        }

        private void showTypeSelector(TableItem item)
        {
        }

        private void showEditor(final TableItem item) {
            SQLStatementParameter param = (SQLStatementParameter)item.getData();
            if (!param.isResolved()) {
                return;
            }
            final DBDValueHandler valueHandler = param.getValueHandler();
            //valueHandler.editValue()
            //tableEditor.setEditor(control, item, 2);
        }
    }

}
