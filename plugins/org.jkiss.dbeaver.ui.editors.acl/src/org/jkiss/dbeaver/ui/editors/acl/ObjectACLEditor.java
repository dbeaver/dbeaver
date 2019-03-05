/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.acl;

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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.*;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.*;
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

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * PostgresRolePrivilegesEditor
 */
public abstract class ObjectACLEditor extends AbstractDatabaseObjectEditor<DBAPrivilegeOwner>
{
    private PageControl pageControl;

    private boolean isLoaded;
    private DatabaseNavigatorTree roleOrObjectTable;
    private Composite permEditPanel;
    private Table permissionTable;
    private ControlEnableState permissionsEnable;

    private DBSObject[] currentObjects;
    private DBAPrivilege[] currentPrivileges;
    private Map<String, DBAPrivilege> privilegeMap = new HashMap<>();
    private Text objectDescriptionText;

    protected abstract DBAPrivilegeType[] getPrivilegeTypes();

    protected abstract DBAPrivilege createNewPrivilege(DBAPrivilegeOwner owner, DBSObject object, DBAPrivilege privilege);

    protected String getObjectUniqueName(DBSObject object) {
        return DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL);
    }

    public void createPartControl(Composite parent) {
        this.pageControl = new PageControl(parent);

        SashForm composite = UIUtils.createPartDivider(getSite().getPart(), this.pageControl, SWT.HORIZONTAL);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));


        roleOrObjectTable = new DatabaseNavigatorTree(
            composite,
            DBWorkbench.getPlatform().getNavigatorModel().getRoot(),
            SWT.MULTI | SWT.FULL_SELECTION,
            false,
            isRoleEditor() ? new DatabaseObjectFilter() : null);
        roleOrObjectTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        final TreeViewer treeViewer = roleOrObjectTable.getViewer();
        treeViewer.setLabelProvider(new DatabaseNavigatorLabelProvider(treeViewer) {
            @Override
            public Font getFont(Object element) {
                if (element instanceof DBNDatabaseNode) {
                    DBSObject object = ((DBNDatabaseNode) element).getObject();
                    if (object instanceof DBSSchema) {
                        String schemaPrefix = DBUtils.getQuotedIdentifier(object) + ".";
                        for (String tableName : privilegeMap.keySet()) {
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
                    try {
                        String elementTypeName = ((DBNDatabaseFolder) element).getMeta().getType();
                        if (elementTypeName == null) {
                            return false;
                        }
                        Class<?> childType = Class.forName(elementTypeName);
                        return DBAPrivilegeOwner.class.isAssignableFrom(childType);
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
                        updateCurrentPrivileges(((TableItem) e.item).getChecked(), (DBAPrivilegeType) e.item.getData());
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
                for (DBAPrivilegeType pt : getPrivilegeTypes()) {
                    if (!pt.isValid() || !pt.supportsType(getDatabaseObject().getClass())) {
                        continue;
                    }
                    TableItem privItem = new TableItem(permissionTable, SWT.LEFT);
                    privItem.setText(0, pt.getName());
                    privItem.setData(pt);
                }
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

    private DBAPrivilege getObjectPermissions(DBSObject object) {
/*
        if (object instanceof PostgreProcedure) {
            String fqProcName = DBUtils.getQuotedIdentifier(((PostgreProcedure) object).getSchema()) + "." + ((PostgreProcedure) object).getSpecificName();
            return privilegeMap.get(fqProcName);
        } else {
            return privilegeMap.get(DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL));
        }
*/
        return privilegeMap.get(getObjectUniqueName(object));
    }

    private void updateCurrentPrivileges(boolean grant, DBAPrivilegeType privilegeType) {

        if (ArrayUtils.isEmpty(currentObjects)) {
            DBWorkbench.getPlatformUI().showError("Update privilege", "Can't update privilege - no current object");
            return;
        }

        for (int i = 0; i < currentObjects.length; i++) {
            DBSObject currentObject = currentObjects[i];
            DBAPrivilege privilege = currentPrivileges[i];
            if (privilege == null) {
                if (!grant) {
                    // No permission - nothing to revoke
                    continue;
                }
                privilege = createNewPrivilege(getDatabaseObject(), currentObject, privilege);
                // Add to map
                privilegeMap.put(privilege.getName(), privilege);
            } else if (privilegeType != null) {
                // Check for privilege was already granted for this object
/*
                boolean hasPriv = privilege.getPermission(privilegeType) != PostgrePermission.NONE;
                if (grant == hasPriv) {
                    continue;
                }
*/
            }

/*
            // Add command
            addChangeCommand(
                new PostgreCommandGrantPrivilege(
                    getDatabaseObject(),
                    grant,
                    privilege,
                    privilegeType == null ? null : new DBAPrivilegeType[] { privilegeType }),
                new DBECommandReflector<DBAPrivilegeOwner, PostgreCommandGrantPrivilege>() {
                    @Override
                    public void redoCommand(PostgreCommandGrantPrivilege cmd)
                    {
                    }
                    @Override
                    public void undoCommand(PostgreCommandGrantPrivilege cmd)
                    {
                    }
                });
*/

        }
    }

    private void updateObjectPermissions(List<DBSObject> objects) {

        boolean hasBadObjects = CommonUtils.isEmpty(objects);

        if (isRoleEditor()) {
            // In role editor each object may have different privilege set
            permissionTable.removeAll();

            if (!CommonUtils.isEmpty(objects)) {
                Class<?> objectType = objects.get(0).getClass();
                for (DBAPrivilegeType pt : getPrivilegeTypes()) {
                    if (!pt.isValid() || !pt.supportsType(objectType)) {
                        continue;
                    }
                    TableItem privItem = new TableItem(permissionTable, SWT.LEFT);
                    privItem.setText(0, pt.getName());
                    privItem.setData(pt);
                }
                permissionTable.getParent().layout(true);
                UIUtils.packColumns(permissionTable, false);
            }
        }

        StringBuilder objectNames = new StringBuilder();
        if (!hasBadObjects) {
            for (DBSObject object : objects) {
                if (!(object instanceof DBAPrivilegeOwner)) {
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

            this.currentPrivileges = null;
            this.currentObjects = null;
            editEnabled = false;

        } else {
            objectDescriptionText.setText(objectNames.toString());

            this.currentObjects = objects.toArray(new DBSObject[0]);
            this.currentPrivileges = new DBAPrivilege[this.currentObjects.length];
            for (int i = 0; i < currentObjects.length; i++) {
                this.currentPrivileges[i] = getObjectPermissions(currentObjects[i]);
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

        if (ArrayUtils.isEmpty(currentPrivileges)) {
            // We have object(s) but no permissions for them
            for (TableItem item : permissionTable.getItems()) {
                item.setChecked(false);
                item.setText(1, "");
                item.setText(2, "");
            }
        } else {
            for (TableItem item : permissionTable.getItems()) {
                DBAPrivilegeType privType = (DBAPrivilegeType) item.getData();
/*
                short perm = currentPrivileges[0] == null ? PostgrePermission.NONE : currentPrivileges[0].getPermission(privType);
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
*/
            }
        }
    }

    private boolean isRoleEditor() {
        return getDatabaseObject() instanceof DBARole;
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

        UIUtils.asyncExec(() -> UIUtils.packColumns(permissionTable, false));

        LoadingJob.createService(
            new DatabaseLoadService<Collection<? extends DBAPrivilege>>("Load permissions", getExecutionContext()) {
                @Override
                public Collection<? extends DBAPrivilege> evaluate(DBRProgressMonitor monitor) throws InvocationTargetException {
                    monitor.beginTask("Load privileges from database..", 1);
                    try {
                        monitor.subTask("Load " + getDatabaseObject().getName() + " privileges");
                        return getDatabaseObject().getPrivileges(monitor, false);
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
        if (force ||
            (source instanceof DBNEvent && ((DBNEvent) source).getSource() == DBNEvent.UPDATE_ON_SAVE) ||
            !isLoaded)
        {
            isLoaded = false;
            UIUtils.syncExec(() -> updateObjectPermissions(null));
            activatePart();
        }
    }

    private static class DatabaseObjectFilter extends DatabaseNavigatorTreeFilter {
        @Override
        public boolean filterFolders() {
            return false;
        }

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

        @Override
        public boolean select(Object element) {
            if (!(element instanceof DBNDatabaseItem)) {
                return true;
            }
            return isLeafObject(element);
        }
    }

    private class PageControl extends ProgressPageControl {
        PageControl(Composite parent) {
            super(parent, SWT.SHEET);
        }

        ProgressVisualizer<Collection<? extends DBAPrivilege>> createLoadVisualizer() {
            return new ProgressVisualizer<Collection<? extends DBAPrivilege>>() {
                @Override
                public void completeLoading(Collection<? extends DBAPrivilege> privs) {
                    super.completeLoading(privs);
                    if (privs == null) {
                        return;
                    }
                    privilegeMap.clear();
                    for (DBAPrivilege perm : privs) {
                        privilegeMap.put(perm.getName(), perm);
                    }
                    // Load navigator tree
                    DBRProgressMonitor monitor = new VoidProgressMonitor();
                    DBSObject parentContainer = DBUtils.getParentOfType(DBSCatalog.class, getDatabaseObject());
                    if (parentContainer == null) {
                        parentContainer = DBUtils.getParentOfType(DBSSchema.class, getDatabaseObject());
                    }
                    if (parentContainer == null) {
                        parentContainer = getDatabaseObject().getParentObject();
                    }
                    DBNDatabaseNode dbNode = NavigatorUtils.getNodeByObject(parentContainer);
                    DBNDatabaseNode rootNode;
                    if (isRoleEditor()) {
                        rootNode = NavigatorUtils.getChildFolder(monitor, dbNode, DBSSchema.class);
                    } else {
                        rootNode = NavigatorUtils.getChildFolder(monitor, dbNode, DBARole.class);
                    }
                    if (rootNode == null) {
                        DBWorkbench.getPlatformUI().showError("Object tree", "Can't detect root node for objects tree");
                    } else {
                        roleOrObjectTable.reloadTree(rootNode);
                    }
                    //roleOrObjectTable.getViewer().getControl().setFocus();
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