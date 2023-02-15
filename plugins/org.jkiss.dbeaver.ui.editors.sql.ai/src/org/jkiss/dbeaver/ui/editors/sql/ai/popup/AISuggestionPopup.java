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
package org.jkiss.dbeaver.ui.editors.sql.ai.popup;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.jkiss.dbeaver.model.ai.translator.DAIHistoryItem;
import org.jkiss.dbeaver.model.ai.translator.DAIHistoryManager;
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
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.sql.ai.gpt3.GPTPreferencePage;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.*;

public class AISuggestionPopup extends AbstractPopupPanel {

    private static final Log log = Log.getLog(AISuggestionPopup.class);

    @NotNull
    private final DAIHistoryManager historyManager;

    @NotNull
    private final DBSLogicalDataSource dataSource;
    @NotNull
    private final DBCExecutionContext executionContext;
    private final DAICompletionSettings settings;

    private Text inputField;
    private String inputText;

    private DAICompletionScope currentScope = DAICompletionScope.CURRENT_SCHEMA;
    private Text scopeText;

    private ToolItem scopeConfigItem;

    private final Set<String> checkedObjectIds = new LinkedHashSet<>();

    public AISuggestionPopup(
        @NotNull Shell parentShell,
        @NotNull String title,
        @NotNull DAIHistoryManager historyManager,
        @NotNull DBSLogicalDataSource dataSource,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DAICompletionSettings settings) {
        super(parentShell, title);
        this.historyManager = historyManager;
        this.dataSource = dataSource;
        this.executionContext = executionContext;
        this.settings = settings;
        setImage(DBIcon.AI);
        setModeless(true);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite placeholder = super.createDialogArea(parent);

        Composite hintPanel = UIUtils.createComposite(placeholder, 2);
        hintPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Link hintLabel = new Link(hintPanel, SWT.NONE);
        hintLabel.setText("Enter a text in a human language, it will be translated into SQL (<a>instructions</a>)");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        hintLabel.setLayoutData(gd);
        hintLabel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                UIUtils.openWebBrowser(HelpUtils.getHelpExternalReference("AI-Smart-Assistance"));
            }
        });

        Composite scopePanel = UIUtils.createComposite(placeholder, 5);
        scopePanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.createControlLabel(scopePanel, "Scope");

        currentScope = settings.getScope();
        if (settings.getCustomObjectIds() != null) {
            checkedObjectIds.addAll(Arrays.asList(settings.getCustomObjectIds()));
        }

        Combo scopeCombo = new Combo(scopePanel, SWT.DROP_DOWN | SWT.READ_ONLY);
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

        scopeText = new Text(scopePanel, SWT.READ_ONLY | SWT.BORDER);
        scopeText.setEditable(false);
        //scopeText.setEnabled(false);
        scopeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        {
            ToolBar tb = new ToolBar(scopePanel, SWT.FLAT);
            scopeConfigItem = UIUtils.createToolItem(tb, "Customize", UIIcon.RS_DETAILS,
                SelectionListener.widgetSelectedAdapter(
                    selectionEvent -> showScopeConfiguration()));
            UIUtils.createToolItem(tb, "Settings", UIIcon.CONFIGURATION,
                SelectionListener.widgetSelectedAdapter(
                    selectionEvent -> UIUtils.showPreferencesFor(getShell(), null, GPTPreferencePage.PAGE_ID)));
            tb.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        }


        inputField = new Text(placeholder, SWT.BORDER | SWT.MULTI);
        //inputField.setLayoutData(new GridData(GridData.FILL_BOTH));
        gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = UIUtils.getFontHeight(placeholder.getFont()) * 10;
        gd.widthHint = UIUtils.getFontHeight(placeholder.getFont()) * 40;
        inputField.setLayoutData(gd);
        inputField.setTextLimit(10000);

        inputField.addModifyListener(e -> inputText = inputField.getText());
        inputField.addListener(SWT.KeyDown, event -> {
            if (event.keyCode == SWT.CR && event.stateMask == 0) {
                event.doit = false;
                okPressed();
            }
        });

        Composite miscPanel = UIUtils.createComposite(placeholder, 2);
        miscPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createControlLabel(miscPanel, "History");
        Combo historyCombo = new Combo(miscPanel, SWT.DROP_DOWN | SWT.READ_ONLY);
        historyCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button applyButton = UIUtils.createDialogButton(placeholder, "Translate",
            SelectionListener.widgetSelectedAdapter(selectionEvent -> okPressed()));
        ((GridData)applyButton.getLayoutData()).grabExcessHorizontalSpace = false;
        ((GridData)applyButton.getLayoutData()).horizontalAlignment = GridData.END;

        closeOnFocusLost(inputField, scopeCombo, scopeText, historyCombo, applyButton);

        historyCombo.setEnabled(false);
        AbstractJob completionJob = new AbstractJob("Read completion history") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    List<DAIHistoryItem> queries = historyManager.readTranslationHistory(monitor, dataSource, executionContext, 100);
                    UIUtils.syncExec(() -> {
                        if (!CommonUtils.isEmpty(queries)) {
                            for (DAIHistoryItem query : queries) {
                                historyCombo.add(query.getNaturalText());
                            }
                            historyCombo.select(0);
                            inputField.setText(queries.get(0).getNaturalText());
                            inputField.selectAll();
                            historyCombo.setEnabled(true);
                        } else {
                            historyCombo.setEnabled(false);
                        }
                    });
                } catch (DBException e) {
                    log.error("Error reading completion history", e);
                }
                return Status.OK_STATUS;
            }
        };
        completionJob.schedule();

        inputField.setFocus();

        historyCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String text = historyCombo.getText();
                if (!CommonUtils.isEmpty(text)) {
                    inputField.setText(text);
                }
            }
        });

        showScopeSettings(currentScope);

        return placeholder;
    }

    private void showScopeConfiguration() {
        ScopeConfigDialog dialog = new ScopeConfigDialog(checkedObjectIds);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        checkedObjectIds.clear();
        checkedObjectIds.addAll(dialog.checkedObjectIds);
        showScopeSettings(DAICompletionScope.CUSTOM);
    }

    private void showScopeSettings(DAICompletionScope scope) {
        String text;
        switch (scope) {
            case CURRENT_SCHEMA:
                text = dataSource.getCurrentSchema();
                if (CommonUtils.isEmpty(text)) {
                    text = dataSource.getCurrentCatalog();
                }
                if (CommonUtils.isEmpty(text)) {
                    text = dataSource.getDataSourceContainer().getName();
                }
                break;
            case CURRENT_DATABASE:
                text = dataSource.getCurrentCatalog();
                if (CommonUtils.isEmpty(text)) {
                    text = dataSource.getDataSourceContainer().getName();
                }
                break;
            case CURRENT_DATASOURCE:
                text = dataSource.getDataSourceContainer().getName();
                break;
            default:
                text = "" + checkedObjectIds.size() + " object(s)";
                break;
        }
        scopeConfigItem.setEnabled(scope == DAICompletionScope.CUSTOM);
        scopeText.setText(CommonUtils.toString(text, "N/A"));
    }

    protected void createButtonsForButtonBar(Composite parent) {
        // No buttons
    }

    @Override
    protected boolean isModeless() {
        return true;
    }

    public String getInputText() {
        return inputText;
    }

    public DAICompletionScope getScope() {
        return currentScope;
    }

    public List<DBSEntity> getCustomEntities() {
        List<DBSEntity> entities = new ArrayList<>();
        try {
            DBPDataSource dataSource = executionContext.getDataSource();
            if (dataSource instanceof DBSObjectContainer) {
                loadCheckedEntitiesById((DBSObjectContainer)dataSource, entities);
            }
        } catch (Exception e) {
            log.error(e);
        }
        return entities;
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

    @Override
    protected void okPressed() {
        inputText = inputField.getText().trim();

        settings.setScope(currentScope);
        settings.setCustomObjectIds(checkedObjectIds.toArray(new String[0]));
        settings.saveSettings();

        super.okPressed();
    }

    private class ScopeConfigDialog extends BaseDialog {

        private Tree objectTree;
        private final Set<String> checkedObjectIds;

        public ScopeConfigDialog(Set<String> checkedIds) {
            super(AISuggestionPopup.this.getShell(), "Customize scope", DBIcon.AI);
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
                            loadContainer(monitor, objectTree, null, (DBSObjectContainer)ds, checkedObjectIds);
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
        )  throws DBException {
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
                        new TreeItem(objectTree, SWT.NONE) :  new TreeItem(parentItem, SWT.NONE);
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
