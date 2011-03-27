/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.controls.PrivilegeTableControl;
import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.ext.mysql.runtime.MySQLCommandGrantPrivilege;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
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

        Composite container = UIUtils.createPlaceholder(pageControl, 2, 5);
        GridData gd = new GridData(GridData.FILL_BOTH);
        container.setLayoutData(gd);

        Composite leftPane = UIUtils.createPlaceholder(container, 2);
        leftPane.setLayoutData(new GridData(GridData.FILL_BOTH));
        leftPane.setLayout(new GridLayout(2, true));
        {
            Composite catalogGroup = UIUtils.createControlGroup(leftPane, "Catalogs", 1, GridData.FILL_BOTH, 0);

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
            Composite tablesGroup = UIUtils.createControlGroup(leftPane, "Tables", 1, GridData.FILL_BOTH, 0);

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
                final boolean isGrant = event.detail == 1;
                final MySQLCatalog curCatalog = selectedCatalog;
                final MySQLTable curTable = selectedTable;
                updateLocalData(privilege, isGrant, curCatalog, curTable);

                // Add command
                addChangeCommand(
                    new MySQLCommandGrantPrivilege(
                        isGrant,
                        curCatalog,
                        curTable,
                        privilege),
                    new DBECommandReflector<MySQLUser, MySQLCommandGrantPrivilege>() {
                        public void redoCommand(MySQLCommandGrantPrivilege mySQLCommandGrantPrivilege)
                        {
                            if (!privTable.isDisposed() && curCatalog == selectedCatalog && curTable == selectedTable) {
                                privTable.checkPrivilege(privilege, isGrant);
                            }
                            updateLocalData(privilege, isGrant, curCatalog, curTable);
                        }
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

    private void updateLocalData(MySQLPrivilege privilege, boolean isGrant, MySQLCatalog curCatalog, MySQLTable curTable)
    {
        // Modify local grants (and clear grants cache in user objects)
        getUser().clearGrantsCache();
        boolean found = false;
        for (MySQLGrant grant : grants) {
            if (grant.matches(curCatalog) && grant.matches(curTable)) {
                if (privilege.isGrantOption()) {
                    grant.setGrantOption(isGrant);
                } else if (isGrant) {
                    if (!grant.getPrivileges().contains(privilege)) {
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
            List<MySQLPrivilege> privileges = new ArrayList<MySQLPrivilege>();
            if (!privilege.isGrantOption()) {
                privileges.add(privilege);
            }
            MySQLGrant grant = new MySQLGrant(
                getUser(),
                privileges,
                curCatalog == null ? "*" : curCatalog.getName(),
                curTable == null ? "*" : curTable.getName(),
                false,
                privilege.isGrantOption());
            grants.add(grant);
        }
        highlightCatalogs();
        highlightTables();
    }

    private void showCatalogTables()
    {
        LoadingUtils.createService(
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
            pageControl.createTablesLoadVisualizer())
            .schedule();
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
        LoadingUtils.createService(
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
        this.grants = new ArrayList<MySQLGrant>(grantsTmp);
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

    private void highlightTables()
    {
        for (TableItem item : tablesTable.getItems()) {
            MySQLTable table = (MySQLTable) item.getData();
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
                    }
                    for (MySQLTable table : tables) {
                        TableItem item = new TableItem(tablesTable, SWT.NONE);
                        item.setText(table.getName());
                        item.setImage(table.isView() ? DBIcon.TREE_VIEW.getImage() : DBIcon.TREE_TABLE.getImage());
                        item.setData(table);
                    }
                    highlightTables();
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