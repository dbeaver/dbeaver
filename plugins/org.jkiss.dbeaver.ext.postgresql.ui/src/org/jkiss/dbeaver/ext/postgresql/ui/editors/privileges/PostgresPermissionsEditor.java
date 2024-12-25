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
package org.jkiss.dbeaver.ext.postgresql.ui.editors.privileges;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.edit.PostgreCommandGrantPrivilege;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorLabelProvider;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilter;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

abstract class PostgresPermissionsEditor<T extends DBSObject>
    extends AbstractDatabaseObjectEditor<PostgrePrivilegeOwner> {
    protected final Map<String, PostgrePrivilege> objectToPrivileges = new HashMap<>();

    @NotNull
    protected DBSObject[] selectedObjects = new DBSObject[0];
    protected Table permissionTable;
    private PageControl pageControl;
    private Composite permEditPanel;
    private DatabaseNavigatorTree roleOrObjectTable;
    private Text selectedObjectNames;
    private boolean isLoaded;

    public void createPartControl(Composite parent) {
        pageControl = new PageControl(parent);

        SashForm composite = UIUtils.createPartDivider(getSite().getPart(), pageControl, SWT.HORIZONTAL);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        addSelectableObjectsTree(composite);
        addPermissionsPanel(composite);
        addButtons(permEditPanel);
        addText(permEditPanel);

        pageControl.createOrSubstituteProgressPanel(getSite());
    }

    private void handleSelectionChange() {
        refreshPermissionsPanel(
            NavigatorUtils.getSelectedObjects(roleOrObjectTable.getViewer().getSelection())
        );
    }

    private PostgrePrivilege getObjectPermissions(DBSObject object) {
        return objectToPrivileges.get(getObjectName(object));
    }

    protected String getObjectName(DBSObject object) {
        if (object instanceof PostgreProcedure procedure) {
            return DBUtils.getQuotedIdentifier(procedure.getSchema()) + "." + procedure.getOverloadedName();
        } else if (object instanceof DBNDatabaseFolder folder) {
            String parentNodeName = folder.getParentNode().getNodeDisplayName();
            Class<? extends DBSObject> childrenClass = folder.getChildrenClass();
            if (DBSSequence.class.isAssignableFrom(childrenClass)) {
                return parentNodeName + "." + PostgrePrivilegeGrant.Kind.SEQUENCE;
            } else if (DBSProcedure.class.isAssignableFrom(childrenClass)) {
                return parentNodeName + "." + PostgrePrivilegeGrant.Kind.FUNCTION;
            } else {
                return parentNodeName + "." + PostgrePrivilegeGrant.Kind.TABLE;
            }
        } else {
            return DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL);
        }
    }

    private void grantAllCurrentPrivileges() {
        PostgrePrivilegeType[] privilegesToGrant = Arrays.stream(permissionTable.getItems())
            .filter(Predicate.not(TableItem::getChecked))
            .peek(x -> x.setChecked(true))
            .map(x -> (PostgrePrivilegeType) x.getData())
            .toArray(PostgrePrivilegeType[]::new);

        grantPrivilegeToSelectedObjects(privilegesToGrant);
    }

    private void revokeAllCurrentPrivileges() {
        PostgrePrivilegeType[] privilegesToRevoke = Arrays.stream(permissionTable.getItems())
            .filter(TableItem::getChecked)
            .peek(x -> x.setChecked(false))
            .map(x -> (PostgrePrivilegeType) x.getData())
            .toArray(PostgrePrivilegeType[]::new);

        revokeFromSelectedObjects(privilegesToRevoke);
    }

    private void grantPrivilegeToSelectedObjects(
        PostgrePrivilegeType[] privilegeTypes
    ) {
        applyToSelectedObjects(
            privilegeTypes,
            this::grantPrivilege,
            Action.GRANT
        );
    }

    private void revokeFromSelectedObjects(
        PostgrePrivilegeType[] privilegeTypes
    ) {
        applyToSelectedObjects(
            privilegeTypes,
            this::revokePrivilege,
            Action.REVOKE
        );
    }

    private void applyToSelectedObjects(
        PostgrePrivilegeType[] privilegeTypes,
        BiFunction<PostgrePrivilegeType, T, PostgrePrivilege> privilegeProvider,
        Action action
    ) {
        for (DBSObject selectedObject : selectedObjects) {
            PostgrePrivilege privilege = null;
            for (PostgrePrivilegeType privilegeType : privilegeTypes) {
                privilege = privilegeProvider.apply(privilegeType, (T) selectedObject);
            }

            if (privilege != null) {
                addCommand(
                    action,
                    privilege,
                    selectedObject,
                    privilegeTypes
                );
            }
        }
    }

    @Nullable
    protected abstract PostgrePrivilege grantPrivilege(
        PostgrePrivilegeType privilegeType,
        T object
    );

    @Nullable
    protected abstract PostgrePrivilege revokePrivilege(
        PostgrePrivilegeType privilegeType,
        T selectedObject
    );

    private void addCommand(
        Action action,
        PostgrePrivilege privilege,
        @NotNull DBSObject object,
        PostgrePrivilegeType[] privilegeTypes
    ) {
        addChangeCommand(
            new PostgreCommandGrantPrivilege(
                privilege.getOwner(),
                action == Action.GRANT,
                object,
                privilege,
                privilegeTypes
            ),
            new DBECommandReflector<PostgrePrivilegeOwner, PostgreCommandGrantPrivilege>() {
                @Override
                public void redoCommand(PostgreCommandGrantPrivilege cmd) {
                }

                @Override
                public void undoCommand(PostgreCommandGrantPrivilege cmd) {
                }
            });
    }

    @NotNull
    protected PostgrePrivilegeGrant createGrant(
        @NotNull PostgrePrivilegeOwner databaseObject,
        @NotNull PostgreRole role,
        @NotNull PostgrePrivilegeType type
    ) {
        String currentUserName = databaseObject.getDataSource()
            .getContainer()
            .getActualConnectionConfiguration()
            .getUserName();
        PostgreRoleReference currentUserReference = new PostgreRoleReference(
            databaseObject.getDatabase(),
            currentUserName,
            null
        );

        return new PostgrePrivilegeGrant(
            currentUserReference,
            role.getRoleReference(),
            databaseObject.getDatabase().getName(),
            databaseObject.getSchema().getName(),
            databaseObject.getName(),
            type,
            false,
            false
        );
    }

    private void refreshPermissionsPanel(@NotNull List<DBSObject> objects) {
        Optional<DBSObject> illegalObject = objects.stream()
            .filter(Predicate.not(this::doesSupportObject))
            .findAny();

        if (objects.isEmpty() || illegalObject.isPresent()) {
            UIUtils.enableWithChildren(permEditPanel, false);
            selectedObjectNames.setText(PostgreMessages.dialog_object_description_text_no_objects);
            return;
        }

        selectedObjects = objects.toArray(new DBSObject[0]);

        permissionTable.removeAll();

        PostgrePrivilegeType[] supportedPrivilegeTypes = getSupportedPrivilegeTypes(objects.get(0));
        PostgrePrivilege objectPermissions = getObjectPermissions(selectedObjects[0]);
        for (PostgrePrivilegeType privilegeType : supportedPrivilegeTypes) {
            TableItem tableItem = new TableItem(permissionTable, SWT.LEFT);
            tableItem.setText(0, privilegeType.name());
            tableItem.setData(privilegeType);
            if (objectPermissions != null) {
                short permission = objectPermissions.getPermission(privilegeType);
                tableItem.setChecked((permission & PostgrePrivilege.GRANTED) != 0);
                tableItem.setText(1, (permission & PostgrePrivilege.WITH_GRANT_OPTION) != 0 ? "X" : "");
                tableItem.setText(2, (permission & PostgrePrivilege.WITH_HIERARCHY) != 0 ? "X" : "");
            }
        }
        permissionTable.getParent().layout(true);
        UIUtils.packColumns(permissionTable, false);

        String objectNames = objects.stream()
            .map(it -> DBUtils.getObjectFullName(it.getDataSource(), it, DBPEvaluationContext.DML))
            .collect(Collectors.joining(", "));
        selectedObjectNames.setText(objectNames);

        UIUtils.enableWithChildren(permEditPanel, true);
    }

    @Override
    public void setFocus() {
        if (this.pageControl != null) {
            // Important! activation of page control fills action toolbar
            this.pageControl.activate(true);
        }
        if (roleOrObjectTable != null) {
            roleOrObjectTable.getViewer().getControl().setFocus();
        }
    }

    @Override
    public synchronized void activatePart() {
        if (isLoaded) {
            return;
        }
        isLoaded = true;

        UIUtils.asyncExec(() -> UIUtils.packColumns(permissionTable, false));

        LoadingJob.createService(
            new DatabaseLoadService<>("Load permissions", getExecutionContext()) {
                @Override
                public PermissionInfo evaluate(DBRProgressMonitor monitor) throws InvocationTargetException {
                    monitor.beginTask("Load privileges from database..", 1);
                    try {
                        monitor.subTask("Load " + getDatabaseObject().getName() + " privileges");
                        return laodPermissionInfo(monitor);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        monitor.done();
                    }
                }
            },
            pageControl.createLoadVisualizer()).schedule();
    }

    @Override
    public RefreshResult refreshPart(Object source, boolean force) {
        boolean onSave = source instanceof DBNEvent &&
            ((DBNEvent) source).getSource() == DBNEvent.UPDATE_ON_SAVE;

        if (force || onSave || !isLoaded) {
            isLoaded = false;
            UIUtils.syncExec(() -> refreshPermissionsPanel(List.of()));
            activatePart();
            return RefreshResult.REFRESHED;
        }

        return RefreshResult.IGNORED;
    }

    private class PageControl extends ProgressPageControl {
        PageControl(Composite parent) {
            super(parent, SWT.SHEET);
        }

        ProgressVisualizer<PermissionInfo> createLoadVisualizer() {
            return new ProgressVisualizer<>() {
                @Override
                public void completeLoading(PermissionInfo privs) {
                    super.completeLoading(privs);
                    if (privs == null) {
                        return;
                    }
                    objectToPrivileges.clear();
                    for (PostgrePrivilege perm : privs.privileges()) {
                        objectToPrivileges.put(perm.getName(), perm);
                    }
                    // Load navigator tree
                    roleOrObjectTable.reloadTree(privs.objectRootNode());
                    roleOrObjectTable.getViewer().expandToLevel(2);
                    handleSelectionChange();
                }
            };
        }

        @Override
        public void fillCustomActions(IContributionManager contributionManager) {
            super.fillCustomActions(contributionManager);

            contributionManager.add(new Separator());

            IWorkbenchSite workbenchSite = getSite();
            if (workbenchSite != null) {
                DatabaseEditorUtils.contributeStandardEditorActions(workbenchSite, contributionManager);
            }
        }
    }

    private Composite addPermissionsPanel(Composite parent) {
        permEditPanel = new Composite(parent, SWT.NONE);
        permEditPanel.setLayout(new GridLayout(1, true));
        permissionTable = addPermissionTable(permEditPanel);

        return permEditPanel;
    }

    private Table addPermissionTable(Composite composite) {
        Table table = new Table(composite, SWT.FULL_SELECTION | SWT.CHECK);
        table.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        UIUtils.createTableColumn(table, SWT.LEFT, PostgreMessages.dialog_create_table_column_name_permission);
        UIUtils.createTableColumn(table, SWT.CENTER, PostgreMessages.dialog_create_table_column_name_with_garant);
        UIUtils.createTableColumn(table, SWT.CENTER, PostgreMessages.dialog_create_table_column_name_with_hierarchy);
        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.item instanceof TableItem tableItem && e.detail == SWT.CHECK) {
                    PostgrePrivilegeType[] privilegeTypes = {(PostgrePrivilegeType) tableItem.getData()};
                    if (tableItem.getChecked()) {
                        grantPrivilegeToSelectedObjects(privilegeTypes);
                    } else {
                        revokeFromSelectedObjects(privilegeTypes);
                    }
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                super.mouseDown(e);
            }
        });

        return table;
    }

    private DatabaseNavigatorTree addSelectableObjectsTree(Composite composite) {
        roleOrObjectTable = new DatabaseNavigatorTree(
            composite,
            NavigatorUtils.getSelectedProject().getNavigatorModel().getRoot(),
            SWT.MULTI | SWT.FULL_SELECTION,
            false,
            navigatorTreeFilter()
        );
        roleOrObjectTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        setupTreeViewer(roleOrObjectTable, objectToPrivileges.keySet());
        return roleOrObjectTable;
    }

    private void setupTreeViewer(
        DatabaseNavigatorTree objectTree,
        Set<String> objectNames
    ) {
        TreeViewer treeViewer = objectTree.getViewer();
        treeViewer.setLabelProvider(new DatabaseNavigatorLabelProvider(objectTree) {
            @Override
            public Font getFont(Object element) {
                if (element instanceof DBNDatabaseNode) {
                    DBSObject object = ((DBNDatabaseNode) element).getObject();
                    if (object instanceof PostgreSchema) {
                        String schemaPrefix = DBUtils.getQuotedIdentifier(object) + ".";
                        for (String tableName : objectNames) {
                            if (tableName.startsWith(schemaPrefix)) {
                                return BaseThemeSettings.instance.baseFontBold;
                            }
                        }
                    } else if (getObjectPermissions(object) != null) {
                        return BaseThemeSettings.instance.baseFontBold;
                    }
                }
                return null;
            }
        });
        treeViewer.addSelectionChangedListener(event -> handleSelectionChange());
        treeViewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof DBNNode && !(element instanceof DBNDatabaseNode)) {
                    return false;
                }
                if (element instanceof DBNDatabaseFolder) {
                    final DBXTreeFolder meta = ((DBNDatabaseFolder) element).getMeta();
                    final Class<?> childType = meta.getSource().getObjectClass(meta.getType());
                    if (childType == null) {
                        return false;
                    }
                    if (PostgreProcedure.class.isAssignableFrom(childType)) {
                        PostgrePrivilegeOwner owner = getDatabaseObject();
                        if (owner instanceof PostgreRole role) {
                            return role.supportsRoutinesPermissions();
                        }
                        return true;
                    }
                    return PostgreTableReal.class.isAssignableFrom(childType) ||
                        PostgreSequence.class.isAssignableFrom(childType) ||
                        PostgreProcedure.class.isAssignableFrom(childType) ||
                        PostgreRole.class.isAssignableFrom(childType);
                }
                return true;
            }
        });
    }

    private void addButtons(Composite parent) {
        Composite buttonPanel = new Composite(parent, SWT.NONE);
        buttonPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        buttonPanel.setLayout(new RowLayout());

        UIUtils.createPushButton(
            buttonPanel,
            PostgreMessages.dialog_create_push_button_grant_all,
            null,
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    grantAllCurrentPrivileges();
                }
            }
        );

        UIUtils.createPushButton(
            buttonPanel,
            PostgreMessages.dialog_create_push_button_revoke_all,
            null,
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    revokeAllCurrentPrivileges();
                }
            }
        );
    }

    private void addText(Composite parent) {
        selectedObjectNames = new Text(
            parent,
            SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL
        );
        selectedObjectNames.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    protected abstract PostgrePrivilegeType[] getSupportedPrivilegeTypes(DBSObject object);

    protected abstract boolean doesSupportObject(DBSObject object);

    protected abstract PermissionInfo laodPermissionInfo(DBRProgressMonitor monitor) throws DBException;

    protected abstract DatabaseNavigatorTreeFilter navigatorTreeFilter();

    private enum Action {
        GRANT,
        REVOKE
    }
}
