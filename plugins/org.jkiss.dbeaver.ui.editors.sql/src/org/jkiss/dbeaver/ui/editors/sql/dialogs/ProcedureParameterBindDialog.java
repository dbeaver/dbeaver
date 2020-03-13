/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTableEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parameter binding
 */
public class ProcedureParameterBindDialog extends StatusDialog {

    private static final String DIALOG_ID = "DBeaver.ProcedureParameterBindDialog";//$NON-NLS-1$

    private DBSProcedure procedure;
    private List<DBSProcedureParameter> parameters;
    private Map<DBSProcedureParameter, Object> values = new HashMap<>();

    public ProcedureParameterBindDialog(Shell shell, DBSProcedure procedure, Map<DBSProcedureParameter, Object> values)
    {
        super(shell);
        this.procedure = procedure;
        this.parameters = new ArrayList<>();

        try {
            this.parameters.addAll(procedure.getParameters(new VoidProgressMonitor()));
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Can't get parameters", "Error getting procedure papameters", e);
        }

        this.values = new HashMap<>(values);
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
        getShell().setText(procedure.getProcedureType().name() + " " + procedure.getName() + " parameter(s)");
        final Composite composite = (Composite)super.createDialogArea(parent);

        final Table paramTable = new Table(composite, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 200;
        paramTable.setLayoutData(gd);
        paramTable.setHeaderVisible(true);
        paramTable.setLinesVisible(true);

        final TableColumn nameColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, "Name");
        nameColumn.setWidth(100);
        final TableColumn valueColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, "Value");
        valueColumn.setWidth(200);
        final TableColumn typeColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, "Type");
        typeColumn.setWidth(60);
        final TableColumn kindColumn = UIUtils.createTableColumn(paramTable, SWT.LEFT, "Kind");
        kindColumn.setWidth(40);

        for (DBSProcedureParameter param : parameters) {
            TableItem item = new TableItem(paramTable, SWT.NONE);
            item.setData(param);
            item.setImage(DBeaverIcons.getImage(DBIcon.TREE_ATTRIBUTE));
            item.setText(0, param.getName());
            Object value = values.get(param);
            item.setText(1, CommonUtils.toString(value, ""));
            item.setText(2, param.getParameterType().getFullTypeName());
            item.setText(3, param.getParameterKind().getTitle());
        }

        final CustomTableEditor tableEditor = new CustomTableEditor(paramTable) {
            {
                firstTraverseIndex = 1;
                lastTraverseIndex = 1;
                editOnEnter = false;
            }
            @Override
            protected Control createEditor(Table table, int index, TableItem item) {
                if (index != 1) {
                    return null;
                }
                DBSProcedureParameter param = (DBSProcedureParameter) item.getData();
                Text editor = new Text(table, SWT.BORDER);
                editor.setText(CommonUtils.toString(values.get(param), ""));
                editor.selectAll();
                return editor;
            }
            @Override
            protected void saveEditorValue(Control control, int index, TableItem item) {
                DBSProcedureParameter param = (DBSProcedureParameter) item.getData();
                String newValue = ((Text) control).getText();
                item.setText(1, newValue);

                values.put(param, newValue);
            }
        };

        if (!parameters.isEmpty()) {
            UIUtils.asyncExec(() -> {
                paramTable.select(0);
                tableEditor.showEditor(paramTable.getItem(0), 1);
            });
        }

        updateStatus(GeneralUtils.makeInfoStatus("Use Tab to switch."));
        return composite;
    }

    @Override
    protected void okPressed()
    {
        super.okPressed();
    }

    public List<DBSProcedureParameter> getParameters() {
        return parameters;
    }

    public Map<DBSProcedureParameter, Object> getValues() {
        return values;
    }
}
