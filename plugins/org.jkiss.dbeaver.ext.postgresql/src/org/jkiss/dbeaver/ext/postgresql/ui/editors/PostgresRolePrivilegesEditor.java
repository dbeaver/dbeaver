/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.ui.editors;

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
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.postgresql.edit.PostgreCommandGrantPrivilege;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.ActionUtils;
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

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * PostgresRolePrivilegesEditor
 */
public class PostgresRolePrivilegesEditor extends AbstractDatabaseObjectEditor<PostgrePermissionsOwner>
{
    private PageControl pageControl;

    private boolean isLoaded;
    private DatabaseNavigatorTree roleOrObjectTable;
    private Composite permEditPanel;
    private Table permissionTable;
    private ControlEnableState permissionsEnable;

    private DBSObject[] currentObjects;
    private PostgrePermission[] currentPermissions;
    private Map<String, PostgrePermission> permissionMap = new HashMap<>();
    private Text objectDescriptionText;

    public void createPartControl(Composite parent) {
        this.pageControl = new PageControl(parent);

        SashForm composite = UIUtils.createPartDivider(getSite().getPart(), this.pageControl, SWT.HORIZONTAL);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        roleOrObjectTable = new DatabaseNavigatorTree(composite, DBeaverCore.getInstance().getNavigatorModel().getRoot(), SWT.MULTI | SWT.FULL_SELECTION, false, new DatabaseNavigatorTreeFilter());
        roleOrObjectTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        final TreeViewer treeViewer = roleOrObjectTable.getViewer();
        treeViewer.setLabelProvider(new DatabaseNavigatorLabelProvider(treeViewer) {
            @Override
            public Font getFont(Object element) {
                if (element instanceof DBNDatabaseNode) {
                    DBSObject object = ((DBNDatabaseNode) element).getObject();
                    if (object instanceof PostgreSchema) {
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
        treeViewer.addSelectionChangedListener(event -> {
            handleSelectionChange();
        });
        treeViewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof DBNNode && !(element instanceof DBNDatabaseNode)) {
                    return false;
                }
                if (element instanceof DBNDatabaseFolder) {
                    try {
                        Class<?> childType = Class.forName(((DBNDatabaseFolder) element).getMeta().getType());
                        return PostgreTableReal.class.isAssignableFrom(childType);
                    } catch (ClassNotFoundException e) {
                        return false;
                    }
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
            UIUtils.createTableColumn(permissionTable, SWT.LEFT, "Permission");
            UIUtils.createTableColumn(permissionTable, SWT.CENTER, "With GRANT");
            UIUtils.createTableColumn(permissionTable, SWT.CENTER, "With Hierarchy");
            permissionTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (e.detail == SWT.CHECK) {
                        updateCurrentPrivileges(((TableItem) e.item).getChecked(), (PostgrePrivilegeType) e.item.getData());
                    }
                }
            });
            permissionTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDown(MouseEvent e) {
                    super.mouseDown(e);
                }
            });

            for (PostgrePrivilegeType pt : PostgrePrivilegeType.values()) {
                if (!pt.isValid()) {
                    continue;
                }
                TableItem privItem = new TableItem(permissionTable, SWT.LEFT);
                privItem.setText(0, pt.name());
                privItem.setData(pt);
            }

            Composite buttonPanel = new Composite(permEditPanel, SWT.NONE);
            buttonPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            buttonPanel.setLayout(new RowLayout());

            UIUtils.createPushButton(buttonPanel, "Grant All", null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    boolean hadNonChecked = false;
                    for (TableItem item : permissionTable.getItems()) {
                        if (!item.getChecked()) hadNonChecked = true;
                        item.setChecked(true);
                    }
                    if (hadNonChecked) updateCurrentPrivileges(true, null);
                }
            });
            UIUtils.createPushButton(buttonPanel, "Revoke All", null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    boolean hadChecked = false;
                    for (TableItem item : permissionTable.getItems()) {
                        if (item.getChecked()) hadChecked = true;
                        item.setChecked(false);
                    }
                    if (hadChecked) {
                        updateCurrentPrivileges(false, null);
                    }
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

    private PostgrePermission getObjectPermissions(DBSObject object) {
        return permissionMap.get(DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL));
    }

    private void updateCurrentPrivileges(boolean grant, PostgrePrivilegeType privilegeType) {

        if (ArrayUtils.isEmpty(currentObjects)) {
            DBeaverUI.getInstance().showError("Update privilege", "Can't update privilege - no current object");
            return;
        }

        for (int i = 0; i < currentObjects.length; i++) {
            DBSObject currentObject = currentObjects[i];
            PostgrePermission permission = currentPermissions[i];
            if (permission == null) {
                if (!grant) {
                    // No permission - nothing to revoke
                    continue;
                }
                if (isRoleEditor()) {
                    PostgreTableBase table = (PostgreTableBase) currentObject;
                    permission = new PostgreRolePermission(getDatabaseObject(), table.getSchema().getName(), table.getName(), Collections.emptyList());
                } else {
                    permission = new PostgreTablePermission(getDatabaseObject(), currentObject.getName(), Collections.emptyList());
                }
                // Add to map
                permissionMap.put(permission.getName(), permission);
            } else {
                // Check for privilege was already granted for this object
                boolean hasPriv = permission.getPermission(privilegeType) != PostgrePermission.NONE;
                if (grant == hasPriv) {
                    continue;
                }
            }

            // Add command
            addChangeCommand(
                new PostgreCommandGrantPrivilege(
                    getDatabaseObject(),
                    grant,
                    permission,
                    privilegeType),
                new DBECommandReflector<PostgrePermissionsOwner, PostgreCommandGrantPrivilege>() {
                    @Override
                    public void redoCommand(PostgreCommandGrantPrivilege cmd)
                    {
//                    if (!privTable.isDisposed() && curCatalog == selectedCatalog && curTable == selectedTable) {
//                        privTable.checkPrivilege(privilege, isGrant);
//                    }
//                    updateLocalData(privilege, isGrant, curCatalog, curTable);
                    }
                    @Override
                    public void undoCommand(PostgreCommandGrantPrivilege cmd)
                    {
//                    if (!privTable.isDisposed() && curCatalog == selectedCatalog && curTable == selectedTable) {
//                        privTable.checkPrivilege(privilege, !isGrant);
//                    }
//                    updateLocalData(privilege, !isGrant, curCatalog, curTable);
                    }
                });

        }
    }

    private void updateObjectPermissions(List<DBSObject> objects) {
        boolean hasBadObjects = CommonUtils.isEmpty(objects);
        StringBuilder objectNames = new StringBuilder();
        if (!hasBadObjects) {
            for (DBSObject object : objects) {
                if (!(object instanceof PostgreTableBase) && !(object instanceof PostgreRole)) {
                    hasBadObjects = true;
                    break;
                }
                if (objectNames.length() > 0) objectNames.append(", ");
                objectNames.append(DBUtils.getObjectFullName(object.getDataSource(), object, DBPEvaluationContext.DML));
            }
        }
        boolean editEnabled;
        if (hasBadObjects) {
            objectDescriptionText.setText("<no objects>");

            this.currentPermissions = null;
            this.currentObjects = null;
            editEnabled = false;

        } else {
            objectDescriptionText.setText(objectNames.toString());

            this.currentObjects = objects.toArray(new DBSObject[objects.size()]);
            this.currentPermissions = new PostgrePermission[this.currentObjects.length];
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
                PostgrePrivilegeType privType = (PostgrePrivilegeType) item.getData();
                short perm = currentPermissions[0] == null ? PostgrePermission.NONE : currentPermissions[0].getPermission(privType);
                item.setChecked((perm & PostgrePermission.GRANTED) != 0);
                if ((perm & PostgrePermission.WITH_GRANT_OPTION) != 0) {
                    item.setText(1, "X");
                } else {
                    item.setText(1, "");
                }
                if ((perm & PostgrePermission.WITH_HIERARCHY) != 0) {
                    item.setText(2, "X");
                } else {
                    item.setText(2, "");
                }
            }
        }
    }

    private boolean isRoleEditor() {
        return getDatabaseObject() instanceof PostgreRole;
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
    public synchronized void activatePart()
    {
        if (isLoaded) {
            return;
        }
        isLoaded = true;

        DBeaverUI.asyncExec(() -> {
            UIUtils.packColumns(permissionTable, false);
        });

        LoadingJob.createService(
            new DatabaseLoadService<Collection<PostgrePermission>>("Load permissions", getExecutionContext()) {
                @Override
                public Collection<PostgrePermission> evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Load privileges from database..", 1);
                    try {
                        monitor.subTask("Load " + getDatabaseObject().getName() + " privileges");
                        return getDatabaseObject().getPermissions(monitor);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        monitor.done();
                    }
                }
            },
            pageControl.createLoadVisualizer()).schedule();
    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        isLoaded = false;
        DBeaverUI.syncExec(() -> {
                updateObjectPermissions(null);
        });
        activatePart();
    }

    private class PageControl extends ProgressPageControl {
        PageControl(Composite parent) {
            super(parent, SWT.SHEET);
        }

        ProgressVisualizer<Collection<PostgrePermission>> createLoadVisualizer() {
            return new ProgressVisualizer<Collection<PostgrePermission>>() {
                @Override
                public void completeLoading(Collection<PostgrePermission> privs) {
                    super.completeLoading(privs);
                    permissionMap.clear();
                    for (PostgrePermission perm : privs) {
                        permissionMap.put(perm.getName(), perm);
                    }
                    // Load navigator tree
                    DBRProgressMonitor monitor = new VoidProgressMonitor();
                    DBNDatabaseNode dbNode = NavigatorUtils.getNodeByObject(getDatabaseObject().getDatabase());
                    DBNDatabaseNode rootNode;
                    if (isRoleEditor()) {
                        rootNode = NavigatorUtils.getChildFolder(monitor, dbNode, PostgreSchema.class);
                    } else {
                        rootNode = NavigatorUtils.getChildFolder(monitor, dbNode, PostgreRole.class);
                    }
                    if (rootNode == null) {
                        DBeaverUI.getInstance().showError("Object tree", "Can't detect root node for objects tree");
                    } else {
                        roleOrObjectTable.reloadTree(rootNode);
                    }
                    roleOrObjectTable.getViewer().getControl().setFocus();
                    handleSelectionChange();
                }
            };
        }

        @Override
        protected void fillCustomActions(IContributionManager contributionManager) {
            super.fillCustomActions(contributionManager);

            contributionManager.add(new Separator());

            IWorkbenchSite workbenchSite = getSite();
            if (workbenchSite != null) {
                DatabaseEditorUtils.contributeStandardEditorActions(workbenchSite, contributionManager);
                contributionManager.add(ActionUtils.makeCommandContribution(workbenchSite, IWorkbenchCommandConstants.FILE_REFRESH));
            }
        }
    }

}