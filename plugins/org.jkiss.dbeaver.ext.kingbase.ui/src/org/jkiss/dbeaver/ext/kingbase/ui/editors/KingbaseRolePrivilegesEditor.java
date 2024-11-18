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
package org.jkiss.dbeaver.ext.kingbase.ui.editors;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ControlEnableState;
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.kingbase.KingbaseMessages;
import org.jkiss.dbeaver.ext.kingbase.edit.KingbaseCommandGrantPrivilege;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDefaultPrivilege;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseObjectPrivilege;
import org.jkiss.dbeaver.ext.kingbase.model.KingbasePrivilege;
import org.jkiss.dbeaver.ext.kingbase.model.KingbasePrivilegeGrant;
import org.jkiss.dbeaver.ext.kingbase.model.KingbasePrivilegeOwner;
import org.jkiss.dbeaver.ext.kingbase.model.KingbasePrivilegeType;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseProcedure;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseProcedureKind;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseRole;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseRolePrivilege;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseRoleReference;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseSchema;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseSequence;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableReal;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSPackage;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorLabelProvider;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilter;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

/**
 * KingbaseRolePrivilegesEditor
 */
public class KingbaseRolePrivilegesEditor extends AbstractDatabaseObjectEditor<KingbasePrivilegeOwner> {
    private static final Log log = Log.getLog(KingbaseRolePrivilegesEditor.class);
    private PageControl pageControl;

    private boolean isLoaded;
    private DatabaseNavigatorTree roleOrObjectTable;
    private Composite permEditPanel;
    private Table permissionTable;
    private ControlEnableState permissionsEnable;

    private DBSObject[] currentObjects;
    private KingbasePrivilege[] currentPermissions;
    private final Map<String, KingbasePrivilege> permissionMap = new HashMap<>();
    private Text objectDescriptionText;

    public void createPartControl(Composite parent) {
        this.pageControl = new PageControl(parent);

        SashForm composite = UIUtils.createPartDivider(getSite().getPart(), this.pageControl, SWT.HORIZONTAL);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        roleOrObjectTable = new DatabaseNavigatorTree(
            composite,
            DBWorkbench.getPlatform().getNavigatorModel().getRoot(),
            SWT.MULTI | SWT.FULL_SELECTION,
            false,
            isRoleEditor() ? new DatabaseObjectFilter() : new ObjectOwnerFiler());
        roleOrObjectTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        final TreeViewer treeViewer = roleOrObjectTable.getViewer();
        treeViewer.setLabelProvider(new DatabaseNavigatorLabelProvider(roleOrObjectTable) {
            @Override
            public Font getFont(Object element) {
                if (element instanceof DBNDatabaseNode) {
                    DBSObject object = ((DBNDatabaseNode) element).getObject();
                    if (object instanceof KingbaseSchema) {
                        String schemaPrefix = DBUtils.getQuotedIdentifier(object) + ".";
                        for (String tableName : permissionMap.keySet()) {
                            if (tableName.startsWith(schemaPrefix)) {
                                return boldFont;
                            }
                        }
                    } else if (getObjectPermissions(object) != null) {
                        return boldFont;
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
                    if (KingbaseProcedure.class.isAssignableFrom(childType)) {
                        KingbasePrivilegeOwner owner = getDatabaseObject();
                        if (owner instanceof KingbaseRole role) {
                            return role.supportsRoutinesPermissions();
                        }
                        return true;
                    }
                    return KingbaseTableReal.class.isAssignableFrom(childType) ||
                        KingbaseSequence.class.isAssignableFrom(childType) ||
                        KingbaseProcedure.class.isAssignableFrom(childType) ||
                        KingbaseRole.class.isAssignableFrom(childType);
                }
                return true;
            }
        });

        {
            permEditPanel = new Composite(composite, SWT.NONE);
            permEditPanel.setLayout(new GridLayout(1, true));

            permissionTable = new Table(permEditPanel, SWT.FULL_SELECTION | SWT.CHECK);
            permissionTable.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            permissionTable.setHeaderVisible(true);
            permissionTable.setLinesVisible(true);
            UIUtils.createTableColumn(permissionTable, SWT.LEFT, KingbaseMessages.dialog_create_table_column_name_permission);
            UIUtils.createTableColumn(permissionTable, SWT.CENTER, KingbaseMessages.dialog_create_table_column_name_with_garant);
            UIUtils.createTableColumn(permissionTable, SWT.CENTER, KingbaseMessages.dialog_create_table_column_name_with_hierarchy);
            permissionTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (e.detail == SWT.CHECK) {
                        updateCurrentPrivileges(((TableItem) e.item).getChecked(), (KingbasePrivilegeType) e.item.getData(), null);
                    }
                }
            });
            permissionTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDown(MouseEvent e) {
                    super.mouseDown(e);
                }
            });

            if (!isRoleEditor()) {
                for (KingbasePrivilegeType pt : getDatabaseObject().getDataSource().getSupportedPrivilegeTypes()) {
                    if (!pt.isValid() || !pt.supportsType(getDatabaseObject().getClass())) {
                        continue;
                    }
                    TableItem privItem = new TableItem(permissionTable, SWT.LEFT);
                    privItem.setText(0, pt.name());
                    privItem.setData(pt);
                }
            }

            Composite buttonPanel = new Composite(permEditPanel, SWT.NONE);
            buttonPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            buttonPanel.setLayout(new RowLayout());

            UIUtils.createPushButton(buttonPanel, KingbaseMessages.dialog_create_push_button_grant_all, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateAllCurrentPrivileges(true);
                }
            });
            UIUtils.createPushButton(buttonPanel, KingbaseMessages.dialog_create_push_button_revoke_all, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateAllCurrentPrivileges(false);
                }
            });

            objectDescriptionText = new Text(permEditPanel, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
            objectDescriptionText.setLayoutData(new GridData(GridData.FILL_BOTH));

        }

        pageControl.createOrSubstituteProgressPanel(getSite());
        updateObjectPermissions(null);
    }

    private void handleSelectionChange() {
        List<DBSObject> selectedObjects = NavigatorUtils.getSelectedObjects(roleOrObjectTable.getViewer().getSelection());
        if (CommonUtils.isEmpty(selectedObjects)) {
            updateObjectPermissions(null);
        } else {
            updateObjectPermissions(selectedObjects);
        }
    }

    private KingbasePrivilege getObjectPermissions(DBSObject object) {
        if (object instanceof KingbaseProcedure) {
            String fqProcName = DBUtils.getQuotedIdentifier(((KingbaseProcedure) object).getSchema()) + "." + ((KingbaseProcedure) object).getOverloadedName();
            return permissionMap.get(fqProcName);
        } else if (object instanceof DBNDatabaseFolder) {
            DBNDatabaseFolder folder = (DBNDatabaseFolder) object;
            String parentNodeName = folder.getParentNode().getNodeDisplayName();
            Class<? extends DBSObject> childrenClass = folder.getChildrenClass();
            String permissionKey = parentNodeName + ".";
            if (DBSSequence.class.isAssignableFrom(childrenClass)) {
                permissionKey += KingbasePrivilegeGrant.Kind.SEQUENCE;
            } else if (DBSProcedure.class.isAssignableFrom(childrenClass)) {
                permissionKey += KingbasePrivilegeGrant.Kind.FUNCTION;
            } else {
                permissionKey += KingbasePrivilegeGrant.Kind.TABLE;
            }
            return permissionMap.get(permissionKey);
        } else {
            return permissionMap.get(DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL));
        }
    }

    private void updateAllCurrentPrivileges(boolean grant) {
        final KingbasePrivilegeType[] previousPrivilegeTypes = Arrays.stream(permissionTable.getItems())
            .filter(x -> grant != x.getChecked())
            .peek(x -> x.setChecked(grant))
            .map(x -> (KingbasePrivilegeType) x.getData())
            .toArray(KingbasePrivilegeType[]::new);

        if (previousPrivilegeTypes.length > 0) {
            updateCurrentPrivileges(grant, null, previousPrivilegeTypes);
        }
    }

    private void updateCurrentPrivileges(boolean grant, @Nullable KingbasePrivilegeType privilegeType, @Nullable KingbasePrivilegeType[] previousPrivilegeTypes) {

        if (ArrayUtils.isEmpty(currentObjects)) {
            DBWorkbench.getPlatformUI().showError("Update privilege", "Can't update privilege - no current object");
            return;
        }

        KingbasePrivilegeOwner databaseObject = getDatabaseObject();
        KingbaseSchema defaultPrivOwner = null;

        for (int i = 0; i < currentObjects.length; i++) {
            DBSObject currentObject = currentObjects[i];
            KingbasePrivilege permission = currentPermissions[i];
            if (permission == null) {
                if (!grant) {
                    // No permission - nothing to revoke
                    continue;
                }
                if (isRoleEditor()) {
                    KingbaseRole role = (KingbaseRole) databaseObject;
                    KingbasePrivilegeGrant.Kind kind;
                    String objectName;
                    String schemaName;
                    if (currentObject instanceof DBNDatabaseFolder) {
                        DBNDatabaseFolder folder = (DBNDatabaseFolder) currentObject;
                        DBSObject parentObject = ((DBNDatabaseItem) folder.getParentNode()).getObject();
                        if (parentObject instanceof KingbaseSchema) {
                            defaultPrivOwner = (KingbaseSchema) parentObject;
                            KingbaseDefaultPrivilege defaultPrivilege = new KingbaseDefaultPrivilege(
                                defaultPrivOwner,
                                role.getRoleReference(),
                                Collections.emptyList());
                            Class<? extends DBSObject> childrenClass = folder.getChildrenClass();
                            if (DBSSequence.class.isAssignableFrom(childrenClass)) {
                                kind = KingbasePrivilegeGrant.Kind.SEQUENCE;
                            } else if (DBSProcedure.class.isAssignableFrom(childrenClass)) {
                                kind = KingbasePrivilegeGrant.Kind.FUNCTION;
                            } else {
                                kind = KingbasePrivilegeGrant.Kind.TABLE;
                            }
                            defaultPrivilege.setUnderKind(kind);
                            permission = defaultPrivilege;
                        }
                    } else {
                        KingbasePrivilegeOwner permissionsOwner = (KingbasePrivilegeOwner) currentObject;
                        if (permissionsOwner instanceof KingbaseProcedure) {
                            if (((KingbaseProcedure) permissionsOwner).getKind() == KingbaseProcedureKind.p) {
                                kind = KingbasePrivilegeGrant.Kind.PROCEDURE;
                            } else {
                                kind = KingbasePrivilegeGrant.Kind.FUNCTION;
                            }
                            objectName = ((KingbaseProcedure) permissionsOwner).getUniqueName();
                        } else {
                            if (permissionsOwner instanceof KingbaseSchema) {
                                kind = KingbasePrivilegeGrant.Kind.SCHEMA;
                            } else if (permissionsOwner instanceof KingbaseSequence) {
                                kind = KingbasePrivilegeGrant.Kind.SEQUENCE;
                            } else {
                                kind = KingbasePrivilegeGrant.Kind.TABLE;
                            }
                            objectName = permissionsOwner.getName();
                        }
                        schemaName = permissionsOwner.getSchema().getName();
                        permission = new KingbaseRolePrivilege(
                            databaseObject,
                            kind,
                            schemaName,
                            objectName,
                            Collections.emptyList());
                    }
                } else {
                    String currentUserName = databaseObject.getDataSource().getContainer().getActualConnectionConfiguration().getUserName();
                    KingbaseRoleReference currentUserReference = new KingbaseRoleReference(databaseObject.getDatabase(), currentUserName, null);
                    KingbaseRoleReference grantee = ((KingbaseRole) currentObject).getRoleReference();
                    KingbasePrivilegeGrant privGrant = new KingbasePrivilegeGrant(
                        currentUserReference,
                        grantee,
                        databaseObject.getDatabase().getName(),
                        databaseObject.getSchema().getName(),
                        databaseObject.getName(),
                        privilegeType,
                        false,
                        false);
                    permission = new KingbaseObjectPrivilege(
                        databaseObject,
                        grantee,
                        Collections.singletonList(privGrant));
                }
                if (permission != null) {
                    // Add to map
                    currentPermissions[i] = permission;
                    permissionMap.put(permission.getName(), permission);
                }
            } else if (privilegeType != null) {
                // Check for privilege was already granted for this object
                boolean hasPriv = permission.getPermission(privilegeType) != KingbasePrivilege.NONE;
                if (grant != hasPriv && !grant) {
                    permissionMap.remove(permission.getName());
                }
            }
            if (permission == null) {
                log.error("Can't set permission to the object " + databaseObject.getName());
                return;
            }

            // Add command
            addChangeCommand(
                new KingbaseCommandGrantPrivilege(
                    defaultPrivOwner != null? defaultPrivOwner : databaseObject,
                    grant,
                    currentObject,
                    permission,
                    privilegeType == null ? previousPrivilegeTypes : new KingbasePrivilegeType[] { privilegeType }),
                new DBECommandReflector<KingbasePrivilegeOwner, KingbaseCommandGrantPrivilege>() {
                    @Override
                    public void redoCommand(KingbaseCommandGrantPrivilege cmd)
                    {

                    }
                    @Override
                    public void undoCommand(KingbaseCommandGrantPrivilege cmd)
                    {

                    }
                });

        }
    }

    private void updateObjectPermissions(List<DBSObject> objects) {

        boolean hasBadObjects = CommonUtils.isEmpty(objects);

        if (isRoleEditor()) {
            // In role editor each object may have different privilege set
            permissionTable.removeAll();

            if (!CommonUtils.isEmpty(objects)) {
                DBSObject object = objects.get(0);
                Class<?> objectType = null;
                if (object instanceof DBNDatabaseFolder && getDatabaseObject() != null
                    && getDatabaseObject().getDataSource().getServerType().supportsDefaultPrivileges()
                ) {
                    objectType = ((DBNDatabaseFolder) object).getChildrenClass();
                    if (objectType != null && KingbaseSchema.class.isAssignableFrom(objectType)) {
                        hasBadObjects = true;
                    }
                }
                if (objectType == null) {
                    objectType = object.getClass();
                }
                for (KingbasePrivilegeType pt : getDatabaseObject().getDataSource().getSupportedPrivilegeTypes()) {
                    if (!pt.isValid() || !pt.supportsType(objectType)) {
                        continue;
                    }
                    TableItem privItem = new TableItem(permissionTable, SWT.LEFT);
                    privItem.setText(0, pt.name());
                    privItem.setData(pt);
                }
                permissionTable.getParent().layout(true);
                UIUtils.packColumns(permissionTable, false);
            }
        }

        StringBuilder objectNames = new StringBuilder();
        if (!hasBadObjects) {
            for (DBSObject object : objects) {
                if (object instanceof DBNDatabaseFolder) {
                    objectNames.append(KingbaseMessages.role_privileges_editor_default_privileges_label);
                    break;
                }
                if (!(object instanceof KingbasePrivilegeOwner)) {
                    hasBadObjects = true;
                    break;
                }
                if (objectNames.length() > 0) objectNames.append(", ");
                objectNames.append(DBUtils.getObjectFullName(object.getDataSource(), object, DBPEvaluationContext.DML));
            }
        }
        boolean editEnabled;
        if (hasBadObjects) {
            objectDescriptionText.setText(KingbaseMessages.dialog_object_description_text_no_objects);

            this.currentPermissions = null;
            this.currentObjects = null;
            editEnabled = false;

        } else {
            objectDescriptionText.setText(objectNames.toString());

            this.currentObjects = objects.toArray(new DBSObject[0]);
            this.currentPermissions = new KingbasePrivilege[this.currentObjects.length];
            for (int i = 0; i < currentObjects.length; i++) {
                this.currentPermissions[i] = getObjectPermissions(currentObjects[i]);
            }
            editEnabled = !CommonUtils.isEmpty(objects);
        }

        if (editEnabled) {
            if (permissionsEnable != null) {
                permissionsEnable.restore();
                permissionsEnable = null;
            }
        } else {
            if (permissionsEnable == null) {
                permissionsEnable = ControlEnableState.disable(permEditPanel);
            }
        }

        if (ArrayUtils.isEmpty(currentPermissions)) {
            // We have object(s) but no permissions for them
            for (TableItem item : permissionTable.getItems()) {
                item.setChecked(false);
                item.setText(1, "");
                item.setText(2, "");
            }
        } else {
            for (TableItem item : permissionTable.getItems()) {
                KingbasePrivilegeType privType = (KingbasePrivilegeType) item.getData();
                short perm = currentPermissions[0] == null ? KingbasePrivilege.NONE : currentPermissions[0].getPermission(privType);
                item.setChecked((perm & KingbasePrivilege.GRANTED) != 0);
                if ((perm & KingbasePrivilege.WITH_GRANT_OPTION) != 0) {
                    item.setText(1, "X");
                } else {
                    item.setText(1, "");
                }
                if ((perm & KingbasePrivilege.WITH_HIERARCHY) != 0) {
                    item.setText(2, "X");
                } else {
                    item.setText(2, "");
                }
            }
        }
    }

    private boolean isRoleEditor() {
        return getDatabaseObject() instanceof KingbaseRole;
    }

    @Override
    public void setFocus() {
        if (this.pageControl != null) {
            this.pageControl.activate(true);
        }
        if (roleOrObjectTable != null) {
            roleOrObjectTable.getViewer().getControl().setFocus();
        }
    }

    @Override
    public synchronized void activatePart()
    {
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
                        PermissionInfo permissionInfo = new PermissionInfo();
                        permissionInfo.privileges = getDatabaseObject().getPrivileges(monitor, false);
                        permissionInfo.objectRootNode = DBNUtils.getNodeByObject(monitor, getDatabaseObject().getDatabase(), true);
                        if (isRoleEditor()) {
                            permissionInfo.objectRootNode = DBNUtils.getChildFolder(monitor, permissionInfo.objectRootNode, KingbaseSchema.class);
                        }
                        return permissionInfo;
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
    public RefreshResult refreshPart(Object source, boolean force)
    {
        if (force ||
            (source instanceof DBNEvent && ((DBNEvent) source).getSource() == DBNEvent.UPDATE_ON_SAVE) ||
            !isLoaded)
        {
            isLoaded = false;
            UIUtils.syncExec(() -> updateObjectPermissions(null));
            activatePart();
            return RefreshResult.REFRESHED;
        }
        return RefreshResult.IGNORED;
    }

    private static class DatabaseObjectFilter extends DatabaseNavigatorTreeFilter {
        @Override
        public boolean isLeafObject(Object object) {
            if (object instanceof DBNDatabaseItem) {
                DBSObject dbObject = ((DBNDatabaseItem) object).getObject();
                return
                    dbObject instanceof DBSEntity ||
                    dbObject instanceof DBSProcedure ||
                    dbObject instanceof DBSTableIndex ||
                    dbObject instanceof DBSPackage ||
                    dbObject instanceof DBSSequence ||
                    dbObject instanceof DBAUser;
            }
            return false;
        }
    }

    private static class ObjectOwnerFiler extends DatabaseNavigatorTreeFilter {
        @Override
        public boolean select(Object element) {
            if (element instanceof DBNDatabaseFolder item) {
                Class<? extends DBSObject> childrenClass = item.getChildrenClass();
                return childrenClass != null && KingbaseRole.class.isAssignableFrom(childrenClass);
            }
            return isLeafObject(element);
        }

        @Override
        public boolean isLeafObject(Object object) {
            return object instanceof DBNDatabaseItem item && item.getObject() instanceof KingbaseRole;
        }
    }

    private static class PermissionInfo {
        Collection<KingbasePrivilege> privileges;
        DBNDatabaseNode objectRootNode;
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
                    permissionMap.clear();
                    for (KingbasePrivilege perm : privs.privileges) {
                        permissionMap.put(perm.getName(), perm);
                    }
                    // Load navigator tree
                    roleOrObjectTable.reloadTree(privs.objectRootNode);
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

}