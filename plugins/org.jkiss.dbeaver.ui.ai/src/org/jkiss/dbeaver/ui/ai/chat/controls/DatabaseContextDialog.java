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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;
import org.jkiss.dbeaver.ui.ai.controls.ScopeSelectorDialog;
import org.jkiss.dbeaver.ui.controls.DatabaseLabelProviders;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilter;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseObjectsSelectorPanel;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DatabaseContextDialog extends BaseDialog {
    private final AIChatControl chat;
    private final AIChatContextSettingsEdit edit;
    private final DBRRunnableContext context = UIUtils.getDefaultRunnableContext();
    private final Map<AIChatContextSettingsEdit.DatabaseEdit, List<DBNNode>> pendingSelections = new LinkedHashMap<>();
    private ScopePanel currentPanel;

    DatabaseContextDialog(@NotNull Shell shell, @NotNull AIChatControl chat) {
        super(shell, AIChatMessagesUI.ai_chat_database_context, null);
        this.chat = chat;
        edit = new AIChatContextSettingsEdit(chat.getChatSession(), chat.getActiveConversation());
    }

    @NotNull
    @Override
    protected Point getInitialSize() {
        return new Point(790, 640);
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite area = super.createDialogArea(parent);
        // keep the content opaque during native window dragging on macOS
        area.setBackground(UIStyles.getDefaultWidgetBackground());
        area.setBackgroundMode(SWT.INHERIT_DEFAULT);
        Composite body = new Composite(area, SWT.NONE);
        body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 0;
        body.setLayout(layout);
        TableViewer databases = new TableViewer(body, SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.BORDER);
        GridData listData = new GridData(SWT.FILL, SWT.FILL, false, true);
        listData.widthHint = 180;
        databases.getTable().setLayoutData(listData);
        databases.setContentProvider(ArrayContentProvider.getInstance());
        databases.setLabelProvider(new DatabaseLabelProviders.ConnectionLabelProvider());
        databases.setInput(edit.getDatabases().stream().map(AIChatContextSettingsEdit.DatabaseEdit::getContainer).toList());
        Composite editor = new Composite(body, SWT.NONE);
        editor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout editorLayout = new GridLayout(1, false);
        editorLayout.marginWidth = 0;
        editorLayout.marginHeight = 0;
        editor.setLayout(editorLayout);
        databases.addSelectionChangedListener(event -> {
            Object selected = event.getStructuredSelection().getFirstElement();
            edit.getDatabases().stream().filter(database -> database.getContainer() == selected).findFirst().ifPresent(database -> {
                if (currentPanel != null) {
                    if (currentPanel.database == database) {
                        return;
                    }
                    currentPanel.rememberSelection();
                    currentPanel.dispose();
                }
                currentPanel = new ScopePanel(editor, database);
                currentPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
                editor.layout(true, true);
                currentPanel.initialize();
            });
        });
        if (!edit.getDatabases().isEmpty()) {
            databases.setSelection(new StructuredSelection(edit.getDatabases().getFirst().getContainer()));
        }
        return area;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        createButton(parent, IDialogConstants.OK_ID, AIChatMessagesUI.ai_chat_context_apply, true);
        updateApplyButton();
    }

    private void updateApplyButton() {
        Button apply = getButton(IDialogConstants.OK_ID);
        if (apply != null && !apply.isDisposed()) {
            apply.setEnabled((currentPanel == null || currentPanel.isDisposed()
                || currentPanel.loadJob == null && currentPanel.database.getSettings().getScope() != null)
                && edit.getDatabases().stream().allMatch(this::hasValidSelection));
        }
    }

    private boolean hasValidSelection(@NotNull AIChatContextSettingsEdit.DatabaseEdit database) {
        AIDatabaseScope scope = database.getSettings().getScope();
        if (scope == null) {
            return !database.isChanged();
        }
        if (scope != AIDatabaseScope.CUSTOM) {
            return true;
        }
        List<DBNNode> checked = pendingSelections.get(database);
        if (currentPanel != null && !currentPanel.isDisposed() && currentPanel.database == database) {
            if (currentPanel.selector == null) {
                return false;
            }
            checked = currentPanel.selector.getCheckedNodes();
        }
        return checked == null ? !ArrayUtils.isEmpty(database.getSettings().getCustomObjectIds())
            : checked.stream().anyMatch(ScopeSelectorDialog::isSupportedNode);
    }

    @Override
    protected void okPressed() {
        if (currentPanel != null) {
            currentPanel.rememberSelection();
        }
        Map<AIChatContextSettingsEdit.DatabaseEdit, List<DBNNode>> selections = new LinkedHashMap<>(pendingSelections);
        try {
            context.run(true, true, monitor -> {
                try {
                    for (var selection : selections.entrySet()) {
                        collectSelection(monitor, selection.getKey(), selection.getValue());
                    }
                    edit.apply(monitor);
                } catch (DBException exception) {
                    throw new InvocationTargetException(exception);
                }
            });
            chat.setCompletionSettings(chat.getChatSession().getConversationSettings(chat.getActiveConversation()));
            super.okPressed();
        } catch (InvocationTargetException exception) {
            DBWorkbench.getPlatformUI().showError(AIChatMessagesUI.ai_chat_database_context, null, exception.getTargetException());
        } catch (InterruptedException expected) {
            // leave the pending edits available after cancellation
        }
    }

    private static void collectSelection(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIChatContextSettingsEdit.DatabaseEdit database,
        @NotNull List<DBNNode> checked
    ) throws DBException, InterruptedException {
        if (database.getSettings().getScope() != AIDatabaseScope.CUSTOM) {
            return;
        }
        Set<DBNNode> resolved = new LinkedHashSet<>();
        for (DBNNode node : checked) {
            ScopeSelectorDialog.collectSelectedNodes(monitor, node, resolved);
        }
        database.getSettings().setCustomObjectIds(resolved.stream().map(DBNDatabaseNode.class::cast)
            .map(DBNDatabaseNode::getValueObject).map(DBSObject.class::cast).map(DBUtils::getObjectFullId).toArray(String[]::new));
    }

    private class ScopePanel extends Composite {
        private final AIChatContextSettingsEdit.DatabaseEdit database;
        private final List<AIDatabaseScope> scopes = new ArrayList<>();
        private final Combo scope;
        private final Label summary;
        private final Composite objects;
        private final Composite connectionState;
        private final Label connectionMessage;
        private final ProgressBar connectionProgress;
        private final Button connectionAction;
        private final Combo level;
        private DatabaseObjectsSelectorPanel selector;
        private AbstractJob loadJob;
        private Throwable loadFailure;
        private boolean loadCancelled;
        private long loadGeneration;

        ScopePanel(@NotNull Composite parent, @NotNull AIChatContextSettingsEdit.DatabaseEdit database) {
            super(parent, SWT.NONE);
            this.database = database;
            GridLayout layout = new GridLayout(1, false);
            layout.marginWidth = 20;
            layout.marginHeight = 0;
            layout.marginBottom = 20;
            layout.verticalSpacing = 10;
            setLayout(layout);
            Composite scopeRow = createRow(this, AIChatMessagesUI.ai_chat_context_objects);
            scope = new Combo(scopeRow, SWT.READ_ONLY | SWT.DROP_DOWN);
            scope.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            refreshScopes();
            connectionState = new Composite(this, SWT.NONE);
            connectionState.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            GridLayout stateLayout = new GridLayout(3, false);
            stateLayout.marginWidth = 0;
            stateLayout.marginHeight = 0;
            connectionState.setLayout(stateLayout);
            connectionMessage = new Label(connectionState, SWT.WRAP);
            connectionMessage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            connectionProgress = new ProgressBar(connectionState, SWT.INDETERMINATE);
            GridData progressData = new GridData(SWT.FILL, SWT.CENTER, false, false);
            progressData.widthHint = 60;
            connectionProgress.setLayoutData(progressData);
            connectionAction = new Button(connectionState, SWT.PUSH);
            connectionAction.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
            connectionAction.addListener(SWT.Selection, event -> {
                if (loadJob == null) {
                    startLoading();
                } else {
                    cancelLoading();
                    updateScope();
                }
            });
            objects = new Composite(this, SWT.NONE);
            GridLayout objectLayout = new GridLayout(1, false);
            objectLayout.marginWidth = 0;
            objectLayout.marginHeight = 0;
            objects.setLayout(objectLayout);
            objects.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            summary = new Label(this, SWT.WRAP);
            summary.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            Composite applyRow = createRow(this, AIChatMessagesUI.ai_chat_context_apply_to);
            level = new Combo(applyRow, SWT.READ_ONLY | SWT.DROP_DOWN);
            level.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            level.setItems(AIChatMessagesUI.ai_chat_context_menu_this_connection, AIChatMessagesUI.ai_chat_context_menu_this_conversation);
            level.select(database.isConnectionLevel() ? 0 : 1);
            level.addListener(SWT.Selection, event -> {
                database.setConnectionLevel(level.getSelectionIndex() == 0);
                if (database.getSettings().getScope() == null) {
                    database.getSettings().setScope(AIDatabaseScope.CURRENT_DATASOURCE);
                }
            });
            scope.addListener(SWT.Selection, event -> {
                AIDatabaseScope selected = scopes.get(scope.getSelectionIndex());
                database.getSettings().setScope(selected);
                loadFailure = null;
                loadCancelled = false;
                refreshScopes();
                updateScope();
                if (selected == AIDatabaseScope.CUSTOM && selector == null) {
                    startLoading();
                }
            });
            addDisposeListener(event -> cancelLoading());
            updateScope();
        }

        private void refreshScopes() {
            DBCExecutionContext execution = database.getContainer().isConnected()
                ? chat.getExecutionContext(database.getContainer()) : null;
            database.initializeScope(execution);
            DBCExecutionContextDefaults<?, ?> defaults = execution == null ? null : execution.getContextDefaults();
            scopes.clear();
            scopes.addAll(AIDatabaseScope.getAvailableScopes(defaults));
            AIDatabaseScope current = database.getSettings().getScope();
            if (current != null && !scopes.contains(current)) {
                if (defaults == null) {
                    scopes.add(current);
                } else {
                    current = current == AIDatabaseScope.CURRENT_SCHEMA && scopes.contains(AIDatabaseScope.CURRENT_DATABASE)
                        ? AIDatabaseScope.CURRENT_DATABASE : AIDatabaseScope.CURRENT_DATASOURCE;
                    database.getSettings().setScope(current);
                }
            }
            scope.setItems(scopes.stream().map(value -> switch (value) {
                case CURRENT_DATASOURCE -> AIChatMessagesUI.ai_chat_context_all_objects;
                case CUSTOM -> AIChatMessagesUI.ai_chat_context_selected_objects;
                default -> AIChatScopeUtils.getTitle(database.getSettings(), value, database.getContainer(), defaults);
            }).toArray(String[]::new));
            if (current == null) {
                scope.deselectAll();
            } else {
                scope.select(scopes.indexOf(current));
            }
        }

        private void initialize() {
            getDisplay().asyncExec(() -> {
                if (!isDisposed() && (!database.getContainer().isConnected()
                    || database.getSettings().getScope() == AIDatabaseScope.CUSTOM)) {
                    startLoading();
                }
            });
            updateApplyButton();
        }

        private void cancelLoading() {
            loadGeneration++;
            if (loadJob != null) {
                loadJob.cancel();
                loadJob = null;
                loadCancelled = true;
            }
        }

        private void startLoading() {
            if (isDisposed() || loadJob != null) {
                return;
            }
            final long generation = ++loadGeneration;
            final boolean loadObjects = database.getSettings().getScope() == AIDatabaseScope.CUSTOM && selector == null;
            final DBPDataSourceContainer container = database.getContainer();
            final List<DBNNode> pending = pendingSelections.get(database);
            String[] customIds = database.getSettings().getCustomObjectIds();
            final Set<String> ids = customIds == null ? Set.of() : new LinkedHashSet<>(Arrays.asList(customIds));
            loadFailure = null;
            loadCancelled = false;
            loadJob = new AbstractJob(AIChatMessagesUI.ai_chat_context_loading) {
                @NotNull
                @Override
                protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                    DBNNode root = null;
                    List<DBNNode> checked = new ArrayList<>();
                    Throwable failure = null;
                    boolean cancelled = false;
                    try {
                        if (monitor.isCanceled()) {
                            throw new OperationCanceledException();
                        }
                        if ((!container.isConnected() && !container.connect(monitor, true, true)) || !container.isConnected()) {
                            throw new DBException("Could not connect to " + container.getName());
                        }
                        if (loadObjects) {
                            DBNModel navigator = container.getProject().getNavigatorModel();
                            if (navigator == null || container.getDataSource() == null) {
                                throw new DBException("Database is not available: " + container.getName());
                            }
                            if (pending != null) {
                                checked.addAll(pending);
                            } else {
                                for (DBSObject object : AITextUtils.loadCustomEntities(monitor, container.getDataSource(), ids)) {
                                    if (monitor.isCanceled()) {
                                        throw new OperationCanceledException();
                                    }
                                    DBNDatabaseNode node = navigator.getNodeByObject(monitor, object, true);
                                    if (node != null) {
                                        checked.add(node);
                                    }
                                }
                            }
                            root = navigator.getNodeByObject(container);
                            if (root == null) {
                                throw new DBException("Database navigator is not available: " + container.getName());
                            }
                        }
                    } catch (OperationCanceledException expected) {
                        cancelled = true;
                    } catch (Exception exception) {
                        failure = exception;
                    }
                    final DBNNode loadedRoot = root;
                    final Throwable loadedFailure = failure;
                    final boolean loadWasCancelled = cancelled || monitor.isCanceled();
                    UIUtils.asyncExec(() -> {
                        if (isDisposed() || generation != loadGeneration) {
                            return;
                        }
                        loadJob = null;
                        loadFailure = loadWasCancelled ? null : loadedFailure;
                        loadCancelled = loadWasCancelled;
                        if (!loadCancelled && loadFailure == null) {
                            try {
                                if (loadedRoot != null) {
                                    createSelector(loadedRoot, checked);
                                }
                                refreshScopes();
                            } catch (RuntimeException exception) {
                                for (Control child : objects.getChildren()) {
                                    child.dispose();
                                }
                                selector = null;
                                loadFailure = exception;
                            }
                        }
                        updateScope();
                    });
                    return Status.OK_STATUS;
                }
            };
            updateScope();
            loadJob.schedule();
        }

        private void createSelector(@NotNull DBNNode root, @NotNull List<DBNNode> checked) {
            DBPDataSourceContainer container = database.getContainer();
            selector = new DatabaseObjectsSelectorPanel(objects, context, SWT.SINGLE | SWT.BORDER | SWT.CHECK, true) {
                @NotNull
                @Override
                protected DatabaseNavigatorTree createNavigatorTree(
                    @Nullable DBNNode rootNode,
                    int style,
                    @Nullable DatabaseNavigatorTreeFilter navigatorFilter
                ) {
                    return new DatabaseNavigatorTree(this, rootNode, style, false, navigatorFilter) {
                        @NotNull
                        @Override
                        protected Text createFilterText(@NotNull Composite parent) {
                            // native macOS search-field drawing dominates the window-drag profile
                            return RuntimeUtils.isMacOS() ? new Text(parent, SWT.SINGLE | SWT.BORDER)
                                : super.createFilterText(parent);
                        }
                    };
                }

                @NotNull
                @Override
                protected DBPProject getSelectedProject() {
                    return container.getProject();
                }

                @Nullable
                @Override
                protected DBNNode getRootNode() {
                    return root;
                }

                @Override
                protected boolean isDatabaseFolderVisible(@NotNull DBNDatabaseFolder folder) {
                    return ScopeSelectorDialog.isSupportedNode(folder);
                }

                @Override
                protected boolean isDatabaseObjectVisible(@NotNull DBSObject object) {
                    return object instanceof DBSStructContainer || object instanceof DBSEntityContainer || object instanceof DBSEntity;
                }
            };
            GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
            data.heightHint = 273;
            selector.setLayoutData(data);
            Text filter = selector.getNavigatorTree().getFilterControl();
            if (filter != null) {
                filter.setMessage(AIChatMessagesUI.ai_chat_context_filter_objects);
            }
            selector.getCheckboxTreeManager().enableLazyCheck(ScopeSelectorDialog::isSupportedNode);
            selector.getNavigatorTree().getViewer().expandToLevel(2);
            selector.checkNodes(checked, true);
            selector.getNavigatorTree().getCheckboxViewer().addCheckStateListener(event -> {
                updateSummary();
                updateApplyButton();
            });
            objects.layout(true, true);
        }

        private void updateScope() {
            boolean custom = database.getSettings().getScope() == AIDatabaseScope.CUSTOM;
            boolean showObjects = custom && selector != null;
            objects.setVisible(showObjects);
            ((GridData) objects.getLayoutData()).exclude = !showObjects;
            updateSummary();
            updateConnectionState();
            layout(true, true);
        }

        private void updateConnectionState() {
            boolean loading = loadJob != null;
            boolean visible = loading || loadFailure != null || loadCancelled || !database.getContainer().isConnected();
            connectionState.setVisible(visible);
            ((GridData) connectionState.getLayoutData()).exclude = !visible;
            connectionProgress.setVisible(loading);
            ((GridData) connectionProgress.getLayoutData()).exclude = !loading;
            connectionMessage.setText(loading ? AIChatMessagesUI.ai_chat_context_loading
                : loadFailure != null ? NLS.bind(AIChatMessagesUI.ai_chat_context_load_failed, loadFailure.getMessage())
                    : loadCancelled ? AIChatMessagesUI.ai_chat_context_load_cancelled
                        : AIChatMessagesUI.ai_chat_context_menu_database_not_connected);
            connectionAction.setText(loading ? IDialogConstants.CANCEL_LABEL
                : loadFailure != null || loadCancelled ? AIChatMessagesUI.ai_chat_context_retry : AIChatMessagesUI.ai_chat_context_connect);
            scope.setEnabled(!loading);
            level.setEnabled(!loading);
            updateApplyButton();
        }

        private void updateSummary() {
            boolean visible = database.getSettings().getScope() == AIDatabaseScope.CUSTOM && selector != null;
            summary.setVisible(visible);
            ((GridData) summary.getLayoutData()).exclude = !visible;
            if (visible) {
                List<DBNNode> checked = selector.getCheckedNodes();
                int count = checked.size();
                List<DBSTable> tables = checked.stream().filter(DBNDatabaseNode.class::isInstance)
                    .map(DBNDatabaseNode.class::cast).map(DBNDatabaseNode::getValueObject)
                    .filter(DBSTable.class::isInstance).map(DBSTable.class::cast).toList();
                DBSObject schema = tables.isEmpty() ? null : tables.getFirst().getParentObject();
                summary.setText(schema != null && tables.size() == checked.size()
                    && tables.stream().allMatch(table -> table.getParentObject() == schema)
                    ? NLS.bind(AIChatMessagesUI.ai_chat_context_tables_selected, count, schema.getName())
                    : NLS.bind(AIChatMessagesUI.ai_chat_context_objects_selected, count));
            }
            summary.requestLayout();
        }

        private void rememberSelection() {
            if (selector != null) {
                pendingSelections.put(database, List.copyOf(selector.getCheckedNodes()));
            }
        }
    }

    @NotNull
    private static Composite createRow(@NotNull Composite parent, @NotNull String label) {
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 8;
        Composite row = new Composite(parent, SWT.NONE);
        row.setLayout(layout);
        row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        Label title = new Label(row, SWT.NONE);
        title.setText(label);
        GridData titleData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        titleData.widthHint = 110;
        title.setLayoutData(titleData);
        return row;
    }
}
