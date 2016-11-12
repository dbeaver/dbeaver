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
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;

import java.util.*;
import java.util.List;


class PostgreExportWizardPageObjects extends PostgreWizardPageSettings<PostgreExportWizard>
{
    private static final Log log = Log.getLog(PostgreExportWizardPageObjects.class);

    private Table catalogTable;
    private Table tablesTable;
    private Map<PostgreSchema, Set<PostgreTableBase>> checkedObjects = new HashMap<>();

    private PostgreSchema curCatalog;

    protected PostgreExportWizardPageObjects(PostgreExportWizard wizard)
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

        Group objectsGroup = UIUtils.createControlGroup(composite, "Objects", 1, GridData.FILL_HORIZONTAL, 0);
        objectsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        SashForm sash = new CustomSashForm(objectsGroup, SWT.VERTICAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        catalogTable = new Table(sash, SWT.BORDER | SWT.CHECK);
        catalogTable.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                TableItem item = (TableItem) event.item;
                PostgreSchema catalog = (PostgreSchema) item.getData();
                if (event.detail == SWT.CHECK) {
                    catalogTable.select(catalogTable.indexOf(item));
                    checkedObjects.remove(catalog);
                }
                loadTables(catalog);
                updateState();
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
                    updateState();
                }
            }
        });

        final Button exportViewsCheck = UIUtils.createCheckbox(objectsGroup, "Show views", false);
        exportViewsCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                wizard.showViews = exportViewsCheck.getSelection();
                loadTables(null);
            }
        });

        PostgreDataSource dataSource = null;
        Set<PostgreSchema> activeCatalogs = new LinkedHashSet<>();
        for (DBSObject object : wizard.getDatabaseObjects()) {
            if (object instanceof PostgreSchema) {
                activeCatalogs.add((PostgreSchema) object);
                dataSource = ((PostgreSchema) object).getDataSource();
            } else if (object instanceof PostgreTableBase) {
                PostgreSchema catalog = ((PostgreTableBase) object).getContainer();
                dataSource = catalog.getDataSource();
                activeCatalogs.add(catalog);
                Set<PostgreTableBase> tables = checkedObjects.get(catalog);
                if (tables == null) {
                    tables = new HashSet<>();
                    checkedObjects.put(catalog, tables);
                }
                tables.add((PostgreTableBase) object);
                if (((PostgreTableBase) object).isView()) {
                    wizard.showViews = true;
                    exportViewsCheck.setSelection(true);
                }
            } else if (object.getDataSource() instanceof PostgreDataSource) {
                dataSource = (PostgreDataSource) object.getDataSource();
            }
        }
        if (dataSource != null) {
            boolean tablesLoaded = false;
            try {
                for (PostgreSchema catalog : dataSource.getDefaultInstance().getSchemas(VoidProgressMonitor.INSTANCE)) {
                    TableItem item = new TableItem(catalogTable, SWT.NONE);
                    item.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
                    item.setText(0, catalog.getName());
                    item.setData(catalog);
                    if (activeCatalogs.contains(catalog)) {
                        item.setChecked(true);
                        catalogTable.select(catalogTable.indexOf(item));
                        if (!tablesLoaded) {
                            loadTables(catalog);
                            tablesLoaded = true;
                        }
                    }
                }
            } catch (DBException e) {
                log.error(e);
            }
        }
        updateState();
        setControl(composite);
    }

    private void updateCheckedTables() {
        Set<PostgreTableBase> checkedTables = new HashSet<>();
        TableItem[] tableItems = tablesTable.getItems();
        for (TableItem item : tableItems) {
            if (item.getChecked()) {
                checkedTables.add((PostgreTableBase) item.getData());
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

    private boolean isChecked(PostgreSchema catalog) {
        for (TableItem item : catalogTable.getItems()) {
            if (item.getData() == catalog) {
                return item.getChecked();
            }
        }
        return false;
    }

    private void loadTables(final PostgreSchema catalog) {
        if (catalog != null) {
            curCatalog = catalog;
        }
        if (curCatalog == null) {
            return;
        }
        final boolean isCatalogChecked = isChecked(curCatalog);
        final Set<PostgreTableBase> checkedObjects = this.checkedObjects.get(curCatalog);
        new AbstractJob("Load '" + curCatalog.getName() + "' tables") {
            {
                setUser(true);
            }
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    final List<PostgreTableBase> objects = new ArrayList<>();
                    objects.addAll(curCatalog.getTables(monitor));
                    if (wizard.showViews) {
                        objects.addAll(curCatalog.getViews(monitor));
                    }
                    Collections.sort(objects, DBUtils.nameComparator());
                    DBeaverUI.syncExec(new Runnable() {
                        @Override
                        public void run() {
                            tablesTable.removeAll();
                            for (PostgreTableBase table : objects) {
                                TableItem item = new TableItem(tablesTable, SWT.NONE);
                                item.setImage(DBeaverIcons.getImage(table.isView() ? DBIcon.TREE_VIEW : DBIcon.TREE_TABLE));
                                item.setText(0, table.getName());
                                item.setData(table);
                                item.setChecked(isCatalogChecked && (checkedObjects == null || checkedObjects.contains(table)));
                            }
                        }
                    });
                } catch (DBException e) {
                    UIUtils.showErrorDialog(null, "Table list", "Can't read table list", e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    public void saveState() {
        wizard.objects.clear();
        for (TableItem item : catalogTable.getItems()) {
            if (item.getChecked()) {
                PostgreSchema catalog = (PostgreSchema) item.getData();
                PostgreDatabaseExportInfo info = new PostgreDatabaseExportInfo(catalog, checkedObjects.get(catalog));
                wizard.objects.add(info);
            }
        }
    }

    private void updateState()
    {
        boolean complete = false;
        if (!checkedObjects.isEmpty()) {
            complete = true;
        }
        for (TableItem item : catalogTable.getItems()) {
            if (item.getChecked()) {
                complete = true;
                break;
            }
        }
        setPageComplete(complete);
    }

}
