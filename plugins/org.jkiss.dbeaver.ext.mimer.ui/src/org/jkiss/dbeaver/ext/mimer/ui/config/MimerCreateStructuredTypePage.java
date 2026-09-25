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
package org.jkiss.dbeaver.ext.mimer.ui.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mimer.model.MimerUserDefinedType;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTableEditor;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * "Create Structured Type" dialog: Name plus an Attributes grid (Name/Data Type/Size/Default).
 * <p>
 * The Data Type cell is a plain, read-only-selection combo, with size collected via its own
 * separate column, rather than a free-typeable cell the user could type {@code TYPE(size)}
 * directly into - an editable combo cell's own type-ahead matching against the dropdown's list
 * items would otherwise snap typed text back to whichever item it prefix-matched, silently
 * discarding a trailing {@code (20)}. Same fix shape as the dedicated "Return type size" field
 * on {@link MimerCreateMethodSpecPage}, applied to a grid cell instead of a single field.
 * <p>
 * Collation per attribute is not offered here - none of the other attribute-bearing create
 * dialogs in this plugin expose it either, and it's rare enough in practice not to be worth the
 * extra grid column. An attribute needing one can still be finished by hand-editing the type's
 * Source-adjacent DDL later. All attributes are collected before the object is created - see
 * {@link MimerUserDefinedTypeConfigurator}.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateStructuredTypePage extends BaseObjectEditPage {

    private static final Log log = Log.getLog(MimerCreateStructuredTypePage.class);

    private final MimerUserDefinedType type;

    private String name = "";
    private final List<AttributeRow> attributeRows = new ArrayList<>();
    private Table attributeTable;
    private CustomTableEditor attributeTableEditor;

    private static class AttributeRow {
        String name = "";
        String dataType = "";
        String size = "";
        String defaultValue = "";
    }

    public MimerCreateStructuredTypePage(@NotNull MimerUserDefinedType type) {
        super("Create structured type");
        this.type = type;
    }

    @Override
    public DBSObject getObject() {
        return type;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(group, "Schema",
            DBUtils.getObjectFullName(type.getParentObject(), DBPEvaluationContext.UI)).setEditable(false);

        Text nameText = UIUtils.createLabelText(group, "Name", "");
        nameText.addModifyListener(e -> {
            name = nameText.getText();
            updatePageState();
        });
        nameText.setFocus();

        createAttributesGrid(group);

        return group;
    }

    private void createAttributesGrid(Composite group) {
        GridData labelGd = new GridData(GridData.FILL_HORIZONTAL);
        labelGd.horizontalSpan = 2;
        UIUtils.createControlLabel(group, "Attributes").setLayoutData(labelGd);

        Composite attrGroup = new Composite(group, SWT.NONE);
        attrGroup.setLayout(new GridLayout(2, false));
        GridData attrGroupGd = new GridData(GridData.FILL_HORIZONTAL);
        attrGroupGd.horizontalSpan = 2;
        attrGroup.setLayoutData(attrGroupGd);

        attributeTable = new Table(attrGroup, SWT.BORDER | SWT.FULL_SELECTION);
        attributeTable.setHeaderVisible(true);
        GridData tableGd = new GridData(GridData.FILL_HORIZONTAL);
        tableGd.heightHint = 120;
        attributeTable.setLayoutData(tableGd);
        UIUtils.createTableColumn(attributeTable, SWT.NONE, "Name").setWidth(120);
        UIUtils.createTableColumn(attributeTable, SWT.NONE, "Data Type").setWidth(140);
        UIUtils.createTableColumn(attributeTable, SWT.NONE, "Size").setWidth(70);
        UIUtils.createTableColumn(attributeTable, SWT.NONE, "Default").setWidth(100);

        attributeTableEditor = new CustomTableEditor(attributeTable) {
            @Override
            protected Control createEditor(Table table, int index, TableItem item) {
                AttributeRow row = (AttributeRow) item.getData();
                if (index == 1) {
                    // Plain, un-editable-suffix combo - size is a separate column rather than
                    // typed into this cell directly (see saveEditorValue below).
                    CCombo combo = new CCombo(table, SWT.DROP_DOWN | SWT.READ_ONLY);
                    for (String t : loadDataTypeNames()) {
                        combo.add(t);
                    }
                    combo.setText(row.dataType);
                    return combo;
                }
                Text text = new Text(table, SWT.BORDER);
                text.setText(switch (index) {
                    case 0 -> row.name;
                    case 2 -> row.size;
                    default -> row.defaultValue;
                });
                text.selectAll();
                return text;
            }

            @Override
            protected void saveEditorValue(Control control, int index, TableItem item) {
                AttributeRow row = (AttributeRow) item.getData();
                if (control instanceof CCombo combo) {
                    row.dataType = combo.getText();
                    item.setText(1, row.dataType);
                } else if (control instanceof Text text) {
                    switch (index) {
                        case 0 -> { row.name = text.getText(); item.setText(0, row.name); }
                        case 2 -> { row.size = text.getText(); item.setText(2, row.size); }
                        default -> { row.defaultValue = text.getText(); item.setText(3, row.defaultValue); }
                    }
                }
                // isPageComplete() depends on the grid contents (at least one fully-filled-in
                // attribute), which only gets re-evaluated when this is called explicitly.
                updatePageState();
            }
        };

        Composite buttons = new Composite(attrGroup, SWT.NONE);
        buttons.setLayout(new GridLayout(1, false));
        buttons.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));
        Button addButton = new Button(buttons, SWT.PUSH);
        addButton.setText(MimerUIMessages.button_add);
        addButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> addAttributeRow()));
        Button removeButton = new Button(buttons, SWT.PUSH);
        removeButton.setText(MimerUIMessages.button_remove);
        removeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> removeSelectedAttributeRow()));
    }

    /**
     * A freshly-added row starts blank, which can read as "the button did nothing" - see {@link
     * MimerCreateMethodSpecPage#addParamRow}. Immediately opening the Name cell's editor makes
     * the new row obvious.
     */
    private void addAttributeRow() {
        AttributeRow row = new AttributeRow();
        attributeRows.add(row);
        TableItem item = new TableItem(attributeTable, SWT.NONE);
        item.setData(row);
        item.setText(0, row.name);
        item.setText(1, row.dataType);
        item.setText(2, row.size);
        item.setText(3, row.defaultValue);
        attributeTable.setSelection(item);
        attributeTableEditor.showEditor(item, 0);
        updatePageState();
    }

    private void removeSelectedAttributeRow() {
        int index = attributeTable.getSelectionIndex();
        if (index >= 0) {
            attributeRows.remove(index);
            attributeTable.remove(index);
            updatePageState();
        }
    }

    @NotNull
    private String[] loadDataTypeNames() {
        try {
            Set<String> names = new LinkedHashSet<>();
            for (DBSObject t : type.getDataSource().getDataTypes(new VoidProgressMonitor())) {
                names.add(t.getName().replaceAll("\\(.*\\)", "").trim());
            }
            return names.toArray(new String[0]);
        } catch (Exception e) {
            log.debug("Can't load data type list for the create-structured-type dialog", e);
        }
        return new String[0];
    }

    @Override
    public boolean isPageComplete() {
        if (CommonUtils.isEmptyTrimmed(name) || attributeRows.isEmpty()) {
            return false;
        }
        for (AttributeRow row : attributeRows) {
            if (CommonUtils.isEmptyTrimmed(row.name) || CommonUtils.isEmptyTrimmed(row.dataType)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Applies the collected values to the type - see {@link
     * MimerUserDefinedType#setAttributesBody}. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        type.setName(name.trim());

        StringBuilder sb = new StringBuilder();
        for (AttributeRow row : attributeRows) {
            if (CommonUtils.isEmptyTrimmed(row.name) || CommonUtils.isEmptyTrimmed(row.dataType)) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(",\n");
            }
            sb.append('"').append(row.name.trim()).append("\" ").append(row.dataType.trim());
            if (!CommonUtils.isEmptyTrimmed(row.size)) {
                sb.append('(').append(row.size.trim()).append(')');
            }
            if (!CommonUtils.isEmptyTrimmed(row.defaultValue)) {
                sb.append(" DEFAULT ").append(row.defaultValue.trim());
            }
        }
        type.setAttributesBody(sb.toString());
    }
}
