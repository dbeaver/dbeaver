/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIExecutionQueue;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardListViewer;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardGroupContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewConfiguration;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DashboardView extends ViewPart implements IDataSourceContainerProvider, DBPEventListener {
    public static final String VIEW_ID = "org.jkiss.dbeaver.ui.dashboardView";

    static protected final Log log = Log.getLog(DashboardView.class);

    private DashboardListViewer dashboardListViewer;
    private DashboardViewConfiguration configuration;
    private DBPDataSourceContainer dataSourceContainer;

    public static DashboardView openView(IWorkbenchWindow workbenchWindow, DBPDataSourceContainer dataSourceContainer) {
        try {
            return (DashboardView) workbenchWindow.getActivePage().showView(
                DashboardView.VIEW_ID,
                dataSourceContainer.getProject().getName() + "/" + dataSourceContainer.getId(),
                IWorkbenchPage.VIEW_ACTIVATE);
        } catch (PartInitException e) {
            DBWorkbench.getPlatformUI().showError(UIDashboardMessages.error_dashboard_view_cannot_open_title, UIDashboardMessages.error_dashboard_view_cannot_open_msg, e);
        }
        return null;
    }

    public DashboardView() {
        super();
    }

    public DashboardViewConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void createPartControl(Composite parent) {
        UIExecutionQueue.queueExec(() -> createDashboardControls(parent));
    }

    private void createDashboardControls(Composite parent) {
        try {
            String secondaryId = getViewSite().getSecondaryId();
            if (CommonUtils.isEmpty(secondaryId)) {
                throw new IllegalStateException("Dashboard view requires active database connection");
            }
            String projectName, dsId;
            int divPos = secondaryId.indexOf("/");
            if (divPos == -1) {
                projectName = null;
                dsId = secondaryId;
            } else {
                projectName = secondaryId.substring(0, divPos);
                dsId = secondaryId.substring(divPos + 1);
            }
            dataSourceContainer = DBUtils.findDataSource(projectName, dsId);
            if (dataSourceContainer == null) {
                throw new IllegalStateException("Database connection '" + dsId + "' not found");
            }

            dataSourceContainer.getRegistry().addDataSourceListener(this);

            configuration = new DashboardViewConfiguration(dataSourceContainer, secondaryId);
            dashboardListViewer = new DashboardListViewer(getSite(), dataSourceContainer, configuration);
            dashboardListViewer.createControl(parent);

            dashboardListViewer.createDashboardsFromConfiguration();

            getSite().setSelectionProvider(dashboardListViewer);

            parent.layout(true, true);

            updateStatus();
        } catch (Throwable e) {
            log.error("Error initializing dashboard view", e);
        }
    }

    @Override
    public void setFocus() {
        if (dashboardListViewer != null) {
            DashboardGroupContainer group = dashboardListViewer.getDefaultGroup();
            List<? extends DashboardContainer> items = group.getItems();
            if (!items.isEmpty()) {
                group.selectItem(items.get(0));
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
            dataSourceContainer.getRegistry().removeDataSourceListener(this);
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

    private void updateStatus() {
        UIUtils.asyncExec(() -> {
            setPartName(dataSourceContainer.getName() + (dataSourceContainer.isConnected() ? "" : UIDashboardMessages.dashboard_view_status_off));
        });
    }


    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
    }

    public DashboardListViewer getDashboardListViewer() {
        return dashboardListViewer;
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return null;
    }

}
