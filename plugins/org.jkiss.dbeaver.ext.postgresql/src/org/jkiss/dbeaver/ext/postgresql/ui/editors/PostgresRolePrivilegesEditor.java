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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorLabelProvider;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilter;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * PostgresRolePrivilegesEditor
 */
public class PostgresRolePrivilegesEditor extends AbstractDatabaseObjectEditor<PostgrePermissionsOwner>
{
    private PageControl pageControl;

    private boolean isLoaded;
    private DatabaseNavigatorTree roleOrObjectTable;
    private Table permissionTable;
    private Action actionCheckAll;
    private Action actionCheckNone;

    private PostgrePermission currentPermission;
    private DBSObject currentObject;
    private Map<String, PostgrePermission> permissionMap = new HashMap<>();

    public void createPartControl(Composite parent) {

        {
/*
            actionGrant = new Action("Grant " + (isRoleEditor() ? "object" : "role"), DBeaverIcons.getImageDescriptor(getObjectAddIcon())) {
                @Override
                public void run() {
                    try {
                        VoidProgressMonitor monitor = new VoidProgressMonitor();
                        if (isRoleEditor()) {
                            DBNDatabaseNode dbNode = NavigatorUtils.getNodeByObject(getDatabaseObject().getDatabase());
                            DBNDatabaseNode schemasNode = NavigatorUtils.getChildFolder(monitor, dbNode, PostgreSchema.class);

                            List<DBNNode> tableNodes = BrowseObjectDialog.selectObjects(getSite().getShell(), "Select object", schemasNode, null,
                                new Class[]{Object.class}, new Class[]{PostgreTableBase.class});
                            if (tableNodes != null) {
                                for (DBNNode node : tableNodes) {

                                }
                            }
                        } else {
                            List<PostgreRole> allRoles = new ArrayList<>(getDatabaseObject().getDatabase().getAuthIds(monitor));
                            if (currentPrivs != null) {
                                for (PostgrePermission p : currentPrivs) {
                                    allRoles.remove(p.getTargetObject(monitor));
                                }
                            }
                            SelectObjectDialog<PostgreRole> roleSelector = new SelectObjectDialog<>(
                                getSite().getShell(),
                                "Select role",
                                true,
                                "Permissions/Role/Selector",
                                allRoles,
                                null);
                            if (roleSelector.open() == IDialogConstants.OK_ID) {

                            }
                        }
                    } catch (DBException e) {
                        DBeaverUI.getInstance().showError("Load object", "Error loading permission objects", e);
                    }
                }
            };

            actionRevoke = new Action("Revoke " + (isRoleEditor() ? "object" : "role"), DBeaverIcons.getImageDescriptor(getObjectRemoveIcon())) {
                @Override
                public void run() {
                    super.run();
                }
            };
*/
            actionCheckAll = new Action("All") {
                @Override
                public void run() {
                    boolean hadNonChecked = false;
                    for (TableItem item : permissionTable.getItems()) {
                        if (!item.getChecked()) hadNonChecked = true;
                        item.setChecked(true);
                    }
                    if (hadNonChecked) updateCurrentPrivileges();
                }
            };
            actionCheckNone = new Action("None") {
                @Override
                public void run() {
                    boolean hadChecked = false;
                    for (TableItem item : permissionTable.getItems()) {
                        item.setChecked(false);
                        if (item.getChecked()) hadChecked = true;
                    }
                    if (hadChecked) {
                        updateCurrentPrivileges();
                    }
                }
            };
        }

        this.pageControl = new PageControl(parent);

        SashForm composite = UIUtils.createPartDivider(getSite().getPart(), this.pageControl, SWT.HORIZONTAL);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        roleOrObjectTable = new DatabaseNavigatorTree(composite, DBeaverCore.getInstance().getNavigatorModel().getRoot(), SWT.FULL_SELECTION, false, new DatabaseNavigatorTreeFilter());
        roleOrObjectTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        final TreeViewer treeViewer = roleOrObjectTable.getViewer();
        treeViewer.setLabelProvider(new DatabaseNavigatorLabelProvider(treeViewer) {
            @Override
            public Font getFont(Object element) {
                if (element instanceof DBNDatabaseNode) {
                    DBSObject object = ((DBNDatabaseNode) element).getObject();
                    if (getObjectPermissions(object) != null) {
                        return boldFont;
                    }
                }
                return null;
            }
        });
        treeViewer.addSelectionChangedListener(event -> {
            DBSObject selectedObject = NavigatorUtils.getSelectedObject(treeViewer.getSelection());
            if (selectedObject == null) {
                updateObjectPermissions(null, null);
            } else {
                updateObjectPermissions(getObjectPermissions(selectedObject), selectedObject);
            }
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

        permissionTable = new Table(composite, SWT.FULL_SELECTION | SWT.CHECK);
        permissionTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        permissionTable.setHeaderVisible(true);
        permissionTable.setLinesVisible(true);
        UIUtils.createTableColumn(permissionTable, SWT.LEFT, "Permission");
        UIUtils.createTableColumn(permissionTable, SWT.CENTER, "With GRANT");
        UIUtils.createTableColumn(permissionTable, SWT.CENTER, "With Hierarchy");
        permissionTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.detail == SWT.CHECK) {
                    updateCurrentPrivileges();
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

        pageControl.createOrSubstituteProgressPanel(getSite());
        updateObjectPermissions(null, null);
        registerContextMenu();
    }

    private PostgrePermission getObjectPermissions(DBSObject object) {
        return permissionMap.get(DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL));
    }

    private void updateCurrentPrivileges() {
        //System.out.println("Privs changed");
    }

    private void registerContextMenu()
    {
/*
        // Register objects context menu
        {
            MenuManager menuMgr = new MenuManager();
            menuMgr.addMenuListener(manager -> {
                manager.add(actionGrant);
                manager.add(actionRevoke);
            });

            menuMgr.setRemoveAllWhenShown(true);
            Menu menu = menuMgr.createContextMenu(roleOrObjectTable);
            roleOrObjectTable.setMenu(menu);
        }
*/
        // Register objects context menu
        {
            MenuManager menuMgr = new MenuManager();
            menuMgr.addMenuListener(manager -> {
                manager.add(actionCheckAll);
                manager.add(actionCheckNone);
            });

            menuMgr.setRemoveAllWhenShown(true);
            Menu menu = menuMgr.createContextMenu(permissionTable);
            permissionTable.setMenu(menu);
        }
    }

    private void openPermObject() {
        DBSObject selectedObject = NavigatorUtils.getSelectedObject(roleOrObjectTable.getViewer().getSelection());
        if (selectedObject == null) {
            updateObjectPermissions(null, null);
        } else {
            PostgrePermission permission = permissionMap.get(selectedObject);
            updateObjectPermissions(permission, selectedObject);
        }

/*
        new AbstractJob("Open target object") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    final PostgreObject targetObject = permission.getTargetObject(monitor);
                    if (targetObject != null) {
                        DBeaverUI.syncExec(() -> NavigatorHandlerObjectOpen.openEntityEditor(targetObject));
                    }
                } catch (DBException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
*/
    }

    private void updateObjectPermissions(PostgrePermission data, DBSObject curObject) {
        this.currentPermission = data;
        if (curObject instanceof PostgreTableBase || curObject instanceof PostgreRole) {
            this.currentObject = curObject;
        } else {
            this.currentObject = curObject = null;
        }

        if (data == null) {
            permissionTable.setEnabled(curObject != null);
            for (TableItem item : permissionTable.getItems()) {
                item.setChecked(false);
                item.setText(1, "");
                item.setText(2, "");
            }
        } else {
            permissionTable.setEnabled(true);
            for (TableItem item : permissionTable.getItems()) {
                PostgrePrivilegeType privType = (PostgrePrivilegeType) item.getData();
                short perm = data.getPermission(privType);
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
        actionCheckAll.setEnabled(data != null);
        actionCheckNone.setEnabled(data != null);
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
            pageControl.createLoadVisualizer())
            .schedule();
    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        isLoaded = false;
        DBeaverUI.syncExec(() -> {
                updateObjectPermissions(null, null);
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
                }
            };
        }

        @Override
        protected void fillCustomActions(IContributionManager contributionManager) {
            super.fillCustomActions(contributionManager);

            contributionManager.add(new Separator());
            contributionManager.add(actionCheckAll);
            contributionManager.add(actionCheckNone);
            contributionManager.add(new Separator());

            IWorkbenchSite workbenchSite = getSite();
            if (workbenchSite != null) {
                contributionManager.add(ActionUtils.makeCommandContribution(workbenchSite, IWorkbenchCommandConstants.FILE_REFRESH));
            }
        }
    }

}