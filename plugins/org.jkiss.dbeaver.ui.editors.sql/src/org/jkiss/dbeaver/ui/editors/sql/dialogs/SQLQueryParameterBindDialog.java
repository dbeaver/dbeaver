/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.dialogs;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.sql.SQLQueryParameter;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLQueryParameterRegistry;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTableEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.List;

/**
 * Parameter binding
 */
public class SQLQueryParameterBindDialog extends StatusDialog {

    private static final String DIALOG_ID = "DBeaver.SQLQueryParameterBindDialog";//$NON-NLS-1$

    private static final String PARAM_HIDE_IF_SET = "PARAM_HIDE_IF_SET";//$NON-NLS-1$

    private List<SQLQueryParameter> parameters;
    private final Map<String, List<SQLQueryParameter>> dupParameters = new HashMap<>();

    private static Map<String, SQLQueryParameterRegistry.ParameterInfo> savedParamValues = new HashMap<>();
    private Button hideIfSetCheck;
    private Table paramTable;

    public SQLQueryParameterBindDialog(Shell shell, List<SQLQueryParameter> parameters)
    {
        super(shell);
        this.parameters = parameters;

        // Restore saved values from registry
        SQLQueryParameterRegistry registry = SQLQueryParameterRegistry.getInstance();
        for (SQLQueryParameter param : this.parameters) {
            if (param.isNamed() && param.getValue() == null) {
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
    public boolean isHelpAvailable() {
        return false;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(SQLEditorMessages.dialog_sql_param_title);
        final Composite composite = (Composite)super.createDialogArea(parent);

        paramTable = new Table(composite, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 200;
        paramTable.setLayoutData(gd);
        paramTable.setHeaderVisible(true);
        paramTable.setLinesVisible(true);

        final TableColumn indexColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, "#");
        indexColumn.setWidth(40);
        final TableColumn nameColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, SQLEditorMessages.dialog_sql_param_column_name);
        nameColumn.setWidth(100);
        final TableColumn valueColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, SQLEditorMessages.dialog_sql_param_column_value);
        valueColumn.setWidth(200);

        fillParameterList(isHideIfSet());

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

                editor.addTraverseListener(e -> {
                    if (e.detail == SWT.TRAVERSE_RETURN && (e.stateMask & SWT.CTRL) == SWT.CTRL) {
                        UIUtils.asyncExec(SQLQueryParameterBindDialog.this::okPressed);
                    }
                });

                return editor;
            }
            @Override
            protected void saveEditorValue(Control control, int index, TableItem item) {
                SQLQueryParameter param = (SQLQueryParameter) item.getData();
                String newValue = ((Text) control).getText();
                item.setText(2, newValue);

                param.setValue(newValue);
                if (param.isNamed()) {
                    final List<SQLQueryParameter> dups = dupParameters.get(param.getName());
                    if (dups != null) {
                        for (SQLQueryParameter dup : dups) {
                            dup.setValue(newValue);
                        }
                    }
                }

                savedParamValues.put(
                    param.getName().toUpperCase(Locale.ENGLISH),
                    new SQLQueryParameterRegistry.ParameterInfo(param.getName(), newValue));
            }
        };

        if (!parameters.isEmpty()) {
            UIUtils.asyncExec(() -> {
                paramTable.select(0);
                tableEditor.showEditor(paramTable.getItem(0), 2);
            });
        }

        hideIfSetCheck = UIUtils.createCheckbox(composite,
            SQLEditorMessages.dialog_sql_param_hide_checkbox, SQLEditorMessages.dialog_sql_param_hide_checkbox_tip,
            isHideIfSet(),
            1);
        hideIfSetCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fillParameterList(hideIfSetCheck.getSelection());
            }
        });

        updateStatus(GeneralUtils.makeInfoStatus(SQLEditorMessages.dialog_sql_param_hint));
        return composite;
    }

    private void fillParameterList(boolean hideVariables) {
        paramTable.removeAll();

        for (SQLQueryParameter param : parameters) {
            if (hideVariables && param.isVariableSet()) {
                continue;
            }
            if (param.getPrevious() != null) {
                // Skip duplicates
                List<SQLQueryParameter> dups = dupParameters.computeIfAbsent(param.getName(), k -> new ArrayList<>());
                dups.add(param);
                continue;
            }
            TableItem item = new TableItem(paramTable, SWT.NONE);
            item.setData(param);
            item.setImage(DBeaverIcons.getImage(DBIcon.TREE_ATTRIBUTE));
            item.setText(0, String.valueOf(param.getOrdinalPosition() + 1));
            item.setText(1, param.getTitle());
            item.setText(2, CommonUtils.notEmpty(param.getValue()));
        }
    }

    @Override
    protected void okPressed()
    {
        SQLQueryParameterRegistry registry = SQLQueryParameterRegistry.getInstance();
        for (SQLQueryParameterRegistry.ParameterInfo param : savedParamValues.values()) {
            registry.setParameter(param.name, param.value);
        }
        registry.save();
        if (hideIfSetCheck != null) {
            getDialogBoundsSettings().put(PARAM_HIDE_IF_SET, hideIfSetCheck.getSelection());
        }
        super.okPressed();
    }

    public static boolean isHideIfSet() {
        return UIUtils.getDialogSettings(DIALOG_ID).getBoolean(PARAM_HIDE_IF_SET);
    }

}
