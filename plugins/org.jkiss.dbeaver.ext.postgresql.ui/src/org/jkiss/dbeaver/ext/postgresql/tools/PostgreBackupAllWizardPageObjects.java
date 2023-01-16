/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreDatabaseBackupAllInfo;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractNativeToolWizardPage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.List;

public class PostgreBackupAllWizardPageObjects extends AbstractNativeToolWizardPage<PostgreBackupAllWizard> {

    private Table databasesTable;

    private PostgreDataSource dataSource;
    private Set<PostgreDatabase> checkedObjects = new HashSet<>();

    protected PostgreBackupAllWizardPageObjects(PostgreBackupAllWizard wizard) {
        super(wizard, PostgreMessages.wizard_backup_all_page_global_backup_name);
        setTitle(PostgreMessages.wizard_backup_page_object_title);
        setDescription(PostgreMessages.wizard_backup_all_page_global_backup_tip);
    }

    @Override
    public void createControl(Composite parent) {

        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group objectsGroup = UIUtils.createControlGroup(
            composite,
            PostgreMessages.wizard_backup_page_object_group_object,
            1,
            GridData.FILL_HORIZONTAL,
            0);
        objectsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Composite catPanel = UIUtils.createComposite(objectsGroup, 1);
            catPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
            databasesTable = new Table(catPanel, SWT.BORDER | SWT.CHECK);
            databasesTable.addListener(SWT.Selection, event -> {
                TableItem item = (TableItem) event.item;
                if (event.detail == SWT.CHECK) {
                    databasesTable.select(databasesTable.indexOf(item));
                }
                updateState();
            });
            GridData gd = new GridData(GridData.FILL_BOTH);
            databasesTable.setLayoutData(gd);

            Composite buttonsPanel = UIUtils.createComposite(catPanel, 3);
            buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            new Label(buttonsPanel, SWT.NONE).setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));
            createCheckButtons(buttonsPanel, databasesTable);
        }

        setControl(composite);
    }

    @Override
    protected boolean determinePageCompletion() {
        boolean complete = false;
        if (!checkedObjects.isEmpty()) {
            complete = true;
        }
        for (TableItem item : databasesTable.getItems()) {
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
        checkedObjects.clear();
        databasesTable.removeAll();

        Set<PostgreDatabase> activeDatabases = new HashSet<>();
        for (PostgreDatabaseBackupAllInfo info : wizard.getSettings().getExportObjects()) {
            dataSource = info.getDataSource();
            List<PostgreDatabase> databases = info.getDatabases();
            if (!CommonUtils.isEmpty(databases)) {
                activeDatabases.addAll(databases);
            }
        }
        if (dataSource != null) {
            // Database list depends on connection setting
            for (PostgreDatabase database : dataSource.getDatabases()) {
                TableItem item = new TableItem(databasesTable, SWT.NONE);
                item.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
                item.setText(0, database.getName());
                item.setData(database);
                if (activeDatabases.contains(database)) {
                    item.setChecked(true);
                    databasesTable.select(databasesTable.indexOf(item));
                }
            }
        }
    }

    @Override
    public void saveState() {
        super.saveState();
        List<PostgreDatabaseBackupAllInfo> objects = wizard.getSettings().getExportObjects();
        objects.clear();
        List<PostgreDatabase> databases = new ArrayList<>();
        for (TableItem item : databasesTable.getItems()) {
            if (item.getChecked()) {
                PostgreDatabase database = (PostgreDatabase) item.getData();
                if (!databases.contains(database)) {
                    databases.add(database);
                }
            }
        }
        PostgreDatabaseBackupAllInfo info = new PostgreDatabaseBackupAllInfo(dataSource, databases);
        objects.add(info);
    }

    @Override
    protected void updateState() {
        updatePageCompletion();
        getContainer().updateButtons();
    }

    @Override
    protected void updateTableCheckedStatus(@NotNull Table table, boolean check) {
        // Handle event from buttons "All" and "None"
        if (table == databasesTable) {
            TableItem[] items = databasesTable.getItems();
            if (items.length != 0) {
                for (TableItem tableItem : items) {
                    tableItem.setChecked(check);
                    Object data = tableItem.getData();
                    if (data instanceof PostgreDatabase) {
                        if (check) {
                            checkedObjects.add((PostgreDatabase) data);
                        } else {
                            checkedObjects.remove((PostgreDatabase) data);
                        }
                    }
                }
            }
        }
    }
}
