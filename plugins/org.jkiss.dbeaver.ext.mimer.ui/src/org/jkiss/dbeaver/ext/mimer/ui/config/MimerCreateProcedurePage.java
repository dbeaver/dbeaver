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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedureParameter;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerLibrary;
import org.jkiss.dbeaver.ext.mimer.model.MimerProcedure;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTableEditor;
import org.jkiss.dbeaver.ui.editors.object.struct.CreateProcedurePage;
import org.jkiss.utils.CommonUtils;

import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * "Create Procedure/Function" dialog for Mimer SQL, extending the framework's own {@link
 * CreateProcedurePage} with Mimer SQL-specific fields: Specific Name, Return Data Type (function
 * only), a Parameters grid, Deterministic, and Access Option. {@link
 * #getPredefinedProcedureType()} returns {@code null} so the base class's
 * Procedure/Function combo stays changeable, while {@link #getDefaultProcedureType()} still
 * pre-selects it from whichever folder was clicked (see {@code
 * MimerProcedureManager#detectProcedureType}).
 * <p>
 * Unlike a name+language+return-type-only dialog, this collects real parameters up front: each
 * row becomes a genuine {@link GenericProcedureParameter}, attached via {@code addColumn} before
 * the object is created, so the Parameters folder (and, for a function, the return value's own
 * "RETURN" row) shows real data immediately instead of only existing inside the generated DDL.
 * <p>
 * On a Mimer SQL 11.1+ server ({@link MimerDataSource#supportsExternalLibraries()}), a Language
 * choice (SQL/CLR) is also offered - see {@link #createExternalRoutineControls}. Choosing CLR
 * swaps the generated body from {@code BEGIN ... END} to {@code EXTERNAL NAME '...' IN library},
 * built from two extra fields (External Name, Library) instead of the usual Source-tab-edited SQL
 * body - both variants still go through the same seed-then-finish-in-Source-tab flow, the CLR one
 * just seeds a already-complete statement rather than a placeholder one.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateProcedurePage extends CreateProcedurePage {

    private static final Log log = Log.getLog(MimerCreateProcedurePage.class);

    private final MimerProcedure procedure;

    private String specificName = "";
    private String returnType = "";
    private String deterministic = "NOT DETERMINISTIC";
    private String accessOption = "READS SQL DATA";
    private String language = "SQL";
    private String externalName = "";
    private String externalLibrary = "";
    private final List<ParamRow> paramRows = new ArrayList<>();

    private Label returnTypeLabel;
    private Combo returnTypeCombo;
    private Table paramTable;

    private static class ParamRow {
        String name = "";
        String direction = "IN";
        String type = "";
    }

    public MimerCreateProcedurePage(MimerProcedure procedure) {
        super(procedure);
        this.procedure = procedure;
    }

    @Override
    public DBSProcedureType getPredefinedProcedureType() {
        return null;
    }

    /**
     * Pre-selects the combo from whichever folder ("Procedures" vs "Functions") was actually
     * clicked - see {@code MimerProcedureManager#detectProcedureType}.
     */
    @Override
    public DBSProcedureType getDefaultProcedureType() {
        return procedure.getProcedureType();
    }

    @Override
    protected void createExtraControls(Composite group) {
        Text specificText = UIUtils.createLabelText(group, "Specific name", "");
        specificText.setMessage(MimerUIMessages.page_create_procedure_specific_name_placeholder);
        specificText.addModifyListener(e -> specificName = specificText.getText());

        returnTypeLabel = UIUtils.createControlLabel(group, "Return data type");
        returnTypeCombo = new Combo(group, SWT.DROP_DOWN);
        returnTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (String t : loadDataTypeNames()) {
            returnTypeCombo.add(t);
        }
        returnTypeCombo.addModifyListener(e -> {
            returnType = returnTypeCombo.getText();
            updatePageState();
        });
        updateReturnTypeVisibility(getDefaultProcedureType() == DBSProcedureType.FUNCTION);

        createParametersGrid(group);

        Combo deterministicCombo = UIUtils.createLabelCombo(group, "Deterministic", SWT.DROP_DOWN | SWT.READ_ONLY);
        deterministicCombo.add("NOT DETERMINISTIC");
        deterministicCombo.add("DETERMINISTIC");
        deterministicCombo.select(0);
        deterministicCombo.addModifyListener(e -> deterministic = deterministicCombo.getText());

        Combo accessCombo = UIUtils.createLabelCombo(group, "Access option", SWT.DROP_DOWN | SWT.READ_ONLY);
        accessCombo.add("CONTAINS SQL");
        accessCombo.add("READS SQL DATA");
        accessCombo.add("MODIFIES SQL DATA");
        accessCombo.select(1);
        accessCombo.addModifyListener(e -> accessOption = accessCombo.getText());

        if (procedure.getDataSource() instanceof MimerDataSource ds && ds.supportsExternalLibraries()) {
            createExternalRoutineControls(group);
        }
    }

    /**
     * Language (SQL/CLR) plus External Name and Library - Mimer SQL 11.1+ only (see {@link
     * MimerDataSource#supportsExternalLibraries()}). External Name/Library are always visible
     * (not shown/hidden by the Language choice - simpler and more robust than a layout toggle),
     * but greyed out (disabled, not just visually) while Language is SQL, since they're not
     * meaningful and {@link #applyChanges()} ignores them then. Deterministic/Access Option
     * above stay enabled either way - both are independent of the routine body per {@code
     * CREATE PROCEDURE}/{@code CREATE FUNCTION}'s own grammar, only the body itself (a SQL
     * statement here, vs. {@code EXTERNAL NAME '...' IN library} for CLR) differs.
     * <p>
     * Uses {@code addSelectionListener}, not {@code addModifyListener}, for both {@code
     * SWT.READ_ONLY} combos here - a {@code READ_ONLY} combo's {@code Modify} event does not
     * fire reliably for a dropdown-picked selection (it's meant for typed text changes); {@code
     * Selection} is the correct, reliably-firing event for that. The other {@code READ_ONLY}
     * combos in this same dialog (Deterministic, Access Option) likely share this same latent
     * gap - not fixed here since it was never reported as broken for them, and their captured
     * values already default to something reasonable even if a change were missed.
     */
    private void createExternalRoutineControls(Composite group) {
        Combo languageCombo = UIUtils.createLabelCombo(group, "Language", SWT.DROP_DOWN | SWT.READ_ONLY);
        languageCombo.add("SQL");
        languageCombo.add("CLR");
        languageCombo.select(0);

        Text externalNameText = UIUtils.createLabelText(group, "External name", "");
        externalNameText.setMessage(MimerUIMessages.page_create_procedure_external_name_placeholder);
        externalNameText.setEnabled(false);
        externalNameText.addModifyListener(e -> {
            externalName = externalNameText.getText();
            updatePageState();
        });

        Combo externalLibraryCombo = UIUtils.createLabelCombo(group, "Library", SWT.DROP_DOWN | SWT.READ_ONLY);
        for (String libraryName : loadLibraryNames()) {
            externalLibraryCombo.add(libraryName);
        }
        externalLibraryCombo.setEnabled(false);
        externalLibraryCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            externalLibrary = externalLibraryCombo.getText();
            updatePageState();
        }));

        languageCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            language = languageCombo.getText();
            boolean external = "CLR".equals(language);
            externalNameText.setEnabled(external);
            externalLibraryCombo.setEnabled(external);
            updatePageState();
        }));
    }

    @NotNull
    private String[] loadLibraryNames() {
        try {
            if (procedure.getDataSource() instanceof MimerDataSource ds) {
                List<String> names = new ArrayList<>();
                for (MimerLibrary library : ds.getLibraries(new VoidProgressMonitor())) {
                    names.add(library.getName());
                }
                return names.toArray(new String[0]);
            }
        } catch (Exception e) {
            log.debug("Can't load library list for the create-procedure dialog", e);
        }
        return new String[0];
    }

    /**
     * Name/Direction/Type grid with Add/Remove buttons, using {@link CustomTableEditor} for
     * inline cell editing. Direction is only editable for a procedure - a function parameter's
     * mode is always implicitly {@code IN}.
     */
    private void createParametersGrid(Composite group) {
        UIUtils.createControlLabel(group, "Parameters").setLayoutData(spanBothColumns());

        Composite paramGroup = new Composite(group, SWT.NONE);
        paramGroup.setLayout(new GridLayout(2, false));
        paramGroup.setLayoutData(spanBothColumns());

        paramTable = new Table(paramGroup, SWT.BORDER | SWT.FULL_SELECTION);
        paramTable.setHeaderVisible(true);
        GridData tableGd = new GridData(GridData.FILL_HORIZONTAL);
        tableGd.heightHint = 120;
        paramTable.setLayoutData(tableGd);
        UIUtils.createTableColumn(paramTable, SWT.NONE, "Name").setWidth(120);
        UIUtils.createTableColumn(paramTable, SWT.NONE, "Direction").setWidth(80);
        UIUtils.createTableColumn(paramTable, SWT.NONE, "Type").setWidth(140);

        new CustomTableEditor(paramTable) {
            @Override
            protected Control createEditor(Table table, int index, TableItem item) {
                ParamRow row = (ParamRow) item.getData();
                if (index == 1) {
                    if (getProcedureType() == DBSProcedureType.FUNCTION) {
                        return null;
                    }
                    CCombo combo = new CCombo(table, SWT.DROP_DOWN | SWT.READ_ONLY);
                    combo.add("IN");
                    combo.add("OUT");
                    combo.add("INOUT");
                    combo.setText(row.direction);
                    return combo;
                }
                if (index == 2) {
                    CCombo combo = new CCombo(table, SWT.DROP_DOWN);
                    for (String t : loadDataTypeNames()) {
                        combo.add(t);
                    }
                    combo.setText(row.type);
                    return combo;
                }
                Text text = new Text(table, SWT.BORDER);
                text.setText(row.name);
                text.selectAll();
                return text;
            }

            @Override
            protected void saveEditorValue(Control control, int index, TableItem item) {
                ParamRow row = (ParamRow) item.getData();
                if (control instanceof CCombo combo) {
                    if (index == 1) {
                        row.direction = combo.getText();
                        item.setText(1, row.direction);
                    } else {
                        row.type = combo.getText();
                        item.setText(2, row.type);
                    }
                } else if (control instanceof Text text) {
                    row.name = text.getText();
                    item.setText(0, row.name);
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

    @NotNull
    private static GridData spanBothColumns() {
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        return gd;
    }

    private void addParamRow() {
        ParamRow row = new ParamRow();
        paramRows.add(row);
        TableItem item = new TableItem(paramTable, SWT.NONE);
        item.setData(row);
        item.setText(0, row.name);
        item.setText(1, row.direction);
        item.setText(2, row.type);
    }

    private void removeSelectedParamRow() {
        int index = paramTable.getSelectionIndex();
        if (index >= 0) {
            paramRows.remove(index);
            paramTable.remove(index);
        }
    }

    @Override
    protected void updateProcedureType(DBSProcedureType type) {
        updateReturnTypeVisibility(type == DBSProcedureType.FUNCTION);
        updatePageState();
    }

    private void updateReturnTypeVisibility(boolean function) {
        if (returnTypeLabel == null || returnTypeCombo == null) {
            return;
        }
        returnTypeLabel.setVisible(function);
        returnTypeCombo.setVisible(function);
        ((GridData) returnTypeLabel.getLayoutData()).exclude = !function;
        ((GridData) returnTypeCombo.getLayoutData()).exclude = !function;
        returnTypeLabel.getParent().layout();
    }

    @Override
    public boolean isPageComplete() {
        if (!super.isPageComplete()) {
            return false;
        }
        if (getProcedureType() == DBSProcedureType.FUNCTION && CommonUtils.isEmptyTrimmed(returnType)) {
            return false;
        }
        if ("CLR".equals(language)) {
            return !CommonUtils.isEmptyTrimmed(externalName) && !CommonUtils.isEmptyTrimmed(externalLibrary);
        }
        return true;
    }

    private String[] loadDataTypeNames() {
        try {
            Set<String> names = new LinkedHashSet<>();
            for (DBSObject type : procedure.getDataSource().getDataTypes(new VoidProgressMonitor())) {
                names.add(type.getName().replaceAll("\\(.*\\)", "").trim());
            }
            return names.toArray(new String[0]);
        } catch (Exception e) {
            log.debug("Can't load data type list for the create-procedure dialog", e);
        }
        return new String[0];
    }

    /**
     * Applies the collected values to the same stub object passed to the constructor rather than
     * replacing it - the framework binds the pending create command to this specific instance,
     * which the Source editor's Save also writes into (see {@link MimerProcedure}). Builds the
     * {@code CREATE FUNCTION}/{@code CREATE PROCEDURE ... BEGIN ... END} text (parameters, then
     * {@code RETURNS} for a function, {@code LANGUAGE SQL}, optional {@code SPECIFIC}, then the
     * deterministic/access-option clauses and body) and attaches each parameter as a real
     * {@link GenericProcedureParameter}. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        DBSProcedureType type = getProcedureType();
        String name = getProcedureName();
        String specific = CommonUtils.isEmptyTrimmed(specificName) ? null : specificName.trim();

        procedure.setName(name);
        procedure.setProcedureType(type);
        procedure.setSpecificName(specific);

        StringBuilder paramList = new StringBuilder();
        int ordinal = 1;
        for (ParamRow row : paramRows) {
            if (CommonUtils.isEmptyTrimmed(row.name) || CommonUtils.isEmptyTrimmed(row.type)) {
                continue;
            }
            String direction = type == DBSProcedureType.FUNCTION ? "IN" : row.direction;
            if (!paramList.isEmpty()) {
                paramList.append(", ");
            }
            paramList.append(direction).append(" \"").append(row.name.trim()).append("\" ").append(row.type.trim());

            DBSProcedureParameterKind kind = switch (direction) {
                case "OUT" -> DBSProcedureParameterKind.OUT;
                case "INOUT" -> DBSProcedureParameterKind.INOUT;
                default -> DBSProcedureParameterKind.IN;
            };
            procedure.addColumn(new GenericProcedureParameter(
                procedure, row.name.trim(), row.type.trim(), Types.OTHER, ordinal++, 0, null, null, false, null, kind));
        }
        if (type == DBSProcedureType.FUNCTION && !CommonUtils.isEmptyTrimmed(returnType)) {
            // Matches the convention MimerProcedure#loadProcedureColumns uses for an
            // already-created function: a RETURN-kind parameter named "RETURN" at ordinal 0.
            procedure.addColumn(new GenericProcedureParameter(
                procedure, "RETURN", returnType.trim(), Types.OTHER, 0, 0, null, null, false, null, DBSProcedureParameterKind.RETURN));
        }

        String schemaName = procedure.getSchema().getName();
        String keyword = type == DBSProcedureType.FUNCTION ? "FUNCTION" : "PROCEDURE";
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE ").append(keyword).append(" \"").append(schemaName).append("\".\"").append(name).append("\" ")
            .append(paramList.isEmpty() ? "()" : "(" + paramList + ")").append('\n');
        if (type == DBSProcedureType.FUNCTION) {
            sb.append("RETURNS ").append(returnType.trim()).append('\n');
        }
        boolean external = "CLR".equals(language);
        sb.append("LANGUAGE ").append(language).append('\n');
        if (specific != null) {
            sb.append("SPECIFIC \"").append(specific).append("\"\n");
        }
        sb.append(deterministic).append('\n');
        sb.append(accessOption).append('\n');
        if (external) {
            sb.append("EXTERNAL NAME '").append(externalName.trim().replace("'", "''")).append('\'')
                .append(" IN \"").append(externalLibrary.trim()).append('"');
        } else {
            sb.append("BEGIN\n");
            sb.append("    -- TODO: ").append(type == DBSProcedureType.FUNCTION ? "function" : "procedure").append(" body\n");
            if (type == DBSProcedureType.FUNCTION) {
                sb.append("    RETURN NULL;\n");
            }
            sb.append("END");
        }

        procedure.setSource(sb.toString());
    }
}
