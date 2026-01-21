/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import java.util.List;

public class StringEditorTableFactory<T> {

    public static final String CUSTOM_EDITABLE_LIST_VALUE_KEY = "CUSTOM_EDITABLE_LIST_VALUE";

    protected final Table valueTable;

    @Nullable
    protected final List<T> values;

    protected final StringEditorTableUtils.TableValuesManager<T> valuesManager;

    @Nullable
    protected final IContentProposalProvider proposalProvider;

    protected final boolean withReordering;
    protected final Runnable buttonsRefresher;
    protected CustomTableEditor tableEditor;
    protected Control addButton;
    protected Control removeButton;

    protected Control clearButton;
    protected Control upButton;
    protected Control downButton;


    public StringEditorTableFactory(
        @NotNull Composite parent,
        @Nullable List<T> values,
        @NotNull StringEditorTableUtils.TableValuesManager<T> valuesManager,
        @Nullable IContentProposalProvider proposalProvider,
        boolean withReordering
    ) {
        this.valueTable = new Table(parent, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        this.values = values;
        this.valuesManager = valuesManager;
        this.proposalProvider = proposalProvider;
        this.withReordering = withReordering;
        this.buttonsRefresher = buttonsRefresher();
    }

    @NotNull
    public Table createTable() {
        setLayout();
        createValueColumn();
        fillItems();
        tableEditor = createTableEditor();
        createRightArea();

        valueTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                buttonsRefresher.run();
            }
        });
        return valueTable;
    }

    @NotNull
    protected Runnable buttonsRefresher() {
        return () -> {
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
    }

    protected void setLayout() {
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 300;
        gd.heightHint = 100;
        valueTable.setLayoutData(gd);
        valueTable.setLinesVisible(true);
    }

    @NotNull
    protected TableColumn createValueColumn() {
        TableColumn valueColumn = UIUtils.createTableColumn(valueTable, SWT.LEFT, UIMessages.properties_value);
        valueTable.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                valueColumn.setWidth(valueTable.getClientArea().width);
            }
        });
        return valueColumn;
    }

    protected void fillItems() {
        valueTable.removeAll();
        if (!CommonUtils.isEmpty(values)) {
            for (T value : values) {
                TableItem tableItem = new TableItem(valueTable, SWT.LEFT);
                tableItem.setText(valuesManager.getString(value));
                tableItem.setData(CUSTOM_EDITABLE_LIST_VALUE_KEY, value);
                DBPImage icon = valuesManager.getIcon(value);
                tableItem.setImage(icon == null ? null : DBeaverIcons.getImage(icon));
            }
        }
    }

    @NotNull
    protected CustomTableEditor createTableEditor() {
        return new CustomTableEditor(valueTable) {
            {
                firstTraverseIndex = 0;
                lastTraverseIndex = 0;
            }

            @Override
            @Nullable
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
    }

    @NotNull
    protected Composite createRightArea() {
        Composite rightArea = UIUtils.createPlaceholder(valueTable.getParent(), 1, 5);
        rightArea.setLayoutData(new GridData(GridData.FILL_VERTICAL));

        Composite buttonsGroup = UIUtils.createPlaceholder(rightArea, 1, 5);
        buttonsGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        addButton = addButton(buttonsGroup);
        removeButton = removeButton(buttonsGroup);
        clearButton = clearButton(buttonsGroup);

        Composite bottomButtonsGroup = UIUtils.createPlaceholder(rightArea, 1, 5);
        bottomButtonsGroup.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, true, true));
        upButton = upButton(bottomButtonsGroup);
        downButton = downButton(bottomButtonsGroup);
        return rightArea;
    }

    @NotNull
    protected Control addButton(@NotNull Composite buttonsGroup) {
        Button addButton = new Button(buttonsGroup, SWT.PUSH);
        addButton.setText(UIMessages.button_add);
        addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem newItem = new TableItem(valueTable, SWT.LEFT);
                addTableItem(newItem);
            }
        });
        return addButton;
    }

    protected void addTableItem(@NotNull TableItem newItem) {
        DBPImage icon = valuesManager.getIcon(null);
        newItem.setImage(icon == null ? null : DBeaverIcons.getImage(icon));
        valueTable.setSelection(newItem);
        tableEditor.closeEditor();
        tableEditor.showEditor(newItem);
        buttonsRefresher.run();
    }

    @NotNull
    protected Control removeButton(@NotNull Composite buttonsGroup) {
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
                    buttonsRefresher.run();
                }
            }
        });
        removeButton.setEnabled(false);
        return removeButton;
    }

    @NotNull
    protected Control clearButton(@NotNull Composite buttonsGroup) {
        final Button clearButton = new Button(buttonsGroup, SWT.PUSH);
        clearButton.setText(UIMessages.button_clear);
        clearButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        clearButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                tableEditor.closeEditor();
                valueTable.removeAll();
                buttonsRefresher.run();
            }
        });
        return clearButton;
    }

    @NotNull
    protected Control upButton(@NotNull Composite buttonsGroup) {
        final Button upButton = new Button(buttonsGroup, SWT.PUSH);
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
                    buttonsRefresher.run();
                }
            }
        });
        upButton.setVisible(withReordering);
        return upButton;
    }

    @NotNull
    protected Control downButton(@NotNull Composite buttonsGroup) {
        final Button downButton = new Button(buttonsGroup, SWT.PUSH);
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
                    buttonsRefresher.run();
                }
            }
        });
        downButton.setVisible(withReordering);
        return downButton;
    }

    @Nullable
    private <T> T getCustomValue(TableItem tableItem) {
        return (T) tableItem.getData(CUSTOM_EDITABLE_LIST_VALUE_KEY);
    }

    private <T> void setCustomValue(TableItem tableItem, T value) {
        tableItem.setData(CUSTOM_EDITABLE_LIST_VALUE_KEY, value);
    }

    public record StringValuesManager(@Nullable DBPImage icon) implements StringEditorTableUtils.TableValuesManager<String> {
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
