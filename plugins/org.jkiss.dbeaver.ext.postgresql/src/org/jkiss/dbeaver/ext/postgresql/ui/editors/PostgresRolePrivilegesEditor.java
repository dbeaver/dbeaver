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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * PostgresRolePrivilegesEditor
 */
public class PostgresRolePrivilegesEditor extends AbstractDatabaseObjectEditor<PostgrePermissionsOwner>
{
    private PageControl pageControl;

    private Font boldFont;
    private boolean isLoaded;
    private Table roleOrObjectTable;
    private Table permissionTable;

    public void createPartControl(Composite parent) {
        this.boldFont = UIUtils.makeBoldFont(parent.getFont());
        parent.addDisposeListener(e -> UIUtils.dispose(boldFont));

        this.pageControl = new PageControl(parent);

        SashForm composite = UIUtils.createPartDivider(getSite().getPart(), this.pageControl, SWT.VERTICAL);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        roleOrObjectTable = new Table(composite, SWT.FULL_SELECTION);
        roleOrObjectTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        roleOrObjectTable.setHeaderVisible(true);
        roleOrObjectTable.setLinesVisible(true);
        UIUtils.createTableColumn(roleOrObjectTable, SWT.LEFT, isRoleEditor() ? "Object" : "Role");
        roleOrObjectTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem[] selection = roleOrObjectTable.getSelection();
                if (selection == null || selection.length == 0) {
                    updateObjectPermissions(null);
                } else {
                    updateObjectPermissions((PostgrePermission)selection[0].getData());
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                openPermObject();
            }
        });

        permissionTable = new Table(composite, SWT.FULL_SELECTION | SWT.CHECK);
        permissionTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        permissionTable.setHeaderVisible(true);
        permissionTable.setLinesVisible(true);
        permissionTable.setEnabled(false);
        UIUtils.createTableColumn(permissionTable, SWT.LEFT, "Permission");
        UIUtils.createTableColumn(permissionTable, SWT.CENTER, "With GRANT");
        UIUtils.createTableColumn(permissionTable, SWT.CENTER, "With Hierarchy");

        for (PostgrePrivilegeType pt : PostgrePrivilegeType.values()) {
            if (pt == PostgrePrivilegeType.UNKNOWN) {
                continue;
            }
            TableItem privItem = new TableItem(permissionTable, SWT.LEFT);
            privItem.setText(0, pt.name());
            privItem.setData(pt);
        }

        pageControl.createOrSubstituteProgressPanel(getSite());
    }

    private void openPermObject() {
        TableItem[] selection = roleOrObjectTable.getSelection();
        if (selection == null || selection.length == 0) {
            return;
        } else {
            updateObjectPermissions((PostgrePermission)selection[0].getData());
        }
        PostgrePermission permission = (PostgrePermission) selection[0].getData();
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
    }

    private void updateObjectPermissions(PostgrePermission data) {
        if (data == null) {
            permissionTable.setEnabled(false);
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
    }

    private void fillPrivileges(Collection<PostgrePermission> privs) {
        for (PostgrePermission permission : privs) {
            TableItem permItem = new TableItem(roleOrObjectTable, SWT.LEFT);
            permItem.setData(permission);
            if (isRoleEditor()) {
                permItem.setText(0,
                    ((PostgreRolePermission)permission).getFullTableName());
                permItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_TABLE));
            } else {
                permItem.setText(0,
                    ((PostgreTablePermission)permission).getGrantee());
                permItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_USER));
            }

        }
    }

    private boolean isRoleEditor() {
        return getDatabaseObject() instanceof PostgreRole;
    }

    @Override
    public void setFocus() {
        if (this.pageControl != null) {
            this.pageControl.setFocus();
            // Important! activation of page control fills action toolbar
            this.pageControl.activate(true);
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
            UIUtils.packColumns(roleOrObjectTable, true);
            UIUtils.packColumns(permissionTable, false);
        });

        LoadingJob.createService(
            new DatabaseLoadService<Collection<PostgrePermission>>("Load permissions", getExecutionContext()) {
                @Override
                public Collection<PostgrePermission> evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        return getDatabaseObject().getPermissions(monitor);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
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
                roleOrObjectTable.removeAll();
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
                    fillPrivileges(privs);
                }
            };
        }

        @Override
        protected void fillCustomActions(IContributionManager contributionManager) {
            super.fillCustomActions(contributionManager);
            IWorkbenchSite workbenchSite = getSite();
            if (workbenchSite != null) {
                contributionManager.add(ActionUtils.makeCommandContribution(workbenchSite, IWorkbenchCommandConstants.FILE_REFRESH));
            }
        }
    }

}