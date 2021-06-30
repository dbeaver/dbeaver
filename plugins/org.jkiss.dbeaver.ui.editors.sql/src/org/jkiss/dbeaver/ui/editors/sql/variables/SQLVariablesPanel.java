/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.editors.sql.variables;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCScriptContext;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.registry.SQLQueryParameterRegistry;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * SQLVariablesPanel
 */
public class SQLVariablesPanel extends Composite {

    static protected final Log log = Log.getLog(SQLVariablesPanel.class);

    private final SQLEditor mainEditor;
    private SQLEditorBase valueEditor;
    private Table varsTable;
    private Button showParametersCheck;

    public SQLVariablesPanel(Composite parent, SQLEditor editor)
    {
        super(parent, SWT.NONE);
        this.mainEditor = editor;

        setLayout(new FillLayout());
    }

    private void createControls() {
        SashForm sash = new SashForm(this, SWT.VERTICAL);

        // Variables table
        {
            Composite varsGroup = UIUtils.createPlaceholder(sash, 1);

            varsTable = new Table(varsGroup, SWT.SINGLE | SWT.FULL_SELECTION);
            varsTable.setHeaderVisible(true);
            varsTable.setLinesVisible(true);
            varsTable.setLayoutData(new GridData(GridData.FILL_BOTH));

            UIUtils.createTableColumn(varsTable, SWT.LEFT, "Variable");
            UIUtils.createTableColumn(varsTable, SWT.LEFT, "Value");
            UIUtils.createTableColumn(varsTable, SWT.LEFT, "Type");

            Composite controlPanel = UIUtils.createComposite(varsGroup, 2);
            controlPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            ToolBar buttonsBar = new ToolBar(controlPanel, SWT.HORIZONTAL);
            buttonsBar.setLayoutData(new GridData(GridData.FILL_VERTICAL));

            ToolItem addButton = UIUtils.createToolItem(buttonsBar, "Add variable", UIIcon.ADD, new SelectionAdapter() {

            });
            ToolItem removeButton = UIUtils.createToolItem(buttonsBar, "Delete variable", UIIcon.DELETE, new SelectionAdapter() {

            });

            showParametersCheck = UIUtils.createCheckbox(controlPanel, "Show parameters", "Show query parameters", false, 1);
            showParametersCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    refreshVariables();
                }
            });

            varsTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    editCurrentVariable();
                }
            });

            //sash.addListener(SWT.Resize, event -> UIUtils.packColumns(varsTable, true));
        }

        // Editor
        {
            Composite editorGroup = UIUtils.createPlaceholder(sash, 1);

            UIUtils.createControlLabel(editorGroup, "Value");

            Composite editorPH = new Composite(editorGroup, SWT.NONE);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.verticalIndent = 3;
            gd.horizontalSpan = 1;
            gd.minimumHeight = 100;
            gd.minimumWidth = 100;
            editorPH.setLayoutData(gd);
            editorPH.setLayout(new FillLayout());

            valueEditor = new SQLEditorBase() {
                @Nullable
                @Override
                public DBCExecutionContext getExecutionContext() {
                    return mainEditor.getExecutionContext();
                }

                @Override
                public void createPartControl(Composite parent) {
                    super.createPartControl(parent);
                    getAction(ITextEditorActionConstants.CONTEXT_PREFERENCES).setEnabled(false);
                }

                @Override
                public boolean isFoldingEnabled() {
                    return false;
                }
            };
            try {
                valueEditor.init(new SubEditorSite(mainEditor.getSite()),
                    new StringEditorInput("Variable value", "", true, GeneralUtils.getDefaultFileEncoding()));
            } catch (PartInitException e) {
                log.error(e);
            }
            valueEditor.createPartControl(editorPH);
            valueEditor.reloadSyntaxRules();

            //valueEditor.getEditorControl().setEnabled(false);

            valueEditor.getEditorControlWrapper().setLayoutData(new GridData(GridData.FILL_BOTH));
        }
    }

    private void editCurrentVariable() {
        int selectionIndex = varsTable.getSelectionIndex();
        StyledText editorControl = valueEditor.getEditorControl();
        if (editorControl == null) {
            return;
        }
        if (selectionIndex >= 0) {
            TableItem item = varsTable.getItem(selectionIndex);
            DBCScriptContext.VariableInfo variable = (DBCScriptContext.VariableInfo) item.getData();

            StringEditorInput sqlInput = new StringEditorInput(
                "Variable " + variable.name,
                CommonUtils.toString(variable.value),
                false,
                GeneralUtils.DEFAULT_ENCODING
                );
            valueEditor.setInput(sqlInput);
            valueEditor.reloadSyntaxRules();
        }
    }

    public void refreshVariables() {
        if (varsTable == null) {
            createControls();
        }

        SQLScriptContext context = mainEditor.getGlobalScriptContext();

        varsTable.removeAll();
        List<DBCScriptContext.VariableInfo> variables = context.getVariables();
        if (showParametersCheck.getSelection()) {
            for (SQLQueryParameterRegistry.ParameterInfo param : SQLQueryParameterRegistry.getInstance().getAllParameters()) {
                if (context.hasVariable(param.name)) {
                    continue;
                }
                Object parameterValue = context.getParameterDefaultValue(param.name);
                if (parameterValue == null) {
                    parameterValue = param.value;
                }
                variables.add(new DBCScriptContext.VariableInfo(
                    param.name,
                    parameterValue,
                    DBCScriptContext.VariableType.PARAMETER));
            }
        }
        for (DBCScriptContext.VariableInfo variable : variables) {
            TableItem ti = new TableItem(varsTable, SWT.NONE);
            ti.setText(0, variable.name);
            ti.setText(1, CommonUtils.toString(variable.value));
            ti.setText(2, variable.type.getTitle());
            ti.setData(variable);
        }

        UIUtils.packColumns(varsTable, true);

        valueEditor.setInput(new StringEditorInput(
            "Variable",
            "",
            true,
            GeneralUtils.DEFAULT_ENCODING
        ));
        valueEditor.reloadSyntaxRules();
    }

}