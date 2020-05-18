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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.connection.DataSourceVariableResolver;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTableEditor;
import org.jkiss.dbeaver.ui.controls.VariablesHintLabel;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.ui.internal.UIMessages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Object filter edit dialog
 */
public class EditBootstrapQueriesDialog extends HelpEnabledDialog {

    public static final int SHOW_GLOBAL_FILTERS_ID = 1000;
    private static final String DIALOG_ID = "DBeaver.EditBootstrapQueriesDialog";

    private DataSourceDescriptor dataSourceDescriptor;
    private List<String> queries;
    private boolean ignoreErrors;
    private Table queriesTable;
    private Button ignoreErrorButton;

    public EditBootstrapQueriesDialog(Shell shell, DataSourceDescriptor dataSourceDescriptor, Collection<String> queries, boolean ignoreErrors) {
        super(shell, IHelpContextIds.CTX_EDIT_OBJECT_FILTERS);
        this.dataSourceDescriptor = dataSourceDescriptor;
        this.queries = new ArrayList<>(queries);
        this.ignoreErrors = ignoreErrors;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        getShell().setText(CoreMessages.dialog_connection_edit_wizard_general_bootstrap_query_title);

        Composite composite = (Composite) super.createDialogArea(parent);

        Group group = UIUtils.createControlGroup(composite, CoreMessages.dialog_connection_edit_wizard_general_bootstrap_query_sql_label, 2, GridData.FILL_BOTH, 0);

        queriesTable = new Table(group, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 300;
        gd.heightHint = 100;
        queriesTable.setLayoutData(gd);
        queriesTable.setLinesVisible(true);
        final TableColumn valueColumn = UIUtils.createTableColumn(queriesTable, SWT.LEFT, "SQL");
        valueColumn.setWidth(300);

        for (String value : queries) {
            new TableItem(queriesTable, SWT.LEFT).setText(value);
        }

        final CustomTableEditor tableEditor = new CustomTableEditor(queriesTable) {
            @Override
            protected Control createEditor(Table table, int index, TableItem item) {
                Text editor = new Text(table, SWT.BORDER);
                editor.setText(item.getText());
                return editor;
            }
            @Override
            protected void saveEditorValue(Control control, int index, TableItem item) {
                item.setText(((Text) control).getText());
            }
        };

        Composite buttonsGroup = UIUtils.createPlaceholder(group, 1, 5);
        buttonsGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        final Button addButton = new Button(buttonsGroup, SWT.PUSH);
        addButton.setText(UIMessages.button_add);
        addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                tableEditor.closeEditor();
                String sql = EditTextDialog.editText(getShell(), CoreMessages.dialog_connection_edit_wizard_general_bootstrap_query_sql_title, "");
                if (sql != null) {
                    TableItem newItem = new TableItem(queriesTable, SWT.LEFT);
                    newItem.setText(sql);
                    queriesTable.setSelection(newItem);
                    UIUtils.packColumns(queriesTable, true);
                }
            }
        });

        final Button removeButton = new Button(buttonsGroup, SWT.PUSH);
        removeButton.setText(UIMessages.button_remove);
        removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectionIndex = queriesTable.getSelectionIndex();
                if (selectionIndex >= 0) {
                    tableEditor.closeEditor();
                    queriesTable.remove(selectionIndex);
                    removeButton.setEnabled(queriesTable.getSelectionIndex() >= 0);
                }
            }
        });
        removeButton.setEnabled(false);

        queriesTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectionIndex = queriesTable.getSelectionIndex();
                removeButton.setEnabled(selectionIndex >= 0);
            }
        });
        queriesTable.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                int sbWidth = 0;
                if (queriesTable.getVerticalBar() != null) {
                    sbWidth = queriesTable.getVerticalBar().getSize().x;
                }
                queriesTable.getColumn(0).setWidth(queriesTable.getSize().x - queriesTable.getBorderWidth() * 2 - sbWidth);
            }
        });

        ignoreErrorButton = UIUtils.createCheckbox(composite, CoreMessages.dialog_connection_edit_wizard_general_bootstrap_query_ignore_error_lable, ignoreErrors);
        VariablesHintLabel variablesHintLabel = new VariablesHintLabel(
            composite,
            CoreMessages.dialog_connection_edit_wizard_shell_cmd_variables_hint_label,
            CoreMessages.dialog_connection_edit_wizard_shell_cmd_variables_hint_title,
            DataSourceDescriptor.CONNECT_VARIABLES);
        if (dataSourceDescriptor != null) {
            variablesHintLabel.setResolver(new DataSourceVariableResolver(dataSourceDescriptor, dataSourceDescriptor.getConnectionConfiguration()));
        }

        UIUtils.asyncExec(() -> UIUtils.packColumns(queriesTable, true));

        return composite;
    }

    private List<String> collectValues(Table table) {
        List<String> values = new ArrayList<>();
        for (TableItem item : table.getItems()) {
            String value = item.getText().trim();
            if (value.isEmpty() || value.equals("%")) { //$NON-NLS-1$
                continue;
            }
            values.add(value);
        }
        return values;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getSettingsSection(DBeaverActivator.getInstance().getDialogSettings(), DIALOG_ID);
    }

    @Override
    protected void okPressed() {
        ignoreErrors = ignoreErrorButton.getSelection();

        queries = collectValues(queriesTable);
        super.okPressed();
    }

    @Override
    protected void cancelPressed() {
        super.cancelPressed();
    }

    public List<String> getQueries() {
        return queries;
    }

    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }
}
