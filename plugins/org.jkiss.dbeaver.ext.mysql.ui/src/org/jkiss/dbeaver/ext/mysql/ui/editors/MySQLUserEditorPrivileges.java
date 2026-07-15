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
package org.jkiss.dbeaver.ext.mysql.ui.editors;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.ext.mysql.ui.config.MySQLCommandGrantPrivilege;
import org.jkiss.dbeaver.ext.mysql.ui.config.MySQLCommandRevokeObjectGrants;
import org.jkiss.dbeaver.ext.mysql.ui.controls.PrivilegeTableControl;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * MySQLUserEditorPrivileges
 */
public class MySQLUserEditorPrivileges extends MySQLUserEditorAbstract
{
    private static final Log log = Log.getLog(MySQLUserEditorPrivileges.class);

    private static final String ADD_ITEM_TEXT = "+"; //$NON-NLS-1$
    private static final String REMOVE_ITEM_TEXT = "-"; //$NON-NLS-1$

    private PageControl pageControl;
    private Table catalogsTable;
    private Table tablesTable;
    private Table columnsTable;
    private Table proceduresTable;
    private Composite tablesGroup;
    private Composite columnsGroup;
    private Composite proceduresGroup;

    private boolean isLoaded = false;
    private MySQLCatalog selectedCatalog;
    private MySQLTableBase selectedTable;
    private MySQLTableColumn selectedColumn;
    private MySQLProcedure selectedProcedure;
    // Full multi-selections; selectedCatalog/selectedTable/selectedColumn/selectedProcedure hold the anchor (first) element
    private final List<MySQLCatalog> selectedCatalogs = new ArrayList<>();
    private final List<MySQLTableBase> selectedTables = new ArrayList<>();
    private final List<MySQLTableColumn> selectedColumns = new ArrayList<>();
    private final List<MySQLProcedure> selectedProcedures = new ArrayList<>();
    private PrivilegeTableControl tablePrivilegesTable;
    private PrivilegeTableControl columnPrivilegesTable;
    private PrivilegeTableControl procedurePrivilegesTable;
    private PrivilegeTableControl otherPrivilegesTable;
    private volatile List<MySQLGrant> grants;

    private Button tablesAddButton;
    private Button columnsAddButton;
    private Button proceduresAddButton;
    private Button catalogsRemoveButton;
    private Button tablesRemoveButton;
    private Button columnsRemoveButton;
    private Button proceduresRemoveButton;
    // Objects without grants added manually via the "+" row - cleared on editor refresh
    private final Set<MySQLCatalog> extraCatalogs = new HashSet<>();
    private final Map<MySQLCatalog, Set<MySQLTableBase>> extraTables = new HashMap<>();
    private final Map<MySQLTableBase, Set<MySQLTableColumn>> extraColumns = new HashMap<>();
    private final Map<MySQLCatalog, Set<MySQLProcedure>> extraProcedures = new HashMap<>();
    private List<MySQLTableBase> catalogTables;
    private List<MySQLTableColumn> tableColumns;
    private List<MySQLProcedure> catalogProcedures;
    // Privilege types applicable on column level (Select, Insert, Update, References)
    private List<MySQLPrivilege> columnPrivilegeTypes;
    // Privilege types shown in the table/other/procedure panels (captured on privileges load)
    private List<MySQLPrivilege> tablePrivilegeTypes;
    private List<MySQLPrivilege> otherPrivilegeTypes;
    private List<MySQLPrivilege> procedurePrivilegeTypes;

    @Override
    public void createPartControl(Composite parent) {
        pageControl = new PageControl(parent);

        GridData gd = new GridData(GridData.FILL_BOTH);
        CustomSashForm sash = new CustomSashForm(pageControl, SWT.HORIZONTAL);
        sash.setLayoutData(gd);

        Composite leftPane = UIUtils.createPlaceholder(sash, 3);
        leftPane.setLayoutData(new GridData(GridData.FILL_BOTH));
        leftPane.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).create());
        {
            Composite catalogGroup = UIUtils.createTitledComposite(
                leftPane,
                MySQLUIMessages.editors_user_editor_privileges_group_catalogs,
                1,
                GridData.FILL_BOTH
            );

            catalogsTable = new Table(catalogGroup, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
            catalogsTable.setHeaderVisible(true);
            gd = new GridData(SWT.FILL, SWT.TOP, true, false);
            catalogsTable.setLayoutData(gd);
            catalogsTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (catalogsTable.getSelectionIndex() < 0) {
                        return;
                    }
                    selectedCatalogs.clear();
                    for (TableItem item : catalogsTable.getSelection()) {
                        if (item.getData() != null) {
                            selectedCatalogs.add((MySQLCatalog) item.getData());
                        }
                    }
                    selectedCatalog = selectedCatalogs.isEmpty() ? null : selectedCatalogs.get(0);
                    // Reset dependent selections right away: table/procedure lists reload asynchronously,
                    // and showGrants() below must not see the previous catalog's selection
                    selectedTable = null;
                    selectedTables.clear();
                    selectedColumn = null;
                    selectedColumns.clear();
                    selectedProcedure = null;
                    selectedProcedures.clear();
                    showCatalogTables();
                    showCatalogProcedures();
                    showGrants();
                }
            });
            UIUtils.createTableColumn(catalogsTable, SWT.LEFT, MySQLUIMessages.editors_user_editor_privileges_column_catalog);
            fillCatalogs();
            Composite catalogButtons = UIUtils.createComposite(catalogGroup, 2);
            UIUtils.createPushButton(catalogButtons, ADD_ITEM_TEXT, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    handleAddCatalog();
                }
            });
            catalogsRemoveButton = UIUtils.createPushButton(catalogButtons, REMOVE_ITEM_TEXT, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    handleRemoveCatalog();
                }
            });
            catalogsRemoveButton.setEnabled(false);
        }

        {
            Composite tablesPane = UIUtils.createPlaceholder(leftPane, 1);
            tablesPane.setLayoutData(new GridData(GridData.FILL_BOTH));

            tablesGroup = UIUtils.createTitledComposite(
                tablesPane,
                MySQLUIMessages.editors_user_editor_privileges_group_tables,
                1,
                GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING
            );

            tablesTable = new Table(tablesGroup, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
            tablesTable.setHeaderVisible(true);
            gd = new GridData(SWT.FILL, SWT.TOP, true, false);
            tablesTable.setLayoutData(gd);
            tablesTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (tablesTable.getSelectionIndex() < 0) {
                        return;
                    }
                    selectedTables.clear();
                    for (TableItem item : tablesTable.getSelection()) {
                        if (item.getData() != null) {
                            selectedTables.add((MySQLTableBase) item.getData());
                        }
                    }
                    selectedTable = selectedTables.isEmpty() ? null : selectedTables.get(0);
                    selectedColumn = null;
                    selectedColumns.clear();
                    selectedProcedure = null;
                    selectedProcedures.clear();
                    proceduresTable.deselectAll();
                    showTableColumns();
                    showGrants();
                }
            });
            UIUtils.createTableColumn(tablesTable, SWT.LEFT, MySQLUIMessages.editors_user_editor_privileges_column_table);
            UIUtils.packColumns(tablesTable);
            Composite tableButtons = UIUtils.createComposite(tablesGroup, 2);
            tablesAddButton = UIUtils.createPushButton(tableButtons, ADD_ITEM_TEXT, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    handleAddTable();
                }
            });
            tablesAddButton.setEnabled(false);
            tablesRemoveButton = UIUtils.createPushButton(tableButtons, REMOVE_ITEM_TEXT, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    handleRemoveTable();
                }
            });
            tablesRemoveButton.setEnabled(false);

            proceduresGroup = UIUtils.createTitledComposite(
                tablesPane,
                MySQLUIMessages.editors_user_editor_privileges_group_procedures,
                1,
                GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING
            );

            proceduresTable = new Table(proceduresGroup, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
            proceduresTable.setHeaderVisible(true);
            gd = new GridData(SWT.FILL, SWT.TOP, true, false);
            proceduresTable.setLayoutData(gd);
            proceduresTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (proceduresTable.getSelectionIndex() < 0) {
                        return;
                    }
                    selectedProcedures.clear();
                    for (TableItem item : proceduresTable.getSelection()) {
                        if (item.getData() != null) {
                            selectedProcedures.add((MySQLProcedure) item.getData());
                        }
                    }
                    selectedProcedure = selectedProcedures.isEmpty() ? null : selectedProcedures.get(0);
                    selectedTable = null;
                    selectedTables.clear();
                    selectedColumn = null;
                    selectedColumns.clear();
                    tablesTable.deselectAll();
                    showTableColumns();
                    showGrants();
                }
            });
            UIUtils.createTableColumn(proceduresTable, SWT.LEFT, MySQLUIMessages.editors_user_editor_privileges_column_procedure);
            UIUtils.packColumns(proceduresTable);
            Composite procedureButtons = UIUtils.createComposite(proceduresGroup, 2);
            proceduresAddButton = UIUtils.createPushButton(procedureButtons, ADD_ITEM_TEXT, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    handleAddProcedure();
                }
            });
            proceduresAddButton.setEnabled(false);
            proceduresRemoveButton = UIUtils.createPushButton(procedureButtons, REMOVE_ITEM_TEXT, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    handleRemoveProcedure();
                }
            });
            proceduresRemoveButton.setEnabled(false);
        }

        {
            columnsGroup = UIUtils.createTitledComposite(
                leftPane,
                MySQLUIMessages.editors_user_editor_privileges_group_columns,
                1,
                GridData.FILL_BOTH
            );

            columnsTable = new Table(columnsGroup, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
            columnsTable.setHeaderVisible(true);
            gd = new GridData(SWT.FILL, SWT.TOP, true, false);
            columnsTable.setLayoutData(gd);
            columnsTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (columnsTable.getSelectionIndex() < 0) {
                        return;
                    }
                    selectedColumns.clear();
                    for (TableItem item : columnsTable.getSelection()) {
                        if (item.getData() != null) {
                            selectedColumns.add((MySQLTableColumn) item.getData());
                        }
                    }
                    selectedColumn = selectedColumns.isEmpty() ? null : selectedColumns.get(0);
                    selectedProcedure = null;
                    selectedProcedures.clear();
                    proceduresTable.deselectAll();
                    showGrants();
                }
            });
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, MySQLUIMessages.editors_user_editor_privileges_column_column);
            UIUtils.packColumns(columnsTable);
            Composite columnButtons = UIUtils.createComposite(columnsGroup, 2);
            columnsAddButton = UIUtils.createPushButton(columnButtons, ADD_ITEM_TEXT, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    handleAddColumn();
                }
            });
            columnsAddButton.setEnabled(false);
            columnsRemoveButton = UIUtils.createPushButton(columnButtons, REMOVE_ITEM_TEXT, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    handleRemoveColumn();
                }
            });
            columnsRemoveButton.setEnabled(false);
        }
        Composite ph = UIUtils.createPlaceholder(sash, 1);
        ph.setLayoutData(new GridData(GridData.FILL_BOTH));

        tablePrivilegesTable = new PrivilegeTableControl(ph, MySQLUIMessages.editors_user_editor_privileges_control_table_privileges, false);
        gd = new GridData(GridData.FILL_BOTH);
        tablePrivilegesTable.setLayoutData(gd);

        columnPrivilegesTable = new PrivilegeTableControl(ph, MySQLUIMessages.editors_user_editor_privileges_control_column_privileges, false);
        gd = new GridData(GridData.FILL_BOTH);
        columnPrivilegesTable.setLayoutData(gd);

        procedurePrivilegesTable = new PrivilegeTableControl(ph, MySQLUIMessages.editors_user_editor_privileges_control_procedure_privileges, false);
        gd = new GridData(GridData.FILL_BOTH);
        procedurePrivilegesTable.setLayoutData(gd);

        otherPrivilegesTable = new PrivilegeTableControl(ph, MySQLUIMessages.editors_user_editor_privileges_control_other_privileges, false);
        gd = new GridData(GridData.FILL_BOTH);
        otherPrivilegesTable.setLayoutData(gd);

        sash.setSashBorders(new boolean[]{false, false});

        catalogsTable.setSelection(0);
        showCatalogTables();
        showCatalogProcedures();

        pageControl.createProgressPanel();

        addGrantListener(tablePrivilegesTable);
        addGrantListener(otherPrivilegesTable);
        addColumnGrantListener(columnPrivilegesTable);
        addProcedureGrantListener(procedurePrivilegesTable);
    }

    private void addGrantListener(final PrivilegeTableControl privTable)
    {
        privTable.addListener(SWT.Modify, event -> {
            final MySQLPrivilege privilege = (MySQLPrivilege) event.data;
            final boolean isGrant = event.detail >= 1;
            final boolean withGrantOption = event.detail == 2;
            final MySQLCatalog curCatalog = selectedCatalog;
            if (selectedCatalogs.size() > 1 && selectedTable == null && selectedProcedure == null) {
                // Multiple schemas selected: apply the privilege on schema level (catalog.*) to each of them
                for (MySQLCatalog targetCatalog : new ArrayList<>(selectedCatalogs)) {
                    if (!tablePrivilegeChangeNeeded(targetCatalog, null, privilege, isGrant)) {
                        continue;
                    }
                    updateLocalData(privilege, isGrant, withGrantOption, targetCatalog, null);
                    applyPrivilegeChange(privilege, isGrant, withGrantOption, targetCatalog, null, null, null);
                }
                return;
            }
            List<MySQLTableBase> targets = new ArrayList<>();
            if (selectedTables.size() > 1) {
                targets.addAll(selectedTables);
            } else {
                targets.add(selectedTable); // may be null - schema-level grant
            }
            boolean multi = targets.size() > 1;
            for (MySQLTableBase curTable : targets) {
                if (multi && !tablePrivilegeChangeNeeded(curCatalog, curTable, privilege, isGrant)) {
                    continue;
                }
                updateLocalData(privilege, isGrant, withGrantOption, curCatalog, curTable);
                applyPrivilegeChange(privilege, isGrant, withGrantOption, curCatalog, curTable, null, null);
            }
        }
        );
    }

    /**
     * Applies one privilege toggle to one grant target, folding it into an already pending
     * command on the same target: several checks produce a single GRANT/REVOKE statement,
     * and check+uncheck cancels out to no SQL at all.
     */
    private void applyPrivilegeChange(
        MySQLPrivilege privilege,
        boolean isGrant,
        boolean withGrantOption,
        MySQLCatalog schema,
        @Nullable MySQLTableBase table,
        @Nullable MySQLProcedure procedure,
        @Nullable List<MySQLTableColumn> columns
    ) {
        DBECommandContext commandContext = getEditorInput().getCommandContext();
        // A pending command of the opposite direction cancels this change out
        MySQLCommandGrantPrivilege opposite = findPendingCommand(!isGrant, schema, table, procedure, columns);
        if (opposite != null) {
            boolean cancelled;
            if (privilege.isGrantOption() && !isGrant && opposite.isWithGrantOption()) {
                opposite.setWithGrantOption(false);
                cancelled = true;
            } else {
                cancelled = opposite.removePrivilege(privilege);
            }
            if (cancelled) {
                if (opposite.isEmptyCommand()) {
                    commandContext.removeCommand(opposite);
                } else {
                    commandContext.updateCommand(opposite, null);
                }
                return;
            }
        }
        // Fold into a pending command of the same direction
        MySQLCommandGrantPrivilege pending = findPendingCommand(isGrant, schema, table, procedure, columns);
        if (pending != null) {
            if (privilege.isGrantOption() && isGrant) {
                pending.setWithGrantOption(true);
            } else {
                pending.addPrivilege(privilege);
                if (withGrantOption) {
                    pending.setWithGrantOption(true);
                }
            }
            commandContext.updateCommand(pending, null);
            return;
        }
        MySQLCommandGrantPrivilege command;
        if (procedure != null) {
            command = new MySQLCommandGrantPrivilege(getDatabaseObject(), isGrant, withGrantOption, schema, procedure, privilege);
        } else if (columns != null) {
            command = new MySQLCommandGrantPrivilege(getDatabaseObject(), isGrant, withGrantOption, schema, table, columns, privilege);
        } else {
            command = new MySQLCommandGrantPrivilege(getDatabaseObject(), isGrant, withGrantOption, schema, table, privilege);
        }
        addChangeCommand(command, null);
    }

    @Nullable
    private MySQLCommandGrantPrivilege findPendingCommand(
        boolean grantDirection,
        MySQLCatalog schema,
        @Nullable MySQLTableBase table,
        @Nullable MySQLProcedure procedure,
        @Nullable List<MySQLTableColumn> columns
    ) {
        DBECommandContext commandContext = getEditorInput().getCommandContext();
        if (commandContext == null) {
            return null;
        }
        for (DBECommand<?> command : commandContext.getFinalCommands()) {
            if (command instanceof MySQLCommandGrantPrivilege grantCommand
                && grantCommand.isGrant() == grantDirection
                && grantCommand.hasSameTarget(schema, table, procedure, columns)
            ) {
                return grantCommand;
            }
        }
        return null;
    }

    /**
     * In multi-selection mode objects already in the desired state are skipped:
     * a GRANT would be redundant and a REVOKE of a non-existing grant fails on the server.
     */
    private boolean tablePrivilegeChangeNeeded(MySQLCatalog catalog, MySQLTableBase table, MySQLPrivilege privilege, boolean isGrant) {
        if (grants == null) {
            return true;
        }
        return hasTablePrivilege(catalog, table, privilege) != isGrant;
    }

    private boolean hasTablePrivilege(MySQLCatalog catalog, @Nullable MySQLTableBase table, MySQLPrivilege privilege) {
        if (grants == null) {
            return false;
        }
        for (MySQLGrant grant : grants) {
            if (grant.matches(catalog) && grant.matches(table)
                && (privilege.isGrantOption()
                    ? grant.isGrantOption()
                    : grant.isAllPrivileges() || ArrayUtils.contains(grant.getPrivileges(), privilege))
            ) {
                return true;
            }
        }
        return false;
    }

    private void addColumnGrantListener(final PrivilegeTableControl privTable)
    {
        privTable.addListener(SWT.Modify, event -> {
            final MySQLPrivilege privilege = (MySQLPrivilege) event.data;
            final boolean isGrant = event.detail >= 1;
            final boolean withGrantOption = event.detail == 2;
            final MySQLCatalog curCatalog = selectedCatalog;
            final MySQLTableBase curTable = selectedTable;
            if (curCatalog == null || curTable == null || selectedColumn == null) {
                return;
            }
            boolean multi = selectedColumns.size() > 1;
            List<MySQLTableColumn> targets = new ArrayList<>();
            for (MySQLTableColumn column : selectedColumns) {
                if (multi && !columnPrivilegeChangeNeeded(curCatalog, curTable, column, privilege, isGrant)) {
                    continue;
                }
                targets.add(column);
            }
            if (targets.isEmpty()) {
                return;
            }
            final List<MySQLTableColumn> curColumns = List.copyOf(targets);
            for (MySQLTableColumn column : curColumns) {
                updateLocalColumnData(privilege, isGrant, withGrantOption, curCatalog, curTable, column);
            }
            // One statement per privilege toggle: GRANT priv (col1, col2, ...) ON catalog.table
            applyPrivilegeChange(privilege, isGrant, withGrantOption, curCatalog, curTable, null, curColumns);
        }
        );
    }

    private boolean columnPrivilegeChangeNeeded(MySQLCatalog catalog, MySQLTableBase table, MySQLTableColumn column, MySQLPrivilege privilege, boolean isGrant) {
        if (grants == null) {
            return true;
        }
        return hasColumnPrivilegeState(catalog, table, column, privilege) != isGrant;
    }

    private boolean hasColumnPrivilegeState(MySQLCatalog catalog, MySQLTableBase table, MySQLTableColumn column, MySQLPrivilege privilege) {
        if (grants == null) {
            return false;
        }
        for (MySQLGrant grant : grants) {
            if (grant.matches(catalog) && grant.matches(table)
                && (privilege.isGrantOption() ? grant.isGrantOption() : grant.hasColumnPrivilege(privilege, column.getName()))
            ) {
                return true;
            }
        }
        return false;
    }

    private void updateLocalColumnData(MySQLPrivilege privilege, boolean isGrant, boolean withGrantOption, MySQLCatalog curCatalog, MySQLTableBase curTable, MySQLTableColumn curColumn) {
        getDatabaseObject().clearGrantsCache();
        boolean found = false;
        for (MySQLGrant grant : grants) {
            if (grant.matches(curCatalog) && grant.matches(curTable)) {
                grant.setGrantOption(withGrantOption);
                if (!privilege.isGrantOption()) {
                    if (isGrant) {
                        grant.addColumnPrivilege(privilege, curColumn.getName());
                    } else {
                        grant.removeColumnPrivilege(privilege, curColumn.getName());
                    }
                }
                found = true;
                break;
            }
        }
        if (!found) {
            MySQLGrant grant = new MySQLGrant(
                getDatabaseObject(),
                new ArrayList<>(),
                curCatalog.getName(),
                curTable.getName(),
                false,
                withGrantOption);
            if (isGrant && !privilege.isGrantOption()) {
                grant.addColumnPrivilege(privilege, curColumn.getName());
            }
            grants.add(grant);
        }
        highlightCatalogs();
        highlightTables();
        highlightColumns();
    }

    private void addProcedureGrantListener(final PrivilegeTableControl privTable)
    {
        privTable.addListener(SWT.Modify, event -> {
            final MySQLPrivilege privilege = (MySQLPrivilege) event.data;
            final boolean isGrant = event.detail >= 1;
            final boolean withGrantOption = event.detail == 2;
            final MySQLCatalog curCatalog = selectedCatalog;
            if (curCatalog == null || selectedProcedure == null) {
                return;
            }
            List<MySQLProcedure> targets = new ArrayList<>(selectedProcedures);
            boolean multi = targets.size() > 1;
            for (MySQLProcedure curProcedure : targets) {
                if (multi && !procedurePrivilegeChangeNeeded(curCatalog, curProcedure, privilege, isGrant)) {
                    continue;
                }
                updateLocalProcedureData(privilege, isGrant, withGrantOption, curCatalog, curProcedure);
                applyPrivilegeChange(privilege, isGrant, withGrantOption, curCatalog, null, curProcedure, null);
            }
        }
        );
    }

    private boolean procedurePrivilegeChangeNeeded(MySQLCatalog catalog, MySQLProcedure procedure, MySQLPrivilege privilege, boolean isGrant) {
        if (grants == null) {
            return true;
        }
        return hasProcedurePrivilege(catalog, procedure, privilege) != isGrant;
    }

    private boolean hasProcedurePrivilege(MySQLCatalog catalog, MySQLProcedure procedure, MySQLPrivilege privilege) {
        if (grants == null) {
            return false;
        }
        for (MySQLGrant grant : grants) {
            if (grant.matches(catalog) && grant.matchesProcedure(procedure)
                && (privilege.isGrantOption()
                    ? grant.isGrantOption()
                    : grant.isAllPrivileges() || ArrayUtils.contains(grant.getPrivileges(), privilege))
            ) {
                return true;
            }
        }
        return false;
    }

    private void updateLocalProcedureData(MySQLPrivilege privilege, boolean isGrant, boolean withGrantOption, MySQLCatalog curCatalog, MySQLProcedure curProcedure) {
        getDatabaseObject().clearGrantsCache();
        boolean found = false;
        for (MySQLGrant grant : grants) {
            if (grant.matches(curCatalog) && grant.matchesProcedure(curProcedure)) {
                grant.setGrantOption(withGrantOption);
                if (isGrant) {
                    if (!ArrayUtils.contains(grant.getPrivileges(), privilege)) {
                        grant.addPrivilege(privilege);
                    }
                } else {
                    grant.removePrivilege(privilege);
                }
                found = true;
                break;
            }
        }
        if (!found) {
            List<MySQLPrivilege> privileges = new ArrayList<>();
            if (!privilege.isGrantOption()) {
                privileges.add(privilege);
            }
            MySQLGrant grant = new MySQLGrant(
                getDatabaseObject(),
                privileges,
                curCatalog.getName(),
                curProcedure.getName(),
                false,
                withGrantOption,
                curProcedure.getProcedureType() == DBSProcedureType.FUNCTION
                    ? MySQLGrant.ObjectType.FUNCTION : MySQLGrant.ObjectType.PROCEDURE);
            grants.add(grant);
        }
        highlightCatalogs();
        highlightProcedures();
    }

    private void updateLocalData(MySQLPrivilege privilege, boolean isGrant, boolean withGrantOption, MySQLCatalog curCatalog, MySQLTableBase curTable) {
        // Modify local grants (and clear grants cache in user objects)
        getDatabaseObject().clearGrantsCache();
        boolean found = false;
        for (MySQLGrant grant : grants) {
            if (grant.matches(curCatalog) && grant.matches(curTable)) {
                //if (privilege.isGrantOption()) {
                    grant.setGrantOption(withGrantOption);
                //} else
                if (isGrant) {
                    if (!ArrayUtils.contains(grant.getPrivileges(), privilege)) {
                        grant.addPrivilege(privilege);
                    }
                } else {
                    grant.removePrivilege(privilege);
                }
                found = true;
                break;
            }
        }
        if (!found) {
            List<MySQLPrivilege> privileges = new ArrayList<>();
            if (!privilege.isGrantOption()) {
                privileges.add(privilege);
            }
            MySQLGrant grant = new MySQLGrant(
                getDatabaseObject(),
                privileges,
                curCatalog == null ? "*" : curCatalog.getName(), //$NON-NLS-1$
                curTable == null ? "*" : curTable.getName(), //$NON-NLS-1$
                false,
                withGrantOption);
            grants.add(grant);
        }
        highlightCatalogs();
        highlightTables();
    }

    private void fillCatalogs()
    {
        if (catalogsTable == null || catalogsTable.isDisposed()) {
            return;
        }
        MySQLCatalog prevSelection = selectedCatalog;
        // setRedraw(false/true) coalesces all row changes into a single native reload:
        // incremental row updates leave ghost rows on macOS
        catalogsTable.setRedraw(false);
        try {
            catalogsTable.removeAll();
            {
                TableItem item = new TableItem(catalogsTable, SWT.NONE);
                item.setText("% (All)"); //$NON-NLS-1$
                item.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
            }
            for (MySQLCatalog catalog : getDatabaseObject().getDataSource().getCatalogs()) {
                if (hasCatalogGrants(catalog) || extraCatalogs.contains(catalog)) {
                    TableItem item = new TableItem(catalogsTable, SWT.NONE);
                    item.setText(catalog.getName());
                    item.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
                    item.setData(catalog);
                }
            }
            int selIndex = 0;
            if (prevSelection != null) {
                for (int i = 1; i < catalogsTable.getItemCount(); i++) {
                    if (catalogsTable.getItem(i).getData() == prevSelection) {
                        selIndex = i;
                        break;
                    }
                }
            }
            catalogsTable.setSelection(selIndex);
            selectedCatalog = (MySQLCatalog) catalogsTable.getItem(selIndex).getData();
            selectedCatalogs.clear();
            if (selectedCatalog != null) {
                selectedCatalogs.add(selectedCatalog);
            }
            UIUtils.packColumns(catalogsTable);
            highlightCatalogs();
            adjustListHeight(catalogsTable);
        } finally {
            catalogsTable.setRedraw(true);
        }
    }

    private void fillTables()
    {
        if (tablesTable == null || tablesTable.isDisposed()) {
            return;
        }
        tablesTable.setRedraw(false);
        try {
            tablesTable.removeAll();
            {
                TableItem item = new TableItem(tablesTable, SWT.NONE);
                item.setText("% (All)"); //$NON-NLS-1$
                item.setImage(DBeaverIcons.getImage(DBIcon.TREE_TABLE));
            }
            if (catalogTables != null) {
                Set<MySQLTableBase> extra = extraTables.getOrDefault(selectedCatalog, Collections.emptySet());
                for (MySQLTableBase table : catalogTables) {
                    if (!hasTableGrants(table) && !extra.contains(table)) {
                        continue;
                    }
                    TableItem item = new TableItem(tablesTable, SWT.NONE);
                    item.setText(table.getName());
                    item.setImage(DBeaverIcons.getImage(table.isView() ? DBIcon.TREE_VIEW : DBIcon.TREE_TABLE));
                    item.setData(table);
                }
                highlightTables();
            }
            if (tablesAddButton != null && !tablesAddButton.isDisposed()) {
                tablesAddButton.setEnabled(catalogTables != null && selectedCatalog != null);
            }
            int selIndex = 0;
            if (selectedTable != null) {
                for (int i = 1; i < tablesTable.getItemCount(); i++) {
                    if (tablesTable.getItem(i).getData() == selectedTable) {
                        selIndex = i;
                        break;
                    }
                }
            }
            if (selIndex == 0) {
                selectedTable = null;
            }
            selectedTables.clear();
            if (selectedTable != null) {
                selectedTables.add(selectedTable);
            }
            tablesTable.setSelection(selIndex);
            UIUtils.packColumns(tablesTable);
            adjustListHeight(tablesTable);
            updateObjectSections();
        } finally {
            tablesTable.setRedraw(true);
        }
    }

    private void fillColumns()
    {
        if (columnsTable == null || columnsTable.isDisposed()) {
            return;
        }
        columnsTable.setRedraw(false);
        try {
            columnsTable.removeAll();
            if (tableColumns != null) {
                Set<MySQLTableColumn> extra = extraColumns.getOrDefault(selectedTable, Collections.emptySet());
                for (MySQLTableColumn column : tableColumns) {
                    if (!hasColumnGrants(column) && !extra.contains(column)) {
                        continue;
                    }
                    TableItem item = new TableItem(columnsTable, SWT.NONE);
                    item.setText(column.getName());
                    item.setImage(DBeaverIcons.getImage(DBIcon.TREE_COLUMN));
                    item.setData(column);
                }
                highlightColumns();
            }
            if (columnsAddButton != null && !columnsAddButton.isDisposed()) {
                columnsAddButton.setEnabled(tableColumns != null && selectedTable != null);
            }
            int selIndex = -1;
            if (selectedColumn != null) {
                for (int i = 0; i < columnsTable.getItemCount(); i++) {
                    if (columnsTable.getItem(i).getData() == selectedColumn) {
                        selIndex = i;
                        break;
                    }
                }
            }
            if (selIndex < 0) {
                selectedColumn = null;
                columnsTable.deselectAll();
            } else {
                columnsTable.setSelection(selIndex);
            }
            selectedColumns.clear();
            if (selectedColumn != null) {
                selectedColumns.add(selectedColumn);
            }
            UIUtils.packColumns(columnsTable);
            adjustListHeight(columnsTable);
            updateObjectSections();
        } finally {
            columnsTable.setRedraw(true);
        }
    }

    private void fillProcedures()
    {
        if (proceduresTable == null || proceduresTable.isDisposed()) {
            return;
        }
        proceduresTable.setRedraw(false);
        try {
            proceduresTable.removeAll();
            if (catalogProcedures != null) {
                Set<MySQLProcedure> extra = extraProcedures.getOrDefault(selectedCatalog, Collections.emptySet());
                for (MySQLProcedure procedure : catalogProcedures) {
                    if (!hasProcedureGrants(procedure) && !extra.contains(procedure)) {
                        continue;
                    }
                    TableItem item = new TableItem(proceduresTable, SWT.NONE);
                    item.setText(procedure.getName());
                    item.setImage(DBeaverIcons.getImage(DBIcon.TREE_PROCEDURE));
                    item.setData(procedure);
                }
                highlightProcedures();
            }
            if (proceduresAddButton != null && !proceduresAddButton.isDisposed()) {
                proceduresAddButton.setEnabled(catalogProcedures != null && selectedCatalog != null);
            }
            int selIndex = -1;
            if (selectedProcedure != null) {
                for (int i = 0; i < proceduresTable.getItemCount(); i++) {
                    if (proceduresTable.getItem(i).getData() == selectedProcedure) {
                        selIndex = i;
                        break;
                    }
                }
            }
            if (selIndex < 0) {
                selectedProcedure = null;
                proceduresTable.deselectAll();
            } else {
                proceduresTable.setSelection(selIndex);
            }
            selectedProcedures.clear();
            if (selectedProcedure != null) {
                selectedProcedures.add(selectedProcedure);
            }
            UIUtils.packColumns(proceduresTable);
            adjustListHeight(proceduresTable);
            updateObjectSections();
        } finally {
            proceduresTable.setRedraw(true);
        }
    }

    private boolean hasCatalogGrants(MySQLCatalog catalog)
    {
        if (grants != null) {
            for (MySQLGrant grant : grants) {
                if (grant.matches(catalog) && !grant.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasTableGrants(MySQLTableBase table)
    {
        if (grants != null) {
            for (MySQLGrant grant : grants) {
                if (grant.matches(selectedCatalog) && grant.matches(table) && !grant.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasColumnGrants(MySQLTableColumn column)
    {
        if (grants != null) {
            for (MySQLGrant grant : grants) {
                if (grant.matches(selectedCatalog) && grant.matches(selectedTable) && grant.hasColumnPrivileges(column.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasProcedureGrants(MySQLProcedure procedure)
    {
        if (grants != null) {
            for (MySQLGrant grant : grants) {
                if (grant.matches(selectedCatalog) && grant.matchesProcedure(procedure) && !grant.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void handleAddCatalog()
    {
        List<MySQLCatalog> candidates = new ArrayList<>();
        for (MySQLCatalog catalog : getDatabaseObject().getDataSource().getCatalogs()) {
            if (!hasCatalogGrants(catalog) && !extraCatalogs.contains(catalog)) {
                candidates.add(catalog);
            }
        }
        List<MySQLCatalog> catalogs = selectObjects(
            candidates,
            MySQLUIMessages.editors_user_editor_privileges_dialog_add_catalog_title,
            MySQLUIMessages.editors_user_editor_privileges_dialog_add_catalog_message,
            DBIcon.TREE_DATABASE);
        if (catalogs == null) {
            return;
        }
        // Rebuild the list in a fresh event loop turn: rebuilding right after the modal dialog's
        // nested event loop exits leaves a stale native row on macOS (refresh, which rebuilds the
        // same way but from a clean async context, never has this problem)
        catalogsTable.getDisplay().timerExec(100, () -> {
            if (catalogsTable.isDisposed()) {
                return;
            }
            extraCatalogs.addAll(catalogs);
            selectedCatalog = catalogs.get(0);
            selectedTable = null;
            selectedTables.clear();
            selectedColumn = null;
            selectedColumns.clear();
            selectedProcedure = null;
            selectedProcedures.clear();
            fillCatalogs();
            // Select all just added schemas so privileges can be granted to them at once
            selectItemsByData(catalogsTable, catalogs);
            selectedCatalogs.clear();
            selectedCatalogs.addAll(catalogs);
            scheduleRepaint(catalogsTable);
            showCatalogTables();
            showCatalogProcedures();
            showGrants();
        });
    }

    private void handleAddTable()
    {
        if (selectedCatalog == null || catalogTables == null) {
            return;
        }
        Set<Object> shownTables = new HashSet<>();
        for (TableItem item : tablesTable.getItems()) {
            if (item.getData() != null) {
                shownTables.add(item.getData());
            }
        }
        List<MySQLTableBase> candidates = new ArrayList<>();
        for (MySQLTableBase table : catalogTables) {
            if (!shownTables.contains(table)) {
                candidates.add(table);
            }
        }
        List<MySQLTableBase> tables = selectObjects(
            candidates,
            MySQLUIMessages.editors_user_editor_privileges_dialog_add_table_title,
            MySQLUIMessages.editors_user_editor_privileges_dialog_add_table_message,
            DBIcon.TREE_TABLE);
        if (tables == null) {
            return;
        }
        tablesTable.getDisplay().timerExec(100, () -> {
            if (tablesTable.isDisposed()) {
                return;
            }
            extraTables.computeIfAbsent(selectedCatalog, c -> new HashSet<>()).addAll(tables);
            selectedTable = tables.get(0);
            selectedColumn = null;
            selectedColumns.clear();
            selectedProcedure = null;
            selectedProcedures.clear();
            proceduresTable.deselectAll();
            fillTables();
            // Select all just added tables so privileges can be granted to them at once
            selectItemsByData(tablesTable, tables);
            selectedTables.clear();
            selectedTables.addAll(tables);
            scheduleRepaint(tablesTable);
            showTableColumns();
            showGrants();
        });
    }

    private void handleAddColumn()
    {
        if (selectedCatalog == null || selectedTable == null || tableColumns == null) {
            return;
        }
        Set<Object> shownColumns = new HashSet<>();
        for (TableItem item : columnsTable.getItems()) {
            if (item.getData() != null) {
                shownColumns.add(item.getData());
            }
        }
        List<MySQLTableColumn> candidates = new ArrayList<>();
        for (MySQLTableColumn column : tableColumns) {
            if (!shownColumns.contains(column)) {
                candidates.add(column);
            }
        }
        List<MySQLTableColumn> columns = selectObjects(
            candidates,
            MySQLUIMessages.editors_user_editor_privileges_dialog_add_column_title,
            MySQLUIMessages.editors_user_editor_privileges_dialog_add_column_message,
            DBIcon.TREE_COLUMN);
        if (columns == null) {
            return;
        }
        columnsTable.getDisplay().timerExec(100, () -> {
            if (columnsTable.isDisposed()) {
                return;
            }
            extraColumns.computeIfAbsent(selectedTable, t -> new HashSet<>()).addAll(columns);
            selectedColumn = columns.get(0);
            selectedProcedure = null;
            selectedProcedures.clear();
            proceduresTable.deselectAll();
            fillColumns();
            selectItemsByData(columnsTable, columns);
            selectedColumns.clear();
            selectedColumns.addAll(columns);
            scheduleRepaint(columnsTable);
            showGrants();
        });
    }

    private void handleAddProcedure()
    {
        if (selectedCatalog == null || catalogProcedures == null) {
            return;
        }
        Set<Object> shownProcedures = new HashSet<>();
        for (TableItem item : proceduresTable.getItems()) {
            if (item.getData() != null) {
                shownProcedures.add(item.getData());
            }
        }
        List<MySQLProcedure> candidates = new ArrayList<>();
        for (MySQLProcedure procedure : catalogProcedures) {
            if (!shownProcedures.contains(procedure)) {
                candidates.add(procedure);
            }
        }
        List<MySQLProcedure> procedures = selectObjects(
            candidates,
            MySQLUIMessages.editors_user_editor_privileges_dialog_add_procedure_title,
            MySQLUIMessages.editors_user_editor_privileges_dialog_add_procedure_message,
            DBIcon.TREE_PROCEDURE);
        if (procedures == null) {
            return;
        }
        proceduresTable.getDisplay().timerExec(100, () -> {
            if (proceduresTable.isDisposed()) {
                return;
            }
            extraProcedures.computeIfAbsent(selectedCatalog, c -> new HashSet<>()).addAll(procedures);
            selectedProcedure = procedures.get(0);
            selectedTable = null;
            selectedTables.clear();
            selectedColumn = null;
            selectedColumns.clear();
            tablesTable.deselectAll();
            fillProcedures();
            selectItemsByData(proceduresTable, procedures);
            selectedProcedures.clear();
            selectedProcedures.addAll(procedures);
            scheduleRepaint(proceduresTable);
            showTableColumns();
            showGrants();
        });
    }

    /**
     * Sizes the list to its content (min 2, max 15 visible rows - then it scrolls) so the "+"
     * button below it stays right under the last row.
     */
    private static void adjustListHeight(Table table) {
        int rows = Math.max(2, Math.min(table.getItemCount(), 15));
        GridData gd = (GridData) table.getLayoutData();
        int heightHint = table.getHeaderHeight() + table.getItemHeight() * rows + 8;
        if (gd.heightHint != heightHint) {
            gd.heightHint = heightHint;
            table.requestLayout();
        }
    }

    private void handleRemoveCatalog()
    {
        if (grants == null || selectedCatalogs.isEmpty()) {
            return;
        }
        for (MySQLCatalog catalog : new ArrayList<>(selectedCatalogs)) {
            for (MySQLGrant grant : new ArrayList<>(grants)) {
                if (!grant.matches(catalog)) {
                    continue;
                }
                revokeWholeGrant(grant, grantObjectSpec(grant, catalog));
            }
            extraCatalogs.remove(catalog);
            extraTables.remove(catalog);
            extraProcedures.remove(catalog);
            extraColumns.keySet().removeIf(table -> table.getContainer() == catalog);
        }
        selectedCatalog = null;
        selectedCatalogs.clear();
        selectedTable = null;
        selectedTables.clear();
        selectedColumn = null;
        selectedColumns.clear();
        selectedProcedure = null;
        selectedProcedures.clear();
        fillCatalogs();
        showCatalogTables();
        showCatalogProcedures();
        showGrants();
    }

    private void handleRemoveTable()
    {
        if (grants == null || selectedCatalog == null || selectedTables.isEmpty()) {
            return;
        }
        MySQLCatalog catalog = selectedCatalog;
        for (MySQLTableBase table : new ArrayList<>(selectedTables)) {
            for (MySQLGrant grant : new ArrayList<>(grants)) {
                if (grant.matches(catalog) && grant.matches(table)) {
                    revokeWholeGrant(grant, tableSpec(catalog, table.getName()));
                }
            }
            Set<MySQLTableBase> extra = extraTables.get(catalog);
            if (extra != null) {
                extra.remove(table);
            }
            extraColumns.remove(table);
        }
        selectedTable = null;
        selectedTables.clear();
        selectedColumn = null;
        selectedColumns.clear();
        fillTables();
        showTableColumns();
        highlightCatalogs();
        showGrants();
    }

    private void handleRemoveColumn()
    {
        if (grants == null || selectedCatalog == null || selectedTable == null || selectedColumns.isEmpty()) {
            return;
        }
        final MySQLCatalog catalog = selectedCatalog;
        final MySQLTableBase table = selectedTable;
        for (MySQLTableColumn column : new ArrayList<>(selectedColumns)) {
            for (MySQLGrant grant : new ArrayList<>(grants)) {
                if (!grant.matches(catalog) || !grant.matches(table)) {
                    continue;
                }
                List<String> privNames = new ArrayList<>();
                List<MySQLPrivilege> affected = new ArrayList<>();
                for (MySQLPrivilege priv : new ArrayList<>(grant.getColumnPrivileges().keySet())) {
                    if (grant.hasColumnPrivilege(priv, column.getName())) {
                        affected.add(priv);
                        privNames.add(priv.getFixedPrivilegeName().toUpperCase(Locale.ROOT)
                            + " (" + DBUtils.getQuotedIdentifier(column) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
                if (affected.isEmpty()) {
                    continue;
                }
                for (MySQLPrivilege priv : affected) {
                    grant.removeColumnPrivilege(priv, column.getName());
                }
                getDatabaseObject().clearGrantsCache();
                final MySQLGrant curGrant = grant;
                final MySQLTableColumn curColumn = column;
                addChangeCommand(
                    new MySQLCommandRevokeObjectGrants(getDatabaseObject(), tableSpec(catalog, table.getName()), privNames, false),
                    new DBECommandReflector<MySQLUser, MySQLCommandRevokeObjectGrants>() {
                        @Override
                        public void redoCommand(@NotNull MySQLCommandRevokeObjectGrants command) {
                            for (MySQLPrivilege priv : affected) {
                                curGrant.removeColumnPrivilege(priv, curColumn.getName());
                            }
                            refreshObjectLists();
                        }
                        @Override
                        public void undoCommand(@NotNull MySQLCommandRevokeObjectGrants command) {
                            for (MySQLPrivilege priv : affected) {
                                curGrant.addColumnPrivilege(priv, curColumn.getName());
                            }
                            refreshObjectLists();
                        }
                    });
            }
            Set<MySQLTableColumn> extra = extraColumns.get(table);
            if (extra != null) {
                extra.remove(column);
            }
        }
        selectedColumn = null;
        selectedColumns.clear();
        fillColumns();
        highlightTables();
        highlightCatalogs();
        showGrants();
    }

    private void handleRemoveProcedure()
    {
        if (grants == null || selectedCatalog == null || selectedProcedures.isEmpty()) {
            return;
        }
        MySQLCatalog catalog = selectedCatalog;
        for (MySQLProcedure procedure : new ArrayList<>(selectedProcedures)) {
            for (MySQLGrant grant : new ArrayList<>(grants)) {
                if (grant.matches(catalog) && grant.matchesProcedure(procedure)) {
                    revokeWholeGrant(grant, procedureSpec(catalog, grant.getObjectType(), procedure.getName()));
                }
            }
            Set<MySQLProcedure> extra = extraProcedures.get(catalog);
            if (extra != null) {
                extra.remove(procedure);
            }
        }
        selectedProcedure = null;
        selectedProcedures.clear();
        fillProcedures();
        highlightCatalogs();
        showGrants();
    }

    /**
     * Creates a REVOKE command covering everything the given grant holds and drops the grant
     * from the local model.
     */
    private void revokeWholeGrant(MySQLGrant grant, String objectSpec)
    {
        // Revoke exactly the privileges the grant holds, each at its own level. Table-level and
        // column-level privileges go in separate statements: mixing them (or listing a privilege
        // that isn't actually held) makes the server reject the whole statement.
        List<String> tablePrivNames = new ArrayList<>();
        if (grant.isAllPrivileges()) {
            tablePrivNames.add("ALL PRIVILEGES"); //$NON-NLS-1$
        } else {
            for (MySQLPrivilege priv : grant.getPrivileges()) {
                tablePrivNames.add(priv.getFixedPrivilegeName().toUpperCase(Locale.ROOT));
            }
        }
        List<String> columnPrivNames = new ArrayList<>();
        for (Map.Entry<MySQLPrivilege, Set<String>> entry : grant.getColumnPrivileges().entrySet()) {
            StringJoiner columnList = new StringJoiner(", ", " (", ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            for (String column : entry.getValue()) {
                columnList.add(DBUtils.getQuotedIdentifier(getDatabaseObject().getDataSource(), column));
            }
            columnPrivNames.add(entry.getKey().getFixedPrivilegeName().toUpperCase(Locale.ROOT) + columnList);
        }
        grants.remove(grant);
        getDatabaseObject().clearGrantsCache();
        final MySQLGrant curGrant = grant;
        DBECommandReflector<MySQLUser, MySQLCommandRevokeObjectGrants> reflector =
            new DBECommandReflector<>() {
                @Override
                public void redoCommand(@NotNull MySQLCommandRevokeObjectGrants command) {
                    grants.remove(curGrant);
                    refreshObjectLists();
                }
                @Override
                public void undoCommand(@NotNull MySQLCommandRevokeObjectGrants command) {
                    if (!grants.contains(curGrant)) {
                        grants.add(curGrant);
                    }
                    refreshObjectLists();
                }
            };
        boolean grantOptionEmitted = false;
        if (!tablePrivNames.isEmpty()) {
            addChangeCommand(new MySQLCommandRevokeObjectGrants(getDatabaseObject(), objectSpec, tablePrivNames, grant.isGrantOption()), reflector);
            grantOptionEmitted = grant.isGrantOption();
        }
        if (!columnPrivNames.isEmpty()) {
            addChangeCommand(new MySQLCommandRevokeObjectGrants(getDatabaseObject(), objectSpec, columnPrivNames, !grantOptionEmitted && grant.isGrantOption()), reflector);
            grantOptionEmitted |= grant.isGrantOption();
        }
        if (!grantOptionEmitted && grant.isGrantOption()) {
            addChangeCommand(new MySQLCommandRevokeObjectGrants(getDatabaseObject(), objectSpec, List.of(), true), reflector);
        }
    }

    private String grantObjectSpec(MySQLGrant grant, MySQLCatalog catalog)
    {
        if (grant.getObjectType() != MySQLGrant.ObjectType.TABLE) {
            return procedureSpec(catalog, grant.getObjectType(), grant.getTable());
        }
        return grant.isAllTables()
            ? DBUtils.getQuotedIdentifier(catalog) + ".*" //$NON-NLS-1$
            : tableSpec(catalog, grant.getTable());
    }

    private String tableSpec(MySQLCatalog catalog, String tableName)
    {
        return DBUtils.getQuotedIdentifier(catalog) + "." //$NON-NLS-1$
            + DBUtils.getQuotedIdentifier(catalog.getDataSource(), tableName);
    }

    private String procedureSpec(MySQLCatalog catalog, MySQLGrant.ObjectType objectType, String procedureName)
    {
        return (objectType == MySQLGrant.ObjectType.FUNCTION ? "FUNCTION " : "PROCEDURE ") //$NON-NLS-1$ //$NON-NLS-2$
            + DBUtils.getQuotedIdentifier(catalog) + "." //$NON-NLS-1$
            + DBUtils.getQuotedIdentifier(catalog.getDataSource(), procedureName);
    }

    private void refreshObjectLists()
    {
        fillCatalogs();
        fillTables();
        fillColumns();
        fillProcedures();
        showGrants();
    }

    /**
     * Shows the Tables/Columns/Procedures sections only when the selected parent object actually
     * has entities of that kind (a schema without procedures gets no Procedures section etc.).
     */
    private void updateObjectSections()
    {
        if (tablesGroup == null || tablesGroup.isDisposed()) {
            return;
        }
        boolean changed = setSectionVisible(tablesGroup, catalogTables != null && !catalogTables.isEmpty());
        changed |= setSectionVisible(proceduresGroup, catalogProcedures != null && !catalogProcedures.isEmpty());
        changed |= setSectionVisible(columnsGroup, selectedTable != null && tableColumns != null && !tableColumns.isEmpty());
        if (changed) {
            // Relayout the whole left pane - the sections live in different sub-panes
            columnsGroup.getParent().getParent().layout(true, true);
        }
    }

    private static boolean setSectionVisible(Composite group, boolean visible)
    {
        // createTitledComposite() returns the inner client area; the layout data belongs
        // to its parent (the titled wrapper), which is what has to be hidden
        Composite host = group.getParent();
        GridData gd = (GridData) host.getLayoutData();
        if (gd.exclude == visible) {
            gd.exclude = !visible;
            host.setVisible(visible);
            return true;
        }
        return false;
    }

    private void updateRemoveButtons()
    {
        if (catalogsRemoveButton == null || catalogsRemoveButton.isDisposed()) {
            return;
        }
        catalogsRemoveButton.setEnabled(selectedCatalog != null);
        tablesRemoveButton.setEnabled(selectedTable != null);
        columnsRemoveButton.setEnabled(selectedTable != null && selectedColumn != null);
        proceduresRemoveButton.setEnabled(selectedProcedure != null);
    }

    private static void selectItemsByData(Table table, List<? extends DBSObject> objects) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < table.getItemCount(); i++) {
            Object data = table.getItem(i).getData();
            if (data != null && objects.contains(data)) {
                indices.add(i);
            }
        }
        int[] selection = new int[indices.size()];
        for (int i = 0; i < selection.length; i++) {
            selection[i] = indices.get(i);
        }
        table.setSelection(selection);
    }

    /**
     * Deferred repaint of the list after it was rebuilt right after a modal dialog closed
     * (belt-and-braces against stale native row rendering on macOS).
     */
    private static void scheduleRepaint(Table table) {
        Runnable repaint = () -> {
            if (!table.isDisposed()) {
                table.redraw();
                table.update();
            }
        };
        UIUtils.asyncExec(repaint);
        table.getDisplay().timerExec(600, repaint);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <T extends DBSObject> List<T> selectObjects(List<T> candidates, String title, String message, DBIcon icon)
    {
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(getSite().getShell(), new LabelProvider() {
            @Override
            public String getText(Object element) {
                return ((DBSObject) element).getName();
            }

            @Override
            public Image getImage(Object element) {
                return DBeaverIcons.getImage(icon);
            }
        });
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setElements(candidates.toArray());
        dialog.setMultipleSelection(true);
        if (dialog.open() != Window.OK) {
            return null;
        }
        Object[] result = dialog.getResult();
        if (result == null || result.length == 0) {
            return null;
        }
        List<T> selected = new ArrayList<>(result.length);
        for (Object object : result) {
            selected.add((T) object);
        }
        return selected;
    }

    private void showCatalogTables()
    {
        LoadingJob.createService(
                new DatabaseLoadService<>(MySQLUIMessages.editors_user_editor_privileges_service_load_tables, getExecutionContext()) {
                    @Override
                    public Collection<MySQLTableBase> evaluate(@NotNull DBRProgressMonitor monitor) {
                        if (selectedCatalog == null) {
                            return Collections.emptyList();
                        }
                        try {
                            return selectedCatalog.getTableCache().getAllObjects(monitor, selectedCatalog);
                        } catch (DBException e) {
                            log.error(e);
                        }
                        return null;
                    }
                },
            pageControl.createTablesLoadVisualizer())
            .schedule();
    }

    private void showTableColumns()
    {
        LoadingJob.createService(
                new DatabaseLoadService<>(MySQLUIMessages.editors_user_editor_privileges_service_load_columns, getExecutionContext()) {
                    @Override
                    public Collection<MySQLTableColumn> evaluate(@NotNull DBRProgressMonitor monitor) {
                        if (selectedTable == null) {
                            return Collections.emptyList();
                        }
                        try {
                            return selectedTable.getAttributes(monitor);
                        } catch (DBException e) {
                            log.error(e);
                        }
                        return null;
                    }
                },
            pageControl.createColumnsLoadVisualizer())
            .schedule();
    }

    private void showCatalogProcedures()
    {
        LoadingJob.createService(
                new DatabaseLoadService<>(MySQLUIMessages.editors_user_editor_privileges_service_load_procedures, getExecutionContext()) {
                    @Override
                    public Collection<MySQLProcedure> evaluate(@NotNull DBRProgressMonitor monitor) {
                        if (selectedCatalog == null) {
                            return Collections.emptyList();
                        }
                        try {
                            return selectedCatalog.getProceduresCache().getAllObjects(monitor, selectedCatalog);
                        } catch (DBException e) {
                            log.error(e);
                        }
                        return null;
                    }
                },
            pageControl.createProceduresLoadVisualizer())
            .schedule();
    }

    private void showGrants()
    {
        updateRemoveButtons();
        if (grants == null) {
            return;
        }
        if (selectedProcedure != null) {
            if (selectedProcedures.size() > 1) {
                final MySQLCatalog curCatalog = selectedCatalog;
                fillTriState(procedurePrivilegesTable, procedurePrivilegeTypes, selectedProcedures.size(), priv -> {
                    int count = 0;
                    for (MySQLProcedure procedure : selectedProcedures) {
                        if (hasProcedurePrivilege(curCatalog, procedure, priv)) {
                            count++;
                        }
                    }
                    return count;
                });
            } else {
                List<MySQLGrant> procedureGrants = new ArrayList<>();
                for (MySQLGrant grant : grants) {
                    if (grant.matches(selectedCatalog) && grant.matchesProcedure(selectedProcedure)) {
                        procedureGrants.add(grant);
                    }
                }
                procedurePrivilegesTable.fillGrants(procedureGrants, true);
            }
            updatePrivilegePanels(false, false, true, false);
            return;
        }
        if (selectedColumn != null && selectedTable != null) {
            Set<MySQLPrivilege> enabled = new HashSet<>();
            List<MySQLPrivilege> partial = new ArrayList<>();
            int columnCount = selectedColumns.size();
            if (columnPrivilegeTypes != null) {
                for (MySQLPrivilege priv : columnPrivilegeTypes) {
                    int count = 0;
                    for (MySQLTableColumn column : selectedColumns) {
                        if (hasColumnPrivilegeState(selectedCatalog, selectedTable, column, priv)) {
                            count++;
                        }
                    }
                    if (columnCount > 0 && count == columnCount) {
                        enabled.add(priv);
                    } else if (count > 0) {
                        partial.add(priv);
                    }
                }
            }
            boolean grantOption = false;
            for (MySQLGrant grant : grants) {
                if (grant.matches(selectedCatalog) && grant.matches(selectedTable) && grant.isGrantOption()) {
                    grantOption = true;
                    break;
                }
            }
            columnPrivilegesTable.fillCheckedPrivileges(enabled, partial, grantOption, false, true);
            updatePrivilegePanels(false, true, false, false);
            return;
        }
        if (selectedTable != null && selectedTables.size() > 1) {
            final MySQLCatalog curCatalog = selectedCatalog;
            fillTriState(tablePrivilegesTable, tablePrivilegeTypes, selectedTables.size(), priv -> {
                int count = 0;
                for (MySQLTableBase table : selectedTables) {
                    if (hasTablePrivilege(curCatalog, table, priv)) {
                        count++;
                    }
                }
                return count;
            });
            updatePrivilegePanels(true, false, false, false);
            return;
        }
        if (selectedTable == null && selectedCatalogs.size() > 1) {
            java.util.function.ToIntFunction<MySQLPrivilege> catalogCounter = priv -> {
                int count = 0;
                for (MySQLCatalog catalog : selectedCatalogs) {
                    if (hasTablePrivilege(catalog, null, priv)) {
                        count++;
                    }
                }
                return count;
            };
            fillTriState(tablePrivilegesTable, tablePrivilegeTypes, selectedCatalogs.size(), catalogCounter);
            fillTriState(otherPrivilegesTable, otherPrivilegeTypes, selectedCatalogs.size(), catalogCounter);
            updatePrivilegePanels(true, false, false, true);
            return;
        }
        List<MySQLGrant> curGrants = new ArrayList<>();
        for (MySQLGrant grant : grants) {
            if (grant.matches(selectedCatalog) && grant.matches(selectedTable)) {
                curGrants.add(grant);
            }
        }
        tablePrivilegesTable.fillGrants(curGrants, true);
        if (selectedTable == null) {
            otherPrivilegesTable.fillGrants(curGrants, true);
            updatePrivilegePanels(true, false, false, true);
        } else {
            updatePrivilegePanels(true, false, false, false);
        }
    }

    /**
     * Fills a privilege panel with tri-state checks computed across a multi-selection:
     * checked - all selected objects have the privilege, partial - only some of them.
     */
    private void fillTriState(
        PrivilegeTableControl panel,
        @Nullable List<MySQLPrivilege> privilegeTypes,
        int totalCount,
        java.util.function.ToIntFunction<MySQLPrivilege> counter
    ) {
        List<MySQLPrivilege> checked = new ArrayList<>();
        List<MySQLPrivilege> partial = new ArrayList<>();
        boolean grantOptionAll = false;
        boolean grantOptionPartial = false;
        if (privilegeTypes != null) {
            for (MySQLPrivilege priv : privilegeTypes) {
                int count = counter.applyAsInt(priv);
                if (priv.isGrantOption()) {
                    grantOptionAll = totalCount > 0 && count == totalCount;
                    grantOptionPartial = count > 0 && count < totalCount;
                } else if (totalCount > 0 && count == totalCount) {
                    checked.add(priv);
                } else if (count > 0) {
                    partial.add(priv);
                }
            }
        }
        panel.fillCheckedPrivileges(checked, partial, grantOptionAll, grantOptionPartial, true);
    }

    /**
     * Shows only the privilege panels applicable to the current selection - hidden panels
     * are excluded from the layout so the visible ones take all the space.
     */
    private void updatePrivilegePanels(boolean tableVisible, boolean columnVisible, boolean procedureVisible, boolean otherVisible) {
        boolean changed = setPanelVisible(tablePrivilegesTable, tableVisible);
        changed |= setPanelVisible(columnPrivilegesTable, columnVisible);
        changed |= setPanelVisible(procedurePrivilegesTable, procedureVisible);
        changed |= setPanelVisible(otherPrivilegesTable, otherVisible);
        if (changed) {
            tablePrivilegesTable.getParent().layout(true, true);
        }
    }

    private static boolean setPanelVisible(PrivilegeTableControl panel, boolean visible) {
        GridData gd = (GridData) panel.getLayoutData();
        if (gd.exclude == visible) {
            gd.exclude = !visible;
            panel.setVisible(visible);
            return true;
        }
        return false;
    }

    @Override
    public synchronized void activatePart()
    {
        if (isLoaded) {
            return;
        }
        isLoaded = true;
        LoadingJob.createService(
                new DatabaseLoadService<>(MySQLUIMessages.editors_user_editor_privileges_service_load_privileges, getExecutionContext()) {
                    @Override
                    public java.util.List<MySQLPrivilege> evaluate(@NotNull DBRProgressMonitor monitor) throws InvocationTargetException {
                        try {
                            return getDatabaseObject().getDataSource().getPrivileges(monitor);
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                },
            pageControl.createPrivilegesLoadVisualizer())
            .schedule();
    }

    @Override
    protected PageControl getPageControl()
    {
        return pageControl;
    }

    @Override
    protected void processGrants(List<MySQLGrant> grantsTmp)
    {
        this.grants = new ArrayList<>(grantsTmp);
        for (Iterator<MySQLGrant> i = grants.iterator(); i.hasNext();) {
            MySQLGrant grant = i.next();
            if (!grant.isAllPrivileges() && !grant.hasNonAdminPrivileges() && !grant.isGrantOption()) {
                i.remove();
            }
        }
        fillCatalogs();

        showGrants();
        showCatalogTables();
        showCatalogProcedures();
    }

    private void highlightCatalogs()
    {
        // Highlight granted catalogs
        if (catalogsTable != null && !catalogsTable.isDisposed()) {
            for (TableItem item : catalogsTable.getItems()) {
                MySQLCatalog catalog = (MySQLCatalog)item.getData();
                item.setFont(null);
                if (grants != null) {
                    for (MySQLGrant grant : grants) {
                        if (grant.matches(catalog) && !grant.isEmpty()) {
                            item.setFont(BaseThemeSettings.instance.treeAndTableFont);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void highlightTables()
    {
        if (tablesTable != null && !tablesTable.isDisposed()) {
            for (TableItem item : tablesTable.getItems()) {
                MySQLTableBase table = (MySQLTableBase) item.getData();
                item.setFont(null);
                if (grants != null) {
                    for (MySQLGrant grant : grants) {
                        if (grant.matches(selectedCatalog) && grant.matches(table) && !grant.isEmpty()) {
                            item.setFont(BaseThemeSettings.instance.treeAndTableFont);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void highlightColumns()
    {
        if (columnsTable != null && !columnsTable.isDisposed()) {
            for (TableItem item : columnsTable.getItems()) {
                MySQLTableColumn column = (MySQLTableColumn) item.getData();
                item.setFont(null);
                if (grants != null && column != null) {
                    for (MySQLGrant grant : grants) {
                        if (grant.matches(selectedCatalog) && grant.matches(selectedTable) && grant.hasColumnPrivileges(column.getName())) {
                            item.setFont(BaseThemeSettings.instance.treeAndTableFont);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void highlightProcedures()
    {
        if (proceduresTable != null && !proceduresTable.isDisposed()) {
            for (TableItem item : proceduresTable.getItems()) {
                MySQLProcedure procedure = (MySQLProcedure) item.getData();
                item.setFont(null);
                if (grants != null) {
                    for (MySQLGrant grant : grants) {
                        if (grant.matches(selectedCatalog) && grant.matchesProcedure(procedure) && !grant.isEmpty()) {
                            item.setFont(BaseThemeSettings.instance.treeAndTableFont);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public RefreshResult refreshPart(Object source, boolean force)
    {
        if (force ||
            (source instanceof DBNEvent event && event.getSource() == DBNEvent.UPDATE_ON_SAVE) ||
            !isLoaded
        ) {
            isLoaded = false;
            // Reset manually added objects: after refresh only objects with actual grants remain
            extraCatalogs.clear();
            extraTables.clear();
            extraColumns.clear();
            extraProcedures.clear();
            selectedTable = null;
            selectedColumn = null;
            selectedProcedure = null;
            selectedCatalogs.clear();
            if (selectedCatalog != null) {
                selectedCatalogs.add(selectedCatalog);
            }
            selectedTables.clear();
            selectedColumns.clear();
            selectedProcedures.clear();
            activatePart();
            return RefreshResult.REFRESHED;
        }
        return RefreshResult.IGNORED;
    }

    private class PageControl extends UserPageControl {
        public PageControl(Composite parent) {
            super(parent);
        }

        public ProgressVisualizer<Collection<MySQLTableBase>> createTablesLoadVisualizer() {
            return new ProgressVisualizer<>() {
                @Override
                public void completeLoading(@Nullable Collection<MySQLTableBase> tables) {
                    super.completeLoading(tables);
                    if (tablesTable.isDisposed()) {
                        return;
                    }
                    catalogTables = tables == null ? null : new ArrayList<>(tables);
                    fillTables();
                    showTableColumns();
                    // Async load completion repaints parts of the page over already-drawn lists
                    if (!catalogsTable.isDisposed()) {
                        catalogsTable.redraw();
                    }
                }
            };
        }

        public ProgressVisualizer<Collection<MySQLTableColumn>> createColumnsLoadVisualizer() {
            return new ProgressVisualizer<>() {
                @Override
                public void completeLoading(@Nullable Collection<MySQLTableColumn> columns) {
                    super.completeLoading(columns);
                    if (columnsTable.isDisposed()) {
                        return;
                    }
                    tableColumns = columns == null ? null : new ArrayList<>(columns);
                    fillColumns();
                    if (!tablesTable.isDisposed()) {
                        tablesTable.redraw();
                    }
                }
            };
        }

        public ProgressVisualizer<Collection<MySQLProcedure>> createProceduresLoadVisualizer() {
            return new ProgressVisualizer<>() {
                @Override
                public void completeLoading(@Nullable Collection<MySQLProcedure> procedures) {
                    super.completeLoading(procedures);
                    if (proceduresTable.isDisposed()) {
                        return;
                    }
                    catalogProcedures = procedures == null ? null : new ArrayList<>(procedures);
                    fillProcedures();
                    if (!catalogsTable.isDisposed()) {
                        catalogsTable.redraw();
                    }
                }
            };
        }

        public ProgressVisualizer<java.util.List<MySQLPrivilege>> createPrivilegesLoadVisualizer() {
            return new ProgressVisualizer<>() {
                @Override
                public void completeLoading(@Nullable java.util.List<MySQLPrivilege> privs) {
                    super.completeLoading(privs);
                    List<MySQLPrivilege> otherPrivs = new ArrayList<>();
                    List<MySQLPrivilege> tablePrivs = new ArrayList<>();
                    List<MySQLPrivilege> procedurePrivs = new ArrayList<>();
                    List<MySQLPrivilege> columnPrivs = new ArrayList<>();
                    if (privs != null) {
                        for (MySQLPrivilege priv : privs) {
                            if (priv.getKind() == MySQLPrivilege.Kind.ADMIN) {
                                continue;
                            }
                            if (priv.getContext().contains("Procedure")) {
                                // Routine-level privileges (Execute, Alter routine, Grant option).
                                // They stay in the other panels too - they are valid on database level as well.
                                procedurePrivs.add(priv);
                            }
                            String name = priv.getName();
                            if (name.equalsIgnoreCase("Select") || name.equalsIgnoreCase("Insert") //$NON-NLS-1$ //$NON-NLS-2$
                                || name.equalsIgnoreCase("Update") || name.equalsIgnoreCase("References") //$NON-NLS-1$ //$NON-NLS-2$
                            ) {
                                // Privileges MySQL allows on individual columns
                                columnPrivs.add(priv);
                            }
                            if (priv.getContext().contains("Table")) {
                                tablePrivs.add(priv);
                            } else {
                                otherPrivs.add(priv);
                            }
                        }
                    }
                    columnPrivilegeTypes = columnPrivs;
                    tablePrivilegeTypes = tablePrivs;
                    otherPrivilegeTypes = otherPrivs;
                    procedurePrivilegeTypes = procedurePrivs;
                    tablePrivilegesTable.fillPrivileges(tablePrivs);
                    columnPrivilegesTable.fillPrivileges(columnPrivs);
                    procedurePrivilegesTable.fillPrivileges(procedurePrivs);
                    otherPrivilegesTable.fillPrivileges(otherPrivs);
                    loadGrants();
                }
            };
        }

    }


}