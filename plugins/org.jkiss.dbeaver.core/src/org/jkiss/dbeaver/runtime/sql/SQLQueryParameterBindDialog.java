package org.jkiss.dbeaver.runtime.sql;

import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;

/**
 * Parameter binding
 */
public class SQLQueryParameterBindDialog extends StatusDialog {

    private List<SQLStatementParameter> parameters;

    protected SQLQueryParameterBindDialog(Shell shell, List<SQLStatementParameter> parameters)
    {
        super(shell);
        this.parameters = parameters;
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
        gd.widthHint = 300;
        gd.heightHint = 200;
        paramTable.setLayoutData(gd);
        paramTable.setHeaderVisible(true);
        paramTable.setLinesVisible(true);

        TableEditor tableEditor = new TableEditor(paramTable);
        tableEditor.horizontalAlignment = SWT.LEFT;
        tableEditor.verticalAlignment = SWT.TOP;
        tableEditor.grabHorizontal = true;

        final TableColumn nameColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, "Name");
        nameColumn.setWidth(100);
        final TableColumn typeColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, "Type");
        typeColumn.setWidth(50);
        final TableColumn valueColumn = UIUtils.createTableColumn(paramTable, SWT.RIGHT, "Value");
        valueColumn.setWidth(150);

        for (SQLStatementParameter param : parameters) {
            TableItem item = new TableItem(paramTable, SWT.NONE);
            item.setText(0, param.getTitle());
            item.setText(1, "STRING");
            item.setText(2, "");


        }

        return composite;
    }
}
