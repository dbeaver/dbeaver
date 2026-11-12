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
package org.jkiss.dbeaver.ui.datadam.mcp;

import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.DDWorkspace;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.mcp.DDMcpConnectionCatalog;
import org.jkiss.dbeaver.model.datadam.mcp.DDMcpConnectionCatalog.AvailableConnection;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.preferences.PrefPageDataSourceAI;
import org.jkiss.dbeaver.ui.datadam.internal.DDMcpMessages;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIIcons;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.dialogs.SelectDataSourceDialog;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.util.*;
import java.util.List;

public class DDMcpConnectionsPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.synchronization.mcp.connections";
    private static final int MIN_ROWS = 10;
    private static final int MAX_ROWS = 20;

    private final DDMcpConnectionCatalog catalog = DDMcpConnectionCatalog.getInstance();
    private final DDWorkspace workspace = DDWorkspace.getInstance();
    private final Set<DBPDataSourceContainer> savedConnections = new LinkedHashSet<>();
    private final Set<DBPDataSourceContainer> selectedConnections = new LinkedHashSet<>();
    private final Set<DBPProject> loadedProjects = new LinkedHashSet<>();
    private final List<DBPProject> projects = new ArrayList<>();

    private TableViewer connectionViewer;
    private TableViewer projectViewer;
    private Button addButton;
    private Button configureButton;
    private Button removeButton;
    private boolean updatingProjects;
    @Nullable
    private UUID displayedAccountId;
    @Nullable
    private DBPDataSourceContainer requestedConnection;

    public DDMcpConnectionsPreferencePage() {
        noDefaultButton();
    }

    @Override
    public void init(@NotNull IWorkbench workbench) {
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite content = UIUtils.createPlaceholder(parent, 1);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite columns = new Composite(content, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        columns.setLayout(layout);
        columns.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        createHeaders(columns);
        createProjects(columns);
        createConnections(columns);
        connectionViewer.setInput(List.of());
        projectViewer.setInput(projects);
        refreshConnections();

        if (requestedConnection != null) {
            selectConnection(requestedConnection);
        } else {
            DBPProject activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
            if (activeProject != null && projects.contains(activeProject)) {
                projectViewer.setSelection(new StructuredSelection(activeProject), true);
            }
        }
        return content;
    }

    @Override
    public void applyData(@Nullable Object data) {
        super.applyData(data);
        if (data instanceof DBPDataSourceContainer connection) {
            requestedConnection = connection;
            if (projectViewer != null && !projectViewer.getControl().isDisposed()) {
                selectConnection(connection);
            }
        }
    }

    private void selectConnection(@NotNull DBPDataSourceContainer connection) {
        DBPProject project = connection.getProject();
        if (!projects.contains(project)) {
            projectViewer.setSelection(StructuredSelection.EMPTY);
            return;
        }
        loadProject(project);
        projectViewer.setSelection(new StructuredSelection(project), true);
        for (TableItem item : connectionViewer.getTable().getItems()) {
            if (item.getData() instanceof AvailableConnection available
                && available.connection().getId().equals(connection.getId())) {
                connectionViewer.setSelection(new StructuredSelection(available), true);
                break;
            }
        }
    }

    private void createHeaders(@NotNull Composite parent) {
        Composite projectsHeader = UIUtils.createComposite(parent, 1);
        projectsHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        new Label(projectsHeader, SWT.NONE).setText(DDMcpMessages.mcp_connections_projects);

        Composite connectionsHeader = UIUtils.createComposite(parent, 3);
        connectionsHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        new Label(connectionsHeader, SWT.NONE).setText(DDMcpMessages.mcp_connections_connections);
        Button refreshButton = iconButton(connectionsHeader, UIIcon.REFRESH,
            DDMcpMessages.mcp_connections_refresh, this::refreshConnections);
        refreshButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
        addButton = iconButton(connectionsHeader, DDTrackingUIIcons.SHARE_PROJECT,
            DDMcpMessages.mcp_connections_add, this::addConnection);
    }

    private void createConnections(@NotNull Composite parent) {
        Composite tableRow = new Composite(parent, SWT.NONE);
        GridLayout rowLayout = new GridLayout(2, false);
        rowLayout.marginWidth = 0;
        rowLayout.marginHeight = 0;
        tableRow.setLayout(rowLayout);
        tableRow.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        connectionViewer = new TableViewer(tableRow, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        Table table = connectionViewer.getTable();
        table.setHeaderVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        connectionViewer.setContentProvider(ArrayContentProvider.getInstance());
        addColumns();
        ColumnViewerToolTipSupport.enableFor(connectionViewer);
        connectionViewer.addSelectionChangedListener(e -> updateButtons());

        Composite actions = UIUtils.createComposite(tableRow, 1);
        actions.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
        configureButton = iconButton(actions, UIIcon.CONFIGURATION,
            DDMcpMessages.mcp_connections_configure_scope, this::configureScope);
        removeButton = iconButton(actions, DDTrackingUIIcons.PROJECT_DISCONNECT,
            DDMcpMessages.mcp_connections_remove, this::removeConnections);
    }

    private void createProjects(@NotNull Composite parent) {
        projectViewer = new TableViewer(parent, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
        Table table = projectViewer.getTable();
        GridData layout = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        layout.widthHint = 210;
        table.setLayoutData(layout);
        projectViewer.setContentProvider(ArrayContentProvider.getInstance());
        projectViewer.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(@NotNull Object element) {
                return ((DBPProject) element).getName();
            }

            @Override
            public Image getImage(@NotNull Object element) {
                return DBeaverIcons.getImage(DBIcon.PROJECT);
            }
        });
        projectViewer.addSelectionChangedListener(e -> {
            if (!updatingProjects) {
                DBPProject project = selectedProject();
                if (project != null) {
                    loadProject(project);
                }
                showConnections();
            }
        });
    }

    private void addColumns() {
        TableViewerColumn name = new TableViewerColumn(connectionViewer, SWT.LEFT);
        name.getColumn().setText(DDMcpMessages.mcp_connections_name);
        name.getColumn().setWidth(250);
        name.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(@NotNull Object element) {
                return ((AvailableConnection) element).connection().getName();
            }
        });

        TableViewerColumn driver = new TableViewerColumn(connectionViewer, SWT.LEFT);
        driver.getColumn().setText(DDMcpMessages.mcp_connections_driver);
        driver.getColumn().setWidth(150);
        driver.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(@NotNull Object element) {
                return ((AvailableConnection) element).connection().getDriver().getName();
            }
        });

        TableViewerColumn accessible = new TableViewerColumn(connectionViewer, SWT.LEFT);
        accessible.getColumn().setText(DDMcpMessages.mcp_connections_accessible);
        accessible.getColumn().setWidth(140);
        accessible.getColumn().setToolTipText(DDMcpMessages.mcp_connections_accessible_tip);
        accessible.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(@NotNull Object element) {
                return "";
            }

            @Override
            public Image getImage(@NotNull Object element) {
                AvailableConnection connection = (AvailableConnection) element;
                return DBeaverIcons.getImage(connection.requiresManualConnection()
                    ? DBIcon.SMALL_WARNING : UIIcon.OK_MARK);
            }

            @Override
            public String getToolTipText(@NotNull Object element) {
                AvailableConnection connection = (AvailableConnection) element;
                return connection.requiresManualConnection()
                    ? DDMcpMessages.mcp_connections_manual_tip : DDMcpMessages.mcp_connections_ready_tip;
            }
        });
    }

    @NotNull
    private Button iconButton(@NotNull Composite parent, @NotNull DBPImage icon,
        @NotNull String tooltip, @NotNull Runnable action) {
        Button button = new Button(parent, SWT.PUSH);
        button.setImage(DBeaverIcons.getImage(icon));
        button.setToolTipText(tooltip);
        button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> action.run()));
        return button;
    }

    @Nullable
    private DBPProject selectedProject() {
        return (DBPProject) projectViewer.getStructuredSelection().getFirstElement();
    }

    private void addConnection() {
        DBPProject project = selectedProject();
        if (project == null) {
            return;
        }
        SelectDataSourceDialog dialog = new SelectDataSourceDialog(getShell(), project, null, false) {
            @NotNull
            @Override
            protected Composite createDialogArea(@NotNull Composite parent) {
                Composite area = super.createDialogArea(parent);
                DatabaseNavigatorTree tree = DatabaseNavigatorTree.getFromShell(getShell());
                tree.getViewer().addFilter(new ViewerFilter() {
                    @Override
                    public boolean select(@NotNull Viewer viewer, @Nullable Object parent, @NotNull Object element) {
                        if (element instanceof DBNDataSource dataSource) {
                            for (TableItem item : connectionViewer.getTable().getItems()) {
                                if (((AvailableConnection) item.getData()).connection().getId()
                                    .equals(dataSource.getObject().getId())) {
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                });
                return area;
            }

            @Override
            protected void initializeBounds() {
                super.initializeBounds();
                Shell shell = getShell();
                shell.setSize(Math.max(shell.getSize().x, 600), shell.getSize().y);
            }
        };
        if (dialog.open() == Window.OK && dialog.getDataSource() != null) {
            selectedConnections.add(dialog.getDataSource());
            showConnections();
        }
    }

    private void removeConnections() {
        for (Object item : connectionViewer.getStructuredSelection()) {
            selectedConnections.remove(((AvailableConnection) item).connection());
        }
        showConnections();
    }

    private void configureScope() {
        IStructuredSelection selection = connectionViewer.getStructuredSelection();
        if (selection.size() != 1) {
            return;
        }
        UIServiceConnections service = DBWorkbench.getService(UIServiceConnections.class);
        if (service != null) {
            AvailableConnection connection = (AvailableConnection) selection.getFirstElement();
            service.openConnectionEditor(connection.connection(), PrefPageDataSourceAI.class.getName());
            if (!connectionViewer.getControl().isDisposed()) {
                refreshConnections();
            }
        }
    }

    private void refreshConnections() {
        UUID accountId = DDDesktopAccessService.getInstance().getAccountId();
        if (!Objects.equals(displayedAccountId, accountId)) {
            displayedAccountId = accountId;
            savedConnections.clear();
            selectedConnections.clear();
            loadedProjects.clear();
        }
        loadedProjects.retainAll(workspace.getProjects());
        Set<DBPDataSourceContainer> additions = new LinkedHashSet<>(selectedConnections);
        additions.removeAll(savedConnections);
        Set<DBPDataSourceContainer> removals = new LinkedHashSet<>(savedConnections);
        removals.removeAll(selectedConnections);

        savedConnections.clear();
        for (DBPProject project : loadedProjects) {
            for (AvailableConnection connection : catalog.getMCPConnections(project)) {
                savedConnections.add(connection.connection());
            }
        }
        selectedConnections.clear();
        selectedConnections.addAll(savedConnections);
        selectedConnections.addAll(additions);
        selectedConnections.removeAll(removals);
        showProjects();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && projectViewer != null && !projectViewer.getControl().isDisposed()) {
            refreshConnections();
        }
    }

    private void loadProject(@NotNull DBPProject project) {
        if (loadedProjects.contains(project) && project.isOpen()
            && (project.isRegistryLoaded() || !project.isPasswordProtected())) {
            return;
        }
        try {
            project.ensureOpen();
            project.getDataSourceRegistry();
            loadedProjects.add(project);
            refreshConnections();
        } catch (RuntimeException e) {
            DBWorkbench.getPlatformUI().showError(
                DDMcpMessages.mcp_connections_title, DDMcpMessages.mcp_connections_open_failed, e);
        }
    }

    private void showProjects() {
        DBPProject selected = selectedProject();
        projects.clear();
        projects.addAll(workspace.getProjects().stream()
            .sorted(Comparator.comparing(DBPProject::getName, String.CASE_INSENSITIVE_ORDER)).toList());
        updatingProjects = true;
        try {
            projectViewer.refresh();
            projectViewer.setSelection(selected != null && projects.contains(selected)
                ? new StructuredSelection(selected) : StructuredSelection.EMPTY);
        } finally {
            updatingProjects = false;
        }
        showConnections();
    }

    private void showConnections() {
        DBPProject project = selectedProject();
        List<AvailableConnection> connections = project == null ? List.of() : selectedConnections.stream()
            .filter(connection -> connection.getProject() == project)
            .map(catalog::getAvailableConnection)
            .sorted(Comparator.comparing(connection -> connection.connection().getName(), String.CASE_INSENSITIVE_ORDER))
            .toList();
        connectionViewer.setInput(connections);
        updateTableHeight();
        updateButtons();
    }

    private void updateButtons() {
        addButton.setEnabled(selectedProject() != null);
        configureButton.setEnabled(connectionViewer.getStructuredSelection().size() == 1);
        removeButton.setEnabled(!connectionViewer.getStructuredSelection().isEmpty());
    }

    private void updateTableHeight() {
        Table table = connectionViewer.getTable();
        int rows = Math.max(MIN_ROWS, Math.min(table.getItemCount(), MAX_ROWS));
        int height = table.getHeaderHeight() + rows * table.getItemHeight() + 2 * table.getBorderWidth();
        GridData connectionLayout = (GridData) table.getLayoutData();
        GridData projectLayout = (GridData) projectViewer.getTable().getLayoutData();
        if (connectionLayout.heightHint != height) {
            connectionLayout.heightHint = height;
            projectLayout.heightHint = height;
            Composite columns = table.getParent().getParent();
            columns.layout(true, true);
            columns.getParent().layout(true, true);
        }
    }

    private boolean saveChanges() {
        if (!Objects.equals(displayedAccountId, DDDesktopAccessService.getInstance().getAccountId())) {
            refreshConnections();
        }
        for (DBPProject project : loadedProjects) {
            for (DBPDataSourceContainer connection : project.getDataSourceRegistry().getDataSources()) {
                boolean enabled = selectedConnections.contains(connection);
                if (enabled != catalog.isEnabled(connection)) {
                    catalog.setEnabled(connection, enabled);
                    try {
                        project.getDataSourceRegistry().updateDataSource(connection, false);
                    } catch (DBException e) {
                        catalog.setEnabled(connection, !enabled);
                        DBWorkbench.getPlatformUI().showError(
                            DDMcpMessages.mcp_connections_title, DDMcpMessages.mcp_connections_save_failed, e);
                        refreshConnections();
                        return false;
                    }
                }
            }
        }
        refreshConnections();
        return true;
    }

    @Override
    protected void performApply() {
        saveChanges();
    }

    @Override
    public boolean performOk() {
        return saveChanges() && super.performOk();
    }
}
