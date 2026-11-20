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
package org.jkiss.dbeaver.ui.ai.preferences;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIContextSettingsDataSource;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.ai.AIProjectConnectionCount;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.DBNRoot;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.AIChatUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.navigator.dialogs.SelectDataSourceDialog;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Function;

/** Shared AI access page. MCP providers contribute their own permission columns. */
public class McpConnectionsPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.ai.mcpConnections";
    private static final String UNGROUPED_PREFERENCE = "org.jkiss.dbeaver.ui.ai.mcpConnections.ungrouped";
    private static final Log log = Log.getLog(McpConnectionsPreferencePage.class);

    private final List<McpConnectionsPageRegistry.McpConnectionsPageContribution> contributions =
        McpConnectionsPageRegistry.createContributions();
    private final Map<DBPDataSourceContainer, AIContextSettingsDataSource> pendingSettings = new LinkedHashMap<>();
    private final Map<DBPProject, List<DBPDataSourceContainer>> groupedConnections = new LinkedHashMap<>();

    private TreeViewer viewer;
    private Button ungroupedCheck;
    private Button removeButton;
    private Label loadWarning;
    @Nullable
    private ILabelDecorator projectDecorator;

    public McpConnectionsPreferencePage() {
        noDefaultButton();
    }

    @Override
    public void init(@NotNull IWorkbench workbench) {
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite content = UIUtils.createComposite(parent, 1);
        content.setLayoutData(new GridData(GridData.FILL_BOTH));

        createGroupingControls(content);
        createConnectionsList(content);
        loadWarning = UIUtils.createLabel(content, "");
        loadWarning.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (ungroupedCheck.getSelection()) {
            openAllProjects();
        }
        refreshConnections();
        return content;
    }

    private void createGroupingControls(@NotNull Composite parent) {
        ungroupedCheck = UIUtils.createCheckbox(parent, AIUIMessages.ai_mcp_connections_ungrouped,
            AIUIMessages.ai_mcp_connections_load_all,
            DBWorkbench.getPlatform().getPreferenceStore().getBoolean(UNGROUPED_PREFERENCE), 1);
        Label loadAllHint = UIUtils.createLabel(parent, AIUIMessages.ai_mcp_connections_load_all);
        loadAllHint.setForeground(loadAllHint.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
        GridData hintLayout = new GridData(GridData.FILL_HORIZONTAL);
        hintLayout.horizontalIndent = UIUtils.getFontHeight(ungroupedCheck) * 2;
        loadAllHint.setLayoutData(hintLayout);
        ungroupedCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            if (ungroupedCheck.getSelection()) {
                openAllProjects();
            }
            refreshConnections();
        }));
    }

    private void createConnectionsList(@NotNull Composite parent) {
        Composite listRow = UIUtils.createComposite(parent, 2);
        GridData listLayout = new GridData(GridData.FILL_BOTH);
        listLayout.verticalIndent = 12;
        listRow.setLayoutData(listLayout);
        createConnectionTree(listRow);
        createConnectionColumns();
        createConnectionActions(listRow);
    }

    private void createConnectionTree(@NotNull Composite parent) {
        viewer = new TreeViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL);
        Tree tree = viewer.getTree();
        tree.setHeaderVisible(true);
        tree.setLinesVisible(false);
        GridData treeData = new GridData(GridData.FILL_BOTH);
        treeData.heightHint = tree.getItemHeight() * 12;
        tree.setLayoutData(treeData);
        ILabelDecorator decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
        projectDecorator = decorator;
        ILabelProviderListener listener = event -> UIUtils.asyncExec(() -> {
            if (!tree.isDisposed()) {
                viewer.refresh();
            }
        });
        decorator.addListener(listener);
        tree.addDisposeListener(event -> {
            decorator.removeListener(listener);
            decorator.dispose();
            projectDecorator = null;
        });
        viewer.setContentProvider(new ITreeContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                return ungroupedCheck.getSelection()
                    ? groupedConnections.values().stream().flatMap(List::stream).toArray()
                    : groupedConnections.keySet().toArray();
            }

            @Override
            public Object[] getChildren(Object parentElement) {
                if (parentElement instanceof DBPProject project) {
                    if (!project.isOpen() || !project.isRegistryLoaded()) {
                        return new Object[]{new ProjectPlaceholder(project)};
                    }
                    return groupedConnections.getOrDefault(project, List.of()).toArray();
                }
                return new Object[0];
            }

            @Override
            public Object getParent(Object element) {
                return element instanceof DBPDataSourceContainer connection && !ungroupedCheck.getSelection()
                    ? connection.getProject()
                    : element instanceof ProjectPlaceholder placeholder ? placeholder.project() : null;
            }

            @Override
            public boolean hasChildren(Object element) {
                return element instanceof DBPProject project &&
                    (!project.isOpen() || !project.isRegistryLoaded() || !groupedConnections.getOrDefault(project, List.of()).isEmpty());
            }
        });
        viewer.addTreeListener(new ITreeViewerListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                if (event.getElement() instanceof DBPProject project && (!project.isOpen() || !project.isRegistryLoaded())) {
                    UIUtils.asyncExec(() -> {
                        if (!viewer.getControl().isDisposed() && viewer.getExpandedState(project)) {
                            loadProject(project);
                        }
                    });
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
            }
        });
    }

    private void createConnectionColumns() {
        TreeViewerColumn connectionColumn = new TreeViewerColumn(viewer, SWT.LEFT);
        connectionColumn.getColumn().setText(AIUIMessages.ai_mcp_connections_connection);
        connectionColumn.getColumn().setWidth(350);
        connectionColumn.setLabelProvider(new DelegatingStyledCellLabelProvider(new ConnectionLabelProvider()));
        createColumn(AIUIMessages.ai_mcp_connections_driver, 110,
            element -> element instanceof DBPDataSourceContainer connection ? connection.getDriver().getName() : "");
        createScopeColumn();
        for (McpConnectionsPageRegistry.McpConnectionsPageContribution contribution : contributions) {
            contribution.createColumns(viewer);
        }
    }

    private void createConnectionActions(@NotNull Composite parent) {
        Composite actions = UIUtils.createComposite(parent, 1);
        actions.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        UIUtils.createPushButton(actions, null, AIUIMessages.ai_mcp_connections_add,
            UIIcon.ROW_ADD, SelectionListener.widgetSelectedAdapter(event -> onAddConnections()));
        removeButton = UIUtils.createPushButton(actions, null, AIUIMessages.ai_mcp_connections_remove,
            UIIcon.ROW_DELETE, SelectionListener.widgetSelectedAdapter(event -> removeSelectedConnections()));
        viewer.addSelectionChangedListener(event -> updateRemoveButton());
        updateRemoveButton();
    }

    private void createColumn(@NotNull String title, int width, @NotNull Function<Object, String> text) {
        TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.LEFT);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public @NotNull String getText(Object element) {
                return text.apply(element);
            }
        });
    }

    @Nullable
    private Integer getProjectCount(@NotNull DBPProject project) {
        if (project.isOpen() && project.isRegistryLoaded()) {
            return groupedConnections.get(project).size();
        }
        return AIProjectConnectionCount.getCount(project);
    }

    private final class ConnectionLabelProvider extends ColumnLabelProvider
        implements DelegatingStyledCellLabelProvider.IStyledLabelProvider {

        @NotNull
        @Override
        public StyledString getStyledText(Object element) {
            if (element instanceof DBPProject project) {
                Integer count = getProjectCount(project);
                return new StyledString(project.getName()).append("    " + NLS.bind(AIUIMessages.ai_mcp_connections_count,
                    count == null ? "—" : count.toString()), StyledString.QUALIFIER_STYLER);
            }
            if (element instanceof DBPDataSourceContainer connection) {
                return new StyledString(connection.getName());
            }
            return new StyledString();
        }

        @NotNull
        @Override
        public String getText(Object element) {
            return getStyledText(element).getString();
        }

        @Nullable
        @Override
        public Image getImage(Object element) {
            if (element instanceof DBPProject project) {
                DBNRoot root = DBWorkbench.getPlatform().getNavigatorModel().getRoot();
                DBNProject node = root == null ? null : root.getProjectNode(project);
                Image base = DBeaverIcons.getImage(node == null ? DBIcon.PROJECT : node.getNodeIconDefault());
                Image decorated = node == null || projectDecorator == null ? null : projectDecorator.decorateImage(base, node);
                return decorated == null ? base : decorated;
            }
            return element instanceof DBPDataSourceContainer connection
                ? DBeaverIcons.getImage(connection.getDriver().getIcon()) : null;
        }
    }

    private void createScopeColumn() {
        String[] titles = new String[AIDatabaseScope.values().length];
        for (AIDatabaseScope scope : AIDatabaseScope.values()) {
            titles[scope.ordinal()] = scope.getTitle();
        }
        TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.LEFT);
        column.getColumn().setText(AIUIMessages.ai_mcp_connections_scope);
        column.getColumn().setWidth(160);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return element instanceof DBPDataSourceContainer connection
                    ? getSettings(connection).getScope().getTitle() : "";
            }

            @Nullable
            @Override
            public Image getImage(Object element) {
                return element instanceof DBPDataSourceContainer connection && isCustomScope(connection)
                    ? DBeaverIcons.getImage(UIIcon.CONFIGURATION) : null;
            }

            @Nullable
            @Override
            public String getToolTipText(Object element) {
                return element instanceof DBPDataSourceContainer connection && isCustomScope(connection)
                    ? AIUIMessages.ai_connection_settings_custom_scope_label : null;
            }
        });
        ColumnViewerToolTipSupport.enableFor(viewer);
        Tree tree = viewer.getTree();
        int columnIndex = tree.indexOf(column.getColumn());
        tree.addListener(SWT.MouseDown, event -> {
            if (event.button != 1) {
                return;
            }
            TreeItem item = tree.getItem(new Point(event.x, event.y));
            if (item != null && item.getData() instanceof DBPDataSourceContainer connection
                && isCustomScope(connection) && item.getImageBounds(columnIndex).contains(event.x, event.y)) {
                event.doit = false;
                viewer.cancelEditing();
                onCustomScopeSelected(connection);
            }
        });
        tree.addListener(SWT.KeyDown, event -> {
            if ((event.stateMask & SWT.MOD1) != 0 && event.keyCode == SWT.CR
                && viewer.getStructuredSelection().getFirstElement() instanceof DBPDataSourceContainer connection
                && isCustomScope(connection)) {
                event.doit = false;
                onCustomScopeSelected(connection);
            }
        });
        column.setEditingSupport(new EditingSupport(viewer) {
            private final ComboBoxCellEditor editor = new ComboBoxCellEditor(viewer.getTree(), titles, SWT.READ_ONLY);

            @Override
            protected ComboBoxCellEditor getCellEditor(Object element) {
                return editor;
            }

            @Override
            protected boolean canEdit(Object element) {
                return element instanceof DBPDataSourceContainer;
            }

            @Override
            protected Object getValue(Object element) {
                return getSettings((DBPDataSourceContainer) element).getScope().ordinal();
            }

            @Override
            protected void setValue(Object element, Object value) {
                if (!(value instanceof Integer index) || index < 0 || index >= AIDatabaseScope.values().length) {
                    return;
                }
                DBPDataSourceContainer connection = (DBPDataSourceContainer) element;
                AIDatabaseScope scope = AIDatabaseScope.values()[index];
                if (scope == AIDatabaseScope.CUSTOM) {
                    onCustomScopeSelected(connection);
                    viewer.update(element, null);
                    return;
                }
                pendingSettings.computeIfAbsent(connection, AIContextSettingsDataSource::new).setScope(scope);
                viewer.update(element, null);
            }
        });
    }

    private boolean isCustomScope(@NotNull DBPDataSourceContainer connection) {
        return getSettings(connection).getScope() == AIDatabaseScope.CUSTOM;
    }

    @NotNull
    private AIContextSettingsDataSource getSettings(@NotNull DBPDataSourceContainer connection) {
        AIContextSettingsDataSource settings = pendingSettings.get(connection);
        return settings == null ? new AIContextSettingsDataSource(connection) : settings;
    }

    private void openAllProjects() {
        boolean failed = false;
        for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getVisibleProjects()) {
            try {
                project.ensureOpen();
                project.getDataSourceRegistry();
            } catch (RuntimeException e) {
                log.debug("Cannot open project for AI connections", e);
                failed = true;
            }
        }
        loadWarning.setText(failed ? AIUIMessages.ai_mcp_connections_open_failed : "");
        loadWarning.getParent().layout(true, true);
    }

    private void loadProject(@NotNull DBPProject project) {
        try {
            project.ensureOpen();
            project.getDataSourceRegistry();
            refreshConnections();
            viewer.setExpandedState(project, true);
        } catch (RuntimeException e) {
            log.debug("Cannot open project for AI connections", e);
            loadWarning.setText(AIUIMessages.ai_mcp_connections_open_failed);
            loadWarning.getParent().layout(true, true);
        }
    }

    private void refreshConnections() {
        groupedConnections.clear();
        List<? extends DBPProject> projects = DBWorkbench.getPlatform().getWorkspace().getVisibleProjects().stream()
            .sorted(Comparator.comparing(DBPProject::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        for (DBPProject project : projects) {
            groupedConnections.put(project, new ArrayList<>());
            if (!project.isOpen() || !project.isRegistryLoaded()) {
                continue;
            }
            try {
                for (DBPDataSourceContainer connection : project.getDataSourceRegistry().getDataSources()) {
                    if (getSettings(connection).isMetaTransferConfirmed()) {
                        groupedConnections.get(project).add(connection);
                    }
                }
            } catch (RuntimeException e) {
                log.debug("Cannot read AI connections from project", e);
            }
        }
        for (List<DBPDataSourceContainer> projectConnections : groupedConnections.values()) {
            projectConnections.sort(Comparator.comparing(DBPDataSourceContainer::getName, String.CASE_INSENSITIVE_ORDER));
        }
        groupedConnections.entrySet().removeIf(entry -> {
            Integer count = getProjectCount(entry.getKey());
            return count != null && count == 0;
        });
        Object[] expanded = viewer.getExpandedElements();
        viewer.setInput(groupedConnections.keySet().toArray());
        if (!ungroupedCheck.getSelection()) {
            viewer.setExpandedElements(expanded);
        }
        updateRemoveButton();
    }

    private void updateRemoveButton() {
        removeButton.setEnabled(viewer.getStructuredSelection().toList().stream()
            .anyMatch(DBPDataSourceContainer.class::isInstance));
    }

    private void removeSelectedConnections() {
        for (Object element : viewer.getStructuredSelection()) {
            if (element instanceof DBPDataSourceContainer connection) {
                pendingSettings.computeIfAbsent(connection, AIContextSettingsDataSource::new)
                    .setMetaTransferConfirmed(false);
            }
        }
        refreshConnections();
    }

    private void onAddConnections() {
        List<? extends DBPProject> projects = DBWorkbench.getPlatform().getWorkspace().getVisibleProjects();
        SelectDataSourceDialog dialog = SelectDataSourceDialog.forMultipleConnections(
            getShell(), projects::contains, connection -> !getSettings(connection).isMetaTransferConfirmed());
        if (dialog.open() != Window.OK) {
            return;
        }
        for (DBPDataSourceContainer connection : dialog.getSelectedDataSources()) {
            pendingSettings.computeIfAbsent(connection, AIContextSettingsDataSource::new).setMetaTransferConfirmed(true);
        }
        refreshConnections();
    }

    private void onCustomScopeSelected(@NotNull DBPDataSourceContainer connection) {
        DBPDataSourceContainer stored = connection.getRegistry().getDataSource(connection.getId());
        if (stored == null) {
            return;
        }
        AIChatUtils.selectCustomScope(
            getShell(), getSettings(connection), stored, dataSource -> DBUtils.getDefaultContext(dataSource, false), null,
            ids -> {
                AIContextSettingsDataSource draft = pendingSettings.computeIfAbsent(connection, AIContextSettingsDataSource::new);
                draft.setScope(AIDatabaseScope.CUSTOM);
                draft.setCustomObjectIds(ids.toArray(String[]::new));
                if (!viewer.getControl().isDisposed()) {
                    viewer.update(connection, null);
                }
            });
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && viewer != null && !viewer.getControl().isDisposed()) {
            refreshConnections();
        }
    }

    private boolean saveChanges() {
        for (var entry : new ArrayList<>(pendingSettings.entrySet())) {
            try {
                DBPDataSourceContainer connection = entry.getKey();
                connection.setExtension(AIContextSettingsDataSource.AI_DS_EXTENSION, entry.getValue().saveSettingsToMap());
                if (!connection.persistConfiguration()) {
                    showSaveError(new DBException("Cannot persist AI settings for connection '" + connection.getName() + "'"));
                    return false;
                }
                AIProjectConnectionCount.refresh(connection.getRegistry());
                pendingSettings.remove(connection);
            } catch (RuntimeException e) {
                showSaveError(e);
                return false;
            }
        }
        for (McpConnectionsPageRegistry.McpConnectionsPageContribution contribution : contributions) {
            try {
                contribution.save();
            } catch (DBException e) {
                showSaveError(e);
                return false;
            }
        }
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        boolean previousUngrouped = store.getBoolean(UNGROUPED_PREFERENCE);
        if (previousUngrouped != ungroupedCheck.getSelection()) {
            store.setValue(UNGROUPED_PREFERENCE, ungroupedCheck.getSelection());
            try {
                store.save();
            } catch (IOException e) {
                store.setValue(UNGROUPED_PREFERENCE, previousUngrouped);
                showSaveError(e);
                return false;
            }
        }
        loadWarning.setText("");
        refreshConnections();
        return true;
    }

    private void showSaveError(@NotNull Exception error) {
        log.error("Cannot save AI connection settings", error);
        loadWarning.setText(AIUIMessages.ai_mcp_connections_save_error_message);
        loadWarning.getParent().layout(true, true);
    }

    @Override
    protected void performApply() {
        saveChanges();
    }

    @Override
    public boolean performOk() {
        return saveChanges() && super.performOk();
    }

    @Override
    public boolean performCancel() {
        pendingSettings.clear();
        for (McpConnectionsPageRegistry.McpConnectionsPageContribution contribution : contributions) {
            contribution.discard();
        }
        return super.performCancel();
    }

    private record ProjectPlaceholder(@NotNull DBPProject project) {
    }

    public static final class McpConnectionsPageRegistry {
        private static final String EXTENSION_ID = "org.jkiss.dbeaver.ui.ai.mcpConnectionsPage";
        private static final Log log = Log.getLog(McpConnectionsPageRegistry.class);

        private McpConnectionsPageRegistry() {
        }

        /** Adds product-specific access columns to the common connections page. */
        public interface McpConnectionsPageContribution {
            /** Project group rows are present unless the user selects the ungrouped view. */
            void createColumns(@NotNull TreeViewer viewer);

            /** Persist prepared changes on Apply or OK; keep unsaved changes after a failure. */
            void save() throws DBException;

            /** Discard changes prepared by this contribution when the dialog is cancelled. */
            void discard();
        }

        @NotNull
        private static List<McpConnectionsPageContribution> createContributions() {
            List<McpConnectionsPageContribution> contributions = new ArrayList<>();
            IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
            List<IConfigurationElement> sorted = new ArrayList<>();
            for (IConfigurationElement element : elements) {
                if ("contribution".equals(element.getName())) {
                    sorted.add(element);
                }
            }
            sorted.sort(Comparator.comparing(element -> element.getAttribute("id")));
            for (IConfigurationElement element : sorted) {
                try {
                    contributions.add((McpConnectionsPageContribution) element.createExecutableExtension("class"));
                } catch (CoreException | ClassCastException e) {
                    log.error("Cannot load MCP connections page contribution", e);
                }
            }
            return contributions;
        }
    }
}
