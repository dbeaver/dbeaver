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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.SmartTextContentAdapter;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Table with editable string rows
 */
public class StringEditorTable {

    public static Table createEditableList(
        @NotNull Composite parent,
        @NotNull String name,
        @Nullable List<String> values,
        @Nullable  DBPImage icon,
        @Nullable IContentProposalProvider proposalProvider)
    {
        Group group = UIUtils.createControlGroup(parent, name, 2, GridData.FILL_BOTH, 0);

        final Table valueTable = new Table(group, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 300;
        gd.heightHint = 100;
        valueTable.setLayoutData(gd);
        // valueTable.setHeaderVisible(true);
        valueTable.setLinesVisible(true);

        final TableColumn valueColumn = UIUtils.createTableColumn(valueTable, SWT.LEFT, UIMessages.properties_value);

        valueTable.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                valueColumn.setWidth(valueTable.getClientArea().width);
            }
        });

        fillFilterValues(valueTable, values, icon);

        final CustomTableEditor tableEditor = new CustomTableEditor(valueTable) {
            {
                firstTraverseIndex = 0;
                lastTraverseIndex = 0;
                //editOnEnter = false;
            }
            @Override
            protected Control createEditor(Table table, int index, TableItem item) {
                Text editor = new Text(table, SWT.BORDER);
                editor.setText(item.getText());
                editor.addModifyListener(e -> {
                    // Save value immediately. This solves MacOS problems with focus events.
                    saveEditorValue(editor, index, item);
                });
                if (proposalProvider != null) {
                    setProposalAdapter(ContentAssistUtils.installContentProposal(
                        editor,
                        new SmartTextContentAdapter(),
                        proposalProvider
                    ));
                }
                return editor;
            }
            @Override
            protected void saveEditorValue(Control control, int index, TableItem item) {
                item.setText(((Text) control).getText().trim());
            }
        };

        Composite buttonsGroup = UIUtils.createPlaceholder(group, 1, 5);
        buttonsGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        final Button addButton = new Button(buttonsGroup, SWT.PUSH);
        addButton.setText(UIMessages.button_add);
        addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button removeButton = new Button(buttonsGroup, SWT.PUSH);
        removeButton.setText(UIMessages.button_remove);
        removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectionIndex = valueTable.getSelectionIndex();
                if (selectionIndex >= 0) {
                    tableEditor.closeEditor();
                    valueTable.remove(selectionIndex);
                    removeButton.setEnabled(valueTable.getSelectionIndex() >= 0);
                }
            }
        });
        removeButton.setEnabled(false);

        final Button clearButton = new Button(buttonsGroup, SWT.PUSH);
        clearButton.setText(UIMessages.button_clear);
        clearButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        clearButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                tableEditor.closeEditor();
                valueTable.removeAll();
                removeButton.setEnabled(false);
            }
        });

        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem newItem = new TableItem(valueTable, SWT.LEFT);
                if (icon != null) {
                    newItem.setImage(DBeaverIcons.getImage(icon));
                }
                valueTable.setSelection(newItem);
                tableEditor.closeEditor();
                tableEditor.showEditor(newItem);
                removeButton.setEnabled(true);
            }
        });

        valueTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectionIndex = valueTable.getSelectionIndex();
                removeButton.setEnabled(selectionIndex >= 0);
            }
        });
        return valueTable;
    }

    public static void fillFilterValues(Table valueTable, List<String> values, DBPImage icon) {
        valueTable.removeAll();
        if (!CommonUtils.isEmpty(values)) {
            for (String value : values) {
                TableItem tableItem = new TableItem(valueTable, SWT.LEFT);
                tableItem.setText(value);
                if (icon != null) {
                    tableItem.setImage(DBeaverIcons.getImage(icon));
                }
            }
        }
    }

    public static List<String> collectValues(Table table) {
        List<String> values = new ArrayList<>();
        for (TableItem item : table.getItems()) {
            String value = item.getText().trim();
            if (value.isEmpty()) { //$NON-NLS-1$
                continue;
            }
            values.add(value);
        }
        return values;
    }
}
