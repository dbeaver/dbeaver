/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.List;


class PostgreBackupWizardPageObjects extends PostgreWizardPageSettings<PostgreBackupWizard>
{
    private static final Log log = Log.getLog(PostgreBackupWizardPageObjects.class);

    private Table schemasTable;
    private Table tablesTable;
    private Map<PostgreSchema, Set<PostgreTableBase>> checkedObjects = new HashMap<>();

    private PostgreSchema curSchema;
    private PostgreDataSource dataSource;

    PostgreBackupWizardPageObjects(PostgreBackupWizard wizard)
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

        {
            Composite catPanel = UIUtils.createPlaceholder(sash, 1);
            catPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
            schemasTable = new Table(catPanel, SWT.BORDER | SWT.CHECK);
            schemasTable.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event event) {
                    TableItem item = (TableItem) event.item;
                    PostgreSchema catalog = (PostgreSchema) item.getData();
                    if (event.detail == SWT.CHECK) {
                        schemasTable.select(schemasTable.indexOf(item));
                        checkedObjects.remove(catalog);
                    }
                    loadTables(catalog);
                    updateState();
                }
            });
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 50;
            schemasTable.setLayoutData(gd);

            Composite buttonsPanel = UIUtils.createPlaceholder(catPanel, 3, 5);
            buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            new Label(buttonsPanel, SWT.NONE).setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));
            createCheckButtons(buttonsPanel, schemasTable);
        }

        final Button exportViewsCheck;
        {
            Composite tablesPanel = UIUtils.createPlaceholder(sash, 1);
            tablesPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

            tablesTable = new Table(tablesPanel, SWT.BORDER | SWT.CHECK);
            GridData gd = new GridData(GridData.FILL_BOTH);
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

            Composite buttonsPanel = UIUtils.createPlaceholder(tablesPanel, 3, 5);
            buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            exportViewsCheck = UIUtils.createCheckbox(buttonsPanel, "Show views", false);
            exportViewsCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    wizard.showViews = exportViewsCheck.getSelection();
                    loadTables(null);
                }
            });
            exportViewsCheck.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));
            createCheckButtons(buttonsPanel, tablesTable);
        }

        dataSource = null;
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
                for (PostgreSchema schema : dataSource.getDefaultInstance().getSchemas(new VoidProgressMonitor())) {
                    if (schema.isSystem() || schema.isUtility()) {
                        continue;
                    }
                    TableItem item = new TableItem(schemasTable, SWT.NONE);
                    item.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
                    item.setText(0, schema.getName());
                    item.setData(schema);
                    if (activeCatalogs.contains(schema)) {
                        item.setChecked(true);
                        schemasTable.select(schemasTable.indexOf(item));
                        if (!tablesLoaded) {
                            loadTables(schema);
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
        TableItem catalogItem = schemasTable.getItem(schemasTable.getSelectionIndex());
        catalogItem.setChecked(!checkedTables.isEmpty());
        if (checkedTables.isEmpty() || checkedTables.size() == tableItems.length) {
            checkedObjects.remove(curSchema);
        } else {
            checkedObjects.put(curSchema, checkedTables);
        }
    }

    private boolean isChecked(PostgreSchema catalog) {
        for (TableItem item : schemasTable.getItems()) {
            if (item.getData() == catalog) {
                return item.getChecked();
            }
        }
        return false;
    }

    private void loadTables(final PostgreSchema catalog) {
        if (catalog != null) {
            curSchema = catalog;
        }
        if (curSchema == null) {
            return;
        }
        final boolean isCatalogChecked = isChecked(curSchema);
        final Set<PostgreTableBase> checkedObjects = this.checkedObjects.get(curSchema);
        new AbstractJob("Load '" + curSchema.getName() + "' tables") {
            {
                setUser(true);
            }
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    final List<PostgreTableBase> objects = new ArrayList<>();
                    objects.addAll(curSchema.getTables(monitor));
                    if (wizard.showViews) {
                        objects.addAll(curSchema.getViews(monitor));
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
                    DBUserInterface.getInstance().showError("Table list", "Can't read table list", e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    public void saveState() {
        wizard.objects.clear();
        List<PostgreSchema> schemas = new ArrayList<>();
        List<PostgreTableBase> tables = new ArrayList<>();
        for (TableItem item : schemasTable.getItems()) {
            if (item.getChecked()) {
                PostgreSchema schema = (PostgreSchema) item.getData();
                Set<PostgreTableBase> checkedTables = checkedObjects.get(schema);
                if (CommonUtils.isEmpty(checkedTables)) {
                    // All tables checked
                    schemas.add(schema);
                } else {
                    // Only a few tables checked
                    tables.addAll(checkedTables);
                }
            }
        }
        PostgreDatabaseBackupInfo info = new PostgreDatabaseBackupInfo(dataSource.getDefaultInstance(), schemas, tables);
        wizard.objects.add(info);
    }

    private void updateState()
    {
        boolean complete = false;
        if (!checkedObjects.isEmpty()) {
            complete = true;
        }
        for (TableItem item : schemasTable.getItems()) {
            if (item.getChecked()) {
                complete = true;
                break;
            }
        }
        setPageComplete(complete);
    }

}
