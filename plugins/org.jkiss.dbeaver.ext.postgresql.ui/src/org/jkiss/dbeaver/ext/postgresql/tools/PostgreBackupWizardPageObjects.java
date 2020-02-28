/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableContainer;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreDatabaseBackupInfo;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractToolWizardPage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.*;


class PostgreBackupWizardPageObjects extends AbstractToolWizardPage<PostgreBackupWizard>
{
    private static final Log log = Log.getLog(PostgreBackupWizardPageObjects.class);

    private Table schemasTable;
    private Table tablesTable;
    private Map<PostgreSchema, Set<PostgreTableBase>> checkedObjects = new HashMap<>();

    private PostgreSchema curSchema;
    private PostgreDatabase dataBase;
    private Button exportViewsCheck;

    PostgreBackupWizardPageObjects(PostgreBackupWizard wizard)
    {
        super(wizard, PostgreMessages.wizard_backup_page_object_title_schema_table);
        setTitle(PostgreMessages.wizard_backup_page_object_title);
        setDescription(PostgreMessages.wizard_backup_page_object_description);
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

        Group objectsGroup = UIUtils.createControlGroup(composite, PostgreMessages.wizard_backup_page_object_group_object, 1, GridData.FILL_HORIZONTAL, 0);
        objectsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        SashForm sash = new CustomSashForm(objectsGroup, SWT.VERTICAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Composite catPanel = UIUtils.createComposite(sash, 1);
            catPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
            schemasTable = new Table(catPanel, SWT.BORDER | SWT.CHECK);
            schemasTable.addListener(SWT.Selection, event -> {
                TableItem item = (TableItem) event.item;
                PostgreSchema catalog = (PostgreSchema) item.getData();
                if (event.detail == SWT.CHECK) {
                    schemasTable.select(schemasTable.indexOf(item));
                    checkedObjects.remove(catalog);
                }
                loadTables(catalog);
                updateState();
            });
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 50;
            schemasTable.setLayoutData(gd);

            Composite buttonsPanel = UIUtils.createComposite(catPanel, 3);
            buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            new Label(buttonsPanel, SWT.NONE).setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));
            createCheckButtons(buttonsPanel, schemasTable);
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
            exportViewsCheck = UIUtils.createCheckbox(buttonsPanel, PostgreMessages.wizard_backup_page_object_checkbox_show_view, false);
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

        setControl(composite);
    }

    @Override
    public void activatePage() {
        super.activatePage();
        loadSettings();

        updateState();
    }

    @Override
    public void deactivatePage() {
        saveState();
    }

    private void loadSettings() {
        checkedObjects.clear();
        schemasTable.removeAll();
        tablesTable.removeAll();

        dataBase = null;
        boolean hasViews = false;
        Set<PostgreSchema> activeSchemas = new LinkedHashSet<>();
        for (PostgreDatabaseBackupInfo info : wizard.getSettings().getExportObjects()) {
            dataBase = info.getDatabase();

            if (!CommonUtils.isEmpty(info.getSchemas())) {
                activeSchemas.addAll(info.getSchemas());
            }
            if (!CommonUtils.isEmpty(info.getTables())) {
                for (PostgreTableBase table : info.getTables()) {
                    PostgreTableContainer tableContainer = table.getContainer();
                    if (!(tableContainer instanceof PostgreSchema)) {
                        continue;
                    }
                    PostgreSchema schema = (PostgreSchema) tableContainer;
                    activeSchemas.add(schema);
                    Set<PostgreTableBase> tables = checkedObjects.computeIfAbsent(schema, k -> new HashSet<>());
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
        if (dataBase != null) {
            boolean tablesLoaded = false;
            try {
                for (PostgreSchema schema : dataBase.getSchemas(new VoidProgressMonitor())) {
                    if (schema.isSystem() || schema.isUtility()) {
                        continue;
                    }
                    TableItem item = new TableItem(schemasTable, SWT.NONE);
                    item.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
                    item.setText(0, schema.getName());
                    item.setData(schema);
                    if (activeSchemas.contains(schema)) {
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
                    for (JDBCTable table : curSchema.getTables(monitor)) {
                        if (table instanceof PostgreTableBase) {
                            objects.add((PostgreTableBase) table);
                        }
                    }
                    if (wizard.getSettings().isShowViews()) {
                        objects.addAll(curSchema.getViews(monitor));
                    }
                    objects.sort(DBUtils.nameComparator());
                    UIUtils.syncExec(() -> {
                        tablesTable.removeAll();
                        for (PostgreTableBase table : objects) {
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
        super.saveState();

        List<PostgreDatabaseBackupInfo> objects = wizard.getSettings().getExportObjects();
        objects.clear();
        List<PostgreSchema> schemas = new ArrayList<>();
        List<PostgreTableBase> tables = new ArrayList<>();
        for (TableItem item : schemasTable.getItems()) {
            if (item.getChecked()) {
                PostgreSchema schema = (PostgreSchema) item.getData();
                Set<PostgreTableBase> checkedTables = checkedObjects.get(schema);
                // All tables checked
                if (!schemas.contains(schema)) {
                    schemas.add(schema);
                }
                if (checkedTables != null) {
                    // Only a few tables checked
                    tables.addAll(checkedTables);
                }
            }
        }
        PostgreDatabaseBackupInfo info = new PostgreDatabaseBackupInfo(dataBase, schemas, tables);
        objects.add(info);
    }

    @Override
    protected void updateState()
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
