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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mimer.model.MimerUdtMethodSpec;
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
 * "Add Method Specification" dialog for Mimer SQL user-defined types - collects the signature
 * only (name for instance/static, Specific Name, Return Data Type for instance/static, a
 * Parameters grid, Deterministic, and Access Option). A constructor method has a fixed name (the
 * type's own name) and fixed return type (the type itself, per {@code CREATE_TYPE.htm}), so both
 * fields are shown read-only for that kind rather than editable. The actual body is written afterward in the
 * Source tab that opens once this closes ({@code FEATURE_EDITOR_ON_CREATE}) - see {@link
 * org.jkiss.dbeaver.ext.mimer.edit.MimerUdtMethodSpecManager}.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateMethodSpecPage extends BaseObjectEditPage {

    private static final Log log = Log.getLog(MimerCreateMethodSpecPage.class);

    private final MimerUdtMethodSpec spec;
    private final boolean constructor;

    private String methodName;
    private String specificName = "";
    private String returnType;
    private String returnTypeSize = "";
    private String deterministic = "DETERMINISTIC";
    private String accessOption = "CONTAINS SQL";
    private final List<ParamRow> paramRows = new ArrayList<>();
    private Table paramTable;
    private CustomTableEditor paramTableEditor;

    private static class ParamRow {
        String name = "";
        String type = "";
        String size = "";
    }

    public MimerCreateMethodSpecPage(@NotNull MimerUdtMethodSpec spec) {
        super("Add method specification");
        this.spec = spec;
        this.constructor = "CONSTRUCTOR METHOD".equals(spec.getMethodKind());
        this.methodName = constructor ? spec.getType().getName() : "";
        this.returnType = constructor
            ? "\"" + spec.getType().getSchema().getName() + "\".\"" + spec.getType().getName() + "\""
            : "";
    }

    @Override
    public DBSObject getObject() {
        return spec;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(group, "Type",
            DBUtils.getObjectFullName(spec.getType(), DBPEvaluationContext.UI)).setEditable(false);

        if (constructor) {
            UIUtils.createLabelText(group, "Method name", methodName).setEditable(false);
        } else {
            Text nameText = UIUtils.createLabelText(group, "Method name", "");
            nameText.addModifyListener(e -> {
                methodName = nameText.getText();
                updatePageState();
            });
            nameText.setFocus();
        }

        Text specificText = UIUtils.createLabelText(group, "Specific name", "");
        specificText.setMessage(MimerUIMessages.page_create_method_spec_specific_name_placeholder);
        specificText.addModifyListener(e -> specificName = specificText.getText());

        if (constructor) {
            UIUtils.createLabelText(group, "Return data type", returnType).setEditable(false);
        } else {
            Combo returnTypeCombo = UIUtils.createLabelCombo(group, "Return data type", SWT.DROP_DOWN);
            for (String t : loadDataTypeNames()) {
                returnTypeCombo.add(t);
            }
            returnTypeCombo.addModifyListener(e -> {
                returnType = returnTypeCombo.getText();
                updatePageState();
            });

            Text sizeText = UIUtils.createLabelText(group, "Return type size", "");
            sizeText.setMessage(MimerUIMessages.page_create_method_spec_return_size_placeholder);
            sizeText.addModifyListener(e -> returnTypeSize = sizeText.getText());
        }

        createParametersGrid(group);

        Combo deterministicCombo = UIUtils.createLabelCombo(group, "Deterministic", SWT.DROP_DOWN | SWT.READ_ONLY);
        deterministicCombo.add("DETERMINISTIC");
        deterministicCombo.add("NOT DETERMINISTIC");
        deterministicCombo.select(0);
        deterministicCombo.addModifyListener(e -> deterministic = deterministicCombo.getText());

        Combo accessCombo = UIUtils.createLabelCombo(group, "Access option", SWT.DROP_DOWN | SWT.READ_ONLY);
        accessCombo.add("CONTAINS SQL");
        accessCombo.add("READS SQL DATA");
        accessCombo.add("MODIFIES SQL DATA");
        accessCombo.select(0);
        accessCombo.addModifyListener(e -> accessOption = accessCombo.getText());

        return group;
    }

    private void createParametersGrid(Composite group) {
        GridData labelGd = new GridData(GridData.FILL_HORIZONTAL);
        labelGd.horizontalSpan = 2;
        UIUtils.createControlLabel(group, "Parameters").setLayoutData(labelGd);

        Composite paramGroup = new Composite(group, SWT.NONE);
        paramGroup.setLayout(new GridLayout(2, false));
        GridData paramGroupGd = new GridData(GridData.FILL_HORIZONTAL);
        paramGroupGd.horizontalSpan = 2;
        paramGroup.setLayoutData(paramGroupGd);

        paramTable = new Table(paramGroup, SWT.BORDER | SWT.FULL_SELECTION);
        paramTable.setHeaderVisible(true);
        GridData tableGd = new GridData(GridData.FILL_HORIZONTAL);
        tableGd.heightHint = 120;
        paramTable.setLayoutData(tableGd);
        UIUtils.createTableColumn(paramTable, SWT.NONE, "Name").setWidth(120);
        UIUtils.createTableColumn(paramTable, SWT.NONE, "Type").setWidth(160);
        UIUtils.createTableColumn(paramTable, SWT.NONE, "Size").setWidth(70);

        paramTableEditor = new CustomTableEditor(paramTable) {
            @Override
            protected Control createEditor(Table table, int index, TableItem item) {
                ParamRow row = (ParamRow) item.getData();
                if (index == 1) {
                    // Read-only selection; size is collected via its own column instead - typing
                    // a trailing (size) directly into an editable combo cell gets silently eaten
                    // by CCombo's own type-ahead matching (same issue fixed the same way for the
                    // attribute-grid cell in MimerCreateStructuredTypePage).
                    CCombo combo = new CCombo(table, SWT.DROP_DOWN | SWT.READ_ONLY);
                    for (String t : loadDataTypeNames()) {
                        combo.add(t);
                    }
                    combo.setText(row.type);
                    return combo;
                }
                Text text = new Text(table, SWT.BORDER);
                text.setText(index == 0 ? row.name : row.size);
                text.selectAll();
                return text;
            }

            @Override
            protected void saveEditorValue(Control control, int index, TableItem item) {
                ParamRow row = (ParamRow) item.getData();
                if (control instanceof CCombo combo) {
                    row.type = combo.getText();
                    item.setText(1, row.type);
                } else if (control instanceof Text text) {
                    if (index == 0) {
                        row.name = text.getText();
                        item.setText(0, row.name);
                    } else {
                        row.size = text.getText();
                        item.setText(2, row.size);
                    }
                }
            }
        };

        Composite buttons = new Composite(paramGroup, SWT.NONE);
        buttons.setLayout(new GridLayout(1, false));
        buttons.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));
        Button addButton = new Button(buttons, SWT.PUSH);
        addButton.setText(MimerUIMessages.button_add);
        addButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> addParamRow()));
        Button removeButton = new Button(buttons, SWT.PUSH);
        removeButton.setText(MimerUIMessages.button_remove);
        removeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> removeSelectedParamRow()));
    }

    /**
     * A freshly-added row starts blank, which can look like the button did nothing. Immediately
     * opening the Name cell's editor makes the new row obvious and lets the user start typing
     * right away, without an extra click.
     */
    private void addParamRow() {
        ParamRow row = new ParamRow();
        paramRows.add(row);
        TableItem item = new TableItem(paramTable, SWT.NONE);
        item.setData(row);
        item.setText(0, row.name);
        item.setText(1, row.type);
        item.setText(2, row.size);
        paramTable.setSelection(item);
        paramTableEditor.showEditor(item, 0);
    }

    private void removeSelectedParamRow() {
        int index = paramTable.getSelectionIndex();
        if (index >= 0) {
            paramRows.remove(index);
            paramTable.remove(index);
        }
    }

    @NotNull
    private String[] loadDataTypeNames() {
        try {
            Set<String> names = new LinkedHashSet<>();
            for (DBSObject t : spec.getDataSource().getDataTypes(new VoidProgressMonitor())) {
                names.add(t.getName().replaceAll("\\(.*\\)", "").trim());
            }
            return names.toArray(new String[0]);
        } catch (Exception e) {
            log.debug("Can't load data type list for the add-method-specification dialog", e);
        }
        return new String[0];
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(methodName) && !CommonUtils.isEmptyTrimmed(returnType);
    }

    /**
     * Applies the collected values to the spec. Call after {@link #edit()} returns {@code true}.
     * <p>
     * A specific name is always generated, even when the field is left blank - {@code
     * MimerUdtMethodSpec#buildCreateBodyDDL} needs the real specific name to supply the body via
     * {@code CREATE SPECIFIC METHOD "s"."specificName"}, and there is no way to read back
     * whatever name Mimer SQL would auto-assign between the {@code ALTER TYPE ... ADD METHOD} action
     * running and the Source tab opening. Same approach as {@code MimerTableColumnManager}'s
     * auto-increment sequence naming - generate the name locally rather than rely on the server.
     */
    public void applyChanges() {
        spec.setName(methodName.trim());
        spec.setSpecificName(CommonUtils.isEmptyTrimmed(specificName) ? autoSpecificName() : specificName.trim());
        spec.setReturnDataType(CommonUtils.isEmptyTrimmed(returnTypeSize)
            ? returnType.trim() : returnType.trim() + "(" + returnTypeSize.trim() + ")");
        spec.setDeterministic("DETERMINISTIC".equals(deterministic));
        spec.setAccessOption(accessOption);

        StringBuilder paramList = new StringBuilder();
        for (ParamRow row : paramRows) {
            if (CommonUtils.isEmptyTrimmed(row.name) || CommonUtils.isEmptyTrimmed(row.type)) {
                continue;
            }
            if (!paramList.isEmpty()) {
                paramList.append(", ");
            }
            paramList.append('"').append(row.name.trim()).append("\" ").append(row.type.trim());
            if (!CommonUtils.isEmptyTrimmed(row.size)) {
                paramList.append('(').append(row.size.trim()).append(')');
            }
        }
        spec.setParameterList(paramList.toString());
    }

    @NotNull
    private String autoSpecificName() {
        return methodName.trim() + "_" + (System.currentTimeMillis() % 100000);
    }
}
