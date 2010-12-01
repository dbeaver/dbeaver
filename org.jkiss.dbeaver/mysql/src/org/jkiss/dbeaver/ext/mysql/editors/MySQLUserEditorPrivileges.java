/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseObjectCommandReflector;
import org.jkiss.dbeaver.ext.mysql.controls.PrivilegeTableControl;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLGrant;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.runtime.MySQLCommandGrantPrivilege;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
 * MySQLUserEditorPrivileges
 */
public class MySQLUserEditorPrivileges extends MySQLUserEditorAbstract
{
    static final Log log = LogFactory.getLog(MySQLUserEditorPrivileges.class);

    private PageControl pageControl;
    private Table catalogsTable;
    private Table tablesTable;

    private boolean isLoaded = false;
    private MySQLCatalog selectedCatalog;
    private MySQLTable selectedTable;
    private PrivilegeTableControl tablePrivilegesTable;
    private PrivilegeTableControl otherPrivilegesTable;
    private volatile List<MySQLGrant> grants;

    private Font boldFont;

    public void createPartControl(Composite parent)
    {
        boldFont = UIUtils.makeBoldFont(parent.getFont());

        pageControl = new PageControl(parent);

        Composite container = UIUtils.createPlaceholder(pageControl, 3, 5);
        GridData gd = new GridData(GridData.FILL_BOTH);
        container.setLayoutData(gd);

        {
            Composite catalogGroup = UIUtils.createControlGroup(container, "Catalogs", 1, GridData.FILL_VERTICAL, 200);

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
            UIUtils.createTableColumn(catalogsTable, SWT.LEFT, "Catalog");
            {
                TableItem item = new TableItem(catalogsTable, SWT.NONE);
                item.setText("% (All)");
                item.setImage(DBIcon.TREE_CATALOG.getImage());
            }
            for (MySQLCatalog catalog : getUser().getDataSource().getCatalogs()) {
                TableItem item = new TableItem(catalogsTable, SWT.NONE);
                item.setText(catalog.getName());
                item.setImage(DBIcon.TREE_CATALOG.getImage());
                item.setData(catalog);
            }
            UIUtils.packColumns(catalogsTable);
        }

        {
            Composite catalogGroup = UIUtils.createControlGroup(container, "Tables", 1, GridData.FILL_VERTICAL, 200);

            tablesTable = new Table(catalogGroup, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
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
                        selectedTable = (MySQLTable) tablesTable.getItem(selIndex).getData();
                    }
                    showGrants();
                }
            });
            UIUtils.createTableColumn(tablesTable, SWT.LEFT, "Table");
            UIUtils.packColumns(tablesTable);
        }
        Composite ph = UIUtils.createPlaceholder(container, 1);
        ph.setLayoutData(new GridData(GridData.FILL_BOTH));

        tablePrivilegesTable = new PrivilegeTableControl(ph, "Table Privileges");
        gd = new GridData(GridData.FILL_BOTH);
        tablePrivilegesTable.setLayoutData(gd);

        otherPrivilegesTable = new PrivilegeTableControl(ph, "Other Privileges");
        gd = new GridData(GridData.FILL_BOTH);
        otherPrivilegesTable.setLayoutData(gd);

        catalogsTable.setSelection(0);
        showCatalogTables();

        pageControl.createProgressPanel();

        parent.addDisposeListener(new DisposeListener() {
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
            public void handleEvent(Event event)
            {
                final MySQLPrivilege privilege = (MySQLPrivilege) event.data;
                final boolean grant = event.detail == 1;
                final MySQLCatalog curCatalog = selectedCatalog;
                final MySQLTable curTable = selectedTable;
                addChangeCommand(
                    new MySQLCommandGrantPrivilege(
                        grant,
                        getDatabaseObject(),
                        curCatalog,
                        curTable,
                        privilege),
                    new IDatabaseObjectCommandReflector<MySQLCommandGrantPrivilege>() {
                        public void redoCommand(MySQLCommandGrantPrivilege mySQLCommandGrantPrivilege)
                        {
                            if (!privTable.isDisposed() && curCatalog == selectedCatalog && curTable == selectedTable) {
                                privTable.checkPrivilege(privilege, grant);
                            }
                        }
                        public void undoCommand(MySQLCommandGrantPrivilege mySQLCommandGrantPrivilege)
                        {
                            if (!privTable.isDisposed() && curCatalog == selectedCatalog && curTable == selectedTable) {
                                privTable.checkPrivilege(privilege, !grant);
                            }
                        }
                    });
            }
        });
    }

    private void showCatalogTables()
    {
        LoadingUtils.executeService(
            new DatabaseLoadService<Collection<MySQLTable>>("Load tables", getUser().getDataSource()) {
                public Collection<MySQLTable> evaluate()
                    throws InvocationTargetException, InterruptedException
                {
                    if (selectedCatalog == null) {
                        return Collections.emptyList();
                    }
                    try {
                        return selectedCatalog.getTables(getProgressMonitor());
                    }
                    catch (DBException e) {
                        log.error(e);
                    }
                    return null;
                }
            },
            pageControl.createTablesLoadVisualizer());
    }

    private void showGrants()
    {
        if (grants == null) {
            return;
        }
        List<MySQLGrant> curGrants = new ArrayList<MySQLGrant>();
        for (MySQLGrant grant : grants) {
            if (grant.matches(selectedCatalog) && grant.matches(selectedTable)) {
                curGrants.add(grant);
            }
        }
        tablePrivilegesTable.fillGrants(curGrants);
        otherPrivilegesTable.fillGrants(curGrants);
    }

    public synchronized void activatePart()
    {
        if (isLoaded) {
            return;
        }
        isLoaded = true;
        LoadingUtils.executeService(
            new DatabaseLoadService<java.util.List<MySQLPrivilege>>("Load privileges", getUser().getDataSource()) {
                public java.util.List<MySQLPrivilege> evaluate() throws InvocationTargetException, InterruptedException
                {
                    try {
                        return getUser().getDataSource().getPrivileges(getProgressMonitor());
                    }
                    catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            },
            pageControl.createPrivilegesLoadVisualizer());
    }

    @Override
    protected PageControl getPageControl()
    {
        return pageControl;
    }

    @Override
    protected void processGrants(List<MySQLGrant> grantsTmp)
    {
        this.grants = new ArrayList<MySQLGrant>(grantsTmp);
        for (Iterator<MySQLGrant> i = grants.iterator(); i.hasNext();) {
            MySQLGrant grant = i.next();
            if (!grant.isAllPrivileges() && !grant.hasNonAdminPrivileges()) {
                i.remove();
            }
        }
        // Highlight granted catalogs
        for (TableItem item : catalogsTable.getItems()) {
            MySQLCatalog catalog = (MySQLCatalog)item.getData();
            item.setFont(null);
            for (MySQLGrant grant : grants) {
                if (grant.matches(catalog)) {
                    item.setFont(boldFont);
                    break;
                }
            }
        }
        showGrants();
        showCatalogTables();
    }

    private class PageControl extends UserPageControl {
        public PageControl(Composite parent) {
            super(parent);
        }

        public ProgressVisualizer<Collection<MySQLTable>> createTablesLoadVisualizer() {
            return new ProgressVisualizer<Collection<MySQLTable>>() {
                public void completeLoading(Collection<MySQLTable> tables) {
                    super.completeLoading(tables);
                    if (tablesTable.isDisposed()) {
                        return;
                    }
                    tablesTable.removeAll();
                    {
                        TableItem item = new TableItem(tablesTable, SWT.NONE);
                        item.setText("% (All)");
                        item.setImage(DBIcon.TREE_TABLE.getImage());
                        if (grants != null) {
                            for (MySQLGrant grant : grants) {
                                if (grant.matches(selectedCatalog) && grant.matches((MySQLTable) null)) {
                                    item.setFont(boldFont);
                                    break;
                                }
                            }
                        }
                    }
                    for (MySQLTable table : tables) {
                        TableItem item = new TableItem(tablesTable, SWT.NONE);
                        item.setText(table.getName());
                        item.setImage(table.isView() ? DBIcon.TREE_VIEW.getImage() : DBIcon.TREE_TABLE.getImage());
                        item.setData(table);

                        if (grants != null) {
                            for (MySQLGrant grant : grants) {
                                if (grant.matches(selectedCatalog) && grant.matches(table)) {
                                    item.setFont(boldFont);
                                    break;
                                }
                            }
                        }

                    }
                    UIUtils.packColumns(tablesTable);
                }
            };
        }

        public ProgressVisualizer<java.util.List<MySQLPrivilege>> createPrivilegesLoadVisualizer() {
            return new ProgressVisualizer<java.util.List<MySQLPrivilege>>() {
                public void completeLoading(java.util.List<MySQLPrivilege> privs) {
                    super.completeLoading(privs);
                    List<MySQLPrivilege> otherPrivs = new ArrayList<MySQLPrivilege>();
                    List<MySQLPrivilege> tablePrivs = new ArrayList<MySQLPrivilege>();
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