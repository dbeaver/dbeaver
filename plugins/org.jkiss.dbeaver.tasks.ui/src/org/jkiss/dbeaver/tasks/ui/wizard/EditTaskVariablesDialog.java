/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tasks.ui.wizard;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTableEditor;
import org.jkiss.dbeaver.ui.controls.TableColumnSortListener;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * EditTaskVariablesDialog
 */
public class EditTaskVariablesDialog extends StatusDialog {

    private static final String DIALOG_ID = "DBeaver.SQLQueryParameterBindDialog";//$NON-NLS-1$

    private static final Log log = Log.getLog(EditTaskVariablesDialog.class);

    private final Map<String, Object> variables;
    private Table variablesTable;

    public EditTaskVariablesDialog(Shell shell, Map<String, Object> variables) {
        super(shell);
        setTitle("Task variables");

        this.variables = new LinkedHashMap<>(variables);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    public boolean isHelpAvailable() {
        return false;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite composite = (Composite) super.createDialogArea(parent);

        {
            final Composite paramsComposite = UIUtils.createComposite(composite, 1);
            paramsComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

            variablesTable = new Table(paramsComposite, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
            final GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 400;
            gd.heightHint = 200;
            variablesTable.setLayoutData(gd);
            variablesTable.setHeaderVisible(true);
            variablesTable.setLinesVisible(!UIStyles.isDarkTheme());

            final TableColumn nameColumn = UIUtils.createTableColumn(variablesTable, SWT.LEFT, "Variable");
            nameColumn.addListener(SWT.Selection, new TableColumnSortListener(variablesTable, 1));
            nameColumn.setWidth(100);
            final TableColumn valueColumn = UIUtils.createTableColumn(variablesTable, SWT.LEFT, "Value");
            valueColumn.setWidth(300);

            final CustomTableEditor tableEditor = new CustomTableEditor(variablesTable) {
                {
                    firstTraverseIndex = 0;
                    lastTraverseIndex = 1;
                    editOnEnter = false;
                }

                @Override
                protected Control createEditor(Table table, int index, TableItem item) {
                    Text editor = new Text(table, SWT.NONE);
                    editor.setText(item.getText(index));
                    editor.selectAll();

                    editor.addTraverseListener(e -> {
                        if (e.detail == SWT.TRAVERSE_RETURN && (e.stateMask & SWT.CTRL) == SWT.CTRL) {
                            UIUtils.asyncExec(EditTaskVariablesDialog.this::okPressed);
                        }
                    });
                    editor.addModifyListener(e -> saveEditorValue(editor, index, item));

                    return editor;
                }

                @Override
                protected void saveEditorValue(Control control, int index, TableItem item) {
                    String newValue = ((Text) control).getText();
                    item.setText(index, newValue);
                }
            };

            ToolBar toolbar = new ToolBar(composite, SWT.FLAT | SWT.HORIZONTAL);
            final ToolItem newButton = new ToolItem(toolbar, SWT.NONE);
            newButton.setImage(DBeaverIcons.getImage(UIIcon.ROW_ADD));
            ToolItem deleteButton = new ToolItem(toolbar, SWT.NONE);
            deleteButton.setImage(DBeaverIcons.getImage(UIIcon.ROW_DELETE));
            deleteButton.setEnabled(false);

            newButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    TableItem newItem = new TableItem(variablesTable, SWT.NONE);
                    variablesTable.setSelection(newItem);
                    tableEditor.showEditor(newItem);
                    deleteButton.setEnabled(true);
                }
            });
            deleteButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    tableEditor.closeEditor();
                    int selectionIndex = variablesTable.getSelectionIndex();
                    if (selectionIndex >= 0) {
                        variablesTable.remove(selectionIndex);
                        deleteButton.setEnabled(false);
                    }
                }
            });

            variablesTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    deleteButton.setEnabled(variablesTable.getSelectionIndex() >= 0);
                }
            });

            for (Map.Entry<String, Object> param : variables.entrySet()) {
                TableItem item = new TableItem(variablesTable, SWT.NONE);
                item.setText(0, param.getKey());
                item.setText(1, CommonUtils.toString(param.getValue()));
            }
            if (variablesTable.getItemCount() == 0) {
                new TableItem(variablesTable, SWT.NONE);
            }

            tableEditor.showEditor(variablesTable.getItem(0));
        }

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {

        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void okPressed() {
        this.variables.clear();
        for (TableItem item : variablesTable.getItems()) {
            String name = item.getText(0);
            if (!CommonUtils.isEmpty(name)) {
                this.variables.put(name, item.getText(1));
            }
        }
        super.okPressed();
    }

    public Map<String, Object> getVariables() {
        return variables;
    }
}
