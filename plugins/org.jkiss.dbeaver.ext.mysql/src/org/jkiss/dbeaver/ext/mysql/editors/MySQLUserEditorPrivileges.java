/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mysql.editors;

import org.jkiss.dbeaver.Log;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.controls.PrivilegeTableControl;
import org.jkiss.dbeaver.ext.mysql.edit.MySQLCommandGrantPrivilege;
import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
 * MySQLUserEditorPrivileges
 */
public class MySQLUserEditorPrivileges extends MySQLUserEditorAbstract
{
    private static final Log log = Log.getLog(MySQLUserEditorPrivileges.class);

    private PageControl pageControl;
    private Table catalogsTable;
    private Table tablesTable;

    private boolean isLoaded = false;
    private MySQLCatalog selectedCatalog;
    private MySQLTableBase selectedTable;
    private PrivilegeTableControl tablePrivilegesTable;
    private PrivilegeTableControl otherPrivilegesTable;
    private volatile List<MySQLGrant> grants;

    private Font boldFont;

    @Override
    public void createPartControl(Composite parent)
    {
        boldFont = UIUtils.makeBoldFont(parent.getFont());

        pageControl = new PageControl(parent);

        Composite container = UIUtils.createPlaceholder(pageControl, 2, 5);
        GridData gd = new GridData(GridData.FILL_BOTH);
        container.setLayoutData(gd);

        Composite leftPane = UIUtils.createPlaceholder(container, 2);
        leftPane.setLayoutData(new GridData(GridData.FILL_BOTH));
        leftPane.setLayout(new GridLayout(2, true));
        {
            Composite catalogGroup = UIUtils.createControlGroup(leftPane, MySQLMessages.editors_user_editor_privileges_group_catalogs, 1, GridData.FILL_BOTH, 0);

            catalogsTable = new Table(catalogGroup, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
            catalogsTable.setHeaderVisible(true);
            gd = new GridData(GridData.FILL_BOTH);
            catalogsTable.setLayoutData(gd);
            catalogsTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selIndex = catalogsTable.getSelectionIndex();
                    if (selIndex <= 0) {
                        selectedCatalog = null;
                    } else {
                        selectedCatalog = (MySQLCatalog) catalogsTable.getItem(selIndex).getData();
                    }
                    showCatalogTables();
                    showGrants();
                }
            });
            UIUtils.createTableColumn(catalogsTable, SWT.LEFT, MySQLMessages.editors_user_editor_privileges_column_catalog);
            {
                TableItem item = new TableItem(catalogsTable, SWT.NONE);
                item.setText("% (All)"); //$NON-NLS-1$
                item.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
            }
            for (MySQLCatalog catalog : getDatabaseObject().getDataSource().getCatalogs()) {
                TableItem item = new TableItem(catalogsTable, SWT.NONE);
                item.setText(catalog.getName());
                item.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
                item.setData(catalog);
            }
            UIUtils.packColumns(catalogsTable);
        }

        {
            Composite tablesGroup = UIUtils.createControlGroup(leftPane, MySQLMessages.editors_user_editor_privileges_group_tables, 1, GridData.FILL_BOTH, 0);

            tablesTable = new Table(tablesGroup, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
            tablesTable.setHeaderVisible(true);
            gd = new GridData(GridData.FILL_BOTH);
            tablesTable.setLayoutData(gd);
            tablesTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selIndex = tablesTable.getSelectionIndex();
                    if (selIndex <= 0) {
                        selectedTable = null;
                    } else {
                        selectedTable = (MySQLTableBase) tablesTable.getItem(selIndex).getData();
                    }
                    showGrants();
                }
            });
            UIUtils.createTableColumn(tablesTable, SWT.LEFT, MySQLMessages.editors_user_editor_privileges_column_table);
            UIUtils.packColumns(tablesTable);
        }
        Composite ph = UIUtils.createPlaceholder(container, 1);
        ph.setLayoutData(new GridData(GridData.FILL_BOTH));

        tablePrivilegesTable = new PrivilegeTableControl(ph, MySQLMessages.editors_user_editor_privileges_control_table_privileges);
        gd = new GridData(GridData.FILL_BOTH);
        tablePrivilegesTable.setLayoutData(gd);

        otherPrivilegesTable = new PrivilegeTableControl(ph, MySQLMessages.editors_user_editor_privileges_control_other_privileges);
        gd = new GridData(GridData.FILL_BOTH);
        otherPrivilegesTable.setLayoutData(gd);

        catalogsTable.setSelection(0);
        showCatalogTables();

        pageControl.createProgressPanel();

        parent.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                UIUtils.dispose(boldFont);
            }
        });

        addGrantListener(tablePrivilegesTable);
        addGrantListener(otherPrivilegesTable);
    }

    private void addGrantListener(final PrivilegeTableControl privTable)
    {
        privTable.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event event)
            {
                final MySQLPrivilege privilege = (MySQLPrivilege) event.data;
                final boolean isGrant = event.detail == 1;
                final MySQLCatalog curCatalog = selectedCatalog;
                final MySQLTableBase curTable = selectedTable;
                updateLocalData(privilege, isGrant, curCatalog, curTable);

                // Add command
                addChangeCommand(
                    new MySQLCommandGrantPrivilege(
                        getDatabaseObject(),
                        isGrant,
                        curCatalog,
                        curTable,
                        privilege),
                    new DBECommandReflector<MySQLUser, MySQLCommandGrantPrivilege>() {
                        @Override
                        public void redoCommand(MySQLCommandGrantPrivilege mySQLCommandGrantPrivilege)
                        {
                            if (!privTable.isDisposed() && curCatalog == selectedCatalog && curTable == selectedTable) {
                                privTable.checkPrivilege(privilege, isGrant);
                            }
                            updateLocalData(privilege, isGrant, curCatalog, curTable);
                        }
                        @Override
                        public void undoCommand(MySQLCommandGrantPrivilege mySQLCommandGrantPrivilege)
                        {
                            if (!privTable.isDisposed() && curCatalog == selectedCatalog && curTable == selectedTable) {
                                privTable.checkPrivilege(privilege, !isGrant);
                            }
                            updateLocalData(privilege, !isGrant, curCatalog, curTable);
                        }
                    });
            }
        });
    }

    private void updateLocalData(MySQLPrivilege privilege, boolean isGrant, MySQLCatalog curCatalog, MySQLTableBase curTable)
    {
        // Modify local grants (and clear grants cache in user objects)
        getDatabaseObject().clearGrantsCache();
        boolean found = false;
        for (MySQLGrant grant : grants) {
            if (grant.matches(curCatalog) && grant.matches(curTable)) {
                if (privilege.isGrantOption()) {
                    grant.setGrantOption(isGrant);
                } else if (isGrant) {
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
                privilege.isGrantOption());
            grants.add(grant);
        }
        highlightCatalogs();
        highlightTables();
    }

    private void showCatalogTables()
    {
        LoadingJob.createService(
            new DatabaseLoadService<Collection<MySQLTableBase>>(MySQLMessages.editors_user_editor_privileges_service_load_tables, getExecutionContext()) {
                @Override
                public Collection<MySQLTableBase> evaluate(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {
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

    private void showGrants()
    {
        if (grants == null) {
            return;
        }
        List<MySQLGrant> curGrants = new ArrayList<>();
        for (MySQLGrant grant : grants) {
            if (grant.matches(selectedCatalog) && grant.matches(selectedTable)) {
                curGrants.add(grant);
            }
        }
        tablePrivilegesTable.fillGrants(curGrants);
        otherPrivilegesTable.fillGrants(curGrants);
    }

    @Override
    public synchronized void activatePart()
    {
        if (isLoaded) {
            return;
        }
        isLoaded = true;
        LoadingJob.createService(
            new DatabaseLoadService<java.util.List<MySQLPrivilege>>(MySQLMessages.editors_user_editor_privileges_service_load_privileges, getExecutionContext()) {
                @Override
                public java.util.List<MySQLPrivilege> evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
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
            if (!grant.isAllPrivileges() && !grant.hasNonAdminPrivileges()) {
                i.remove();
            }
        }
        highlightCatalogs();

        showGrants();
        showCatalogTables();
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
                            item.setFont(boldFont);
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
                            item.setFont(boldFont);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        // do nothing
    }

    private class PageControl extends UserPageControl {
        public PageControl(Composite parent) {
            super(parent);
        }

        public ProgressVisualizer<Collection<MySQLTableBase>> createTablesLoadVisualizer() {
            return new ProgressVisualizer<Collection<MySQLTableBase>>() {
                @Override
                public void completeLoading(Collection<MySQLTableBase> tables) {
                    super.completeLoading(tables);
                    if (tablesTable.isDisposed()) {
                        return;
                    }
                    tablesTable.removeAll();
                    {
                        TableItem item = new TableItem(tablesTable, SWT.NONE);
                        item.setText("% (All)"); //$NON-NLS-1$
                        item.setImage(DBeaverIcons.getImage(DBIcon.TREE_TABLE));
                    }
                    for (MySQLTableBase table : tables) {
                        TableItem item = new TableItem(tablesTable, SWT.NONE);
                        item.setText(table.getName());
                        item.setImage(DBeaverIcons.getImage(table.isView() ? DBIcon.TREE_VIEW : DBIcon.TREE_TABLE));
                        item.setData(table);
                    }
                    highlightTables();
                    UIUtils.packColumns(tablesTable);
                }
            };
        }

        public ProgressVisualizer<java.util.List<MySQLPrivilege>> createPrivilegesLoadVisualizer() {
            return new ProgressVisualizer<java.util.List<MySQLPrivilege>>() {
                @Override
                public void completeLoading(java.util.List<MySQLPrivilege> privs) {
                    super.completeLoading(privs);
                    List<MySQLPrivilege> otherPrivs = new ArrayList<>();
                    List<MySQLPrivilege> tablePrivs = new ArrayList<>();
                    for (MySQLPrivilege priv : privs) {
                        if (priv.getKind() == MySQLPrivilege.Kind.ADMIN) {
                            continue;
                        }
                        if (priv.getContext().contains("Table")) {
                            tablePrivs.add(priv);
                        } else {
                            otherPrivs.add(priv);
                        }
                    }
                    tablePrivilegesTable.fillPrivileges(tablePrivs);
                    otherPrivilegesTable.fillPrivileges(otherPrivs);
                    loadGrants();
                }
            };
        }

    }


}