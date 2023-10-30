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
package org.jkiss.dbeaver.ui.editors.sql.ai.controls;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionScope;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionSettings;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSStructContainer;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.*;

public class ScopeSelectorControl extends Composite {
    private static final Log log = Log.getLog(ScopeSelectorControl.class);

    private final DBSLogicalDataSource dataSource;
    private final DBCExecutionContext executionContext;

    private final Combo scopeCombo;
    private final Text scopeText;
    private final ToolItem scopeConfigItem;
    private final ToolBar toolBar;

    private final Set<String> checkedObjectIds;
    private DAICompletionScope currentScope;

    public ScopeSelectorControl(
        @NotNull Composite parent,
        @NotNull DBSLogicalDataSource dataSource,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DAICompletionSettings settings
    ) {
        super(parent, SWT.NONE);

        setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(5).create());

        this.dataSource = dataSource;
        this.executionContext = executionContext;
        this.currentScope = settings.getScope();
        this.checkedObjectIds = new HashSet<>();

        if (!ArrayUtils.isEmpty(settings.getCustomObjectIds())) {
            checkedObjectIds.addAll(Arrays.asList(settings.getCustomObjectIds()));
        }

        scopeCombo = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (DAICompletionScope scope : DAICompletionScope.values()) {
            scopeCombo.add(scope.getTitle());
            if (currentScope == scope) {
                scopeCombo.select(scopeCombo.getItemCount() - 1);
            }
        }
        scopeCombo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                currentScope = CommonUtils.fromOrdinal(DAICompletionScope.class, scopeCombo.getSelectionIndex());
                showScopeSettings(currentScope);
                if (currentScope == DAICompletionScope.CUSTOM) {
                    showScopeConfiguration();
                }
            }
        });

        scopeText = new Text(this, SWT.READ_ONLY | SWT.BORDER);
        scopeText.setEditable(false);
        scopeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        toolBar = new ToolBar(this, SWT.FLAT);
        toolBar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        scopeConfigItem = UIUtils.createToolItem(
            toolBar,
            "Customize",
            UIIcon.RS_DETAILS,
            SelectionListener.widgetSelectedAdapter(e -> showScopeConfiguration())
        );

        showScopeSettings(currentScope);
    }

    @NotNull
    public ToolBar getToolBar() {
        return toolBar;
    }

    @NotNull
    public Combo getScopeCombo() {
        return scopeCombo;
    }

    @NotNull
    public Text getScopeText() {
        return scopeText;
    }

    @NotNull
    public Set<String> getCheckedObjectIds() {
        return checkedObjectIds;
    }

    @NotNull
    public DAICompletionScope getScope() {
        return currentScope;
    }

    @NotNull
    public List<DBSEntity> getCustomEntities() {
        List<DBSEntity> entities = new ArrayList<>();
        try {
            DBPDataSource dataSource = executionContext.getDataSource();
            if (dataSource instanceof DBSObjectContainer) {
                loadCheckedEntitiesById((DBSObjectContainer) dataSource, entities);
            }
        } catch (Exception e) {
            log.error(e);
        }
        return entities;
    }

    private void showScopeSettings(@NotNull DAICompletionScope scope) {
        final String text = switch (scope) {
            case CURRENT_SCHEMA -> {
                if (CommonUtils.isNotEmpty(dataSource.getCurrentSchema())) {
                    yield dataSource.getCurrentSchema();
                } else if (CommonUtils.isNotEmpty(dataSource.getCurrentCatalog())) {
                    yield dataSource.getCurrentCatalog();
                } else {
                    yield dataSource.getDataSourceContainer().getName();
                }
            }
            case CURRENT_DATABASE -> {
                if (CommonUtils.isNotEmpty(dataSource.getCurrentCatalog())) {
                    yield dataSource.getCurrentCatalog();
                } else {
                    yield dataSource.getDataSourceContainer().getName();
                }
            }
            case CURRENT_DATASOURCE -> dataSource.getDataSourceContainer().getName();
            default -> checkedObjectIds.size() + " object(s)";
        };

        scopeConfigItem.setEnabled(scope == DAICompletionScope.CUSTOM);
        scopeText.setText(CommonUtils.toString(text, "N/A"));
    }

    private void loadCheckedEntitiesById(DBSObjectContainer container, List<DBSEntity> entities) throws DBException {
        Collection<? extends DBSObject> children = container.getChildren(new LoggingProgressMonitor(log));

        if (children != null) {
            for (DBSObject child : children) {
                if (child instanceof DBSEntity) {
                    if (checkedObjectIds.contains(DBUtils.getObjectFullId(child))) {
                        entities.add((DBSEntity) child);
                    }
                } else if (child instanceof DBSStructContainer) {
                    loadCheckedEntitiesById((DBSObjectContainer) child, entities);
                }
            }
        }
    }

    private void showScopeConfiguration() {
        ScopeConfigDialog dialog = new ScopeConfigDialog(getShell(), checkedObjectIds);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        checkedObjectIds.clear();
        checkedObjectIds.addAll(dialog.checkedObjectIds);
        showScopeSettings(DAICompletionScope.CUSTOM);
    }

    private class ScopeConfigDialog extends BaseDialog {
        private Tree objectTree;
        private final Set<String> checkedObjectIds;

        public ScopeConfigDialog(@NotNull Shell shell, Set<String> checkedIds) {
            super(shell, "Customize scope", DBIcon.AI);
            this.checkedObjectIds = new LinkedHashSet<>(checkedIds);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite composite = super.createDialogArea(parent);

            objectTree = new Tree(composite, SWT.CHECK);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 400;
            gd.heightHint = 300;
            objectTree.setLayoutData(gd);
            objectTree.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (e.detail != SWT.CHECK) {
                        return;
                    }
                    TreeItem item = (TreeItem) e.item;
                    if (item.getData() instanceof DBSStructContainer) {
                        checkTreeItems(item.getItems(), item.getChecked());
                    }
                }

                private void checkTreeItems(TreeItem[] items, boolean check) {
                    for (TreeItem child : items) {
                        child.setChecked(check);
                        if (child.getData() instanceof DBSEntity) {

                        } else {
                            TreeItem[] children = child.getItems();
                            if (!ArrayUtils.isEmpty(children)) {
                                checkTreeItems(children, check);
                            }
                        }
                    }
                }
            });

            loadObjects(objectTree, executionContext.getDataSource());

            return composite;
        }

        private void loadObjects(Tree objectTree, DBPDataSource ds) {
            new AbstractJob("Load database structure") {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    if (ds instanceof DBSObjectContainer) {
                        try {
                            loadContainer(monitor, objectTree, null, (DBSObjectContainer) ds, checkedObjectIds);
                        } catch (Exception e) {
                            return GeneralUtils.makeExceptionStatus(e);
                        }
                        UIUtils.syncExec(() -> {
                            for (TreeItem item : objectTree.getItems()) {
                                item.setExpanded(true);
                            }
                        });
                    }
                    return Status.OK_STATUS;
                }
            }.schedule();
        }

        private void loadContainer(
            DBRProgressMonitor monitor,
            Tree objectTree,
            TreeItem parentItem,
            DBSObjectContainer objectContainer,
            Set<String> checkedObjectIds
        ) throws DBException {
            Collection<? extends DBSObject> children;
            try {
                children = objectContainer.getChildren(monitor);
            } catch (Exception e) {
                log.debug("Error loading container '" + objectContainer.getName() + "' contents: " + e.getMessage());
                return;
            }
            if (children == null) {
                return;
            }
            Map<TreeItem, DBSObjectContainer> addedContainers = new LinkedHashMap<>();
            UIUtils.syncExec(() -> {
                for (DBSObject child : children) {
                    if (monitor.isCanceled()) {
                        return;
                    }
                    if (!(child instanceof DBSStructContainer) && !(child instanceof DBSEntity)) {
                        continue;
                    }
                    DBNDatabaseNode node = DBNUtils.getNodeByObject(monitor, child, false);
                    if (node == null) {
                        continue;
                    }
                    TreeItem item = parentItem == null ?
                        new TreeItem(objectTree, SWT.NONE) : new TreeItem(parentItem, SWT.NONE);
                    item.setData(child);
                    item.setImage(DBeaverIcons.getImage(node.getNodeIconDefault()));
                    item.setText(node.getNodeName());
                    String objectId = DBUtils.getObjectFullId(child);
                    if (checkedObjectIds.contains(objectId)) {
                        item.setChecked(true);
                        if (parentItem != null && !parentItem.getExpanded()) {
                            parentItem.setExpanded(true);
                        }
                    }
                    if (child instanceof DBSObjectContainer) {
                        addedContainers.put(item, (DBSObjectContainer) child);
                    }
                }
            });
            if (monitor.isCanceled()) {
                return;
            }
            for (Map.Entry<TreeItem, DBSObjectContainer> contItem : addedContainers.entrySet()) {
                DBSObjectContainer object = contItem.getValue();
                loadContainer(monitor, objectTree, contItem.getKey(), object, checkedObjectIds);
            }
        }

        @Override
        protected void okPressed() {
            checkedObjectIds.clear();
            collectCheckedObjects(objectTree.getItems());

            super.okPressed();
        }

        private void collectCheckedObjects(TreeItem[] items) {
            for (TreeItem item : items) {
                if (item.getChecked()) {
                    if (item.getData() instanceof DBSEntity) {
                        checkedObjectIds.add(DBUtils.getObjectFullId((DBSEntity) item.getData()));
                    }
                }
                TreeItem[] children = item.getItems();
                if (!ArrayUtils.isEmpty(children)) {
                    collectCheckedObjects(children);
                }
            }
        }
    }
}
