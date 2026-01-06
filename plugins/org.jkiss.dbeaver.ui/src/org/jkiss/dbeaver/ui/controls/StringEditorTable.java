/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.ui.UIIcon;
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

    private static final String CUSTOM_EDITABLE_LIST_VALUE_KEY = "CUSTOM_EDITABLE_LIST_VALUE";

    /**
     * Creates the panel to manage list of string values
     */
    public static Table createEditableList(
        @NotNull Composite parent,
        @NotNull String name,
        @Nullable List<String> values,
        @Nullable DBPImage icon,
        @Nullable IContentProposalProvider proposalProvider
    ) {
        return createCustomEditableList(
            parent, name, values, new StringValuesManager(icon), proposalProvider, false
        );
    }

    /**
     * Creates the panel to manage list of custom values
     */
    public static <T> Table createCustomEditableList(
        @NotNull Composite parent,
        @NotNull String name,
        @Nullable List<T> values,
        @NotNull TableValuesManager<T> valuesManager,
        @Nullable IContentProposalProvider proposalProvider,
        boolean withReordering
    ) {
        Group group = UIUtils.createControlGroup(parent, name, 2, GridData.FILL_BOTH, 0);

        final Table valueTable = new Table(group, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 300;
        gd.heightHint = 100;
        valueTable.setLayoutData(gd);
        valueTable.setLinesVisible(true);

        final TableColumn valueColumn = UIUtils.createTableColumn(valueTable, SWT.LEFT, UIMessages.properties_value);

        valueTable.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                valueColumn.setWidth(valueTable.getClientArea().width);
            }
        });

        valueTable.removeAll();
        if (!CommonUtils.isEmpty(values)) {
            for (T value : values) {
                TableItem tableItem = new TableItem(valueTable, SWT.LEFT);
                tableItem.setText(valuesManager.getString(value));
                setCustomValue(tableItem, value);
                DBPImage icon = valuesManager.getIcon(value);
                tableItem.setImage(icon == null ? null : DBeaverIcons.getImage(icon));
            }
        }

        final CustomTableEditor tableEditor = new CustomTableEditor(valueTable) {
            {
                firstTraverseIndex = 0;
                lastTraverseIndex = 0;
            }

            @Override
            protected Control createEditor(Table table, int index, TableItem item) {
                Text editor;
                if (valuesManager.isEditable(getCustomValue(item))) {
                    editor = new Text(table, SWT.BORDER);
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
                } else {
                    editor = null;
                }
                return editor;
            }

            @Override
            protected void saveEditorValue(Control control, int index, TableItem item) {
                String text = ((Text) control).getText().trim();
                T value = valuesManager.prepareNewValue(getCustomValue(item), text);
                if (value != null) {
                    setCustomValue(item, value);
                    item.setText(valuesManager.getString(value));
                    DBPImage icon = valuesManager.getIcon(value);
                    item.setImage(icon == null ? null : DBeaverIcons.getImage(icon));
                }
            }
        };

        Composite rightArea = UIUtils.createPlaceholder(group, 1, 5);
        rightArea.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        var buttonsRefresher = new Object() {
            public Runnable refreshButtons;
        };

        Composite buttonsGroup = UIUtils.createPlaceholder(rightArea, 1, 5);
        buttonsGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        final Button addButton = new Button(buttonsGroup, SWT.PUSH);
        addButton.setText(UIMessages.button_add);
        addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem newItem = new TableItem(valueTable, SWT.LEFT);
                DBPImage icon = valuesManager.getIcon(null);
                newItem.setImage(icon == null ? null : DBeaverIcons.getImage(icon));
                valueTable.setSelection(newItem);
                tableEditor.closeEditor();
                tableEditor.showEditor(newItem);
                buttonsRefresher.refreshButtons.run();
            }
        });

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
                    buttonsRefresher.refreshButtons.run();
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
                buttonsRefresher.refreshButtons.run();
            }
        });

        Composite bottomButtonsGroup = UIUtils.createPlaceholder(rightArea, 1, 5);
        bottomButtonsGroup.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, true, true));

        final Button upButton = new Button(bottomButtonsGroup, SWT.PUSH);
        upButton.setImage(DBeaverIcons.getImage(UIIcon.ARROW_UP));
        upButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        upButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectionIndex = valueTable.getSelectionIndex();
                if (selectionIndex >= 1 && selectionIndex < valueTable.getItemCount()) {
                    T value = getCustomValue(valueTable.getItem(selectionIndex));
                    valueTable.remove(selectionIndex);
                    TableItem tableItem = new TableItem(valueTable, SWT.LEFT, selectionIndex - 1);
                    tableItem.setText(valuesManager.getString(value));
                    setCustomValue(tableItem, value);
                    DBPImage icon = valuesManager.getIcon(value);
                    tableItem.setImage(icon == null ? null : DBeaverIcons.getImage(icon));
                    valueTable.setSelection(selectionIndex - 1);
                    buttonsRefresher.refreshButtons.run();
                }
            }
        });
        upButton.setVisible(withReordering);

        final Button downButton = new Button(bottomButtonsGroup, SWT.PUSH);
        downButton.setImage(DBeaverIcons.getImage(UIIcon.ARROW_DOWN));
        downButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        downButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectionIndex = valueTable.getSelectionIndex();
                if (selectionIndex >= 0 && selectionIndex < valueTable.getItemCount() - 1) {
                    T value = getCustomValue(valueTable.getItem(selectionIndex));
                    valueTable.remove(selectionIndex);
                    TableItem tableItem = new TableItem(valueTable, SWT.LEFT, selectionIndex + 1);
                    tableItem.setText(valuesManager.getString(value));
                    setCustomValue(tableItem, value);
                    DBPImage icon = valuesManager.getIcon(value);
                    tableItem.setImage(icon == null ? null : DBeaverIcons.getImage(icon));
                    valueTable.setSelection(selectionIndex + 1);
                    buttonsRefresher.refreshButtons.run();
                }
            }
        });
        downButton.setVisible(withReordering);

        valueTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                buttonsRefresher.refreshButtons.run();
            }
        });

        buttonsRefresher.refreshButtons = () -> {
            int selectionIndex = valueTable.getSelectionIndex();
            if (selectionIndex < 0) {
                removeButton.setEnabled(false);
                upButton.setEnabled(false);
                downButton.setEnabled(false);
            } else {
                removeButton.setEnabled(true);
                upButton.setEnabled(selectionIndex > 0);
                downButton.setEnabled(selectionIndex < valueTable.getItemCount() - 1);
            }
        };

        return valueTable;
    }


    /**
     * Replaces all the values in the Table with the new collection of strings
     */
    public static void replaceAllStringValues(Table valueTable, List<String> values, DBPImage icon) {
        valueTable.removeAll();
        if (!CommonUtils.isEmpty(values)) {
            for (String value : values) {
                TableItem tableItem = new TableItem(valueTable, SWT.LEFT);
                tableItem.setText(value);
                setCustomValue(tableItem, value);
                if (icon != null) {
                    tableItem.setImage(DBeaverIcons.getImage(icon));
                }
            }
        }
    }

    /**
     * Returns collection of strings from the Table
     */
    public static List<String> collectStringValues(Table table) {
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

    /**
     * Returns collection of custom values from the Table
     */
    public static <T> List<T> collectCustomValues(@NotNull Table table) {
        List<T> values = new ArrayList<>(table.getItemCount());
        for (TableItem item : table.getItems()) {
            T value = getCustomValue(item);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private static <T> T getCustomValue(TableItem tableItem) {
        return (T) tableItem.getData(CUSTOM_EDITABLE_LIST_VALUE_KEY);
    }

    private static <T> void setCustomValue(TableItem tableItem, T value) {
        tableItem.setData(CUSTOM_EDITABLE_LIST_VALUE_KEY, value);
    }

    /**
     * Manager of the custom values handled by StringEditorTable
     */
    public interface TableValuesManager<T> {
        /**
         * Returns the icon for the list element
         */
        @Nullable
        DBPImage getIcon(@Nullable T value);

        /**
         * Returns the string representation of the value
         */
        @NotNull
        String getString(@Nullable T value);

        /**
         * Checks if the string representation of the value is editable
         */
        @NotNull
        Boolean isEditable(@Nullable T value);

        /**
         * Returns a new instance of the value as a result of editing operation
         */
        @Nullable
        T prepareNewValue(@Nullable T originalValue, @Nullable String string);
    }

    private record StringValuesManager(@Nullable DBPImage icon) implements TableValuesManager<String> {
        @Nullable
        @Override
        public DBPImage getIcon(@Nullable String value) {
            return icon;
        }

        @NotNull
        @Override
        public String getString(@Nullable String value) {
            return value == null ? "" : value;
        }

        @NotNull
        @Override
        public Boolean isEditable(@Nullable String value) {
            return true;
        }

        @Nullable
        @Override
        public String prepareNewValue(@Nullable String originalValue, @Nullable String string) {
            return string;
        }
    }
}
