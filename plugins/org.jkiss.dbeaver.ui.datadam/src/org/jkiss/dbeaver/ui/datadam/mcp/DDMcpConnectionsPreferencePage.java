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

import com.dbeaver.datadam.dam.remote.api.DDRemoteDamSettingKeys;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSettings;
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIContextSettingsDataSource;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.DDWorkspace;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.mcp.DDMcpConnectionCatalog;
import org.jkiss.dbeaver.model.datadam.mcp.DDMcpConnectionCatalog.AvailableConnection;
import org.jkiss.dbeaver.model.datadam.mcp.DDMcpScopeUtils;
import org.jkiss.dbeaver.model.datadam.rpc.DDRemoteDamDesktopSessionService;
import org.jkiss.dbeaver.model.datadam.rpc.DDRemoteDamSettingsStore;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.preferences.PrefPageDataSourceAI;
import org.jkiss.dbeaver.ui.controls.CenteredImageLabelProvider;
import org.jkiss.dbeaver.ui.datadam.DDDesktopDamService;
import org.jkiss.dbeaver.ui.datadam.DDUIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDMcpMessages;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIIcons;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.dialogs.SelectDataSourceDialog;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.net.URI;
import java.util.*;
import java.util.List;

public class DDMcpConnectionsPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.synchronization.mcp.connections";
    private static final int MIN_ROWS = 10;
    private static final int MAX_ROWS = 20;
    private static final Log log = Log.getLog(DDMcpConnectionsPreferencePage.class);
    private static final String MCP_SETTINGS_URL = "remote-dams";
    private static final String MCP_DOCS_URL = "https://docs.datadam.cloud/MCP";

    private final DDMcpConnectionCatalog catalog = DDMcpConnectionCatalog.getInstance();
    private final DDWorkspace workspace = DDWorkspace.getInstance();
    private final DDDesktopAccessService accessService = DDDesktopAccessService.getInstance();
    private final DDRemoteDamSettingsStore settingsStore = DDRemoteDamSettingsStore.getInstance();
    private final Set<DBPDataSourceContainer> savedConnections = new LinkedHashSet<>();
    private final Set<DBPDataSourceContainer> selectedConnections = new LinkedHashSet<>();
    private final Set<DBPProject> loadedProjects = new LinkedHashSet<>();
    private final List<DBPProject> projects = new ArrayList<>();

    private TableViewer connectionViewer;
    private TableViewer projectViewer;
    private Button addButton;
    private Button configureButton;
    private Button removeButton;
    private Button mcpCheck;
    private Control loginHint;
    private Control loadingHint;
    private Control unavailableHint;
    private Control globallyDisabledHint;
    private Composite accessInfo;
    @Nullable
    private AbstractJob mcpJob;
    @Nullable
    private DDAccountClient.DamSettingsState mcpPermissions;
    private boolean mcpLoading;
    private boolean editable;
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
        ((GridLayout) content.getLayout()).verticalSpacing = 4;
        Label description = DDUIUtils.create(content, DDMcpMessages.mcp_connections_description,
            DDMcpMessages.mcp_connections_documentation, MCP_DOCS_URL);
        GridLayout descriptionLayout = (GridLayout) description.getParent().getLayout();
        descriptionLayout.marginHeight = 0;
        descriptionLayout.verticalSpacing = 0;
        createMcpControls(content);

        Composite columns = new Composite(content, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        columns.setLayout(layout);
        GridData columnsData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        columnsData.verticalIndent = 8;
        columns.setLayoutData(columnsData);

        createHeaders(columns);
        createProjects(columns);
        createConnections(columns);
        connectionViewer.setInput(List.of());
        projectViewer.setInput(projects);
        refreshConnections();
        subscribeToSettings(content);
        loadMcpPermissions();

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

    private void createMcpControls(@NotNull Composite content) {
        Composite controls = UIUtils.createPlaceholder(content, 1);
        GridData controlsData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        controlsData.verticalIndent = 3;
        controls.setLayoutData(controlsData);
        mcpCheck = UIUtils.createCheckbox(controls,
            DDTrackingUIMessages.sync_preference_page_mcp_enabled,
            DDTrackingUIMessages.sync_preference_page_mcp_hint, false, 1);
        mcpCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateMcpStatus(mcpCheck.getSelection())));
        loginHint = createStatusLabel(controls, DDMcpMessages.mcp_connections_login_required);
        loadingHint = createStatusLabel(controls, DDTrackingUIMessages.sync_preference_page_mcp_loading);
        unavailableHint = createStatusLabel(controls, DDTrackingUIMessages.sync_preference_page_mcp_unavailable);
        globallyDisabledHint = DDUIUtils.create(
            controls, DDTrackingUIMessages.sync_preference_page_mcp_globally_disabled,
            DDTrackingUIMessages.sync_preference_page_mcp_settings_link,
            URI.create(DDEndpoints.getAccountBaseUrl()).resolve("/" + MCP_SETTINGS_URL).toString()
        ).getParent();
        globallyDisabledHint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    @NotNull
    private Label createStatusLabel(@NotNull Composite parent, @NotNull String message) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(message);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return label;
    }

    private void subscribeToSettings(@NotNull Composite content) {
        Runnable refresh = () -> UIUtils.asyncExec(() -> {
            if (!mcpCheck.isDisposed()) {
                showMcpStatus();
            }
        });
        DDRemoteDamSettingsStore.Listener listener = settings -> refresh.run();
        DDDesktopAccessListener accessListener = new DDDesktopAccessListener() {
            @Override
            public void accessChanging() {
            }

            @Override
            public void accessChanged() {
                UIUtils.asyncExec(() -> {
                    if (!mcpCheck.isDisposed()) {
                        loadMcpPermissions();
                        refreshConnections();
                    }
                });
            }
        };
        settingsStore.addListener(listener);
        accessService.addAccessListener(accessListener);
        content.addDisposeListener(e -> {
            if (mcpJob != null) {
                mcpJob.cancel();
            }
            settingsStore.removeListener(listener);
            accessService.removeAccessListener(accessListener);
        });
    }

    private void loadMcpPermissions() {
        if (mcpJob != null) {
            mcpJob.cancel();
        }
        mcpPermissions = null;
        mcpLoading = false;
        if (accessService.getStatus() != DDDesktopAccessService.Status.SIGNED_IN
            || !DDProductConfig.isIntegrationEnabled()) {
            showMcpStatus();
            return;
        }
        mcpLoading = true;
        showMcpStatus();
        mcpJob = DDDesktopDamService.getInstance().createJob(
            "Read DataDam MCP permissions",
            account -> account.getDamSettings(DDRemoteDamDesktopSessionService.getInstance().getClientDamId()),
            state -> {
                if (!mcpCheck.isDisposed() && accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN) {
                    mcpPermissions = state;
                    mcpLoading = false;
                    showMcpStatus();
                }
            },
            error -> {
                if (!mcpCheck.isDisposed() && accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN) {
                    log.debug("Cannot read DataDam MCP permissions", error);
                    mcpLoading = false;
                    showMcpStatus();
                }
            });
    }

    private void updateMcpStatus(boolean enabled) {
        DDAccountClient.DamSettingsState previous = mcpPermissions;
        if (previous == null || !mcpCheck.isEnabled()) {
            return;
        }
        DDRemoteDamSettings before = settingsStore.getSettings();
        mcpLoading = true;
        showMcpStatus();
        mcpJob = DDDesktopDamService.getInstance().createJob(
            "Update DataDam MCP status",
            account -> account.setDamSetting(
                DDRemoteDamDesktopSessionService.getInstance().getClientDamId(), DDRemoteDamSettingKeys.MCP_ENABLED,
                Boolean.toString(enabled), previous.accountId()),
            updated -> {
                if (!mcpCheck.isDisposed() && accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN
                    && mcpPermissions == previous) {
                    mcpLoading = false;
                    if (settingsStore.getSettings() == before) {
                        settingsStore.updateSettings(updated);
                    }
                    showMcpStatus();
                }
            },
            error -> {
                if (!mcpCheck.isDisposed() && accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN
                    && mcpPermissions == previous) {
                    mcpLoading = false;
                    showMcpStatus();
                    DBWorkbench.getPlatformUI().showError(
                        DDMcpMessages.mcp_connections_title,
                        DDTrackingUIMessages.sync_preference_page_mcp_update_failed, error);
                }
            });
    }

    private void showMcpStatus() {
        boolean signedIn = accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN;
        boolean supported = DDProductConfig.isIntegrationEnabled();
        DDRemoteDamSettings settings = settingsStore.getSettings();
        boolean accountEnabled = settings != null && settings.isAccountEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, true);
        boolean selected = signedIn && supported && settings != null && accountEnabled
            && settings.isDamEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, false);
        mcpCheck.setSelection(selected);
        mcpCheck.setEnabled(signedIn && supported && accountEnabled
            && mcpPermissions != null && mcpPermissions.canManage() && !mcpLoading);
        editable = isEditingAllowed();
        connectionViewer.getTable().setEnabled(editable);
        accessInfo.setVisible(editable);
        ((GridData) accessInfo.getLayoutData()).exclude = !editable;
        showHint(loginHint, !signedIn);
        showHint(loadingHint, signedIn && mcpLoading);
        showHint(unavailableHint, signedIn && !mcpLoading && (!supported || settings == null || mcpPermissions == null));
        showHint(globallyDisabledHint, signedIn && supported && settings != null && !accountEnabled);
        updateButtons();
        mcpCheck.getParent().getParent().layout(true, true);
    }

    private void showHint(@NotNull Control hint, boolean visible) {
        hint.setVisible(visible);
        ((GridData) hint.getLayoutData()).exclude = !visible;
    }

    private boolean isEditingAllowed() {
        DDRemoteDamSettings settings = settingsStore.getSettings();
        return accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN
            && DDProductConfig.isIntegrationEnabled() && !mcpLoading
            && mcpPermissions != null && mcpPermissions.canManage()
            && settings != null && settings.isEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, true, false);
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

        accessInfo = UIUtils.createComposite(tableRow, 2);
        accessInfo.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
        Label infoIcon = new Label(accessInfo, SWT.NONE);
        infoIcon.setImage(DBeaverIcons.getImage(DBIcon.SMALL_INFO));
        Label infoText = new Label(accessInfo, SWT.WRAP);
        infoText.setText(DDMcpMessages.mcp_connections_accessible_tip);
        GridData infoData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        infoData.widthHint = 540;
        infoText.setLayoutData(infoData);
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

        TableViewerColumn scopeColumn = new TableViewerColumn(connectionViewer, SWT.LEFT);
        scopeColumn.getColumn().setText(DDMcpMessages.mcp_connections_scope);
        scopeColumn.getColumn().setWidth(150);
        scopeColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(@NotNull Object element) {
                DBPDataSourceContainer connection = ((AvailableConnection) element).connection();
                AIDatabaseScope scope = DDMcpScopeUtils.getScope(
                    new AIContextSettingsDataSource(connection), connection, DBUtils.getDefaultContext(connection, true));
                return scope == null ? "" : scope.getTitle();
            }
        });

        TableViewerColumn accessible = new TableViewerColumn(connectionViewer, SWT.LEFT);
        accessible.getColumn().setText(DDMcpMessages.mcp_connections_accessible);
        accessible.getColumn().setWidth(140);
        accessible.getColumn().setToolTipText(DDMcpMessages.mcp_connections_accessible_tip);
        accessible.setLabelProvider(new CenteredImageLabelProvider() {
            @Override
            @NotNull
            protected Image getImage(@NotNull Object element) {
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
        addButton.setEnabled(editable && selectedProject() != null);
        configureButton.setEnabled(editable && connectionViewer.getStructuredSelection().size() == 1);
        removeButton.setEnabled(editable && !connectionViewer.getStructuredSelection().isEmpty());
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
        if (!isEditingAllowed()) {
            return true;
        }
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
