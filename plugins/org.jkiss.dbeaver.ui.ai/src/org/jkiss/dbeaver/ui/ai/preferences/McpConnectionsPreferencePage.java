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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIContextSettingsDataSource;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
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
import org.jkiss.dbeaver.ui.controls.CInfoLabel;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.forms.HintLabel;
import org.jkiss.dbeaver.ui.navigator.dialogs.SelectDataSourceDialog;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Function;

/** Shared AI access page. MCP providers contribute their own permission columns. */
public class McpConnectionsPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.ai.mcpConnections";
    private static final String UNGROUPED_PREFERENCE = "org.jkiss.dbeaver.ui.ai.mcpConnections.ungrouped";
    private static final Log log = Log.getLog(McpConnectionsPreferencePage.class);

    private final List<McpConnectionsPageRegistry.McpConnectionsPageContribution> contributions =
        McpConnectionsPageRegistry.createContributions();
    private final List<ContributionColumn> contributionColumns = new ArrayList<>();
    private final ConnectionsSnapshot snapshot = new ConnectionsSnapshot();

    private TreeViewer viewer;
    private ViewerColumnController<McpBooleanColumn, Object> columnController;
    private boolean restoringProjects;
    private Button groupByProjectCheck;
    private ToolItem removeButton;
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
    public IAdaptable getElement() {
        return AISettingsManager.getInstance().getSettings();
    }

    @Override
    public void setElement(@Nullable IAdaptable element) {
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite content = UIUtils.createComposite(parent, 1);
        content.setLayoutData(new GridData(GridData.FILL_BOTH));

        new CInfoLabel(content, AIUIMessages.ai_mcp_connections_info, DBIcon.SMALL_INFO,
            UIUtils.getFontHeight(content) * HintLabel.PREFERRED_WIDTH_IN_LINE_HEIGHTS);
        createGroupingControls(content);
        snapshot.reload();
        createConnectionsList(content);
        if (!groupByProjectCheck.getSelection()) {
            openAllProjects();
        }
        refreshConnections();
        return content;
    }

    private void createGroupingControls(@NotNull Composite parent) {
        groupByProjectCheck = UIUtils.createCheckbox(parent, AIUIMessages.ai_mcp_connections_ungrouped,
            !DBWorkbench.getPlatform().getPreferenceStore().getBoolean(UNGROUPED_PREFERENCE));
        Label loadAllHint = UIUtils.createLabel(parent, AIUIMessages.ai_mcp_connections_load_all);
        loadAllHint.setForeground(loadAllHint.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
        GridData hintLayout = new GridData(GridData.FILL_HORIZONTAL);
        hintLayout.horizontalIndent = UIUtils.getFontHeight(groupByProjectCheck) * 2;
        loadAllHint.setLayoutData(hintLayout);
        UIUtils.setControlVisible(loadAllHint, !groupByProjectCheck.getSelection());
        groupByProjectCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            UIUtils.setControlVisible(loadAllHint, !groupByProjectCheck.getSelection());
            if (!groupByProjectCheck.getSelection()) {
                openAllProjects();
            }
            refreshConnections();
            loadAllHint.getParent().layout(true, true);
        }));
    }

    private void createConnectionsList(@NotNull Composite parent) {
        Composite listRow = UIUtils.createComposite(parent, 2);
        GridData listLayout = new GridData(GridData.FILL_BOTH);
        listLayout.verticalIndent = 12;
        listRow.setLayoutData(listLayout);
        createConnectionTree(listRow);
        createConnectionColumns();
        columnController.createColumns();
        Tree tree = viewer.getTree();
        createConnectionActions(listRow);
        for (McpConnectionsPageRegistry.McpConnectionsPageContribution contribution : contributions) {
            Runnable onChange = () -> UIUtils.asyncExec(() -> {
                if (!tree.isDisposed()) {
                    updateContributionVisibility();
                }
            });
            contribution.addVisibilityListener(onChange);
            tree.addDisposeListener(event -> contribution.removeVisibilityListener(onChange));
        }
        updateContributionVisibility();
    }

    private void createConnectionTree(@NotNull Composite parent) {
        PatternFilter filter = new PatternFilter() {
            @Override
            protected boolean isLeafMatch(@NotNull Viewer viewer, @Nullable Object element) {
                return element instanceof DBPDataSourceContainer connection
                    && (wordMatches(connection.getName()) || wordMatches(connection.getProject().getName()))
                    || element instanceof DBPProject project && snapshot.get(project).loaded() && wordMatches(project.getName());
            }
        };
        filter.setIncludeLeadingWildcard(true);
        FilteredTree filteredTree = new FilteredTree(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL, filter, true, true) {
            @Override
            protected @NotNull Text doCreateFilterText(@NotNull Composite parent) {
                return new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
            }
        };
        filteredTree.setInitialText(AIUIMessages.ai_mcp_connections_filter);
        viewer = filteredTree.getViewer();
        TreeViewerEditor.create(viewer, new ColumnViewerEditorActivationStrategy(viewer),
            ColumnViewerEditor.DEFAULT | ColumnViewerEditor.KEEP_EDITOR_ON_DOUBLE_CLICK);
        Text filterControl = filteredTree.getFilterControl();
        if (filterControl != null) {
            UIUtils.installAndUpdateMainFont(filterControl);
        }
        Tree tree = viewer.getTree();
        tree.setHeaderVisible(true);
        tree.setLinesVisible(false);
        GridData treeData = new GridData(GridData.FILL_BOTH);
        treeData.heightHint = tree.getItemHeight() * 12;
        filteredTree.setLayoutData(treeData);
        columnController = new ViewerColumnController<>(PAGE_ID, viewer) {
            @Override
            protected void repackColumns(boolean forceRepack) {
                super.repackColumns(forceRepack);
                // Packing also gives hidden contribution columns a non-zero width.
                updateContributionVisibility();
            }

            @Override
            public boolean configureColumns() {
                boolean changed = super.configureColumns();
                updateContributionVisibility();
                return changed;
            }
        };
        columnController.setComparator(new ViewerColumnController.DefaultComparator() {
            @Override
            public int category(@Nullable Object element) {
                return element instanceof DBPProject ? 0 : 1;
            }
        });
        MenuManager menu = new MenuManager();
        menu.setRemoveAllWhenShown(true);
        menu.addMenuListener(manager -> columnController.fillConfigMenu(manager));
        tree.setMenu(menu.createContextMenu(tree));
        ILabelDecorator decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
        projectDecorator = decorator;
        ILabelProviderListener listener = event -> UIUtils.asyncExec(() -> {
            if (!tree.isDisposed()) {
                snapshot.projects.values().forEach(project -> project.icon = null);
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
                ProjectEntry[] projects = (ProjectEntry[]) inputElement;
                return groupByProjectCheck.getSelection()
                    ? Arrays.stream(projects).map(ProjectEntry::project).toArray()
                    : Arrays.stream(projects).flatMap(project -> project.visibleConnections().stream()).toArray();
            }

            @Override
            public Object[] getChildren(Object parentElement) {
                if (parentElement instanceof DBPProject project) {
                    ProjectEntry state = snapshot.get(project);
                    if (!state.loaded()) {
                        return new Object[]{state};
                    }
                    return state.visibleConnections().toArray();
                }
                return new Object[0];
            }

            @Override
            public Object getParent(Object element) {
                return element instanceof DBPDataSourceContainer connection && groupByProjectCheck.getSelection()
                    ? connection.getProject()
                    : element instanceof ProjectEntry project ? project.project() : null;
            }

            @Override
            public boolean hasChildren(Object element) {
                if (element instanceof DBPProject project) {
                    ProjectEntry state = snapshot.get(project);
                    return !state.loaded() || !state.visibleConnections().isEmpty();
                }
                return false;
            }
        });
        viewer.addTreeListener(new ITreeViewerListener() {
            @Override
            public void treeExpanded(@NotNull TreeExpansionEvent event) {
                if (!restoringProjects && event.getElement() instanceof DBPProject project && !snapshot.get(project).loaded()) {
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
        ConnectionLabelProvider connectionLabelProvider = new ConnectionLabelProvider();
        columnController.addColumn(AIUIMessages.ai_mcp_connections_connection, null, SWT.LEFT, true, true,
            connectionLabelProvider);
        createScopeColumn();
        ColumnViewerToolTipSupport.enableFor(viewer);
        for (McpConnectionsPageRegistry.McpConnectionsPageContribution contribution : contributions) {
            McpBooleanColumn key = contribution.createColumns(viewer, columnController, this::getSettings);
            contributionColumns.add(new ContributionColumn(contribution, key));
        }
    }

    private void updateContributionVisibility() {
        for (ContributionColumn contributionColumn : contributionColumns) {
            TreeColumn column = null;
            for (int index = 0; index < viewer.getTree().getColumnCount(); index++) {
                if (columnController.getColumnData(index) == contributionColumn.key) {
                    column = viewer.getTree().getColumn(index);
                    break;
                }
            }
            if (column == null) {
                continue;
            }
            boolean visible = contributionColumn.contribution.isVisible();
            if (visible && column.getWidth() == 0) {
                column.setWidth(contributionColumn.width);
            } else if (!visible && column.getWidth() > 0) {
                contributionColumn.width = column.getWidth();
                viewer.cancelEditing();
                column.setWidth(0);
            }
            column.setResizable(visible);
        }
    }

    private void createConnectionActions(@NotNull Composite parent) {
        ToolBar actions = new ToolBar(parent, SWT.FLAT | SWT.VERTICAL);
        actions.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        UIUtils.createToolItem(actions, AIUIMessages.ai_mcp_connections_add,
            UIIcon.ROW_ADD, SelectionListener.widgetSelectedAdapter(event -> onAddConnections()));
        removeButton = UIUtils.createToolItem(actions, AIUIMessages.ai_mcp_connections_remove,
            UIIcon.ROW_DELETE, SelectionListener.widgetSelectedAdapter(event -> removeSelectedConnections()));
        viewer.addSelectionChangedListener(event -> updateRemoveButton());
        updateRemoveButton();
    }

    private final class ConnectionLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

        @Override
        public void update(@NotNull ViewerCell cell) {
            StyledString styledText = getStyledText(cell.getElement());
            cell.setText(styledText.getString());
            cell.setStyleRanges(styledText.getStyleRanges());
            cell.setImage(getImage(cell.getElement()));
            super.update(cell);
        }

        @NotNull
        public StyledString getStyledText(@Nullable Object element) {
            if (element instanceof DBPProject project) {
                ProjectEntry state = snapshot.get(project);
                StyledString text = new StyledString(project.getName());
                if (state.loaded()) {
                    text.append("    " + NLS.bind(AIUIMessages.ai_mcp_connections_count, state.count()), StyledString.QUALIFIER_STYLER);
                }
                return text;
            }
            if (element instanceof DBPDataSourceContainer connection) {
                return new StyledString(connection.getName());
            }
            return new StyledString();
        }

        @NotNull
        @Override
        public String getText(@Nullable Object element) {
            return element instanceof DBPProject project ? project.getName()
                : element instanceof DBPDataSourceContainer connection ? connection.getName() : "";
        }

        @Nullable
        @Override
        public Image getImage(@Nullable Object element) {
            if (element instanceof DBPProject project) {
                return snapshot.get(project).getIcon(projectDecorator);
            }
            return element instanceof DBPDataSourceContainer connection
                ? DBeaverIcons.getImage(connection.getDriver().getIcon()) : null;
        }

        @Nullable
        @Override
        public String getToolTipText(@Nullable Object element) {
            return element instanceof DBPDataSourceContainer connection
                ? AIUIMessages.ai_mcp_connections_driver + ": " + connection.getDriver().getName() : null;
        }
    }

    private void createScopeColumn() {
        String[] titles = new String[AIDatabaseScope.values().length];
        for (AIDatabaseScope scope : AIDatabaseScope.values()) {
            titles[scope.ordinal()] = scope.getTitle();
        }
        ColumnLabelProvider labelProvider = new ColumnLabelProvider() {
            @Override
            public @NotNull String getText(@Nullable Object element) {
                return element instanceof DBPDataSourceContainer connection
                    ? getSettings(connection).getScope().getTitle() : "";
            }

            @Nullable
            @Override
            public Image getImage(@Nullable Object element) {
                return element instanceof DBPDataSourceContainer connection
                    && getSettings(connection).getScope() == AIDatabaseScope.CUSTOM
                    ? DBeaverIcons.getImage(UIIcon.CONFIGURATION) : null;
            }

            @Nullable
            @Override
            public String getToolTipText(@Nullable Object element) {
                return element instanceof DBPDataSourceContainer connection
                    && getSettings(connection).getScope() == AIDatabaseScope.CUSTOM
                    ? AIUIMessages.ai_connection_settings_custom_scope_label : null;
            }
        };
        Tree tree = viewer.getTree();
        tree.addListener(SWT.MouseDown, event -> {
            if (event.button != 1) {
                return;
            }
            TreeItem item = tree.getItem(new Point(event.x, event.y));
            if (item == null || !(item.getData() instanceof DBPDataSourceContainer connection)
                || getSettings(connection).getScope() != AIDatabaseScope.CUSTOM) {
                return;
            }
            for (int index = 0; index < tree.getColumnCount(); index++) {
                if (AIUIMessages.ai_mcp_connections_scope.equals(columnController.getColumnName(index))
                    && item.getImageBounds(index).contains(event.x, event.y)) {
                    event.doit = false;
                    viewer.cancelEditing();
                    openCustomScope(connection);
                    return;
                }
            }
        });
        EditingSupport editingSupport = new EditingSupport(viewer) {
            private final ComboBoxCellEditor editor = new ComboBoxCellEditor(viewer.getTree(), titles, SWT.READ_ONLY);
            private DBPDataSourceContainer editingConnection;

            {
                editor.setActivationStyle(ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION
                    | ComboBoxCellEditor.DROP_DOWN_ON_KEY_ACTIVATION);
                ((CCombo) editor.getControl()).addSelectionListener(
                    SelectionListener.widgetSelectedAdapter(event -> {
                        boolean customSelected = ((CCombo) editor.getControl()).getSelectionIndex() == AIDatabaseScope.CUSTOM.ordinal();
                        DBPDataSourceContainer connection = editingConnection;
                        viewer.applyEditorValue();
                        if (customSelected && connection != null) {
                            openCustomScope(connection);
                        }
                    }));
            }

            @Override
            protected @NotNull ComboBoxCellEditor getCellEditor(@NotNull Object element) {
                editingConnection = (DBPDataSourceContainer) element;
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
                    return;
                }
                getSettings(connection).setScope(scope);
                viewer.update(element, null);
            }
        };
        columnController.addColumn(AIUIMessages.ai_mcp_connections_scope, null, SWT.LEFT, true, false,
            false, null, labelProvider, editingSupport);
    }

    private void openCustomScope(@NotNull DBPDataSourceContainer connection) {
        UIUtils.asyncExec(() -> {
            if (!viewer.getControl().isDisposed()) {
                onCustomScopeSelected(connection);
            }
        });
    }

    @NotNull
    private AIContextSettingsDataSource getSettings(@NotNull DBPDataSourceContainer connection) {
        return snapshot.get(connection.getProject()).getSettings(connection);
    }

    private void openAllProjects() {
        for (ProjectEntry project : snapshot.projects.values()) {
            try {
                project.load();
            } catch (Exception e) {
                showProjectError(e);
            }
        }
    }

    private void loadProject(@NotNull DBPProject project) {
        try {
            snapshot.get(project).load();
            refreshConnections();
            viewer.setExpandedState(project, true);
        } catch (Exception e) {
            viewer.setExpandedState(project, false);
            showProjectError(e);
        }
    }

    private void showProjectError(@NotNull Exception error) {
        DBWorkbench.getPlatformUI().showError(AIUIMessages.ai_mcp_connections_open_failed, null, error);
    }

    private void refreshConnections() {
        Object[] expanded = viewer.getExpandedElements();
        restoringProjects = true;
        try {
            viewer.setInput(snapshot.visibleProjects());
            if (groupByProjectCheck.getSelection()) {
                viewer.setExpandedElements(expanded);
            }
        } finally {
            restoringProjects = false;
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
                getSettings(connection).setMetaTransferConfirmed(false);
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
        List<DBPDataSourceContainer> added = new ArrayList<>();
        for (DBPDataSourceContainer connection : dialog.getSelectedDataSources()) {
            try {
                snapshot.get(connection.getProject()).add(connection);
                added.add(connection);
            } catch (Exception e) {
                showProjectError(e);
            }
        }
        refreshConnections();
        if (groupByProjectCheck.getSelection()) {
            for (DBPDataSourceContainer connection : added) {
                viewer.setExpandedState(connection.getProject(), true);
            }
        }
        viewer.setSelection(new StructuredSelection(added), true);
    }

    private void onCustomScopeSelected(@NotNull DBPDataSourceContainer connection) {
        DBPDataSourceContainer stored = connection.getRegistry().getDataSource(connection.getId());
        if (stored == null) {
            return;
        }
        AIChatUtils.selectCustomScope(
            getShell(), getSettings(connection), stored, dataSource -> DBUtils.getDefaultContext(dataSource, false), null,
            ids -> {
                AIContextSettingsDataSource draft = getSettings(connection);
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
            updateContributionVisibility();
            refreshConnections();
        }
    }

    private boolean saveChanges() {
        for (ProjectEntry project : snapshot.projects.values()) {
            if (!project.loaded()) {
                continue;
            }
            for (ConnectionEntry entry : project.connections.values()) {
                DBPDataSourceContainer connection = entry.connection;
                Object previous = connection.getExtension(AIContextSettingsDataSource.AI_DS_EXTENSION);
                try {
                    connection.setExtension(AIContextSettingsDataSource.AI_DS_EXTENSION, entry.settings.saveSettingsToMap());
                    if (!connection.persistConfiguration()) {
                        connection.setExtension(AIContextSettingsDataSource.AI_DS_EXTENSION, previous);
                        showSaveError(new DBException("Cannot persist AI settings for connection '" + connection.getName() + "'"));
                        return false;
                    }
                } catch (Exception e) {
                    connection.setExtension(AIContextSettingsDataSource.AI_DS_EXTENSION, previous);
                    showSaveError(e);
                    return false;
                }
            }
        }
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        boolean previousUngrouped = store.getBoolean(UNGROUPED_PREFERENCE);
        if (previousUngrouped == groupByProjectCheck.getSelection()) {
            store.setValue(UNGROUPED_PREFERENCE, !groupByProjectCheck.getSelection());
            try {
                store.save();
            } catch (IOException e) {
                store.setValue(UNGROUPED_PREFERENCE, previousUngrouped);
                showSaveError(e);
                return false;
            }
        }
        List<DBPProject> loadedProjects = snapshot.projects.values().stream()
            .filter(ProjectEntry::loaded)
            .map(ProjectEntry::project)
            .toList();
        snapshot.reload();
        for (DBPProject project : loadedProjects) {
            try {
                snapshot.get(project).load();
            } catch (Exception e) {
                viewer.setExpandedState(project, false);
                showProjectError(e);
            }
        }
        refreshConnections();
        return true;
    }

    private void showSaveError(@NotNull Exception error) {
        log.error("Cannot save AI connection settings", error);
        DBWorkbench.getPlatformUI().showError(AIUIMessages.ai_mcp_connections_save_error_message, null, error);
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
        snapshot.projects.clear();
        return super.performCancel();
    }

    private static final class ConnectionsSnapshot {
        private final Map<String, ProjectEntry> projects = new LinkedHashMap<>();

        private void reload() {
            projects.clear();
            DBWorkbench.getPlatform().getWorkspace().getVisibleProjects().stream()
                .sorted(Comparator.comparing(DBPProject::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(this::get);
        }

        @NotNull
        private ProjectEntry get(@NotNull DBPProject project) {
            return projects.computeIfAbsent(project.getId(), id ->
                new ProjectEntry(project));
        }

        @NotNull
        private ProjectEntry[] visibleProjects() {
            return projects.values().toArray(ProjectEntry[]::new);
        }
    }

    private static final class ProjectEntry {
        @NotNull
        private final DBPProject project;
        private final Map<String, ConnectionEntry> connections = new LinkedHashMap<>();
        @Nullable
        private Image icon;
        private boolean loaded;

        private ProjectEntry(@NotNull DBPProject project) {
            this.project = project;
        }

        @NotNull
        private DBPProject project() {
            return project;
        }

        private boolean loaded() {
            return loaded;
        }

        @NotNull
        private AIContextSettingsDataSource getSettings(@NotNull DBPDataSourceContainer connection) {
            return connections.computeIfAbsent(connection.getId(), id -> new ConnectionEntry(connection)).settings;
        }

        private void add(@NotNull DBPDataSourceContainer connection) throws DBException {
            load();
            getSettings(connection).setMetaTransferConfirmed(true);
        }

        private void load() throws DBException {
            if (loaded) {
                return;
            }
            boolean retry = project.isRegistryLoaded();
            project.ensureOpen();
            DBPDataSourceRegistry registry = project.getDataSourceRegistry();
            if (retry && registry.hasError()) {
                registry.refreshConfig();
            }
            if (registry.hasError()) {
                throw new DBException("Cannot load AI connections for project '" + project.getName() + "'");
            }
            readConnections(registry);
        }

        private void readConnections(@NotNull DBPDataSourceRegistry registry) {
            Map<String, ConnectionEntry> loadedConnections = new LinkedHashMap<>();
            for (DBPDataSourceContainer connection : registry.getDataSources()) {
                ConnectionEntry existing = connections.get(connection.getId());
                loadedConnections.put(connection.getId(), existing == null ? new ConnectionEntry(connection) : existing);
            }
            connections.clear();
            connections.putAll(loadedConnections);
            loaded = true;
        }

        @NotNull
        private Image getIcon(@Nullable ILabelDecorator decorator) {
            if (icon == null || icon.isDisposed()) {
                DBNRoot root = DBWorkbench.getPlatform().getNavigatorModel().getRoot();
                DBNProject node = root == null ? null : root.getProjectNode(project);
                Image base = DBeaverIcons.getImage(node == null ? DBIcon.PROJECT : node.getNodeIconDefault());
                Image decorated = node == null || decorator == null ? null : decorator.decorateImage(base, node);
                icon = decorated == null ? base : decorated;
            }
            return icon;
        }

        private int count() {
            return (int) connections.values().stream()
                .filter(connection -> connection.settings.isMetaTransferConfirmed())
                .count();
        }

        @NotNull
        private List<DBPDataSourceContainer> visibleConnections() {
            return connections.values().stream()
                .filter(connection -> connection.settings.isMetaTransferConfirmed())
                .map(connection -> connection.connection)
                .sorted(Comparator.comparing(DBPDataSourceContainer::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        }
    }

    private static final class ConnectionEntry {
        @NotNull
        private final DBPDataSourceContainer connection;
        @NotNull
        private final AIContextSettingsDataSource settings;

        private ConnectionEntry(@NotNull DBPDataSourceContainer connection) {
            this.connection = connection;
            this.settings = new AIContextSettingsDataSource(connection);
        }
    }

    private static final class ContributionColumn {
        @NotNull
        private final McpConnectionsPageRegistry.McpConnectionsPageContribution contribution;
        @NotNull
        private final McpBooleanColumn key;
        private int width = 100;

        private ContributionColumn(
            @NotNull McpConnectionsPageRegistry.McpConnectionsPageContribution contribution,
            @NotNull McpBooleanColumn key
        ) {
            this.contribution = contribution;
            this.key = key;
        }
    }

    public static final class McpConnectionsPageRegistry {
        private static final String EXTENSION_ID = "org.jkiss.dbeaver.ui.ai.mcpConnectionsPage";
        private static final Log log = Log.getLog(McpConnectionsPageRegistry.class);

        private McpConnectionsPageRegistry() {
        }

        /** Adds product-specific access columns to the common connections page. */
        public interface McpConnectionsPageContribution {
            /** Whether this provider's columns should appear in the current global configuration. */
            boolean isVisible();

            /** Notify the page when the provider's global visibility changes. */
            void addVisibilityListener(@NotNull Runnable listener);

            void removeVisibilityListener(@NotNull Runnable listener);

            /** Project group rows are present unless the user selects the ungrouped view. */
            @NotNull
            McpBooleanColumn createColumns(
                @NotNull TreeViewer viewer,
                @NotNull ViewerColumnController<McpBooleanColumn, Object> columnController,
                @NotNull Function<DBPDataSourceContainer, AIContextSettingsDataSource> settings
            );
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
