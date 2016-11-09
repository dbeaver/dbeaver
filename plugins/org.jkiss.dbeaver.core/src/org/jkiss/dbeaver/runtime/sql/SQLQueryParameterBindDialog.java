/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.runtime.sql;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.sql.SQLQueryParameter;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTableEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parameter binding
 */
public class SQLQueryParameterBindDialog extends StatusDialog {

    private static final String DIALOG_ID = "DBeaver.SQLQueryParameterBindDialog";//$NON-NLS-1$

    private List<SQLQueryParameter> parameters;

    private static Map<String, SQLQueryParameterRegistry.ParameterInfo> savedParamValues = new HashMap<>();

    protected SQLQueryParameterBindDialog(Shell shell, List<SQLQueryParameter> parameters)
    {
        super(shell);
        this.parameters = parameters;

        // Restore saved values from registry
        SQLQueryParameterRegistry registry = SQLQueryParameterRegistry.getInstance();
        for (SQLQueryParameter param : this.parameters) {
            if (param.isNamed()) {
                SQLQueryParameterRegistry.ParameterInfo paramInfo = registry.getParameter(param.getName());
                if (paramInfo != null) {
                    param.setValue(paramInfo.value);
                }
            }
        }
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
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

        final TableColumn indexColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, "#");
        indexColumn.setWidth(40);
        final TableColumn nameColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, "Name");
        nameColumn.setWidth(100);
        final TableColumn valueColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, "Value");
        valueColumn.setWidth(200);

        for (SQLQueryParameter param : parameters) {
            if (param.getPrevious() != null) {
                // Skip duplicates
                continue;
            }
            TableItem item = new TableItem(paramTable, SWT.NONE);
            item.setData(param);
            item.setImage(DBeaverIcons.getImage(DBIcon.TREE_ATTRIBUTE));
            item.setText(0, String.valueOf(param.getOrdinalPosition() + 1));
            item.setText(1, param.getTitle());
            item.setText(2, CommonUtils.notEmpty(param.getValue()));
        }

        final CustomTableEditor tableEditor = new CustomTableEditor(paramTable) {
            {
                firstTraverseIndex = 2;
                lastTraverseIndex = 2;
                editOnEnter = false;
            }
            @Override
            protected Control createEditor(Table table, int index, TableItem item) {
                if (index != 2) {
                    return null;
                }
                SQLQueryParameter param = (SQLQueryParameter) item.getData();
                Text editor = new Text(table, SWT.BORDER);
                editor.setText(CommonUtils.notEmpty(param.getValue()));
                editor.selectAll();
                return editor;
            }
            @Override
            protected void saveEditorValue(Control control, int index, TableItem item) {
                SQLQueryParameter param = (SQLQueryParameter) item.getData();
                String newValue = ((Text) control).getText();
                item.setText(2, newValue);

                if (newValue.isEmpty()) {
                    newValue = null;
                }
                param.setValue(newValue);

                savedParamValues.put(
                    param.getName().toUpperCase(Locale.ENGLISH),
                    new SQLQueryParameterRegistry.ParameterInfo(param.getName(), newValue));
            }
        };

        if (!parameters.isEmpty()) {
            paramTable.select(0);
            tableEditor.showEditor(paramTable.getItem(0), 2);
        }

        updateStatus(GeneralUtils.makeInfoStatus("Use Tab to switch. String values must be quoted. You can use expressions in values"));
        return composite;
    }

    @Override
    protected void okPressed()
    {
        SQLQueryParameterRegistry registry = SQLQueryParameterRegistry.getInstance();
        for (SQLQueryParameterRegistry.ParameterInfo param : savedParamValues.values()) {
            registry.setParameter(param.name, param.value);
        }
        registry.save();
        super.okPressed();
    }

}
