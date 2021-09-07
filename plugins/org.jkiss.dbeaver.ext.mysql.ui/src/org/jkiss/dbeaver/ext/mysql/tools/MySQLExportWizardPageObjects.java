/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.ext.mysql.tasks.MySQLDatabaseExportInfo;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.*;


class MySQLExportWizardPageObjects extends MySQLWizardPageSettings<MySQLExportWizard>
{

    private Table catalogTable;
    private Table tablesTable;
    private Map<MySQLCatalog, Set<MySQLTableBase>> checkedObjects = new HashMap<>();

    private MySQLCatalog curCatalog;
    private Button exportViewsCheck;

    protected MySQLExportWizardPageObjects(MySQLExportWizard wizard)
    {
        super(wizard, MySQLUIMessages.tools_db_export_wizard_page_objects_dialog_wizard_title);
        setTitle(MySQLUIMessages.tools_db_export_wizard_page_objects_dialog_title_choose_objects);
        setDescription(MySQLUIMessages.tools_db_export_wizard_page_objects_dialog_description);
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

        Group objectsGroup = UIUtils.createControlGroup(composite, MySQLUIMessages.tools_db_export_wizard_page_settings_group_objects, 1, GridData.FILL_HORIZONTAL, 0);
        objectsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        SashForm sash = new CustomSashForm(objectsGroup, SWT.VERTICAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Composite catPanel = UIUtils.createComposite(sash, 1);
            catPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
            catalogTable = new Table(catPanel, SWT.BORDER | SWT.CHECK);
            catalogTable.addListener(SWT.Selection, event -> {
                TableItem item = (TableItem) event.item;
                if (item != null) {
                    MySQLCatalog catalog = (MySQLCatalog) item.getData();
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

            Composite buttonsPanel = UIUtils.createComposite(catPanel, 3);
            buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            new Label(buttonsPanel, SWT.NONE).setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));
            createCheckButtons(buttonsPanel, catalogTable);
        }

        {
            Composite tablesPanel = UIUtils.createComposite(sash, 1);
            tablesPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

            tablesTable = new Table(tablesPanel, SWT.BORDER | SWT.CHECK);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 50;
            tablesTable.setLayoutData(gd);
            tablesTable.addListener(SWT.Selection, event -> {
                if (event.detail == SWT.CHECK) {
                    updateCheckedTables();
                    updateState();
                }
            });
            Composite buttonsPanel = UIUtils.createComposite(tablesPanel, 3);
            buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            exportViewsCheck = UIUtils.createCheckbox(buttonsPanel, MySQLUIMessages.tools_db_export_wizard_page_settings_group_show_views, false);
            exportViewsCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    wizard.getSettings().setShowViews(exportViewsCheck.getSelection());
                    loadTables(null);
                }
            });
            exportViewsCheck.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));
            createCheckButtons(buttonsPanel, tablesTable);
        }

        loadSettings();
        setControl(composite);
    }

    @Override
    public void activatePage() {
        loadSettings();
    }

    @Override
    public void deactivatePage() {
        saveState();
    }

    private void loadSettings() {
        checkedObjects.clear();
        catalogTable.removeAll();

        boolean hasViews = false;
        MySQLDataSource dataSource = null;
        Set<MySQLCatalog> activeCatalogs = new LinkedHashSet<>();
        for (MySQLDatabaseExportInfo info : wizard.getSettings().getExportObjects()) {
            activeCatalogs.add(info.getDatabase());
            dataSource = info.getDatabase().getDataSource();
            if (!CommonUtils.isEmpty(info.getTables())) {
                Set<MySQLTableBase> tables = checkedObjects.computeIfAbsent(
                    info.getDatabase(), k -> new HashSet<>());
                for (MySQLTableBase table : info.getTables()) {
                    tables.add(table);
                    if (table.isView()) {
                        hasViews = true;
                    }
                }
            }
        }
        if (hasViews) {
            wizard.getSettings().setShowViews(true);
            exportViewsCheck.setSelection(true);
        }
        if (dataSource != null) {
            boolean tablesLoaded = false;
            for (MySQLCatalog catalog : dataSource.getCatalogs()) {
                if (catalog.getName().equalsIgnoreCase(MySQLConstants.INFO_SCHEMA_NAME)) {
                    // Dumping "information_schema" DB content is not supported
                    continue;
                }
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
        }
        updateState();
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
                    if (wizard.getSettings().isShowViews()) {
                        objects.addAll(curCatalog.getViews(monitor));
                    }
                    objects.sort(DBUtils.nameComparator());
                    UIUtils.syncExec(() -> {
                        tablesTable.removeAll();
                        for (MySQLTableBase table : objects) {
                            TableItem item = new TableItem(tablesTable, SWT.NONE);
                            item.setImage(DBeaverIcons.getImage(table.isView() ? DBIcon.TREE_VIEW : DBIcon.TREE_TABLE));
                            item.setText(0, table.getName());
                            item.setData(table);
                            item.setChecked(isCatalogChecked && (checkedObjects == null || checkedObjects.contains(table)));
                        }
                    });
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Table list", "Can't read table list", e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    public void saveState() {
        List<MySQLDatabaseExportInfo> objects = wizard.getSettings().getExportObjects();
        objects.clear();
        for (TableItem item : catalogTable.getItems()) {
            if (item.getChecked()) {
                MySQLCatalog catalog = (MySQLCatalog) item.getData();
                MySQLDatabaseExportInfo info = new MySQLDatabaseExportInfo(catalog, checkedObjects.get(catalog));
                objects.add(info);
            }
        }
    }

    @Override
    protected void updateState()
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

    @Override
    protected void updateTableCheckedStatus(@NotNull Table table, boolean check) {
        // Handle event from buttons "All" and "None"
        if (table == catalogTable) {
            for (TableItem tableItem : tablesTable.getItems()) {
                tableItem.setChecked(check);
            }
        }
        updateCheckedTables();
    }
}
