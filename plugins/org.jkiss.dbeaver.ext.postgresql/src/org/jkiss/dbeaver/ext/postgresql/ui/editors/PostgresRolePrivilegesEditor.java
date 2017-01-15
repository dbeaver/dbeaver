/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.ui.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRolePrivilege;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * PostgresRolePrivilegesEditor
 */
public class PostgresRolePrivilegesEditor extends AbstractDatabaseObjectEditor<PostgreRole>
{
    private static final Log log = Log.getLog(PostgresRolePrivilegesEditor.class);

    private PageControl pageControl;

    private Font boldFont;
    private boolean isLoaded;

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
/*
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
*/
    }

    @Override
    public void setFocus() {

    }

    @Override
    public synchronized void activatePart()
    {
        if (isLoaded) {
            return;
        }
        isLoaded = true;
        LoadingJob.createService(
            new DatabaseLoadService<List<PostgreRolePrivilege>>("Load privileges", getExecutionContext()) {
                @Override
                public List<PostgreRolePrivilege> evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        return getDatabaseObject().getPrivileges(monitor);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            },
            pageControl.createPrivilegesLoadVisualizer())
            .schedule();
    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        // do nothing
    }

    private class PageControl extends ProgressPageControl {
        public PageControl(Composite parent) {
            super(parent, SWT.NONE);
        }

        public ProgressPageControl.ProgressVisualizer<List<PostgreRolePrivilege>> createPrivilegesLoadVisualizer() {
            return new ProgressPageControl.ProgressVisualizer<List<PostgreRolePrivilege>>() {
                @Override
                public void completeLoading(List<PostgreRolePrivilege> privs) {
                    super.completeLoading(privs);
/*
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
*/
                }
            };
        }

    }


}