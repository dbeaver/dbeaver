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

import org.eclipse.jface.action.IContributionManager;
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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * Object-centric privileges editor: shows which users have grants on a schema/table/procedure
 * and lets them be edited with the standard editor Save/Revert/Refresh flow (deferred commands).
 */
public class MySQLObjectPrivilegesEditor extends AbstractDatabaseObjectEditor<DBSObject> {

    private static final Log log = Log.getLog(MySQLObjectPrivilegesEditor.class);

    private PageControl pageControl;
    private Table usersTable;
    private Table tablesTable;
    private Table proceduresTable;
    private Table columnsTable;
    private Composite tablesGroup;
    private Composite proceduresGroup;
    private Composite columnsGroup;
    private PrivilegeTableControl tablePanel;
    private PrivilegeTableControl columnPanel;
    private PrivilegeTableControl procedurePanel;
    private PrivilegeTableControl otherPanel;
    private PrivilegeTableControl inheritedPanel;

    private boolean isLoaded;
    private MySQLCatalog catalog;
    private MySQLTableBase rootTable;
    private MySQLProcedure rootProcedure;

    private List<MySQLUser> users = new ArrayList<>();
    private final Map<MySQLUser, List<MySQLGrant>> userGrants = new HashMap<>();
    private List<MySQLTableBase> catalogTables = new ArrayList<>();
    private List<MySQLProcedure> catalogProcedures = new ArrayList<>();
    private List<MySQLPrivilege> tablePrivTypes = new ArrayList<>();
    private List<MySQLPrivilege> procedurePrivTypes = new ArrayList<>();
    private List<MySQLPrivilege> columnPrivTypes = new ArrayList<>();
    private final Map<MySQLTableBase, List<MySQLTableColumn>> tableColumnsCache = new HashMap<>();

    private MySQLUser selectedUser;
    private MySQLTableBase selectedTable;
    private MySQLTableColumn selectedColumn;
    private final List<MySQLTableColumn> selectedColumns = new ArrayList<>();
    private MySQLProcedure selectedProcedure;

    private Button usersRemoveButton;
    private Button tablesAddButton;
    private Button tablesRemoveButton;
    private Button proceduresAddButton;
    private Button proceduresRemoveButton;
    private Button columnsAddButton;
    private Button columnsRemoveButton;
    // Objects added via "+" that have no grants yet - kept in the lists until refresh
    private final Set<MySQLUser> extraUsers = new HashSet<>();
    private final Map<MySQLUser, Set<MySQLTableBase>> extraTables = new HashMap<>();
    private final Map<MySQLUser, Set<MySQLProcedure>> extraProcedures = new HashMap<>();
    private final Map<MySQLUser, Set<MySQLTableColumn>> extraColumns = new HashMap<>();
    private static final String ADD_TEXT = "+"; //$NON-NLS-1$
    private static final String REMOVE_TEXT = "-"; //$NON-NLS-1$

    private void resolveRoot() {
        DBSObject object = getDatabaseObject();
        rootTable = null;
        rootProcedure = null;
        if (object instanceof MySQLCatalog c) {
            catalog = c;
        } else if (object instanceof MySQLTableBase t) {
            catalog = t.getContainer();
            rootTable = t;
        } else if (object instanceof MySQLProcedure p) {
            catalog = p.getContainer();
            rootProcedure = p;
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        resolveRoot();
        pageControl = new PageControl(parent);

        CustomSashForm sash = new CustomSashForm(pageControl, SWT.HORIZONTAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Composite usersGroup = UIUtils.createTitledComposite(
                sash, MySQLUIMessages.dialog_object_privileges_group_users, 1, GridData.FILL_BOTH);
            usersTable = new Table(usersGroup, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
            usersTable.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            usersTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int index = usersTable.getSelectionIndex();
                    selectedUser = index < 0 ? null : (MySQLUser) usersTable.getItem(index).getData();
                    selectedTable = null;
                    selectedColumn = null;
                    selectedProcedure = null;
                    fillObjectLists();
                    updatePanels();
                    updateButtons();
                }
            });
            Composite userButtons = UIUtils.createComposite(usersGroup, 2);
            UIUtils.createPushButton(userButtons, ADD_TEXT, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    handleAddUser();
                }
            });
            usersRemoveButton = UIUtils.createPushButton(userButtons, REMOVE_TEXT, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    handleRemoveUser();
                }
            });
            usersRemoveButton.setEnabled(false);
        }

        if (rootProcedure == null) {
            Composite objectsPane = UIUtils.createPlaceholder(sash, rootTable == null ? 2 : 1);
            objectsPane.setLayoutData(new GridData(GridData.FILL_BOTH));
            Composite tablesPane = rootTable == null ? UIUtils.createPlaceholder(objectsPane, 1) : objectsPane;
            if (rootTable == null) {
                tablesPane.setLayoutData(new GridData(GridData.FILL_BOTH));
                tablesGroup = UIUtils.createTitledComposite(
                    tablesPane, MySQLUIMessages.editors_user_editor_privileges_group_tables, 1,
                    GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
                tablesTable = new Table(tablesGroup, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
                tablesTable.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
                tablesTable.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        int index = tablesTable.getSelectionIndex();
                        selectedTable = index < 0 ? null : (MySQLTableBase) tablesTable.getItem(index).getData();
                        selectedColumn = null;
                        selectedProcedure = null;
                        if (proceduresTable != null) {
                            proceduresTable.deselectAll();
                        }
                        fillColumnsList();
                        updatePanels();
                        updateButtons();
                    }
                });
                Composite tableButtons = UIUtils.createComposite(tablesGroup, 2);
                tablesAddButton = UIUtils.createPushButton(tableButtons, ADD_TEXT, null, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        handleAddTable();
                    }
                });
                tablesAddButton.setEnabled(false);
                tablesRemoveButton = UIUtils.createPushButton(tableButtons, REMOVE_TEXT, null, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        handleRemoveTable();
                    }
                });
                tablesRemoveButton.setEnabled(false);

                proceduresGroup = UIUtils.createTitledComposite(
                    tablesPane, MySQLUIMessages.editors_user_editor_privileges_group_procedures, 1,
                    GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
                proceduresTable = new Table(proceduresGroup, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
                proceduresTable.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
                proceduresTable.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        int index = proceduresTable.getSelectionIndex();
                        selectedProcedure = index < 0 ? null : (MySQLProcedure) proceduresTable.getItem(index).getData();
                        selectedTable = null;
                        selectedColumn = null;
                        tablesTable.deselectAll();
                        fillColumnsList();
                        updatePanels();
                        updateButtons();
                    }
                });
                Composite procButtons = UIUtils.createComposite(proceduresGroup, 2);
                proceduresAddButton = UIUtils.createPushButton(procButtons, ADD_TEXT, null, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        handleAddProcedure();
                    }
                });
                proceduresAddButton.setEnabled(false);
                proceduresRemoveButton = UIUtils.createPushButton(procButtons, REMOVE_TEXT, null, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        handleRemoveProcedure();
                    }
                });
                proceduresRemoveButton.setEnabled(false);
            }
            columnsGroup = UIUtils.createTitledComposite(
                objectsPane, MySQLUIMessages.editors_user_editor_privileges_group_columns, 1, GridData.FILL_BOTH);
            columnsTable = new Table(columnsGroup, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
            columnsTable.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            columnsTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    selectedColumns.clear();
                    for (TableItem item : columnsTable.getSelection()) {
                        if (item.getData() != null) {
                            selectedColumns.add((MySQLTableColumn) item.getData());
                        }
                    }
                    selectedColumn = selectedColumns.isEmpty() ? null : selectedColumns.get(0);
                    updatePanels();
                    updateButtons();
                }
            });
            Composite columnButtons = UIUtils.createComposite(columnsGroup, 2);
            columnsAddButton = UIUtils.createPushButton(columnButtons, ADD_TEXT, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    handleAddColumn();
                }
            });
            columnsAddButton.setEnabled(false);
            columnsRemoveButton = UIUtils.createPushButton(columnButtons, REMOVE_TEXT, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    handleRemoveColumn();
                }
            });
            columnsRemoveButton.setEnabled(false);
        }

        {
            Composite panelsPane = UIUtils.createPlaceholder(sash, 1);
            panelsPane.setLayoutData(new GridData(GridData.FILL_BOTH));
            tablePanel = new PrivilegeTableControl(panelsPane, MySQLUIMessages.editors_user_editor_privileges_control_table_privileges, false);
            tablePanel.setLayoutData(new GridData(GridData.FILL_BOTH));
            columnPanel = new PrivilegeTableControl(panelsPane, MySQLUIMessages.editors_user_editor_privileges_control_column_privileges, false);
            columnPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
            procedurePanel = new PrivilegeTableControl(panelsPane, MySQLUIMessages.editors_user_editor_privileges_control_procedure_privileges, false);
            procedurePanel.setLayoutData(new GridData(GridData.FILL_BOTH));
            otherPanel = new PrivilegeTableControl(panelsPane, MySQLUIMessages.editors_user_editor_privileges_control_other_privileges, false);
            otherPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
            // Editable panel for schema-wide (db.*) privileges that also apply to the selected object
            inheritedPanel = new PrivilegeTableControl(panelsPane, MySQLUIMessages.editors_object_privileges_control_schema_inherited, false);
            inheritedPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

            addGrantListener(tablePanel, PanelKind.TABLE);
            addGrantListener(columnPanel, PanelKind.COLUMN);
            addGrantListener(procedurePanel, PanelKind.PROCEDURE);
            addGrantListener(otherPanel, PanelKind.OTHER);
            addGrantListener(inheritedPanel, PanelKind.SCHEMA);
        }

        sash.setWeights(rootProcedure == null ? new int[]{25, 25, 50} : new int[]{35, 65});
        pageControl.createProgressPanel();
    }

    @Override
    public void setFocus() {
        if (pageControl != null) {
            pageControl.setFocus();
        }
    }

    @Override
    public synchronized void activatePart() {
        if (isLoaded) {
            return;
        }
        isLoaded = true;
        LoadingJob.createService(
            new DatabaseLoadService<PrivData>(MySQLUIMessages.dialog_object_privileges_title, getExecutionContext()) {
                @Override
                public PrivData evaluate(@NotNull DBRProgressMonitor monitor) throws InvocationTargetException {
                    try {
                        return loadData(monitor);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            },
            pageControl.createLoadVisualizer()
        ).schedule();
    }

    @Override
    public RefreshResult refreshPart(Object source, boolean force) {
        if (force
            || (source instanceof DBNEvent event && event.getSource() == DBNEvent.UPDATE_ON_SAVE)
            || !isLoaded
        ) {
            isLoaded = false;
            tableColumnsCache.clear();
            extraUsers.clear();
            extraTables.clear();
            extraProcedures.clear();
            extraColumns.clear();
            selectedTable = null;
            selectedColumn = null;
            selectedProcedure = null;
            activatePart();
            return RefreshResult.REFRESHED;
        }
        return RefreshResult.IGNORED;
    }

    private static final class PrivData {
        List<MySQLUser> users = new ArrayList<>();
        Map<MySQLUser, List<MySQLGrant>> userGrants = new HashMap<>();
        List<MySQLTableBase> catalogTables = new ArrayList<>();
        List<MySQLProcedure> catalogProcedures = new ArrayList<>();
        List<MySQLPrivilege> tablePrivs = new ArrayList<>();
        List<MySQLPrivilege> otherPrivs = new ArrayList<>();
        List<MySQLPrivilege> procedurePrivs = new ArrayList<>();
        List<MySQLPrivilege> columnPrivs = new ArrayList<>();
        List<MySQLTableColumn> rootTableColumns = new ArrayList<>();
    }

    private PrivData loadData(DBRProgressMonitor monitor) throws DBException {
        PrivData data = new PrivData();
        MySQLDataSource dataSource = catalog.getDataSource();
        for (MySQLPrivilege priv : dataSource.getPrivileges(monitor)) {
            if (priv.getKind() == MySQLPrivilege.Kind.ADMIN) {
                continue;
            }
            if (priv.getContext().contains("Procedure")) { //$NON-NLS-1$
                data.procedurePrivs.add(priv);
            }
            String name = priv.getName();
            if (name.equalsIgnoreCase("Select") || name.equalsIgnoreCase("Insert") //$NON-NLS-1$ //$NON-NLS-2$
                || name.equalsIgnoreCase("Update") || name.equalsIgnoreCase("References") //$NON-NLS-1$ //$NON-NLS-2$
            ) {
                data.columnPrivs.add(priv);
            }
            if (priv.getContext().contains("Table")) { //$NON-NLS-1$
                data.tablePrivs.add(priv);
            } else {
                data.otherPrivs.add(priv);
            }
        }
        Set<String> candidateKeys = findCandidateUsers(monitor, dataSource, catalog);
        for (MySQLUser user : dataSource.getUsers(monitor)) {
            if (!candidateKeys.contains(userKey(user.getUserName(), user.getHost()))) {
                continue;
            }
            List<MySQLGrant> catalogGrants = new ArrayList<>();
            boolean relevant = false;
            for (MySQLGrant grant : user.getGrants(monitor)) {
                if (grant.isEmpty() || !grant.matches(catalog)) {
                    continue;
                }
                catalogGrants.add(grant);
                if (rootTable != null) {
                    relevant |= grant.matches(rootTable) || isSchemaLevelGrant(grant);
                } else if (rootProcedure != null) {
                    relevant |= grant.matchesProcedure(rootProcedure)
                        || (isSchemaLevelGrant(grant) && hasAnyProcedurePrivilege(grant, data.procedurePrivs));
                } else {
                    relevant = true;
                }
            }
            if (relevant) {
                data.users.add(user);
                data.userGrants.put(user, catalogGrants);
            }
        }
        if (rootTable == null && rootProcedure == null) {
            Set<String> tableNames = new LinkedHashSet<>();
            Set<String> procedureNames = new LinkedHashSet<>();
            for (List<MySQLGrant> grantList : data.userGrants.values()) {
                for (MySQLGrant grant : grantList) {
                    String name = grant.getTable();
                    if (name == null || "*".equals(name)) { //$NON-NLS-1$
                        continue;
                    }
                    if (grant.getObjectType() == MySQLGrant.ObjectType.TABLE) {
                        tableNames.add(name);
                    } else {
                        procedureNames.add(name);
                    }
                }
            }
            for (String name : tableNames) {
                MySQLTableBase t = catalog.getTableCache().getObject(monitor, catalog, name);
                if (t != null) {
                    data.catalogTables.add(t);
                }
            }
            for (String name : procedureNames) {
                MySQLProcedure p = catalog.getProceduresCache().getObject(monitor, catalog, name);
                if (p != null) {
                    data.catalogProcedures.add(p);
                }
            }
        }
        if (rootTable != null) {
            List<MySQLTableColumn> attributes = rootTable.getAttributes(monitor);
            if (attributes != null) {
                data.rootTableColumns.addAll(attributes);
            }
        }
        return data;
    }

    private void applyData(PrivData data) {
        users = data.users;
        userGrants.clear();
        userGrants.putAll(data.userGrants);
        catalogTables = data.catalogTables;
        catalogProcedures = data.catalogProcedures;
        tablePrivTypes = data.tablePrivs;
        procedurePrivTypes = data.procedurePrivs;
        columnPrivTypes = data.columnPrivs;
        if (rootTable != null) {
            tableColumnsCache.put(rootTable, data.rootTableColumns);
        }

        tablePanel.fillPrivileges(tablePrivTypes);
        columnPanel.fillPrivileges(columnPrivTypes);
        procedurePanel.fillPrivileges(procedurePrivTypes);
        otherPanel.fillPrivileges(data.otherPrivs);

        usersTable.removeAll();
        for (MySQLUser user : users) {
            TableItem item = new TableItem(usersTable, SWT.NONE);
            item.setText(user.getName());
            item.setImage(DBeaverIcons.getImage(DBIcon.TREE_USER));
            item.setData(user);
        }
        UIUtils.packColumns(usersTable);
        adjustListHeight(usersTable);
        if (!users.isEmpty()) {
            usersTable.setSelection(0);
            selectedUser = users.get(0);
            fillObjectLists();
        } else {
            selectedUser = null;
        }
        updatePanels();
        updateButtons();
    }

    // ==== user/grant helpers ====

    private static String userKey(String user, String host) {
        return user + "@" + host; //$NON-NLS-1$
    }

    private static boolean isSchemaLevelGrant(MySQLGrant grant) {
        return grant.getObjectType() == MySQLGrant.ObjectType.TABLE && grant.isAllTables();
    }

    private static boolean hasAnyProcedurePrivilege(MySQLGrant grant, List<MySQLPrivilege> procedurePrivs) {
        if (grant.isAllPrivileges()) {
            return true;
        }
        for (MySQLPrivilege priv : grant.getPrivileges()) {
            if (procedurePrivs.contains(priv)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> findCandidateUsers(DBRProgressMonitor monitor, MySQLDataSource dataSource, MySQLCatalog catalog) throws DBException {
        Set<String> keys = new HashSet<>();
        String catalogName = catalog.getName();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Find users with grants on object")) { //$NON-NLS-1$
            collectGrantees(session, keys,
                "SELECT DISTINCT GRANTEE FROM information_schema.SCHEMA_PRIVILEGES WHERE TABLE_SCHEMA = ?", catalogName); //$NON-NLS-1$
            collectGrantees(session, keys,
                "SELECT DISTINCT GRANTEE FROM information_schema.TABLE_PRIVILEGES WHERE TABLE_SCHEMA = ?", catalogName); //$NON-NLS-1$
            collectGrantees(session, keys,
                "SELECT DISTINCT GRANTEE FROM information_schema.COLUMN_PRIVILEGES WHERE TABLE_SCHEMA = ?", catalogName); //$NON-NLS-1$
            collectGrantees(session, keys,
                "SELECT DISTINCT GRANTEE FROM information_schema.USER_PRIVILEGES WHERE PRIVILEGE_TYPE <> 'USAGE'", null); //$NON-NLS-1$
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT DISTINCT User, Host FROM mysql.procs_priv WHERE Db = ?")) { //$NON-NLS-1$
                dbStat.setString(1, catalogName);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        keys.add(userKey(
                            JDBCUtils.safeGetString(dbResult, "User"), //$NON-NLS-1$
                            JDBCUtils.safeGetString(dbResult, "Host"))); //$NON-NLS-1$
                    }
                }
            } catch (SQLException e) {
                log.debug("Can't query mysql.procs_priv: " + e.getMessage()); //$NON-NLS-1$
            }
        }
        return keys;
    }

    private static void collectGrantees(JDBCSession session, Set<String> keys, String sql, @Nullable String param) {
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            if (param != null) {
                dbStat.setString(1, param);
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next()) {
                    String key = granteeToKey(JDBCUtils.safeGetString(dbResult, "GRANTEE")); //$NON-NLS-1$
                    if (key != null) {
                        keys.add(key);
                    }
                }
            }
        } catch (SQLException e) {
            log.debug("Can't query privileges view: " + e.getMessage()); //$NON-NLS-1$
        }
    }

    @Nullable
    private static String granteeToKey(@Nullable String grantee) {
        if (grantee == null) {
            return null;
        }
        int at = grantee.lastIndexOf("@"); //$NON-NLS-1$
        if (at < 0) {
            return null;
        }
        return userKey(unquote(grantee.substring(0, at)), unquote(grantee.substring(at + 1)));
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2) {
            char q = trimmed.charAt(0);
            if ((q == '\'' || q == '`' || q == '"') && trimmed.charAt(trimmed.length() - 1) == q) {
                return trimmed.substring(1, trimmed.length() - 1).replace("''", "'"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        return trimmed;
    }

    private List<MySQLGrant> grantsOfSelectedUser() {
        List<MySQLGrant> grants = selectedUser == null ? null : userGrants.get(selectedUser);
        return grants == null ? Collections.emptyList() : grants;
    }

    // ==== list/panel filling (mirrors user editor semantics, keyed by selected user) ====

    private void fillObjectLists() {
        if (rootProcedure != null) {
            return;
        }
        if (rootTable == null) {
            tablesTable.removeAll();
            {
                TableItem item = new TableItem(tablesTable, SWT.NONE);
                item.setText("% (All)"); //$NON-NLS-1$
                item.setImage(DBeaverIcons.getImage(DBIcon.TREE_TABLE));
            }
            Set<MySQLTableBase> shownTables = new LinkedHashSet<>(catalogTables);
            shownTables.addAll(extraTables.getOrDefault(selectedUser, Collections.emptySet()));
            for (MySQLTableBase table : shownTables) {
                Set<MySQLTableBase> extra = extraTables.getOrDefault(selectedUser, Collections.emptySet());
                if (userHasTableGrant(table) || extra.contains(table)) {
                    TableItem item = new TableItem(tablesTable, SWT.NONE);
                    item.setText(table.getName());
                    item.setImage(DBeaverIcons.getImage(table.isView() ? DBIcon.TREE_VIEW : DBIcon.TREE_TABLE));
                    item.setData(table);
                }
            }
            tablesTable.setSelection(0);
            UIUtils.packColumns(tablesTable);
        adjustListHeight(tablesTable);

            proceduresTable.removeAll();
            Set<MySQLProcedure> shownProcedures = new LinkedHashSet<>(catalogProcedures);
            shownProcedures.addAll(extraProcedures.getOrDefault(selectedUser, Collections.emptySet()));
            for (MySQLProcedure procedure : shownProcedures) {
                boolean hasGrant = false;
                for (MySQLGrant grant : grantsOfSelectedUser()) {
                    if (grant.matchesProcedure(procedure)) {
                        hasGrant = true;
                        break;
                    }
                }
                Set<MySQLProcedure> extra = extraProcedures.getOrDefault(selectedUser, Collections.emptySet());
                if (hasGrant || extra.contains(procedure)) {
                    TableItem item = new TableItem(proceduresTable, SWT.NONE);
                    item.setText(procedure.getName());
                    item.setImage(DBeaverIcons.getImage(DBIcon.TREE_PROCEDURE));
                    item.setData(procedure);
                }
            }
            UIUtils.packColumns(proceduresTable);
        adjustListHeight(proceduresTable);
        }
        fillColumnsList();
    }

    private boolean userHasTableGrant(MySQLTableBase table) {
        for (MySQLGrant grant : grantsOfSelectedUser()) {
            if (grant.matches(table)) {
                return true;
            }
        }
        return false;
    }

    private void fillColumnsList() {
        if (columnsTable == null) {
            return;
        }
        columnsTable.removeAll();
        MySQLTableBase table = rootTable != null ? rootTable : selectedTable;
        if (table != null) {
            Set<MySQLTableColumn> extra = extraColumns.getOrDefault(selectedUser, Collections.emptySet());
            for (MySQLTableColumn column : loadColumns(table)) {
                boolean hasGrant = false;
                for (MySQLGrant grant : grantsOfSelectedUser()) {
                    if (grant.matches(table) && grant.hasColumnPrivileges(column.getName())) {
                        hasGrant = true;
                        break;
                    }
                }
                if (hasGrant || extra.contains(column)) {
                    TableItem item = new TableItem(columnsTable, SWT.NONE);
                    item.setText(column.getName());
                    item.setImage(DBeaverIcons.getImage(DBIcon.TREE_COLUMN));
                    item.setData(column);
                }
            }
        }
        selectedColumn = null;
        selectedColumns.clear();
        UIUtils.packColumns(columnsTable);
        adjustListHeight(columnsTable);
        updateObjectSections();
    }

    private List<MySQLTableColumn> loadColumns(MySQLTableBase table) {
        List<MySQLTableColumn> columns = tableColumnsCache.get(table);
        if (columns != null) {
            return columns;
        }
        final List<MySQLTableColumn> loaded = new ArrayList<>();
        try {
            UIUtils.runInProgressService(monitor -> {
                try {
                    List<MySQLTableColumn> attributes = table.getAttributes(monitor);
                    if (attributes != null) {
                        loaded.addAll(attributes);
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            log.error("Error loading table columns", e.getTargetException()); //$NON-NLS-1$
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        tableColumnsCache.put(table, loaded);
        return loaded;
    }

    private void updatePanels() {
        boolean editable = selectedUser != null;
        List<MySQLGrant> grants = grantsOfSelectedUser();
        MySQLProcedure procedure = rootProcedure != null ? rootProcedure : selectedProcedure;
        MySQLTableBase table = rootTable != null ? rootTable : selectedTable;
        if (procedure != null) {
            // Only routine-specific grants are editable here. Schema-wide (db.*) privileges are
            // shown in a separate editable panel below.
            List<MySQLGrant> matched = new ArrayList<>();
            for (MySQLGrant grant : grants) {
                if (grant.matchesProcedure(procedure)) {
                    matched.add(grant);
                }
            }
            procedurePanel.fillGrants(matched, editable);
            boolean hasInherited = fillInherited(grants, procedurePrivTypes, editable);
            showPanels(false, false, true, false, hasInherited);
            return;
        }
        if (selectedColumn != null && table != null) {
            List<MySQLTableColumn> cols = selectedColumns.isEmpty() ? List.of(selectedColumn) : selectedColumns;
            Set<MySQLPrivilege> enabledAll = new HashSet<>();
            List<MySQLPrivilege> partial = new ArrayList<>();
            boolean grantOption = false;
            for (MySQLGrant grant : grants) {
                if (grant.matches(table) && grant.isGrantOption()) {
                    grantOption = true;
                }
            }
            for (MySQLPrivilege priv : columnPrivTypes) {
                int count = 0;
                for (MySQLTableColumn col : cols) {
                    for (MySQLGrant grant : grants) {
                        if (grant.matches(table) && grant.hasColumnPrivilege(priv, col.getName())) {
                            count++;
                            break;
                        }
                    }
                }
                if (count == cols.size()) {
                    enabledAll.add(priv);
                } else if (count > 0) {
                    partial.add(priv);
                }
            }
            columnPanel.fillCheckedPrivileges(enabledAll, partial, grantOption, false, editable);
            showPanels(false, true, false, false, false);
            return;
        }
        List<MySQLGrant> matched = new ArrayList<>();
        for (MySQLGrant grant : grants) {
            if (grant.matches(table)) {
                matched.add(grant);
            }
        }
        if (table == null) {
            // Schema (or % All) level: these grants ARE the schema-wide ones (db.*)
            tablePanel.fillGrants(matched, editable);
            otherPanel.fillGrants(matched, editable);
            showPanels(true, false, false, true, false);
        } else {
            // Only table-specific grants are editable here; schema-wide (db.*) privileges that also
            // apply to this table are shown in the separate editable panel below.
            tablePanel.fillGrants(matched, editable);
            boolean hasInherited = fillInherited(grants, tablePrivTypes, editable);
            showPanels(true, false, false, false, hasInherited);
        }
    }

    /**
     * Fills the editable "schema privileges" panel with the user's schema-wide (db.*) grants.
     * Toggling there grants/revokes on db.* (affecting the whole schema).
     * @return true if there is at least one such grant (panel should be shown)
     */
    private boolean fillInherited(List<MySQLGrant> grants, List<MySQLPrivilege> privTypes, boolean editable) {
        List<MySQLGrant> schemaGrants = new ArrayList<>();
        for (MySQLGrant grant : grants) {
            if (isSchemaLevelGrant(grant)) {
                schemaGrants.add(grant);
            }
        }
        if (schemaGrants.isEmpty()) {
            return false;
        }
        inheritedPanel.fillPrivileges(privTypes);
        inheritedPanel.fillGrants(schemaGrants, editable);
        return true;
    }

    // SCHEMA targets db.* (schema-wide) - used by the editable "schema inherited" panel
    private enum PanelKind { TABLE, COLUMN, PROCEDURE, OTHER, SCHEMA }

    private void addGrantListener(PrivilegeTableControl panel, PanelKind kind) {
        panel.addListener(SWT.Modify, event -> {
            if (selectedUser == null) {
                return;
            }
            final MySQLUser user = selectedUser;
            final MySQLPrivilege privilege = (MySQLPrivilege) event.data;
            final boolean isGrant = event.detail >= 1;
            final boolean withGrantOption = event.detail == 2;
            final MySQLTableBase table = (kind == PanelKind.TABLE || kind == PanelKind.COLUMN)
                ? (rootTable != null ? rootTable : selectedTable) : null;
            final MySQLProcedure procedure = kind == PanelKind.PROCEDURE
                ? (rootProcedure != null ? rootProcedure : selectedProcedure) : null;
            if (kind == PanelKind.PROCEDURE && procedure == null) {
                return;
            }
            List<MySQLTableColumn> columns = null;
            if (kind == PanelKind.COLUMN) {
                if (table == null || selectedColumns.isEmpty()) {
                    return;
                }
                columns = new ArrayList<>(selectedColumns);
            }
            if (columns != null) {
                for (MySQLTableColumn c : columns) {
                    updateLocalGrant(user, privilege, isGrant, withGrantOption, table, procedure, c);
                }
            } else {
                updateLocalGrant(user, privilege, isGrant, withGrantOption, table, procedure, null);
            }
            applyUserPrivilegeChange(user, privilege, isGrant, withGrantOption, table, procedure, columns);
        });
    }

    /**
     * Folds one privilege toggle into a single pending GRANT/REVOKE command per (user, object,
     * direction), so several privileges on the same object collapse into one statement, and a
     * check followed by an uncheck cancels out to no SQL at all.
     */
    private void applyUserPrivilegeChange(
        MySQLUser user, MySQLPrivilege privilege, boolean isGrant, boolean withGrantOption,
        @Nullable MySQLTableBase table, @Nullable MySQLProcedure procedure, @Nullable List<MySQLTableColumn> columns
    ) {
        DBECommandContext ctx = getEditorInput().getCommandContext();
        MySQLCommandGrantPrivilege opposite = findPendingUserCommand(user, !isGrant, table, procedure, columns);
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
                    ctx.removeCommand(opposite);
                } else {
                    ctx.updateCommand(opposite, null);
                }
                return;
            }
        }
        MySQLCommandGrantPrivilege pending = findPendingUserCommand(user, isGrant, table, procedure, columns);
        if (pending != null) {
            if (privilege.isGrantOption() && isGrant) {
                pending.setWithGrantOption(true);
            } else {
                pending.addPrivilege(privilege);
                if (withGrantOption) {
                    pending.setWithGrantOption(true);
                }
            }
            ctx.updateCommand(pending, null);
            return;
        }
        addUserCommand(createCommand(user, isGrant, withGrantOption, table, procedure, columns, privilege));
    }

    @Nullable
    private MySQLCommandGrantPrivilege findPendingUserCommand(
        MySQLUser user, boolean grantDirection,
        @Nullable MySQLTableBase table, @Nullable MySQLProcedure procedure, @Nullable List<MySQLTableColumn> columns
    ) {
        DBECommandContext ctx = getEditorInput().getCommandContext();
        if (ctx == null) {
            return null;
        }
        for (DBECommand<?> command : ctx.getFinalCommands()) {
            if (command instanceof MySQLCommandGrantPrivilege g
                && g.getObject() == user
                && g.isGrant() == grantDirection
                && g.hasSameTarget(catalog, table, procedure, columns)
            ) {
                return g;
            }
        }
        return null;
    }

    // The commands operate on MySQLUser while this editor's object is the schema/table/procedure;
    // the command context handles them by their own object manager, so a raw cast is safe here.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addUserCommand(MySQLCommandGrantPrivilege command) {
        addChangeCommand((DBECommand) command, null);
    }

    private MySQLCommandGrantPrivilege createCommand(
        MySQLUser user, boolean isGrant, boolean withGrantOption,
        @Nullable MySQLTableBase table, @Nullable MySQLProcedure procedure,
        @Nullable List<MySQLTableColumn> columns, MySQLPrivilege privilege
    ) {
        if (procedure != null) {
            return new MySQLCommandGrantPrivilege(user, isGrant, withGrantOption, catalog, procedure, privilege);
        }
        if (columns != null && !columns.isEmpty()) {
            return new MySQLCommandGrantPrivilege(user, isGrant, withGrantOption, catalog, table, columns, privilege);
        }
        return new MySQLCommandGrantPrivilege(user, isGrant, withGrantOption, catalog, table, privilege);
    }

    /**
     * Reflects a pending change in the in-memory grant list of the user so the UI stays consistent
     * before the change is saved.
     */
    private void updateLocalGrant(
        MySQLUser user, MySQLPrivilege privilege, boolean isGrant, boolean withGrantOption,
        @Nullable MySQLTableBase table, @Nullable MySQLProcedure procedure, @Nullable MySQLTableColumn column
    ) {
        List<MySQLGrant> grants = userGrants.computeIfAbsent(user, u -> new ArrayList<>());
        MySQLGrant target = null;
        for (MySQLGrant grant : grants) {
            boolean sameObject = procedure != null
                ? grant.matchesProcedure(procedure)
                : (table == null ? grant.isAllTables() && grant.getObjectType() == MySQLGrant.ObjectType.TABLE : grant.matches(table));
            if (grant.matches(catalog) && sameObject) {
                target = grant;
                break;
            }
        }
        if (target == null) {
            MySQLGrant.ObjectType type = procedure != null
                ? (procedure.getProcedureType() == org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType.FUNCTION
                    ? MySQLGrant.ObjectType.FUNCTION : MySQLGrant.ObjectType.PROCEDURE)
                : MySQLGrant.ObjectType.TABLE;
            String objName = procedure != null ? procedure.getName() : (table == null ? "*" : table.getName()); //$NON-NLS-1$
            target = new MySQLGrant(user, new ArrayList<>(), catalog.getName(), objName, false, false, type);
            grants.add(target);
        }
        if (privilege.isGrantOption()) {
            // Only the grant-option pseudo-privilege changes the WITH GRANT OPTION flag
            target.setGrantOption(withGrantOption);
        } else if (column != null) {
            if (isGrant) {
                target.addColumnPrivilege(privilege, column.getName());
            } else {
                target.removeColumnPrivilege(privilege, column.getName());
            }
        } else if (isGrant) {
            target.addPrivilege(privilege);
        } else {
            target.removePrivilege(privilege);
        }
        user.clearGrantsCache();
    }

    /**
     * Shows Tables/Procedures/Columns sections only when there is something to show
     * (no procedures in the schema -> no Procedures section; no table selected -> no Columns).
     */
    private void updateObjectSections() {
        if (columnsGroup == null || columnsGroup.isDisposed()) {
            return;
        }
        boolean changed = false;
        if (proceduresGroup != null) {
            boolean hasProcedures = !catalogProcedures.isEmpty()
                || !extraProcedures.getOrDefault(selectedUser, Collections.emptySet()).isEmpty();
            changed |= setSectionVisible(proceduresGroup, hasProcedures);
        }
        if (tablesGroup != null) {
            boolean hasTables = !catalogTables.isEmpty()
                || !extraTables.getOrDefault(selectedUser, Collections.emptySet()).isEmpty();
            changed |= setSectionVisible(tablesGroup, hasTables);
        }
        MySQLTableBase table = rootTable != null ? rootTable : selectedTable;
        changed |= setSectionVisible(columnsGroup, table != null);
        if (changed) {
            columnsGroup.getParent().getParent().layout(true, true);
        }
    }

    private static boolean setSectionVisible(Composite group, boolean visible) {
        Composite host = group.getParent();
        GridData gd = (GridData) host.getLayoutData();
        if (gd.exclude == visible) {
            gd.exclude = !visible;
            host.setVisible(visible);
            return true;
        }
        return false;
    }

    /** Sizes the list to its content (min 2, max 15 rows) so the +/- buttons stay under the list. */
    private static void adjustListHeight(Table table) {
        if (table == null || table.isDisposed()) {
            return;
        }
        int rows = Math.max(2, Math.min(table.getItemCount(), 15));
        GridData gd = (GridData) table.getLayoutData();
        int heightHint = table.getHeaderHeight() + table.getItemHeight() * rows + 8;
        if (gd.heightHint != heightHint) {
            gd.heightHint = heightHint;
            table.requestLayout();
        }
    }

    private void updateButtons() {
        if (usersRemoveButton == null || usersRemoveButton.isDisposed()) {
            return;
        }
        usersRemoveButton.setEnabled(selectedUser != null);
        if (tablesAddButton != null) {
            tablesAddButton.setEnabled(selectedUser != null);
            tablesRemoveButton.setEnabled(selectedUser != null && selectedTable != null);
        }
        if (proceduresAddButton != null) {
            proceduresAddButton.setEnabled(selectedUser != null);
            proceduresRemoveButton.setEnabled(selectedUser != null && selectedProcedure != null);
        }
        if (columnsAddButton != null) {
            MySQLTableBase table = rootTable != null ? rootTable : selectedTable;
            columnsAddButton.setEnabled(selectedUser != null && table != null);
            columnsRemoveButton.setEnabled(selectedUser != null && selectedColumn != null);
        }
    }

    // ==== add ("+") handlers ====

    private void handleAddUser() {
        List<MySQLUser> allUsers = loadAllUsers();
        if (allUsers == null) {
            return;
        }
        List<MySQLUser> candidates = new ArrayList<>();
        for (MySQLUser user : allUsers) {
            if (!users.contains(user)) {
                candidates.add(user);
            }
        }
        List<MySQLUser> chosen = selectObjects(candidates, MySQLUIMessages.dialog_object_privileges_add_user, DBIcon.TREE_USER);
        if (chosen == null) {
            return;
        }
        for (MySQLUser user : chosen) {
            if (!users.contains(user)) {
                users.add(user);
                extraUsers.add(user);
                userGrants.computeIfAbsent(user, u -> new ArrayList<>());
                TableItem item = new TableItem(usersTable, SWT.NONE);
                item.setText(user.getName());
                item.setImage(DBeaverIcons.getImage(DBIcon.TREE_USER));
                item.setData(user);
            }
        }
        UIUtils.packColumns(usersTable);
        adjustListHeight(usersTable);
        selectUserItem(chosen.get(0));
        selectedUser = chosen.get(0);
        fillObjectLists();
        updatePanels();
        updateButtons();
    }

    private void handleAddTable() {
        if (selectedUser == null) {
            return;
        }
        List<MySQLTableBase> allTables = loadAllTables();
        if (allTables == null) {
            return;
        }
        Set<Object> shown = shownData(tablesTable);
        List<MySQLTableBase> candidates = new ArrayList<>();
        for (MySQLTableBase t : allTables) {
            if (!shown.contains(t)) {
                candidates.add(t);
            }
        }
        List<MySQLTableBase> chosen = selectObjects(candidates, MySQLUIMessages.editors_user_editor_privileges_dialog_add_table_title, DBIcon.TREE_TABLE);
        if (chosen == null) {
            return;
        }
        extraTables.computeIfAbsent(selectedUser, u -> new HashSet<>()).addAll(chosen);
        selectedTable = chosen.get(0);
        selectedColumn = null;
        selectedProcedure = null;
        fillObjectLists();
        selectTableByData(tablesTable, selectedTable);
        fillColumnsList();
        updatePanels();
        updateButtons();
    }

    private void handleAddProcedure() {
        if (selectedUser == null) {
            return;
        }
        List<MySQLProcedure> allProcedures = loadAllProcedures();
        if (allProcedures == null) {
            return;
        }
        Set<Object> shown = shownData(proceduresTable);
        List<MySQLProcedure> candidates = new ArrayList<>();
        for (MySQLProcedure p : allProcedures) {
            if (!shown.contains(p)) {
                candidates.add(p);
            }
        }
        List<MySQLProcedure> chosen = selectObjects(candidates, MySQLUIMessages.editors_user_editor_privileges_dialog_add_procedure_title, DBIcon.TREE_PROCEDURE);
        if (chosen == null) {
            return;
        }
        extraProcedures.computeIfAbsent(selectedUser, u -> new HashSet<>()).addAll(chosen);
        selectedProcedure = chosen.get(0);
        selectedTable = null;
        selectedColumn = null;
        tablesTable.deselectAll();
        fillObjectLists();
        selectTableByData(proceduresTable, selectedProcedure);
        updatePanels();
        updateButtons();
    }

    private void handleAddColumn() {
        MySQLTableBase table = rootTable != null ? rootTable : selectedTable;
        if (selectedUser == null || table == null) {
            return;
        }
        Set<Object> shown = shownData(columnsTable);
        List<MySQLTableColumn> candidates = new ArrayList<>();
        for (MySQLTableColumn column : loadColumns(table)) {
            if (!shown.contains(column)) {
                candidates.add(column);
            }
        }
        List<MySQLTableColumn> chosen = selectObjects(candidates, MySQLUIMessages.editors_user_editor_privileges_dialog_add_column_title, DBIcon.TREE_COLUMN);
        if (chosen == null) {
            return;
        }
        extraColumns.computeIfAbsent(selectedUser, u -> new HashSet<>()).addAll(chosen);
        fillColumnsList();
        selectItemsByData(columnsTable, chosen);
        selectedColumns.clear();
        selectedColumns.addAll(chosen);
        selectedColumn = chosen.get(0);
        updatePanels();
        updateButtons();
    }

    // ==== remove ("-") handlers: revoke everything the user has on the object ====

    private void handleRemoveUser() {
        if (selectedUser == null) {
            return;
        }
        MySQLUser user = selectedUser;
        for (MySQLGrant grant : new ArrayList<>(grantsOfSelectedUser())) {
            if (rootTable != null && !(grant.matches(rootTable) || isSchemaLevelGrant(grant))) {
                continue;
            }
            if (rootProcedure != null && !grant.matchesProcedure(rootProcedure)) {
                continue;
            }
            revokeWholeGrant(user, grant);
        }
        extraUsers.remove(user);
        users.remove(user);
        userGrants.remove(user);
        selectUserItem(null);
        selectedUser = null;
        selectedTable = null;
        selectedColumn = null;
        selectedProcedure = null;
        rebuildUsersTable();
        fillObjectLists();
        updatePanels();
        updateButtons();
    }

    private void handleRemoveTable() {
        if (selectedUser == null || selectedTable == null) {
            return;
        }
        MySQLUser user = selectedUser;
        MySQLTableBase table = selectedTable;
        for (MySQLGrant grant : new ArrayList<>(grantsOfSelectedUser())) {
            if (grant.matches(table)) {
                revokeWholeGrant(user, grant);
            }
        }
        Set<MySQLTableBase> extra = extraTables.get(user);
        if (extra != null) {
            extra.remove(table);
        }
        selectedTable = null;
        selectedColumn = null;
        fillObjectLists();
        updatePanels();
        updateButtons();
    }

    private void handleRemoveProcedure() {
        if (selectedUser == null || selectedProcedure == null) {
            return;
        }
        MySQLUser user = selectedUser;
        MySQLProcedure procedure = selectedProcedure;
        for (MySQLGrant grant : new ArrayList<>(grantsOfSelectedUser())) {
            if (grant.matchesProcedure(procedure)) {
                revokeWholeGrant(user, grant);
            }
        }
        Set<MySQLProcedure> extra = extraProcedures.get(user);
        if (extra != null) {
            extra.remove(procedure);
        }
        selectedProcedure = null;
        fillObjectLists();
        updatePanels();
        updateButtons();
    }

    private void handleRemoveColumn() {
        MySQLTableBase table = rootTable != null ? rootTable : selectedTable;
        if (selectedUser == null || table == null || selectedColumns.isEmpty()) {
            return;
        }
        MySQLUser user = selectedUser;
        for (MySQLTableColumn column : new ArrayList<>(selectedColumns)) {
            for (MySQLGrant grant : new ArrayList<>(grantsOfSelectedUser())) {
                if (!grant.matches(table)) {
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
                user.clearGrantsCache();
                addUserRevokeCommand(new MySQLCommandRevokeObjectGrants(user, tableSpec(table), privNames, false));
            }
            Set<MySQLTableColumn> extra = extraColumns.get(user);
            if (extra != null) {
                extra.remove(column);
            }
        }
        selectedColumn = null;
        selectedColumns.clear();
        fillColumnsList();
        updatePanels();
        updateButtons();
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

    /** Creates a REVOKE for the whole grant and drops it from the user's local grant list. */
    private void revokeWholeGrant(MySQLUser user, MySQLGrant grant) {
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
            StringBuilder cols = new StringBuilder(" ("); //$NON-NLS-1$
            boolean first = true;
            for (String col : entry.getValue()) {
                if (!first) {
                    cols.append(", "); //$NON-NLS-1$
                }
                cols.append(DBUtils.getQuotedIdentifier(catalog.getDataSource(), col));
                first = false;
            }
            cols.append(")"); //$NON-NLS-1$
            columnPrivNames.add(entry.getKey().getFixedPrivilegeName().toUpperCase(Locale.ROOT) + cols);
        }
        String objectSpec = grantObjectSpec(grant);
        userGrants.getOrDefault(user, new ArrayList<>()).remove(grant);
        user.clearGrantsCache();
        // Grant option is folded into the table-level statement when there is one, otherwise into
        // the column-level statement, so it is never revoked on its own (which would error).
        boolean grantOptionEmitted = false;
        if (!tablePrivNames.isEmpty()) {
            addUserRevokeCommand(new MySQLCommandRevokeObjectGrants(user, objectSpec, tablePrivNames, grant.isGrantOption()));
            grantOptionEmitted = grant.isGrantOption();
        }
        if (!columnPrivNames.isEmpty()) {
            addUserRevokeCommand(new MySQLCommandRevokeObjectGrants(user, objectSpec, columnPrivNames, !grantOptionEmitted && grant.isGrantOption()));
            grantOptionEmitted |= grant.isGrantOption();
        }
        if (!grantOptionEmitted && grant.isGrantOption()) {
            addUserRevokeCommand(new MySQLCommandRevokeObjectGrants(user, objectSpec, List.of(), true));
        }
    }

    private String grantObjectSpec(MySQLGrant grant) {
        if (grant.getObjectType() != MySQLGrant.ObjectType.TABLE) {
            return (grant.getObjectType() == MySQLGrant.ObjectType.FUNCTION ? "FUNCTION " : "PROCEDURE ") //$NON-NLS-1$ //$NON-NLS-2$
                + DBUtils.getQuotedIdentifier(catalog) + "." //$NON-NLS-1$
                + DBUtils.getQuotedIdentifier(catalog.getDataSource(), grant.getTable());
        }
        if (grant.isAllTables()) {
            return DBUtils.getQuotedIdentifier(catalog) + ".*"; //$NON-NLS-1$
        }
        return DBUtils.getQuotedIdentifier(catalog) + "." //$NON-NLS-1$
            + DBUtils.getQuotedIdentifier(catalog.getDataSource(), grant.getTable());
    }

    private String tableSpec(MySQLTableBase table) {
        return DBUtils.getQuotedIdentifier(catalog) + "." + DBUtils.getQuotedIdentifier(table); //$NON-NLS-1$
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addUserRevokeCommand(MySQLCommandRevokeObjectGrants command) {
        addChangeCommand((DBECommand) command, null);
    }

    // ==== object loading for "+" dialogs (lazy) ====

    @Nullable
    private List<MySQLUser> loadAllUsers() {
        final List<MySQLUser> result = new ArrayList<>();
        if (runLoad(monitor -> result.addAll(catalog.getDataSource().getUsers(monitor)))) {
            return result;
        }
        return null;
    }

    @Nullable
    private List<MySQLTableBase> loadAllTables() {
        final List<MySQLTableBase> result = new ArrayList<>();
        if (runLoad(monitor -> result.addAll(catalog.getTableCache().getAllObjects(monitor, catalog)))) {
            return result;
        }
        return null;
    }

    @Nullable
    private List<MySQLProcedure> loadAllProcedures() {
        final List<MySQLProcedure> result = new ArrayList<>();
        if (runLoad(monitor -> result.addAll(catalog.getProceduresCache().getAllObjects(monitor, catalog)))) {
            return result;
        }
        return null;
    }

    private interface LoadTask {
        void run(DBRProgressMonitor monitor) throws DBException;
    }

    private boolean runLoad(LoadTask task) {
        try {
            UIUtils.runInProgressService(monitor -> {
                try {
                    task.run(monitor);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
            return true;
        } catch (InvocationTargetException e) {
            log.error("Error loading objects", e.getTargetException()); //$NON-NLS-1$
            return false;
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <T extends DBSObject> List<T> selectObjects(List<T> candidates, String title, DBIcon icon) {
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
        dialog.setMessage(title);
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
        for (Object o : result) {
            selected.add((T) o);
        }
        return selected;
    }

    private static Set<Object> shownData(Table table) {
        Set<Object> data = new HashSet<>();
        for (TableItem item : table.getItems()) {
            if (item.getData() != null) {
                data.add(item.getData());
            }
        }
        return data;
    }

    private void selectUserItem(@Nullable MySQLUser user) {
        if (user == null) {
            usersTable.deselectAll();
            return;
        }
        selectTableByData(usersTable, user);
    }

    private void rebuildUsersTable() {
        usersTable.removeAll();
        for (MySQLUser user : users) {
            TableItem item = new TableItem(usersTable, SWT.NONE);
            item.setText(user.getName());
            item.setImage(DBeaverIcons.getImage(DBIcon.TREE_USER));
            item.setData(user);
        }
        UIUtils.packColumns(usersTable);
        adjustListHeight(usersTable);
    }

    private static void selectTableByData(Table table, Object data) {
        for (int i = 0; i < table.getItemCount(); i++) {
            if (table.getItem(i).getData() == data) {
                table.setSelection(i);
                return;
            }
        }
    }

    private void showPanels(boolean tableVisible, boolean columnVisible, boolean procedureVisible, boolean otherVisible, boolean inheritedVisible) {
        boolean changed = setVisible(tablePanel, tableVisible);
        changed |= setVisible(columnPanel, columnVisible);
        changed |= setVisible(procedurePanel, procedureVisible);
        changed |= setVisible(otherPanel, otherVisible);
        changed |= setVisible(inheritedPanel, inheritedVisible);
        if (changed) {
            tablePanel.getParent().layout(true, true);
        }
    }

    private static boolean setVisible(Composite control, boolean visible) {
        GridData gd = (GridData) control.getLayoutData();
        if (gd.exclude == visible) {
            gd.exclude = !visible;
            control.setVisible(visible);
            return true;
        }
        return false;
    }

    private class PageControl extends ObjectEditorPageControl {
        PageControl(Composite parent) {
            super(parent, SWT.NONE, MySQLObjectPrivilegesEditor.this);
        }

        ProgressVisualizer<PrivData> createLoadVisualizer() {
            return new ProgressVisualizer<>() {
                @Override
                public void completeLoading(PrivData data) {
                    super.completeLoading(data);
                    if (data != null && !pageControl.isDisposed()) {
                        applyData(data);
                    }
                }
            };
        }

        @Override
        public void fillCustomActions(@NotNull IContributionManager contributionManager) {
            super.fillCustomActions(contributionManager);
            DatabaseEditorUtils.contributeStandardEditorActions(getSite(), contributionManager);
        }
    }
}
