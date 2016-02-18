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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTableEditor;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Object filter edit dialog
 */
public class EditBootstrapQueriesDialog extends HelpEnabledDialog {

    public static final int SHOW_GLOBAL_FILTERS_ID = 1000;

    private List<String> queries;
    private boolean ignoreErrors;
    private Table queriesTable;
    private Button ignoreErrorButton;

    public EditBootstrapQueriesDialog(Shell shell, Collection<String> queries, boolean ignoreErrors) {
        super(shell, IHelpContextIds.CTX_EDIT_OBJECT_FILTERS);
        this.queries = new ArrayList<>(queries);
        this.ignoreErrors = ignoreErrors;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        getShell().setText("Bootstrap SQL queries");

        Composite composite = (Composite) super.createDialogArea(parent);

        Group group = UIUtils.createControlGroup(composite, "SQL Queries", 2, GridData.FILL_BOTH, 0);

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
        addButton.setText(CoreMessages.dialog_filter_button_add);
        addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                tableEditor.closeEditor();
                String sql = EditTextDialog.editText(getShell(), "Enter SQL", "");
                if (sql != null) {
                    TableItem newItem = new TableItem(queriesTable, SWT.LEFT);
                    newItem.setText(sql);
                    queriesTable.setSelection(newItem);
                    UIUtils.packColumns(queriesTable, true);
                }
            }
        });

        final Button removeButton = new Button(buttonsGroup, SWT.PUSH);
        removeButton.setText(CoreMessages.dialog_filter_button_remove);
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

        ignoreErrorButton = UIUtils.createCheckbox(composite, "Ignore SQL errors", ignoreErrors);

        UIUtils.packColumns(queriesTable, true);

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
