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
package org.jkiss.dbeaver.ui.ai.controls;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIChatContextProvider;
import org.jkiss.dbeaver.model.ai.AITextUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;
import org.jkiss.dbeaver.ui.ai.internal.AIUIActivator;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseObjectSelectionTree;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class ScopeSelectorDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.AIScopeSelectorDialog.v2";//$NON-NLS-1$

    private final DBRRunnableContext runnableContext;
    private final DBPDataSourceContainer dataSourceContainer;
    private final AIChatContextProvider contextProvider;
    private final Set<String> savedObjectIds;
    private List<? extends DBNNode> initialNodes = List.of();
    private DatabaseObjectSelectionTree selectorTree;
    private Label scopeInfoLabel;
    private Composite treeArea;
    private Label loadingLabel;
    private ProgressBar loadingBar;
    @Nullable
    private AbstractJob loadJob;
    private List<? extends DBNNode> selectedNodes;

    public ScopeSelectorDialog(
        @NotNull Shell parentShell,
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBPDataSourceContainer container,
        @NotNull AIChatContextProvider contextProvider,
        @NotNull Set<String> savedObjectIds
    ) {
        super(parentShell, AIUIMessages.scope_selector_title, null);
        this.runnableContext = runnableContext;
        this.dataSourceContainer = container;
        this.contextProvider = contextProvider;
        this.savedObjectIds = Set.copyOf(savedObjectIds);
    }

    public ScopeSelectorDialog(
        @NotNull Shell parentShell,
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBPDataSourceContainer container,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<? extends DBNNode> selectedNodes
    ) {
        this(parentShell, runnableContext, container, ignored -> executionContext, Set.of());
        this.initialNodes = selectedNodes;
        this.selectedNodes = selectedNodes;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getSettingsSection(AIUIActivator.getInstance().getDialogSettings(), DIALOG_ID);
    }

    @NotNull
    @Override
    protected Point getInitialSize() {
        Point size = super.getInitialSize();
        size.x = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        return size;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);
        UIUtils.createInfoLabel(dialogArea, AIUIMessages.scope_selector_hint);

        treeArea = UIUtils.createComposite(dialogArea, 1);
        GridData treeData = new GridData(GridData.FILL_BOTH);
        treeData.widthHint = convertWidthInCharsToPixels(60);
        treeData.heightHint = 300;
        treeArea.setLayoutData(treeData);
        loadingLabel = UIUtils.createLabel(treeArea, UINavigatorMessages.ui_navigator_loading_text_loading);
        loadingBar = new ProgressBar(treeArea, SWT.INDETERMINATE);
        loadingBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        scopeInfoLabel = new Label(dialogArea, SWT.WRAP);
        scopeInfoLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        CSSUtils.applyStyles(scopeInfoLabel);
        return dialogArea;
    }

    private void updateScopeInfo() {
        int catalogs = 0;
        int schemas = 0;
        int tables = 0;
        if (selectorTree != null) {
            for (DBSObject object : selectorTree.getCheckedObjects()) {
                if (object instanceof DBSCatalog) {
                    catalogs++;
                } else if (object instanceof DBSSchema) {
                    schemas++;
                } else if (object instanceof DBSEntity) {
                    tables++;
                }
            }
        }
        List<String> summary = new ArrayList<>();
        if (catalogs > 0) {
            summary.add(NLS.bind(AIUIMessages.scope_selector_selection_catalogs, catalogs));
        }
        if (schemas > 0) {
            summary.add(NLS.bind(AIUIMessages.scope_selector_selection_schemas, schemas));
        }
        if (tables > 0) {
            summary.add(NLS.bind(AIUIMessages.scope_selector_selection_tables, tables));
        }
        boolean emptyScope = summary.isEmpty();
        scopeInfoLabel.setText(emptyScope ? AIUIMessages.scope_selector_empty_scope : String.join(", ", summary));
        scopeInfoLabel.setForeground(emptyScope ? BaseThemeSettings.instance.colorErrorForeground : null);
        scopeInfoLabel.requestLayout();
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateOkButton();
        getShell().addDisposeListener(event -> {
            if (loadJob != null) {
                loadJob.cancel();
            }
        });
        UIUtils.asyncExec(() -> {
            if (!getShell().isDisposed()) {
                loadInitialObjects();
            }
        });
    }

    private void loadInitialObjects() {
        loadJob = new AbstractJob(AIUIMessages.scope_selector_loading) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    if (!dataSourceContainer.isConnected() || dataSourceContainer.getDataSource() == null) {
                        AtomicReference<IStatus> connectionStatus = new AtomicReference<>();
                        if (!DBUtils.initDataSource(monitor, dataSourceContainer, connectionStatus::set)) {
                            IStatus status = connectionStatus.get();
                            if (status != null && status.getException() != null) {
                                throw new DBException(AIChatMessagesUI.ai_chat_connect_error_message, status.getException());
                            }
                            throw new DBException(AIChatMessagesUI.ai_chat_connect_error_message);
                        }
                    }
                    DBCExecutionContext context = contextProvider.getExecutionContext(dataSourceContainer);
                    if (context == null) {
                        throw new DBException(AIChatMessagesUI.ai_chat_connect_error_message);
                    }
                    DBSObjectContainer root = DBUtils.getAdapter(DBSObjectContainer.class, context.getDataSource());
                    if (root == null) {
                        throw new DBException(AIUIMessages.scope_selector_load_error);
                    }
                    List<DBSObject> selected = initialNodes.isEmpty()
                        ? AITextUtils.loadCustomEntities(monitor, context.getDataSource(), savedObjectIds)
                        : initialNodes.stream().filter(DBNDatabaseNode.class::isInstance)
                            .map(DBNDatabaseNode.class::cast).map(DBNDatabaseNode::getObject)
                            .filter(Objects::nonNull).toList();
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    UIUtils.asyncExec(() -> {
                        if (!getShell().isDisposed() && !monitor.isCanceled()) {
                            showObjects(root, selected);
                        }
                    });
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    UIUtils.asyncExec(() -> {
                        if (!getShell().isDisposed() && !monitor.isCanceled()) {
                            DBWorkbench.getPlatformUI().showError(AIUIMessages.scope_selector_load_error, null, e);
                            cancelPressed();
                        }
                    });
                    return Status.CANCEL_STATUS;
                }
            }
        };
        loadJob.setSystem(true);
        loadJob.schedule();
    }

    private void showObjects(@NotNull DBSObjectContainer root, @NotNull List<DBSObject> selected) {
        loadingLabel.dispose();
        loadingBar.dispose();
        selectorTree = new DatabaseObjectSelectionTree(treeArea, runnableContext, root);
        selectorTree.setLayoutData(new GridData(GridData.FILL_BOTH));
        selectorTree.setCheckedObjects(selected);
        Button showSystemObjects = UIUtils.createCheckbox(
            treeArea, UIConnectionMessages.dialog_connection_wizard_final_checkbox_show_system_objects,
            selectorTree.isShowSystemObjects());
        showSystemObjects.addSelectionListener(SelectionListener.widgetSelectedAdapter(
            event -> selectorTree.setShowSystemObjects(showSystemObjects.getSelection())));
        selectorTree.addCheckListener(() -> {
            updateOkButton();
            updateScopeInfo();
        });
        updateOkButton();
        updateScopeInfo();
        getShell().layout(true, true);
    }

    private void updateOkButton() {
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            okButton.setEnabled(selectorTree != null && selectorTree.hasCheckedObjects());
        }
    }

    @Override
    protected void okPressed() {
        Set<DBNNode> resolvedNodes = new LinkedHashSet<>();
        List<DBSObject> checkedObjects = selectorTree.getCheckedObjects();
        DBNModel navigatorModel = dataSourceContainer.getProject().getNavigatorModel();
        try {
            runnableContext.run(true, true, monitor -> {
                monitor.beginTask(AIUIMessages.scope_selector_loading, checkedObjects.size());
                try {
                    for (DBSObject object : checkedObjects) {
                        if (monitor.isCanceled()) {
                            throw new InterruptedException();
                        }
                        DBNDatabaseNode node = navigatorModel.getNodeByObject(monitor, object, true);
                        if (node == null) {
                            throw new InvocationTargetException(new DBException("Cannot resolve selected object " + object.getName()));
                        }
                        resolvedNodes.add(node);
                        monitor.worked(1);
                    }
                    if (monitor.isCanceled()) {
                        throw new InterruptedException();
                    }
                } finally {
                    monitor.done();
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(AIUIMessages.scope_selector_load_error, null, e.getTargetException());
            return;
        } catch (InterruptedException e) {
            return;
        }
        if (resolvedNodes.isEmpty()) {
            MessageDialog.openInformation(getShell(), AIUIMessages.scope_selector_customize, AIUIMessages.scope_selector_empty);
            return;
        }
        selectedNodes = new ArrayList<>(resolvedNodes);
        super.okPressed();
    }

    public List<? extends DBNNode> getSelectedNodes() {
        return selectedNodes;
    }
}
