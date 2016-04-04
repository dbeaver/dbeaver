/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.*;
import java.util.List;


class MySQLExportWizardPageObjects extends MySQLWizardPageSettings<MySQLExportWizard>
{

    private Table catalogTable;
    private Table tablesTable;
    private Map<MySQLCatalog, Set<MySQLTableBase>> checkedObjects = new HashMap<>();
    private boolean exportViews;

    private MySQLCatalog curCatalog;

    protected MySQLExportWizardPageObjects(MySQLExportWizard wizard)
    {
        super(wizard, "Schemas/tables");
        setTitle("Choose objects to export");
        setDescription("Schemas/tables/views which will be exported");
    }

    @Override
    public boolean isPageComplete()
    {
        return super.isPageComplete();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group objectsGroup = UIUtils.createControlGroup(composite, MySQLMessages.tools_db_export_wizard_page_settings_group_objects, 1, GridData.FILL_HORIZONTAL, 0);
        objectsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        SashForm sash = new CustomSashForm(objectsGroup, SWT.VERTICAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        catalogTable = new Table(sash, SWT.BORDER | SWT.CHECK);
        catalogTable.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                TableItem item = (TableItem) event.item;
                MySQLCatalog catalog = (MySQLCatalog) item.getData();
                if (event.detail == SWT.CHECK) {
                    catalogTable.select(catalogTable.indexOf(item));
                    checkedObjects.remove(catalog);
                }
                loadTables(catalog);
            }
        });
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 50;
        catalogTable.setLayoutData(gd);

        tablesTable = new Table(sash, SWT.BORDER | SWT.CHECK);
        gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 50;
        tablesTable.setLayoutData(gd);
        tablesTable.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                if (event.detail == SWT.CHECK) {
                    updateCheckedTables();
                }
            }
        });

        final Button exportViewsCheck = UIUtils.createCheckbox(objectsGroup, "Export views", false);
        exportViewsCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                exportViews = exportViewsCheck.getSelection();
                loadTables(null);
            }
        });

        final MySQLCatalog activeCatalog = wizard.getDatabaseObject();
        final MySQLDataSource dataSource = activeCatalog.getDataSource();
        for (MySQLCatalog catalog : dataSource.getCatalogs()) {
            TableItem item = new TableItem(catalogTable, SWT.NONE);
            item.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
            item.setText(0, catalog.getName());
            item.setData(catalog);
            if (catalog == activeCatalog) {
                item.setChecked(true);
                catalogTable.select(catalogTable.indexOf(item));
            }
        }
        loadTables(activeCatalog);

        setControl(composite);
    }

    private void updateCheckedTables() {
        Set<MySQLTableBase> checkedTables = new HashSet<>();
        TableItem[] tableItems = tablesTable.getItems();
        for (TableItem item : tableItems) {
            if (item.getChecked()) {
                checkedTables.add((MySQLTableBase) item.getData());
            }
        }
        TableItem catalogItem = catalogTable.getItem(catalogTable.getSelectionIndex());
        catalogItem.setChecked(!checkedTables.isEmpty());
        if (checkedTables.isEmpty() || checkedTables.size() == tableItems.length) {
            checkedObjects.remove(curCatalog);
        } else {
            checkedObjects.put(curCatalog, checkedTables);
        }
    }

    private boolean isChecked(MySQLCatalog catalog) {
        for (TableItem item : catalogTable.getItems()) {
            if (item.getData() == catalog) {
                return item.getChecked();
            }
        }
        return false;
    }

    private void loadTables(final MySQLCatalog catalog) {
        if (catalog != null) {
            curCatalog = catalog;
        }
        if (curCatalog == null) {
            return;
        }
        final boolean isCatalogChecked = isChecked(curCatalog);
        final Set<MySQLTableBase> checkedObjects = this.checkedObjects.get(curCatalog);
        new AbstractJob("Load '" + curCatalog.getName() + "' tables") {
            {
                setUser(true);
            }
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    final List<MySQLTableBase> objects = new ArrayList<>();
                    objects.addAll(curCatalog.getTables(monitor));
                    if (exportViews) {
                        objects.addAll(curCatalog.getViews(monitor));
                    }
                    Collections.sort(objects, DBUtils.nameComparator());
                    UIUtils.runInUI(getShell(), new Runnable() {
                        @Override
                        public void run() {
                            tablesTable.removeAll();
                            for (MySQLTableBase table : objects) {
                                TableItem item = new TableItem(tablesTable, SWT.NONE);
                                item.setImage(DBeaverIcons.getImage(table.isView() ? DBIcon.TREE_VIEW : DBIcon.TREE_TABLE));
                                item.setText(0, table.getName());
                                item.setData(table);
                                item.setChecked(isCatalogChecked && (checkedObjects == null || checkedObjects.contains(table)));
                            }
                        }
                    });
                } catch (DBException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    public void saveState() {
        wizard.objects.clear();
        for (TableItem item : catalogTable.getItems()) {
            if (item.getChecked()) {
                MySQLCatalog catalog = (MySQLCatalog) item.getData();
                MySQLDatabaseExportInfo info = new MySQLDatabaseExportInfo(catalog, checkedObjects.get(catalog));
                wizard.objects.add(info);
            }
        }
    }

    private void updateState()
    {
        //wizard.removeDefiner = removeDefiner.getSelection();

        getContainer().updateButtons();
    }

}
