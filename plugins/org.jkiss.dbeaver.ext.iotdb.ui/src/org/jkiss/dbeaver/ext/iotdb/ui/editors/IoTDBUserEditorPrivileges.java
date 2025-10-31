/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.iotdb.ui.editors;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.iotdb.model.IoTDBGrant;
import org.jkiss.dbeaver.ext.iotdb.model.IoTDBPrivilege;
import org.jkiss.dbeaver.ext.iotdb.model.IoTDBRelationalUser;
import org.jkiss.dbeaver.ext.iotdb.ui.config.IoTDBCommandGrantPrivilege;
import org.jkiss.dbeaver.ext.iotdb.ui.controls.PrivilegeTableControl;
import org.jkiss.dbeaver.ext.iotdb.ui.internal.IoTDBUiMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IoTDBUserEditorPrivileges extends IoTDBUserEditorAbstract {

    private static Log log = Log.getLog(IoTDBUserEditorPrivileges.class);

    private PageControl pageControl;
    private Table databasesTable;
    private Table tablesTable;

    private boolean isLoaded = false;
    private IoTDBRelationalUser.IoTDBDatabase selectedDatabase;
    private String selectedTable;
    private PrivilegeTableControl tablePrivilegesTable;
    private volatile List<IoTDBGrant> grants;

    private Font boldFont;

    @Override
    public void createPartControl(Composite parent) {
        boldFont = UIUtils.makeBoldFont(parent.getFont());

        pageControl = new PageControl(parent);

        GridData gd = new GridData(GridData.FILL_BOTH);
        CustomSashForm sash = new CustomSashForm(pageControl, SWT.HORIZONTAL);
        sash.setLayoutData(gd);

        // left side
        Composite leftPane = UIUtils.createPlaceholder(sash, 2);
        leftPane.setLayoutData(new GridData(GridData.FILL_BOTH));
        leftPane.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());

        // databases
        {
            Composite databaseGroup = UIUtils.createControlGroup(leftPane, "Databases", 1, GridData.FILL_BOTH, 0);
            databasesTable = new Table(databaseGroup, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
            databasesTable.setHeaderVisible(true);
            gd = new GridData(GridData.FILL_BOTH);
            databasesTable.setLayoutData(gd);
            databasesTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selIndex = databasesTable.getSelectionIndex();
                    if (selIndex < 0) {
                        selectedDatabase = null;
                    } else {
                        selectedDatabase = (IoTDBRelationalUser.IoTDBDatabase) databasesTable.getItem(selIndex).getData();
                    }
                    showDatabaseTables();
                }
            });
            UIUtils.createTableColumn(databasesTable, SWT.LEFT, "Database");
            TableItem item = new TableItem(databasesTable, SWT.NONE);
            item.setText("(ALL)");
            item.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
            item.setData(getDatabaseObject().getDatabaseAll());
            for (IoTDBRelationalUser.IoTDBDatabase db : getDatabaseObject().getDatabases()) {
                item = new TableItem(databasesTable, SWT.NONE);
                item.setText(db.name);
                item.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
                item.setData(db);
            }
            UIUtils.packColumns(databasesTable);
        }

        // tables
        {
            Composite tablesGroup = UIUtils.createControlGroup(leftPane, "Tables", 1, GridData.FILL_BOTH, 0);
            tablesTable = new Table(tablesGroup, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
            tablesTable.setHeaderVisible(true);
            gd = new GridData(GridData.FILL_BOTH);
            tablesTable.setLayoutData(gd);
            tablesTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selIndex = tablesTable.getSelectionIndex();
                    if (selIndex < 0) {
                        selectedTable = null;
                    } else {
                        selectedTable = tablesTable.getItem(selIndex).getText();
                    }
                    showGrants();
                }
            });
            UIUtils.createTableColumn(tablesTable, SWT.LEFT, "Table");
            UIUtils.packColumns(tablesTable);
        }

        // right side
        Composite rightPane = UIUtils.createPlaceholder(sash, 1);
        rightPane.setLayoutData(new GridData(GridData.FILL_BOTH));

        // table privileges
        {
            tablePrivilegesTable = new PrivilegeTableControl(rightPane, "Table Privileges");
            gd = new GridData(GridData.FILL_BOTH);
            tablePrivilegesTable.setLayoutData(gd);
        }

        sash.setSashBorders(new boolean[]{false, false});

        pageControl.createProgressPanel();

        parent.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                UIUtils.dispose(boldFont);
            }
        });

        tablePrivilegesTable.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event event) {
                final IoTDBPrivilege privilege = (IoTDBPrivilege) event.data;
                final int tp = event.detail;
                final String db = selectedDatabase.name;
                final String tb = selectedTable;
                addChangeCommand(
                    new IoTDBCommandGrantPrivilege(getDatabaseObject(), tp, db, tb, privilege),
                    new DBECommandReflector<IoTDBRelationalUser, IoTDBCommandGrantPrivilege>() {
                        @Override
                        public void redoCommand(@NotNull IoTDBCommandGrantPrivilege command) {
                            // no-op
                        }

                        @Override
                        public void undoCommand(@NotNull IoTDBCommandGrantPrivilege command) {
                            // no-op
                        }
                    });
            }
        });
    }

    private void showDatabaseTables() {
        LoadingJob.createService(
            new DatabaseLoadService<List<String>>(
                    IoTDBUiMessages.editors_user_editor_privileges_service_load_tables, getExecutionContext()) {
                @Override
                public List<String> evaluate(@NotNull DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    if (selectedDatabase == null) {
                        return Collections.emptyList();
                    }
                    List<String> tables = new ArrayList<>();
                    if (!selectedDatabase.name.equals("(ALL)")) {
                        tables.add("(ALL)");
                    }
                    try {
                        tables.addAll(selectedDatabase.tables);
                        return tables;
                    } catch (Exception e) {
                        log.error("Error loading tables", e);
                    }
                    return null;
                }
            },
            pageControl.createTablesLoadVisualizer()
        ).schedule();
    }

    private void showGrants() {
        if (grants == null) {
            return;
        }
        List<IoTDBGrant> currentGrants = new ArrayList<>();
        String db = "";
        String tb = "";
        for (IoTDBGrant grant : grants) {
            db = selectedDatabase.name.equals("(ALL)") ? "*" : selectedDatabase.name;
            tb = selectedTable.equals("(ALL)") ? "*" : selectedTable;
            if (grant.matches(db, tb)) {
                currentGrants.add(grant);
            }
        }
        tablePrivilegesTable.fillGrants(currentGrants);
    }

    @Override
    public synchronized void activatePart() {
        if (isLoaded) {
            return;
        }
        DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext == null) {
            return;
        }
        isLoaded = true;
        LoadingJob.createService(
            new DatabaseLoadService<>(
                IoTDBUiMessages.editors_user_editor_privileges_service_load_privileges,
                executionContext
            ) {
                @Override
                public List<IoTDBPrivilege> evaluate(@NotNull DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    IoTDBRelationalUser user = getDatabaseObject();
                    if (user == null) {
                        isLoaded = false;
                        return null;
                    }
                    return user.getDataSource().getPrivilegesByKind(false);
                }
            },
            pageControl.createPrivilegesLoadVisualizer()
        ).schedule();
    }

    @Override
    protected PageControl getPageControl() {
        return pageControl;
    }

    @Override
    protected void processGrants(List<IoTDBGrant> grantsTmp) {
        this.grants = new ArrayList<>(grantsTmp);
        highlightDatabases();
        showGrants();
        showDatabaseTables();
    }

    /**
     * Highlight databases with granted privileges
     */
    private void highlightDatabases() {
        if (databasesTable != null && !databasesTable.isDisposed()) {
            for (TableItem item : databasesTable.getItems()) {
                String db = item.getText();
                if (db.equals("(ALL)")) {
                    db = "*";
                }
                item.setFont(null);
                if (grants != null) {
                    for (IoTDBGrant grant : grants) {
                        if (grant.canHighlightDatabase(db)) {
                            item.setFont(boldFont);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Highlight tables with granted privileges
     */
    private void highlightTables() {
        if (tablesTable != null && !tablesTable.isDisposed()) {
            for (TableItem item : tablesTable.getItems()) {
                String tb = item.getText();
                if (tb.equals("(ALL)")) {
                    tb = "*";
                }
                item.setFont(null);
                if (grants != null) {
                    for (IoTDBGrant grant : grants) {
                        if (grant.canHighlightTable(selectedDatabase.name, tb)) {
                            item.setFont(boldFont);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public RefreshResult refreshPart(Object source, boolean force) {
        if (force ||
            (source instanceof DBNEvent && ((DBNEvent) source).getSource() == DBNEvent.UPDATE_ON_SAVE) ||
            !isLoaded) {
            isLoaded = false;
            activatePart();
            return RefreshResult.REFRESHED;
        }
        return RefreshResult.IGNORED;
    }

    private class PageControl extends UserPageControl {
        public PageControl(Composite parent) {
            super(parent);
        }

        public ProgressVisualizer<List<String>> createTablesLoadVisualizer() {
            return new ProgressVisualizer<List<String>>() {
                @Override
                public void completeLoading(List<String> tables) {
                    super.completeLoading(tables);
                    if (tablesTable.isDisposed()) {
                        return;
                    }
                    tablesTable.removeAll();;
                    if (tables != null) {
                        for (String table : tables) {
                            TableItem item = new TableItem(tablesTable, SWT.NONE);
                            item.setText(table);
                            item.setImage(DBeaverIcons.getImage(DBIcon.TREE_TABLE));
                            item.setData(table);
                        }
                        highlightTables();
                    }
                    UIUtils.packColumns(tablesTable);
                }
            };
        }

        public ProgressVisualizer<List<IoTDBPrivilege>> createPrivilegesLoadVisualizer() {
            return new ProgressVisualizer<List<IoTDBPrivilege>>() {
                @Override
                public void completeLoading(List<IoTDBPrivilege> privileges) {
                    super.completeLoading(privileges);
                    tablePrivilegesTable.fillPrivileges(privileges);
                    loadGrants();
                }
            };
        }
    }
}
