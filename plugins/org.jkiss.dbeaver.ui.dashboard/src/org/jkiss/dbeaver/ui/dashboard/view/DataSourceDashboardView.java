/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dashboard.view;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIExecutionQueue;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPainter;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardListViewer;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;
import org.jkiss.dbeaver.ui.dashboard.model.*;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DataSourceDashboardView extends ViewPart implements DashboardViewer, DBPDataSourceContainerProvider, DBPEventListener {
    public static final String VIEW_ID = "org.jkiss.dbeaver.ui.dashboardView";

    static protected final Log log = Log.getLog(DataSourceDashboardView.class);

    private DashboardListViewer dashboardListViewer;
    private DashboardConfigurationList configurationList;
    private DashboardConfiguration configuration;
    private DBPDataSourceContainer dataSourceContainer;
    private DBPProject project;
    private String dashboardId;

    public static DataSourceDashboardView openView(
        @NotNull IWorkbenchWindow workbenchWindow,
        @NotNull DBPProject project,
        @Nullable DBPDataSourceContainer dataSourceContainer,
        @Nullable String id) {
        try {
            return (DataSourceDashboardView) workbenchWindow.getActivePage().showView(
                DataSourceDashboardView.VIEW_ID,
                DashboardConfiguration.getViewId(project, dataSourceContainer, id),
                IWorkbenchPage.VIEW_ACTIVATE);
        } catch (PartInitException e) {
            DBWorkbench.getPlatformUI().showError(
                UIDashboardMessages.error_dashboard_view_cannot_open_title,
                UIDashboardMessages.error_dashboard_view_cannot_open_msg,
                e);
        }
        return null;
    }

    public DataSourceDashboardView() {
        super();
    }

    @Override
    public DashboardConfigurationList getConfigurationList() {
        return configurationList;
    }

    @Override
    public DashboardConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return configuration == null ? null : configuration.getDataSourceContainer();
    }

    @Override
    public void createPartControl(Composite parent) {
        UIExecutionQueue.queueExec(() -> createDashboardControls(parent));
    }

    private void createDashboardControls(Composite parent) {
        ProgressPainter dashboardProgressPainter = new ProgressPainter(parent);

        try {
            String secondaryId = getViewSite().getSecondaryId();
            if (CommonUtils.isEmpty(secondaryId)) {
                throw new IllegalStateException("Dashboard view requires active database connection");
            }
            String projectName = null;
            String datasourceId = null;
            dashboardId = null;
            if (secondaryId.startsWith(DashboardConfiguration.REF_PREFIX)) {
                String[] params = secondaryId.substring(DashboardConfiguration.REF_PREFIX.length()).split(",");
                for (String param : params) {
                    int divPos = param.indexOf("=");
                    if (divPos < 0) {
                        log.debug("Invalid dashboard parameter '" + param + "'");
                        continue;
                    }
                    DashboardConfiguration.Parameter dp = CommonUtils.valueOf(
                        DashboardConfiguration.Parameter.class,
                        param.substring(0, divPos),
                        null);
                    String value = param.substring(divPos + 1);
                    switch (dp) {
                        case project -> projectName = value;
                        case id -> dashboardId = value;
                        case datasource -> datasourceId = value;
                    }
                }
            } else {
                // Legacy, backward compatibility
                String[] idParts = secondaryId.split("/");
                if (idParts.length == 1) {
                    datasourceId = idParts[0];
                } else {
                    projectName = idParts[0];
                    datasourceId = idParts[1];
                }
            }
            if (CommonUtils.isEmpty(dashboardId) && CommonUtils.isEmpty(projectName)) {
                throw new IllegalStateException("Bad dashboard view ID: " + secondaryId);
            }

            if (CommonUtils.isEmpty(projectName)) {
                dataSourceContainer = DBUtils.findDataSource(null, datasourceId);
                if (datasourceId == null) {
                    throw new IllegalStateException("Invalid datasource ID: " + datasourceId);
                }
                project = dataSourceContainer.getProject();
            } else {
                project = DBWorkbench.getPlatform().getWorkspace().getProject(projectName);
                if (project == null) {
                    throw new IllegalStateException("Invalid project name: " + projectName);
                }
                if (!CommonUtils.isEmpty(datasourceId)) {
                    dataSourceContainer = project.getDataSourceRegistry().getDataSource(datasourceId);
                }
            }

            if (dataSourceContainer != null) {
                dataSourceContainer.getRegistry().addDataSourceListener(this);

                configurationList = new DashboardConfigurationList(dataSourceContainer);
                configurationList.checkDefaultDashboardExistence();
                if (CommonUtils.isEmpty(dashboardId)) {
                    dashboardId = configurationList.getDashboards().get(0).getDashboardId();
                }

                configuration = configurationList.getDashboard(dashboardId);
                if (configuration == null) {
                    dashboardId = DashboardConfigurationList.DEFAULT_DASHBOARD_ID;
                    configuration = configurationList.getDashboard(dashboardId);
                }

                updateStatus();

                dashboardListViewer = new DashboardListViewer(getSite(), this, configurationList, configuration);
                dashboardListViewer.createControl(parent);

                dashboardListViewer.createDashboardsFromConfiguration();

                getSite().setSelectionProvider(dashboardListViewer);
            } else {
                updateStatus();
            }

            parent.layout(true, true);

            dashboardProgressPainter.close();
        } catch (Throwable e) {
            log.error("Error initializing dashboard view", e);
        }
    }

    @Override
    public void setFocus() {
        if (dashboardListViewer != null) {
            DashboardGroupContainer group = dashboardListViewer.getDefaultGroup();
            if (group != null) {
                List<? extends DashboardItemContainer> items = group.getItems();
                if (!items.isEmpty()) {
                    group.selectItem(items.get(0));
                }
            }
        }
    }

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);

    }

    @Override
    public void dispose() {
        super.dispose();

        if (dashboardListViewer != null) {
            if (dataSourceContainer != null) {
                dataSourceContainer.getRegistry().removeDataSourceListener(this);
            }
            dashboardListViewer.dispose();
            dashboardListViewer = null;
        }
    }

    @Override
    public void handleDataSourceEvent(DBPEvent event) {
        if (event.getObject() != dataSourceContainer) {
            return;
        }
        switch (event.getAction()) {
            case OBJECT_UPDATE:
            case OBJECT_REMOVE:
                UIUtils.asyncExec(this::updateStatus);
                break;
        }
    }

    @Override
    public void updateStatus() {
        if (configuration == null) {
            return;
        }
        String partName;
        if (DashboardConfigurationList.DEFAULT_DASHBOARD_NAME.equals(configuration.getDashboardName())) {
            if (dataSourceContainer != null) {
                partName = dataSourceContainer.getName() + (dataSourceContainer.isConnected() ? "" : UIDashboardMessages.dashboard_view_status_off);
            } else {
                partName = project.getName() + ":" + dashboardId;
            }
        } else {
            partName = configuration.getDashboardName();
        }
        if (dataSourceContainer != null) {
            if (dataSourceContainer.isConnected()) {
                DashboardUpdateJob.getDefault().resumeDashboardUpdate();
            }
        }
        if (dataSourceContainer != null) {
            setTitleToolTip("Connection: " + dataSourceContainer.getName() + " (" + dataSourceContainer.getDriver().getFullName() + ")");
        }
        UIUtils.syncExec(() -> setPartName(partName));
    }


    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
    }

    @Override
    public DashboardListViewer getDashboardListViewer() {
        return dashboardListViewer;
    }


}
