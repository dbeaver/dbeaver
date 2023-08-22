/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.dm.tools;

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
import org.jkiss.dbeaver.ext.dm.model.DmDataSource;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.ext.dm.model.DmTableBase;
import org.jkiss.dbeaver.ext.dm.tasks.DmSchemaExportInfo;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.List;

/**
 * 该类为选择要导出的数据库
 * @author saorionesan
 *
 */
class DmExportWizardPageObjects extends DmWizardPageSettings<DmExportWizard>
{
	
	public static final Log log= Log.getLog(DmExportWizardPageObjects.class);

    private Table catalogTable;
    private Table tablesTable;
    private Map<DmSchema, Set<DmTableBase>> checkedObjects = new HashMap<>();

    private DmSchema curCatalog;
    private Button exportViewsCheck;

    protected DmExportWizardPageObjects(DmExportWizard wizard)
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

        Group objectsGroup = UIUtils.createControlGroup(composite, "对象", 1, GridData.FILL_HORIZONTAL, 0);
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
                    DmSchema catalog = (DmSchema) item.getData();
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

            exportViewsCheck = UIUtils.createCheckbox(buttonsPanel, "Show views", false);
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

    private void loadSettings() {
        checkedObjects.clear();
        catalogTable.removeAll();

        boolean hasViews = false;
        DmDataSource dataSource = null;
        Set<DmSchema> activeCatalogs = new LinkedHashSet<>();
        for (DmSchemaExportInfo info : wizard.getSettings().getExportObjects()) {
            activeCatalogs.add(info.getDatabase());
            dataSource = info.getDatabase().getDataSource();
            if (!CommonUtils.isEmpty(info.getTables())) {
                Set<DmTableBase> tables = checkedObjects.computeIfAbsent(
                    info.getDatabase(), k -> new HashSet<>());
                for (DmTableBase table : info.getTables()) {
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
        try {
            if (dataSource != null) {
                boolean tablesLoaded = false;
                for (DmSchema catalog : dataSource.getSchemas(new VoidProgressMonitor())) {
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
		} catch (Exception e) {
			// TODO: handle exception
			log.error("获取模式失败");
		}
        updateState();
    }

    private void updateCheckedTables() {
        Set<DmTableBase> checkedTables = new HashSet<>();
        TableItem[] tableItems = tablesTable.getItems();
        for (TableItem item : tableItems) {
            if (item.getChecked()) {
                checkedTables.add((DmTableBase) item.getData());
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

    private boolean isChecked(DmSchema catalog) {
        for (TableItem item : catalogTable.getItems()) {
            if (item.getData() == catalog) {
                return item.getChecked();
            }
        }
        return false;
    }

    private void loadTables(final DmSchema catalog) {
        if (catalog != null) {
            curCatalog = catalog;
        }
        if (curCatalog == null) {
            return;
        }
        final boolean isCatalogChecked = isChecked(curCatalog);
        final Set<DmTableBase> checkedObjects = this.checkedObjects.get(curCatalog);
        new AbstractJob("Load '" + curCatalog.getName() + "' tables") {
            {
                setUser(true);
            }
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    final List<DmTableBase> objects = new ArrayList<>();
                    objects.addAll(curCatalog.getTables(monitor));
                    if (wizard.getSettings().isShowViews()) {
                        objects.addAll(curCatalog.getViews(monitor));
                    }
                    objects.sort(DBUtils.nameComparator());
                    UIUtils.syncExec(() -> {
                        tablesTable.removeAll();
                        for (DmTableBase table : objects) {
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
        List<DmSchemaExportInfo> objects = wizard.getSettings().getExportObjects();
        objects.clear();
        for (TableItem item : catalogTable.getItems()) {
            if (item.getChecked()) {
                DmSchema catalog = (DmSchema) item.getData();
                DmSchemaExportInfo info = new DmSchemaExportInfo(catalog, checkedObjects.get(catalog));
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

}
