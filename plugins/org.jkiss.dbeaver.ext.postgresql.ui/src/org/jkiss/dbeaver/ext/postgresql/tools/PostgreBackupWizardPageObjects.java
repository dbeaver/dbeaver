/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.PostgreUIUtils;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableContainer;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreDatabaseBackupInfo;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreDatabaseBackupSelection;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractNativeToolWizardPage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;
import org.jkiss.utils.CommonUtils;

import java.util.*;


class PostgreBackupWizardPageObjects extends AbstractNativeToolWizardPage<PostgreBackupWizard> {
    private static final Log log = Log.getLog(PostgreBackupWizardPageObjects.class);

    private Table schemasTable;
    private Table tablesTable;
    private Composite tableButtonsPanel;
    private final PostgreDatabaseBackupSelection selection = new PostgreDatabaseBackupSelection();
    private int tableLoadId;

    private PostgreSchema curSchema;
    private PostgreDatabase dataBase;
    private Button exportViewsCheck;
    private Button fullSchemaBackupCheck;

    PostgreBackupWizardPageObjects(@NotNull PostgreBackupWizard wizard) {
        super(wizard, PostgreMessages.wizard_backup_page_object_title_schema_table);
        setTitle(PostgreMessages.wizard_backup_page_object_title);
        setDescription(PostgreMessages.wizard_backup_page_object_description);
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Composite objectsGroup = UIUtils.createTitledComposite(
            composite,
            PostgreMessages.wizard_backup_page_object_group_object,
            1,
            GridData.FILL_BOTH
        );

        connInfo = new CLabel(objectsGroup, SWT.WRAP);
        connInfo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        connInfo.setImage(DBeaverIcons.getImage(DBIcon.DATABASE_DEFAULT));

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
                    selection.selectSchema(catalog, item.getChecked());
                    updateState();
                }
                if (catalog == curSchema && tablesTable.getItemCount() > 0) {
                    updateTableChecks();
                } else {
                    loadTables(catalog);
                }
            });
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 50;
            schemasTable.setLayoutData(gd);

            Composite buttonsPanel = UIUtils.createComposite(catPanel, 3);

            fullSchemaBackupCheck = UIUtils.createCheckbox(
                buttonsPanel, PostgreMessages.wizard_backup_page_object_checkbox_complete_backup, false);
            fullSchemaBackupCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                if (fullSchemaBackupCheck.getSelection()) {
                    updateTableCheckedStatus(schemasTable, true);
                    updateSchemaChecks();
                }
                updateState();
            }));
            fullSchemaBackupCheck.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));
            buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
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

            tableButtonsPanel = UIUtils.createComposite(tablesPanel, 3);
            tableButtonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            exportViewsCheck = UIUtils.createCheckbox(
                tableButtonsPanel, PostgreMessages.wizard_backup_page_object_checkbox_show_view, false);
            exportViewsCheck.setSelection(wizard.getSettings().isShowViews());
            exportViewsCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                wizard.getSettings().setShowViews(exportViewsCheck.getSelection());
                loadTables(null);
            }));
            exportViewsCheck.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));
            createCheckButtons(tableButtonsPanel, tablesTable);
        }
        {
            PostgreUIUtils.addCompatibilityInfoLabelForForks(composite, wizard, dataBase != null ? dataBase.getDataSource() : null);
        }

        setControl(composite);
    }

    @Override
    protected boolean determinePageCompletion() {
        boolean complete = false;
        for (TableItem item : schemasTable.getItems()) {
            if (item.getChecked()) {
                complete = true;
                break;
            }
        }

        return complete && super.determinePageCompletion();
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
        selection.clear();
        tableLoadId++;
        curSchema = null;
        schemasTable.removeAll();
        tablesTable.removeAll();
        UIUtils.enableWithChildren(tableButtonsPanel, false);

        dataBase = null;
        boolean hasViews = false;
        Set<PostgreSchema> activeSchemas = new LinkedHashSet<>();
        Map<PostgreSchema, Set<PostgreTableBase>> checkedObjects = new HashMap<>();
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
        }
        exportViewsCheck.setSelection(wizard.getSettings().isShowViews());
        if (dataBase != null) {
            setConnectionInfo(dataBase.getDataSource().getContainer(), dataBase.getName());

            boolean tablesLoaded = false;
            try {
                for (PostgreSchema schema : dataBase.getSchemas(new VoidProgressMonitor())) {
                    if (schema.isSystem() || schema.isUtility()) {
                        continue;
                    }
                    TableItem item = new TableItem(schemasTable, SWT.NONE);
                    item.setImage(DBeaverIcons.getImage(DBIcon.TREE_SCHEMA));
                    item.setText(0, schema.getName());
                    item.setData(schema);
                    if (activeSchemas.contains(schema)) {
                        item.setChecked(true);
                        Set<PostgreTableBase> tables = checkedObjects.get(schema);
                        if (tables == null) {
                            selection.selectSchema(schema, true);
                        } else {
                            selection.selectTables(schema, tables, false);
                        }
                        if (!tablesLoaded) {
                            schemasTable.select(schemasTable.indexOf(item));
                            loadTables(schema);
                            tablesLoaded = true;
                        }
                    }
                }
                if (!tablesLoaded && schemasTable.getItemCount() > 0) {
                    schemasTable.select(0);
                    loadTables((PostgreSchema) schemasTable.getItem(0).getData());
                }
            } catch (DBException e) {
                log.error(e);
            }
        }
    }

    private void updateCheckedTables() {
        Set<PostgreTableBase> checkedTables = new HashSet<>();
        TableItem[] tableItems = tablesTable.getItems();
        if (curSchema == null || tableItems.length == 0) {
            return;
        }
        for (TableItem item : tableItems) {
            if (item.getChecked()) {
                checkedTables.add((PostgreTableBase) item.getData());
            }
        }
        selection.selectTables(curSchema, checkedTables, checkedTables.size() == tableItems.length);
        updateSchemaChecks();
    }

    private void updateSchemaChecks() {
        for (TableItem item : schemasTable.getItems()) {
            item.setChecked(selection.isSchemaSelected((PostgreSchema) item.getData()));
        }
    }

    private void updateTableChecks() {
        if (curSchema != null) {
            for (TableItem item : tablesTable.getItems()) {
                item.setChecked(selection.isTableSelected(curSchema, (PostgreTableBase) item.getData()));
            }
        }
    }

    private void loadTables(@Nullable PostgreSchema catalog) {
        if (catalog != null) {
            curSchema = catalog;
        }
        if (curSchema == null) {
            return;
        }
        final PostgreSchema schema = curSchema;
        final int loadId = ++tableLoadId;
        final boolean showViews = wizard.getSettings().isShowViews();
        final List<PostgreTableBase> objects = new ArrayList<>();
        tablesTable.removeAll();
        UIUtils.enableWithChildren(tableButtonsPanel, false);
        new AbstractJob("Load '" + schema.getName() + "' tables") {
            {
                setUser(true);
            }

            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                monitor.beginTask("Collect tables", 1);
                try {
                    monitor.subTask("Collect tables to dump");
                    for (JDBCTable table : schema.getTables(monitor)) {
                        if (table instanceof PostgreTableBase) {
                            objects.add((PostgreTableBase) table);
                        }
                    }
                    if (showViews) {
                        objects.addAll(schema.getViews(monitor));
                    }
                    objects.sort(DBUtils.nameComparator());
                    UIUtils.syncExec(() -> {
                        if (tablesTable.isDisposed() || loadId != tableLoadId) {
                            return;
                        }
                        tablesTable.removeAll();
                        for (PostgreTableBase table : objects) {
                            TableItem item = new TableItem(tablesTable, SWT.NONE);
                            item.setImage(DBeaverIcons.getImage(table.isView() ? DBIcon.TREE_VIEW : DBIcon.TREE_TABLE));
                            item.setText(0, table.getName());
                            item.setData(table);
                            item.setChecked(selection.isTableSelected(schema, table));
                        }
                    });
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Table list", "Can't read table list", e);
                } finally {
                    monitor.done();
                    UIUtils.syncExec(() -> {
                        if (!tablesTable.isDisposed() && loadId == tableLoadId) {
                            UIUtils.enableWithChildren(tableButtonsPanel, true);
                        }
                    });
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    public void saveState() {
        super.saveState();

        List<PostgreDatabaseBackupInfo> objects = wizard.getSettings().getExportObjects();
        objects.clear();
        if (dataBase != null) {
            objects.addAll(selection.getExportObjects(dataBase, wizard.getSettings().isFullSchemaBackup()));
        }
    }

    @NotNull
    private List<PostgreSchema> getSchemas() {
        return Arrays.stream(schemasTable.getItems()).map(item -> (PostgreSchema) item.getData()).toList();
    }

    private void setFullSchemaBackup(boolean completeBackup) {
        fullSchemaBackupCheck.setEnabled(schemasTable.getItemCount() > 0);
        fullSchemaBackupCheck.setSelection(completeBackup);
        wizard.getSettings().setFullSchemaBackup(completeBackup);
    }

    @Override
    protected void updateState() {
        // a full selection must include database-level objects such as extensions, not just -n for every schema
        setFullSchemaBackup(selection.isCompleteBackup(getSchemas()));
        updatePageCompletion();
        getContainer().updateButtons();
    }

    @Override
    protected void updateTableCheckedStatus(@NotNull Table table, boolean check) {
        // Handle event from buttons "All" and "None"
        if (table == schemasTable) {
            selection.selectSchemas(getSchemas(), check);
            updateTableChecks();
            if (curSchema == null && schemasTable.getItemCount() > 0) {
                schemasTable.select(0);
                loadTables((PostgreSchema) schemasTable.getItem(0).getData());
            }
        } else {
            updateCheckedTables();
        }
    }

}
